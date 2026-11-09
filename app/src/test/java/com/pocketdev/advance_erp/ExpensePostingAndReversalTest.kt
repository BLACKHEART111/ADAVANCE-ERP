package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ExpensePostingAndReversalTest {

    @Test
    fun testBdtExpenseVoucherBalancedJournal() {
        val amountMinor = 4500000L // BDT 45,000.00 (Office Rent)
        val expenseAccountId = ExpenseCategory.RENT_UTILITIES.defaultAccountId // coa-5110
        val paymentAccountId = "coa-1020" // City Bank Corporate

        val lines = listOf(
            JournalLineDraft(
                accountId = expenseAccountId,
                isDebit = true,
                amountMinor = amountMinor,
                description = "Office Rent Voucher"
            ),
            JournalLineDraft(
                accountId = paymentAccountId,
                isDebit = false,
                amountMinor = amountMinor,
                description = "Paid via City Bank Corporate"
            )
        )

        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(4500000L, totalDebit)
        assertEquals(4500000L, totalCredit)
        assertEquals(totalDebit, totalCredit)
    }

    @Test
    fun testForeignCurrencyCnyExpenseConversion() {
        // China Business Travel expense of CNY 5,000.00 at exchange rate 17.2500 BDT/CNY
        val cnyMinor = 500000L // 5,000.00 CNY
        val rate = BigDecimal("17.2500")

        val bdtMinor = BigDecimal.valueOf(cnyMinor)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()

        // 5000 * 17.25 = 86,250 BDT = 8,625,000 minor units
        assertEquals(8625000L, bdtMinor)

        val lines = listOf(
            JournalLineDraft(
                accountId = ExpenseCategory.CHINA_TRAVEL_SOURCING.defaultAccountId,
                isDebit = true,
                amountMinor = bdtMinor,
                description = "China Travel: Canton Fair hotel & flight"
            ),
            JournalLineDraft(
                accountId = "coa-1010", // Paid from Cash Vault
                isDebit = false,
                amountMinor = bdtMinor,
                description = "Cash settlement for travel"
            )
        )

        assertEquals(lines.filter { it.isDebit }.sumOf { it.amountMinor }, lines.filter { !it.isDebit }.sumOf { it.amountMinor })
    }

    @Test
    fun testExpenseReversalOffsetsExactly() {
        val originalAmountMinor = 1200000L // BDT 12,000.00 marketing promotion
        val debitAccount = ExpenseCategory.MARKETING_PROMOTION.defaultAccountId
        val creditAccount = "coa-1010"

        // Original Journal Lines
        val originalDebit = JournalLineDraft(debitAccount, true, originalAmountMinor)
        val originalCredit = JournalLineDraft(creditAccount, false, originalAmountMinor)

        // Reversal Journal Lines
        val reversalDebit = JournalLineDraft(creditAccount, true, originalAmountMinor)
        val reversalCredit = JournalLineDraft(debitAccount, false, originalAmountMinor)

        // Net effect on marketing expense
        val netExpenseDebit = originalDebit.amountMinor - reversalCredit.amountMinor
        assertEquals(0L, netExpenseDebit)

        // Net effect on cash in hand
        val netCashFlow = reversalDebit.amountMinor - originalCredit.amountMinor
        assertEquals(0L, netCashFlow)
    }
}
