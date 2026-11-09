package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ExpenseCategory(
    val code: String,
    val defaultAccountId: String,
    val titleEn: String,
    val titleBn: String
) {
    RENT_UTILITIES("5110", "coa-5110", "Office Rent & Warehouse Utilities", "অফিস ভাড়া ও ওয়্যারহাউস ইউটিলিটি"),
    SALARIES_BENEFITS("5120", "coa-5120", "Salaries, Wages & Benefits", "কর্মচারী বেতন ও সুবিধা"),
    CHINA_TRAVEL_SOURCING("5130", "coa-5130", "China Business Travel & Sourcing", "চীন ভ্রমণ ও সোর্সিং ব্যয়"),
    MARKETING_PROMOTION("5140", "coa-5140", "Marketing, Promotion & Samples", "মার্কেটিং ও স্যাম্পল ব্যয়"),
    BANK_FEES_COMMISSION("5150", "coa-5150", "Bank Charges, Commission & LC Fees", "ব্যাংক চার্জ ও এলসি ফি"),
    CUSTOMS_CLEARING("5020", "coa-5020", "Customs, Tariff & Port Charges", "কাস্টমস ও পোর্ট চার্জ"),
    SHIPPING_FREIGHT("5030", "coa-5030", "International Shipping & Freight", "আন্তর্জাতিক শিপিং ও ফ্রেইট"),
    LOCAL_LOGISTICS("5040", "coa-5040", "Warehouse Handling & Logistics", "ওয়্যারহাউস হ্যান্ডলিং ও স্থানীয় পরিবহন"),
    GENERAL_ADMIN("5190", "coa-5190", "General & Administrative Expenses", "সাধারণ ও প্রশাসনিক ব্যয়"),
    MISCELLANEOUS("5190", "coa-5190", "Miscellaneous Operating Expenses", "বিবিধ পরিচালনা ব্যয়")
}

@Entity(
    tableName = "expense_vouchers",
    foreignKeys = [
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["paymentAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["expenseAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["voucherNumber"], unique = true),
        Index("category"),
        Index("paymentAccountId"),
        Index("expenseAccountId"),
        Index("voucherDate")
    ]
)
data class ExpenseVoucher(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val voucherNumber: String,
    val category: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val amountMinor: Long,
    val currency: String = "BDT",
    val exchangeRateToBdt: String = "1.0000",
    val bdtAmountMinor: Long,
    val paymentAccountId: String,
    val expenseAccountId: String,
    val voucherDate: Long = System.currentTimeMillis(),
    val referenceNumber: String? = null,
    val attachmentPath: String? = null,
    val isPosted: Boolean = true,
    val isReversed: Boolean = false,
    val journalEntryId: String? = null,
    val reversalJournalEntryId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
