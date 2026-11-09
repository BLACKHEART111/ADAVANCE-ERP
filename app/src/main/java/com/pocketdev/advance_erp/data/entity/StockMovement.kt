package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
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
        )
    ],
    indices = [
        Index(value = ["movementNumber"], unique = true),
        Index(value = ["warehouseId"]),
        Index(value = ["productId"]),
        Index(value = ["variantId"]),
        Index(value = ["movementType"]),
        Index(value = ["purchaseOrderId"]),
        Index(value = ["salesInvoiceId"]),
        Index(value = ["stockTransferId"]),
        Index(value = ["createdAt"])
    ]
)
data class StockMovement(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val movementNumber: String,
    val warehouseId: String,
    val productId: String,
    val variantId: String? = null,
    val movementType: String,
    val quantity: Int,
    val unitCostMinor: Long,
    val balanceAfterMovement: Int = 0,
    val totalCostImpactMinor: Long = 0L,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val purchaseOrderId: String? = null,
    val purchaseBatchId: String? = null,
    val salesInvoiceId: String? = null,
    val stockTransferId: String? = null,
    val operatorName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
