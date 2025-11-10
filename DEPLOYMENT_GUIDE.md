# LocationTracker Deployment & Configuration Guide

## Prerequisites

### Development Environment
- **Android Studio**: Arctic Fox or later (recommended: Latest stable)
- **Java Development Kit**: JDK 11 or higher
- **Android SDK**: API level 24+ (Android 7.0) to API level 35 (Android 15)
- **Gradle**: Version 8.0+ (managed by Android Studio)
- **Git**: For version control

### External Services Setup
- **AWS Account**: For Cognito authentication services
- **Google Cloud Console**: For Maps API and authentication
- **Google Play Console**: For app publishing and billing
- **Backend Server**: API server for data storage

## AWS Cognito Configuration

### 1. Create User Pool

#### Step 1: Create User Pool in AWS Console
1. Navigate to AWS Cognito Console
2. Click "Create User Pool"
3. Configure the following settings:

**Authentication Options**:
```
☑️ Email
☐ Phone number  
☐ Username
```

**Password Policy**:
```
Minimum length: 8 characters
☑️ Require numbers
☑️ Require special characters
☑️ Require uppercase letters
☑️ Require lowercase letters
```

**Multi-Factor Authentication**:
```
☑️ Optional MFA
☑️ SMS
☑️ TOTP
```

#### Step 2: Configure User Pool Settings
```json
{
  "UserPoolId": "us-east-1_XXXXXXXXX",
  "UserPoolName": "LocationTrackerUsers",
  "Policies": {
    "PasswordPolicy": {
      "MinimumLength": 8,
      "RequireUppercase": true,
      "RequireLowercase": true,
      "RequireNumbers": true,
      "RequireSymbols": true
    }
  },
  "AutoVerifiedAttributes": ["email"],
  "Schema": [
    {
      "AttributeDataType": "String",
      "Name": "email",
      "Required": true,
      "Mutable": true
    }
  ]
}
```

#### Step 3: Create App Client
1. In User Pool settings, go to "App integration"
2. Create new App Client:

```json
{
  "AppClientName": "LocationTrackerApp",
  "AppClientId": "your-app-client-id",
  "ClientSecret": null,
  "RefreshTokenValidity": 30,
  "AccessTokenValidity": 60,
  "IdTokenValidity": 60,
  "TokenValidityUnits": {
    "RefreshToken": "days",
    "AccessToken": "minutes",
    "IdToken": "minutes"
  },
  "ExplicitAuthFlows": [
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH"
  ]
}
```

### 2. Configure Application

#### Add Amplify Configuration
Create `app/src/main/res/raw/amplifyconfiguration.json`:
```json
{
  "auth": {
    "plugins": {
      "awsCognitoAuthPlugin": {
        "UserAgent": "aws-amplify-cli/0.1.0",
        "Version": "0.1.0",
        "IdentityManager": {
          "Default": {}
        },
        "CredentialsProvider": {
          "CognitoIdentity": {
            "Default": {
              "PoolId": "us-east-1:your-identity-pool-id",
              "Region": "us-east-1"
            }
          }
        },
        "CognitoUserPool": {
          "Default": {
            "PoolId": "us-east-1_XXXXXXXXX",
            "AppClientId": "your-app-client-id",
            "Region": "us-east-1"
          }
        },
        "Auth": {
          "Default": {
            "authenticationFlowType": "USER_SRP_AUTH"
          }
        }
      }
    }
  }
}
```

#### Update CognitoAuthService Configuration
```java
public class CognitoAuthService {
    private static final String USER_POOL_ID = "us-east-1_XXXXXXXXX";
    private static final String APP_CLIENT_ID = "your-app-client-id";
    private static final String REGION = "us-east-1";
    
    // Initialize Amplify in Application onCreate()
    public static void initializeAmplify(Context context) {
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(context);
        } catch (AmplifyException e) {
            Log.e("AmplifyInit", "Could not initialize Amplify", e);
        }
    }
}
```

## Google Services Configuration

### 1. Google Cloud Console Setup

#### Enable Required APIs
1. Google Maps Android API
2. Google Sign-In API
3. Places API (if using location search)

#### Create Credentials
1. **API Key** for Google Maps:
   - Restrict to Android apps
   - Add package name and SHA-1 certificate fingerprint
   - Restrict API access to Maps Android API

2. **OAuth 2.0 Client ID** for Google Sign-In:
   - Application type: Android
   - Package name: `com.majboormajdoor.locationtracker`
   - SHA-1 certificate fingerprint

#### Get SHA-1 Fingerprint
```bash
# Debug keystore (development)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release keystore (production)
keytool -list -v -keystore /path/to/your/release.keystore -alias your-alias
```

### 2. Add Google Services to Project

#### Download google-services.json
1. Go to Firebase Console or Google Cloud Console
2. Create/select your project
3. Add Android app with package name `com.majboormajdoor.locationtracker`
4. Download `google-services.json`
5. Place in `app/` directory

#### Configure Google Maps API Key
Add to `app/src/main/AndroidManifest.xml`:
```xml
<application>
    <!-- Google Maps API Key -->
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_GOOGLE_MAPS_API_KEY" />
        
    <!-- Google Sign-In -->
    <meta-data
        android:name="com.google.android.gms.version"
        android:value="@integer/google_play_services_version" />
</application>
```

## Backend API Configuration

### 1. API Endpoints Configuration

#### Update ApiService Base URL
```java
public class ApiService {
    // Development environment
    private static final String BASE_URL_DEV = "http://10.0.2.2:8080/api";
    
    // Staging environment  
    private static final String BASE_URL_STAGING = "https://staging-api.yourdomain.com/api";
    
    // Production environment
    private static final String BASE_URL_PROD = "https://api.yourdomain.com/api";
    
    private static final String BASE_URL = BuildConfig.DEBUG ? BASE_URL_DEV : BASE_URL_PROD;
}
```

#### Build Variants Configuration
Add to `app/build.gradle.kts`:
```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api\"")
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
            isDebuggable = true
        }
        
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.yourdomain.com/api\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        
        create("staging") {
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.yourdomain.com/api\"")
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")
            isDebuggable = true
        }
    }
}
```

### 2. SSL Certificate Configuration

#### Custom SSL Context for API Calls
```java
private void configureSslContext() {
    try {
        // For production: use system default trust store
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());
        
        // For custom certificates (if needed)
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        trustManagerFactory.init((KeyStore) null);
        
        this.sslContext = sslContext;
    } catch (Exception e) {
        Log.e("ApiService", "SSL configuration failed", e);
    }
}
```

## Google Play Store Configuration

### 1. App Signing Setup

#### Upload Key Certificate
1. Create release keystore:
```bash
keytool -genkey -v -keystore release-key.keystore -alias my-alias -keyalg RSA -keysize 2048 -validity 10000
```

2. Configure signing in `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your-keystore-password"
            keyAlias = "my-alias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your-key-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### 2. Google Play Billing Configuration

#### Create Subscription Products
1. Go to Google Play Console
2. Navigate to Monetization → Subscriptions
3. Create subscription product:

```json
{
  "productId": "monthly_subscription_5usd",
  "name": "Monthly Location Tracking",
  "description": "Access to premium location tracking features",
  "subscriptionPeriod": "P1M",
  "price": {
    "currency": "USD",
    "amount": "5.00"
  },
  "trialPeriod": "P3D",
  "gracePeriod": "P3D"
}
```

#### Update BillingManager Configuration
```java
public class BillingManager {
    // Product IDs
    public static final String MONTHLY_SUBSCRIPTION_ID = "monthly_subscription_5usd";
    
    // Test product ID for development
    public static final String TEST_SUBSCRIPTION_ID = "android.test.purchased";
    
    private static final String getCurrentProductId() {
        return BuildConfig.DEBUG ? TEST_SUBSCRIPTION_ID : MONTHLY_SUBSCRIPTION_ID;
    }
}
```

### 3. Play Console Upload Configuration

#### App Bundle Generation
```bash
# Generate release bundle
./gradlew bundleRelease

# Generated file location
app/build/outputs/bundle/release/app-release.aab
```

## Permissions & Security Configuration

### 1. Android Permissions

#### Required Permissions in AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    
    <!-- Service permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    
    <!-- Notification permissions -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- System permissions -->
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Billing permission -->
    <uses-permission android:name="com.android.vending.BILLING" />
</manifest>
```

#### Runtime Permission Handling
```java
public class PermissionUtils {
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    };
    
    @TargetApi(Build.VERSION_CODES.Q)
    private static final String[] BACKGROUND_PERMISSIONS = {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    
    public static void requestAllPermissions(Activity activity, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                permissionsToRequest.toArray(new String[0]), requestCode);
        }
    }
}
```

### 2. ProGuard Configuration

#### ProGuard Rules (proguard-rules.pro)
```proguard
# AWS SDK
-keep class com.amazonaws.** { *; }
-keep class com.amplifyframework.** { *; }
-dontwarn com.amazonaws.**

# Google Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Play Billing
-keep class com.android.billingclient.api.** { *; }

# Jackson JSON processor
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# Application DTOs
-keep class com.majboormajdoor.locationtracker.dto.** { *; }

# Apache HTTP Client
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**

# Application classes with native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Keep callback interfaces
-keep interface com.majboormajdoor.locationtracker.services.*$*Callback {
    <methods>;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}
```

## Build & Deployment Process

### 1. Development Build
```bash
# Debug build
./gradlew assembleDebug

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Release Build Process
```bash
# Clean project
./gradlew clean

# Generate release bundle
./gradlew bundleRelease

# Generate release APK (if needed)
./gradlew assembleRelease

# Verify APK
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

### 3. Testing Before Release
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## Environment-Specific Configuration

### 1. Development Environment
```java
// Development configuration
public class DevConfig {
    public static final String API_BASE_URL = "http://10.0.2.2:8080/api";
    public static final String AWS_REGION = "us-east-1";
    public static final boolean ENABLE_LOGGING = true;
    public static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds for testing
}
```

### 2. Staging Environment  
```java
// Staging configuration
public class StagingConfig {
    public static final String API_BASE_URL = "https://staging-api.yourdomain.com/api";
    public static final String AWS_REGION = "us-east-1";
    public static final boolean ENABLE_LOGGING = true;
    public static final int LOCATION_UPDATE_INTERVAL = 60000; // 1 minute
}
```

### 3. Production Environment
```java
// Production configuration
public class ProductionConfig {
    public static final String API_BASE_URL = "https://api.yourdomain.com/api";
    public static final String AWS_REGION = "us-east-1";
    public static final boolean ENABLE_LOGGING = false;
    public static final int LOCATION_UPDATE_INTERVAL = 600000; // 10 minutes minimum
}
```

## Monitoring & Analytics

### 1. Crashlytics Integration
Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.firebase:firebase-crashlytics:18.6.0")
    implementation("com.google.firebase:firebase-analytics:21.5.0")
}

plugins {
    id("com.google.firebase.crashlytics")
}
```

### 2. Custom Logging
```java
public class AppLogger {
    private static final String TAG = "LocationTracker";
    
    public static void logLocationUpdate(double lat, double lng) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Location updated: %.6f, %.6f", lat, lng));
        }
    }
    
    public static void logError(String operation, Exception e) {
        Log.e(TAG, "Error in " + operation, e);
        
        // Send to crash reporting in production
        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}
```

## Troubleshooting Deployment Issues

### Common Issues & Solutions

#### 1. Build Errors
```bash
# Clear build cache
./gradlew clean
./gradlew build --refresh-dependencies

# Check for dependency conflicts
./gradlew app:dependencies
```

#### 2. Signing Issues
```bash
# Verify keystore
keytool -list -v -keystore release-key.keystore

# Check signing configuration
./gradlew signingReport
```

#### 3. Permission Issues
- Ensure all required permissions are declared
- Test permission flow on different Android versions
- Check for background permission restrictions

#### 4. API Integration Issues
- Verify network security configuration
- Check API endpoint accessibility
- Validate authentication tokens

This deployment guide provides comprehensive instructions for setting up and deploying the LocationTracker application across different environments.
