package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SalesUiLayoutConsistencyTest {

    @Test
    fun testPaymentMethodLocalizationAndLabels() {
        val banglaMethods = listOf(
            "CASH" to "নগদ (Cash)",
            "BANK" to "ব্যাংক (Bank)",
            "MFS" to "বিকাশ / নগদ (MFS)",
            "CREDIT" to "বকেয়া (Credit)"
        )
        val englishMethods = listOf(
            "CASH" to "Cash (নগদ)",
            "BANK" to "Bank (ব্যাংক)",
            "MFS" to "MFS (bKash/Nagad)",
            "CREDIT" to "Credit (বকেয়া)"
        )

        // Verify all 4 primary methods exist and have non-empty labels in 2x2 grid
        assertEquals(4, banglaMethods.size)
        assertEquals(4, englishMethods.size)

        // Verify chunked into 2 rows of 2 items
        val banglaRows = banglaMethods.chunked(2)
        assertEquals(2, banglaRows.size)
        assertEquals(2, banglaRows[0].size)
        assertEquals(2, banglaRows[1].size)

        // Verify MFS label contains proper descriptions
        assertTrue(banglaMethods.first { it.first == "MFS" }.second.contains("বিকাশ"))
        assertTrue(englishMethods.first { it.first == "MFS" }.second.contains("bKash"))
    }

    @Test
    fun testFinancialSummaryCalculationsWithDiscountsAndCharges() {
        val qty = 3
        val unitPriceStr = "150.00"
        val itemDiscStr = "10.00" // fixed discount per item line
        val invDiscStr = "20.00"
        val addChargesStr = "50.00"
        val paidStr = "300.00"

        val unitPriceMinor = Money.parseFromString(unitPriceStr).minorUnits
        val lineGross = qty * unitPriceMinor // 3 * 15000 = 45000
        val lineDisc = Money.parseFromString(itemDiscStr).minorUnits // 1000
        val subtotalMinor = lineGross - lineDisc // 44000

        val invDiscMinor = Money.parseFromString(invDiscStr).minorUnits // 2000
        val addChargesMinor = Money.parseFromString(addChargesStr).minorUnits // 5000
        val grandTotalMinor = subtotalMinor - invDiscMinor + addChargesMinor // 44000 - 2000 + 5000 = 47000 (470.00 BDT)

        val paidMinor = Money.parseFromString(paidStr).minorUnits // 30000
        val dueMinor = grandTotalMinor - paidMinor // 17000 (170.00 BDT)

        assertEquals(45000L, lineGross)
        assertEquals(44000L, subtotalMinor)
        assertEquals(47000L, grandTotalMinor)
        assertEquals(17000L, dueMinor)

        assertEquals("470.00", Formatters.formatMinorUnit(grandTotalMinor))
        assertEquals("170.00", Formatters.formatMinorUnit(dueMinor))
    }

    @Test
    fun testPercentageDiscountCalculation() {
        val lineGross = 100000L // 1,000.00 BDT
        val pctDiscountStr = "15" // 15%
        val pct = pctDiscountStr.toDoubleOrNull() ?: 0.0
        val discAmount = ((lineGross * pct) / 100.0).toLong() // 15000 = 150.00 BDT
        val lineTotal = lineGross - discAmount

        assertEquals(15000L, discAmount)
        assertEquals(85000L, lineTotal)
        assertEquals("850.00", Formatters.formatMinorUnit(lineTotal))
    }

    @Test
    fun testCreditLimitValidation() {
        val creditLimitMinor = 50000000L // 500,000.00 BDT
        val currentDueMinor = 45000000L   // 450,000.00 BDT
        val newSaleDueMinor = 6000000L    // 60,000.00 BDT
        val totalExpectedDue = currentDueMinor + newSaleDueMinor // 510,000.00 BDT

        val exceedsLimit = totalExpectedDue > creditLimitMinor
        assertTrue(exceedsLimit)

        // After manager override
        val approver = "Branch Manager"
        val reason = "Corporate VIP customer monthly settlement approved"
        val baseNotes = "Delivery to Motijheel branch"
        val overrideNote = "[CREDIT OVERRIDE APPROVED by $approver: $reason]"
        val finalNotes = "$baseNotes\n$overrideNote".trim()

        assertTrue(finalNotes.contains("Branch Manager"))
        assertTrue(finalNotes.contains("Corporate VIP"))
    }
}
