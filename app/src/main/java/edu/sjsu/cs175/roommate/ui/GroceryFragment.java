package edu.sjsu.cs175.roommate.ui;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.data.HouseholdRepository;
import edu.sjsu.cs175.roommate.data.model.GroceryItem;
import edu.sjsu.cs175.roommate.databinding.FragmentGroceryBinding;
import edu.sjsu.cs175.roommate.databinding.ItemGroceryBinding;

/** Shared household grocery list: add, check off and delete items. Updates live. */
public class GroceryFragment extends Fragment {

    private FragmentGroceryBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private String householdId;
    private ListenerRegistration listener;
    private final GroceryAdapter adapter = new GroceryAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGroceryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        binding.groceryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.groceryList.setAdapter(adapter);
        binding.addButton.setOnClickListener(v -> addItem());
        binding.itemInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItem();
                return true;
            }
            return false;
        });
        setInputEnabled(false);

        if (user == null) {
            showStatus(getString(R.string.error_signed_out));
            return;
        }
        HouseholdRepository.ensureHousehold(user.getUid(), new HouseholdRepository.Callback() {
            @Override
            public void onHousehold(@NonNull String id) {
                if (binding == null) return;
                householdId = id;
                setInputEnabled(true);
                listen();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (binding == null) return;
                binding.progress.setVisibility(View.GONE);
                showStatus(getString(R.string.error_grocery_load));
            }
        });
    }

    private void listen() {
        listener = db.collection(FirestoreSchema.GROCERIES)
                .whereEqualTo("householdId", householdId)
                .addSnapshotListener(this::onItems);
    }

    private void onItems(@Nullable QuerySnapshot snapshot, @Nullable Exception error) {
        if (binding == null) return;
        binding.progress.setVisibility(View.GONE);
        if (error != null || snapshot == null) {
            showStatus(getString(R.string.error_grocery_load));
            return;
        }
        List<Row> rows = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            GroceryItem item = doc.toObject(GroceryItem.class,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            if (item != null && item.getName() != null) rows.add(new Row(doc.getId(), item));
        }
        // Unbought items first, then oldest first. Sorted here so no Firestore index is needed.
        rows.sort(Comparator.comparing((Row r) -> r.purchased())
                .thenComparing(r -> r.item.getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        adapter.submit(rows);
        if (rows.isEmpty()) {
            showStatus(getString(R.string.grocery_empty));
        } else {
            binding.statusText.setVisibility(View.GONE);
        }
    }

    private void addItem() {
        if (householdId == null || user == null) return;
        CharSequence raw = binding.itemInput.getText();
        String name = raw == null ? "" : raw.toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.itemLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.itemLayout.setError(null);

        Map<String, Object> data = new HashMap<>();
        data.put("householdId", householdId);
        data.put("name", name);
        data.put("addedBy", user.getUid());
        data.put("purchased", false);
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection(FirestoreSchema.GROCERIES).add(data)
                .addOnFailureListener(e -> toast(R.string.error_generic));
        binding.itemInput.setText(null);
    }

    private void setPurchased(Row row, boolean purchased) {
        db.collection(FirestoreSchema.GROCERIES).document(row.id)
                .update("purchased", purchased)
                .addOnFailureListener(e -> toast(R.string.error_generic));
    }

    private void delete(Row row) {
        db.collection(FirestoreSchema.GROCERIES).document(row.id).delete()
                .addOnFailureListener(e -> toast(R.string.error_generic));
    }

    private void setInputEnabled(boolean enabled) {
        binding.itemInput.setEnabled(enabled);
        binding.addButton.setEnabled(enabled);
    }

    private void showStatus(String message) {
        binding.statusText.setText(message);
        binding.statusText.setVisibility(View.VISIBLE);
    }

    private void toast(int message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        if (listener != null) listener.remove();
        listener = null;
        binding = null;
        super.onDestroyView();
    }

    private static final class Row {
        final String id;
        final GroceryItem item;

        Row(String id, GroceryItem item) {
            this.id = id;
            this.item = item;
        }

        boolean purchased() {
            return Boolean.TRUE.equals(item.getPurchased());
        }
    }

    private final class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.Holder> {
        private final List<Row> rows = new ArrayList<>();

        void submit(List<Row> newRows) {
            rows.clear();
            rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemGroceryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Row row = rows.get(position);
            CheckBoxBinder.bind(holder.binding, row);
            holder.binding.purchasedCheck.setOnCheckedChangeListener(
                    (button, checked) -> setPurchased(row, checked));
            holder.binding.deleteButton.setOnClickListener(v -> delete(row));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ItemGroceryBinding binding;

            Holder(ItemGroceryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private static final class CheckBoxBinder {
        static void bind(ItemGroceryBinding b, Row row) {
            // Clear the old listener so setting the state doesn't write back to Firestore.
            b.purchasedCheck.setOnCheckedChangeListener(null);
            b.purchasedCheck.setText(row.item.getName());
            b.purchasedCheck.setChecked(row.purchased());
            int flags = b.purchasedCheck.getPaintFlags();
            b.purchasedCheck.setPaintFlags(row.purchased()
                    ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                    : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.purchasedCheck.setAlpha(row.purchased() ? 0.6f : 1f);
        }
    }
}
