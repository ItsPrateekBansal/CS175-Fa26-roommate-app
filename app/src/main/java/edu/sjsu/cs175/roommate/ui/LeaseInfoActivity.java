package edu.sjsu.cs175.roommate.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.databinding.ActivityPlaceholderBinding;

// TODO (Lease Info Page): static do's and don'ts for leases.
public class LeaseInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPlaceholderBinding binding = ActivityPlaceholderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.title_lease_info);
        binding.placeholderText.setText(R.string.title_lease_info);
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
