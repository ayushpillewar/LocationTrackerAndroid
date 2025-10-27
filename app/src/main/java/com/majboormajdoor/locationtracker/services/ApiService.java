package com.majboormajdoor.locationtracker.services;

import android.util.Log;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;

import org.apache.hc.core5.http.io.entity.StringEntity;


public class ApiService {

    private static final String TAG = "ApiService";
    private final CloseableHttpClient httpClient;
    private final CognitoAuthService authService;

    private static final String BASE_URL = "https://i81leg33o7.execute-api.ap-south-1.amazonaws.com/prod";

    public ApiService() {
        this.httpClient = CustomHttpClientConfig.createHttpClient();
        this.authService = CognitoAuthService.getInstance();
    }

    /**
     * Posts location data to the backend with authentication using Amplify session directly
     */
    public void postLocation(Location locationData, ApiCallback callback) {
        // First get the authentication token using CognitoAuthService for proper JWT token
        authService.getTokenForApiCall(new CognitoAuthService.TokenCallback() {
            @Override
            public void onTokenRetrieved(String accessToken, String idToken) {
                // Get Amplify session for additional context
                com.amplifyframework.core.Amplify.Auth.fetchAuthSession(
                    result -> {
                        if (result.isSignedIn()) {
                            // Use both JWT token and session for API call
                            executeLocationPostWithAuth(locationData, accessToken, result, callback);
                        } else {
                            Log.e(TAG, "User not signed in");
                            callback.onError("User not signed in - please authenticate first");
                        }
                    },
                    error -> {
                        Log.e(TAG, "Failed to fetch auth session: " + error);
                        callback.onError("Authentication session failed: " + error.getMessage());
                    }
                );
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get authentication token: " + error);
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Execute the actual HTTP POST request with JWT token and Amplify session authentication
     */
    private void executeLocationPostWithAuth(Location locationData, String jwtToken, com.amplifyframework.auth.AuthSession authSession, ApiCallback callback) {
        // Execute network operation on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            HttpPost postRequest;
            try {

                AWSCognitoAuthSession cogSession = (AWSCognitoAuthSession) authSession;
                if (cogSession.getUserPoolTokensResult().getValue() == null){
                    Log.e(TAG, "Cognito session tokens are null");
                    callback.onError("Authentication tokens are null - please authenticate first");
                    return;
                 }
                postRequest = new HttpPost(BASE_URL+ "/sendLocation");
                postRequest.setHeader("Content-Type", "application/json");
                postRequest.setHeader("Authorization", cogSession.getUserPoolTokensResult().getValue().getIdToken());
                postRequest.setHeader("X-Amz-Date", ValidationUtils.generateISO8601BasicFormat());
                Log.d(TAG, "Added Authorization header with JWT token");

                // For AWS API Gateway, we need to pass Cognito identity information
                // Try to extract user information from the session
                try {
                    // Get current user to add user context headers
                    com.amplifyframework.core.Amplify.Auth.getCurrentUser(
                        user -> {
                            // Add user identification headers for AWS API Gateway
                            postRequest.setHeader("X-Amz-User-Id", user.getUserId());
                            postRequest.setHeader("X-Amz-User-Sub", user.getUserId());

                            Log.d(TAG, "Added user headers - ID: " + user.getUserId());
                        },
                        error -> Log.w(TAG, "Could not get user info: " + error)
                    );
                } catch (Exception e) {
                    Log.w(TAG, "Error getting user context: " + e.getMessage());
                }

                Log.d(TAG, "Using Amplify session for authentication");
                Log.d(TAG, "Session signed in: " + authSession.isSignedIn());

                // Create the request entity
                postRequest.setEntity(createLocationEntity(locationData));

                // Log the location data being sent
                Log.d(TAG, "Sending location data - Lat: " + locationData.getLatitude() +
                          ", Lng: " + locationData.getLongitude() +
                          ", Email: " + locationData.getEmail()); // Using getPhoneNumber() which contains email
                Log.d(TAG, "Sending POST request to: " + BASE_URL);

            } catch (JsonProcessingException e) {
                Log.e(TAG, "Error serializing location data", e);
                callback.onError("Failed to serialize location data");
                return;
            }

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                int statusCode = response.getCode();
                if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                    Log.d(TAG, "Location updated successfully - Status: " + statusCode);
                    callback.onSuccess("Location sent successfully to API");
                } else {
                    Log.w(TAG, "Failed to update location. HTTP Status: " + statusCode);
                    // Try to get response body for more details
                    try {
                        if (response.getEntity() != null) {

                            callback.onError("API call failed with status " + statusCode + ": " );
                        } else {
                            callback.onError("API call failed with status " + statusCode + " (no response body)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading response body: " + e.getMessage());
                        callback.onError("API call failed with status " + statusCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending location data", e);
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Callback interface for API responses
     */
    public interface ApiCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private StringEntity createLocationEntity(Location locationData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(locationData);
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }

}

