package com.pocketdev.advance_erp.domain.model

import java.util.Calendar

enum class DateRangeOption(val titleEn: String, val titleBn: String) {
    ALL_TIME("All Time", "সকল সময়"),
    TODAY("Today", "আজকের হিসাব"),
    YESTERDAY("Yesterday", "গতকাল"),
    LAST_7_DAYS("Last 7 Days", "গত ৭ দিন"),
    LAST_30_DAYS("Last 30 Days", "গত ৩০ দিন"),
    THIS_WEEK("This Week", "চলতি সপ্তাহ"),
    THIS_MONTH("This Month", "চলতি মাস"),
    LAST_MONTH("Last Month", "গত মাস"),
    THIS_YEAR("This Year", "চলতি বছর"),
    LAST_YEAR("Last Year", "গত বছর"),
    CUSTOM("Custom Range", "কাস্টম সময়সীমা");

    fun getBoundsMillis(customStart: Long? = null, customEnd: Long? = null): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        fun startOfDay(c: Calendar) {
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
        }

        fun endOfDay(c: Calendar) {
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
        }

        return when (this) {
            ALL_TIME -> 0L to Long.MAX_VALUE
            TODAY -> {
                val start = Calendar.getInstance().apply { startOfDay(this) }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            YESTERDAY -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    endOfDay(this)
                }.timeInMillis
                start to end
            }
            LAST_7_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            LAST_30_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            THIS_WEEK -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            THIS_MONTH -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            LAST_MONTH -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.DAY_OF_MONTH, -1)
                    endOfDay(this)
                }.timeInMillis
                start to end
            }
            THIS_YEAR -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply { endOfDay(this) }.timeInMillis
                start to end
            }
            LAST_YEAR -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.DAY_OF_YEAR, 1)
                    startOfDay(this)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    add(Calendar.DAY_OF_YEAR, -1)
                    endOfDay(this)
                }.timeInMillis
                start to end
            }
            CUSTOM -> {
                val rawS = customStart?.let {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                        startOfDay(this)
                    }.timeInMillis
                } ?: 0L
                val rawE = customEnd?.let {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                        endOfDay(this)
                    }.timeInMillis
                } ?: Long.MAX_VALUE
                val s = minOf(rawS, rawE)
                val e = maxOf(rawS, rawE)
                s to e
            }
        }
    }
}

enum class ReportCategory(val titleEn: String, val titleBn: String) {
    ALL_TRANSACTIONS("All Transactions", "সকল লেনদেন"),
    SALES("Sales", "বিক্রয়"),
    PURCHASE("Purchase", "ক্রয়"),
    IMPORT("Import", "আমদানি"),
    CUSTOMER_LEDGER("Customer Ledger", "কাস্টমার হিসাব"),
    SUPPLIER_LEDGER("Supplier Ledger", "সাপ্লায়ার হিসাব"),
    PAYMENT("Payment", "টাকা পরিশোধ"),
    RECEIPT("Receipt", "টাকা গ্রহণ"),
    INCOME("Income", "আয়"),
    EXPENSE("Expense", "ব্যয়"),
    INVENTORY("Inventory", "স্টক হিসাব"),
    DELIVERY("Delivery", "ডেলিভারি"),
    SALES_RETURN("Sales Return", "বিক্রয় ফেরত"),
    PURCHASE_RETURN("Purchase Return", "ক্রয় ফেরত"),
    STOCK_TRANSFER("Stock Transfer", "স্টক স্থানান্তর"),
    STOCK_ADJUSTMENT("Stock Adjustment", "স্টক সমন্বয়")
}

enum class PaymentStatusFilter(val titleEn: String, val titleBn: String) {
    ALL("All", "সকল"),
    PAID("Paid", "সম্পূর্ণ পরিশোধিত"),
    UNPAID("Unpaid", "অপরিশোধিত"),
    PARTIAL_PAID("Partial Paid", "আংশিক পরিশোধিত"),
    OVERDUE("Overdue", "মেয়াদোত্তীর্ণ বকেয়া"),
    ADVANCE("Advance Payment", "অগ্রিম পরিশোধ"),
    CASH("Cash", "নগদ"),
    BANK("Bank", "ব্যাংক"),
    MOBILE_BANKING("Mobile Banking", "মোবাইল ব্যাংকিং"),
    LC("LC Payment", "এলসি পেমেন্ট"),
    TT("TT Payment", "টিটি পেমেন্ট")
}

enum class DeliveryReturnFilter(val titleEn: String, val titleBn: String) {
    ALL("All Delivery & Return", "সকল ডেলিভারি ও রিটার্ন"),
    PENDING("Pending Delivery", "অপেক্ষমাণ"),
    PROCESSING("Processing Delivery", "প্রক্রিয়াধীন"),
    DISPATCHED("Dispatched Delivery", "প্রেরণ করা হয়েছে"),
    DELIVERED("Delivered", "ডেলিভারি সম্পন্ন"),
    PARTIAL_DELIVERED("Partial Delivered", "আংশিক ডেলিভারি"),
    CANCELLED("Cancelled Delivery", "বাতিল"),
    ALL_RETURNS("All Returns", "সকল রিটার্ন"),
    APPROVED_RETURN("Approved Return", "অনুমোদিত রিটার্ন"),
    PENDING_RETURN("Pending Return", "অপেক্ষমাণ রিটার্ন"),
    REJECTED_RETURN("Rejected Return", "বাতিল রিটার্ন"),
    DAMAGED_PRODUCT("Damaged Product", "ক্ষতিগ্রস্ত পণ্য"),
    GOOD_CONDITION("Good Condition Return", "ভালো অবস্থায় ফেরত")
}

enum class FinancialMetricFilter(val titleEn: String, val titleBn: String) {
    ALL("All Financials", "সকল আর্থিক হিসাব"),
    INCOME("Income", "আয়"),
    EXPENSE("Expense", "ব্যয়"),
    PROFIT("Gross / Net Profit", "লাভ"),
    LOSS("Loss / Negative Margins", "ক্ষতি"),
    CUSTOMER_DUE("Customer Due", "কাস্টমারের বকেয়া"),
    SUPPLIER_DUE("Supplier Due", "সাপ্লায়ারের দেনা"),
    OPENING_BALANCE("Opening Balance", "প্রারম্ভিক ব্যালেন্স"),
    CLOSING_BALANCE("Closing Balance", "সমাপনী ব্যালেন্স"),
    DISCOUNT("Discount Allowed", "ছাড়"),
    TAX("Tax / VAT", "কর / ভ্যাট"),
    REFUND("Refund Issued", "টাকা ফেরত")
}

enum class AmountRangeFilter(val titleEn: String, val titleBn: String) {
    ALL("All Amounts", "যে কোনো পরিমাণ"),
    RANGE_0_1K("৳ 0 – 1,000", "০–১,০০০ টাকা"),
    RANGE_1K_10K("৳ 1,000 – 10,000", "১,০০০–১০,০০০ টাকা"),
    RANGE_10K_50K("৳ 10,000 – 50,000", "১০,০০০–৫০,০০০ টাকা"),
    ABOVE_50K("Above ৳ 50,000", "৫০,০০০ টাকার বেশি"),
    CUSTOM("Custom Amount Range", "কাস্টম টাকার পরিমাণ")
}

enum class SortOption(val titleEn: String, val titleBn: String) {
    NEWEST_FIRST("Newest First", "সর্বশেষ লেনদেন আগে"),
    OLDEST_FIRST("Oldest First", "পুরোনো লেনদেন আগে"),
    HIGHEST_AMOUNT("Highest Amount", "সর্বোচ্চ টাকার পরিমাণ আগে"),
    LOWEST_AMOUNT("Lowest Amount", "সর্বনিম্ন টাকার পরিমাণ আগে"),
    NAME_ASC("Name (A–Z)", "নাম অনুযায়ী সাজানো (A–Z)"),
    NAME_DESC("Name (Z–A)", "বিপরীত ক্রমে সাজানো (Z–A)")
}

data class ControlCenterFilterState(
    val dateRangeOption: DateRangeOption = DateRangeOption.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val category: ReportCategory = ReportCategory.ALL_TRANSACTIONS,
    val searchQuery: String = "",
    val selectedCustomerId: String? = null,
    val selectedSupplierId: String? = null,
    val selectedWarehouseId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedBrand: String? = null,
    val paymentStatus: PaymentStatusFilter = PaymentStatusFilter.ALL,
    val deliveryReturn: DeliveryReturnFilter = DeliveryReturnFilter.ALL,
    val financialMetric: FinancialMetricFilter = FinancialMetricFilter.ALL,
    val amountFilter: AmountRangeFilter = AmountRangeFilter.ALL,
    val customMinAmountMinor: Long? = null,
    val customMaxAmountMinor: Long? = null,
    val sortBy: SortOption = SortOption.NEWEST_FIRST
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (dateRangeOption != DateRangeOption.ALL_TIME) count++
            if (category != ReportCategory.ALL_TRANSACTIONS) count++
            if (searchQuery.isNotBlank()) count++
            if (selectedCustomerId != null) count++
            if (selectedSupplierId != null) count++
            if (selectedWarehouseId != null) count++
            if (selectedCategoryId != null) count++
            if (selectedBrand != null) count++
            if (paymentStatus != PaymentStatusFilter.ALL) count++
            if (deliveryReturn != DeliveryReturnFilter.ALL) count++
            if (financialMetric != FinancialMetricFilter.ALL) count++
            if (amountFilter != AmountRangeFilter.ALL) count++
            return count
        }
}

data class BusinessSummaryDashboardData(
    val totalSalesMinor: Long = 0L,
    val totalPurchaseMinor: Long = 0L,
    val totalReceivedMinor: Long = 0L,
    val totalPaidMinor: Long = 0L,
    val totalExpensesMinor: Long = 0L,
    val totalSalesReturnMinor: Long = 0L,
    val totalPurchaseReturnMinor: Long = 0L,
    val totalDeliveryCount: Int = 0,
    val totalOutstandingReceivableMinor: Long = 0L,
    val totalOutstandingPayableMinor: Long = 0L,
    val grossProfitMinor: Long = 0L,
    val netProfitMinor: Long = 0L,
    val totalTransactionsCount: Int = 0,
    val openingBalanceMinor: Long = 0L,
    val closingBalanceMinor: Long = 0L
)

data class UnifiedTransactionItem(
    val id: String,
    val referenceNumber: String,
    val date: Long,
    val category: ReportCategory,
    val primaryEntityName: String,
    val primaryEntityId: String? = null,
    val particulars: String,
    val amountMinor: Long,
    val secondaryAmountMinor: Long? = null, // e.g. Due amount or COGS
    val isInflow: Boolean, // Inflow = Debit / Collection, Outflow = Credit / Payment
    val paymentMethod: String? = null,
    val status: String,
    val statusColorHex: Long = 0xFF475569,
    val warehouseName: String? = null,
    val runningBalanceMinor: Long? = null,
    val rawType: String, // INVOICE, PAYMENT, EXPENSE, PURCHASE, RETURN, STOCK_MOVEMENT, STOCK_TRANSFER
    val lineItemsCount: Int = 0,
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: Long = date
)

data class LineItemDetail(
    val productName: String,
    val sku: String,
    val variantInfo: String? = null,
    val quantity: Int,
    val unitPriceMinor: Long,
    val totalAmountMinor: Long
)

data class TransactionDetailRecord(
    val transaction: UnifiedTransactionItem,
    val items: List<LineItemDetail> = emptyList(),
    val subtotalMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val paidMinor: Long = 0L,
    val dueMinor: Long = 0L,
    val contactPhone: String? = null,
    val contactAddress: String? = null,
    val referenceDocument: String? = null,
    val auditTrailInfo: String? = null
)

data class SavedFilterPreset(
    val id: String,
    val name: String,
    val filterState: ControlCenterFilterState,
    val createdAt: Long = System.currentTimeMillis()
)

data class PartyLedgerStatement(
    val partyId: String,
    val partyName: String,
    val partyCode: String,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val phone: String?,
    val address: String?,
    val openingBalanceMinor: Long,
    val transactions: List<UnifiedTransactionItem>,
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val closingBalanceMinor: Long
)
