# LocationTracker Android Application

## Overview

LocationTracker is a comprehensive Android application that enables real-time location tracking and sharing. The application allows users to securely authenticate, track their location, and share location data via email notifications. It features subscription-based access, AWS Cognito authentication, Google Maps integration, and robust background location tracking.

## Features

### 🔐 Authentication System
- **AWS Cognito Integration**: Secure user authentication with email verification
- **Google Sign-In Support**: Alternative authentication method
- **Password Reset**: Complete forgot password flow
- **Email Verification**: Two-factor authentication for account security

### 📍 Location Tracking
- **Real-time GPS Tracking**: Continuous location monitoring using Google Play Services
- **Background Service**: Location tracking continues even when app is minimized
- **Customizable Intervals**: Track location every 10 minutes to 12 hours
- **Email Notifications**: Automatic location sharing via email

### 🗺️ Maps & Visualization
- **Google Maps Integration**: View tracked locations on interactive maps
- **Location History**: Browse through previously tracked locations
- **Map Navigation**: Open locations directly in Google Maps

### 💰 Subscription Management
- **Google Play Billing**: Integrated subscription system
- **$5 Monthly Subscription**: Premium access to all features
- **Subscription Validation**: Server-side and client-side validation

### 🏠 User Interface
- **Modern Material Design**: Clean and intuitive interface
- **Bottom Navigation**: Easy access to different app sections
- **Dark/Light Theme Support**: Responsive UI design
- **Real-time Updates**: Live location and status updates

### 🔧 Technical Features
- **Background Processing**: Robust service architecture
- **Permission Management**: Smart permission handling
- **Data Caching**: Efficient local data storage
- **Error Handling**: Comprehensive error management
- **Notification System**: System notifications for tracking status

## Architecture

### Project Structure
```
app/
├── src/main/java/com/majboormajdoor/locationtracker/
│   ├── activities/          # Android Activities
│   │   ├── MainActivity.java
│   │   ├── PinLockActivity.java
│   │   └── SubscriptionActivity.java
│   ├── fragments/           # UI Fragments
│   │   ├── HomeFragment.java
│   │   └── CloudFragment.java
│   ├── services/           # Background Services & API
│   │   ├── LocationTrackingService.java
│   │   ├── ApiService.java
│   │   ├── CognitoAuthService.java
│   │   ├── GoogleSignInService.java
│   │   └── NavigatorService.java
│   ├── dto/                # Data Transfer Objects
│   │   ├── User.java
│   │   ├── Location.java
│   │   └── SubscriptionRequest.java
│   ├── utils/              # Utility Classes
│   │   ├── PreferenceManager.java
│   │   ├── PermissionUtils.java
│   │   ├── ValidationUtils.java
│   │   └── CacheLocations.java
│   ├── billing/            # In-app Billing
│   │   └── BillingManager.java
│   ├── adapters/           # RecyclerView Adapters
│   │   └── LocationAdapter.java
│   └── constants/          # Application Constants
│       └── AppConstants.java
└── res/                    # Resources
    ├── layout/             # XML Layouts
    ├── drawable/           # Images and Graphics
    ├── values/             # Strings, Colors, Styles
    └── xml/                # Configuration Files
```

## Technical Implementation

### Authentication Flow
1. **Initial Launch**: Check for existing authentication
2. **Sign Up/Sign In**: Email and password validation
3. **Email Verification**: AWS Cognito verification code
4. **Subscription Check**: Validate active subscription
5. **Main Access**: Navigate to core application features

### Location Tracking Process
1. **Permission Request**: Request location permissions
2. **Service Initialization**: Start background location service
3. **GPS Monitoring**: Continuous location updates using FusedLocationProviderClient
4. **Data Processing**: Format and validate location data
5. **API Submission**: Send location data to backend server
6. **Email Notification**: Automated email with Google Maps link

### Data Management
- **Local Storage**: SharedPreferences for user settings
- **In-Memory Cache**: Recent locations cached for quick access
- **Cloud Sync**: Real-time synchronization with backend server
- **Offline Support**: Local data persistence during network issues

## Dependencies & Libraries

### Core Android
- **AndroidX Components**: Latest Android support libraries
- **Material Design**: Modern UI components
- **ConstraintLayout**: Responsive layouts
- **Fragment Navigation**: Navigation component

### Location Services
- **Google Play Services Location**: GPS and location APIs
- **FusedLocationProviderClient**: Efficient location tracking
- **Background Location**: Foreground service for continuous tracking

### Authentication
- **AWS Amplify**: Cognito authentication SDK
- **Google Sign-In**: Google authentication services
- **Credentials Manager**: Modern authentication framework

### Networking & Data
- **Apache HttpClient5**: HTTP client for API calls
- **Jackson Databind**: JSON serialization/deserialization
- **Gson**: Additional JSON processing

### Billing & Subscriptions
- **Google Play Billing**: In-app purchase and subscription management

### Testing
- **JUnit**: Unit testing framework
- **Mockito**: Mocking framework for tests
- **Espresso**: UI testing framework
- **Robolectric**: Android unit testing

## Configuration

### AWS Cognito Setup
```java
// Configure in CognitoAuthService
private static final String USER_POOL_ID = "your-user-pool-id";
private static final String APP_CLIENT_ID = "your-app-client-id";
private static final String REGION = "your-aws-region";
```

### Google Services
- Add `google-services.json` to `app/` directory
- Configure Google Sign-In client ID
- Enable Google Maps API key in AndroidManifest.xml

### API Configuration
```java
// Backend API endpoints in ApiService
private static final String BASE_URL = "your-backend-url";
private static final String LOCATION_ENDPOINT = "/location";
private static final String USER_ENDPOINT = "/user";
```

## Installation & Setup

### Prerequisites
1. **Android Studio**: Latest version with SDK 24+ support
2. **AWS Account**: For Cognito authentication setup
3. **Google Cloud Console**: For Maps API and authentication
4. **Backend Server**: API server for location data storage

### Build Instructions
1. Clone the repository
2. Open project in Android Studio
3. Configure AWS Cognito credentials
4. Add Google Services configuration
5. Set up backend API endpoints
6. Build and run the application

### Environment Setup
```bash
# Clone repository
git clone <repository-url>

# Open in Android Studio
# File -> Open -> Select project directory

# Configure API keys and endpoints
# Update configuration files as needed

# Build project
./gradlew assembleDebug
```

## Usage Guide

### Getting Started
1. **Launch App**: Open LocationTracker application
2. **Create Account**: Sign up with email and password
3. **Verify Email**: Enter verification code from email
4. **Subscribe**: Purchase monthly subscription
5. **Grant Permissions**: Allow location access
6. **Start Tracking**: Begin location monitoring

### Core Features
- **Home Tab**: Control location tracking, view current status
- **History Tab**: Browse tracked location history
- **Settings**: Configure tracking intervals and preferences
- **Maps**: View locations on interactive Google Maps

### Location Tracking
1. Enter email address for notifications
2. Set tracking interval (10 minutes to 12 hours)
3. Tap "Start Tracking" button
4. Monitor status in notification area
5. Receive location emails at specified intervals

## API Reference

### Location Data Structure
```java
public class Location {
    private String userEmail;
    private double latitude;
    private double longitude;
    private String insertionTimestamp;
    // Getters and setters
}
```

### User Data Structure
```java
public class User {
    private String userId;
    private String email;
    private String subStartDate;
    private String subEndDate;
    private String insertionTimestamp;
    // Getters and setters
}
```

### Service Interfaces
```java
// Authentication callback
public interface AuthCallback {
    void onSuccess(String message);
    void onError(String error);
    void onConfirmationRequired(String username);
}

// API callback
public interface ApiCallback {
    void onSuccess(String message);
    void onError(String error);
}
```

## Security Features

### Data Protection
- **Encrypted Communications**: HTTPS for all API calls
- **Secure Authentication**: AWS Cognito with MFA
- **Permission Validation**: Runtime permission checks
- **Input Sanitization**: Validation of all user inputs

### Privacy Considerations
- **Location Data**: Encrypted transmission and storage
- **User Consent**: Clear permission requests
- **Data Retention**: Configurable data retention policies
- **GDPR Compliance**: Privacy policy and data handling

## Testing

### Unit Tests
```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests CacheLocationsTest
```

### Integration Tests
```bash
# Run instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
- **Authentication**: Complete auth flow testing
- **Location Services**: GPS and tracking validation
- **API Integration**: Backend communication testing
- **UI Components**: User interface testing

## Troubleshooting

### Common Issues

#### Location Not Updating
- Verify location permissions granted
- Check GPS/location services enabled
- Ensure background app refresh allowed
- Verify network connectivity

#### Authentication Problems
- Validate AWS Cognito configuration
- Check internet connection
- Verify email format and password requirements
- Confirm user pool settings

#### Subscription Issues
- Verify Google Play Console setup
- Check billing permissions
- Validate subscription product IDs
- Confirm payment method

#### Background Service Issues
- Check battery optimization settings
- Verify foreground service permissions
- Ensure notification channel enabled
- Review system resource constraints

## Performance Optimization

### Battery Efficiency
- **Smart Location Updates**: Optimized GPS usage
- **Background Limits**: Efficient service management
- **Doze Mode Compatibility**: Android battery optimization support

### Memory Management
- **Efficient Caching**: Limited in-memory storage
- **Resource Cleanup**: Proper service lifecycle management
- **Garbage Collection**: Optimized object creation

### Network Optimization
- **Batch Requests**: Efficient API communication
- **Retry Logic**: Smart network error handling
- **Offline Support**: Local data caching

## Contributing

### Development Guidelines
1. Follow Android coding standards
2. Maintain comprehensive documentation
3. Write unit tests for new features
4. Follow Git workflow best practices
5. Update documentation with changes

### Code Style
- **Java Conventions**: Oracle Java coding standards
- **Android Guidelines**: Material Design principles
- **Documentation**: JavaDoc for public methods
- **Error Handling**: Comprehensive exception management

## License

This project is proprietary software. All rights reserved.

## Support

For technical support and questions:
- **Documentation**: Reference this README
- **Issues**: Report bugs through issue tracking
- **Support**: Contact development team

## Version History

### Version 11 (Current)
- AWS Cognito authentication
- Subscription management
- Google Maps integration
- Background location tracking
- Email notifications
- Comprehensive error handling

### Future Enhancements
- Multiple device synchronization
- Advanced location analytics
- Geofencing capabilities
- Social sharing features
- Enhanced security features

---

**LocationTracker** - Comprehensive location tracking and sharing solution for Android devices.
