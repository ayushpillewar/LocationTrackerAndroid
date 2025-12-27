# LocationTracker API Documentation

## Overview

The LocationTracker Android application communicates with a backend REST API for user management, location data storage, and subscription management. This document details all API endpoints, request/response formats, and integration patterns.

## Base Configuration

### API Base URL
```java
// Configured in ApiService.java
private static final String BASE_URL = "your-backend-api-url";
```

### Authentication
All API calls require authentication via AWS Cognito session tokens:
```java
// Authentication header
Authorization: Bearer <cognito-session-token>
Content-Type: application/json
```

## API Endpoints

### 1. User Management

#### POST /user
**Purpose**: Create or update user subscription information

**Request Headers**:
```
Authorization: Bearer <session-token>
Content-Type: application/json
```

**Request Body**:
```json
{
    "userId": "cognito-user-uuid",
    "email": "user@example.com",
    "subStartDate": "2024-01-15",
    "subEndDate": "2024-02-15",
    "insertionTimestamp": "2024-01-15 10:30:00"
}
```

**Response** (200 OK):
```json
{
    "message": "User subscription updated successfully",
    "status": "success"
}
```

**Error Response** (400/500):
```json
{
    "error": "Invalid subscription data",
    "status": "error"
}
```

**Implementation**:
```java
public void updateSubscription(SubscriptionRequest request, ApiCallback callback) {
    try {
        String jsonBody = objectMapper.writeValueAsString(request);
        HttpPost httpPost = new HttpPost(BASE_URL + "/user");
        
        // Add authentication header
        String token = getAuthToken();
        httpPost.setHeader("Authorization", "Bearer " + token);
        httpPost.setHeader("Content-Type", "application/json");
        
        StringEntity entity = new StringEntity(jsonBody);
        httpPost.setEntity(entity);
        
        // Execute request
        executeRequest(httpPost, callback);
    } catch (Exception e) {
        callback.onError("Failed to update subscription: " + e.getMessage());
    }
}
```

#### GET /user/subscription
**Purpose**: Check user subscription status

**Request Headers**:
```
Authorization: Bearer <session-token>
```

**Query Parameters**:
- `userId` (required): Cognito user identifier

**Response** (200 OK):
```json
{
    "userId": "cognito-user-uuid",
    "email": "user@example.com",
    "subStartDate": "2024-01-15",
    "subEndDate": "2024-02-15",
    "insertionTimestamp": "2024-01-15 10:30:00",
    "isActive": true
}
```

**Implementation**:
```java
public void checkSubscription(Context context, UserCallback callback) {
    try {
        String userId = getUserId(context);
        String url = BASE_URL + "/user/subscription?userId=" + userId;
        
        HttpGet httpGet = new HttpGet(url);
        String token = getAuthToken();
        httpGet.setHeader("Authorization", "Bearer " + token);
        
        // Execute and parse response
        executeUserRequest(httpGet, callback);
    } catch (Exception e) {
        callback.onSubscriptionCheckError("Failed to check subscription");
    }
}
```

### 2. Location Data Management

#### POST /location
**Purpose**: Submit location data for tracking

**Request Headers**:
```
Authorization: Bearer <session-token>
Content-Type: application/json
```

**Request Body**:
```json
{
    "userEmail": "user@example.com",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "insertionTimestamp": "2024-01-15 14:30:00"
}
```

**Response** (200 OK):
```json
{
    "message": "Location data saved successfully",
    "locationId": "loc-12345",
    "status": "success"
}
```

**Error Responses**:
```json
// 401 Unauthorized
{
    "error": "Invalid or expired authentication token",
    "status": "error"
}

// 403 Forbidden
{
    "error": "Subscription required for location tracking",
    "status": "error"
}

// 400 Bad Request
{
    "error": "Invalid location data format",
    "status": "error"
}
```

**Implementation**:
```java
public void postLocation(Location location, ApiCallback callback) {
    try {
        // Prepare request
        String jsonBody = objectMapper.writeValueAsString(location);
        HttpPost httpPost = new HttpPost(BASE_URL + "/location");
        
        // Authentication
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            callback.onError("Authentication token not available");
            return;
        }
        
        httpPost.setHeader("Authorization", "Bearer " + token);
        httpPost.setHeader("Content-Type", "application/json");
        
        StringEntity entity = new StringEntity(jsonBody);
        httpPost.setEntity(entity);
        
        // Execute request in background thread
        CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    return "Success: " + responseBody;
                } else {
                    throw new RuntimeException("HTTP " + statusCode + ": " + responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException("Network error", e);
            }
        }).thenAccept(result -> {
            callback.onSuccess(result);
        }).exceptionally(throwable -> {
            callback.onError(throwable.getMessage());
            return null;
        });
        
    } catch (Exception e) {
        callback.onError("Failed to send location: " + e.getMessage());
    }
}
```

#### GET /location
**Purpose**: Retrieve user's location history

**Request Headers**:
```
Authorization: Bearer <session-token>
```

**Query Parameters**:
- `userEmail` (required): User's email address
- `limit` (optional): Number of records to return (default: 50)
- `offset` (optional): Pagination offset (default: 0)
- `startDate` (optional): Filter from date (YYYY-MM-DD)
- `endDate` (optional): Filter to date (YYYY-MM-DD)

**Response** (200 OK):
```json
{
    "locations": [
        {
            "userEmail": "user@example.com",
            "latitude": 40.7128,
            "longitude": -74.0060,
            "insertionTimestamp": "2024-01-15 14:30:00"
        },
        {
            "userEmail": "user@example.com",
            "latitude": 40.7589,
            "longitude": -73.9851,
            "insertionTimestamp": "2024-01-15 15:00:00"
        }
    ],
    "totalCount": 25,
    "hasMore": false
}
```

**Implementation**:
```java
public void getLocationHistory(String userEmail, LocationCallback callback) {
    try {
        String url = BASE_URL + "/location?userEmail=" + URLEncoder.encode(userEmail, "UTF-8");
        HttpGet httpGet = new HttpGet(url);
        
        String token = getAuthToken();
        httpGet.setHeader("Authorization", "Bearer " + token);
        
        // Execute request
        executeLocationHistoryRequest(httpGet, callback);
    } catch (Exception e) {
        callback.onError("Failed to fetch location history");
    }
}
```

## Authentication Integration

### AWS Cognito Token Management

```java
public class ApiService {
    private String authToken;
    private long tokenExpirationTime;
    
    private String getAuthToken() {
        // Check if token is still valid
        if (authToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            return authToken;
        }
        
        // Get fresh token from Cognito
        try {
            AuthSession session = Amplify.Auth.fetchAuthSession().get();
            if (session.isSignedIn()) {
                AWSCognitoAuthSession cognitoSession = (AWSCognitoAuthSession) session;
                String token = cognitoSession.getAccessToken().getValue();
                
                // Cache token with expiration
                this.authToken = token;
                this.tokenExpirationTime = System.currentTimeMillis() + (50 * 60 * 1000); // 50 minutes
                
                return token;
            }
        } catch (Exception e) {
            Log.e("ApiService", "Failed to get auth token", e);
        }
        
        return null;
    }
}
```

### Session Validation

```java
public void validateSession(Context context) {
    CognitoAuthService.getInstance(context).isSignedIn(new CognitoAuthService.AuthCallback() {
        @Override
        public void onSuccess(String message) {
            // Session is valid, continue with API calls
            saveUserIdToPreferences(context);
        }
        
        @Override
        public void onError(String error) {
            // Session expired, redirect to authentication
            navigateToAuthentication(context);
        }
        
        @Override
        public void onConfirmationRequired(String username) {
            // Not used for session validation
        }
    }, context);
}
```

## Error Handling

### HTTP Status Code Handling

```java
private void handleHttpResponse(CloseableHttpResponse response, ApiCallback callback) {
    try {
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());
        
        switch (statusCode) {
            case 200:
                callback.onSuccess(responseBody);
                break;
            case 401:
                // Unauthorized - token expired
                clearAuthToken();
                callback.onError("Authentication expired. Please sign in again.");
                break;
            case 403:
                // Forbidden - subscription required
                callback.onError("Subscription required for this feature.");
                break;
            case 400:
                // Bad request - invalid data
                callback.onError("Invalid request data.");
                break;
            case 500:
                // Server error
                callback.onError("Server error. Please try again later.");
                break;
            default:
                callback.onError("Unexpected error (HTTP " + statusCode + ")");
        }
    } catch (Exception e) {
        callback.onError("Failed to process response: " + e.getMessage());
    }
}
```

### Network Error Handling

```java
public void executeWithRetry(HttpUriRequest request, ApiCallback callback, int maxRetries) {
    CompletableFuture.supplyAsync(() -> {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return httpClient.execute(request);
            } catch (ConnectTimeoutException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                lastException = e;
                break; // Don't retry for other IO exceptions
            }
        }
        throw new RuntimeException("Request failed after " + (maxRetries + 1) + " attempts", lastException);
    }).thenAccept(response -> {
        handleHttpResponse(response, callback);
    }).exceptionally(throwable -> {
        callback.onError("Network error: " + throwable.getMessage());
        return null;
    });
}
```

## Data Transfer Objects

### SubscriptionRequest.java
```java
public class SubscriptionRequest {
    private String userId;
    private String email;
    private String subStartDate;
    private String subEndDate;
    private String insertionTimestamp;
    
    // Constructors
    public SubscriptionRequest() {}
    
    public SubscriptionRequest(String userId, String email, String subStartDate, String subEndDate) {
        this.userId = userId;
        this.email = email;
        this.subStartDate = subStartDate;
        this.subEndDate = subEndDate;
        this.insertionTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
    }
    
    // Getters and Setters
    // ... (standard getters/setters)
}
```

### Location.java
```java
public class Location {
    private String userEmail;
    private double latitude;
    private double longitude;
    private String insertionTimestamp;
    
    // Validation methods
    public boolean isValid() {
        return userEmail != null && !userEmail.isEmpty() &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180 &&
               insertionTimestamp != null && !insertionTimestamp.isEmpty();
    }
    
    // Factory method
    public static Location create(String email, double lat, double lng) {
        Location location = new Location();
        location.setUserEmail(email);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setInsertionTimestamp(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())
        );
        return location;
    }
}
```

## Callback Interfaces

### ApiService Callbacks
```java
public interface ApiCallback {
    void onSuccess(String message);
    void onError(String error);
}

public interface UserCallback {
    void onSubscriptionCheckSuccess(User user);
    void onSubscriptionCheckError(String error);
}

public interface LocationCallback {
    void onLocationHistorySuccess(List<Location> locations);
    void onError(String error);
}
```

## Testing API Integration

### Unit Tests for API Service
```java
@Test
public void testLocationSubmission() {
    // Mock HTTP client
    CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
    StatusLine mockStatusLine = mock(StatusLine.class);
    
    // Setup mocks
    when(mockStatusLine.getStatusCode()).thenReturn(200);
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
    
    // Test location submission
    ApiService apiService = new ApiService(context);
    apiService.setHttpClient(mockClient); // Inject mock
    
    Location testLocation = Location.create("test@example.com", 40.7128, -74.0060);
    
    apiService.postLocation(testLocation, new ApiService.ApiCallback() {
        @Override
        public void onSuccess(String message) {
            // Verify success
            assertNotNull(message);
            testPassed = true;
        }
        
        @Override
        public void onError(String error) {
            fail("Expected success but got error: " + error);
        }
    });
    
    assertTrue(testPassed);
}
```

### Integration Tests
```java
@Test
public void testAuthenticationFlow() {
    // Test complete flow from authentication to API call
    // 1. Authenticate with Cognito
    // 2. Get session token
    // 3. Make API call with token
    // 4. Verify successful response
}
```

## Performance Considerations

### Connection Pooling
```java
private void configureConnectionManager() {
    PoolingHttpClientConnectionManager connectionManager = 
        new PoolingHttpClientConnectionManager();
    
    // Configure pool settings
    connectionManager.setMaxTotal(20);
    connectionManager.setDefaultMaxPerRoute(10);
    connectionManager.closeExpiredConnections();
    connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
    
    this.httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .build();
}
```

### Request Timeout Configuration
```java
private RequestConfig getRequestConfig() {
    return RequestConfig.custom()
        .setConnectTimeout(30, TimeUnit.SECONDS)
        .setResponseTimeout(30, TimeUnit.SECONDS)
        .setConnectionRequestTimeout(30, TimeUnit.SECONDS)
        .build();
}
```

## Security Best Practices

### Token Security
- Store tokens securely using Android Keystore
- Implement token refresh mechanism
- Clear tokens on app logout

### Request Validation
- Validate all input data before API calls
- Sanitize user inputs
- Implement request signing for critical operations

### Error Information
- Don't expose sensitive information in error messages
- Log detailed errors server-side only
- Provide user-friendly error messages

This API documentation provides comprehensive coverage of all backend integration aspects for the LocationTracker application.
