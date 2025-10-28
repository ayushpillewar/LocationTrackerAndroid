package com.majboormajdoor.locationtracker.constants;

/**
 * Constants class to centralize all hardcoded values in the application
 * Following good coding practices by avoiding magic numbers and strings
 */
public class AppConstants {

    // Shared Preferences Keys
    public static final String PREFS_NAME = "LocationTrackerPrefs";
    public static final String PREF_PIN_KEY = "user_pin";
    public static final String PREF_IS_FIRST_LAUNCH = "is_first_launch";
    public static final String PREF_PHONE_NUMBER = "phone_number";
    public static final String PREF_EMAIL_ADDRESS = "email_address";
    public static final String PREF_TIME_INTERVAL = "time_interval";

    // PIN related constants
    public static final int PIN_LENGTH = 4;
    public static final String DEFAULT_PIN = "";

    // Location tracking constants
    public static final int MIN_TIME_INTERVAL_HOURS = 1;
    public static final int MAX_TIME_INTERVAL_HOURS = 12;
    public static final int DEFAULT_TIME_INTERVAL_HOURS = 1;

    // Keep minute constants for backward compatibility and internal calculations
    public static final int MIN_TIME_INTERVAL_MINUTES = MIN_TIME_INTERVAL_HOURS * 60;
    public static final int MAX_TIME_INTERVAL_MINUTES = MAX_TIME_INTERVAL_HOURS * 60;
    public static final int DEFAULT_TIME_INTERVAL_MINUTES = DEFAULT_TIME_INTERVAL_HOURS * 60;

    // Location service constants
    public static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    public static final long FASTEST_LOCATION_INTERVAL = 5000; // 5 seconds
    public static final float MINIMUM_DISTANCE = 10.0f; // 10 meters

    // Service and notification constants
    public static final String SERVICE_ACTION_START = "START_LOCATION_SERVICE";
    public static final String SERVICE_ACTION_STOP = "STOP_LOCATION_SERVICE";
    public static final int LOCATION_SERVICE_NOTIFICATION_ID = 1001;
    public static final String NOTIFICATION_CHANNEL_ID = "LocationTrackingChannel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Location Tracking";

    // Intent extras
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_EMAIL_ADDRESS = "email_address";
    public static final String EXTRA_TIME_INTERVAL = "time_interval";

    // Permission request codes
    public static final int PERMISSION_REQUEST_LOCATION = 1000;
    public static final int PERMISSION_REQUEST_SMS = 1001;
    public static final int PERMISSION_REQUEST_NOTIFICATION = 1002;

    // UI related constants
    public static final int PIN_INPUT_DELAY_MS = 100;
    public static final int SPLASH_DELAY_MS = 2000;

    // SMS message template
    public static final String SMS_MESSAGE_TEMPLATE = "Location Update: https://maps.google.com/?q=%f,%f - Time: %s";

    // Error messages
    public static final String ERROR_INVALID_PIN = "Invalid PIN. Please try again.";
    public static final String ERROR_PIN_MISMATCH = "PINs do not match. Please try again.";
    public static final String ERROR_INVALID_PHONE = "Please enter a valid phone number.";
    public static final String ERROR_LOCATION_PERMISSION = "Location permission is required for this app.";
    public static final String ERROR_SMS_PERMISSION = "SMS permission is required to send location updates.";

    // Success messages
    public static final String SUCCESS_PIN_CREATED = "PIN created successfully!";
    public static final String SUCCESS_SERVICE_STARTED = "Location tracking started successfully.";
    public static final String SUCCESS_SERVICE_STOPPED = "Location tracking stopped.";

    public static final int TIME_MULTIPLIER = 1;
    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
