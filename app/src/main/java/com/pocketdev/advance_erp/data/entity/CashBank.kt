package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "account_transfers",
    foreignKeys = [
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transferNumber"], unique = true),
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("transferDate")
    ]
)
data class AccountTransfer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val transferNumber: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val transferDate: Long = System.currentTimeMillis(),
    val referenceNumber: String? = null,
    val notes: String? = null,
    val journalEntryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReconciliationStatus(val labelEn: String, val labelBn: String) {
    IN_PROGRESS("In Progress", "প্রক্রিয়াধীন"),
    RECONCILED("Reconciled (Matched)", "সমন্বিত (মিলিত)"),
    DISCREPANCY("Discrepancy (Unresolved)", "অমিল (অমীমাংসিত)")
}

@Entity(
    tableName = "bank_reconciliations",
    foreignKeys = [
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["bankAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["reconciliationNumber"], unique = true),
        Index("bankAccountId"),
        Index("statementEndDate")
    ]
)
data class BankReconciliation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reconciliationNumber: String,
    val bankAccountId: String,
    val statementStartDate: Long,
    val statementEndDate: Long,
    val statementOpeningBalanceMinor: Long,
    val statementClosingBalanceMinor: Long,
    val erpClosingBalanceMinor: Long,
    val reconciledBalanceMinor: Long,
    val discrepancyMinor: Long,
    val status: String = ReconciliationStatus.IN_PROGRESS.name,
    val notes: String? = null,
    val reconciledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bank_reconciliation_items",
    foreignKeys = [
        ForeignKey(
            entity = BankReconciliation::class,
            parentColumns = ["id"],
            childColumns = ["reconciliationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("reconciliationId"),
        Index("journalLineId")
    ]
)
data class BankReconciliationItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reconciliationId: String,
    val journalLineId: String? = null,
    val transactionDate: Long,
    val description: String,
    val amountMinor: Long,
    val isDebit: Boolean,
    val isMatched: Boolean = false,
    val statementReference: String? = null,
    val matchedAt: Long? = null
)
