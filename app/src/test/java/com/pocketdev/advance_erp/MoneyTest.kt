package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun testMoneyMinorUnitsAddition() {
        val m1 = Money.ofMinor(150000L, CurrencyCode.BDT) // 1,500.00
        val m2 = Money.ofMinor(7550L, CurrencyCode.BDT)   // 75.50
        val sum = m1 + m2

        assertEquals(157550L, sum.minorUnits)
        assertEquals(CurrencyCode.BDT, sum.currency)
    }

    @Test
    fun testMoneySubtraction() {
        val m1 = Money.ofMinor(100000L, CurrencyCode.BDT)
        val m2 = Money.ofMinor(25000L, CurrencyCode.BDT)
        val diff = m1 - m2

        assertEquals(75000L, diff.minorUnits)
    }

    @Test
    fun testMoneyMultiplicationWithFactor() {
        val unitCost = Money.ofMinor(85000L, CurrencyCode.BDT) // 850.00
        val totalCost = unitCost * 12L

        assertEquals(1020000L, totalCost.minorUnits) // 10,200.00
    }

    @Test
    fun testMoneyMultiplicationWithBigDecimalRate() {
        val cnyAmount = Money.ofMinor(10000L, CurrencyCode.CNY) // 100.00 CNY
        val exchangeRate = BigDecimal("16.85") // 1 CNY = 16.85 BDT
        val bdtMinor = (cnyAmount * exchangeRate).minorUnits

        assertEquals(168500L, bdtMinor) // 1,685.00
    }

    @Test
    fun testMoneyParseFromString() {
        val parsed = Money.parseFromString("1,250.50", CurrencyCode.BDT)
        assertEquals(125050L, parsed.minorUnits)

        val parsedZero = Money.parseFromString("", CurrencyCode.BDT)
        assertEquals(0L, parsedZero.minorUnits)
    }

    @Test
    fun testComparison() {
        val m1 = Money.ofMinor(50000L, CurrencyCode.BDT)
        val m2 = Money.ofMinor(60000L, CurrencyCode.BDT)

        assertTrue(m1 < m2)
        assertTrue(m2 > m1)
    }
}
