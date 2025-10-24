package com.majboormajdoor.locationtracker.fragments;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import com.majboormajdoor.locationtracker.services.LocationTrackingService;
import com.majboormajdoor.locationtracker.utils.PermissionUtils;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;

/**
 * Fragment for home screen with location tracking configuration
 */
public class HomeFragment extends Fragment {

    private TextInputEditText etPhoneNumber;
    private SeekBar seekBarInterval;
    private TextView tvIntervalValue, tvIntervalLabel;
    private Button btnStartTracking, btnStopTracking;

    private PreferenceManager preferenceManager;
    private boolean isTrackingActive = false;

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
        etPhoneNumber = view.findViewById(R.id.et_phone_number);
        seekBarInterval = view.findViewById(R.id.seekbar_interval);
        tvIntervalValue = view.findViewById(R.id.tv_interval_value);
        tvIntervalLabel = view.findViewById(R.id.tv_interval_label);
        btnStartTracking = view.findViewById(R.id.btn_start_tracking);
        btnStopTracking = view.findViewById(R.id.btn_stop_tracking);
    }

    /**
     * Initialize preference manager
     */
    private void initializePreferences() {
        preferenceManager = PreferenceManager.getInstance(requireContext());
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
            if (!hasFocus && etPhoneNumber.getText() != null) {
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
        String formattedText = String.format("Send location every %d %s", intervalMinutes, label);
        tvIntervalLabel.setText(formattedText);
    }

    /**
     * Start location tracking service
     */
    private void startLocationTracking() {
        if (etPhoneNumber.getText() == null) {
            showError("Please enter a phone number");
            return;
        }

        String phoneNumber = etPhoneNumber.getText().toString().trim();

        // Validate phone number
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            showError(AppConstants.ERROR_INVALID_PHONE);
            etPhoneNumber.requestFocus();
            return;
        }

        // Check permissions
        if (!PermissionUtils.areAllPermissionsGranted(requireContext())) {
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
        Intent serviceIntent = new Intent(requireContext(), LocationTrackingService.class);
        serviceIntent.setAction(AppConstants.SERVICE_ACTION_START);
        serviceIntent.putExtra(AppConstants.EXTRA_PHONE_NUMBER, cleanPhone);
        serviceIntent.putExtra(AppConstants.EXTRA_TIME_INTERVAL, intervalMinutes);

        // Use API level compatible service start
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }

        isTrackingActive = true;
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
        if (!PermissionUtils.isLocationPermissionGranted(requireContext())) {
            PermissionUtils.requestLocationPermissions(requireActivity());
            return;
        }

        if (!PermissionUtils.isSmsPermissionGranted(requireContext())) {
            PermissionUtils.requestSmsPermission(requireActivity());
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

    @Override
    public void onResume() {
        super.onResume();
        // Check if service is running and update UI accordingly
        updateUI();
    }
}
