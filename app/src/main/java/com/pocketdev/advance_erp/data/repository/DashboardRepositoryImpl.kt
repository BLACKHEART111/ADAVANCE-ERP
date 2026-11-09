package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.DashboardRepository
import com.pocketdev.advance_erp.domain.repository.DashboardSummary
import com.pocketdev.advance_erp.domain.repository.DashboardTransaction
import com.pocketdev.advance_erp.domain.repository.DateWiseMetrics
import com.pocketdev.advance_erp.domain.repository.HistoricalClosingBalances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val accountingDao: AccountingDao,
    private val expenseDao: ExpenseDao,
    private val salesDao: SalesDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val salesReturnDao: SalesReturnDao
) : DashboardRepository {

    override fun getDashboardSummary(): Flow<DashboardSummary> {
        return getDashboardSummaryForDate(System.currentTimeMillis())
    }

    override fun getDashboardSummaryForDate(dateMillis: Long): Flow<DashboardSummary> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val financialsFlow = combine(
            inventoryCostLayerDao.getTotalInventoryValuationMinor(),
            customerDao.getTotalReceivableMinor(),
            supplierDao.getTotalPayableMinor(),
            expenseDao.getTotalActiveExpenseBdtMinor()
        ) { stockVal, rec, pay, exp ->
            listOf(stockVal, rec, pay, exp)
        }

        val countsFlow = combine(
            productDao.getProductCount(),
            customerDao.getCustomerCount(),
            supplierDao.getSupplierCount(),
            stockBalanceDao.getLowStockItems()
        ) { prodCount, custCount, suppCount, lowStock ->
            listOf(prodCount, custCount, suppCount, lowStock.size)
        }

        return combine(
            financialsFlow,
            countsFlow,
            stockMovementDao.getMovementsInRange(startOfDay, endOfDay)
        ) { financials, counts, dayMovements ->
            val stockValuationMinor = financials[0]
            val receivableMinor = financials[1]
            val payableMinor = financials[2]
            val totalExpenseMinor = financials[3]

            val productCount = counts[0] as Int
            val customerCount = counts[1] as Int
            val supplierCount = counts[2] as Int
            val lowStockCount = counts[3] as Int

            // 1. Transactions on selected date
            val transactions = dayMovements.map { mov ->
                val totalAmount = mov.quantity.toLong() * mov.unitCostMinor
                val movType = StockMovementType.fromCode(mov.movementType)
                val isCredit = movType.isIncoming
                val statusText = when (movType) {
                    StockMovementType.PURCHASE_RECEIPT -> "In Stock"
                    StockMovementType.SALE_DISPATCH -> "Dispatched"
                    StockMovementType.SALE_RETURN -> "Returned (+)"
                    StockMovementType.PURCHASE_RETURN -> "Return Out (-)"
                    StockMovementType.STOCK_ADJUSTMENT_IN -> "Adjusted (+)"
                    StockMovementType.STOCK_ADJUSTMENT_OUT -> "Adjusted (-)"
                    StockMovementType.TRANSFER_OUT -> "Transfer Out"
                    StockMovementType.TRANSFER_IN -> "Transfer In"
                    StockMovementType.DAMAGE_WRITE_OFF -> "Damage Write-Off"
                    StockMovementType.SHORTAGE -> "Shortage (-)"
                    StockMovementType.OPENING_STOCK -> "Opening Stock"
                    StockMovementType.INVENTORY_COST_ADJUSTMENT -> "Cost Adjustment"
                }

                DashboardTransaction(
                    id = mov.id,
                    title = "${mov.productNameBn} (${mov.movementNumber})",
                    subtitle = "${mov.warehouseName} • ${mov.quantity} units",
                    amountMinor = totalAmount,
                    currency = CurrencyCode.BDT,
                    isCredit = isCredit,
                    timestamp = mov.createdAt,
                    statusText = statusText,
                    type = mov.movementType
                )
            }

            // 2. Date-wise business metrics calculations
            val postedInvoices = runCatching { salesDao.getPostedInvoicesInRange(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val salesMinor = postedInvoices.sumOf { it.totalAmountMinor }
            val cogsMinor = postedInvoices.sumOf { it.totalCogsMinor }
            val dailyGrossProfit = salesMinor - cogsMinor

            val orders = runCatching { purchaseOrderDao.getOrdersInRange(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val purchasesMinor = orders.sumOf { it.totalBdtAmountMinor }

            val custPayments = runCatching { customerPaymentDao.getPaymentsInRange(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val collectionsMinor = custPayments.sumOf { it.amountMinor }

            val suppPayments = runCatching { supplierPaymentDao.getPaymentsInRange(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val paymentsMinor = suppPayments.sumOf { it.bdtAmountMinor }

            val expenses = runCatching { expenseDao.getExpensesByDateRangeSync(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val dayExpensesMinor = expenses.sumOf { it.bdtAmountMinor }

            val returns = runCatching { salesReturnDao.getReturnsInRange(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val returnsMinor = returns.sumOf { it.totalReturnAmountMinor }

            val stockIn = dayMovements.filter { StockMovementType.fromCode(it.movementType).isIncoming }.sumOf { it.quantity }
            val stockOut = dayMovements.filter { !StockMovementType.fromCode(it.movementType).isIncoming }.sumOf { it.quantity }

            val netCashFlowMinor = collectionsMinor - paymentsMinor - dayExpensesMinor
            val transactionCount = postedInvoices.size + orders.size + custPayments.size + suppPayments.size + expenses.size + returns.size

            val dateWiseMetrics = DateWiseMetrics(
                selectedDateMillis = dateMillis,
                totalSales = Money.ofMinor(salesMinor, CurrencyCode.BDT),
                totalPurchases = Money.ofMinor(purchasesMinor, CurrencyCode.BDT),
                totalCollections = Money.ofMinor(collectionsMinor, CurrencyCode.BDT),
                totalPayments = Money.ofMinor(paymentsMinor, CurrencyCode.BDT),
                totalExpenses = Money.ofMinor(dayExpensesMinor, CurrencyCode.BDT),
                totalReturns = Money.ofMinor(returnsMinor, CurrencyCode.BDT),
                stockInUnits = stockIn,
                stockOutUnits = stockOut,
                netCashFlow = Money.ofMinor(netCashFlowMinor, CurrencyCode.BDT),
                dailyGrossProfit = Money.ofMinor(dailyGrossProfit, CurrencyCode.BDT),
                transactionCount = transactionCount
            )

            // 3. Historical Closing Balances up to endOfDay
            val activeLinesUpTo = try {
                accountingDao.getActivePostedLinesUpTo(endOfDay)
            } catch (e: Exception) {
                emptyList()
            }

            val cashBankIds = setOf("coa-1010", "coa-1020", "coa-1030")
            val cashBankLines = activeLinesUpTo.filter { it.accountId in cashBankIds }
            val closingCashBankMinor = cashBankLines.filter { it.isDebit }.sumOf { it.amountMinor } -
                    cashBankLines.filter { !it.isDebit }.sumOf { it.amountMinor }

            // AR & AP balances up to endOfDay
            val arLines = activeLinesUpTo.filter { it.accountId == "coa-1040" }
            val closingArMinor = if (arLines.isNotEmpty()) {
                arLines.filter { it.isDebit }.sumOf { it.amountMinor } - arLines.filter { !it.isDebit }.sumOf { it.amountMinor }
            } else {
                receivableMinor
            }

            val apLines = activeLinesUpTo.filter { it.accountId == "coa-2010" }
            val closingApMinor = if (apLines.isNotEmpty()) {
                apLines.filter { !it.isDebit }.sumOf { it.amountMinor } - apLines.filter { it.isDebit }.sumOf { it.amountMinor }
            } else {
                payableMinor
            }

            val invAssetLines = activeLinesUpTo.filter { it.accountId == "coa-1210" }
            val closingInvMinor = if (invAssetLines.isNotEmpty()) {
                invAssetLines.filter { it.isDebit }.sumOf { it.amountMinor } - invAssetLines.filter { !it.isDebit }.sumOf { it.amountMinor }
            } else {
                stockValuationMinor
            }

            val closingBalances = HistoricalClosingBalances(
                asOfDateMillis = endOfDay,
                closingCashBank = Money.ofMinor(closingCashBankMinor, CurrencyCode.BDT),
                customerOutstanding = Money.ofMinor(closingArMinor, CurrencyCode.BDT),
                supplierOutstanding = Money.ofMinor(closingApMinor, CurrencyCode.BDT),
                inventoryValuation = Money.ofMinor(closingInvMinor, CurrencyCode.BDT)
            )

            // Cumulative Profit & Loss metrics from accounting
            val grossSales = activeLinesUpTo.filter { it.accountId == "coa-4010" }
                .sumOf { if (!it.isDebit) it.amountMinor else -it.amountMinor }
            val salesReturnsAcc = activeLinesUpTo.filter { it.accountId == "coa-4020" }
                .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
            val salesDiscounts = activeLinesUpTo.filter { it.accountId == "coa-4030" }
                .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
            val netRevenue = grossSales - salesReturnsAcc - salesDiscounts

            val cogsAcc = activeLinesUpTo.filter { it.accountId == "coa-5010" }
                .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
            val grossProfitMinor = netRevenue - cogsAcc

            val opExpenseCodes = setOf("coa-5020", "coa-5030", "coa-5040", "coa-5110", "coa-5120", "coa-5130", "coa-5140", "coa-5150", "coa-5190")
            val opExpenses = activeLinesUpTo.filter { it.accountId in opExpenseCodes }
                .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
            val netProfitMinor = grossProfitMinor - opExpenses

            DashboardSummary(
                todaysSales = Money.ofMinor(salesMinor, CurrencyCode.BDT),
                totalReceivable = Money.ofMinor(closingArMinor, CurrencyCode.BDT),
                totalPayable = Money.ofMinor(closingApMinor, CurrencyCode.BDT),
                stockValuation = Money.ofMinor(closingInvMinor, CurrencyCode.BDT),
                productCount = productCount,
                lowStockCount = lowStockCount,
                customerCount = customerCount,
                supplierCount = supplierCount,
                recentTransactions = transactions,
                isLiveDatabaseData = true,
                totalCashBank = Money.ofMinor(closingCashBankMinor, CurrencyCode.BDT),
                totalExpenses = Money.ofMinor(totalExpenseMinor, CurrencyCode.BDT),
                grossProfit = Money.ofMinor(grossProfitMinor, CurrencyCode.BDT),
                netProfit = Money.ofMinor(netProfitMinor, CurrencyCode.BDT),
                dateWiseMetrics = dateWiseMetrics,
                closingBalances = closingBalances
            )
        }
    }
}
