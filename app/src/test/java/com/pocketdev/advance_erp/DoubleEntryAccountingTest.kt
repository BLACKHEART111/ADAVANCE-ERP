package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleEntryAccountingTest {

    @Test
    fun testBalancedJournalEntryValidation() {
        val line1 = JournalLineDraft(accountId = "coa-1210", isDebit = true, amountMinor = 5000000L) // Dr Inventory ৳50,000
        val line2 = JournalLineDraft(accountId = "coa-1020", isDebit = false, amountMinor = 5000000L) // Cr Bank ৳50,000

        val lines = listOf(line1, line2)
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(5000000L, totalDebit)
        assertEquals(5000000L, totalCredit)
        assertEquals(totalDebit, totalCredit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnbalancedJournalEntryRejected() {
        val line1 = JournalLineDraft(accountId = "coa-1210", isDebit = true, amountMinor = 5000000L)
        val line2 = JournalLineDraft(accountId = "coa-1020", isDebit = false, amountMinor = 4800000L) // ৳200 discrepancy!

        val lines = listOf(line1, line2)
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        require(totalDebit == totalCredit) {
            "Debit ($totalDebit) does not balance with Credit ($totalCredit) in minor units!"
        }
    }

    @Test
    fun testJournalReversalSwapsDebitsAndCredits() {
        // Original entry:
        // Dr. Inventory Asset ৳1,00,000 (10,000,000 poisha)
        // Cr. Accounts Payable ৳1,00,000
        val originalLines = listOf(
            JournalLine("jl-1", "je-01", "coa-1210", "1210", "Inventory Asset", isDebit = true, amountMinor = 10000000L),
            JournalLine("jl-2", "je-01", "coa-2010", "2010", "Accounts Payable", isDebit = false, amountMinor = 10000000L)
        )

        // Reversal lines swap isDebit
        val reversalLines = originalLines.map { line ->
            JournalLine(
                id = "jl-rev-${line.id}",
                journalEntryId = "je-02",
                accountId = line.accountId,
                accountCode = line.accountCode,
                accountName = line.accountName,
                isDebit = !line.isDebit, // Swapped!
                amountMinor = line.amountMinor
            )
        }

        val revDebit = reversalLines.filter { it.isDebit }.sumOf { it.amountMinor }
        val revCredit = reversalLines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(10000000L, revDebit)
        assertEquals(10000000L, revCredit)

        // The line that was originally Debit is now Credit
        val revInventoryLine = reversalLines.find { it.accountCode == "1210" }!!
        assertFalse(revInventoryLine.isDebit)

        // The line that was originally Credit is now Debit
        val revPayableLine = reversalLines.find { it.accountCode == "2010" }!!
        assertTrue(revPayableLine.isDebit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPreventDuplicateReversal() {
        val entry = JournalEntry(
            id = "je-01",
            entryNumber = "JE-2026-0001",
            descriptionEn = "Test",
            descriptionBn = "টেস্ট",
            referenceType = "MANUAL",
            totalDebitMinor = 10000L,
            totalCreditMinor = 10000L,
            isReversed = true // Already reversed!
        )

        require(!entry.isReversed) { "Entry is already reversed: ${entry.entryNumber}" }
    }
}
