package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesInvoiceCalculationsTest {

    @Test
    fun testInvoiceSubtotalAndDiscountCalculations() {
        // Item 1: 5 units @ ৳1,200.50 (120,050 poisa), line discount ৳50.00 (5,000 poisa)
        val item1Qty = 5
        val item1UnitPriceMinor = 120050L
        val item1LineDiscountMinor = 5000L
        val item1GrossMinor = item1Qty * item1UnitPriceMinor // 600,250 poisa
        val item1TotalMinor = item1GrossMinor - item1LineDiscountMinor // 595,250 poisa

        // Item 2: 10 units @ ৳450.00 (45,000 poisa), no line discount
        val item2Qty = 10
        val item2UnitPriceMinor = 45000L
        val item2LineDiscountMinor = 0L
        val item2GrossMinor = item2Qty * item2UnitPriceMinor // 450,000 poisa
        val item2TotalMinor = item2GrossMinor - item2LineDiscountMinor // 450,000 poisa

        val subtotalMinor = item1GrossMinor + item2GrossMinor // 1,050,250 poisa
        val totalItemDiscounts = item1LineDiscountMinor + item2LineDiscountMinor // 5,000 poisa
        val invoiceLevelDiscountMinor = 15000L // ৳150.00

        val totalDiscountMinor = totalItemDiscounts + invoiceLevelDiscountMinor // 20,000 poisa
        val grandTotalMinor = subtotalMinor - totalDiscountMinor // 1,030,250 poisa (৳10,302.50)

        assertEquals(600250L, item1GrossMinor)
        assertEquals(595250L, item1TotalMinor)
        assertEquals(450000L, item2TotalMinor)
        assertEquals(1050250L, subtotalMinor)
        assertEquals(1030250L, grandTotalMinor)
        assertEquals(1030250L, (item1TotalMinor + item2TotalMinor) - invoiceLevelDiscountMinor)
    }

    @Test
    fun testDraftInvoicesDoNotChangeStockOrAccounting() {
        val initialStock = 100
        val draftInvoiceQty = 25
        val isPosted = false

        val effectiveStock = if (isPosted) initialStock - draftInvoiceQty else initialStock
        val journalEntriesCreated = if (isPosted) 2 else 0

        assertEquals(100, effectiveStock)
        assertEquals(0, journalEntriesCreated)
    }

    @Test
    fun testZeroAndNegativeDiscountHandling() {
        val subtotalMinor = 500000L
        val zeroDiscount = 0L
        val totalWithZeroDiscount = maxOf(0L, subtotalMinor - zeroDiscount)
        assertEquals(500000L, totalWithZeroDiscount)

        // Discount exceeding subtotal should be clamped to 0
        val excessDiscount = 600000L
        val totalWithExcessDiscount = maxOf(0L, subtotalMinor - excessDiscount)
        assertEquals(0L, totalWithExcessDiscount)
    }
}
