package edu.sjsu.cs175.roommate.expenses;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.data.model.Expense;
import edu.sjsu.cs175.roommate.databinding.ActivityAddExpenseBinding;

/** Form to add a bill and split it equally or by custom amounts. */
public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_HOUSEHOLD_ID = "household_id";
    public static final String EXTRA_MEMBER_IDS = "member_ids";
    public static final String EXTRA_MEMBER_NAMES = "member_names";

    private static final String SPLIT_EQUAL = "equal";
    private static final String SPLIT_CUSTOM = "custom";
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private ActivityAddExpenseBinding binding;
    private String householdId;
    private List<String> memberIds;
    private List<String> memberNames;
    private final List<TextInputEditText> customInputs = new ArrayList<>();
    private int payerIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.title_add_expense);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        householdId = getIntent().getStringExtra(EXTRA_HOUSEHOLD_ID);
        memberIds = getIntent().getStringArrayListExtra(EXTRA_MEMBER_IDS);
        memberNames = getIntent().getStringArrayListExtra(EXTRA_MEMBER_NAMES);
        if (householdId == null || memberIds == null || memberNames == null || memberIds.isEmpty()) {
            finish();
            return;
        }

        setUpPayer();
        setUpCustomFields();

        binding.splitGroup.setOnCheckedChangeListener((group, checkedId) -> updateSplitViews());
        binding.amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updateSplitViews(); }
        });
        binding.saveButton.setOnClickListener(v -> save());
        updateSplitViews();
    }

    private void setUpPayer() {
        // Default payer is the current user.
        String me = FirebaseAuth.getInstance().getUid();
        payerIndex = Math.max(0, memberIds.indexOf(me));
        binding.paidByInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, memberNames));
        binding.paidByInput.setText(memberNames.get(payerIndex), false);
        binding.paidByInput.setOnItemClickListener((parent, view, position, id) -> payerIndex = position);
    }

    private void setUpCustomFields() {
        for (String name : memberNames) {
            TextInputLayout layout = new TextInputLayout(this);
            layout.setHint(name);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = 8;
            layout.setLayoutParams(params);

            TextInputEditText input = new TextInputEditText(layout.getContext());
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            layout.addView(input);

            binding.customContainer.addView(layout);
            customInputs.add(input);
        }
    }

    private void updateSplitViews() {
        boolean custom = binding.splitCustom.isChecked();
        binding.customContainer.setVisibility(custom ? View.VISIBLE : View.GONE);

        Long total = parseCents(text(binding.amountInput.getText()));
        if (custom || total == null) {
            binding.equalPreview.setVisibility(View.GONE);
            return;
        }
        long each = total / memberIds.size();
        binding.equalPreview.setVisibility(View.VISIBLE);
        binding.equalPreview.setText(getString(R.string.equal_preview,
                MONEY.format(BalanceCalculator.toDollars(each)), memberIds.size()));
    }

    private void save() {
        String title = text(binding.titleInput.getText());
        Long totalCents = parseCents(text(binding.amountInput.getText()));

        binding.titleLayout.setError(null);
        binding.amountLayout.setError(null);
        boolean ok = true;
        if (TextUtils.isEmpty(title)) {
            binding.titleLayout.setError(getString(R.string.error_required));
            ok = false;
        }
        if (totalCents == null || totalCents <= 0) {
            binding.amountLayout.setError(getString(R.string.error_amount));
            ok = false;
        }
        if (!ok) return;

        Map<String, Long> sharesCents;
        boolean custom = binding.splitCustom.isChecked();
        if (custom) {
            sharesCents = customShares(totalCents);
            if (sharesCents == null) return;
        } else {
            sharesCents = BalanceCalculator.splitEqually(totalCents, memberIds);
        }

        Map<String, Double> shares = new HashMap<>();
        for (Map.Entry<String, Long> s : sharesCents.entrySet()) {
            shares.put(s.getKey(), BalanceCalculator.toDollars(s.getValue()));
        }

        Expense expense = new Expense();
        expense.setHouseholdId(householdId);
        expense.setTitle(title);
        expense.setPaidBy(memberIds.get(payerIndex));
        expense.setAmount(BalanceCalculator.toDollars(totalCents));
        expense.setSplitType(custom ? SPLIT_CUSTOM : SPLIT_EQUAL);
        expense.setShares(shares);
        expense.setCreatedAt(Timestamp.now());

        binding.saveButton.setEnabled(false);
        binding.progress.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection(FirestoreSchema.EXPENSES).add(expense)
                .addOnSuccessListener(ref -> finish())
                .addOnFailureListener(e -> {
                    binding.saveButton.setEnabled(true);
                    binding.progress.setVisibility(View.GONE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Reads the custom amounts. Returns null and shows an error if they don't add up. */
    private Map<String, Long> customShares(long totalCents) {
        Map<String, Long> shares = new HashMap<>();
        long sum = 0;
        for (int i = 0; i < memberIds.size(); i++) {
            String value = text(customInputs.get(i).getText());
            Long cents = value.isEmpty() ? Long.valueOf(0) : parseCents(value);
            if (cents == null || cents < 0) {
                toast(getString(R.string.error_amount));
                return null;
            }
            if (cents > 0) shares.put(memberIds.get(i), cents);
            sum += cents;
        }
        if (sum != totalCents) {
            toast(getString(R.string.error_split_total,
                    MONEY.format(BalanceCalculator.toDollars(sum)),
                    MONEY.format(BalanceCalculator.toDollars(totalCents))));
            return null;
        }
        return shares;
    }

    private static Long parseCents(String text) {
        try {
            return BalanceCalculator.toCents(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
