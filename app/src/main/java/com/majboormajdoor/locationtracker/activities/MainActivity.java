package com.majboormajdoor.locationtracker.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import com.majboormajdoor.locationtracker.utils.PermissionUtils;

/**
 * Main activity with bottom navigation for home and cloud sections
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private HomeFragment homeFragment;
    private CloudFragment cloudFragment;

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeFragments();
        setupBottomNavigation();
        setupBackPressHandling();

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }

        // Request permissions if not already granted
        checkAndRequestPermissions();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
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
