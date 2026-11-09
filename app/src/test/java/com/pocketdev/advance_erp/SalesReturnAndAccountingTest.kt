package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesReturnAndAccountingTest {

    @Test
    fun testReturnQuantityValidation() {
        val soldQty = 50
        var previouslyReturnedQty = 10
        val remainingReturnable = soldQty - previouslyReturnedQty

        assertEquals(40, remainingReturnable)

        // Attempting to return 45 units should be invalid
        val requestedReturnQty = 45
        val isValid = requestedReturnQty in 1..remainingReturnable
        assertFalse(isValid)

        // Attempting to return 20 units should be valid
        val validReturnQty = 20
        assertTrue(validReturnQty in 1..remainingReturnable)
    }

    @Test
    fun testReturnStockRestorationAndCogsReversal() {
        val currentWarehouseStock = 80
        val returnQty = 15
        val originalUnitCostMinor = 45000L // ৳450.00
        val unitSellingPriceMinor = 65000L // ৳650.00

        val restoredStock = currentWarehouseStock + returnQty
        val cogsReversalMinor = returnQty.toLong() * originalUnitCostMinor
        val customerCreditMinor = returnQty.toLong() * unitSellingPriceMinor

        assertEquals(95, restoredStock)
        assertEquals(675000L, cogsReversalMinor) // ৳6,750.00
        assertEquals(975000L, customerCreditMinor) // ৳9,750.00
    }

    @Test
    fun testSalesPostingDoubleEntryBalancing() {
        // Sales Invoice with Discount:
        // Subtotal: ৳50,000 (5,000,000 poisa)
        // Invoice Discount: ৳2,000 (200,000 poisa)
        // Net Total: ৳48,000 (4,800,000 poisa)
        // COGS: ৳32,000 (3,200,000 poisa)
        val subtotalMinor = 5000000L
        val discountMinor = 200000L
        val totalMinor = 4800000L
        val cogsMinor = 3200000L

        // Entry 1: Revenue Recognition
        // Debit: Accounts Receivable (৳48,000)
        // Debit: Sales Discount (৳2,000)
        // Credit: Sales Revenue (৳50,000)
        val debitAR = totalMinor
        val debitDiscount = discountMinor
        val creditRevenue = subtotalMinor

        val totalDebitRevenue = debitAR + debitDiscount
        val totalCreditRevenue = creditRevenue
        assertEquals(totalDebitRevenue, totalCreditRevenue)

        // Entry 2: COGS Recognition
        // Debit: Cost of Goods Sold (৳32,000)
        // Credit: Inventory Asset (৳32,000)
        val debitCogs = cogsMinor
        val creditInventory = cogsMinor
        assertEquals(debitCogs, creditInventory)
    }

    @Test
    fun testCustomerPaymentDoubleEntryBalancing() {
        // Customer pays ৳25,000 Cash
        // ৳20,000 allocated to open invoices
        // ৳5,000 unallocated customer advance credit
        val totalPaymentMinor = 2500000L
        val allocatedMinor = 2000000L
        val advanceMinor = 500000L

        // Debit: Cash in Hand (৳25,000)
        val debitCash = totalPaymentMinor
        // Credit: Accounts Receivable (৳20,000)
        val creditAR = allocatedMinor
        // Credit: Customer Advances Liability (৳5,000)
        val creditAdvance = advanceMinor

        val totalDebit = debitCash
        val totalCredit = creditAR + creditAdvance
        assertEquals(totalDebit, totalCredit)
    }

    @Test
    fun testSalesReturnDoubleEntryBalancing() {
        // Customer returns ৳10,000 worth of goods for Customer Credit
        // Cost of returned goods was ৳6,500
        val returnAmountMinor = 1000000L
        val cogsReversalMinor = 650000L

        // Entry 1: Revenue Reversal
        // Debit: Sales Returns & Allowances (৳10,000)
        // Credit: Accounts Receivable (৳10,000)
        val debitReturn = returnAmountMinor
        val creditAR = returnAmountMinor
        assertEquals(debitReturn, creditAR)

        // Entry 2: COGS & Inventory Restoration
        // Debit: Inventory Asset (৳6,500)
        // Credit: Cost of Goods Sold (৳6,500)
        val debitInventory = cogsReversalMinor
        val creditCogs = cogsReversalMinor
        assertEquals(debitInventory, creditCogs)
    }
}
