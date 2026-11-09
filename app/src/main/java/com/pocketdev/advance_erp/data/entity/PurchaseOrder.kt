package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PurchaseOrderStatus(val labelEn: String, val labelBn: String) {
    DRAFT("Draft", "খসড়া"),
    CONFIRMED("Confirmed", "নিশ্চিতকৃত"),
    PARTIALLY_PAID("Partially Paid", "আংশিক পরিশোধিত"),
    PAID("Paid", "পরিশোধিত"),
    SHIPPED("Shipped", "জাহাজীকরণ সম্পন্ন"),
    ACTUAL_COST_FINALIZED("Actual Cost Finalized", "প্রকৃত ব্যয় চূড়ান্তকৃত"),
    PARTIALLY_RECEIVED("Partially Received", "আংশিক গৃহীত"),
    RECEIVED("Received", "সম্পূর্ণ গৃহীত"),
    COMPLETED("Completed", "সম্পন্ন"),
    CANCELLED("Cancelled", "বাতিল")
}

@Entity(
    tableName = "purchase_orders",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("supplierId"),
        Index(value = ["poNumber"], unique = true)
    ]
)
data class PurchaseOrder(
    @PrimaryKey
    val id: String,
    val poNumber: String,
    val supplierId: String,
    val status: String = PurchaseOrderStatus.DRAFT.name,
    val currency: String = "CNY",
    val exchangeRateToBdt: String = "17.0000",
    val totalAmountMinor: Long = 0L,
    val totalBdtAmountMinor: Long = 0L,
    val paidAmountMinor: Long = 0L,
    val paidBdtAmountMinor: Long = 0L,
    val orderDate: Long = System.currentTimeMillis(),
    val expectedArrivalDate: Long? = null,
    val notes: String? = null,
    val isActualCostFinalized: Boolean = false,
    val actualCostFinalizedAt: Long? = null,
    val shippingRatePerKgBdt: String = "0.00",
    val totalGrossWeightKg: String = "0.000",
    val totalNetWeightKg: String = "0.000",
    val totalCbm: String = "0.000000",
    val totalCartons: Int = 0,
    val attachments: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
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
        Index("purchaseOrderId"),
        Index("productId"),
        Index("variantId")
    ]
)
data class PurchaseOrderItem(
    @PrimaryKey
    val id: String,
    val purchaseOrderId: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val unitPriceMinor: Long,
    val totalAmountMinor: Long,
    val totalBdtAmountMinor: Long,
    val receivedQuantity: Int = 0,
    val cartonQuantity: Int = 0,
    val pcsPerCarton: Int = 0,
    val grossWeightKg: String = "0.000",
    val netWeightKg: String = "0.000",
    val cbm: String = "0.000000"
)
