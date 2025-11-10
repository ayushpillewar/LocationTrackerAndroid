plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.majboormajdoor.locationtracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.majboormajdoor.locationtracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "11"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging{
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")

    // AWS Cognito SDK
    implementation("com.amplifyframework:aws-auth-cognito:2.14.5")
    implementation("com.amplifyframework:core:2.14.5")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Credential Manager for modern authentication
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("com.android.billingclient:billing:8.0.0")
    implementation("com.google.code.gson:gson:2.13.2")

    // https://mvnrepository.com/artifact/javax.mail/mail
    implementation("javax.mail:mail:1.4.1")
    implementation(libs.play.services.location)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}