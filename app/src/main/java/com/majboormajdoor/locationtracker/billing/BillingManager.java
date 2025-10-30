package com.majboormajdoor.locationtracker.billing;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.PendingPurchasesParams;

import java.util.ArrayList;
import java.util.List;

public class BillingManager {
    private static final String TAG = "BillingManager";
    private static final String SUBSCRIPTION_PRODUCT_ID = "com.majboormajdoor.monthly_sub"; // Test product for development

    private BillingClient billingClient;
    private Context context;
    private BillingListener billingListener;
    private ProductDetails subscriptionProductDetails;

    public interface BillingListener {
        void onBillingSetupFinished(BillingResult billingResult);
        void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases);
        void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList);
        void onPurchaseAcknowledged(BillingResult billingResult);
    }

    public BillingManager(Context context, BillingListener listener) {
        this.context = context;
        this.billingListener = listener;

        PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                Log.d(TAG, "onPurchasesUpdated: " + billingResult.getDebugMessage());
                handlePurchasesUpdated(billingResult, purchases);
            }
        };

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enableAutoServiceReconnection()
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        startConnection();
    }

    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.d(TAG, "Billing setup finished: " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryProductDetails();
                    queryPurchases();
                }
                if (billingListener != null) {
                    billingListener.onBillingSetupFinished(billingResult);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected");
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();

        billingClient.queryProductDetailsAsync(params,
            (billingResult, productDetailsList) -> {
                Log.d(TAG, "Product details query result: " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList != null && !productDetailsList.getProductDetailsList().isEmpty()) {
                        subscriptionProductDetails = productDetailsList.getProductDetailsList().get(0);
                        Log.d(TAG, "Successfully loaded product details for: " + SUBSCRIPTION_PRODUCT_ID);
                    } else {
                        Log.w(TAG, "No product details found for product ID: " + SUBSCRIPTION_PRODUCT_ID +
                              ". Make sure the product is configured in Google Play Console.");
                    }
                } else {
                    Log.e(TAG, "Failed to query product details. Response code: " + billingResult.getResponseCode() +
                          ", Debug message: " + billingResult.getDebugMessage());
                }
                if (billingListener != null) {
                    billingListener.onProductDetailsResponse(billingResult, productDetailsList.getProductDetailsList());
                }
            });
    }

    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.w(TAG, "BillingClient is not ready");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build();

        billingClient.queryPurchasesAsync(params,
            (billingResult, purchasesList) -> {
                Log.d(TAG, "Query purchases result: " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    handlePurchases(purchasesList);
                }
            });
    }

    public void launchBillingFlow(Activity activity) {
        if (subscriptionProductDetails == null) {
            Log.w(TAG, "Product details not available");
            return;
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList =
            subscriptionProductDetails.getSubscriptionOfferDetails();

        if (offerDetailsList == null || offerDetailsList.isEmpty()) {
            Log.w(TAG, "No subscription offers available");
            return;
        }

        ProductDetails.SubscriptionOfferDetails offerDetails = offerDetailsList.get(0);
        String offerToken = offerDetails.getOfferToken();

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(subscriptionProductDetails)
                .setOfferToken(offerToken)
                .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        Log.d(TAG, "Launch billing flow result: " + billingResult.getDebugMessage());
    }

    private void handlePurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase");
        } else {
            Log.w(TAG, "Purchase update failed: " + billingResult.getDebugMessage());
        }

        if (billingListener != null) {
            billingListener.onPurchasesUpdated(billingResult, purchases);
        }
    }

    private void handlePurchases(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            handlePurchase(purchase);
        }
    }

    private void handlePurchase(Purchase purchase) {
        Log.d(TAG, "Handling purchase: " + purchase.getPurchaseToken());

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Verify the purchase
            if (verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {

                if (grantPremiumAccess() && !purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }

            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending");
        } else {
            Log.d(TAG, "Purchase state: " + purchase.getPurchaseState());
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();

        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            Log.d(TAG, "Acknowledge purchase result: " + billingResult.getDebugMessage());
            if (billingListener != null) {
                billingListener.onPurchaseAcknowledged(billingResult);
            }
        });
    }

    private boolean verifyValidSignature(String signedData, String signature) {
        // TODO: Implement proper signature verification using your app's public key
        // For now, we'll just return true, but in production you should verify the signature
        // using Google Play's RSA public key for your app
        Log.w(TAG, "Signature verification not implemented - accepting all purchases");
        return true;
    }

    private boolean grantPremiumAccess() {
        Log.d(TAG, "Granting premium access to user");
        // TODO: Implement your app's logic to grant premium features
        // This could include:
        // - Setting a flag in SharedPreferences
        // - Updating user status in your backend
        // - Enabling premium features in the UI
        return true;

    }

    public void checkSubscriptionStatus(SubscriptionStatusCallback callback) {
        // Check if user has an active subscription
        if (!billingClient.isReady()) {
            callback.onResult(false);
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build();

        billingClient.queryPurchasesAsync(params,
            (result, purchases) -> {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getProducts().contains(SUBSCRIPTION_PRODUCT_ID) &&
                            purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            callback.onResult(true);
                            return;
                        }
                    }
                }
                callback.onResult(false);
            });
    }

    public interface SubscriptionStatusCallback {
        void onResult(boolean isSubscribed);
    }

    public void endConnection() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    public boolean isReady() {
        return billingClient != null && billingClient.isReady();
    }

    public ProductDetails getSubscriptionProductDetails() {
        return subscriptionProductDetails;
    }

    public String getSubscriptionPrice() {
        if (subscriptionProductDetails != null &&
            subscriptionProductDetails.getSubscriptionOfferDetails() != null &&
            !subscriptionProductDetails.getSubscriptionOfferDetails().isEmpty()) {

            ProductDetails.SubscriptionOfferDetails offerDetails =
                subscriptionProductDetails.getSubscriptionOfferDetails().get(0);

            if (offerDetails.getPricingPhases() != null &&
                offerDetails.getPricingPhases().getPricingPhaseList() != null &&
                !offerDetails.getPricingPhases().getPricingPhaseList().isEmpty()) {

                return offerDetails.getPricingPhases()
                    .getPricingPhaseList().get(0)
                    .getFormattedPrice();
            }
        }
        return "$5.00"; // Fallback price
    }
}
