package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CashBankTransferAndBookTest {

    @Test
    fun testInterAccountTransferPreservesTotalAssets() {
        // Initial state:
        // Cash: ৳1,00,000 (10,000,000 minor)
        // City Bank: ৳5,00,000 (50,000,000 minor)
        // Total Liquid Assets = ৳6,00,000 (60,000,000 minor)
        var cashBalance = 10000000L
        var cityBankBalance = 50000000L
        val initialTotalAssets = cashBalance + cityBankBalance

        // Transfer ৳40,000 (4,000,000 minor) from Cash into City Bank
        val transferAmount = 4000000L
        val lines = listOf(
            JournalLineDraft(accountId = "coa-1020", isDebit = true, amountMinor = transferAmount), // Debit Destination
            JournalLineDraft(accountId = "coa-1010", isDebit = false, amountMinor = transferAmount) // Credit Source
        )

        // Apply transfer
        for (line in lines) {
            if (line.accountId == "coa-1020") {
                cityBankBalance += if (line.isDebit) line.amountMinor else -line.amountMinor
            } else if (line.accountId == "coa-1010") {
                cashBalance += if (line.isDebit) line.amountMinor else -line.amountMinor
            }
        }

        assertEquals(6000000L, cashBalance) // ৳60,000 left in cash
        assertEquals(54000000L, cityBankBalance) // ৳5,40,000 in bank
        val finalTotalAssets = cashBalance + cityBankBalance

        // Fundamental rule: Inter-account transfer does not change total liquid assets!
        assertEquals(initialTotalAssets, finalTotalAssets)
    }

    @Test
    fun testCashBookRunningBalanceCalculation() {
        val openingBalance = 5000000L // ৳50,000 opening
        var running = openingBalance

        val transactions = listOf(
            // 1. Customer Payment (Cash Receipt) +৳25,000
            Pair(2500000L, 0L),
            // 2. Petty cash expense (Office tea/supplies) -৳1,500
            Pair(0L, 150000L),
            // 3. Deposit to bank -৳30,000
            Pair(0L, 3000000L),
            // 4. Another retail cash sale receipt +৳10,000
            Pair(1000000L, 0L)
        )

        val runningBalances = mutableListOf<Long>()
        for ((receipt, payment) in transactions) {
            running += (receipt - payment)
            runningBalances.add(running)
        }

        // Check running balances:
        // 50,000 + 25,000 = 75,000 (7,500,000)
        assertEquals(7500000L, runningBalances[0])
        // 75,000 - 1,500 = 73,500 (7,350,000)
        assertEquals(7350000L, runningBalances[1])
        // 73,500 - 30,000 = 43,500 (4,350,000)
        assertEquals(4350000L, runningBalances[2])
        // 43,500 + 10,000 = 53,500 (5,350,000)
        assertEquals(5350000L, runningBalances[3])
    }
}
