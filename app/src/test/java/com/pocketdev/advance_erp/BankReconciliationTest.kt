package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.ReconciliationStatus
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankReconciliationTest {

    @Test
    fun testReconciliationMatchingAndDiscrepancyResolution() {
        val statementOpeningMinor = 10000000L // ৳1,00,000 opening on bank statement
        val statementClosingMinor = 14500000L // ৳1,45,000 closing on bank statement

        val item1 = BankReconciliationItem(
            id = "bri-1",
            reconciliationId = "rec-1",
            transactionDate = System.currentTimeMillis() - 86400000L,
            description = "Customer payment Cheque #102",
            amountMinor = 5000000L, // +৳50,000 (Debit in ERP = Credit on bank statement)
            isDebit = true,
            isMatched = true // Matched with bank statement!
        )

        val item2 = BankReconciliationItem(
            id = "bri-2",
            reconciliationId = "rec-1",
            transactionDate = System.currentTimeMillis() - 40000000L,
            description = "Supplier payment TT #554",
            amountMinor = 2000000L, // -৳20,000 (Credit in ERP = Debit on bank statement)
            isDebit = false,
            isMatched = true // Matched!
        )

        val item3 = BankReconciliationItem(
            id = "bri-3",
            reconciliationId = "rec-1",
            transactionDate = System.currentTimeMillis() - 10000000L,
            description = "Customer deposit pending clearance",
            amountMinor = 1500000L, // +৳15,000 (In-transit cheque not yet cleared at bank)
            isDebit = true,
            isMatched = false // In-transit / unmatched!
        )

        val matchedItems = listOf(item1, item2, item3).filter { it.isMatched }

        // Matched net change = +50,000 - 20,000 = +30,000
        val matchedNet = matchedItems.sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        assertEquals(3000000L, matchedNet)

        // Reconciled balance from matched items
        val reconciledBalanceMinor = statementOpeningMinor + matchedNet
        // 100,000 + 30,000 = 130,000
        assertEquals(13000000L, reconciledBalanceMinor)

        // Bank statement closing is 145,000. So discrepancy is 145,000 - 130,000 = 15,000 (1,500,000 minor)
        val discrepancyMinor = statementClosingMinor - reconciledBalanceMinor
        assertEquals(1500000L, discrepancyMinor)

        // If the bank credited ৳15,000 (the uncleared deposit cleared on the closing date):
        val item3Matched = item3.copy(isMatched = true)
        val allMatched = listOf(item1, item2, item3Matched)
        val finalMatchedNet = allMatched.sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        val finalReconciledBalance = statementOpeningMinor + finalMatchedNet

        // 100,000 + 45,000 = 145,000
        assertEquals(14500000L, finalReconciledBalance)
        val finalDiscrepancy = statementClosingMinor - finalReconciledBalance
        assertEquals(0L, finalDiscrepancy)

        val finalStatus = if (finalDiscrepancy == 0L) ReconciliationStatus.RECONCILED.name else ReconciliationStatus.DISCREPANCY.name
        assertEquals(ReconciliationStatus.RECONCILED.name, finalStatus)
    }

    @Test
    fun testBankChargesAdjustmentBalancedJournal() {
        // Missing bank charge discovered during statement reconciliation: ৳575.00 (57,500 minor)
        val feeAmountMinor = 57500L
        val bankAccountId = "coa-1020"
        val bankChargesAccountId = "coa-5150"

        val lines = listOf(
            JournalLineDraft(bankChargesAccountId, isDebit = true, amountMinor = feeAmountMinor, description = "Bank monthly service ledger fee"),
            JournalLineDraft(bankAccountId, isDebit = false, amountMinor = feeAmountMinor, description = "Deducted by bank")
        )

        assertEquals(lines.filter { it.isDebit }.sumOf { it.amountMinor }, lines.filter { !it.isDebit }.sumOf { it.amountMinor })
    }
}
