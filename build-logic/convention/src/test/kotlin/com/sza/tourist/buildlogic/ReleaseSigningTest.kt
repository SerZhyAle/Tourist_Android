package com.sza.tourist.buildlogic

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReleaseSigningTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun propertiesFile(content: String): File =
        temp.newFile("keystore.properties").apply { writeText(content) }

    @Test
    fun `no env var and no file means no release key`() {
        assertNull(ReleaseSigning.locate(temp.root, null))
        assertNull(ReleaseSigning.locate(temp.root, ""))
    }

    @Test
    fun `the root file is found without the env var`() {
        val file = propertiesFile("storeFile=../keys/tourist.jks\n")
        assertEquals(file, ReleaseSigning.locate(temp.root, null))
    }

    @Test
    fun `the env var wins and must name a file`() {
        val named = propertiesFile("storeFile=anywhere.jks\n")
        assertEquals(named, ReleaseSigning.locate(temp.root, named.path))
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseSigning.locate(temp.root, File(temp.root, "missing.properties").path)
        }
    }

    @Test
    fun `a relative storeFile resolves against the properties file directory`() {
        val file = propertiesFile(
            """
            storeFile=keys/tourist-upload.jks
            storePassword=secret
            keyAlias=tourist-upload
            keyPassword=secret
            """.trimIndent(),
        )
        val loaded = ReleaseSigning.load(file).appSigning
        assertEquals(File(file.parentFile, "keys/tourist-upload.jks"), loaded.storeFile)
        assertEquals("tourist-upload", loaded.keyAlias)
    }

    @Test
    fun `an absolute storeFile is kept as declared`() {
        val file = propertiesFile(
            """
            storeFile=P:/ANDROID/Tourist_credentials/tourist-upload.jks
            storePassword=secret
            keyAlias=tourist-upload
            keyPassword=secret
            """.trimIndent(),
        )
        assertEquals(File("P:/ANDROID/Tourist_credentials/tourist-upload.jks"), ReleaseSigning.load(file).appSigning.storeFile)
    }

    @Test
    fun `a missing key is named, a value never reaches the message`() {
        val file = propertiesFile("storeFile=tourist.jks\nstorePassword=secret\nkeyAlias=tourist\n")
        val error = assertThrows(IllegalArgumentException::class.java) { ReleaseSigning.load(file) }
        assertEquals("${file.path} lacks: keyPassword.", error.message)
    }

    @Test
    fun `a file without upload keys has no upload key`() {
        val file = propertiesFile("storeFile=tourist.jks\nstorePassword=secret\nkeyAlias=tourist\nkeyPassword=secret\n")
        assertNull(ReleaseSigning.load(file).upload)
    }

    @Test
    fun `the upload key is read from the prefixed keys beside the app signing key`() {
        val file = propertiesFile(
            """
            storeFile=tourist-app-signing.jks
            storePassword=secret
            keyAlias=tourist
            keyPassword=secret
            upload.storeFile=tourist-upload.jks
            upload.storePassword=other
            upload.keyAlias=tourist-upload
            upload.keyPassword=other
            """.trimIndent(),
        )
        val keys = ReleaseSigning.load(file)
        assertEquals(File(file.parentFile, "tourist-app-signing.jks"), keys.appSigning.storeFile)
        assertEquals("tourist", keys.appSigning.keyAlias)
        assertEquals(File(file.parentFile, "tourist-upload.jks"), keys.upload?.storeFile)
        assertEquals("tourist-upload", keys.upload?.keyAlias)
    }

    @Test
    fun `a partial upload key is refused by name`() {
        val file = propertiesFile(
            "storeFile=tourist.jks\nstorePassword=secret\nkeyAlias=tourist\nkeyPassword=secret\n" +
                "upload.storeFile=tourist-upload.jks\nupload.keyAlias=tourist-upload\n",
        )
        val error = assertThrows(IllegalArgumentException::class.java) { ReleaseSigning.load(file) }
        assertEquals("${file.path} lacks: upload.storePassword, upload.keyPassword.", error.message)
    }
}
