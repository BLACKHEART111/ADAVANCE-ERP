package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "inventory_reconciliations",
    foreignKeys = [
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["reconciliationNumber"], unique = true),
        Index("warehouseId"),
        Index("createdAt")
    ]
)
data class InventoryReconciliation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reconciliationNumber: String,
    val warehouseId: String? = null, // null means all warehouses
    val status: String, // "MATCHED", "DISCREPANCY_DETECTED"
    val totalStockBalanceQty: Int,
    val totalMovementQty: Int,
    val totalFifoRemainingQty: Int,
    val totalFifoValuationMinor: Long,
    val account1210BalanceMinor: Long,
    val quantityDiscrepancy: Int,
    val valuationDiscrepancyMinor: Long,
    val notes: String? = null,
    val reconciledBy: String = "System Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_reconciliation_items",
    foreignKeys = [
        ForeignKey(
            entity = InventoryReconciliation::class,
            parentColumns = ["id"],
            childColumns = ["reconciliationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("reconciliationId"),
        Index("productId"),
        Index("variantId"),
        Index("warehouseId")
    ]
)
data class InventoryReconciliationItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reconciliationId: String,
    val productId: String,
    val variantId: String? = null,
    val warehouseId: String,
    val stockBalanceQty: Int,
    val movementNetQty: Int,
    val fifoRemainingQty: Int,
    val fifoValuationMinor: Long,
    val qtyMismatch: Boolean,
    val details: String? = null
)
