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
import com.majboormajdoor.locationtracker.utils.PreferenceManager;

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
    private Handler apiHandler;
    private Runnable apiRunnable;
    private ApiService apiService;

    private String targetPhoneNumber;
    private int timeIntervalMinutes;
    private android.location.Location lastKnownLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiHandler = new Handler(Looper.getMainLooper());
        apiService = new ApiService();

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
        scheduleLocationAPI();

        Log.d(TAG, "Location tracking started, will send to API every " + timeIntervalMinutes + " minutes");
    }

    /**
     * Stop location tracking and API scheduling
     */
    private void stopLocationTracking() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (apiHandler != null && apiRunnable != null) {
            apiHandler.removeCallbacks(apiRunnable);
        }

        Log.d(TAG, "Location tracking stopped");
    }

    /**
     * Schedule periodic API calls to send location data
     */
    private void scheduleLocationAPI() {
        apiRunnable = new Runnable() {
            @Override
            public void run() {
                sendLocationToAPI();
                // Schedule next API call
                apiHandler.postDelayed(this, timeIntervalMinutes * 60 * 1000L);
            }
        };

        // Send first location data immediately
        apiHandler.post(apiRunnable);
    }

    /**
     * Send location data to API endpoint
     */
    private void sendLocationToAPI() {
        if (lastKnownLocation == null) {
            Log.w(TAG, "No location available for API call");
            return;
        }

        // Run API call on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            try {
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Create Location DTO object
                com.majboormajdoor.locationtracker.dto.Location locationData = new com.majboormajdoor.locationtracker.dto.Location();
                locationData.setLatitude(latitude);
                locationData.setLongitude(longitude);
                locationData.setTimestamp(timestamp);
                locationData.setEmail(PreferenceManager.getInstance(getApplicationContext()).getEmailAddress());

                // Send location data to API
                apiService.postLocation(locationData, new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Location sent to API successfully: " + latitude + ", " + longitude);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to send location to API: " + error);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing location data for API: " + e.getMessage(), e);
            }
        }).start();
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
