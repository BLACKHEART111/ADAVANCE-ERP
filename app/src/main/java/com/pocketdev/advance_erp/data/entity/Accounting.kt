package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val titleEn: String, val titleBn: String) {
    ASSET("Asset", "সম্পদ"),
    LIABILITY("Liability", "দায়"),
    EQUITY("Equity", "মালিকানাস্বত্ব"),
    REVENUE("Revenue", "আয়"),
    EXPENSE("Expense", "ব্যয়")
}

enum class NormalBalance {
    DEBIT, CREDIT
}

@Entity(
    tableName = "chart_of_accounts",
    indices = [
        Index(value = ["accountCode"], unique = true)
    ]
)
data class ChartOfAccount(
    @PrimaryKey
    val id: String,
    val accountCode: String,
    val nameEn: String,
    val nameBn: String,
    val accountType: String,
    val normalBalance: String,
    val description: String? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index("referenceType"),
        Index("referenceId")
    ]
)
data class JournalEntry(
    @PrimaryKey
    val id: String,
    val entryNumber: String,
    val entryDate: Long = System.currentTimeMillis(),
    val descriptionEn: String,
    val descriptionBn: String,
    val referenceType: String,
    val referenceId: String? = null,
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val isPosted: Boolean = true,
    val isReversed: Boolean = false,
    val reversalOfEntryId: String? = null,
    val reversalEntryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("journalEntryId"),
        Index("accountId")
    ]
)
data class JournalLine(
    @PrimaryKey
    val id: String,
    val journalEntryId: String,
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val isDebit: Boolean,
    val amountMinor: Long,
    val description: String? = null
)

data class TrialBalanceItem(
    val accountId: String,
    val accountCode: String,
    val accountNameEn: String,
    val accountNameBn: String,
    val accountType: String,
    val normalBalance: String,
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val netDebitMinor: Long,
    val netCreditMinor: Long
)
