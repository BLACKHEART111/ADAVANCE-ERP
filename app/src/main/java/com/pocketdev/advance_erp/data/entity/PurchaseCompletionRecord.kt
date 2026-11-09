package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_completion_records",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("purchaseOrderId")
    ]
)
data class PurchaseCompletionRecord(
    @PrimaryKey
    val id: String,
    val purchaseOrderId: String,
    val completedAt: Long = System.currentTimeMillis(),
    val orderedQuantity: Int,
    val receivedQuantity: Int,
    val goodQuantity: Int,
    val damagedQuantity: Int,
    val missingQuantity: Int,
    val remainingQuantity: Int,
    val paymentDueMinor: Long,
    val unreceivedDisposition: String? = null, // "SHORTAGE", "CANCELLED_BALANCE", "SUPPLIER_CLAIM"
    val reason: String? = null,
    val notes: String? = null
)
