package com.majboormajdoor.locationtracker.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.majboormajdoor.locationtracker.constants.AppConstants;

/**
 * Utility class for handling runtime permissions
 * Centralizes permission management logic
 */
public class PermissionUtils {

    /**
     * Check if location permissions are granted
     */
    public static boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if SMS permission is granted
     */
    public static boolean isSmsPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if notification permission is granted (for Android 13+)
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required for older versions
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(activity, permissions, AppConstants.PERMISSION_REQUEST_LOCATION);
    }

    /**
     * Request SMS permission
     */
    public static void requestSmsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.SEND_SMS},
            AppConstants.PERMISSION_REQUEST_SMS);
    }

    /**
     * Request notification permission (Android 13+)
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                AppConstants.PERMISSION_REQUEST_NOTIFICATION);
        }
    }

    /**
     * Check if all required permissions are granted
     */
    public static boolean areAllPermissionsGranted(Context context) {
        return isLocationPermissionGranted(context) &&
               isSmsPermissionGranted(context) &&
               isNotificationPermissionGranted(context);
    }
}
