package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReceiptStatus(val labelEn: String, val labelBn: String) {
    DRAFT("Draft Inspection", "খসড়া পরিদর্শন"),
    POSTED("Posted to Stock", "মজুদ পোস্টিং সম্পন্ন"),
    CANCELLED("Cancelled", "বাতিল")
}

@Entity(
    tableName = "warehouse_receipts",
    foreignKeys = [
        ForeignKey(
            entity = ImportShipment::class,
            parentColumns = ["id"],
            childColumns = ["shipmentId"],
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
        Index("shipmentId"),
        Index("warehouseId"),
        Index("purchaseOrderId"),
        Index(value = ["receiptNumber"], unique = true)
    ]
)
data class WarehouseReceipt(
    @PrimaryKey
    val id: String,
    val receiptNumber: String,
    val shipmentId: String,
    val warehouseId: String,
    val receiptDate: Long = System.currentTimeMillis(),
    val status: String = ReceiptStatus.DRAFT.name,
    val receivedBy: String = "Tejgaon Warehouse Manager",
    val hasDiscrepancies: Boolean = false,
    val discrepancyApproved: Boolean = false,
    val notes: String? = null,
    val purchaseOrderId: String? = null,
    val receivingMethod: String = "PHYSICAL_COUNT", // PHYSICAL_COUNT or CARTON_WISE
    val createdAt: Long = System.currentTimeMillis(),
    val postedAt: Long? = null
)

@Entity(
    tableName = "warehouse_receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseReceipt::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
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
        )
    ],
    indices = [
        Index("receiptId"),
        Index("productId"),
        Index("variantId")
    ]
)
data class WarehouseReceiptItem(
    @PrimaryKey
    val id: String,
    val receiptId: String,
    val cartonId: String? = null,
    val productId: String,
    val variantId: String? = null,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val damagedQuantity: Int = 0,
    val missingQuantity: Int = 0,
    val excessQuantity: Int = 0,
    val unitLandedCostMinor: Long = 0L,
    val conditionNotes: String? = null
)
