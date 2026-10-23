package com.sza.tourist.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * What the three packages of this product (phone, watch, watch face) have in common: platform levels,
 * the mechanically derived version, and the release signing config.
 *
 * Applied by every module as `tourist.android-application`. The family of a module is its name.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val family = PackageFamily.forModule(name)
            val platform = Platform.of(this)
            val version = resolveVersion(family)
            val signing = resolveReleaseSigning()

            logger.lifecycle(
                "[tourist] :$name ${version.name} (${version.code}) from the ${version.source.name.lowercase()}",
            )

            extensions.configure<ApplicationExtension> {
                compileSdk = platform.compileSdk

                defaultConfig {
                    minSdk = platform.minSdk(family)
                    targetSdk = platform.targetSdk
                    versionCode = version.code
                    versionName = version.name
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                if (signing != null) {
                    signingConfigs.create("release") {
                        storeFile = signing.storeFile
                        storePassword = signing.storePassword
                        keyAlias = signing.keyAlias
                        keyPassword = signing.keyPassword
                    }
                    buildTypes.getByName("release") {
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }
        }
    }

    /**
     * PACKAGE-VERSIONING rules 5-7. A pinned pair wins; otherwise an invocation that packages stamps
     * itself from one clock reading, shared by every module of the invocation; otherwise the sentinel.
     * `-Ptourist.stableVersion=true` opts a packaging invocation out of the stamp, for builds that never
     * leave the developer's devices (rule 6).
     */
    private fun Project.resolveVersion(family: PackageFamily): ResolvedVersion {
        val pinnedName = providers.gradleProperty("tourist.versionName").orNull
        val pinnedCode = providers.gradleProperty("tourist.versionCode").orNull
        val stable = providers.gradleProperty("tourist.stableVersion").orNull.equals("true", ignoreCase = true)

        val taskNames = gradle.startParameter.taskNames
        val stamping = pinnedName == null && !stable && StampPolicy.packagingRequested(taskNames)
        return try {
            VersionStamp.resolve(
                family,
                pinnedName,
                pinnedCode,
                if (stamping) sharedClockReading() else null,
                built = StampPolicy.targetsModule(taskNames, name),
            )
        } catch (e: IllegalArgumentException) {
            throw GradleException("[tourist] :$name - ${e.message}", e)
        }
    }

    /**
     * One reading for the whole invocation, kept on the root project: this plugin is applied once per
     * module and `providers.of` hands each a provider with its own memo. Two readings are two moments
     * of the clock, and an invocation that builds the phone and the watch together would sooner or later
     * straddle a minute boundary and ship two versionNames that differ (rule 1).
     */
    private fun Project.sharedClockReading(): String {
        val extra = rootProject.extensions.extraProperties
        if (extra.has(CLOCK_KEY)) return extra.get(CLOCK_KEY) as String
        val reading = providers.of(BuildClockValueSource::class.java) { }.get()
        extra.set(CLOCK_KEY, reading)
        return reading
    }

    /**
     * The release signing config exists only on a machine that has the key. Without it release variants
     * are built unsigned (which no device and no store accepts) and a signed release is never faked with
     * the debug key.
     */
    private fun Project.resolveReleaseSigning(): KeystoreProperties? {
        val file = try {
            ReleaseSigning.locate(rootDir, System.getenv(ReleaseSigning.ENV_VAR))
        } catch (e: IllegalArgumentException) {
            throw GradleException("[tourist] ${e.message}", e)
        }
        if (file == null) {
            if (StampPolicy.releasePackagingRequested(gradle.startParameter.taskNames)) {
                logger.warn(
                    "[tourist] :$name has no release key (no ${ReleaseSigning.FILE_NAME}, no " +
                        "${ReleaseSigning.ENV_VAR}); the release package will be UNSIGNED.",
                )
            }
            return null
        }
        return try {
            ReleaseSigning.load(file)
        } catch (e: IllegalArgumentException) {
            throw GradleException("[tourist] ${e.message}", e)
        }
    }

    private companion object {
        const val CLOCK_KEY = "tourist.buildClockReading"
    }
}
