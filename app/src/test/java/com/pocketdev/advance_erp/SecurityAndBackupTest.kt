package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.security.PinSecurityHelper
import com.pocketdev.advance_erp.core.worker.FridayBackupScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class SecurityAndBackupTest {

    @Test
    fun testPinValidationRules() {
        // Valid 4 to 8 digit pins
        assertTrue("4 digit PIN should be valid", PinSecurityHelper.isValidPin("1234"))
        assertTrue("5 digit PIN should be valid", PinSecurityHelper.isValidPin("12345"))
        assertTrue("6 digit PIN should be valid", PinSecurityHelper.isValidPin("123456"))
        assertTrue("7 digit PIN should be valid", PinSecurityHelper.isValidPin("1234567"))
        assertTrue("8 digit PIN should be valid", PinSecurityHelper.isValidPin("12345678"))
        assertTrue("PIN with leading zeros should be valid", PinSecurityHelper.isValidPin("0000"))
        assertTrue("8 zeros should be valid", PinSecurityHelper.isValidPin("00000000"))

        // Invalid: too short (< 4)
        assertFalse("Empty PIN must be invalid", PinSecurityHelper.isValidPin(""))
        assertFalse("1 digit PIN must be invalid", PinSecurityHelper.isValidPin("1"))
        assertFalse("2 digit PIN must be invalid", PinSecurityHelper.isValidPin("12"))
        assertFalse("3 digit PIN must be invalid", PinSecurityHelper.isValidPin("123"))

        // Invalid: too long (> 8)
        assertFalse("9 digit PIN must be invalid", PinSecurityHelper.isValidPin("123456789"))
        assertFalse("10 digit PIN must be invalid", PinSecurityHelper.isValidPin("1234567890"))

        // Invalid: non-digit characters
        assertFalse("PIN with alphabet must be invalid", PinSecurityHelper.isValidPin("123a"))
        assertFalse("PIN with special character must be invalid", PinSecurityHelper.isValidPin("123#"))
        assertFalse("PIN with space must be invalid", PinSecurityHelper.isValidPin("12 4"))
        assertFalse("PIN with negative sign must be invalid", PinSecurityHelper.isValidPin("-123"))
    }

    @Test
    fun testPinHashingAndVerification() {
        val pin = "87654321"
        val salt = PinSecurityHelper.generateSalt()
        assertTrue("Salt should be 16 chars", salt.length == 16)

        val hash1 = PinSecurityHelper.hashPin(pin, salt)
        val hash2 = PinSecurityHelper.hashPin(pin, salt)
        assertEquals("Hashing same pin with same salt should be deterministic", hash1, hash2)

        val diffSalt = PinSecurityHelper.generateSalt()
        val hashDiff = PinSecurityHelper.hashPin(pin, diffSalt)
        assertNotEquals("Hashing with different salt should yield different hash", hash1, hashDiff)

        // Verify correct pin
        assertTrue("Verify correct PIN should succeed", PinSecurityHelper.verifyPin(pin, hash1, salt))

        // Verify wrong pin
        assertFalse("Verify wrong PIN should fail", PinSecurityHelper.verifyPin("12345678", hash1, salt))
        assertFalse("Verify empty PIN should fail", PinSecurityHelper.verifyPin("", hash1, salt))

        // Verify null / blank hash or salt
        assertFalse("Verify with null hash should fail safely", PinSecurityHelper.verifyPin(pin, null, salt))
        assertFalse("Verify with empty hash should fail safely", PinSecurityHelper.verifyPin(pin, "", salt))
        assertFalse("Verify with null salt should fail safely", PinSecurityHelper.verifyPin(pin, hash1, null))
        assertFalse("Verify with empty salt should fail safely", PinSecurityHelper.verifyPin(pin, hash1, ""))
    }

    @Test
    fun testBackupFileNameFormatting() {
        val testDate = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 18, 30, 45) // 10 Nov 2026 18:30:45
        }.time

        val timeStamp = SimpleDateFormat("dd_MM_yy_HHmmss", Locale.US).format(testDate)
        val fileName = "AdvanceERP_Backup_${timeStamp}.zip"

        assertEquals("AdvanceERP_Backup_10_11_26_183045.zip", fileName)

        // Verify date/mm/yy format pattern with regex
        val regex = Regex("^AdvanceERP_Backup_\\d{2}_\\d{2}_\\d{2}_\\d{6}\\.zip$")
        assertTrue("Backup filename must follow AdvanceERP_Backup_dd_MM_yy_HHmmss.zip pattern", regex.matches(fileName))
    }

    @Test
    fun testFridayDelayCalculation() {
        val delayMs = FridayBackupScheduler.calculateInitialDelayToFriday(targetHour = 22, targetMinute = 0)
        assertTrue("Delay to Friday must be strictly positive", delayMs > 0)

        // The scheduled time must fall on a Friday at 22:00
        val scheduledTime = System.currentTimeMillis() + delayMs
        val calendar = Calendar.getInstance().apply {
            timeInMillis = scheduledTime
        }

        assertEquals("Scheduled day must be Friday", Calendar.FRIDAY, calendar.get(Calendar.DAY_OF_WEEK))
        assertEquals("Scheduled hour must be 22 (10 PM)", 22, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("Scheduled minute must be 0", 0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun testFullZipBackupAndRestoreContentIntegrity() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "erp_backup_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // 1. Create simulated source files
            val dbFile = File(tempDir, "advance_erp.db")
            val dbContent = "SQLite format 3\u0000--ADVANCE ERP MOCK DB DATA--".toByteArray(Charsets.UTF_8)
            FileOutputStream(dbFile).use { it.write(dbContent) }

            val imgDir = File(tempDir, "product_images").apply { mkdirs() }
            val imgFile1 = File(imgDir, "prod_001.jpg")
            val imgContent1 = "MOCK_IMAGE_BYTES_PROD_1".toByteArray(Charsets.UTF_8)
            FileOutputStream(imgFile1).use { it.write(imgContent1) }

            val avatarFile = File(tempDir, "avatar_user_1.jpg")
            val avatarContent = "MOCK_AVATAR_BYTES_USER_1".toByteArray(Charsets.UTF_8)
            FileOutputStream(avatarFile).use { it.write(avatarContent) }

            // 2. Package into zip
            val zipFile = File(tempDir, "test_backup.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Database
                zos.putNextEntry(ZipEntry("database/advance_erp.db"))
                FileInputStream(dbFile).use { it.copyTo(zos) }
                zos.closeEntry()

                // Product image
                zos.putNextEntry(ZipEntry("product_images/prod_001.jpg"))
                FileInputStream(imgFile1).use { it.copyTo(zos) }
                zos.closeEntry()

                // Avatar
                zos.putNextEntry(ZipEntry("avatars/avatar_user_1.jpg"))
                FileInputStream(avatarFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }

            assertTrue("Zip file must be created and non-empty", zipFile.exists() && zipFile.length() > 0)

            // 3. Inspect zip entries
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }.toSet()
                assertTrue("Must contain database entry", entries.contains("database/advance_erp.db"))
                assertTrue("Must contain product image entry", entries.contains("product_images/prod_001.jpg"))
                assertTrue("Must contain avatar entry", entries.contains("avatars/avatar_user_1.jpg"))

                // Verify db content restored from zip
                val dbEntry = zip.getEntry("database/advance_erp.db")
                val restoredDbBytes = zip.getInputStream(dbEntry).readBytes()
                assertEquals(String(dbContent), String(restoredDbBytes))

                // Verify photo restored from zip
                val imgEntry = zip.getEntry("product_images/prod_001.jpg")
                val restoredImgBytes = zip.getInputStream(imgEntry).readBytes()
                assertEquals(String(imgContent1), String(restoredImgBytes))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
