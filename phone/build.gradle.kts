import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("tourist.android-application")
    id("tourist.distribution-variants")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sza.tourist"

    defaultConfig {
        // Frozen anchor (owner, 2026-10-02): one id for the phone and the watch app, because the Data
        // Layer routes by (package name, signing certificate) and drops a message whose package name
        // differs across the two devices. The noLegal variant shares it too. Never a suffix, never a
        // rename once a package ships.
        applicationId = "com.sza.tourist"
        val license = rootProject.file("LICENSE").readText().replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n")
        buildConfigField("String", "LICENSE_TEXT", "\"$license\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // The ported classes annotate constructor properties (`@ApplicationContext private val`) and are
        // written for this target; FastMediaSorter builds with the same flag, so the annotations land on
        // the same elements here.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.okhttp.okhttp)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.google.gms.play.services.wearable)
    implementation(libs.gson)

    implementation(libs.timber)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
