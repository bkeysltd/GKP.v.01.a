plugins {
    id("com.android.application")
}

android {
    namespace = "com.bkeysltd.gkp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bkeysltd.gkp"
        minSdk = 24
        targetSdk = 35
        versionCode = 30
        versionName = "0.03.a.00"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
}
