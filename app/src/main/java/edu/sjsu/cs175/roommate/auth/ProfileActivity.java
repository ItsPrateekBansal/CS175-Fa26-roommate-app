package edu.sjsu.cs175.roommate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.sjsu.cs175.roommate.MainActivity;
import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.data.model.UserProfile;
import edu.sjsu.cs175.roommate.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_FIRST_TIME = "first_time";

    // Values stored in Firestore. Matching compares these strings.
    public static final String SLEEP_EARLY = "Early bird";
    public static final String SLEEP_NIGHT = "Night owl";
    public static final String SLEEP_FLEXIBLE = "Flexible";
    public static final String CLEAN_VERY_TIDY = "Very tidy";
    public static final String CLEAN_AVERAGE = "Average";
    public static final String CLEAN_RELAXED = "Relaxed";

    private ActivityProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private boolean firstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        firstTime = getIntent().getBooleanExtra(EXTRA_FIRST_TIME, false);

        setTitle(firstTime ? R.string.title_profile_setup : R.string.title_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!firstTime);
        }
        if (firstTime) {
            // A new user must save a profile before using the app.
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Toast.makeText(ProfileActivity.this,
                            R.string.profile_required, Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.emailText.setText(user.getEmail());
        binding.saveButton.setOnClickListener(v -> save());
        loadProfile();
    }

    private void loadProfile() {
        setLoading(true);
        db.collection(FirestoreSchema.USERS).document(user.getUid()).get()
                .addOnSuccessListener(this::fillForm)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(e.getMessage());
                });
    }

    private void fillForm(DocumentSnapshot doc) {
        setLoading(false);
        if (!doc.exists()) return;
        UserProfile profile = doc.toObject(UserProfile.class);
        if (profile == null) return;

        binding.nameInput.setText(profile.getDisplayName());
        if (profile.getBudget() != null) {
            binding.budgetInput.setText(String.format(Locale.US, "%.0f", profile.getBudget()));
        }
        checkByValue(binding.sleepGroup, profile.getSleepTime());
        checkByValue(binding.cleanGroup, profile.getCleanliness());
        binding.smokingSwitch.setChecked(Boolean.TRUE.equals(profile.getSmoking()));
        binding.petsSwitch.setChecked(Boolean.TRUE.equals(profile.getPets()));
    }

    private void save() {
        String name = text(binding.nameInput.getText());
        String budgetText = text(binding.budgetInput.getText());
        String sleep = selectedValue(binding.sleepGroup);
        String clean = selectedValue(binding.cleanGroup);

        binding.nameLayout.setError(null);
        binding.budgetLayout.setError(null);
        boolean ok = true;
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.error_required));
            ok = false;
        }
        Double budget = parseBudget(budgetText);
        if (budget == null) {
            binding.budgetLayout.setError(getString(R.string.error_budget));
            ok = false;
        }
        if (sleep == null || clean == null) {
            toast(getString(R.string.error_choose_options));
            ok = false;
        }
        if (!ok) return;

        UserProfile profile = new UserProfile(name, user.getEmail());
        profile.setBudget(budget);
        profile.setSleepTime(sleep);
        profile.setCleanliness(clean);
        profile.setSmoking(binding.smokingSwitch.isChecked());
        profile.setPets(binding.petsSwitch.isChecked());

        setLoading(true);
        // merge() keeps fields this screen does not edit, like householdId.
        db.collection(FirestoreSchema.USERS).document(user.getUid())
                .set(profileFields(profile), SetOptions.merge())
                .addOnSuccessListener(unused -> onSaved())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(e.getMessage());
                });
    }

    private Map<String, Object> profileFields(UserProfile p) {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", p.getDisplayName());
        map.put("email", p.getEmail());
        map.put("budget", p.getBudget());
        map.put("sleepTime", p.getSleepTime());
        map.put("cleanliness", p.getCleanliness());
        map.put("smoking", p.getSmoking());
        map.put("pets", p.getPets());
        return map;
    }

    private void onSaved() {
        setLoading(false);
        toast(getString(R.string.profile_saved));
        if (firstTime) {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private static Double parseBudget(String text) {
        try {
            double value = Double.parseDouble(text);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String selectedValue(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == View.NO_ID) return null;
        RadioButton button = findViewById(id);
        return button.getText().toString();
    }

    private static void checkByValue(RadioGroup group, String value) {
        if (value == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton
                    && value.contentEquals(((RadioButton) child).getText())) {
                group.check(child.getId());
                return;
            }
        }
    }

    private static String text(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!loading);
    }

    private void toast(String message) {
        Toast.makeText(this,
                TextUtils.isEmpty(message) ? getString(R.string.error_generic) : message,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
