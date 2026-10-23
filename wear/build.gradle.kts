import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("tourist.android-application")
    id("tourist.distribution-variants")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    // The code namespace keeps the .wear segment; only the install identity has to match the phone.
    namespace = "com.sza.tourist.wear"

    defaultConfig {
        // Frozen anchor (owner, 2026-10-02): MUST stay identical to the phone's applicationId. The Data
        // Layer routes by (package name, signing certificate) and drops anything whose package name
        // differs across the two devices, below the app - a mismatch is invisible to both sides.
        applicationId = "com.sza.tourist"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Wear OS Compose, on the Compose BOM that wear-compose 1.4.x is built against.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
