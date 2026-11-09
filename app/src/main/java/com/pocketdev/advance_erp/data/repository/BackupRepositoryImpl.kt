package com.pocketdev.advance_erp.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.BackupDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AdvanceErpDatabase,
    private val backupDao: BackupDao,
    private val auditLogDao: AuditLogDao
) : BackupRepository {

    override fun getAllBackupRecords(): Flow<List<BackupRecord>> = backupDao.getAllBackupRecords()

    override suspend fun createBackup(isAutomatic: Boolean, notes: String?): BackupRecord = withContext(Dispatchers.IO) {
        // 1. Flush SQLite write-ahead log (WAL) to main DB file
        try {
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)
        require(dbFile.exists()) { "Database file does not exist at ${dbFile.absolutePath}" }

        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "advance_erp_backup_${timeStamp}.zip"
        val zipBackupFile = File(backupDir, fileName)

        val imagesDir = File(context.filesDir, "product_images")
        val imageFiles = if (imagesDir.exists()) imagesDir.listFiles()?.filter { it.isFile } ?: emptyList() else emptyList()

        val schemaVersion = database.openHelper.readableDatabase.version
        val dbChecksum = calculateSha256(dbFile)

        // Write comprehensive ZIP archive bundling SQLite database + product_images/ + metadata.json
        ZipOutputStream(FileOutputStream(zipBackupFile)).use { zos ->
            // Entry 1: Database
            val dbEntry = ZipEntry("database/advance_erp.db")
            zos.putNextEntry(dbEntry)
            FileInputStream(dbFile).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()

            // Entry 2..N: Product Images
            for (img in imageFiles) {
                val imgEntry = ZipEntry("product_images/${img.name}")
                zos.putNextEntry(imgEntry)
                FileInputStream(img).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            // Entry N+1: Metadata JSON
            val metadataContent = buildString {
                append("{\n")
                append("  \"application\": \"ADVANCE ERP\",\n")
                append("  \"package\": \"com.pocketdev.advance_erp\",\n")
                append("  \"timestamp\": ${System.currentTimeMillis()},\n")
                append("  \"schemaVersion\": $schemaVersion,\n")
                append("  \"dbChecksumSha256\": \"$dbChecksum\",\n")
                append("  \"totalImages\": ${imageFiles.size},\n")
                append("  \"notes\": \"${notes?.replace("\"", "\\\"") ?: "Offline Comprehensive Backup"}\"\n")
                append("}\n")
            }
            val metaEntry = ZipEntry("metadata.json")
            zos.putNextEntry(metaEntry)
            zos.write(metadataContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        // Calculate final ZIP SHA-256
        val checksum = calculateSha256(zipBackupFile)

        val record = BackupRecord(
            id = "bak-${UUID.randomUUID()}",
            fileName = fileName,
            filePath = zipBackupFile.absolutePath,
            fileSizeBytes = zipBackupFile.length(),
            checksumSha256 = checksum,
            schemaVersion = schemaVersion,
            isAutomatic = isAutomatic,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        backupDao.insertBackupRecord(record)

        auditLogDao.insertAuditLog(
            AuditLog(
                id = "audit-${UUID.randomUUID()}",
                recordType = "BACKUP",
                recordId = record.id,
                action = "BACKUP",
                details = "Created comprehensive ZIP backup $fileName (${record.fileSizeBytes / 1024} KB, ${imageFiles.size} images, schema v$schemaVersion, SHA-256: ${checksum.take(8)}...)"
            )
        )

        record
    }

    override suspend fun verifyBackupFile(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!backupFile.exists() || backupFile.length() < 100) return@withContext false

        if (backupFile.name.endsWith(".zip", ignoreCase = true)) {
            verifyZipBackup(backupFile)
        } else {
            verifySqliteDbFile(backupFile)
        }
    }

    private fun verifyZipBackup(zipBackupFile: File): Boolean {
        return try {
            ZipFile(zipBackupFile).use { zip ->
                val dbEntry = zip.getEntry("database/advance_erp.db") ?: return false
                val tempDbFile = File(context.cacheDir, "verify_db_${UUID.randomUUID()}.db")
                try {
                    zip.getInputStream(dbEntry).use { input ->
                        FileOutputStream(tempDbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    verifySqliteDbFile(tempDbFile)
                } finally {
                    if (tempDbFile.exists()) tempDbFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun verifySqliteDbFile(dbFile: File): Boolean {
        val header = ByteArray(16)
        return try {
            FileInputStream(dbFile).use { fis ->
                fis.read(header)
            }
            val headerStr = String(header, Charsets.US_ASCII)
            if (!headerStr.startsWith("SQLite format 3")) {
                return false
            }

            val testDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val cursor = testDb.rawQuery("PRAGMA quick_check;", null)
            var isOk = false
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                isOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()
            testDb.close()
            isOk
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun restoreBackup(backupFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!verifyBackupFile(backupFile)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid or corrupted backup file"))
            }

            val dbFile = context.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME)

            // Step 1: Create a safety rollback snapshot of current DB
            if (dbFile.exists()) {
                val safetyDir = File(context.filesDir, "safety_backups").apply { if (!exists()) mkdirs() }
                val safetyFile = File(safetyDir, "safety_before_restore_${System.currentTimeMillis()}.db.bak")
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(safetyFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Step 2: Checkpoint current DB
            try {
                database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
            } catch (e: Exception) {
                // Ignore if in-transit
            }

            // Step 3: Extract from ZIP or restore DB directly
            if (backupFile.name.endsWith(".zip", ignoreCase = true)) {
                ZipFile(backupFile).use { zip ->
                    val dbEntry = zip.getEntry("database/advance_erp.db")
                        ?: throw IllegalStateException("Backup ZIP does not contain database/advance_erp.db")

                    zip.getInputStream(dbEntry).use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Extract product images
                    val targetImagesDir = File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith("product_images/") && !entry.isDirectory) {
                            val imgFileName = entry.name.removePrefix("product_images/")
                            val destImgFile = File(targetImagesDir, imgFileName)
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(destImgFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            } else {
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Step 4: Remove stale WAL and SHM journal files
            val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
            val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            auditLogDao.insertAuditLog(
                AuditLog(
                    id = "audit-${UUID.randomUUID()}",
                    recordType = "RESTORE",
                    recordId = backupFile.name,
                    action = "RESTORE",
                    details = "Restored comprehensive database package from ${backupFile.name} successfully."
                )
            )

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
