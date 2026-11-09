package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class StockTransferStatus(val titleEn: String, val titleBn: String) {
    DRAFT("Draft", "খসড়া"),
    IN_TRANSIT("In Transit", "ট্রানজিটে রয়েছে"),
    RECEIVED("Received", "গৃহীত"),
    CANCELLED("Cancelled", "বাতিল")
}

@Entity(
    tableName = "stock_transfers",
    foreignKeys = [
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["sourceWarehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["destinationWarehouseId"],
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
        Index(value = ["transferNumber"], unique = true),
        Index("sourceWarehouseId"),
        Index("destinationWarehouseId"),
        Index("productId"),
        Index("variantId"),
        Index("status"),
        Index("transferDate")
    ]
)
data class StockTransfer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val transferNumber: String,
    val sourceWarehouseId: String,
    val destinationWarehouseId: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val transferDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val status: String = StockTransferStatus.DRAFT.name,
    val dispatchedAt: Long? = null,
    val receivedAt: Long? = null,
    val cancelledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_transfer_cost_layers",
    foreignKeys = [
        ForeignKey(
            entity = StockTransfer::class,
            parentColumns = ["id"],
            childColumns = ["transferId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InventoryCostLayer::class,
            parentColumns = ["id"],
            childColumns = ["costLayerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("transferId"),
        Index("costLayerId")
    ]
)
data class StockTransferCostLayer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val transferId: String,
    val costLayerId: String,
    val quantity: Int,
    val unitCostMinor: Long
)
