package com.majboormajdoor.locationtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Location data transfer object for API communication
 */
public class Location {

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("insertionTimestamp")
    private String insertionTimestamp;

    @JsonProperty("userId")
    private String userId;

    // Constructors
    public Location() {}

    public Location(double latitude, double longitude, String email, String timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userEmail = email;
        this.insertionTimestamp = timestamp;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getInsertionTimestamp() {
        return insertionTimestamp;
    }

    public void setInsertionTimestamp(String insertionTimestamp) {
        this.insertionTimestamp = insertionTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }


    @Override
    public String toString() {
        return "Location{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", userEmail='" + userEmail + '\'' +
                ", insertionTimestamp='" + insertionTimestamp + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
