package com.majboormajdoor.locationtracker.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import com.majboormajdoor.locationtracker.services.LocationTrackingService;
import com.majboormajdoor.locationtracker.utils.PermissionUtils;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;

/**
 * Main activity for configuring location tracking
 * Allows users to set phone number, time interval, and start/stop tracking
 */
public class MainActivity extends AppCompatActivity {

    private TextInputEditText etPhoneNumber;
    private SeekBar seekBarInterval;
    private TextView tvIntervalValue, tvIntervalLabel;
    private Button btnStartTracking, btnStopTracking;
    private CardView cardPhoneConfig, cardIntervalConfig, cardControls;

    private PreferenceManager preferenceManager;
    private boolean isTrackingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializePreferences();
        setupSeekBar();
        setupClickListeners();
        loadSavedData();
        updateUI();

        // Request permissions if not already granted
        checkAndRequestPermissions();

        // Setup modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTrackingActive) {
                    // Move app to background instead of closing when tracking is active
                    moveTaskToBack(true);
                } else {
                    finish();
                }
            }
        });
    }

    /**
     * Initialize all views
     */
    private void initializeViews() {
        etPhoneNumber = findViewById(R.id.et_phone_number);
        seekBarInterval = findViewById(R.id.seekbar_interval);
        tvIntervalValue = findViewById(R.id.tv_interval_value);
        tvIntervalLabel = findViewById(R.id.tv_interval_label);
        btnStartTracking = findViewById(R.id.btn_start_tracking);
        btnStopTracking = findViewById(R.id.btn_stop_tracking);
        cardPhoneConfig = findViewById(R.id.card_phone_config);
        cardIntervalConfig = findViewById(R.id.card_interval_config);
        cardControls = findViewById(R.id.card_controls);
    }

    /**
     * Initialize preference manager
     */
    private void initializePreferences() {
        preferenceManager = PreferenceManager.getInstance(this);
    }

    /**
     * Setup time interval seekbar
     */
    private void setupSeekBar() {
        seekBarInterval.setMax(AppConstants.MAX_TIME_INTERVAL_MINUTES - AppConstants.MIN_TIME_INTERVAL_MINUTES);
        seekBarInterval.setProgress(AppConstants.DEFAULT_TIME_INTERVAL_MINUTES - AppConstants.MIN_TIME_INTERVAL_MINUTES);

        seekBarInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + progress;
                updateIntervalDisplay(intervalMinutes);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save the selected interval
                int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + seekBar.getProgress();
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

        // Save phone number when focus is lost
        etPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                if (!phoneNumber.isEmpty() && ValidationUtils.isValidPhoneNumber(phoneNumber)) {
                    preferenceManager.savePhoneNumber(ValidationUtils.cleanPhoneNumber(phoneNumber));
                }
            }
        });
    }

    /**
     * Load saved data from preferences
     */
    private void loadSavedData() {
        // Load saved phone number
        String savedPhone = preferenceManager.getPhoneNumber();
        if (!savedPhone.isEmpty()) {
            etPhoneNumber.setText(ValidationUtils.formatPhoneNumber(savedPhone));
        }

        // Load saved time interval
        int savedInterval = preferenceManager.getTimeInterval();
        seekBarInterval.setProgress(savedInterval - AppConstants.MIN_TIME_INTERVAL_MINUTES);
        updateIntervalDisplay(savedInterval);
    }

    /**
     * Update interval display text
     */
    private void updateIntervalDisplay(int intervalMinutes) {
        tvIntervalValue.setText(String.valueOf(intervalMinutes));

        String label = intervalMinutes == 1 ? "minute" : "minutes";
        tvIntervalLabel.setText("Send location every " + intervalMinutes + " " + label);
    }

    /**
     * Start location tracking service
     */
    private void startLocationTracking() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        // Validate phone number
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            showError(AppConstants.ERROR_INVALID_PHONE);
            etPhoneNumber.requestFocus();
            return;
        }

        // Check permissions
        if (!PermissionUtils.areAllPermissionsGranted(this)) {
            showError("Please grant all required permissions to start tracking");
            checkAndRequestPermissions();
            return;
        }

        // Save phone number
        String cleanPhone = ValidationUtils.cleanPhoneNumber(phoneNumber);
        preferenceManager.savePhoneNumber(cleanPhone);

        // Get time interval
        int intervalMinutes = AppConstants.MIN_TIME_INTERVAL_MINUTES + seekBarInterval.getProgress();

        // Start the service
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        serviceIntent.setAction(AppConstants.SERVICE_ACTION_START);
        serviceIntent.putExtra(AppConstants.EXTRA_PHONE_NUMBER, cleanPhone);
        serviceIntent.putExtra(AppConstants.EXTRA_TIME_INTERVAL, intervalMinutes);

        // Use API level compatible service start
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isTrackingActive = true;
        updateUI();
        showSuccess(AppConstants.SUCCESS_SERVICE_STARTED);
    }

    /**
     * Stop location tracking service
     */
    private void stopLocationTracking() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        serviceIntent.setAction(AppConstants.SERVICE_ACTION_STOP);
        startService(serviceIntent);

        isTrackingActive = false;
        updateUI();
        showSuccess(AppConstants.SUCCESS_SERVICE_STOPPED);
    }

    /**
     * Update UI based on tracking state
     */
    private void updateUI() {
        if (isTrackingActive) {
            btnStartTracking.setEnabled(false);
            btnStartTracking.setAlpha(0.5f);
            btnStopTracking.setEnabled(true);
            btnStopTracking.setAlpha(1.0f);

            // Disable configuration changes while tracking
            etPhoneNumber.setEnabled(false);
            seekBarInterval.setEnabled(false);

        } else {
            btnStartTracking.setEnabled(true);
            btnStartTracking.setAlpha(1.0f);
            btnStopTracking.setEnabled(false);
            btnStopTracking.setAlpha(0.5f);

            // Enable configuration changes
            etPhoneNumber.setEnabled(true);
            seekBarInterval.setEnabled(true);
        }
    }

    /**
     * Check and request required permissions
     */
    private void checkAndRequestPermissions() {
        if (!PermissionUtils.isLocationPermissionGranted(this)) {
            PermissionUtils.requestLocationPermissions(this);
            return;
        }

        if (!PermissionUtils.isSmsPermissionGranted(this)) {
            PermissionUtils.requestSmsPermission(this);
            return;
        }

        if (!PermissionUtils.isNotificationPermissionGranted(this)) {
            PermissionUtils.requestNotificationPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        switch (requestCode) {
            case AppConstants.PERMISSION_REQUEST_LOCATION:
                if (allGranted) {
                    showSuccess("Location permission granted");
                    checkAndRequestPermissions(); // Check for other permissions
                } else {
                    showError(AppConstants.ERROR_LOCATION_PERMISSION);
                }
                break;

            case AppConstants.PERMISSION_REQUEST_SMS:
                if (allGranted) {
                    showSuccess("SMS permission granted");
                    checkAndRequestPermissions(); // Check for other permissions
                } else {
                    showError(AppConstants.ERROR_SMS_PERMISSION);
                }
                break;

            case AppConstants.PERMISSION_REQUEST_NOTIFICATION:
                if (allGranted) {
                    showSuccess("Notification permission granted");
                } else {
                    showError("Notification permission denied. Service notifications may not work properly.");
                }
                break;
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if service is running and update UI accordingly
        // This is a simplified check - in production you might want to bind to the service
        updateUI();
    }
}
