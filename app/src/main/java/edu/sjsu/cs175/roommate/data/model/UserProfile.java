package edu.sjsu.cs175.roommate.data.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserProfile {
    private String displayName;
    private String email;
    private String householdId;
    private Double budget;
    private String sleepTime;
    private String cleanliness;
    private Boolean smoking;
    private Boolean pets;

    public UserProfile() {
        // Required by Firestore deserialization.
    }

    public UserProfile(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public String getSleepTime() { return sleepTime; }
    public void setSleepTime(String sleepTime) { this.sleepTime = sleepTime; }
    public String getCleanliness() { return cleanliness; }
    public void setCleanliness(String cleanliness) { this.cleanliness = cleanliness; }
    public Boolean getSmoking() { return smoking; }
    public void setSmoking(Boolean smoking) { this.smoking = smoking; }
    public Boolean getPets() { return pets; }
    public void setPets(Boolean pets) { this.pets = pets; }
}
