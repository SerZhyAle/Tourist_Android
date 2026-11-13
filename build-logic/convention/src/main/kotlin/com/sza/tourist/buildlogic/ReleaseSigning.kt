package com.sza.tourist.buildlogic

import java.io.File
import java.util.Properties

/** The four values of a release signing config, read from a properties file that is never tracked. */
internal class KeystoreProperties(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

/**
 * The two keys of owner decision 4.2 (2026-10-02): the product's own app signing key, which signs the
 * sideload packages and is imported into Play App Signing, and a separate upload key, which signs what is
 * uploaded to Play. Play re-signs an upload with the app signing key, so a store build installed from Play
 * and a sideload build carry one signature and update each other.
 */
internal class ReleaseKeys(
    val appSigning: KeystoreProperties,
    val upload: KeystoreProperties?,
)

/**
 * Where the release keys come from. No key material is ever stored in the repository; the build reads
 * paths (and the passwords) from one of two places, in this order:
 *
 *  1. the file named by the environment variable [ENV_VAR], and failing that
 *  2. `keystore.properties` in the repository root, which `.gitignore` excludes.
 *
 * The file has four keys for the app signing key - `storeFile`, `storePassword`, `keyAlias`,
 * `keyPassword` - and the same four with the prefix [UPLOAD_PREFIX] for the upload key, which may be
 * absent as a whole. A relative `storeFile` is resolved against the directory of the file that names it.
 * The keys themselves live outside the repository, in `P:\ANDROID\Tourist_credentials`, which is the
 * owner's to create (S0002 section 3.3, S0010); no agent generates anything under that path.
 */
internal object ReleaseSigning {
    const val ENV_VAR = "TOURIST_KEYSTORE_PROPERTIES"
    const val FILE_NAME = "keystore.properties"
    const val UPLOAD_PREFIX = "upload."

    private val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

    /** The properties file, or null on a machine that has no release key (debug builds still work). */
    fun locate(rootDir: File, envValue: String?): File? {
        if (!envValue.isNullOrBlank()) {
            val named = File(envValue)
            require(named.isFile) { "$ENV_VAR names '$envValue', which is not a file." }
            return named
        }
        return File(rootDir, FILE_NAME).takeIf(File::isFile)
    }

    /** Messages name keys and paths, never values: a password must not reach a build log. */
    fun load(file: File): ReleaseKeys {
        val properties = Properties().apply { file.inputStream().use(::load) }
        val app = keystore(file, properties, prefix = "")
        val uploadNames = requiredKeys.map { UPLOAD_PREFIX + it }
        val upload = if (uploadNames.all { properties.getProperty(it).isNullOrBlank() }) {
            null
        } else {
            keystore(file, properties, UPLOAD_PREFIX)
        }
        return ReleaseKeys(app, upload)
    }

    private fun keystore(file: File, properties: Properties, prefix: String): KeystoreProperties {
        val missing = requiredKeys.map { prefix + it }.filter { properties.getProperty(it).isNullOrBlank() }
        require(missing.isEmpty()) { "${file.path} lacks: ${missing.joinToString()}." }
        val rawStoreFile = properties.getProperty(prefix + "storeFile")
        val declared = File(rawStoreFile)
        return KeystoreProperties(
            storeFile = if (declared.isAbsolute) declared else File(file.parentFile, rawStoreFile).normalize(),
            storePassword = properties.getProperty(prefix + "storePassword"),
            keyAlias = properties.getProperty(prefix + "keyAlias"),
            keyPassword = properties.getProperty(prefix + "keyPassword"),
        )
    }
}
