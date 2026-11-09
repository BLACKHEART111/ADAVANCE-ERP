package com.pocketdev.advance_erp.domain.model

data class AccountSummaryItem(
    val accountId: String,
    val accountCode: String,
    val nameEn: String,
    val nameBn: String,
    val amountMinor: Long
)

data class CashBankBookEntry(
    val id: String,
    val date: Long,
    val referenceNumber: String,
    val description: String,
    val receiptMinor: Long, // Debit
    val paymentMinor: Long, // Credit
    val runningBalanceMinor: Long,
    val accountCode: String,
    val accountName: String
)

data class ProfitAndLossStatement(
    val fromDate: Long,
    val toDate: Long,
    val grossSalesMinor: Long,
    val salesReturnsMinor: Long,
    val salesDiscountsMinor: Long,
    val netSalesRevenueMinor: Long,
    val costOfGoodsSoldMinor: Long,
    val grossProfitMinor: Long,
    val operatingExpenses: List<AccountSummaryItem>,
    val totalOperatingExpensesMinor: Long,
    val operatingProfitMinor: Long,
    val foreignExchangeGainLossMinor: Long,
    val netProfitMinor: Long
)

data class BalanceSheet(
    val asOfDate: Long,
    val currentAssets: List<AccountSummaryItem>,
    val totalCurrentAssetsMinor: Long,
    val totalAssetsMinor: Long,
    val currentLiabilities: List<AccountSummaryItem>,
    val totalCurrentLiabilitiesMinor: Long,
    val totalLiabilitiesMinor: Long,
    val equityItems: List<AccountSummaryItem>,
    val retainedEarningsMinor: Long,
    val totalEquityMinor: Long,
    val totalLiabilitiesAndEquityMinor: Long,
    val isBalanced: Boolean
)

data class CashFlowStatement(
    val fromDate: Long,
    val toDate: Long,
    val cashInflowCustomersMinor: Long,
    val cashOutflowSuppliersMinor: Long,
    val cashOutflowExpensesMinor: Long,
    val netCashFromOperationsMinor: Long,
    val netCashFromInvestingMinor: Long,
    val netCashFromFinancingMinor: Long,
    val netChangeInCashMinor: Long,
    val openingCashBankMinor: Long,
    val closingCashBankMinor: Long
)

data class AgingBucket(
    val labelEn: String,
    val labelBn: String,
    val amountMinor: Long,
    val percentage: Double
)

data class AgingReport(
    val reportType: String, // "RECEIVABLES" or "PAYABLES"
    val totalOutstandingMinor: Long,
    val buckets: List<AgingBucket>
)
