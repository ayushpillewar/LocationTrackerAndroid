package com.majboormajdoor.locationtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.billing.BillingManager;
import com.majboormajdoor.locationtracker.services.ApiService;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;

import java.util.List;

public class SubscriptionActivity extends AppCompatActivity implements BillingManager.BillingListener {
    private static final String TAG = "SubscriptionActivity";

    private BillingManager billingManager;
    private Button btnStartSubscription;
    private TextView tvSubscribePrice;
    private TextView tvSubscribeTitle;
    private TextView tvSubscribeDesc;
    private TextView tvSubscriptionInfo;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_subscription);
        apiService = new ApiService();
        initViews();
        initBilling();
        setupClickListeners();
    }

    private void initViews() {
        btnStartSubscription = findViewById(R.id.btnStartSubscription);
        tvSubscribePrice = findViewById(R.id.tvSubscribePrice);
        tvSubscribeTitle = findViewById(R.id.tvSubscribeTitle);
        tvSubscribeDesc = findViewById(R.id.tvSubscribeDesc);
        tvSubscriptionInfo = findViewById(R.id.tvSubscriptionInfo);

        // Disable button initially until billing is ready
        btnStartSubscription.setEnabled(false);
    }

    private void initBilling() {
        billingManager = new BillingManager(this, this);
    }

    private void setupClickListeners() {
        btnStartSubscription.setOnClickListener(v -> {
            if (billingManager.isReady()) {
                ProductDetails productDetails = billingManager.getSubscriptionProductDetails();

                if (productDetails != null) {
                    billingManager.launchBillingFlow(this);
                } else {
                    Toast.makeText(this, "Product not available. Please check your Google Play Console configuration.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Product details are null. Make sure 'location-monthly' is configured in Google Play Console");
                }
            } else {
                Toast.makeText(this, "Billing service not ready. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        Log.d(TAG, "Billing setup finished: " + billingResult.getResponseCode());

        if (billingResult.getResponseCode() == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
            runOnUiThread(() -> {
                btnStartSubscription.setEnabled(true);
                btnStartSubscription.setText(R.string.start_subscription);
            });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to connect to billing service", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        Log.d(TAG, "Purchases updated: " + billingResult.getResponseCode());

        if (billingResult.getResponseCode() == com.android.billingclient.api.BillingClient.BillingResponseCode.OK && purchases != null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Subscription successful! Thank you!", Toast.LENGTH_LONG).show();
                navigateToMainActivity();
            });
        } else if (billingResult.getResponseCode() == com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_SHORT).show();
            });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Purchase failed: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }


    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
        Log.d(TAG, "Product details response: " + billingResult.getResponseCode());


        if (billingResult.getResponseCode() == com.android.billingclient.api.BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
            runOnUiThread(() -> {
                // Update price display with actual price from Google Play
                String actualPrice = billingManager.getSubscriptionPrice();
                tvSubscribePrice.setText(actualPrice + " / month");
            });
        }

        // Check if user already has an active subscription
        billingManager.queryActiveSubscription(isActive -> {
            runOnUiThread(() -> {
                if (isActive) {
                    PreferenceManager.getInstance(getApplicationContext()).setUserSubscriptionStatus(true);
                    navigateToMainActivity();
                } else {
                    PreferenceManager.getInstance(getApplicationContext()).setUserSubscriptionStatus(false);
                    tvSubscriptionInfo.setText("No Active Subscription");
                    btnStartSubscription.setEnabled(true);
                    btnStartSubscription.setText(R.string.start_subscription);
                }
            });
        });
    }

    @Override
    public void onPurchaseAcknowledged(BillingResult billingResult) {
        Log.d(TAG, "Purchase acknowledged: " + billingResult.getResponseCode());

        if (billingResult.getResponseCode() == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Subscription activated!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) {
            billingManager.endConnection();
        }
    }
}
