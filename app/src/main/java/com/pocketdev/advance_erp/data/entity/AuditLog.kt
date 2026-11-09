package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "audit_logs",
    indices = [
        Index("recordType"),
        Index("recordId"),
        Index("action"),
        Index("timestamp")
    ]
)
data class AuditLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recordType: String,
    val recordId: String,
    val action: String,
    val details: String,
    val operator: String = "SYSTEM_ADMIN",
    val timestamp: Long = System.currentTimeMillis(),
    val documentNumber: String? = null,
    val operationType: String? = null,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val cancellationReason: String? = null,
    val reversalReference: String? = null
)
