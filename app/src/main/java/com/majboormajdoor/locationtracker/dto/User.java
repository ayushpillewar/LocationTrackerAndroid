package com.majboormajdoor.locationtracker.dto;

public class User {

    private String userId;
    private String email;
    private String subStartDate;
    private String subEndDate;
    private String insertionTimestamp;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubStartDate() {
        return subStartDate;
    }

    public void setSubStartDate(String subStartDate) {
        this.subStartDate = subStartDate;
    }

    public String getSubEndDate() {
        return subEndDate;
    }

    public void setSubEndDate(String subEndDate) {
        this.subEndDate = subEndDate;
    }

    public String getInsertionTimestamp() {
        return insertionTimestamp;
    }

    public void setInsertionTimestamp(String insertionTimestamp) {
        this.insertionTimestamp = insertionTimestamp;
    }
}
