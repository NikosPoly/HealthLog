plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.example.healthlog"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.healthlog"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 optimizes the whole app (code and libraries). The experimental packageScope
            // (only androidx, kotlin, kotlinx) made the release build crash on start.
            optimization {
                enable = true
            }
        }
        // Like release (fast, optimized, not debuggable), but signed with the debug key,
        // so it installs on any device without the release key. For trying the app's real
        // speed: pick "benchmark" in Build Variants. Not for publishing.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    testBuildType = "debug"
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Room writes a JSON description of each database version into app/schemas.
// The files are committed, so migration tests can create databases of older versions.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Color schemes from one color per type (ui/theme/TypeTheme.kt)
    implementation(libs.material.kolor)
    // Charts in the history (only used in ui/history/chart/vico)
    implementation(libs.vico.compose.m3)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    // Keeps all kotlinx.serialization libraries (used by Navigation and by Room's test tools)
    // on the same version. Without it, the migration tests fail with an AbstractMethodError.
    implementation(platform(libs.kotlinx.serialization.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Room annotation processor
    ksp(libs.androidx.room.compiler)
}