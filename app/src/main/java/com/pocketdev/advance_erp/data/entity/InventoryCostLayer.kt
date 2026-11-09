package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_cost_layers",
    foreignKeys = [
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
        Index("productId"),
        Index("variantId"),
        Index("warehouseId"),
        Index("shipmentNumber"),
        Index("receiptNumber"),
        Index("purchaseOrderId")
    ]
)
data class InventoryCostLayer(
    @PrimaryKey
    val id: String,
    val productId: String,
    val variantId: String? = null,
    val warehouseId: String,
    val shipmentId: String,
    val shipmentNumber: String,
    val warehouseReceiptId: String,
    val receiptNumber: String,
    val initialQuantity: Int,
    val remainingQuantity: Int,
    val unitLandedCostBdtMinor: Long,
    val originalCurrency: String = "CNY",
    val originalPurchasePriceMinor: Long,
    val exchangeRateToBdt: String,
    val allocatedExpensePerUnitBdtMinor: Long,
    val receiptDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val purchaseOrderId: String? = null,
    val purchaseBatchId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
