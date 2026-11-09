package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAllocationAndLedgerTest {

    data class MockInvoice(
        val id: String,
        val totalMinor: Long,
        var paidMinor: Long
    ) {
        val dueMinor: Long get() = maxOf(0L, totalMinor - paidMinor)
    }

    @Test
    fun testMultiInvoicePaymentAllocation() {
        // Customer has 3 unpaid invoices:
        // Invoice 1: ৳10,000 (due ৳10,000)
        // Invoice 2: ৳15,000 (due ৳15,000)
        // Invoice 3: ৳20,000 (due ৳20,000)
        // Total due = ৳45,000 (4,500,000 poisa)
        val invoices = listOf(
            MockInvoice("inv-1", 1000000L, 0L),
            MockInvoice("inv-2", 1500000L, 0L),
            MockInvoice("inv-3", 2000000L, 0L)
        )

        // Customer pays ৳30,000 (3,000,000 poisa)
        var paymentRemaining = 3000000L
        val allocations = mutableMapOf<String, Long>()

        for (inv in invoices) {
            if (paymentRemaining <= 0) break
            val alloc = minOf(paymentRemaining, inv.dueMinor)
            allocations[inv.id] = alloc
            inv.paidMinor += alloc
            paymentRemaining -= alloc
        }

        // Invoice 1 should be fully paid (৳10,000)
        assertEquals(1000000L, allocations["inv-1"])
        assertEquals(0L, invoices[0].dueMinor)

        // Invoice 2 should be fully paid (৳15,000)
        assertEquals(1500000L, allocations["inv-2"])
        assertEquals(0L, invoices[1].dueMinor)

        // Invoice 3 should be partially paid with remaining ৳5,000
        assertEquals(500000L, allocations["inv-3"])
        assertEquals(1500000L, invoices[2].dueMinor)

        // Zero unallocated payment left
        assertEquals(0L, paymentRemaining)

        // Remaining customer receivable is ৳15,000
        val remainingReceivable = invoices.sumOf { it.dueMinor }
        assertEquals(1500000L, remainingReceivable)
    }

    @Test
    fun testCustomerAdvanceHandling() {
        // Customer has 1 invoice for ৳10,000 (1,000,000 poisa)
        val invoices = listOf(MockInvoice("inv-1", 1000000L, 0L))

        // Customer pays ৳15,000 (1,500,000 poisa)
        val totalPayment = 1500000L
        var remaining = totalPayment
        var allocated = 0L

        for (inv in invoices) {
            val alloc = minOf(remaining, inv.dueMinor)
            allocated += alloc
            inv.paidMinor += alloc
            remaining -= alloc
        }

        val unallocatedAdvanceCredit = remaining

        assertEquals(1000000L, allocated)
        assertEquals(500000L, unallocatedAdvanceCredit) // ৳5,000 Advance Credit
        assertEquals(0L, invoices[0].dueMinor)
    }

    @Test
    fun testCustomerLedgerRunningBalanceCalculation() {
        data class LedgerItem(
            val type: String,
            val debitMinor: Long,
            val creditMinor: Long
        )

        val txns = listOf(
            LedgerItem("INVOICE", 1000000L, 0L),      // +10,000 (Bal: 10,000)
            LedgerItem("PAYMENT", 0L, 600000L),       // -6,000  (Bal: 4,000)
            LedgerItem("INVOICE", 500000L, 0L),       // +5,000  (Bal: 9,000)
            LedgerItem("RETURN", 0L, 200000L),        // -2,000  (Bal: 7,000)
            LedgerItem("PAYMENT", 0L, 700000L)        // -7,000  (Bal: 0)
        )

        var runningBal = 0L
        val balances = mutableListOf<Long>()

        for (t in txns) {
            runningBal += (t.debitMinor - t.creditMinor)
            balances.add(runningBal)
        }

        assertEquals(1000000L, balances[0])
        assertEquals(400000L, balances[1])
        assertEquals(900000L, balances[2])
        assertEquals(700000L, balances[3])
        assertEquals(0L, balances[4])
    }
}
