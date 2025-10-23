package com.majboormajdoor.locationtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;

/**
 * Main activity that handles PIN-based parental lock
 * Shows PIN creation on first launch, PIN validation on subsequent launches
 */
public class PinLockActivity extends AppCompatActivity {

    private TextInputEditText etPin, etConfirmPin;
    private TextInputLayout layoutConfirmPin;
    private Button btnSubmit;
    private TextView tvTitle, tvSubtitle;

    private PreferenceManager preferenceManager;
    private boolean isFirstLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        initializeViews();
        initializePreferences();
        setupUI();
        setupClickListeners();

        // Setup modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Disable back button to prevent bypassing PIN
                if (!isFirstLaunch) {
                    // Move app to background instead of closing
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
        etPin = findViewById(R.id.et_pin);
        etConfirmPin = findViewById(R.id.et_confirm_pin);
        layoutConfirmPin = (TextInputLayout) etConfirmPin.getParent().getParent();
        btnSubmit = findViewById(R.id.btn_submit);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
    }

    /**
     * Initialize preference manager and check first launch
     */
    private void initializePreferences() {
        preferenceManager = PreferenceManager.getInstance(this);
        isFirstLaunch = preferenceManager.isFirstLaunch();
    }

    /**
     * Setup UI based on first launch or PIN validation
     */
    private void setupUI() {
        if (isFirstLaunch) {
            // First launch - PIN creation mode
            tvTitle.setText("Create PIN");
            tvSubtitle.setText("Create a 4-digit PIN to secure the app");
            layoutConfirmPin.setVisibility(View.VISIBLE);
            btnSubmit.setText("Create PIN");
        } else {
            // PIN validation mode
            tvTitle.setText("Enter PIN");
            tvSubtitle.setText("Enter your 4-digit PIN to continue");
            layoutConfirmPin.setVisibility(View.GONE);
            btnSubmit.setText("Unlock");
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> {
            if (isFirstLaunch) {
                handlePinCreation();
            } else {
                handlePinValidation();
            }
        });

        // Auto-submit when PIN length is reached (for validation mode)
        etPin.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isFirstLaunch && s.length() == AppConstants.PIN_LENGTH) {
                    // Delay slightly for better UX
                    new Handler().postDelayed(() -> handlePinValidation(), AppConstants.PIN_INPUT_DELAY_MS);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Handle PIN creation (first launch)
     */
    private void handlePinCreation() {
        String pin = etPin.getText().toString().trim();
        String confirmPin = etConfirmPin.getText().toString().trim();

        // Validate PIN format
        if (!ValidationUtils.isValidPin(pin)) {
            showError("Please enter a valid 4-digit PIN");
            return;
        }

        // Check if PINs match
        if (!pin.equals(confirmPin)) {
            showError(AppConstants.ERROR_PIN_MISMATCH);
            etConfirmPin.setText("");
            etConfirmPin.requestFocus();
            return;
        }

        // Save PIN and mark first launch as completed
        preferenceManager.savePin(pin);
        preferenceManager.setFirstLaunchCompleted();

        showSuccess(AppConstants.SUCCESS_PIN_CREATED);

        // Navigate to main activity
        navigateToMainActivity();
    }

    /**
     * Handle PIN validation
     */
    private void handlePinValidation() {
        String enteredPin = etPin.getText().toString().trim();

        if (!ValidationUtils.isValidPin(enteredPin)) {
            showError("Please enter a 4-digit PIN");
            return;
        }

        String savedPin = preferenceManager.getPin();

        if (enteredPin.equals(savedPin)) {
            // PIN is correct
            navigateToMainActivity();
        } else {
            // PIN is incorrect
            showError(AppConstants.ERROR_INVALID_PIN);
            etPin.setText("");
            etPin.requestFocus();

            // Add slight vibration for better UX (if permission available)
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(200);
                }
            } catch (Exception ignored) {
                // Vibration not available or permission denied
            }
        }
    }

    /**
     * Navigate to main activity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();

        // Add smooth transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
