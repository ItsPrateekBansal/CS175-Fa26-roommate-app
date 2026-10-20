package edu.sjsu.cs175.roommate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import edu.sjsu.cs175.roommate.databinding.FragmentPlaceholderBinding;

// Shared empty screen. Replace each feature's fragment with its own layout.
public abstract class PlaceholderFragment extends Fragment {

    @StringRes
    protected abstract int titleRes();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentPlaceholderBinding binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        binding.placeholderText.setText(titleRes());
        return binding.getRoot();
    }
}
