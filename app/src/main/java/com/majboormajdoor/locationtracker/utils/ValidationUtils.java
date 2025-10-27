package com.majboormajdoor.locationtracker.utils;
import com.majboormajdoor.locationtracker.constants.AppConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for input validation
 * Centralizes all validation logic for better maintainability
 */
public class ValidationUtils {

    /**
     * Validate PIN format
     */
    public static boolean isValidPin(String pin) {
        return pin != null &&
               pin.length() == AppConstants.PIN_LENGTH &&
               pin.matches("\\d{" + AppConstants.PIN_LENGTH + "}");
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove all non-digit characters for validation
        String cleanPhone = phoneNumber.replaceAll("[^\\d]", "");

        // Check if it's a valid length (10-15 digits typically)
        return cleanPhone.length() >= 10 && cleanPhone.length() <= 15;
    }

    /**
     * Validate email address format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Basic email validation pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Validate time interval
     */
    public static boolean isValidTimeInterval(int intervalMinutes) {
        return intervalMinutes >= AppConstants.MIN_TIME_INTERVAL_MINUTES &&
               intervalMinutes <= AppConstants.MAX_TIME_INTERVAL_MINUTES;
    }

    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        // Remove all non-digit characters
        String cleanPhone = phoneNumber.replaceAll("[^\\d]", "");

        // Format as (XXX) XXX-XXXX if it's a 10-digit US number
        if (cleanPhone.length() == 10) {
            return String.format("(%s) %s-%s",
                cleanPhone.substring(0, 3),
                cleanPhone.substring(3, 6),
                cleanPhone.substring(6));
        }

        // For international numbers, just add spaces every 3-4 digits
        if (cleanPhone.length() > 10) {
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < cleanPhone.length(); i++) {
                if (i > 0 && i % 3 == 0) {
                    formatted.append(" ");
                }
                formatted.append(cleanPhone.charAt(i));
            }
            return formatted.toString();
        }

        return cleanPhone;
    }

    /**
     * Clean phone number for SMS sending (remove formatting)
     */
    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^\\d+]", "");
    }

    public static String generateISO8601BasicFormat(){
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd'T'HHmmss'Z'", Locale.US);
        return sdf.format(now);
    }
}
