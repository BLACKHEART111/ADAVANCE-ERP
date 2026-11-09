package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 100): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE recordType = :recordType AND recordId = :recordId ORDER BY timestamp DESC")
    fun getAuditLogsForRecord(recordType: String, recordId: String): Flow<List<AuditLog>>
}
