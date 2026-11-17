package edu.sjsu.cs175.roommate.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Chore {
    private String householdId;
    private String title;
    private String assignedTo;
    private Timestamp dueAt;
    private Boolean completed;

    public Chore() { }

    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public Timestamp getDueAt() { return dueAt; }
    public void setDueAt(Timestamp dueAt) { this.dueAt = dueAt; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
