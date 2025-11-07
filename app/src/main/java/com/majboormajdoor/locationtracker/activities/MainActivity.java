package com.majboormajdoor.locationtracker.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.billing.BillingManager;
import com.majboormajdoor.locationtracker.constants.AppConstants;
import com.majboormajdoor.locationtracker.fragments.CloudFragment;
import com.majboormajdoor.locationtracker.fragments.HomeFragment;
import com.majboormajdoor.locationtracker.services.CognitoAuthService;
import com.majboormajdoor.locationtracker.utils.PermissionUtils;

/**
 * Main activity with bottom navigation for home and cloud sections
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final String PREF_FIRST_LAUNCH = "first_launch";
    private static final String PREF_NAME = "LocationTrackerPrefs";

    private BottomNavigationView bottomNavigation;
    private HomeFragment homeFragment;
    private CloudFragment cloudFragment;
    private ImageButton btnInfo;
    private ImageButton btnSignOut;
    private SharedPreferences sharedPreferences;
    private CognitoAuthService cognitoAuthService;

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize CognitoAuthService
        cognitoAuthService = CognitoAuthService.getInstance(getApplicationContext());

        initializeViews();
        initializeFragments();
        setupBottomNavigation();
        setupBackPressHandling();
        setupInfoButton();
        setupSignOutButton();

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }

        // Request permissions if not already granted
        checkAndRequestPermissions();

        // Show info dialog on first launch
        showFirstLaunchDialog();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        btnInfo = findViewById(R.id.btn_info);
        btnSignOut = findViewById(R.id.btn_sign_out);
    }

    /**
     * Initialize fragments
     */
    private void initializeFragments() {
        homeFragment = new HomeFragment();
        cloudFragment = new CloudFragment();
    }

    /**
     * Setup bottom navigation listener
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = homeFragment;
            } else if (item.getItemId() == R.id.navigation_cloud) {
                selectedFragment = cloudFragment;
            }

            if (selectedFragment != null) {
                try{
                    loadFragment(selectedFragment);
                }catch(Exception e){
                    Log.e(TAG, "Error loading fragment: " + e.getMessage());
                }

                return true;
            }
            return false;
        });
    }

    /**
     * Load fragment into container
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Setup modern back press handling
     */
    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If we're not on home fragment, go back to home
                if (bottomNavigation.getSelectedItemId() != R.id.navigation_home) {
                    bottomNavigation.setSelectedItemId(R.id.navigation_home);
                    loadFragment(homeFragment);
                } else {
                    // If on home fragment, minimize app
                    moveTaskToBack(true);
                }
            }
        });
    }

    /**
     * Setup info button click listener
     */
    private void setupInfoButton() {
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showInfoDialog());
        }
    }

    /**
     * Setup sign-out button click listener
     */
    private void setupSignOutButton() {
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> showSignOutConfirmationDialog());
        }
    }

    /**
     * Show confirmation dialog before signing out
     */
    private void showSignOutConfirmationDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out? You will need to log in again to access the app.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        dialog.dismiss();
                        performSignOut();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .create()
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing sign out confirmation dialog: " + e.getMessage());
            // Fallback to direct sign out if dialog fails
            performSignOut();
        }
    }

    /**
     * Perform sign out operation
     */
    private void performSignOut() {
        if (cognitoAuthService != null) {
            // Show loading message
            Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();

            // Disable sign-out button to prevent multiple clicks
            btnSignOut.setEnabled(false);

            cognitoAuthService.signOut(new CognitoAuthService.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Sign out successful: " + message);
                    runOnUiThread(() -> {
                        showSuccess("Signed out successfully");
                        navigateToPinLockActivity();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Sign out failed: " + error);
                    runOnUiThread(() -> {
                        showError("Sign out failed: " + error);
                        // Re-enable sign-out button
                        btnSignOut.setEnabled(true);
                    });
                }

                @Override
                public void onConfirmationRequired(String message) {
                    // Not needed for sign out, but required by interface
                    Log.d(TAG, "Confirmation required for sign out: " + message);
                }
            });
        } else {
            showError("Authentication service not available");
        }
    }

    /**
     * Navigate back to PinLockActivity
     */
    private void navigateToPinLockActivity() {
        try {
            Intent intent = new Intent(this, com.majboormajdoor.locationtracker.activities.PinLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to PinLockActivity: " + e.getMessage());
            showError("Unable to navigate to login screen");
        }
    }

    /**
     * Show info dialog on first launch
     */
    private void showFirstLaunchDialog() {
        boolean isFirstLaunch = sharedPreferences.getBoolean(PREF_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            // Mark first launch as completed
            sharedPreferences.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();

            // Show info dialog after a short delay to ensure UI is ready
            btnInfo.postDelayed(this::showInfoDialog, 500);
        }
    }

    /**
     * Show info dialog with app instructions
     */
    private void showInfoDialog() {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_app_info);
            dialog.setCancelable(true);

            // Set dialog window properties
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            // Setup dialog button
            Button btnGotIt = dialog.findViewById(R.id.btn_got_it);
            if (btnGotIt != null) {
                btnGotIt.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing info dialog: " + e.getMessage());
            showError("Unable to show app information");
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
                    // Check for notification permission next (non-recursively)
                    if (!PermissionUtils.isNotificationPermissionGranted(this)) {
                        PermissionUtils.requestNotificationPermission(this);
                    }
                } else {
                    showError(AppConstants.ERROR_LOCATION_PERMISSION);
                }
                break;

            case AppConstants.PERMISSION_REQUEST_SMS:
                if (allGranted) {
                    showSuccess("SMS permission granted");
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
}
