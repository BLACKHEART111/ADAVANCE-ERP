package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class InvoiceStatus(val titleEn: String, val titleBn: String) {
    DRAFT("Draft", "খসড়া"),
    QUOTATION("Quotation", "দরপত্র / কোটেশন"),
    SALES_ORDER("Sales Order", "বিক্রয় আদেশ"),
    CONFIRMED("Confirmed", "নিশ্চিত"),
    PARTIALLY_DELIVERED("Partially Delivered", "আংশিক ডেলিভারিকৃত"),
    DELIVERED("Delivered", "সম্পূর্ণ ডেলিভারিকৃত"),
    POSTED("Posted", "পোস্ট করা"),
    PARTIALLY_PAID("Partially Paid", "আংশিক পরিশোধিত"),
    PAID("Paid", "পরিশোধিত"),
    RETURNED("Returned", "ফেরতকৃত"),
    CANCELLED("Cancelled", "বাতিল")
}

@Entity(
    tableName = "sales_invoices",
    foreignKeys = [
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
        Index(value = ["invoiceNumber"], unique = true),
        Index("customerId"),
        Index("warehouseId"),
        Index("status"),
        Index("invoiceDate")
    ]
)
data class SalesInvoice(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String? = null,
    val warehouseId: String,
    val warehouseName: String,
    val status: String = InvoiceStatus.DRAFT.name,
    val subtotalMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalAmountMinor: Long = 0L,
    val paidAmountMinor: Long = 0L,
    val dueAmountMinor: Long = 0L,
    val totalCogsMinor: Long = 0L,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String? = null,
    val isPosted: Boolean = false,
    val postedAt: Long? = null,
    val cancellationReason: String? = null,
    val cancelledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales_invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
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
        Index("invoiceId"),
        Index("productId"),
        Index("variantId")
    ]
)
data class SalesInvoiceItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val productId: String,
    val variantId: String? = null,
    val productNameEn: String,
    val productNameBn: String,
    val sku: String,
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val unitPriceMinor: Long,
    val discountMinor: Long = 0L,
    val lineTotalMinor: Long,
    val unitCostMinor: Long = 0L,
    val totalCogsMinor: Long = 0L
)

@Entity(
    tableName = "sales_cost_layer_usages",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("invoiceId"),
        Index("invoiceItemId"),
        Index("costLayerId")
    ]
)
data class SalesCostLayerUsage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val invoiceItemId: String,
    val costLayerId: String,
    val quantityDepleted: Int,
    val unitCostMinor: Long
)
