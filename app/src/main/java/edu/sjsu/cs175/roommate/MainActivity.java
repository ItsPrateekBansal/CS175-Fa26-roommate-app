package edu.sjsu.cs175.roommate;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import edu.sjsu.cs175.roommate.auth.LoginActivity;
import edu.sjsu.cs175.roommate.auth.ProfileActivity;
import edu.sjsu.cs175.roommate.databinding.ActivityMainBinding;
import edu.sjsu.cs175.roommate.ui.ChatFragment;
import edu.sjsu.cs175.roommate.ui.ChoresFragment;
import edu.sjsu.cs175.roommate.ui.ExpensesFragment;
import edu.sjsu.cs175.roommate.ui.GroceryFragment;
import edu.sjsu.cs175.roommate.ui.LeaseInfoActivity;
import edu.sjsu.cs175.roommate.ui.MatchesFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showScreen(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_matches);
        }
    }

    private void showScreen(int itemId) {
        Fragment fragment;
        int title;
        if (itemId == R.id.nav_chat) {
            fragment = new ChatFragment();
            title = R.string.title_chat;
        } else if (itemId == R.id.nav_expenses) {
            fragment = new ExpensesFragment();
            title = R.string.title_expenses;
        } else if (itemId == R.id.nav_chores) {
            fragment = new ChoresFragment();
            title = R.string.title_chores;
        } else if (itemId == R.id.nav_grocery) {
            fragment = new GroceryFragment();
            title = R.string.title_grocery;
        } else {
            fragment = new MatchesFragment();
            title = R.string.title_matches;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_lease_info) {
            startActivity(new Intent(this, LeaseInfoActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
