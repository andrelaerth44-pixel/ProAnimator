plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.proanimator.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.proanimator.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.5.0-alpha"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:engine"))
    implementation(project(":core:brushes"))
    implementation(project(":core:timeline"))
    implementation(project(":core:export"))
    implementation(project(":core:ink"))
    implementation(project(":core:lottie"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lottie also pulled via :core:lottie
    implementation("com.airbnb.android:lottie:6.6.2")

    // Optional Jetpack Ink (uncomment + enable InkFeatureFlags.enabled):
    // val ink = "1.0.0"
    // implementation("androidx.ink:ink-nativeloader:$ink")
    // implementation("androidx.ink:ink-strokes:$ink")
    // implementation("androidx.ink:ink-brush:$ink")
    // implementation("androidx.ink:ink-brush-compose:$ink")
    // implementation("androidx.ink:ink-authoring-compose:$ink")
    // implementation("androidx.ink:ink-rendering:$ink")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
