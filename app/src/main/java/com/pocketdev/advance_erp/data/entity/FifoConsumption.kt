package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "fifo_consumptions",
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
        Index("costLayerId"),
        Index("productId"),
        Index("variantId"),
        Index("warehouseId"),
        Index("invoiceId"),
        Index("invoiceItemId"),
        Index("createdAt")
    ]
)
data class FifoConsumption(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String? = null,
    val invoiceItemId: String? = null,
    val costLayerId: String,
    val productId: String,
    val variantId: String? = null,
    val warehouseId: String,
    val quantity: Int,
    val unitCostBdtMinor: Long,
    val totalCogsBdtMinor: Long,
    val referenceType: String = "SALE", // e.g. "SALE", "TRANSFER_OUT", "DAMAGE_WRITE_OFF", "ADJUSTMENT"
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
