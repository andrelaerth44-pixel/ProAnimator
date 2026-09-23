plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.proanimator.core.ink"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Optional Jetpack Ink — uncomment when enabling full path:
    // implementation("androidx.ink:ink-nativeloader:1.0.0")
    // implementation("androidx.ink:ink-brush:1.0.0")
    // implementation("androidx.ink:ink-strokes:1.0.0")
    // implementation("androidx.ink:ink-authoring-compose:1.0.0")
    // implementation("androidx.ink:ink-brush-compose:1.0.0")
}
