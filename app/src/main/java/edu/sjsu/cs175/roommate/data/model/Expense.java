package edu.sjsu.cs175.roommate.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Expense {
    private String householdId;
    private String title;
    private String paidBy;
    private Double amount;
    private String splitType;
    private Timestamp createdAt;
    // Who owes how much of this bill: user ID -> dollars.
    private Map<String, Double> shares = new HashMap<>();

    public Expense() { }

    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getSplitType() { return splitType; }
    public void setSplitType(String splitType) { this.splitType = splitType; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Map<String, Double> getShares() { return shares; }
    public void setShares(Map<String, Double> shares) { this.shares = shares; }
}
