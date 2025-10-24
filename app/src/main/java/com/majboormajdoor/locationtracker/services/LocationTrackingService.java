package com.majboormajdoor.locationtracker.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.activities.MainActivity;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Background service for location tracking and SMS sending
 * Runs as a foreground service to ensure continuous operation
 */
public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Handler smsHandler;
    private Runnable smsRunnable;

    private String targetPhoneNumber;
    private int timeIntervalMinutes;
    private Location lastKnownLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        smsHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        setupLocationRequest();
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (AppConstants.SERVICE_ACTION_START.equals(action)) {
                targetPhoneNumber = intent.getStringExtra(AppConstants.EXTRA_PHONE_NUMBER);
                timeIntervalMinutes = intent.getIntExtra(AppConstants.EXTRA_TIME_INTERVAL,
                    AppConstants.DEFAULT_TIME_INTERVAL_MINUTES);

                startLocationTracking();
                startForeground(AppConstants.LOCATION_SERVICE_NOTIFICATION_ID, createNotification());

            } else if (AppConstants.SERVICE_ACTION_STOP.equals(action)) {
                stopLocationTracking();
                stopForeground(true);
                stopSelf();
            }
        }

        return START_STICKY; // Restart service if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopLocationTracking();
    }

    /**
     * Setup location request parameters
     */
    private void setupLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,
                AppConstants.LOCATION_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(AppConstants.FASTEST_LOCATION_INTERVAL)
                .setMaxUpdateDelayMillis(AppConstants.LOCATION_UPDATE_INTERVAL * 2)
                .build();
    }

    /**
     * Setup location callback to handle location updates
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    lastKnownLocation = location;
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        };
    }

    /**
     * Start location tracking and SMS scheduling
     */
    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        scheduleLocationSMS();

        Log.d(TAG, "Location tracking started for number: " + targetPhoneNumber +
              " with interval: " + timeIntervalMinutes + " minutes");
    }

    /**
     * Stop location tracking and SMS scheduling
     */
    private void stopLocationTracking() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (smsHandler != null && smsRunnable != null) {
            smsHandler.removeCallbacks(smsRunnable);
        }

        Log.d(TAG, "Location tracking stopped");
    }

    /**
     * Schedule periodic SMS sending
     */
    private void scheduleLocationSMS() {
        smsRunnable = new Runnable() {
            @Override
            public void run() {
                sendLocationSMS();
                // Schedule next SMS
                smsHandler.postDelayed(this, timeIntervalMinutes * 60 * 1000L);
            }
        };

        // Send first SMS immediately
        smsHandler.post(smsRunnable);
    }

    /**
     * Send SMS with current location
     */
    private void sendLocationSMS() {
        if (lastKnownLocation == null) {
            Log.w(TAG, "No location available for SMS");
            return;
        }

        if (targetPhoneNumber == null || targetPhoneNumber.isEmpty()) {
            Log.e(TAG, "No target phone number set");
            return;
        }

        try {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            String message = String.format(Locale.getDefault(), AppConstants.SMS_MESSAGE_TEMPLATE,
                latitude, longitude, timestamp);

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(targetPhoneNumber, null, message, null, null);

            Log.d(TAG, "SMS sent to " + targetPhoneNumber + " with location: " + latitude + ", " + longitude);

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                AppConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Location tracking service notifications");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Create foreground service notification
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Tracker")
            .setContentText("Tracking location and sending updates every " + timeIntervalMinutes + " minutes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
}
