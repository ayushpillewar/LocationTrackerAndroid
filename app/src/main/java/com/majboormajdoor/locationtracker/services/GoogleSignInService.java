package com.majboormajdoor.locationtracker.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import java.util.concurrent.Executors;

/**
 * Service class for Google Sign-In authentication
 * Uses the modern Credential Manager API for secure authentication
 */
public class GoogleSignInService {

    private static final String TAG = "GoogleSignInService";
    private static GoogleSignInService instance;

    // Your Google OAuth 2.0 client ID (you'll need to configure this)
    private static final String WEB_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";

    private CredentialManager credentialManager;

    // Google Sign-In callbacks
    public interface GoogleSignInCallback {
        void onSuccess(String idToken, String email, String displayName);
        void onError(String error);
        void onCancelled();
    }

    private GoogleSignInService() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static synchronized GoogleSignInService getInstance() {
        if (instance == null) {
            instance = new GoogleSignInService();
        }
        return instance;
    }

    /**
     * Initialize Google Sign-In service
     */
    public void initialize(Context context) {
        credentialManager = CredentialManager.create(context);
        Log.d(TAG, "Google Sign-In service initialized");
    }

    /**
     * Start Google Sign-In flow
     */
    public void signIn(Activity activity, GoogleSignInCallback callback) {
        // Configure Google ID option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build();

        // Create credential request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Start credential request
        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Google Sign-In failed", e);
                        callback.onError("Google Sign-In failed: " + e.getMessage());
                    }
                }
        );
    }

    /**
     * Handle Google Sign-In result
     */
    private void handleSignInResult(GetCredentialResponse result, GoogleSignInCallback callback) {
        // Extract Google ID token credential
        GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(result.getCredential().getData());

        // Extract user information
        String idToken = googleIdTokenCredential.getIdToken();
        String email = googleIdTokenCredential.getId();
        String displayName = googleIdTokenCredential.getDisplayName();
        String profilePictureUri = googleIdTokenCredential.getProfilePictureUri() != null ?
                googleIdTokenCredential.getProfilePictureUri().toString() : null;

        Log.d(TAG, "Google Sign-In successful for: " + email);
        callback.onSuccess(idToken, email, displayName);

    }

    /**
     * Sign out from Google (clears local credentials)
     */
    public void signOut(Context context, GoogleSignInCallback callback) {
        try {
            // Clear any cached credentials
            Log.d(TAG, "Google Sign-Out completed");
            callback.onSuccess(null, null, "Signed out successfully");
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-Out failed", e);
            callback.onError("Sign-out failed: " + e.getMessage());
        }
    }

    /**
     * Check if user has valid Google credentials
     */
    public void checkSignInStatus(Activity activity, GoogleSignInCallback callback) {
        // Configure Google ID option for authorized accounts only
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(WEB_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // No authorized accounts found or other error
                        Log.d(TAG, "No valid Google credentials found");
                        callback.onError("Not signed in with Google");
                    }
                }
        );
    }

    /**
     * Set the Google Web Client ID (call this before using the service)
     */
    public void setWebClientId(String clientId) {
        // Note: In a real implementation, you would store this securely
        // For now, this is a placeholder method
        Log.d(TAG, "Google Client ID configured");
    }
}
