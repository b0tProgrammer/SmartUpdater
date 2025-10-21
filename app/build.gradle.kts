plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mohith.smartUpdater"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mohith.smartUpdater"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ... your existing dependencies
    implementation("androidx.compose.foundation:foundation:1.6.8") // For LazyColumn/Row

    // Apache POI for reading Excel files (.xlsx)
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // You also need to exclude certain dependencies that conflict with Android's built-in ones
    implementation("org.apache.poi:poi:5.2.5") {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    // In app/build.gradle.kts, inside the dependencies { ... } block

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

// Add this line for Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("com.github.virtuald:curvesapi:1.08")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}