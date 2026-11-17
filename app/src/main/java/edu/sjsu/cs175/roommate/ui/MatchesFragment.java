package edu.sjsu.cs175.roommate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import edu.sjsu.cs175.roommate.matching.MatchEngine;

public class MatchesFragment extends Fragment {
    private TextView status;
    private ListView list;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        status = new TextView(requireContext());
        list = new ListView(requireContext());
        root.addView(status);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        loadMatches();
    }

    private void loadMatches() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { status.setText("Sign in to see roommate matches."); return; }
        status.setText("Loading matches...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(profile -> {
            if (!isAdded() || getView() == null) return;
            Map<String, Object> mine = profile.getData();
            if (mine == null) { status.setText("Create your profile to see matches."); return; }
            db.collection("users").get().addOnSuccessListener(snapshot -> {
                if (!isAdded() || getView() == null) return;
                List<MatchEngine.Result> results = new ArrayList<>();
                for (DocumentSnapshot candidate : snapshot.getDocuments()) {
                    if (candidate.getId().equals(user.getUid()) || candidate.getData() == null) continue;
                    MatchEngine.Result result = MatchEngine.compare(candidate.getId(), mine, candidate.getData());
                    if (result.compared > 0) results.add(result);
                }
                MatchEngine.sort(results);
                List<String> rows = new ArrayList<>();
                for (MatchEngine.Result result : results) rows.add(result.name + " — " + result.percentage() + "% match (" + result.matched + "/" + result.compared + " fields)");
                status.setText(rows.isEmpty() ? "No matches yet. Add profile preferences and check back." : "Matches based on shared profile fields:");
                list.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rows));
            }).addOnFailureListener(error -> showError());
        }).addOnFailureListener(error -> showError());
    }
    private void showError() {
        if (isAdded() && getView() != null) status.setText("Couldn't load matches. Check your connection or access permissions and try again.");
    }
}
