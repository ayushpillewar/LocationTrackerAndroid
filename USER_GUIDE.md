# LocationTracker User Guide

## Table of Contents
1. [Getting Started](#getting-started)
2. [Account Setup](#account-setup)
3. [Subscription Management](#subscription-management)
4. [Location Tracking](#location-tracking)
5. [Viewing Location History](#viewing-location-history)
6. [Settings & Configuration](#settings--configuration)
7. [Troubleshooting](#troubleshooting)
8. [Privacy & Security](#privacy--security)
9. [Frequently Asked Questions](#frequently-asked-questions)

## Getting Started

### What is LocationTracker?
LocationTracker is a comprehensive mobile application that enables real-time location tracking and sharing. Whether you need to share your location with family, track your routes, or monitor device location for security purposes, LocationTracker provides a secure and reliable solution.

### Key Features
- 🔒 **Secure Authentication**: AWS Cognito-powered login system
- 📍 **Real-time Tracking**: Continuous GPS location monitoring
- 📧 **Email Notifications**: Automatic location sharing via email
- 🗺️ **Google Maps Integration**: View locations on interactive maps
- ⏰ **Customizable Intervals**: Track from every 10 minutes to 12 hours
- 📊 **Location History**: Browse and analyze tracked locations
- 💰 **Subscription-based**: $5/month for premium features

### System Requirements
- **Android Version**: 7.0 (API level 24) or higher
- **Storage**: At least 50MB available space
- **Internet**: Active internet connection required
- **GPS**: Device must support location services
- **Google Play Services**: Required for location and billing features

## Account Setup

### 1. First Launch
When you first open LocationTracker, you'll see the welcome screen with authentication options.

### 2. Creating an Account

#### Step 1: Choose Sign Up
1. Open the LocationTracker app
2. Tap "Sign Up" at the bottom of the screen
3. You'll see the account creation form

#### Step 2: Enter Your Information
**Email Address**: 
- Enter a valid email address
- This will be used for login and location notifications
- Must be a real email as verification is required

**Password Requirements**:
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one number (0-9)
- At least one special character (!@#$%^&*)

**Confirm Password**:
- Re-enter your password to ensure accuracy

#### Step 3: Email Verification
1. After successful registration, check your email
2. You'll receive a verification code from AWS Cognito
3. Enter the 6-digit code in the app
4. Tap "Verify Email"

#### Step 4: Account Activation
Once verified, your account is created and you'll be redirected to the subscription page.

### 3. Signing In

#### Existing Users
1. Open the app
2. Enter your registered email and password
3. Tap "Sign In"
4. If successful, you'll proceed to the main application

#### Forgot Password
1. On the sign-in screen, tap "Forgot Password?"
2. Enter your registered email address
3. Check your email for a reset code
4. Enter the code and your new password
5. Tap "Reset Password"

## Subscription Management

### 1. Why Subscription?
LocationTracker operates on a subscription model to provide:
- Continuous location tracking services
- Secure cloud data storage
- Email notification services
- Google Maps integration
- Premium customer support

### 2. Subscription Plans

#### Monthly Plan
- **Price**: $5.00 USD per month
- **Features**: Full access to all LocationTracker features
- **Billing**: Automatic monthly renewal through Google Play
- **Trial**: 3-day free trial for new users

#### What's Included
✅ Unlimited location tracking
✅ Email notifications
✅ Location history storage
✅ Google Maps integration
✅ Multiple device sync
✅ Priority customer support

### 3. Managing Your Subscription

#### Subscribing
1. After creating your account, you'll see the subscription screen
2. Review the plan details
3. Tap "Subscribe for $5"
4. Complete the purchase through Google Play
5. Your subscription activates immediately

#### Checking Subscription Status
- Your subscription status is displayed on the home screen
- The app automatically validates your subscription

#### Canceling Subscription
1. Open Google Play Store app
2. Tap Menu → Subscriptions
3. Find LocationTracker subscription
4. Tap "Cancel Subscription"
5. Follow the cancellation steps

**Note**: You can continue using the app until the end of your current billing period.

## Location Tracking

### 1. Setting Up Tracking

#### Granting Permissions
Before tracking can begin, you must grant location permissions:

1. **Location Access**: Allow "All the time" for background tracking
2. **Notification Permission**: Enable to receive tracking status updates
3. **Background App Refresh**: Ensure the app can run in the background

#### First-Time Setup Wizard
The app will show an information popup explaining:
- How to start tracking
- How to view history
- How to test location sending
- Multiple device login capability

### 2. Starting Location Tracking

#### Basic Setup
1. Navigate to the **Home** tab
2. Enter your **email address** in the designated field
3. Set your **tracking interval** using the slider
   - Minimum: 10 minutes
   - Maximum: 12 hours (720 minutes)
   - Default: 10 minutes

#### Tracking Intervals
Choose based on your needs:
- **10-30 minutes**: High frequency for detailed tracking
- **1-2 hours**: Balanced tracking for general monitoring
- **6-12 hours**: Low frequency for basic location updates

#### Starting Tracking
1. Ensure your email and interval are set
2. Tap the **"Start Tracking"** button
3. The button will change to show "Stop Tracking"
4. You'll receive a notification confirming tracking has started

### 3. Understanding Tracking Status

#### Status Indicators
- **Green**: Tracking is active and working normally
- **Yellow**: Tracking is starting up or experiencing minor issues
- **Red**: Tracking has stopped or encountered an error

#### Notification Bar
- A persistent notification shows tracking status
- Displays last update time and interval
- Cannot be dismissed while tracking is active

### 4. Testing Your Setup

#### Test Location Button
Use the "Test Location" button to:
1. Send a one-time location update
2. Verify email delivery
3. Test your configuration before starting continuous tracking
4. Ensure everything is working correctly

## Viewing Location History

### 1. Accessing History

#### Cloud Tab
1. Navigate to the **Cloud** tab at the bottom of the screen
2. Your location history will automatically load
3. If no data is available, ensure you've started tracking

### 2. Understanding the History View

#### Location List
Each location entry shows:
- **Coordinates**: Latitude and longitude
- **Timestamp**: When the location was recorded
- **Address**: Approximate street address (if available)

#### Map Integration
- Tap **"Show Location on Google Map"** to open the location in Google Maps
- This allows you to see the exact location on a detailed map
- You can get directions or additional information from Google Maps

### 3. Location Data Management

#### Data Refresh
- Pull down on the history list to refresh data
- New locations appear automatically
- Data syncs across all your devices

#### Data Retention
- Your location history is stored securely in the cloud
- Data is retained according to your subscription status
- You can access historical data from any logged-in device

## Settings & Configuration

### 1. Account Settings

#### Profile Management
- View your current email address
- Check subscription status and expiry date
- Manage account preferences

#### Sign Out
1. Tap the **menu icon** (three dots) in the top right
2. Select **"Sign Out"**
3. Confirm your choice in the popup dialog
4. You'll be returned to the login screen

### 2. Tracking Preferences

#### Email Configuration
- Update the email address for location notifications
- Multiple email addresses not currently supported
- Email address must be valid for successful delivery

#### Interval Adjustment
- Modify tracking frequency based on your needs
- Changes take effect immediately
- Consider battery life when choosing shorter intervals

### 3. App Information

#### Info Panel
Access the info panel by:
1. Tapping the **info icon** (i) in the top right menu
2. View the user guide information
3. Learn about app features and usage

## Troubleshooting

### Common Issues and Solutions

#### 1. Location Not Updating

**Symptoms**: No location emails received, tracking appears inactive

**Solutions**:
- ✅ Check that location permissions are granted
- ✅ Ensure GPS/Location Services are enabled on your device
- ✅ Verify internet connectivity
- ✅ Check that battery optimization is disabled for LocationTracker
- ✅ Restart the app and begin tracking again

#### 2. Authentication Problems

**Symptoms**: Cannot sign in, authentication errors

**Solutions**:
- ✅ Verify email and password are correct
- ✅ Check internet connection
- ✅ Try password reset if needed
- ✅ Ensure email is verified (check spam folder for verification code)

#### 3. Subscription Issues

**Symptoms**: Subscription not recognized, billing problems

**Solutions**:
- ✅ Check Google Play subscription status
- ✅ Ensure payment method is valid
- ✅ Wait a few minutes for subscription activation
- ✅ Contact support if issues persist

#### 4. Email Not Received

**Symptoms**: Not receiving location notification emails

**Solutions**:
- ✅ Check spam/junk email folders
- ✅ Verify email address is entered correctly
- ✅ Test with "Test Location" button first
- ✅ Check email provider settings for blocked emails

#### 5. App Crashing or Freezing

**Symptoms**: App closes unexpectedly, becomes unresponsive

**Solutions**:
- ✅ Force close and restart the app
- ✅ Restart your device
- ✅ Update to the latest app version
- ✅ Clear app cache and data (will require re-login)

#### 6. Background Tracking Stops

**Symptoms**: Tracking works only when app is open

**Solutions**:
- ✅ Disable battery optimization for LocationTracker
- ✅ Add app to "Auto-start" or "Protected apps" list
- ✅ Check background app refresh settings
- ✅ Ensure "Allow all the time" location permission is granted

### Battery Optimization Settings

#### Android Settings Path
1. Settings → Apps → LocationTracker → Battery
2. Select "Don't optimize" or "No restrictions"
3. Alternatively: Settings → Battery → Battery Optimization
4. Find LocationTracker and set to "Not optimized"

## Privacy & Security

### 1. Data Protection

#### What Data is Collected
- **Location Data**: GPS coordinates and timestamps
- **Account Information**: Email address and authentication details
- **Usage Data**: App usage patterns for service improvement

#### How Data is Used
- **Location Sharing**: Send location updates to your specified email
- **Service Improvement**: Analyze usage patterns to enhance the app
- **Account Management**: Maintain your account and subscription

#### Data Security
- All data is encrypted in transit using HTTPS
- Authentication handled by AWS Cognito enterprise security
- Location data stored securely in cloud infrastructure
- No data shared with third parties except as required for service operation

### 2. Privacy Controls

#### Location Access
- You control when and how often location is tracked
- You can stop tracking at any time
- You choose which email address receives updates

#### Account Control
- You own your account data
- You can delete your account at any time
- You control subscription status

### 3. Permissions Explained

#### Required Permissions
- **Location**: Essential for GPS tracking functionality
- **Internet**: Required to send location data and authenticate
- **Notifications**: To inform you of tracking status
- **Billing**: For subscription management through Google Play

#### Optional Permissions
- **Background Location**: Enables tracking when app is closed
- **Precise Location**: Provides more accurate GPS coordinates

## Frequently Asked Questions

### General Questions

**Q: How accurate is the location tracking?**
A: LocationTracker uses GPS for high accuracy, typically within 3-5 meters under good conditions. Accuracy depends on GPS signal strength and environmental factors.

**Q: Can I track multiple devices?**
A: Yes, you can log in to the same account on multiple devices. Each device will send location updates to the same email address.

**Q: Does LocationTracker work without internet?**
A: The app requires internet connectivity to send location updates via email. Location data will be cached locally and sent when connection is restored.

**Q: How much battery does continuous tracking use?**
A: LocationTracker is optimized for battery efficiency. Typical usage is 5-15% of daily battery consumption, depending on tracking interval and device.

### Technical Questions

**Q: What happens if my subscription expires?**
A: Location tracking will stop when your subscription expires. You can reactivate by renewing your subscription. Historical data is preserved for 30 days.

**Q: Can I change my email address?**
A: Yes, you can update the email address for location notifications in the home screen. Your login email remains the same.

**Q: Is there a limit to location history?**
A: No, there's no limit to stored location data while your subscription is active. Data is retained based on your subscription status.

**Q: Can I export my location data?**
A: Currently, location data can be viewed within the app. Data export features may be added in future updates.

### Troubleshooting Questions

**Q: Why did tracking stop automatically?**
A: Tracking may stop due to:
- Battery optimization settings
- Loss of internet connectivity for extended periods
- Device restart without app auto-start
- System resource constraints

**Q: What if I don't receive the verification email?**
A: Check your spam/junk folder, ensure the email address is correct, and try requesting a new verification code.

**Q: How do I contact support?**
A: For technical issues or questions not covered in this guide, please contact our support team through the app feedback system or visit our support website.

---

## Quick Reference

### Essential Functions
- **Start Tracking**: Home tab → Enter email → Set interval → Start Tracking
- **View History**: Cloud tab → Browse locations → Tap to view on map
- **Test Setup**: Home tab → Test Location button
- **Sign Out**: Menu (⋮) → Sign Out → Confirm

### Important Settings
- **Location Permission**: Allow "All the time"
- **Battery Optimization**: Disable for LocationTracker
- **Background Refresh**: Enable for LocationTracker
- **Notifications**: Enable for tracking status

### Support Resources
- **In-app Info**: Tap (i) icon in top menu
- **This User Guide**: Complete feature documentation
- **App Settings**: Access via menu in top right corner

**LocationTracker** - Your trusted location tracking companion.
