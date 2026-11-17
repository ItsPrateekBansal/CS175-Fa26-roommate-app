package edu.sjsu.cs175.roommate.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    private String householdId;
    private String senderId;
    private String text;
    private Timestamp createdAt;

    public Message() { }

    public Message(String householdId, String senderId, String text, Timestamp createdAt) {
        this.householdId = householdId;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
