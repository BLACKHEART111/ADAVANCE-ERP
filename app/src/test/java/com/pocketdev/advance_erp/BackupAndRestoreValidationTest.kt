package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class BackupAndRestoreValidationTest {

    @Test
    fun testSha256ChecksumVerification() {
        val tempFile = File.createTempFile("test_backup", ".db.bak")
        val sampleData = "SAMPLE_SQLITE_ERP_BACKUP_CONTENT_ROOM_V4_ADVANCE_ERP".toByteArray(Charsets.UTF_8)
        FileOutputStream(tempFile).use { it.write(sampleData) }

        val digest = MessageDigest.getInstance("SHA-256")
        val expectedHash = digest.digest(sampleData).joinToString("") { "%02x".format(it) }

        // Recalculate hash from file
        val calculatedDigest = MessageDigest.getInstance("SHA-256")
        tempFile.inputStream().use { fis ->
            val buf = ByteArray(1024)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                calculatedDigest.update(buf, 0, n)
            }
        }
        val fileHash = calculatedDigest.digest().joinToString("") { "%02x".format(it) }

        assertEquals(expectedHash, fileHash)
        tempFile.delete()
    }

    @Test
    fun testSqliteHeaderValidation() {
        // Valid SQLite format starts with 16 bytes: "SQLite format 3\000"
        val validHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val validFile = File.createTempFile("valid_header", ".db.bak")
        FileOutputStream(validFile).use { it.write(validHeader) }

        val readHeader = ByteArray(16)
        validFile.inputStream().use { it.read(readHeader) }
        val readHeaderStr = String(readHeader, Charsets.US_ASCII)

        assertTrue(readHeaderStr.startsWith("SQLite format 3"))

        // Invalid file (e.g. random text or corrupted data)
        val invalidFile = File.createTempFile("invalid_header", ".db.bak")
        FileOutputStream(invalidFile).use { it.write("INVALID_HEADER_DATA_12345".toByteArray()) }

        val invalidReadHeader = ByteArray(16)
        invalidFile.inputStream().use { it.read(invalidReadHeader) }
        val invalidHeaderStr = String(invalidReadHeader, Charsets.US_ASCII)

        assertFalse(invalidHeaderStr.startsWith("SQLite format 3"))

        validFile.delete()
        invalidFile.delete()
    }
}
