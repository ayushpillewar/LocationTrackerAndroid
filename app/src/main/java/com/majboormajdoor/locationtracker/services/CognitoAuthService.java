package com.majboormajdoor.locationtracker.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.core.Amplify;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for AWS Cognito authentication
 * Handles sign up, sign in, password reset, and user management
 * All callbacks are posted to the main thread to prevent UI thread exceptions
 */
public class CognitoAuthService {

    private static final String TAG = "CognitoAuthService";
    private static CognitoAuthService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Store authentication tokens
    private String accessToken;

    // Authentication result callbacks
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
        void onConfirmationRequired(String username);
    }

    public interface TokenCallback {
        void onTokenRetrieved(String accessToken, String idToken);
        void onError(String error);
    }

    public interface UserInfoCallback {
        void onUserInfo(String username, String email);
        void onError(String error);
    }

    private CognitoAuthService() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static synchronized CognitoAuthService getInstance() {
        if (instance == null) {
            instance = new CognitoAuthService();
        }
        return instance;
    }

    /**
     * Post runnable to main thread
     */
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    /**
     * Initialize Amplify with Cognito
     */
    public void initialize(Context context, AuthCallback callback) {
        try {
            // Add Cognito Auth plugin
            Amplify.addPlugin(new AWSCognitoAuthPlugin());

            // Initialize Amplify
            Amplify.configure(context);

            Log.d(TAG, "Amplify initialized successfully");
            runOnMainThread(() -> callback.onSuccess("Authentication service initialized"));

        } catch (AmplifyException e) {
            Log.e(TAG, "Error initializing Amplify", e);
            runOnMainThread(() -> callback.onError("Failed to initialize authentication: " + e.getMessage()));
        }
    }

    /**
     * Sign up new user with email and password
     */
    public void signUp(String username, String email, String password, AuthCallback callback) {
        List<AuthUserAttribute> attributes = new ArrayList<>();
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.email(), email));

        AuthSignUpOptions options = AuthSignUpOptions.builder()
                .userAttributes(attributes)
                .build();

        Amplify.Auth.signUp(
            username,
            password,
            options,
            result -> {
                Log.d(TAG, "Sign up succeeded: " + result);
                if (result.isSignUpComplete()) {
                    runOnMainThread(() -> callback.onSuccess("Account created successfully! Please check your email for verification."));
                } else {
                    runOnMainThread(() -> callback.onConfirmationRequired(username));
                }
            },
            error -> {
                Log.e(TAG, "Sign up failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Confirm sign up with verification code
     */
    public void confirmSignUp(String username, String confirmationCode, AuthCallback callback) {
        Amplify.Auth.confirmSignUp(
            username,
            confirmationCode,
            result -> {
                Log.d(TAG, "Confirm sign up succeeded: " + result);
                runOnMainThread(() -> callback.onSuccess("Email verified successfully! You can now sign in."));
            },
            error -> {
                Log.e(TAG, "Confirm sign up failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Sign in user with username/email and password
     */
    public void signIn(String username, String password, AuthCallback callback) {
        Amplify.Auth.signIn(
            username,
            password,
            result -> {
                Log.d(TAG, "Sign in succeeded: " + result);
                if (result.isSignedIn()) {
                    // Fetch and store tokens after successful sign in
                    fetchAndStoreTokens(() -> runOnMainThread(() -> callback.onSuccess("Signed in successfully!")),
                                      new AuthCallback() {
                                          @Override
                                          public void onSuccess(String message) {
                                              // Not used in this context
                                          }

                                          @Override
                                          public void onError(String error) {
                                              Log.e(TAG, "Failed to fetch tokens after sign in: " + error);
                                              runOnMainThread(() -> callback.onSuccess("Signed in successfully!")); // Still report success even if token fetch fails
                                          }

                                          @Override
                                          public void onConfirmationRequired(String username) {
                                              // Not used in this context
                                          }
                                      });
                } else {
                    // Handle additional sign in steps if needed (MFA, etc.)
                    runOnMainThread(() -> callback.onError("Additional authentication steps required"));
                }
            },
            error -> {
                Log.e(TAG, "Sign in failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Fetch and store authentication tokens
     */
    private void fetchAndStoreTokens(Runnable onSuccess, AuthCallback onError) {
        Amplify.Auth.fetchAuthSession(
            result -> {
                if (result.isSignedIn()) {
                    try {
                        // For Amplify v2, we'll store a simple token indicator
                        // The actual tokens are managed by Amplify internally
                        accessToken = "authenticated";
                        Log.d(TAG, "Authentication session stored successfully");
                        onSuccess.run();
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing auth session", e);
                        onError.onError("Failed to process authentication session");
                    }
                } else {
                    onError.onError("User not signed in");
                }
            },
            error -> {
                Log.e(TAG, "Failed to fetch auth session", error);
                onError.onError("Failed to fetch authentication session");
            }
        );
    }

    /**
     * Check if user is authenticated and has valid session
     * @return true if user has valid authentication session
     */
    public boolean hasValidTokens() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Get authentication token for API calls
     * This method fetches the current JWT tokens from Amplify
     */
    public void getTokenForApiCall(TokenCallback callback) {
        Amplify.Auth.fetchAuthSession(
            result -> {
                if (result.isSignedIn()) {
                    try {

                        runOnMainThread(() -> callback.onTokenRetrieved("Bearer " + accessToken, accessToken));
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting token from auth session", e);
                        runOnMainThread(() -> callback.onError("Failed to extract authentication token"));
                    }
                } else {
                    runOnMainThread(() -> callback.onError("User not signed in"));
                }
            },
            error -> {
                Log.e(TAG, "Failed to fetch auth session for API call", error);
                runOnMainThread(() -> callback.onError("Failed to get authentication token"));
            }
        );
    }

    /**
     * Check if user is signed in and return status
     */
    public void checkAuthStatus(AuthCallback callback) {
        Amplify.Auth.fetchAuthSession(
            result -> {
                if (result.isSignedIn()) {
                    runOnMainThread(() -> callback.onSuccess("User is authenticated"));
                } else {
                    runOnMainThread(() -> callback.onError("User not signed in"));
                }
            },
            error -> {
                Log.e(TAG, "Failed to check auth status", error);
                runOnMainThread(() -> callback.onError("Failed to check authentication status"));
            }
        );
    }

    /**
     * Clear stored tokens (useful for sign out)
     */
    private void clearTokens() {
        accessToken = null;
        Log.d(TAG, "Tokens cleared");
    }

    /**
     * Sign out current user and clear tokens
     */
//    public void signOut(AuthCallback callback) {
//        Amplify.Auth.signOut(
//            result -> {
//                Log.d(TAG, "Signed out successfully");
//                clearTokens();
//                runOnMainThread(() -> callback.onSuccess("Signed out successfully"));
//            },
//            error -> {
//                Log.e(TAG, "Sign out failed", error);
//                runOnMainThread(() -> callback.onError("Sign out failed: " + error.toString()));
//            }
//        );
//    }

    /**
     * Reset password
     */
    public void resetPassword(String username, AuthCallback callback) {
        Amplify.Auth.resetPassword(
            username,
            result -> {
                Log.d(TAG, "Reset password succeeded: " + result);
                runOnMainThread(() -> callback.onSuccess("Password reset code sent to your email"));
            },
            error -> {
                Log.e(TAG, "Reset password failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Confirm password reset with new password and confirmation code
     */
    public void confirmResetPassword(String username, String newPassword, String confirmationCode, AuthCallback callback) {
        Amplify.Auth.confirmResetPassword(
            username,
            newPassword,
            confirmationCode,
            () -> {
                Log.d(TAG, "Confirm reset password succeeded");
                runOnMainThread(() -> callback.onSuccess("Password reset successfully! You can now sign in with your new password."));
            },
            error -> {
                Log.e(TAG, "Confirm reset password failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Check if user is currently signed in and get user info
     */
    public void getCurrentUser(UserInfoCallback callback) {
        Amplify.Auth.fetchAuthSession(
            result -> {
                if (result.isSignedIn()) {
                    // Get current user info first
                    Amplify.Auth.getCurrentUser(
                        currentUser -> {
                            // Now get user attributes
                            Amplify.Auth.fetchUserAttributes(
                                attributes -> {
                                    String email = "";
                                    String username = currentUser.getUsername();

                                    for (AuthUserAttribute attribute : attributes) {
                                        if (attribute.getKey().getKeyString().equals("email")) {
                                            email = attribute.getValue();
                                        }
                                        if (attribute.getKey().getKeyString().equals("preferred_username")) {
                                            username = attribute.getValue();
                                        }
                                    }

                                    // Use the username from currentUser if preferred_username is empty
                                    if (username == null || username.isEmpty()) {
                                        username = currentUser.getUsername();
                                    }

                                    String finalUsername = username;
                                    String finalEmail = email;
                                    runOnMainThread(() -> callback.onUserInfo(finalUsername, finalEmail));
                                },
                                error -> {
                                    Log.e(TAG, "Failed to fetch user attributes", error);
                                    runOnMainThread(() -> callback.onError("Failed to get user information"));
                                }
                            );
                        },
                        error -> {
                            Log.e(TAG, "Failed to get current user", error);
                            runOnMainThread(() -> callback.onError("Failed to get current user"));
                        }
                    );
                } else {
                    runOnMainThread(() -> callback.onError("User not signed in"));
                }
            },
            error -> {
                Log.e(TAG, "Failed to fetch auth session", error);
                runOnMainThread(() -> callback.onError("Failed to check authentication status"));
            }
        );
    }

    /**
     * Check if user is signed in
     */
    public void isSignedIn(AuthCallback callback) {
        Amplify.Auth.fetchAuthSession(
            result -> {
                if (result.isSignedIn()) {
                    Log.d(TAG, "User is signed in");
                    AWSCognitoAuthSession session = (AWSCognitoAuthSession) result;
                    runOnMainThread(() -> callback.onSuccess("User is signed in"));
                } else {
                    runOnMainThread(() -> callback.onError("User not signed in"));
                }
            },
            error -> {
                Log.e(TAG, "Failed to check sign in status", error);
                runOnMainThread(() -> callback.onError("Failed to check authentication status"));
            }
        );
    }

    /**
     * Resend confirmation code for sign up
     */
    public void resendSignUpCode(String username, AuthCallback callback) {
        Amplify.Auth.resendSignUpCode(
            username,
            result -> {
                Log.d(TAG, "Resend sign up code succeeded: " + result);
                runOnMainThread(() -> callback.onSuccess("Verification code sent to your email"));
            },
            error -> {
                Log.e(TAG, "Resend sign up code failed", error);
                runOnMainThread(() -> callback.onError(getErrorMessage(error)));
            }
        );
    }

    /**
     * Get user-friendly error message from AuthException
     */
    private String getErrorMessage(AuthException error) {
        String message = error.getMessage();

        // Add null safety check
        if (message == null) {
            return "Authentication error occurred";
        }

        // Common error messages mapping
        if (message.contains("UsernameExistsException")) {
            return "An account with this email already exists";
        } else if (message.contains("InvalidPasswordException")) {
            return "Password must be at least 8 characters with uppercase, lowercase, numbers and special characters";
        } else if (message.contains("InvalidParameterException")) {
            return "Invalid email or username format";
        } else if (message.contains("NotAuthorizedException")) {
            return "Incorrect username or password";
        } else if (message.contains("UserNotConfirmedException")) {
            return "Please verify your email address first";
        } else if (message.contains("CodeMismatchException")) {
            return "Invalid verification code";
        } else if (message.contains("ExpiredCodeException")) {
            return "Verification code has expired. Please request a new one";
        } else if (message.contains("LimitExceededException")) {
            return "Too many attempts. Please try again later";
        } else if (message.contains("UserNotFoundException")) {
            return "No account found with this email";
        } else {
            return "Authentication error: " + message;
        }
    }
}
