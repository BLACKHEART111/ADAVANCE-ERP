package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ReturnRefundOption(val titleEn: String, val titleBn: String) {
    CUSTOMER_CREDIT("Customer Credit", "গ্রাহকের হিসাবের ক্রেডিট"),
    CASH_REFUND("Cash Refund", "নগদ মূল্য ফেরত"),
    BANK_REFUND("Bank Refund", "ব্যাংক রিফান্ড")
}

@Entity(
    tableName = "sales_returns",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
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
        Index(value = ["returnNumber"], unique = true),
        Index("invoiceId"),
        Index("customerId"),
        Index("warehouseId"),
        Index("returnDate")
    ]
)
data class SalesReturn(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val returnNumber: String,
    val invoiceId: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val warehouseId: String,
    val totalReturnAmountMinor: Long,
    val totalCogsReversalMinor: Long,
    val refundOption: String = ReturnRefundOption.CUSTOMER_CREDIT.name,
    val reason: String? = null,
    val returnDate: Long = System.currentTimeMillis(),
    val isPosted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales_return_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesReturn::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("returnId"),
        Index("productId"),
        Index("invoiceItemId")
    ]
)
data class SalesReturnItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val returnId: String,
    val invoiceItemId: String,
    val productId: String,
    val variantId: String? = null,
    val productNameEn: String,
    val productNameBn: String,
    val returnQuantity: Int,
    val unitPriceMinor: Long,
    val returnTotalMinor: Long,
    val originalUnitCostMinor: Long,
    val cogsReversalMinor: Long
)
