package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "damage_records",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WarehouseReceipt::class,
            parentColumns = ["id"],
            childColumns = ["warehouseReceiptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("purchaseOrderId"),
        Index("warehouseReceiptId"),
        Index("productId"),
        Index("warehouseId"),
        Index(value = ["recordNumber"], unique = true)
    ]
)
data class DamageRecord(
    @PrimaryKey
    val id: String,
    val recordNumber: String,
    val purchaseOrderId: String,
    val warehouseReceiptId: String,
    val warehouseId: String,
    val productId: String,
    val variantId: String? = null,
    val damagedQuantity: Int,
    val unitCostMinor: Long,
    val totalLossMinor: Long,
    val reason: String? = null,
    val disposition: String = "WRITE_OFF",
    val journalEntryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
