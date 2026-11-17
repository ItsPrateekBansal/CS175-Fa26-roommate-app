package edu.sjsu.cs175.roommate.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.sjsu.cs175.roommate.data.model.Household;

/** Looks up the signed-in user's household, creating one if they don't have it yet. */
public final class HouseholdRepository {

    public interface Callback {
        void onHousehold(@NonNull String householdId);
        void onError(@NonNull Exception e);
    }

    private HouseholdRepository() { }

    public static void ensureHousehold(@NonNull String uid, @NonNull Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection(FirestoreSchema.USERS).document(uid);
        userRef.get().addOnSuccessListener(doc -> {
            String existing = doc.getString("householdId");
            if (existing != null && !existing.trim().isEmpty()) {
                callback.onHousehold(existing);
                return;
            }
            // No household yet: start one with just this user as a member.
            DocumentReference householdRef = db.collection(FirestoreSchema.HOUSEHOLDS).document();
            Household household = new Household("My household", uid);
            household.setMemberIds(Collections.singletonList(uid));

            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("householdId", householdRef.getId());

            WriteBatch batch = db.batch();
            batch.set(householdRef, household);
            batch.set(userRef, userUpdate, SetOptions.merge());
            batch.commit()
                    .addOnSuccessListener(unused -> callback.onHousehold(householdRef.getId()))
                    .addOnFailureListener(callback::onError);
        }).addOnFailureListener(callback::onError);
    }
}
