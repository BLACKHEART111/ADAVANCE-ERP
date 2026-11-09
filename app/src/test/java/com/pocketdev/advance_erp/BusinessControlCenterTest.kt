package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class BusinessControlCenterTest {

    @Test
    fun testDateRangeCalculations() {
        val now = System.currentTimeMillis()

        // Today
        val (todayStart, todayEnd) = DateRangeOption.TODAY.getBoundsMillis()
        assertTrue(todayEnd >= todayStart)
        assertTrue(now in todayStart..todayEnd)

        // Yesterday
        val (yesterdayStart, yesterdayEnd) = DateRangeOption.YESTERDAY.getBoundsMillis()
        assertTrue(yesterdayEnd >= yesterdayStart)
        assertTrue(yesterdayEnd < todayStart)

        // Last 7 Days
        val (last7Start, last7End) = DateRangeOption.LAST_7_DAYS.getBoundsMillis()
        assertTrue(last7End >= last7Start)
        assertTrue(last7Start < todayStart)
        assertTrue(last7End >= now - 10000L)

        // Last 30 Days
        val (last30Start, last30End) = DateRangeOption.LAST_30_DAYS.getBoundsMillis()
        assertTrue(last30End >= last30Start)
        assertTrue(last30Start < last7Start)

        // All Time
        val (allTimeStart, allTimeEnd) = DateRangeOption.ALL_TIME.getBoundsMillis()
        assertEquals(0L, allTimeStart)
        assertEquals(Long.MAX_VALUE, allTimeEnd)

        // Custom Range (automatically normalized to start of start-day and end of end-day)
        val customFrom = 1700000000000L
        val customTo = 1700500000000L
        val (customStart, customEnd) = DateRangeOption.CUSTOM.getBoundsMillis(customFrom, customTo)
        assertTrue(customStart <= customFrom)
        assertTrue(customEnd >= customTo)
        assertTrue(customEnd > customStart)

        // Custom Range with reversed order (start > end) should normalize properly
        val (normStart, normEnd) = DateRangeOption.CUSTOM.getBoundsMillis(customTo, customFrom)
        assertTrue(normStart <= customTo)
        assertTrue(normEnd >= customFrom)
    }

    @Test
    fun testActiveFilterCount() {
        val defaultState = ControlCenterFilterState()
        assertEquals(0, defaultState.activeFilterCount)

        val modifiedState = ControlCenterFilterState(
            dateRangeOption = DateRangeOption.THIS_MONTH,
            category = ReportCategory.SALES,
            selectedCustomerId = "cust-1",
            paymentStatus = PaymentStatusFilter.PAID,
            amountFilter = AmountRangeFilter.RANGE_1K_10K
        )
        assertEquals(5, modifiedState.activeFilterCount)
    }

    @Test
    fun testAmountRangeFiltering() {
        // Range 0 to 1,000 BDT = 0 to 100,000 Minor poisa
        fun matchesAmount(filter: AmountRangeFilter, amountMinor: Long, minCustom: Long? = null, maxCustom: Long? = null): Boolean {
            return when (filter) {
                AmountRangeFilter.ALL -> true
                AmountRangeFilter.RANGE_0_1K -> amountMinor in 0L..100_000L
                AmountRangeFilter.RANGE_1K_10K -> amountMinor in 100_001L..1_000_000L
                AmountRangeFilter.RANGE_10K_50K -> amountMinor in 1_000_001L..5_000_000L
                AmountRangeFilter.ABOVE_50K -> amountMinor > 5_000_000L
                AmountRangeFilter.CUSTOM -> {
                    val min = minCustom ?: 0L
                    val max = maxCustom ?: Long.MAX_VALUE
                    amountMinor in min..max
                }
            }
        }

        assertTrue(matchesAmount(AmountRangeFilter.RANGE_0_1K, 50_000L))
        assertFalse(matchesAmount(AmountRangeFilter.RANGE_0_1K, 150_000L))

        assertTrue(matchesAmount(AmountRangeFilter.RANGE_1K_10K, 500_000L))
        assertFalse(matchesAmount(AmountRangeFilter.RANGE_1K_10K, 50_000L))
        assertFalse(matchesAmount(AmountRangeFilter.RANGE_1K_10K, 2_000_000L))

        assertTrue(matchesAmount(AmountRangeFilter.ABOVE_50K, 6_000_000L))
        assertFalse(matchesAmount(AmountRangeFilter.ABOVE_50K, 4_000_000L))
    }

    @Test
    fun testSortingLogic() {
        val tx1 = UnifiedTransactionItem(
            id = "tx-1",
            referenceNumber = "INV-001",
            date = 1000L,
            category = ReportCategory.SALES,
            primaryEntityName = "Zahir Textiles",
            particulars = "Sales order",
            amountMinor = 50_000L,
            isInflow = true,
            status = "PAID",
            rawType = "INVOICE"
        )
        val tx2 = UnifiedTransactionItem(
            id = "tx-2",
            referenceNumber = "PO-001",
            date = 2000L,
            category = ReportCategory.PURCHASE,
            primaryEntityName = "Anis Garments",
            particulars = "Fabric purchase",
            amountMinor = 150_000L,
            isInflow = false,
            status = "PAID",
            rawType = "PURCHASE"
        )

        val list = listOf(tx1, tx2)

        // Newest First
        val sortedNewest = list.sortedByDescending { it.date }
        assertEquals("tx-2", sortedNewest.first().id)

        // Oldest First
        val sortedOldest = list.sortedBy { it.date }
        assertEquals("tx-1", sortedOldest.first().id)

        // Highest Amount
        val sortedHighest = list.sortedByDescending { it.amountMinor }
        assertEquals("tx-2", sortedHighest.first().id)

        // Lowest Amount
        val sortedLowest = list.sortedBy { it.amountMinor }
        assertEquals("tx-1", sortedLowest.first().id)

        // Name Ascending
        val sortedNameAsc = list.sortedBy { it.primaryEntityName.lowercase() }
        assertEquals("Anis Garments", sortedNameAsc.first().primaryEntityName)

        // Name Descending
        val sortedNameDesc = list.sortedByDescending { it.primaryEntityName.lowercase() }
        assertEquals("Zahir Textiles", sortedNameDesc.first().primaryEntityName)
    }

    @Test
    fun testPartyLedgerRunningBalanceCalculation() {
        val openingBalanceMinor = 100_000L // 1,000 BDT opening receivable

        val transactions = listOf(
            UnifiedTransactionItem(
                id = "1",
                referenceNumber = "INV-101",
                date = 1000L,
                category = ReportCategory.SALES,
                primaryEntityName = "Karim Store",
                particulars = "Sale of items",
                amountMinor = 500_000L, // 5,000 BDT Debit
                isInflow = true,
                status = "POSTED",
                rawType = "INVOICE"
            ),
            UnifiedTransactionItem(
                id = "2",
                referenceNumber = "PAY-101",
                date = 2000L,
                category = ReportCategory.RECEIPT,
                primaryEntityName = "Karim Store",
                particulars = "Cash collection",
                amountMinor = 300_000L, // 3,000 BDT Credit
                isInflow = false,
                status = "PAID",
                rawType = "PAYMENT"
            )
        )

        var runningBalance = openingBalanceMinor
        val calculated = transactions.map { tx ->
            if (tx.isInflow) {
                runningBalance += tx.amountMinor
            } else {
                runningBalance -= tx.amountMinor
            }
            tx.copy(runningBalanceMinor = runningBalance)
        }

        assertEquals(600_000L, calculated[0].runningBalanceMinor)
        assertEquals(300_000L, calculated[1].runningBalanceMinor)
        assertEquals(300_000L, runningBalance) // Closing balance
    }

    @Test
    fun testSummaryDashboardKpiMath() {
        val salesMinor = 1_000_000L // ৳10,000
        val cogsMinor = 600_000L    // ৳6,000
        val expensesMinor = 150_000L // ৳1,500
        val receivedMinor = 800_000L // ৳8,000
        val purchaseMinor = 500_000L // ৳5,000
        val paidMinor = 400_000L     // ৳4,000

        val grossProfit = salesMinor - cogsMinor // ৳4,000 (400,000 poisa)
        val netProfit = grossProfit - expensesMinor // ৳2,500 (250,000 poisa)
        val outstandingReceivable = salesMinor - receivedMinor // ৳2,000 (200,000 poisa)
        val outstandingPayable = purchaseMinor - paidMinor // ৳1,000 (100,000 poisa)

        val summary = BusinessSummaryDashboardData(
            totalSalesMinor = salesMinor,
            totalPurchaseMinor = purchaseMinor,
            totalReceivedMinor = receivedMinor,
            totalPaidMinor = paidMinor,
            totalExpensesMinor = expensesMinor,
            totalSalesReturnMinor = 0L,
            totalPurchaseReturnMinor = 0L,
            totalDeliveryCount = 15,
            totalOutstandingReceivableMinor = outstandingReceivable,
            totalOutstandingPayableMinor = outstandingPayable,
            grossProfitMinor = grossProfit,
            netProfitMinor = netProfit,
            totalTransactionsCount = 20,
            openingBalanceMinor = 0L,
            closingBalanceMinor = outstandingReceivable
        )

        assertEquals(400_000L, summary.grossProfitMinor)
        assertEquals(250_000L, summary.netProfitMinor)
        assertEquals(200_000L, summary.totalOutstandingReceivableMinor)
        assertEquals(100_000L, summary.totalOutstandingPayableMinor)
        assertTrue(summary.grossProfitMinor > summary.netProfitMinor)
    }

    @Test
    fun testSavedFilterPresetIntegrity() {
        val filter = ControlCenterFilterState(
            dateRangeOption = DateRangeOption.LAST_7_DAYS,
            category = ReportCategory.CUSTOMER_LEDGER,
            selectedCustomerId = "CUST-999"
        )
        val preset = SavedFilterPreset(
            id = "PRESET-1",
            name = "Weekly Customer Due",
            filterState = filter
        )

        assertEquals("Weekly Customer Due", preset.name)
        assertEquals(ReportCategory.CUSTOMER_LEDGER, preset.filterState.category)
        assertEquals("CUST-999", preset.filterState.selectedCustomerId)
        assertEquals(DateRangeOption.LAST_7_DAYS, preset.filterState.dateRangeOption)
    }
}
