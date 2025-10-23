package com.majboormajdoor.locationtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.majboormajdoor.locationtracker.constants.AppConstants;

/**
 * Utility class for managing SharedPreferences operations
 * Centralizes all preference-related operations for better maintainability
 */
public class PreferenceManager {

    private static PreferenceManager instance;
    private SharedPreferences sharedPreferences;

    private PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Singleton pattern to ensure single instance
     */
    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Save user PIN
     */
    public void savePin(String pin) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AppConstants.PREF_PIN_KEY, pin);
        editor.apply();
    }

    /**
     * Get saved PIN
     */
    public String getPin() {
        return sharedPreferences.getString(AppConstants.PREF_PIN_KEY, AppConstants.DEFAULT_PIN);
    }

    /**
     * Check if this is first app launch
     */
    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean(AppConstants.PREF_IS_FIRST_LAUNCH, true);
    }

    /**
     * Set first launch flag to false
     */
    public void setFirstLaunchCompleted() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AppConstants.PREF_IS_FIRST_LAUNCH, false);
        editor.apply();
    }

    /**
     * Save phone number
     */
    public void savePhoneNumber(String phoneNumber) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AppConstants.PREF_PHONE_NUMBER, phoneNumber);
        editor.apply();
    }

    /**
     * Get saved phone number
     */
    public String getPhoneNumber() {
        return sharedPreferences.getString(AppConstants.PREF_PHONE_NUMBER, "");
    }

    /**
     * Save time interval
     */
    public void saveTimeInterval(int intervalMinutes) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(AppConstants.PREF_TIME_INTERVAL, intervalMinutes);
        editor.apply();
    }

    /**
     * Get saved time interval
     */
    public int getTimeInterval() {
        return sharedPreferences.getInt(AppConstants.PREF_TIME_INTERVAL, AppConstants.DEFAULT_TIME_INTERVAL_MINUTES);
    }

    /**
     * Check if PIN is set
     */
    public boolean isPinSet() {
        String pin = getPin();
        return pin != null && !pin.isEmpty() && pin.length() == AppConstants.PIN_LENGTH;
    }

    /**
     * Clear all preferences (for testing or reset purposes)
     */
    public void clearAllPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
