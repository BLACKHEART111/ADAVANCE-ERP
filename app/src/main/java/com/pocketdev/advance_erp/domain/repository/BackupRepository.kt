package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.BackupRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface BackupRepository {
    fun getAllBackupRecords(): Flow<List<BackupRecord>>
    suspend fun createBackup(isAutomatic: Boolean = false, notes: String? = null): BackupRecord
    suspend fun restoreBackup(backupFile: File): Result<Boolean>
    suspend fun verifyBackupFile(backupFile: File): Boolean
}
