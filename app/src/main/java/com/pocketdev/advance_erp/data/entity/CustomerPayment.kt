package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customer_payments",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["paymentNumber"], unique = true),
        Index("customerId"),
        Index("paymentDate")
    ]
)
data class CustomerPayment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val paymentNumber: String,
    val customerId: String,
    val customerName: String,
    val amountMinor: Long,
    val allocatedAmountMinor: Long = 0L,
    val unallocatedCreditMinor: Long = 0L,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "CASH",
    val accountId: String = "coa-1010",
    val referenceNote: String? = null,
    val isAdvance: Boolean = false,
    val isPosted: Boolean = true,
    val isVoided: Boolean = false,
    val voidedAt: Long? = null,
    val voidReason: String? = null,
    val voidedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = CustomerPayment::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SalesInvoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("paymentId"),
        Index("invoiceId")
    ]
)
data class PaymentAllocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val paymentId: String,
    val invoiceId: String,
    val invoiceNumber: String,
    val amountMinor: Long,
    val allocatedAt: Long = System.currentTimeMillis()
)
