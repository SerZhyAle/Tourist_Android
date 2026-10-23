package com.sza.tourist.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

/** The platform levels, read from the version catalog so that one file holds every pin. */
internal class Platform(private val catalog: VersionCatalog) {
    val compileSdk: Int get() = level("compile-sdk")
    val targetSdk: Int get() = level("target-sdk")

    fun minSdk(family: PackageFamily): Int = level(
        when (family) {
            PackageFamily.PHONE -> "min-sdk-phone"
            PackageFamily.WATCH -> "min-sdk-wear"
            PackageFamily.WATCH_FACE -> "min-sdk-watchface"
        },
    )

    private fun level(alias: String): Int =
        catalog.findVersion(alias).orElseThrow { IllegalStateException("libs.versions.toml has no version '$alias'.") }
            .requiredVersion.toInt()

    companion object {
        fun of(project: Project): Platform =
            Platform(project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs"))
    }
}
