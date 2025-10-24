# Location Tracker - Authentication Setup Guide

## AWS Cognito Configuration

To use AWS Cognito authentication, you need to:

1. **Create AWS Cognito User Pool:**
   - Go to AWS Console → Cognito → User Pools
   - Create a new User Pool
   - Configure sign-in options (email)
   - Set password policy
   - Enable email verification
   - Note down the User Pool ID and App Client ID

2. **Update Configuration:**
   - Open `app/src/main/res/raw/amplifyconfiguration.json`
   - Replace placeholders with your actual values:
     ```json
     "PoolId": "us-east-1_XXXXXXXXX",
     "AppClientId": "your_app_client_id_here",
     "Region": "us-east-1"
     ```

## Google Sign-In Configuration

1. **Google Cloud Console Setup:**
   - Go to Google Cloud Console
   - Create/select project
   - Enable Google Sign-In API
   - Create OAuth 2.0 credentials
   - Add your app's package name and SHA-1 fingerprint

2. **Update Google Client ID:**
   - In `GoogleSignInService.java`, update the `WEB_CLIENT_ID` constant
   - Replace `YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com` with your actual client ID

3. **Get SHA-1 Fingerprint:**
   ```bash
   # For debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For release keystore
   keytool -list -v -keystore your_release.keystore -alias your_alias
   ```

## Features Implemented

✅ **Sign In/Sign Up** - Email and password authentication
✅ **Email Verification** - Automatic verification code handling  
✅ **Password Reset** - Forgot password flow with email codes
✅ **Google Sign-In** - OAuth authentication with Google accounts
✅ **Input Validation** - Real-time form validation
✅ **Modern UI** - Material Design with smooth transitions
✅ **State Management** - Proper handling of different auth modes

## Authentication Flow

1. **First Time Users:** Sign Up → Email Verification → Sign In
2. **Existing Users:** Sign In directly
3. **Google Users:** One-tap Google Sign-In
4. **Password Reset:** Email → Verification Code → New Password

## Security Features

- Secure token storage
- Input validation and sanitization
- Password strength requirements
- Email verification mandatory
- Session management
- Automatic sign-in check on app start

## Next Steps

1. Set up AWS Cognito User Pool
2. Configure Google OAuth credentials  
3. Update configuration files with your credentials
4. Test authentication flows
5. Deploy and monitor usage

## Testing

- Test all authentication modes
- Verify email flows work
- Test Google Sign-In integration
- Check session persistence
- Validate error handling
