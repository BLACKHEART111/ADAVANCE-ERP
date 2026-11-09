package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "inventory_cost_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = InventoryCostLayer::class,
            parentColumns = ["id"],
            childColumns = ["costLayerId"],
            onDelete = ForeignKey.RESTRICT
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
        Index(value = ["adjustmentNumber"], unique = true),
        Index("costLayerId"),
        Index("productId"),
        Index("variantId"),
        Index("warehouseId"),
        Index("purchaseOrderId"),
        Index("createdAt")
    ]
)
data class InventoryCostAdjustment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val adjustmentNumber: String,
    val costLayerId: String,
    val productId: String,
    val variantId: String? = null,
    val warehouseId: String,
    val purchaseOrderId: String? = null,
    val referenceType: String? = null, // e.g. "FREIGHT_VARIANCE", "CUSTOMS_DUTY_ADJUSTMENT", "POST_LANDING_EXPENSE"
    val referenceId: String? = null,
    val reason: String,
    val oldUnitCostBdtMinor: Long,
    val newUnitCostBdtMinor: Long,
    val unitVarianceBdtMinor: Long, // newUnitCostBdtMinor - oldUnitCostBdtMinor
    val totalRemainingQty: Int,
    val totalSoldQty: Int,
    val inventoryAdjustmentAmountMinor: Long, // unitVarianceBdtMinor * totalRemainingQty
    val cogsAdjustmentAmountMinor: Long, // unitVarianceBdtMinor * totalSoldQty
    val totalAdjustmentAmountMinor: Long, // inventoryAdjustmentAmountMinor + cogsAdjustmentAmountMinor
    val journalEntryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
