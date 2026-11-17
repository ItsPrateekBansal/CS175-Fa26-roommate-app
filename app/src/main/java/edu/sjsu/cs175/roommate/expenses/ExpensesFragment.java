package edu.sjsu.cs175.roommate.expenses;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.data.HouseholdRepository;
import edu.sjsu.cs175.roommate.data.model.Expense;
import edu.sjsu.cs175.roommate.data.model.Household;
import edu.sjsu.cs175.roommate.databinding.FragmentExpensesBinding;

/**
 * Shared expenses for the user's household: balances on top, bill list below.
 * Users without a household see a create / join screen first.
 */
public class ExpensesFragment extends Fragment {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private FragmentExpensesBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private String householdId;
    private String displayName;

    private final Map<String, String> names = new HashMap<>();
    private final List<String> memberIds = new ArrayList<>();
    private final List<ExpenseAdapter.Row> rows = new ArrayList<>();
    private ExpenseAdapter adapter;

    private ListenerRegistration householdListener;
    private ListenerRegistration expensesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        adapter = new ExpenseAdapter(this::confirmDelete);
        binding.expenseList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.expenseList.setAdapter(adapter);

        binding.createHouseholdButton.setOnClickListener(v -> createHousehold());
        binding.joinHouseholdButton.setOnClickListener(v -> joinHousehold());
        binding.householdCode.setOnClickListener(v -> copyCode());
        binding.addExpenseButton.setOnClickListener(v -> openAddExpense());

        loadUser();
    }

    // Step 1: find the user's household.
    private void loadUser() {
        showState(State.LOADING);
        db.collection(FirestoreSchema.USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    displayName = doc.getString("displayName");
                    householdId = doc.getString("householdId");
                    if (TextUtils.isEmpty(householdId)) {
                        showState(State.NO_HOUSEHOLD);
                    } else {
                        listenToHousehold();
                    }
                })
                .addOnFailureListener(this::showError);
    }

    private void createHousehold() {
        String name = TextUtils.isEmpty(displayName)
                ? getString(R.string.default_household_name)
                : getString(R.string.household_name_for, displayName);
        showState(State.LOADING);
        new HouseholdRepository().create(uid, name)
                .addOnSuccessListener(id -> {
                    householdId = id;
                    listenToHousehold();
                })
                .addOnFailureListener(this::showError);
    }

    private void joinHousehold() {
        CharSequence input = binding.joinCodeInput.getText();
        String code = input == null ? "" : input.toString().trim();
        if (code.isEmpty()) {
            binding.joinCodeLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.joinCodeLayout.setError(null);
        showState(State.LOADING);
        new HouseholdRepository().join(uid, code)
                .addOnSuccessListener(id -> {
                    householdId = id;
                    listenToHousehold();
                })
                .addOnFailureListener(e -> {
                    showState(State.NO_HOUSEHOLD);
                    binding.joinCodeLayout.setError(getString(R.string.error_bad_code));
                });
    }

    // Step 2: keep the member list current, so new roommates show up.
    private void listenToHousehold() {
        removeListeners();
        householdListener = db.collection(FirestoreSchema.HOUSEHOLDS).document(householdId)
                .addSnapshotListener((doc, e) -> {
                    if (binding == null) return;
                    if (e != null) {
                        showError(e);
                        return;
                    }
                    if (doc == null || !doc.exists()) return;
                    Household household = doc.toObject(Household.class);
                    if (household == null) return;
                    binding.householdName.setText(household.getName());
                    binding.householdCode.setText(getString(R.string.household_code, householdId));
                    memberIds.clear();
                    memberIds.addAll(household.getMemberIds());
                    loadMemberNames();
                });
    }

    private void loadMemberNames() {
        if (memberIds.isEmpty()) return;
        // whereIn allows up to 30 values, more than enough for one household.
        db.collection(FirestoreSchema.USERS)
                .whereIn(FieldPath.documentId(), new ArrayList<>(memberIds))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    names.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("displayName");
                        names.put(doc.getId(), name == null ? doc.getString("email") : name);
                    }
                    if (expensesListener == null) {
                        listenToExpenses();
                    } else {
                        render();
                    }
                })
                .addOnFailureListener(this::showError);
    }

    // Step 3: live list of this household's expenses.
    private void listenToExpenses() {
        // Only one where() and no orderBy(), so Firestore needs no custom index.
        expensesListener = db.collection(FirestoreSchema.EXPENSES)
                .whereEqualTo("householdId", householdId)
                .addSnapshotListener(this::onExpenses);
    }

    private void onExpenses(@Nullable QuerySnapshot snapshot, @Nullable Exception e) {
        if (binding == null) return;
        if (e != null) {
            showError(e);
            return;
        }
        if (snapshot == null) return;
        rows.clear();
        for (QueryDocumentSnapshot doc : snapshot) {
            rows.add(new ExpenseAdapter.Row(doc.getId(), doc.toObject(Expense.class)));
        }
        // Newest first.
        rows.sort((a, b) -> {
            if (a.expense.getCreatedAt() == null || b.expense.getCreatedAt() == null) return 0;
            return b.expense.getCreatedAt().compareTo(a.expense.getCreatedAt());
        });
        render();
    }

    private void render() {
        showState(State.CONTENT);
        adapter.submit(rows, names, uid);
        binding.emptyText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        renderBalances();
    }

    private void renderBalances() {
        Map<String, Long> net = new HashMap<>();
        for (ExpenseAdapter.Row row : rows) {
            Expense e = row.expense;
            if (e.getPaidBy() == null || e.getAmount() == null || e.getShares() == null) continue;
            Map<String, Long> shares = new HashMap<>();
            for (Map.Entry<String, Double> s : e.getShares().entrySet()) {
                shares.put(s.getKey(), BalanceCalculator.toCents(s.getValue()));
            }
            BalanceCalculator.addExpense(net, e.getPaidBy(),
                    BalanceCalculator.toCents(e.getAmount()), shares);
        }

        binding.balanceList.removeAllViews();
        List<BalanceCalculator.Transfer> transfers = BalanceCalculator.settle(net);
        if (transfers.isEmpty()) {
            addBalanceLine(getString(R.string.all_settled), false);
            return;
        }
        for (BalanceCalculator.Transfer t : transfers) {
            String amount = MONEY.format(BalanceCalculator.toDollars(t.cents));
            String line;
            if (uid.equals(t.from)) {
                line = getString(R.string.you_owe, nameOf(t.to), amount);
            } else if (uid.equals(t.to)) {
                line = getString(R.string.owes_you, nameOf(t.from), amount);
            } else {
                line = getString(R.string.owes, nameOf(t.from), nameOf(t.to), amount);
            }
            addBalanceLine(line, uid.equals(t.from) || uid.equals(t.to));
        }
    }

    private void addBalanceLine(String text, boolean involvesMe) {
        TextView line = new TextView(requireContext());
        line.setText(text);
        line.setTextSize(16);
        line.setPadding(0, 6, 0, 6);
        if (involvesMe) line.setTypeface(line.getTypeface(), android.graphics.Typeface.BOLD);
        binding.balanceList.addView(line);
    }

    private String nameOf(String memberUid) {
        String name = names.get(memberUid);
        return name == null ? getString(R.string.former_member) : name;
    }

    private void openAddExpense() {
        ArrayList<String> ids = new ArrayList<>(memberIds);
        ArrayList<String> memberNames = new ArrayList<>();
        for (String id : ids) {
            memberNames.add(id.equals(uid) ? getString(R.string.you) : nameOf(id));
        }
        Intent intent = new Intent(requireContext(), AddExpenseActivity.class);
        intent.putExtra(AddExpenseActivity.EXTRA_HOUSEHOLD_ID, householdId);
        intent.putStringArrayListExtra(AddExpenseActivity.EXTRA_MEMBER_IDS, ids);
        intent.putStringArrayListExtra(AddExpenseActivity.EXTRA_MEMBER_NAMES, memberNames);
        startActivity(intent);
    }

    private void confirmDelete(ExpenseAdapter.Row row) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_expense_title)
                .setMessage(getString(R.string.delete_expense_message, row.expense.getTitle()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) ->
                        db.collection(FirestoreSchema.EXPENSES).document(row.id).delete()
                                .addOnFailureListener(this::showError))
                .show();
    }

    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Household code", householdId));
        Toast.makeText(requireContext(), R.string.code_copied, Toast.LENGTH_SHORT).show();
    }

    private enum State { LOADING, NO_HOUSEHOLD, CONTENT }

    private void showState(State state) {
        if (binding == null) return;
        binding.progress.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        binding.setupGroup.setVisibility(state == State.NO_HOUSEHOLD ? View.VISIBLE : View.GONE);
        binding.contentGroup.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        if (state == State.CONTENT) {
            binding.addExpenseButton.show();
        } else {
            binding.addExpenseButton.hide();
        }
    }

    private void showError(Exception e) {
        if (binding == null || getContext() == null) return;
        Toast.makeText(getContext(),
                e.getMessage() == null ? getString(R.string.error_generic) : e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    private void removeListeners() {
        if (householdListener != null) householdListener.remove();
        if (expensesListener != null) expensesListener.remove();
        householdListener = null;
        expensesListener = null;
    }

    @Override
    public void onDestroyView() {
        removeListeners();
        binding = null;
        super.onDestroyView();
    }
}
