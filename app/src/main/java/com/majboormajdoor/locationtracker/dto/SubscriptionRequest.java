package com.majboormajdoor.locationtracker.dto;

public class SubscriptionRequest {

    private String userId;
    private String subType;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }
}
