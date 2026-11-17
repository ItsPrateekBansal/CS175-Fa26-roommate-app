package edu.sjsu.cs175.roommate.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.sjsu.cs175.roommate.chores.WeeklyRotation;
import edu.sjsu.cs175.roommate.data.FirestoreSchema;
import edu.sjsu.cs175.roommate.data.model.Chore;
import edu.sjsu.cs175.roommate.data.model.Household;
import edu.sjsu.cs175.roommate.data.model.UserProfile;

public class ChoresFragment extends Fragment {
    private static final int MAX_WEEKS = 52;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Chore> chores = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private final List<String> members = new ArrayList<>();
    private LinearLayout rows;
    private TextView status;
    private Button add;
    private long selected = System.currentTimeMillis();
    private String householdId;
    private CalendarView calendar;
    private ListenerRegistration listener;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        status = new TextView(requireContext());
        root.addView(status);
        calendar = new CalendarView(requireContext());
        calendar.setEnabled(false);
        root.addView(calendar);
        calendar.setOnDateChangeListener((view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.set(year, month, day, 12, 0, 0);
            selected = date.getTimeInMillis();
            render();
        });
        add = new Button(requireContext());
        add.setText("Add weekly chore");
        add.setEnabled(false);
        add.setOnClickListener(v -> promptChore());
        root.addView(add);
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        rows = new LinearLayout(requireContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            status.setText("Sign in to use shared chores.");
            return;
        }
        status.setText("Loading household…");
        db.collection(FirestoreSchema.USERS).document(user.getUid()).get().addOnSuccessListener(snapshot -> {
            if (getView() == null) return;
            UserProfile profile = snapshot.toObject(UserProfile.class);
            if (profile == null || profile.getHouseholdId() == null || profile.getHouseholdId().isEmpty()) {
                status.setText("Join a household to use shared chores."); return;
            }
            householdId = profile.getHouseholdId();
            db.collection(FirestoreSchema.HOUSEHOLDS).document(householdId).get().addOnSuccessListener(doc -> {
                if (getView() == null) return;
                Household household = doc.toObject(Household.class);
                if (household == null || household.getMemberIds() == null || household.getMemberIds().isEmpty()) {
                    status.setText("This household has no members to assign chores to."); return;
                }
                members.clear(); members.addAll(household.getMemberIds());
                listen();
            }).addOnFailureListener(e -> error());
        }).addOnFailureListener(e -> error());
    }

    private void listen() {
        listener = db.collection(FirestoreSchema.CHORES).whereEqualTo("householdId", householdId)
                .addSnapshotListener((snapshot, error) -> {
                    if (getView() == null) return;
                    if (error != null || snapshot == null) { error(); return; }
                    chores.clear(); ids.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Chore chore = doc.toObject(Chore.class);
                        if (chore != null) { chores.add(chore); ids.add(doc.getId()); }
                    }
                    add.setEnabled(true);
                    calendar.setEnabled(true);
                    render();
                });
    }

    private void promptChore() {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);
        EditText input = new EditText(requireContext());
        input.setHint("Chore name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(input);
        EditText weeksInput = new EditText(requireContext());
        weeksInput.setHint("Number of weeks (1–52)");
        weeksInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        weeksInput.setText("12");
        form.addView(weeksInput);
        TextView label = new TextView(requireContext());
        label.setText("First week's assignee");
        form.addView(label);
        Spinner assignee = new Spinner(requireContext());
        assignee.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, members));
        form.addView(assignee);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("Weekly chore")
                .setMessage("Starts on the selected date and rotates through household members each week.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Add", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) { input.setError("Enter a chore name"); return; }
            int weeks;
            try { weeks = Integer.parseInt(weeksInput.getText().toString()); }
            catch (NumberFormatException e) { weeksInput.setError("Enter a number from 1 to 52"); return; }
            if (weeks < 1 || weeks > MAX_WEEKS) { weeksInput.setError("Enter a number from 1 to 52"); return; }
            createSeries(title, weeks, assignee.getSelectedItemPosition());
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void createSeries(String title, int weeks, int startingMember) {
        WriteBatch batch = db.batch();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(selected);
        day.set(Calendar.HOUR_OF_DAY, 12); day.set(Calendar.MINUTE, 0); day.set(Calendar.SECOND, 0); day.set(Calendar.MILLISECOND, 0);
        long start = day.getTimeInMillis();
        for (int week = 0; week < weeks; week++) {
            Chore chore = new Chore();
            chore.setHouseholdId(householdId);
            chore.setTitle(title);
            chore.setAssignedTo(WeeklyRotation.assignee(members, start, day.getTimeInMillis(), startingMember));
            chore.setDueAt(new Timestamp(day.getTime()));
            chore.setCompleted(false);
            batch.set(db.collection(FirestoreSchema.CHORES).document(), chore);
            day.add(Calendar.DAY_OF_MONTH, 7);
        }
        add.setEnabled(false);
        batch.commit().addOnSuccessListener(unused -> { if (getView() != null) add.setEnabled(true); })
                .addOnFailureListener(e -> { if (getView() != null) { add.setEnabled(true); error(); } });
    }

    private void render() {
        if (getView() == null) return;
        status.setText("Chores for " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(selected)));
        rows.removeAllViews();
        int count = 0;
        for (int i = 0; i < chores.size(); i++) {
            Chore chore = chores.get(i);
            if (chore.getDueAt() == null || !sameDay(selected, chore.getDueAt().toDate().getTime())) continue;
            count++;
            CheckBox check = new CheckBox(requireContext());
            check.setText(chore.getTitle() + " — " + chore.getAssignedTo());
            check.setChecked(Boolean.TRUE.equals(chore.getCompleted()));
            String id = ids.get(i);
            check.setOnClickListener(v -> {
                boolean done = check.isChecked();
                check.setEnabled(false);
                db.collection(FirestoreSchema.CHORES).document(id).update("completed", done)
                        .addOnFailureListener(e -> { if (getView() != null) { check.setChecked(!done); check.setEnabled(true); error(); } });
            });
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(check, new LinearLayout.LayoutParams(0, -2, 1));
            Button reassign = new Button(requireContext());
            reassign.setText("Reassign");
            reassign.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                    .setTitle("Assign this occurrence")
                    .setItems(members.toArray(new String[0]), (dialog, choice) -> {
                        reassign.setEnabled(false);
                        db.collection(FirestoreSchema.CHORES).document(id).update("assignedTo", members.get(choice))
                                .addOnSuccessListener(unused -> reassign.setEnabled(true))
                                .addOnFailureListener(e -> { if (getView() != null) { reassign.setEnabled(true); error(); } });
                    }).setNegativeButton("Cancel", null).show());
            row.addView(reassign);
            rows.addView(row);
        }
        if (count == 0) { TextView empty = new TextView(requireContext()); empty.setText("No chores on this date."); rows.addView(empty); }
    }

    private boolean sameDay(long a, long b) {
        Calendar x = Calendar.getInstance(), y = Calendar.getInstance();
        x.setTimeInMillis(a); y.setTimeInMillis(b);
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR);
    }

    private void error() { if (getView() != null) status.setText("Couldn't load or save chores. Check your connection and Firestore permissions."); }
    @Override public void onDestroyView() {
        if (listener != null) { listener.remove(); listener = null; }
        super.onDestroyView();
    }
}
