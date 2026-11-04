package edu.sjsu.cs175.roommate.data.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Household {
    private String name;
    private String createdBy;
    private List<String> memberIds = new ArrayList<>();

    public Household() { }

    public Household(String name, String createdBy) {
        this.name = name;
        this.createdBy = createdBy;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
}
