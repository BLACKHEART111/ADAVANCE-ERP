package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import kotlinx.coroutines.flow.Flow

data class DashboardTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val isCredit: Boolean,
    val timestamp: Long,
    val statusText: String,
    val type: String
)

data class DateWiseMetrics(
    val selectedDateMillis: Long,
    val totalSales: Money,
    val totalPurchases: Money,
    val totalCollections: Money,
    val totalPayments: Money,
    val totalExpenses: Money,
    val totalReturns: Money,
    val stockInUnits: Int,
    val stockOutUnits: Int,
    val netCashFlow: Money,
    val dailyGrossProfit: Money,
    val transactionCount: Int
)

data class HistoricalClosingBalances(
    val asOfDateMillis: Long,
    val closingCashBank: Money,
    val customerOutstanding: Money,
    val supplierOutstanding: Money,
    val inventoryValuation: Money
)

data class DashboardSummary(
    val todaysSales: Money,
    val totalReceivable: Money,
    val totalPayable: Money,
    val stockValuation: Money,
    val productCount: Int,
    val lowStockCount: Int,
    val customerCount: Int,
    val supplierCount: Int,
    val recentTransactions: List<DashboardTransaction>,
    val isLiveDatabaseData: Boolean,
    val totalCashBank: Money = Money.ofMinor(0L, CurrencyCode.BDT),
    val totalExpenses: Money = Money.ofMinor(0L, CurrencyCode.BDT),
    val grossProfit: Money = Money.ofMinor(0L, CurrencyCode.BDT),
    val netProfit: Money = Money.ofMinor(0L, CurrencyCode.BDT),
    val dateWiseMetrics: DateWiseMetrics? = null,
    val closingBalances: HistoricalClosingBalances? = null
)

interface DashboardRepository {
    fun getDashboardSummary(): Flow<DashboardSummary>
    fun getDashboardSummaryForDate(dateMillis: Long): Flow<DashboardSummary>
}
