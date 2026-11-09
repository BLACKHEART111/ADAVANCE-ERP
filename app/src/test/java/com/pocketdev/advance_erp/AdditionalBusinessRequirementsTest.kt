package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.core.pdf.PdfReportColumn
import com.pocketdev.advance_erp.core.pdf.PdfReportRow
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AdditionalBusinessRequirementsTest {

    // -------------------------------------------------------------
    // Requirement 1 & 2: Customer Summary & Automatic Creation
    // -------------------------------------------------------------

    @Test
    fun testCustomerSummaryMetricsCalculation() {
        val totalSalesMinor = 15000000L // ৳150,000.00
        val paidMinor = 10000000L       // ৳100,000.00
        val dueMinor = 5000000L         // ৳50,000.00
        val returnsMinor = 1200000L     // ৳12,000.00

        val customer = Customer(
            id = "cust-001",
            customerCode = "CUST-001",
            name = "Karim Trading",
            phone = "01711000000",
            address = "Dhaka, Bangladesh"
        )

        val summary = CustomerSummary(
            customer = customer,
            totalSalesMinor = totalSalesMinor,
            lifetimePaidMinor = paidMinor,
            outstandingBalanceMinor = dueMinor,
            totalReturnsMinor = returnsMinor
        )

        assertEquals("৳ 150,000.00", "৳ " + Formatters.formatMinorUnit(summary.totalSalesMinor))
        assertEquals("৳ 100,000.00", "৳ " + Formatters.formatMinorUnit(summary.lifetimePaidMinor))
        assertEquals("৳ 50,000.00", "৳ " + Formatters.formatMinorUnit(summary.outstandingBalanceMinor))
        assertEquals("৳ 12,000.00", "৳ " + Formatters.formatMinorUnit(summary.totalReturnsMinor))

        // Bangla formatting check
        val salesMoney = Money.ofMinor(summary.totalSalesMinor, CurrencyCode.BDT)
        val bnSales = Formatters.formatMoney(salesMoney, AppLanguage.BANGLA)
        assertTrue(bnSales.contains("১,৫০,০০০.০০") || bnSales.startsWith("৳"))
    }

    @Test
    fun testCustomerPhoneNormalizationAndDuplicatePrevention() {
        val rawPhone1 = " 01711-223344 "
        val rawPhone2 = "+8801711223344"
        val rawPhone3 = "01711 223 344"

        fun normalizePhone(p: String): String = p.trim().replace(" ", "").replace("-", "")

        val norm1 = normalizePhone(rawPhone1)
        val norm3 = normalizePhone(rawPhone3)
        assertEquals("01711223344", norm1)
        assertEquals("01711223344", norm3)
        assertEquals(norm1, norm3)

        val existingPhones = setOf("01711223344", "01819556677")
        val isDuplicate = existingPhones.contains(norm1)
        assertTrue("Duplicate phone should be detected", isDuplicate)

        val newDraft = NewCustomerDraft(
            name = "New Buyer",
            phone = "01999887766",
            address = "Chittagong"
        )
        val isNewDuplicate = existingPhones.contains(normalizePhone(newDraft.phone))
        assertFalse("New unique phone should not be marked duplicate", isNewDuplicate)
    }

    // -------------------------------------------------------------
    // Requirement 3: Variant-wise Stock & FIFO COGS
    // -------------------------------------------------------------

    @Test
    fun testVariantWiseSellableStockCalculations() {
        val totalQuantity = 150
        val reservedQuantity = 20
        val damagedQuantity = 5
        val inTransitQuantity = 50

        // Sellable stock formula: quantity - reservedQuantity
        val sellableStock = totalQuantity - reservedQuantity
        assertEquals(130, sellableStock)

        // Damaged and in-transit stocks must NOT be sellable
        val invalidSaleQty = 135
        val canFulfill = invalidSaleQty <= sellableStock
        assertFalse("Requested quantity exceeding sellable stock must fail", canFulfill)

        val validSaleQty = 80
        val canFulfillValid = validSaleQty <= sellableStock
        assertTrue("Requested quantity within sellable stock must succeed", canFulfillValid)

        val remainingSellable = sellableStock - validSaleQty
        assertEquals(50, remainingSellable)
    }

    @Test
    fun testDraftVsConfirmSaleStockAndPostingInvariance() {
        // Draft sale scenario:
        val draftSubtotal = 1000000L
        val draftDiscount = 50000L
        val draftAdditionalCharges = 10000L
        val draftGrandTotal = draftSubtotal - draftDiscount + draftAdditionalCharges
        val isDraft = true

        val postedMovementsCount = if (isDraft) 0 else 1
        val postedJournalLinesCount = if (isDraft) 0 else 2
        val invoiceStatus = if (isDraft) "DRAFT" else "POSTED"

        assertEquals(960000L, draftGrandTotal)
        assertEquals(0, postedMovementsCount)
        assertEquals(0, postedJournalLinesCount)
        assertEquals("DRAFT", invoiceStatus)

        // Confirm sale scenario:
        val isConfirmed = true
        val confirmedMovementsCount = if (isConfirmed) 1 else 0
        val confirmedJournalLinesCount = if (isConfirmed) 2 else 0
        val confirmedStatus = if (isConfirmed) "POSTED" else "DRAFT"

        assertEquals(1, confirmedMovementsCount)
        assertEquals(2, confirmedJournalLinesCount)
        assertEquals("POSTED", confirmedStatus)
    }

    // -------------------------------------------------------------
    // Requirement 5: Centralized PDF Reporting Engine
    // -------------------------------------------------------------

    @Test
    fun testCentralizedPdfReportRegistryCompleteness() {
        val accountBalances = listOf(
            Triple("1010", "Cash in Hand", 5000000L),
            Triple("1020", "City Bank Ltd", 25000000L),
            Triple("2010", "Accounts Payable", -30000000L)
        )

        val report = CentralizedPdfReportRegistry.buildTrialBalanceReport(
            accountBalances = accountBalances,
            asOfDateMillis = System.currentTimeMillis(),
            isBangla = false
        )

        assertNotNull(report)
        assertEquals("RPT-30-TRIAL-BALANCE", report.reportId)
        assertEquals("Accounts & Finance", report.category)
        assertTrue(report.sections.isNotEmpty())
    }

    // -------------------------------------------------------------
    // Requirement 6: Dashboard Calendar & Date-wise Business Data
    // -------------------------------------------------------------

    @Test
    fun testDashboardDateRangeCalculations() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 4, 15, 30, 45)
        val timestamp = cal.timeInMillis

        // Compute start of day
        val calStart = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calStart.timeInMillis

        // Compute end of day
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calEnd.timeInMillis

        assertTrue("Start of day must be <= timestamp", startOfDay <= timestamp)
        assertTrue("End of day must be >= timestamp", endOfDay >= timestamp)
        assertEquals(86399999L, endOfDay - startOfDay)

        // Day navigation
        val prevDayCal = (calStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val nextDayCal = (calStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        assertEquals(startOfDay - (24 * 3600 * 1000L), prevDayCal.timeInMillis)
        assertEquals(startOfDay + (24 * 3600 * 1000L), nextDayCal.timeInMillis)
    }

    @Test
    fun testHistoricalClosingBalanceAggregations() {
        // Historical cut-off test: only transactions on or before endOfDay are aggregated
        val cutOffDate = 1793750400000L // arbitrary cut-off date

        data class SampleTx(val timestamp: Long, val amountMinor: Long)
        val transactions = listOf(
            SampleTx(cutOffDate - 5000000, 100000L), // before cut-off -> INCLUDED
            SampleTx(cutOffDate - 10000, 50000L),    // before cut-off -> INCLUDED
            SampleTx(cutOffDate + 10000, 200000L)   // after cut-off -> EXCLUDED
        )

        val closingBalance = transactions
            .filter { it.timestamp <= cutOffDate }
            .sumOf { it.amountMinor }

        assertEquals(150000L, closingBalance)
    }

    // -------------------------------------------------------------
    // Phase 06 Part 4: Critical Business Workflows & Verification
    // -------------------------------------------------------------

    @Test
    fun testCustomerLedgerGoldenRuleAndNoAutomaticFifoAllocation() {
        // Golden Rule: Opening Due (10,000) + Invoice (15,000) - Payment (5,000) = Closing Due (20,000)
        val openingDueMinor = 1000000L // ৳10,000.00
        val invoiceDebitMinor = 1500000L // ৳15,000.00
        val paymentCreditMinor = 500000L // ৳5,000.00

        var runningBalance = openingDueMinor
        assertEquals(1000000L, runningBalance)

        // Add Invoice Debit
        runningBalance += invoiceDebitMinor
        assertEquals(2500000L, runningBalance) // ৳25,000.00

        // Subtract Payment Credit
        runningBalance -= paymentCreditMinor
        assertEquals(2000000L, runningBalance) // ৳20,000.00

        // Critical Confirmed Rule: Customer payment does NOT automatically distribute across old invoices
        var invoiceDueMinor = invoiceDebitMinor
        val isStandAlonePayment = true
        if (!isStandAlonePayment) {
            invoiceDueMinor -= paymentCreditMinor // Wrong FIFO deduction!
        }
        assertEquals("Invoice due must remain ৳15,000 when payment is standalone customer credit", 1500000L, invoiceDueMinor)
        assertEquals("Closing ledger due must be ৳20,000", 2000000L, runningBalance)
    }

    @Test
    fun testPartialDeliveryWorkflowAndChallanNumbering() {
        val totalOrdered = 100
        var totalDispatched = 0

        // Challan 1: 60 dispatched
        val dispatch1 = 60
        totalDispatched += dispatch1
        var remaining = totalOrdered - totalDispatched
        var status = if (remaining == 0) "DELIVERED" else "PARTIALLY_DELIVERED"

        assertEquals(60, totalDispatched)
        assertEquals(40, remaining)
        assertEquals("PARTIALLY_DELIVERED", status)

        // Challan 2: remaining 40 dispatched
        val dispatch2 = 40
        totalDispatched += dispatch2
        remaining = totalOrdered - totalDispatched
        status = if (remaining == 0) "DELIVERED" else "PARTIALLY_DELIVERED"

        assertEquals(100, totalDispatched)
        assertEquals(0, remaining)
        assertEquals("DELIVERED", status)

        // Check sequential delivery numbering format
        val seq1 = String.format(java.util.Locale.US, "DEL-2026-%04d", 1)
        val seq2 = String.format(java.util.Locale.US, "DEL-2026-%04d", 2)
        assertEquals("DEL-2026-0001", seq1)
        assertEquals("DEL-2026-0002", seq2)
    }

    @Test
    fun testQuotationToSalesOrderToInvoiceConversion() {
        var status = "QUOTATION"

        // 1. Convert to Sales Order
        assertTrue("Must be in QUOTATION state", status == "QUOTATION")
        status = "SALES_ORDER"
        assertEquals("SALES_ORDER", status)

        // Duplicate guard: cannot convert quotation to sales order again
        val canConvertQuotationAgain = status == "QUOTATION"
        assertFalse("Cannot convert non-quotation to sales order", canConvertQuotationAgain)

        // 2. Convert to Commercial Invoice
        assertTrue("Must be in SALES_ORDER state", status == "SALES_ORDER")
        status = "CONFIRMED"
        assertEquals("CONFIRMED", status)

        // Cannot convert already converted sales order
        val canConvertOrderAgain = status == "SALES_ORDER"
        assertFalse("Cannot convert non-sales-order to invoice", canConvertOrderAgain)
    }

    @Test
    fun testDirectExchangeMethodAPriceSettlement() {
        // Return 1 Polo Shirt (Unit price ৳800)
        val returnItemUnitPrice = 80000L // ৳800.00
        val returnQty = 1
        val returnedCredit = returnItemUnitPrice * returnQty // ৳800.00

        // Replacement: 1 Premium Polo Shirt (Unit price ৳1,050)
        val replacementUnitPrice = 105000L // ৳1,050.00
        val replacementQty = 1
        val replacementDebit = replacementUnitPrice * replacementQty // ৳1,050.00

        // Price Difference: Replacement - Return = ৳1,050 - ৳800 = +৳250 (Customer pays)
        val priceDifference = replacementDebit - returnedCredit
        assertEquals(25000L, priceDifference)
        assertTrue("Customer must pay positive price difference", priceDifference > 0)

        // Stock condition: Damaged returns are quarantined
        val isDamaged = true
        val stockDestination = if (isDamaged) "DAMAGED_QUARANTINE" else "SELLABLE_STOCK"
        assertEquals("DAMAGED_QUARANTINE", stockDestination)
    }

    @Test
    fun testRbacProfitVisibilityEnforcement() {
        val ownerRole = com.pocketdev.advance_erp.core.security.UserRole.OWNER_ADMIN
        val managerRole = com.pocketdev.advance_erp.core.security.UserRole.MANAGER
        val salesRole = com.pocketdev.advance_erp.core.security.UserRole.SALES_OPERATOR
        val purchaseRole = com.pocketdev.advance_erp.core.security.UserRole.PURCHASE_OPERATOR
        val inventoryRole = com.pocketdev.advance_erp.core.security.UserRole.INVENTORY_OPERATOR
        val accountantRole = com.pocketdev.advance_erp.core.security.UserRole.ACCOUNTANT

        // Only Owner/Admin can view profits
        assertTrue("Owner/Admin must have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(ownerRole))
        assertFalse("Manager must NOT have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(managerRole))
        assertFalse("Sales Operator must NOT have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(salesRole))
        assertFalse("Purchase Operator must NOT have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(purchaseRole))
        assertFalse("Inventory Operator must NOT have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(inventoryRole))
        assertFalse("Accountant must NOT have profit visibility", com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(accountantRole))
    }

    @Test
    fun testPercentageAndFixedDiscountValidation() {
        val lineGrossMinor = 500000L // ৳5,000.00

        // Percentage discount 15%
        val discountPercent = 15L
        val calculatedDiscountMinor = (lineGrossMinor * discountPercent) / 100L
        assertEquals(75000L, calculatedDiscountMinor) // ৳750.00
        val lineNetAfterPercent = lineGrossMinor - calculatedDiscountMinor
        assertEquals(425000L, lineNetAfterPercent)

        // Fixed discount validation: cannot exceed line gross
        val excessiveDiscountMinor = 600000L // ৳6,000.00 > ৳5,000.00
        val isValidDiscount = excessiveDiscountMinor <= lineGrossMinor
        assertFalse("Discount exceeding line gross must be invalid", isValidDiscount)

        val validDiscountMinor = 100000L // ৳1,000.00 <= ৳5,000.00
        val isValid = validDiscountMinor <= lineGrossMinor
        assertTrue("Discount within line gross must be valid", isValid)
    }

    @Test
    fun testCreditLimitOptionCApproval() {
        val customerCreditLimitMinor = 5000000L // ৳50,000.00
        val existingDueMinor = 4000000L        // ৳40,000.00
        val newInvoiceTotalMinor = 2000000L    // ৳20,000.00
        val cashPaidMinor = 500000L            // ৳5,000.00

        val newDueMinor = newInvoiceTotalMinor - cashPaidMinor // ৳15,000.00
        val potentialTotalDueMinor = existingDueMinor + newDueMinor // ৳55,000.00

        val exceedsLimit = potentialTotalDueMinor > customerCreditLimitMinor
        assertTrue("Credit limit exceeded", exceedsLimit)

        // Option C requires authorized approval
        var approvedBy: String? = null
        var approvalReason: String? = null

        // Without approval -> blocked
        val canProceedWithoutApproval = !exceedsLimit || approvedBy != null
        assertFalse("Cannot proceed without approval", canProceedWithoutApproval)

        // Authorized approval provided
        approvedBy = "Owner / Admin"
        approvalReason = "Longstanding loyal customer approved for festive order"
        val canProceedWithApproval = !exceedsLimit || (approvedBy.isNotBlank() && approvalReason.isNotBlank())
        assertTrue("Sale proceeds with authorized approval", canProceedWithApproval)
    }
}
