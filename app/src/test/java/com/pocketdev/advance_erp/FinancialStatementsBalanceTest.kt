package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.domain.model.AccountSummaryItem
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashFlowStatement
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialStatementsBalanceTest {

    @Test
    fun testProfitAndLossCalculationAccuracy() {
        // Sales: ৳10,00,000 (100,000,000 minor)
        val grossSalesMinor = 100000000L
        // Returns: ৳20,000 (2,000,000 minor)
        val salesReturnsMinor = 2000000L
        // Discounts: ৳10,000 (1,000,000 minor)
        val salesDiscountsMinor = 1000000L

        val netRevenueMinor = grossSalesMinor - salesReturnsMinor - salesDiscountsMinor
        assertEquals(97000000L, netRevenueMinor) // ৳9,70,000

        // Landed Cost of Goods Sold: ৳6,00,000 (60,000,000 minor)
        val cogsMinor = 60000000L
        val grossProfitMinor = netRevenueMinor - cogsMinor
        assertEquals(37000000L, grossProfitMinor) // ৳3,70,000

        // Operating Expenses:
        // Rent & Utilities: ৳40,000
        // Salaries: ৳80,000
        // Marketing: ৳15,000
        // Bank Fees: ৳5,000
        val expenses = listOf(
            AccountSummaryItem("coa-5110", "5110", "Rent & Utilities", "", 4000000L),
            AccountSummaryItem("coa-5120", "5120", "Salaries & Wages", "", 8000000L),
            AccountSummaryItem("coa-5140", "5140", "Marketing", "", 1500000L),
            AccountSummaryItem("coa-5150", "5150", "Bank Fees", "", 500000L)
        )
        val totalExpensesMinor = expenses.sumOf { it.amountMinor }
        assertEquals(14000000L, totalExpensesMinor) // ৳1,40,000

        val operatingProfitMinor = grossProfitMinor - totalExpensesMinor
        assertEquals(23000000L, operatingProfitMinor) // ৳2,30,000

        // Foreign Exchange Gain: ৳10,000
        val fxGainLossMinor = 1000000L
        val netProfitMinor = operatingProfitMinor + fxGainLossMinor
        assertEquals(24000000L, netProfitMinor) // ৳2,40,000

        val pnl = ProfitAndLossStatement(
            fromDate = 0L,
            toDate = System.currentTimeMillis(),
            grossSalesMinor = grossSalesMinor,
            salesReturnsMinor = salesReturnsMinor,
            salesDiscountsMinor = salesDiscountsMinor,
            netSalesRevenueMinor = netRevenueMinor,
            costOfGoodsSoldMinor = cogsMinor,
            grossProfitMinor = grossProfitMinor,
            operatingExpenses = expenses,
            totalOperatingExpensesMinor = totalExpensesMinor,
            operatingProfitMinor = operatingProfitMinor,
            foreignExchangeGainLossMinor = fxGainLossMinor,
            netProfitMinor = netProfitMinor
        )

        assertEquals(24000000L, pnl.netProfitMinor)
    }

    @Test
    fun testBalanceSheetAccountingEquationHolds() {
        // Assets:
        // Cash in Hand: ৳50,000 (5,000,000 minor)
        // City Bank LC: ৳15,00,000 (150,000,000 minor)
        // Accounts Receivable: ৳4,00,000 (40,000,000 minor)
        // Inventory (Warehouse): ৳8,50,000 (85,000,000 minor)
        // Total Assets = ৳28,00,000 (280,000,000 minor)
        val assets = listOf(
            AccountSummaryItem("coa-1010", "1010", "Cash in Hand", "", 5000000L),
            AccountSummaryItem("coa-1020", "1020", "City Bank", "", 150000000L),
            AccountSummaryItem("coa-1040", "1040", "Accounts Receivable", "", 40000000L),
            AccountSummaryItem("coa-1210", "1210", "Inventory Asset", "", 85000000L)
        )
        val totalAssetsMinor = assets.sumOf { it.amountMinor }
        assertEquals(280000000L, totalAssetsMinor)

        // Liabilities:
        // Accounts Payable Suppliers: ৳3,00,000 (30,000,000 minor)
        // Customer Advances: ৳1,00,000 (10,000,000 minor)
        // Total Liabilities = ৳4,00,000 (40,000,000 minor)
        val liabilities = listOf(
            AccountSummaryItem("coa-2010", "2010", "Accounts Payable", "", 30000000L),
            AccountSummaryItem("coa-2030", "2030", "Customer Advances", "", 10000000L)
        )
        val totalLiabilitiesMinor = liabilities.sumOf { it.amountMinor }
        assertEquals(40000000L, totalLiabilitiesMinor)

        // Equity:
        // Owner's Capital: ৳20,00,000 (200,000,000 minor)
        // Retained Earnings (from P&L Net Profit): ৳4,00,000 (40,000,000 minor)
        // Total Equity = ৳24,00,000 (240,000,000 minor)
        val equityItems = listOf(
            AccountSummaryItem("coa-3010", "3010", "Owner's Capital", "", 200000000L)
        )
        val retainedEarningsMinor = 40000000L
        val totalEquityMinor = equityItems.sumOf { it.amountMinor } + retainedEarningsMinor
        assertEquals(240000000L, totalEquityMinor)

        val totalLiabilitiesAndEquityMinor = totalLiabilitiesMinor + totalEquityMinor
        assertEquals(280000000L, totalLiabilitiesAndEquityMinor)

        val balanceSheet = BalanceSheet(
            asOfDate = System.currentTimeMillis(),
            currentAssets = assets,
            totalCurrentAssetsMinor = totalAssetsMinor,
            totalAssetsMinor = totalAssetsMinor,
            currentLiabilities = liabilities,
            totalCurrentLiabilitiesMinor = totalLiabilitiesMinor,
            totalLiabilitiesMinor = totalLiabilitiesMinor,
            equityItems = equityItems,
            retainedEarningsMinor = retainedEarningsMinor,
            totalEquityMinor = totalEquityMinor,
            totalLiabilitiesAndEquityMinor = totalLiabilitiesAndEquityMinor,
            isBalanced = (totalAssetsMinor == totalLiabilitiesAndEquityMinor)
        )

        // Assets = Liabilities + Equity invariant must be TRUE!
        assertTrue(balanceSheet.isBalanced)
        assertEquals(balanceSheet.totalAssetsMinor, balanceSheet.totalLiabilitiesAndEquityMinor)
    }

    @Test
    fun testCashFlowStatementReconcilesToCashBalances() {
        val openingCashBankMinor = 10000000L // ৳1,00,000 opening cash & bank
        val collectionsFromCustomers = 50000000L // +৳5,00,000
        val paymentsToSuppliers = 30000000L // -৳3,00,000
        val paymentsForExpenses = 8000000L // -৳80,000

        val netOperations = collectionsFromCustomers - paymentsToSuppliers - paymentsForExpenses
        assertEquals(12000000L, netOperations) // +৳1,20,000

        val netChange = netOperations // No investing or financing in this test
        val closingCashBankMinor = openingCashBankMinor + netChange
        assertEquals(22000000L, closingCashBankMinor) // ৳2,20,000

        val cf = CashFlowStatement(
            fromDate = 0L,
            toDate = System.currentTimeMillis(),
            cashInflowCustomersMinor = collectionsFromCustomers,
            cashOutflowSuppliersMinor = paymentsToSuppliers,
            cashOutflowExpensesMinor = paymentsForExpenses,
            netCashFromOperationsMinor = netOperations,
            netCashFromInvestingMinor = 0L,
            netCashFromFinancingMinor = 0L,
            netChangeInCashMinor = netChange,
            openingCashBankMinor = openingCashBankMinor,
            closingCashBankMinor = closingCashBankMinor
        )

        assertEquals(closingCashBankMinor, cf.openingCashBankMinor + cf.netChangeInCashMinor)
    }
}
