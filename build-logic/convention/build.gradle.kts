plugins {
    `kotlin-dsl`
}

group = "com.sza.tourist.buildlogic"

dependencies {
    // compileOnly: the root build puts the Android Gradle Plugin on the classpath once (build.gradle.kts
    // of the repository root, `apply false`), and the plugins below find it there at run time.
    compileOnly(libs.android.gradle.plugin)

    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tourist.android-application"
            implementationClass = "com.sza.tourist.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("distributionVariants") {
            id = "tourist.distribution-variants"
            implementationClass = "com.sza.tourist.buildlogic.DistributionVariantsConventionPlugin"
        }
    }
}
