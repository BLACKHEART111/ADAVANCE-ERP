package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "backup_records",
    indices = [
        Index("createdAt"),
        Index("isAutomatic")
    ]
)
data class BackupRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val schemaVersion: Int,
    val isAutomatic: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
