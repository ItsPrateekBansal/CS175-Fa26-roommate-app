package edu.sjsu.cs175.roommate.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.databinding.ActivityPlaceholderBinding;

// TODO (Login & Profile): budget, sleep time, cleanliness, smoking, pets.
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPlaceholderBinding binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.title_profile);
        binding.placeholderText.setText(R.string.title_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
