package edu.sjsu.cs175.roommate.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;

import edu.sjsu.cs175.roommate.data.model.Household;

/**
 * Create or join a household. The household document ID is the join code.
 * Shared features (expenses, grocery, chores) all read users/{uid}.householdId.
 */
public final class HouseholdRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Creates a household with this user as the only member. Returns the new household ID. */
    public Task<String> create(String uid, String name) {
        Household household = new Household(name, uid);
        household.setMemberIds(Collections.singletonList(uid));
        DocumentReference ref = db.collection(FirestoreSchema.HOUSEHOLDS).document();
        return ref.set(household)
                .onSuccessTask(unused -> linkUser(uid, ref.getId()))
                .onSuccessTask(unused -> Tasks.forResult(ref.getId()));
    }

    /** Adds this user to an existing household. Fails if the code does not exist. */
    public Task<String> join(String uid, String code) {
        DocumentReference ref = db.collection(FirestoreSchema.HOUSEHOLDS).document(code);
        return ref.get().onSuccessTask(doc -> {
            if (!doc.exists()) {
                return Tasks.<String>forException(new IllegalArgumentException("No household with that code"));
            }
            return ref.update("memberIds", FieldValue.arrayUnion(uid))
                    .onSuccessTask(unused -> linkUser(uid, code))
                    .onSuccessTask(unused -> Tasks.forResult(code));
        });
    }

    private Task<Void> linkUser(String uid, String householdId) {
        return db.collection(FirestoreSchema.USERS).document(uid)
                .set(Collections.singletonMap("householdId", householdId), SetOptions.merge());
    }
}
