package com.majboormajdoor.locationtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.services.CognitoAuthService;
import com.majboormajdoor.locationtracker.services.GoogleSignInService;

/**
 * Authentication activity that handles user sign-in and sign-up
 * Supports AWS Cognito authentication and Google Sign-In
 */
public class PinLockActivity extends AppCompatActivity {

    private static final String TAG = "AuthenticationActivity";

    // UI Components
    private TextView tvWelcomeTitle, tvWelcomeSubtitle;
    private TextInputEditText etEmail, etPassword, etConfirmPassword, etVerificationCode;
    private TextInputLayout layoutEmail, layoutPassword, layoutConfirmPassword, layoutVerificationCode;
    private Button btnPrimaryAction, btnResendCode, btnGoogleSignIn;
    private TextView tvForgotPassword, tvSwitchModePrompt, tvSwitchMode;

    // Authentication Services
    private CognitoAuthService cognitoAuthService;
    private GoogleSignInService googleSignInService;

    // State Management
    private enum AuthMode {
        SIGN_IN,
        SIGN_UP,
        EMAIL_VERIFICATION,
        FORGOT_PASSWORD,
        RESET_PASSWORD
    }

    private AuthMode currentMode = AuthMode.SIGN_IN;
    private String pendingUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        initializeViews();
        initializeServices();
        setupClickListeners();
        setupBackPressHandling();

        // Check if user is already signed in
        checkExistingAuth();
    }

    /**
     * Initialize all views
     */
    private void initializeViews() {
        tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etVerificationCode = findViewById(R.id.et_verification_code);

        layoutEmail = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        layoutVerificationCode = findViewById(R.id.layout_verification_code);

        btnPrimaryAction = findViewById(R.id.btn_primary_action);
        btnResendCode = findViewById(R.id.btn_resend_code);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);

        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSwitchModePrompt = findViewById(R.id.tv_switch_mode_prompt);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);

        // Set initial UI state
        updateUIForMode(AuthMode.SIGN_IN);
    }

    /**
     * Initialize authentication services
     */
    private void initializeServices() {
        cognitoAuthService = CognitoAuthService.getInstance();
        googleSignInService = GoogleSignInService.getInstance();

        // Initialize Cognito
        cognitoAuthService.initialize(this, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Cognito initialized: " + message);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Cognito initialization failed: " + error);
                showError("Authentication service initialization failed");
            }

            @Override
            public void onConfirmationRequired(String username) {
                // Not used for initialization
            }
        });

        // Initialize Google Sign-In
        googleSignInService.initialize(this);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnPrimaryAction.setOnClickListener(v -> handlePrimaryAction());
        btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
        btnResendCode.setOnClickListener(v -> handleResendCode());
        tvForgotPassword.setOnClickListener(v -> switchToMode(AuthMode.FORGOT_PASSWORD));
        tvSwitchMode.setOnClickListener(v -> handleSwitchMode());

        // Add text watchers for validation
        setupTextWatchers();
    }

    /**
     * Setup text watchers for real-time validation
     */
    private void setupTextWatchers() {
        TextWatcher emailWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutEmail.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layoutPassword.setError(null);
                layoutConfirmPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etEmail.addTextChangedListener(emailWatcher);
        etPassword.addTextChangedListener(passwordWatcher);
        etConfirmPassword.addTextChangedListener(passwordWatcher);
    }

    /**
     * Handle primary action based on current mode
     */
    private void handlePrimaryAction() {
        switch (currentMode) {
            case SIGN_IN:
                handleSignIn();
                break;
            case SIGN_UP:
                handleSignUp();
                break;
            case EMAIL_VERIFICATION:
                handleEmailVerification();
                break;
            case FORGOT_PASSWORD:
                handleForgotPassword();
                break;
            case RESET_PASSWORD:
                handleResetPassword();
                break;
        }
    }

    /**
     * Handle sign in
     */
    private void handleSignIn() {
        if (!validateSignInInputs()) return;

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        showLoading(true);

        cognitoAuthService.signIn(email, password, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showSuccess(message);
                navigateToMainActivity();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }

            @Override
            public void onConfirmationRequired(String username) {
                showLoading(false);
                pendingUsername = username;
                switchToMode(AuthMode.EMAIL_VERIFICATION);
            }
        });
    }

    /**
     * Handle sign up
     */
    private void handleSignUp() {
        if (!validateSignUpInputs()) return;

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        showLoading(true);

        cognitoAuthService.signUp(email, email, password, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showSuccess(message);
                pendingUsername = email;
                switchToMode(AuthMode.EMAIL_VERIFICATION);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }

            @Override
            public void onConfirmationRequired(String username) {
                showLoading(false);
                pendingUsername = username;
                switchToMode(AuthMode.EMAIL_VERIFICATION);
            }
        });
    }

    /**
     * Handle email verification
     */
    private void handleEmailVerification() {
        String code = etVerificationCode.getText() != null ?
            etVerificationCode.getText().toString().trim() : "";

        if (code.isEmpty()) {
            layoutVerificationCode.setError("Please enter the verification code");
            return;
        }

        showLoading(true);

        cognitoAuthService.confirmSignUp(pendingUsername, code, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showSuccess(message);
                switchToMode(AuthMode.SIGN_IN);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
                layoutVerificationCode.setError("Invalid verification code");
            }

            @Override
            public void onConfirmationRequired(String username) {
                // Should not reach here
            }
        });
    }

    /**
     * Handle forgot password
     */
    private void handleForgotPassword() {
        if (!validateEmailInput()) return;

        String email = etEmail.getText().toString().trim();
        showLoading(true);

        cognitoAuthService.resetPassword(email, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showSuccess(message);
                pendingUsername = email;
                switchToMode(AuthMode.RESET_PASSWORD);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }

            @Override
            public void onConfirmationRequired(String username) {
                // Not used for password reset
            }
        });
    }

    /**
     * Handle password reset
     */
    private void handleResetPassword() {
        if (!validateResetPasswordInputs()) return;

        String newPassword = etPassword.getText().toString().trim();
        String code = etVerificationCode.getText().toString().trim();

        showLoading(true);

        cognitoAuthService.confirmResetPassword(pendingUsername, newPassword, code,
            new CognitoAuthService.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    showLoading(false);
                    showSuccess(message);
                    switchToMode(AuthMode.SIGN_IN);
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    showError(error);
                }

                @Override
                public void onConfirmationRequired(String username) {
                    // Should not reach here
                }
            });
    }

    /**
     * Handle Google Sign-In
     */
    private void handleGoogleSignIn() {
        showLoading(true);

        googleSignInService.signIn(this, new GoogleSignInService.GoogleSignInCallback() {
            @Override
            public void onSuccess(String idToken, String email, String displayName) {
                showLoading(false);
                showSuccess("Signed in with Google successfully!");
                navigateToMainActivity();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError("Google Sign-In failed: " + error);
            }

            @Override
            public void onCancelled() {
                showLoading(false);
                showMessage("Google Sign-In cancelled");
            }
        });
    }

    /**
     * Handle resend verification code
     */
    private void handleResendCode() {
        if (pendingUsername.isEmpty()) {
            showError("No pending verification");
            return;
        }

        showLoading(true);

        cognitoAuthService.resendSignUpCode(pendingUsername, new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showSuccess(message);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showError(error);
            }

            @Override
            public void onConfirmationRequired(String username) {
                // Not used for resend
            }
        });
    }

    /**
     * Handle switching between sign in and sign up modes
     */
    private void handleSwitchMode() {
        if (currentMode == AuthMode.SIGN_IN) {
            switchToMode(AuthMode.SIGN_UP);
        } else if (currentMode == AuthMode.SIGN_UP) {
            switchToMode(AuthMode.SIGN_IN);
        }
    }

    /**
     * Switch to a specific authentication mode
     */
    private void switchToMode(AuthMode mode) {
        currentMode = mode;
        updateUIForMode(mode);
        clearInputs();
    }

    /**
     * Update UI based on current authentication mode
     */
    private void updateUIForMode(AuthMode mode) {
        switch (mode) {
            case SIGN_IN:
                tvWelcomeTitle.setText("Welcome Back");
                tvWelcomeSubtitle.setText("Sign in to your account to continue");
                layoutEmail.setVisibility(View.VISIBLE);
                layoutPassword.setVisibility(View.VISIBLE);
                layoutConfirmPassword.setVisibility(View.GONE);
                layoutVerificationCode.setVisibility(View.GONE);
                btnPrimaryAction.setText("Sign In");
                btnResendCode.setVisibility(View.GONE);
                tvForgotPassword.setVisibility(View.VISIBLE);
                tvSwitchModePrompt.setText("Don't have an account? ");
                tvSwitchMode.setText("Sign Up");
                btnGoogleSignIn.setVisibility(View.VISIBLE);
                break;

            case SIGN_UP:
                tvWelcomeTitle.setText("Create Account");
                tvWelcomeSubtitle.setText("Sign up to start tracking your location");
                layoutEmail.setVisibility(View.VISIBLE);
                layoutPassword.setVisibility(View.VISIBLE);
                layoutConfirmPassword.setVisibility(View.VISIBLE);
                layoutVerificationCode.setVisibility(View.GONE);
                btnPrimaryAction.setText("Sign Up");
                btnResendCode.setVisibility(View.GONE);
                tvForgotPassword.setVisibility(View.GONE);
                tvSwitchModePrompt.setText("Already have an account? ");
                tvSwitchMode.setText("Sign In");
                btnGoogleSignIn.setVisibility(View.VISIBLE);
                break;

            case EMAIL_VERIFICATION:
                tvWelcomeTitle.setText("Verify Email");
                tvWelcomeSubtitle.setText("Enter the verification code sent to your email");
                layoutEmail.setVisibility(View.GONE);
                layoutPassword.setVisibility(View.GONE);
                layoutConfirmPassword.setVisibility(View.GONE);
                layoutVerificationCode.setVisibility(View.VISIBLE);
                btnPrimaryAction.setText("Verify Email");
                btnResendCode.setVisibility(View.VISIBLE);
                tvForgotPassword.setVisibility(View.GONE);
                tvSwitchModePrompt.setText("Didn't receive code? ");
                tvSwitchMode.setText("Resend");
                btnGoogleSignIn.setVisibility(View.GONE);
                break;

            case FORGOT_PASSWORD:
                tvWelcomeTitle.setText("Reset Password");
                tvWelcomeSubtitle.setText("Enter your email to receive a reset code");
                layoutEmail.setVisibility(View.VISIBLE);
                layoutPassword.setVisibility(View.GONE);
                layoutConfirmPassword.setVisibility(View.GONE);
                layoutVerificationCode.setVisibility(View.GONE);
                btnPrimaryAction.setText("Send Reset Code");
                btnResendCode.setVisibility(View.GONE);
                tvForgotPassword.setVisibility(View.GONE);
                tvSwitchModePrompt.setText("Remember your password? ");
                tvSwitchMode.setText("Sign In");
                btnGoogleSignIn.setVisibility(View.GONE);
                break;

            case RESET_PASSWORD:
                tvWelcomeTitle.setText("New Password");
                tvWelcomeSubtitle.setText("Enter your new password and verification code");
                layoutEmail.setVisibility(View.GONE);
                layoutPassword.setVisibility(View.VISIBLE);
                layoutConfirmPassword.setVisibility(View.VISIBLE);
                layoutVerificationCode.setVisibility(View.VISIBLE);
                btnPrimaryAction.setText("Reset Password");
                btnResendCode.setVisibility(View.GONE);
                tvForgotPassword.setVisibility(View.GONE);
                tvSwitchModePrompt.setText("Remember your password? ");
                tvSwitchMode.setText("Sign In");
                btnGoogleSignIn.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Clear all input fields
     */
    private void clearInputs() {
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        etVerificationCode.setText("");

        // Clear errors
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);
        layoutVerificationCode.setError(null);
    }

    /**
     * Validation methods
     */
    private boolean validateEmailInput() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            layoutEmail.setError("Email is required");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    private boolean validatePasswordInput() {
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (password.isEmpty()) {
            layoutPassword.setError("Password is required");
            return false;
        }

        if (password.length() < 8) {
            layoutPassword.setError("Password must be at least 8 characters");
            return false;
        }

        return true;
    }

    private boolean validateSignInInputs() {
        return validateEmailInput() && validatePasswordInput();
    }

    private boolean validateSignUpInputs() {
        if (!validateEmailInput() || !validatePasswordInput()) {
            return false;
        }

        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText() != null ?
            etConfirmPassword.getText().toString().trim() : "";

        if (confirmPassword.isEmpty()) {
            layoutConfirmPassword.setError("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean validateResetPasswordInputs() {
        if (!validatePasswordInput()) {
            return false;
        }

        String code = etVerificationCode.getText() != null ?
            etVerificationCode.getText().toString().trim() : "";

        if (code.isEmpty()) {
            layoutVerificationCode.setError("Verification code is required");
            return false;
        }

        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText() != null ?
            etConfirmPassword.getText().toString().trim() : "";

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    /**
     * Check if user is already authenticated
     */
    private void checkExistingAuth() {
        cognitoAuthService.isSignedIn(new CognitoAuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                // User is already signed in, navigate to main activity
                navigateToMainActivity();
            }

            @Override
            public void onError(String error) {
                // User not signed in, stay on authentication screen
                Log.d(TAG, "User not signed in: " + error);
            }

            @Override
            public void onConfirmationRequired(String username) {
                // Not used for status check
            }
        });
    }

    /**
     * Setup back press handling
     */
    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentMode == AuthMode.EMAIL_VERIFICATION ||
                    currentMode == AuthMode.FORGOT_PASSWORD ||
                    currentMode == AuthMode.RESET_PASSWORD) {
                    // Go back to sign in mode
                    switchToMode(AuthMode.SIGN_IN);
                } else {
                    // Exit app
                    finish();
                }
            }
        });
    }

    /**
     * Navigate to main activity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * UI helper methods
     */
    private void showLoading(boolean isLoading) {
        btnPrimaryAction.setEnabled(!isLoading);
        btnGoogleSignIn.setEnabled(!isLoading);
        btnResendCode.setEnabled(!isLoading);

        if (isLoading) {
            btnPrimaryAction.setText("Loading...");
        } else {
            // Reset button text based on current mode
            updateUIForMode(currentMode);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
