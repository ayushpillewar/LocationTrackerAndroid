# LocationTracker Technical Documentation

## Code Architecture & Design Patterns

### Design Patterns Used

#### 1. Singleton Pattern
**CognitoAuthService**, **PreferenceManager**, **CacheLocations**
- Ensures single instance across app lifecycle
- Centralizes state management
- Provides global access to services

```java
public static CognitoAuthService getInstance(Context context) {
    if (instance == null) {
        synchronized (CognitoAuthService.class) {
            if (instance == null) {
                instance = new CognitoAuthService(context);
            }
        }
    }
    return instance;
}
```

#### 2. Observer Pattern
**Callback Interfaces** for asynchronous operations
- Decouples components
- Enables reactive programming
- Handles async responses

```java
public interface AuthCallback {
    void onSuccess(String message);
    void onError(String error);
    void onConfirmationRequired(String username);
}
```

#### 3. Factory Pattern
**Service Creation** and **Fragment Management**
- Encapsulates object creation
- Provides flexible instantiation
- Simplifies dependency management

#### 4. Strategy Pattern
**Authentication Methods** (Cognito, Google Sign-In)
- Interchangeable authentication strategies
- Extensible for future auth methods
- Clean separation of concerns

### Application Flow Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Splash/Auth   │───▶│  Subscription   │───▶│   MainActivity  │
│  PinLockActivity│    │    Activity     │    │  (Home/Cloud)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   AWS Cognito   │    │  Play Billing   │    │ Location Service│
│ Authentication  │    │    Manager      │    │   Background    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Service Layer Documentation

### LocationTrackingService
**Purpose**: Background location monitoring and API communication

**Key Responsibilities**:
- GPS location tracking using FusedLocationProviderClient
- Scheduled location data transmission
- Foreground service management
- Error handling and logging

**Implementation Details**:
```java
public class LocationTrackingService extends Service {
    // Core Components
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ScheduledExecutorService scheduler;
    
    // Service Lifecycle
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle START/STOP commands
        // Initialize location tracking
        // Return START_STICKY for persistence
    }
    
    // Location Updates
    private void scheduleLocationAPI() {
        scheduler.scheduleWithFixedDelay(() -> {
            sendLocationToAPI();
        }, 1, timeIntervalMinutes, TimeUnit.MINUTES);
    }
}
```

### CognitoAuthService
**Purpose**: AWS Cognito authentication management

**Key Features**:
- Sign up/Sign in operations
- Email verification
- Password reset functionality
- Session management

**Security Implementation**:
```java
public void signUp(String username, String email, String password, AuthCallback callback) {
    AuthSignUpOptions options = AuthSignUpOptions.builder()
        .userAttribute(AuthUserAttributeKey.email(), email)
        .build();
    
    Amplify.Auth.signUp(username, password, options, /* callbacks */);
}
```

### ApiService
**Purpose**: Backend API communication

**HTTP Client Configuration**:
- Custom SSL context for security
- Connection pooling for efficiency
- Timeout management
- Authentication token handling

```java
public class ApiService {
    private CloseableHttpClient httpClient;
    
    private void initializeHttpClient() {
        httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();
    }
}
```

## Data Layer Documentation

### Data Transfer Objects (DTOs)

#### User.java
```java
public class User {
    private String userId;        // Unique identifier
    private String email;         // User email address
    private String subStartDate;  // Subscription start date
    private String subEndDate;    // Subscription end date
    private String insertionTimestamp; // Record creation time
}
```

#### Location.java
```java
public class Location {
    private String userEmail;     // Owner identification
    private double latitude;      // GPS latitude
    private double longitude;     // GPS longitude
    private String insertionTimestamp; // Location capture time
}
```

### Data Persistence Strategy

#### SharedPreferences (PreferenceManager)
- User settings and preferences
- Authentication tokens
- App configuration

#### In-Memory Cache (CacheLocations)
- Recent location data
- Performance optimization
- Thread-safe operations

```java
public class CacheLocations {
    private static final List<Location> locationCache = 
        Collections.synchronizedList(new ArrayList<>());
    
    public static synchronized void addLocation(Location location) {
        locationCache.add(location);
        if (locationCache.size() > MAX_CACHE_SIZE) {
            locationCache.remove(0); // Remove oldest
        }
    }
}
```

## UI Layer Documentation

### Activity Structure

#### PinLockActivity (Authentication)
**State Management**: Multiple authentication modes
```java
private enum AuthMode {
    SIGN_IN, SIGN_UP, EMAIL_VERIFICATION, 
    FORGOT_PASSWORD, RESET_PASSWORD
}
```

**UI Updates**: Dynamic interface based on current mode
```java
private void updateUIForMode(AuthMode mode) {
    switch (mode) {
        case SIGN_IN:
            // Configure sign-in UI
        case SIGN_UP:
            // Configure sign-up UI
        // ... other modes
    }
}
```

#### MainActivity (Core Interface)
**Fragment Management**: Bottom navigation with fragment switching
```java
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private Fragment currentFragment;
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }
}
```

### Fragment Architecture

#### HomeFragment
**Purpose**: Location tracking control and status display
- Track start/stop functionality
- Real-time status updates
- Settings configuration

#### CloudFragment
**Purpose**: Location history and map visualization
- RecyclerView for location list
- Google Maps integration
- Navigation to external maps

### UI Components & Validation

#### Input Validation (ValidationUtils)
```java
public static boolean isValidEmail(String email) {
    return email != null && 
           Patterns.EMAIL_ADDRESS.matcher(email).matches();
}

public static boolean isValidPassword(String password) {
    return password != null && 
           password.length() >= 8 &&
           hasUpperCase(password) &&
           hasSpecialCharacter(password);
}
```

## Security Implementation

### Authentication Security
- **AWS Cognito**: Enterprise-grade authentication
- **Multi-Factor Authentication**: Email verification
- **Secure Token Management**: Automatic token refresh
- **Session Validation**: Regular authentication checks

### Data Security
- **HTTPS Communication**: All API calls encrypted
- **Input Sanitization**: SQL injection prevention
- **Permission Management**: Runtime permission handling
- **Secure Storage**: Encrypted local data storage

### Network Security
```java
private SSLContext createSSLContext() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, null, new SecureRandom());
    return sslContext;
}
```

## Background Processing

### Service Management
```java
// Foreground Service for Location Tracking
private void startForegroundService() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Location Tracking Active")
        .setContentText("Tracking your location...")
        .setSmallIcon(R.drawable.ic_location)
        .setPriority(NotificationCompat.PRIORITY_LOW);
        
    startForeground(NOTIFICATION_ID, builder.build());
}
```

### Scheduled Operations
```java
// Scheduler for periodic API calls
private void scheduleLocationAPI() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleWithFixedDelay(() -> {
        try {
            sendLocationToAPI();
        } catch (Exception e) {
            Log.e(TAG, "Scheduled task error", e);
        }
    }, 1, timeIntervalMinutes, TimeUnit.MINUTES);
}
```

## Error Handling Strategy

### Exception Management
```java
public class LocationTrackingService extends Service {
    private void handleLocationError(Exception e) {
        Log.e(TAG, "Location error: " + e.getMessage(), e);
        
        // Notify user if critical
        if (e instanceof SecurityException) {
            showPermissionError();
        } else {
            scheduleRetry();
        }
    }
}
```

### Callback Error Handling
```java
public void postLocation(Location location, ApiCallback callback) {
    try {
        // Make API call
        executeHttpRequest();
    } catch (ConnectTimeoutException e) {
        callback.onError("Network timeout - please check connection");
    } catch (IOException e) {
        callback.onError("Network error - please try again");
    } catch (Exception e) {
        callback.onError("Unexpected error occurred");
    }
}
```

## Performance Optimizations

### Memory Management
- **Object Pooling**: Reuse expensive objects
- **Lazy Loading**: Initialize components when needed
- **Cache Management**: Limited size with LRU eviction
- **Garbage Collection**: Minimize object creation

### Battery Optimization
- **Location Strategy**: Balanced accuracy and power consumption
- **Background Limits**: Respect Android battery optimization
- **Smart Updates**: Reduce frequency when stationary
- **Doze Mode**: Handle system sleep states

### Network Optimization
```java
// Efficient HTTP connection management
private void configureHttpClient() {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(30, TimeUnit.SECONDS)
        .setResponseTimeout(30, TimeUnit.SECONDS)
        .build();
        
    PoolingHttpClientConnectionManager connectionManager = 
        new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(20);
    connectionManager.setDefaultMaxPerRoute(10);
}
```

## Testing Strategy

### Unit Testing
```java
@Test
public void testLocationCaching() {
    // Given
    Location testLocation = new Location();
    testLocation.setLatitude(40.7128);
    testLocation.setLongitude(-74.0060);
    
    // When
    CacheLocations.addLocation(testLocation);
    
    // Then
    assertEquals(1, CacheLocations.getCachedLocations().size());
}
```

### Integration Testing
```java
@Test
public void testAuthenticationFlow() {
    // Test complete sign-up process
    // Verify email validation
    // Confirm success navigation
}
```

### Mocking Strategy
```java
@Mock
private ApiService mockApiService;

@Test
public void testLocationSubmission() {
    // Mock API responses
    when(mockApiService.postLocation(any(), any()))
        .thenAnswer(invocation -> {
            ApiService.ApiCallback callback = invocation.getArgument(1);
            callback.onSuccess("Location saved");
            return null;
        });
}
```

## Constants Management

### AppConstants.java
Centralized configuration management:
```java
public class AppConstants {
    // Timing Constants
    public static final int MIN_TIME_INTERVAL_MINUTES = 10;
    public static final int MAX_TIME_INTERVAL_MINUTES = 720;
    
    // Service Constants
    public static final String NOTIFICATION_CHANNEL_ID = "LocationTrackingChannel";
    public static final int LOCATION_SERVICE_NOTIFICATION_ID = 1001;
    
    // Permission Request Codes
    public static final int PERMISSION_REQUEST_LOCATION = 1000;
    public static final int PERMISSION_REQUEST_NOTIFICATION = 1002;
    
    // Error Messages
    public static final String ERROR_LOCATION_PERMISSION = 
        "Location permission is required for this app.";
    
    // Private constructor prevents instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```

## Build Configuration

### Gradle Configuration
```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "11"
    }
    
    // Resource packaging
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE"
            )
        }
    }
}
```

### ProGuard Configuration
```proguard
# Keep AWS SDK classes
-keep class com.amazonaws.** { *; }
-keep class com.amplifyframework.** { *; }

# Keep Google Services
-keep class com.google.android.gms.** { *; }

# Keep DTOs for JSON serialization
-keep class com.majboormajdoor.locationtracker.dto.** { *; }
```

This technical documentation provides comprehensive coverage of the LocationTracker application's architecture, implementation details, and development guidelines.
