// Tourist watch face, Watch Face Format.
//
// A resource-only module: no Kotlin, no Java, no dependencies, and the manifest declares
// android:hasCode="false". The renderer lives in the Wear OS system image, so the package ships nothing
// but a declarative XML description and the resources it references.
//
// The composition (a clone of the FastMediaSorter face, or a clone with tourist complications) is owner
// decision 4.4, asked right before S0009; this skeleton shows the time and the working name only.
plugins {
    id("tourist.android-application")
}

android {
    // No Kotlin toolchain: the module has no sources to compile, and leaving it on would make an empty
    // compile task part of every build of this module.
    enableKotlin = false

    namespace = "com.sza.tourist.watchface"

    defaultConfig {
        // Frozen anchor (owner, 2026-10-02). Deliberately NOT com.sza.tourist: that id belongs to the
        // phone app and the watch app jointly, and the store needs the watch face to be a separate
        // app entry with its own listing category.
        applicationId = "com.sza.tourist.watchface"
    }

    buildTypes {
        // Code shrinking is ON in BOTH build types, which is the opposite of the usual arrangement and
        // not a slip. AGP generates an R class for the resources even in a module with no sources, dexes
        // it and packages the result; a package that declares android:hasCode="false" and then ships a
        // dex contradicts its own manifest, so R8 runs purely to remove the class nobody asked for.
        debug {
            isMinifyEnabled = true
        }
        release {
            isMinifyEnabled = true
            // Resource shrinking must stay OFF even though code shrinking is on: the resource shrinker
            // decides reachability by following code references, this module has no code, and so every
            // drawable the watch face markup names would look unused and be stripped from the bundle.
            isShrinkResources = false
        }
    }
}
