package com.majboormajdoor.locationtracker.fragments;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import com.majboormajdoor.locationtracker.services.ApiService;
import com.majboormajdoor.locationtracker.services.LocationTrackingService;
import com.majboormajdoor.locationtracker.utils.PermissionUtils;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for home screen with location tracking configuration
 */
public class HomeFragment extends Fragment {

    private TextInputEditText trackieName;
    private SeekBar seekBarInterval;
    private TextView tvIntervalValue, tvIntervalLabel;
    private Button btnStartTracking, btnStopTracking, btnTestLocation;

    private PreferenceManager preferenceManager;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        initializePreferences();
        setupSeekBar();
        setupClickListeners();
        loadSavedData();
        updateUI();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Check and request permissions if not already granted
        checkAndRequestPermissions();
    }

    /**
     * Initialize all views
     */
    private void initializeViews(View view) {
        trackieName = view.findViewById(R.id.et_email_address);
        seekBarInterval = view.findViewById(R.id.seekbar_interval);
        tvIntervalValue = view.findViewById(R.id.tv_interval_value);
        tvIntervalLabel = view.findViewById(R.id.tv_interval_label);
        btnStartTracking = view.findViewById(R.id.btn_start_tracking);
        btnStopTracking = view.findViewById(R.id.btn_stop_tracking);
        btnTestLocation = view.findViewById(R.id.btn_test_location);
    }

    /**
     * Initialize preference manager and services
     */
    private void initializePreferences() {
        preferenceManager = PreferenceManager.getInstance(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        apiService = new ApiService(getContext());
    }

    /**
     * Setup time interval seekbar
     */
    private void setupSeekBar() {
        // Set up seekbar for minutes (10-minute increments from 10 to 720 minutes)
        int maxProgress = (AppConstants.MAX_TIME_INTERVAL_MINUTES - AppConstants.MIN_TIME_INTERVAL_MINUTES) / 10;
        seekBarInterval.setMax(maxProgress);

        int defaultProgress = (AppConstants.DEFAULT_TIME_INTERVAL_MINUTES - AppConstants.MIN_TIME_INTERVAL_MINUTES) / 10;
        seekBarInterval.setProgress(defaultProgress);

        seekBarInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + (progress * 10);
                updateIntervalDisplay(intervalMinutes);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save the selected interval in minutes
                int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + (seekBar.getProgress() * 10);
                preferenceManager.saveTimeInterval(intervalMinutes);
            }
        });
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        btnStartTracking.setOnClickListener(v -> startLocationTracking());
        btnStopTracking.setOnClickListener(v -> stopLocationTracking());
        btnTestLocation.setOnClickListener(v -> testLocationSending());

        // Save email address when focus is lost
        trackieName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && trackieName.getText() != null) {
                String trackieName = this.trackieName.getText().toString().trim();
                if (!trackieName.isEmpty() && ValidationUtils.isValidName(trackieName)) {
                    preferenceManager.saveTrackieName(trackieName);
                }
            }
        });
    }

    /**
     * Load saved data from preferences
     */
    private void loadSavedData() {
        // Load saved email address
        String savedEmail = preferenceManager.getTrackieName();
        if (!savedEmail.isEmpty()) {
            trackieName.setText(savedEmail);
        }

        // Load saved time interval (stored in minutes)
        int savedIntervalMinutes = preferenceManager.getTimeInterval();

        // Ensure the value is within our new minute range and is in 10-minute increments
        if (savedIntervalMinutes < AppConstants.MIN_TIME_INTERVAL_MINUTES) {
            savedIntervalMinutes = AppConstants.DEFAULT_TIME_INTERVAL_MINUTES;
        } else if (savedIntervalMinutes > AppConstants.MAX_TIME_INTERVAL_MINUTES) {
            savedIntervalMinutes = AppConstants.MAX_TIME_INTERVAL_MINUTES;
        }

        // Round to nearest 10-minute increment
        savedIntervalMinutes = ((savedIntervalMinutes + 5) / 10) * 10;

        // Set seekbar progress based on 10-minute increments
        int progress = (savedIntervalMinutes - AppConstants.MIN_TIME_INTERVAL_MINUTES) / 10;
        seekBarInterval.setProgress(progress);
        updateIntervalDisplay(savedIntervalMinutes);
    }

    /**
     * Update interval display text
     */
    private void updateIntervalDisplay(int intervalMinutes) {
        String displayText;
        String labelText;

        if (intervalMinutes < 60) {
            // Display in minutes
            tvIntervalValue.setText(String.valueOf(intervalMinutes));
            String label = intervalMinutes == 1 ? "minute" : "minutes";
            displayText = String.valueOf(intervalMinutes);
            labelText = String.format(java.util.Locale.getDefault(), "Send location every %d %s", intervalMinutes, label);
        } else {
            // Display in hours if 60 minutes or more
            int hours = intervalMinutes / 60;
            int remainingMinutes = intervalMinutes % 60;

            if (remainingMinutes == 0) {
                // Exact hours
                tvIntervalValue.setText(String.valueOf(hours));
                String label = hours == 1 ? "hour" : "hours";
                labelText = String.format(java.util.Locale.getDefault(), "Send location every %d %s", hours, label);
            } else {
                // Hours and minutes
                tvIntervalValue.setText(String.format(java.util.Locale.getDefault(), "%dh %dm", hours, remainingMinutes));
                labelText = String.format(java.util.Locale.getDefault(), "Send location every %d hours %d minutes", hours, remainingMinutes);
            }
        }

        tvIntervalLabel.setText(labelText);
    }

    /**
     * Start location tracking service
     */
    private void startLocationTracking() {
        if (trackieName.getText() == null) {
            showError("Please enter an email address");
            return;
        }

        String trackieName = this.trackieName.getText().toString().trim();

        // Validate email address
        if (!ValidationUtils.isValidName(trackieName)) {
            showError("Please enter a valid email address");
            this.trackieName.requestFocus();
            return;
        }

        // Check permissions
        if (!PermissionUtils.areAllPermissionsGranted(requireContext())) {
            showError("Please grant all required permissions to start tracking");
            checkAndRequestPermissions();
            return;
        }

        // Save email address
        preferenceManager.saveTrackieName(trackieName);

        // Get time interval in minutes (10-minute increments)
        int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + (seekBarInterval.getProgress() * 10);

        // Start the service
        Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
        serviceIntent.setAction(AppConstants.SERVICE_ACTION_START);
        serviceIntent.putExtra(AppConstants.EXTRA_EMAIL_ADDRESS, trackieName);
        serviceIntent.putExtra(AppConstants.EXTRA_TIME_INTERVAL, intervalMinutes);

        // Use API level compatible service start
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        PreferenceManager.getInstance(getContext()).setTrackingStatus(true);
        updateUI();
        showSuccess(AppConstants.SUCCESS_SERVICE_STARTED);
    }

    /**
     * Stop location tracking service
     */
    private void stopLocationTracking() {
        Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
        serviceIntent.setAction(AppConstants.SERVICE_ACTION_STOP);
        requireContext().startService(serviceIntent);
        PreferenceManager.getInstance(getContext()).setTrackingStatus(false);
        updateUI();
        showSuccess(AppConstants.SUCCESS_SERVICE_STOPPED);
    }

    /**
     * Update UI based on tracking state
     */
    private void updateUI() {
        if (PreferenceManager.getInstance(getContext()).getTrackingStatus()) {
            btnStartTracking.setEnabled(false);
            btnStartTracking.setAlpha(0.5f);
            btnStopTracking.setEnabled(true);
            btnStopTracking.setAlpha(1.0f);

            // Disable configuration changes while tracking
            trackieName.setEnabled(false);
            seekBarInterval.setEnabled(false);

        } else {
            btnStartTracking.setEnabled(true);
            btnStartTracking.setAlpha(1.0f);
            btnStopTracking.setEnabled(false);
            btnStopTracking.setAlpha(0.5f);

            // Enable configuration changes
            trackieName.setEnabled(true);
            seekBarInterval.setEnabled(true);
        }
    }

    /**
     * Check and request required permissions
     */
    private void checkAndRequestPermissions() {
        if (!PermissionUtils.isLocationPermissionGranted(requireContext())) {
            PermissionUtils.requestLocationPermissions(requireActivity());
            return;
        }

        if (!PermissionUtils.isNotificationPermissionGranted(requireContext())) {
            PermissionUtils.requestNotificationPermission(requireActivity());
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Test location sending to API
     */
    private void testLocationSending() {
        if (trackieName.getText() == null) {
            showError("Please enter name");
            return;
        }

        String trackieName = this.trackieName.getText().toString().trim();

        // Validate email address
        if (!ValidationUtils.isValidName(trackieName)) {
            showError("Please enter a valid trackie name");
            this.trackieName.requestFocus();
            return;
        }

        // Check permissions
        if (!PermissionUtils.isLocationPermissionGranted(requireContext())) {
            showError("Location permission is required to test location sending");
            checkAndRequestPermissions();
            return;
        }

        // Show loading message
        showSuccess("Getting current location...");

        // Get current location
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showError("Location permissions not granted");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        sendTestLocationToAPI(location, trackieName);
                    } else {
                        showError("Unable to get current location. Please try again.");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Failed to get location: " + e.getMessage());
                });
    }

    /**
     * Send test location data to API
     */
    private void sendTestLocationToAPI(Location location, String emailAddress) {
        try {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Create Location DTO object
            com.majboormajdoor.locationtracker.dto.Location locationData = new com.majboormajdoor.locationtracker.dto.Location();
            locationData.setLatitude(latitude);
            locationData.setLongitude(longitude);
            locationData.setInsertionTimestamp(timestamp);
            locationData.setUserName(emailAddress); // Using email address in phone number field

            // Send test location data to API
            apiService.postLocation(locationData, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(String message) {
                    requireActivity().runOnUiThread(() ->
                        showSuccess("Test location sent successfully!\nLat: " + String.format(Locale.getDefault(), "%.6f", latitude) +
                                   ", Lng: " + String.format(Locale.getDefault(), "%.6f", longitude))
                    );
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() ->
                        showError("Failed to send test location: " + error)
                    );
                }
            });

        } catch (Exception e) {
            showError("Error preparing test location data: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if service is running and update UI accordingly
        updateUI();
    }
}
