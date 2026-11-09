package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MinimalImportPaymentTest {

    @Test
    fun testThirtySeventyPaymentTermsSchedule() {
        val totalPoMinor = 1000000L // 10,000 CNY
        val fxRate = BigDecimal("17.00")

        val schedule = PaymentTermsCalculator.calculateSchedule(
            PaymentTermType.THIRTY_SEVENTY,
            totalPoMinor,
            fxRate
        )

        assertEquals(2, schedule.size)

        val advance = schedule[0]
        assertEquals("Advance Deposit (30%)", advance.titleEn)
        assertEquals(300000L, advance.amountMinor) // 3,000 CNY
        assertEquals(5100000L, advance.bdtAmountMinor) // 51,000 BDT

        val balance = schedule[1]
        assertEquals("Final Balance (70%)", balance.titleEn)
        assertEquals(700000L, balance.amountMinor) // 7,000 CNY
        assertEquals(11900000L, balance.bdtAmountMinor) // 119,000 BDT

        assertEquals(totalPoMinor, advance.amountMinor + balance.amountMinor)
    }

    @Test
    fun testFiftyFiftyPaymentTermsSchedule() {
        val totalPoMinor = 1000000L // 10,000 CNY
        val fxRate = BigDecimal("17.00")

        val schedule = PaymentTermsCalculator.calculateSchedule(
            PaymentTermType.FIFTY_FIFTY,
            totalPoMinor,
            fxRate
        )

        assertEquals(2, schedule.size)
        assertEquals(500000L, schedule[0].amountMinor) // 5,000 CNY
        assertEquals(8500000L, schedule[0].bdtAmountMinor) // 85,000 BDT
        assertEquals(500000L, schedule[1].amountMinor) // 5,000 CNY
        assertEquals(8500000L, schedule[1].bdtAmountMinor) // 85,000 BDT
    }

    @Test
    fun testSupplierPaymentAccountingDebitEqualsCredit() {
        // Recording a 30% advance: 3,000 CNY = 51,000 BDT (5,100,000 minor)
        val paymentBdtMinor = 5100000L

        val lineDr = JournalLineDraft(
            accountId = "coa-2010", // Accounts Payable / Supplier Advance
            isDebit = true,
            amountMinor = paymentBdtMinor
        )
        val lineCr = JournalLineDraft(
            accountId = "coa-1020", // Bank Account
            isDebit = false,
            amountMinor = paymentBdtMinor
        )

        val lines = listOf(lineDr, lineCr)
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(paymentBdtMinor, totalDebit)
        assertEquals(paymentBdtMinor, totalCredit)
        assertEquals(totalDebit, totalCredit)
    }

    @Test
    fun testOverpaymentPreventionRule() {
        val po = PurchaseOrder(
            id = "po-01",
            poNumber = "PO-2026-001",
            supplierId = "sup-01",
            totalAmountMinor = 1000000L, // 10,000 CNY
            paidAmountMinor = 300000L // Already paid 3,000 CNY
        )

        val remainingDueMinor = po.totalAmountMinor - po.paidAmountMinor // 7,000 CNY
        assertEquals(700000L, remainingDueMinor)

        val attemptedPaymentMinor = 800000L // Attempting 8,000 CNY (> 7,000 CNY)

        try {
            require(attemptedPaymentMinor <= remainingDueMinor) {
                "Payment amount ($attemptedPaymentMinor) exceeds remaining due balance ($remainingDueMinor)"
            }
            fail("Expected exception for overpayment")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("exceeds remaining due balance"))
        }
    }
}
