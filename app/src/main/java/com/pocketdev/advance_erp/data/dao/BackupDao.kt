package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.BackupRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(record: BackupRecord)

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun getAllBackupRecords(): Flow<List<BackupRecord>>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    suspend fun getAllBackupRecordsSync(): List<BackupRecord>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBackup(): BackupRecord?

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteBackupRecord(id: String)
}
