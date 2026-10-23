package com.sza.tourist.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The two variants of one codebase: `store` (the Google Play edition) and `noLegal` (the sideload
 * edition). They share one application id and one signature, so the variants update each other and the
 * Data Layer pairs a phone with a watch across them; neither takes an applicationIdSuffix, ever.
 *
 * The interface of a variant-dependent capability lives in `src/main`, its implementation in the source
 * set of each variant, and each variant carries one DI module that binds it (handoff section 3). A
 * capability a variant withholds is a real class in that variant's source set, never a default in
 * `src/main`, which would diverge silently from the variant that overrides it. What the variants hide is
 * owner decision 4.3 (S0008); the skeleton seam only names the distribution a build belongs to.
 *
 * Applied by the phone and the watch. The watch face is resource-only and has no variants.
 */
class DistributionVariantsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    flavorDimensions += DIMENSION
                    productFlavors.create(STORE) {
                        dimension = DIMENSION
                        isDefault = true
                    }
                    productFlavors.create(NO_LEGAL) {
                        dimension = DIMENSION
                    }
                }
            }
        }
    }

    private companion object {
        const val DIMENSION = "distribution"
        const val STORE = "store"
        const val NO_LEGAL = "noLegal"
    }
}
