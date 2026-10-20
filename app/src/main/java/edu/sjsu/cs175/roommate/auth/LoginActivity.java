package edu.sjsu.cs175.roommate.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.sjsu.cs175.roommate.MainActivity;
import edu.sjsu.cs175.roommate.databinding.ActivityLoginBinding;

// TODO (Login & Profile): sign up and log in with FirebaseAuth.
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> openMain());
        binding.signupButton.setOnClickListener(v -> openMain());
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
