plugins { id("com.android.application") }

android {
    namespace = "com.bkeysltd.gkp07"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bkeysltd.gkp07"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.b.07"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
}
