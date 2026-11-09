package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod(val labelEn: String, val labelBn: String) {
    BANK_TT("Bank T/T Telegraphic Transfer", "ব্যাংক টি/টি রেমিট্যান্স"),
    LETTER_OF_CREDIT("Letter of Credit (LC)", "লেটার অব ক্রেডিট (এলসি)"),
    CASH("Cash / Hawala", "নগদ / হুন্ডি"),
    OTHER("Other Payment Gateway", "অন্যান্য মাধ্যম")
}

@Entity(
    tableName = "supplier_payments",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("supplierId"),
        Index("purchaseOrderId"),
        Index(value = ["paymentNumber"], unique = true)
    ]
)
data class SupplierPayment(
    @PrimaryKey
    val id: String,
    val paymentNumber: String,
    val supplierId: String,
    val purchaseOrderId: String? = null,
    val amountMinor: Long,
    val currency: String = "CNY",
    val exchangeRateToBdt: String = "17.0000",
    val bdtAmountMinor: Long,
    val paymentMethod: String = PaymentMethod.BANK_TT.name,
    val paymentAccount: String = "City Bank Corporate (Import LC)",
    val referenceNumber: String? = null,
    val notes: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
