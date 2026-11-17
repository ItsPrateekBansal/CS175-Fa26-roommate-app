package edu.sjsu.cs175.roommate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.sjsu.cs175.roommate.MainActivity;
import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.loginButton.setOnClickListener(v -> logIn());
        binding.signupButton.setOnClickListener(v -> signUp());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            routeUser(user);
        }
    }

    private void logIn() {
        if (!validInput()) return;
        setLoading(true);
        auth.signInWithEmailAndPassword(email(), password())
                .addOnSuccessListener(result -> routeUser(result.getUser()))
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void signUp() {
        if (!validInput()) return;
        setLoading(true);
        auth.createUserWithEmailAndPassword(email(), password())
                .addOnSuccessListener(result -> openProfile(true))
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // Users with a saved profile go to the app. New users fill the profile first.
    private void routeUser(FirebaseUser user) {
        setLoading(true);
        db.collection(FirestoreSchema.USERS).document(user.getUid()).get()
                .addOnSuccessListener(this::onProfileLoaded)
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void onProfileLoaded(DocumentSnapshot doc) {
        if (doc.exists() && doc.getString("displayName") != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            openProfile(true);
        }
    }

    private void openProfile(boolean firstTime) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_FIRST_TIME, firstTime);
        startActivity(intent);
        finish();
    }

    private boolean validInput() {
        boolean ok = true;
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email()).matches()) {
            binding.emailLayout.setError(getString(R.string.error_email));
            ok = false;
        }
        if (password().length() < MIN_PASSWORD_LENGTH) {
            binding.passwordLayout.setError(getString(R.string.error_password));
            ok = false;
        }
        return ok;
    }

    private String email() {
        CharSequence text = binding.emailInput.getText();
        return text == null ? "" : text.toString().trim();
    }

    private String password() {
        CharSequence text = binding.passwordInput.getText();
        return text == null ? "" : text.toString();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.signupButton.setEnabled(!loading);
    }

    private void showError(String message) {
        setLoading(false);
        Toast.makeText(this,
                TextUtils.isEmpty(message) ? getString(R.string.error_generic) : message,
                Toast.LENGTH_LONG).show();
    }
}
