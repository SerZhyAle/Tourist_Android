pluginManagement {
    // The convention plugins (platform levels, version stamp, signing, variants) live in their own
    // included build.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tourist"

include(":phone")
include(":wear")
// Resource-only Watch Face Format package. Its own module by necessity: Google Play refuses a bundle
// that carries both Wear OS logic and a watch face.
include(":watchface")
