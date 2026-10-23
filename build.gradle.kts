// Plugins are put on the root classpath once, with their versions, and applied per module. The
// convention plugins of build-logic compile against the Android Gradle Plugin and rely on this line
// to find it at run time.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
