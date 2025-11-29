package com.majboormajdoor.locationtracker.services;

import static com.majboormajdoor.locationtracker.constants.AppConstants.BASE_URL;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.dto.SubscriptionRequest;
import com.majboormajdoor.locationtracker.dto.User;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import com.majboormajdoor.locationtracker.utils.ValidationUtils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;

import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.Arrays;
import java.util.List;


public class ApiService {

    private static final String TAG = "ApiService";
    private final CloseableHttpClient httpClient;
    private final CognitoAuthService authService;



    public ApiService(Context context) {
        this.httpClient = CustomHttpClientConfig.createHttpClient();
        this.authService = CognitoAuthService.getInstance(context);
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

    public void createSubscription(SubscriptionRequest subscriptionRequest, ApiCallback callback) {
        // First get the authentication token using CognitoAuthService for proper JWT token
        authService.getTokenForApiCall(new CognitoAuthService.TokenCallback() {
            @Override
            public void onTokenRetrieved(String accessToken, String idToken) {
                // Get Amplify session for additional context
                com.amplifyframework.core.Amplify.Auth.fetchAuthSession(
                        result -> {
                            if (result.isSignedIn()) {
                                // Use both JWT token and session for API call
                                executeSubscriptionPutWithAuth(subscriptionRequest, accessToken, result, callback);
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

    public void checkSubscription(Context context, UserCallback callback) {
        // First get the authentication token using CognitoAuthService for proper JWT token
        authService.getTokenForApiCall(new CognitoAuthService.TokenCallback() {
            @Override
            public void onTokenRetrieved(String accessToken, String idToken) {
                // Get Amplify session for additional context
                com.amplifyframework.core.Amplify.Auth.fetchAuthSession(
                        result -> {
                            if (result.isSignedIn()) {
                                // Use both JWT token and session for API call
                                executeUserCheckSubscription(result, callback, context);
                            } else {
                                Log.e(TAG, "User not signed in");
                                callback.onSubscriptionCheckError("Error");
                            }
                        },
                        error -> {
                            Log.e(TAG, "Failed to fetch auth session: " + error);
                            callback.onSubscriptionCheckError("Authentication error");
                        }
                );
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get authentication token: " + error);
                callback.onSubscriptionCheckError("Authentication failed: " + error);
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
                postRequest = new HttpPost(BASE_URL+ "/location");
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
                          ", Email: " + locationData.getUserName()); // Using getPhoneNumber() which contains email
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
     * Fetches location history from the backend
     */
    public void getLocationHistory(LocationHistoryCallback callback, Context context) {
        // First get the authentication token using CognitoAuthService for proper JWT token
        authService.getTokenForApiCall(new CognitoAuthService.TokenCallback() {
            @Override
            public void onTokenRetrieved(String accessToken, String idToken) {
                // Get Amplify session for additional context
                com.amplifyframework.core.Amplify.Auth.fetchAuthSession(
                    result -> {
                        if (result.isSignedIn()) {
                            // Use both JWT token and session for API call
                            executeLocationGetWithAuth(accessToken, result, callback, context);
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
     * Execute the actual HTTP GET request to fetch location history with JWT token and Amplify session authentication
     */
    private void executeLocationGetWithAuth(String jwtToken, com.amplifyframework.auth.AuthSession authSession, LocationHistoryCallback callback, Context context) {
        // Execute network operation on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {

            HttpGet getRequest;
            try {
                AWSCognitoAuthSession cogSession = (AWSCognitoAuthSession) authSession;
                if (cogSession.getUserPoolTokensResult().getValue() == null){
                    Log.e(TAG, "Cognito session tokens are null");
                    callback.onError("Authentication tokens are null - please authenticate first");
                    return;
                }


                getRequest = new HttpGet(BASE_URL + "/location" + "?userId=" + PreferenceManager.getInstance(context).getUserId());
                getRequest.setHeader("Content-Type", "application/json");
                getRequest.setHeader("Authorization", cogSession.getUserPoolTokensResult().getValue().getIdToken());
                getRequest.setHeader("X-Amz-Date", ValidationUtils.generateISO8601BasicFormat());
                Log.d(TAG, "Added Authorization header with JWT token for GET request");

                // For AWS API Gateway, we need to pass Cognito identity information
                try {
                    // Get current user to add user context headers
                    Amplify.Auth.getCurrentUser(
                        user -> {

                            // Add user identification headers for AWS API Gateway
                            getRequest.setHeader("X-Amz-User-Id", user.getUserId());
                            getRequest.setHeader("X-Amz-User-Sub", user.getUserId());

                            Log.d(TAG, "Added user headers - ID: " + user.getUserId());
                        },
                        error -> Log.w(TAG, "Could not get user info: " + error)
                    );
                } catch (Exception e) {
                    Log.w(TAG, "Error getting user context: " + e.getMessage());
                }

                Log.d(TAG, "Sending GET request to: " + BASE_URL + "/location");

            } catch (Exception e) {
                Log.e(TAG, "Error creating GET request", e);
                callback.onError("Failed to create request");
                return;
            }

            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                int statusCode = response.getCode();
                if (statusCode == HttpStatus.SC_OK) {
                    Log.d(TAG, "Location history fetched successfully - Status: " + statusCode);

                    // Parse the response body
                    if (response.getEntity() != null) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        Log.d(TAG, "Response body: " + responseBody);

                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            // Parse as array of Location objects
                            Location[] locations = mapper.readValue(responseBody, Location[].class);
                            List<Location> locationList = Arrays.asList(locations);
                            callback.onSuccess(locationList);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing location history response", e);
                            callback.onError("Failed to parse location data: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Empty response from server");
                    }
                } else {
                    Log.w(TAG, "Failed to fetch location history. HTTP Status: " + statusCode);
                    // Try to get response body for more details
                    try {
                        if (response.getEntity() != null) {
                            String errorBody = EntityUtils.toString(response.getEntity());
                            callback.onError("API call failed with status " + statusCode + ": " + errorBody);
                        } else {
                            callback.onError("API call failed with status " + statusCode + " (no response body)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error response body: " + e.getMessage());
                        callback.onError("API call failed with status " + statusCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching location history", e);
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }

    private StringEntity createLocationEntity(Location locationData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(locationData);
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }

    private StringEntity createSubscriptionRequestEntity(SubscriptionRequest subscriptionRequest) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(subscriptionRequest);
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }


    /**
     * Callback interface for API responses
     */
    public interface ApiCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Callback interface for location history responses
     */
    public interface LocationHistoryCallback {
        void onSuccess(java.util.List<Location> locationHistory);
        void onError(String error);
    }

    public interface UserCallback {
        void onSubscriptionCheckSuccess(User user);
        void onSubscriptionCheckError(String error);
    }

    private void executeUserCheckSubscription(com.amplifyframework.auth.AuthSession authSession, UserCallback callback, Context context) {
        // Execute network operation on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            saveUserIdToPreferences(context);
            HttpGet getRequest = buildGetRequest((AWSCognitoAuthSession) authSession, callback, context);
            if (getRequest == null) return;

            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                int statusCode = response.getCode();
                if (statusCode == HttpStatus.SC_OK) {
                    Log.d(TAG, "User checked successfully " + statusCode);

                    // Parse the response body
                    if (response.getEntity() != null) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        Log.d(TAG, "Response body: " + responseBody);

                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            // Parse as array of Location objects
                            User user = mapper.readValue(responseBody, User.class);
                            callback.onSubscriptionCheckSuccess(user);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing luser subscription", e);
                            callback.onSubscriptionCheckError("Failed to parse user subscription " + e.getMessage());
                        }
                    } else {
                        callback.onSubscriptionCheckError("Empty response from server");
                    }
                } else {
                    Log.w(TAG, "Failed to check user subscription" + statusCode);
                    // Try to get response body for more details
                    try {
                        if (response.getEntity() != null) {
                            String errorBody = EntityUtils.toString(response.getEntity());
                            callback.onSubscriptionCheckError("API call failed with status " + statusCode + ": " + errorBody);
                        } else {
                            callback.onSubscriptionCheckError("API call failed with status " + statusCode + " (no response body)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error response body: " + e.getMessage());
                        callback.onSubscriptionCheckError("API call failed with status " + statusCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching location history", e);
                callback.onSubscriptionCheckError("Network error: " + e.getMessage());
            }
        }).start();
    }

    @Nullable
    private static HttpGet buildGetRequest(AWSCognitoAuthSession authSession, UserCallback callback, Context context) {
        HttpGet getRequest;
        try {
            AWSCognitoAuthSession cogSession = authSession;
            if (cogSession.getUserPoolTokensResult().getValue() == null){
                Log.e(TAG, "Cognito session tokens are null");
                callback.onSubscriptionCheckError("Authentication tokens are null - please authenticate first");
                return null;
            }

            String userID = PreferenceManager.getInstance(context).getUserId();
            if(userID == null || userID.isEmpty()){
               saveUserIdToPreferences(context.getApplicationContext());
            }
            getRequest = new HttpGet(BASE_URL + "/subscribe" + "?userId=" + userID);
            getRequest.setHeader("Content-Type", "application/json");
            getRequest.setHeader("Authorization", cogSession.getUserPoolTokensResult().getValue().getIdToken());
            getRequest.setHeader("X-Amz-Date", ValidationUtils.generateISO8601BasicFormat());
            getRequest.setHeader("X-Amz-User-Id", userID);
            getRequest.setHeader("X-Amz-User-Sub", userID);

            Log.d(TAG, "Added user headers - ID: " + userID);
            Log.d(TAG, "Added Authorization header with JWT token for GET request");
            Log.d(TAG, "Sending GET request to: " + BASE_URL + "/location");

        } catch (Exception e) {
            Log.e(TAG, "Error creating GET request", e);
            callback.onSubscriptionCheckError("Failed to create request");
            return null;
        }
        return getRequest;
    }

    public static void saveUserIdToPreferences(Context context) {
        try {
            // Get current user to add user context headers
            Amplify.Auth.getCurrentUser(
                    authUser -> {
                        // Add user identification headers for AWS API Gateway
                       PreferenceManager.getInstance(context).saveUserId(authUser.getUserId());
                    },
                    error -> Log.w(TAG, "Could not get user info: " + error)
            );
        } catch (Exception e) {
            Log.w(TAG, "Error getting user context: " + e.getMessage());
        }
    }

    private void executeSubscriptionPutWithAuth(SubscriptionRequest subscriptionRequest, String jwtToken, com.amplifyframework.auth.AuthSession authSession, ApiCallback callback) {
        // Execute network operation on background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {

            HttpPut httpPut;
            try {

                AWSCognitoAuthSession cogSession = (AWSCognitoAuthSession) authSession;
                if (cogSession.getUserPoolTokensResult().getValue() == null){
                    Log.e(TAG, "Cognito session tokens are null");
                    callback.onError("Authentication tokens are null - please authenticate first");
                    return;
                }
                httpPut = new HttpPut(BASE_URL+ "/subscribe");
                httpPut.setHeader("Content-Type", "application/json");
                httpPut.setHeader("Authorization", cogSession.getUserPoolTokensResult().getValue().getIdToken());
                httpPut.setHeader("X-Amz-Date", ValidationUtils.generateISO8601BasicFormat());
                Log.d(TAG, "Added Authorization header with JWT token");

                // For AWS API Gateway, we need to pass Cognito identity information
                // Try to extract user information from the session
                try {
                    // Get current user to add user context headers
                    com.amplifyframework.core.Amplify.Auth.getCurrentUser(
                            authUser -> {
                                // Add user identification headers for AWS API Gateway
                                httpPut.setHeader("X-Amz-User-Id", authUser.getUserId());
                                httpPut.setHeader("X-Amz-User-Sub", authUser.getUserId());

                                Log.d(TAG, "Added user headers - ID: " + authUser.getUserId());
                            },
                            error -> Log.w(TAG, "Could not get user info: " + error)
                    );
                } catch (Exception e) {
                    Log.w(TAG, "Error getting user context: " + e.getMessage());
                }

                Log.d(TAG, "Using Amplify session for authentication");
                Log.d(TAG, "Session signed in: " + authSession.isSignedIn());

                // Create the request entity
                httpPut.setEntity(createSubscriptionRequestEntity(subscriptionRequest));


            } catch (JsonProcessingException e) {
                Log.e(TAG, "Error serializing user data", e);
                callback.onError("Failed to serialize user data");
                return;
            }

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
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
}

