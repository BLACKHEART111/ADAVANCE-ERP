package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test
    fun testBanglaDigitsConversion() {
        val input = "1234567890"
        val expected = "১২৩৪৫৬৭৮৯০"
        assertEquals(expected, Formatters.toBanglaDigits(input))
    }

    @Test
    fun testFormatMoneyEnglish() {
        val money = Money.ofMinor(125000L, CurrencyCode.BDT) // 1,250.00
        val formatted = Formatters.formatMoney(money, AppLanguage.ENGLISH)
        assertEquals("৳ 1,250.00", formatted)
    }

    @Test
    fun testFormatMoneyBangla() {
        val money = Money.ofMinor(125000L, CurrencyCode.BDT) // 1,250.00
        val formatted = Formatters.formatMoney(money, AppLanguage.BANGLA)
        assertEquals("৳ ১,২৫০.০০", formatted)
    }

    @Test
    fun testFormatMoneyUSDAndCNY() {
        val usd = Money.ofMinor(5500L, CurrencyCode.USD)
        val formattedUsd = Formatters.formatMoney(usd, AppLanguage.ENGLISH)
        assertEquals("$ 55.00", formattedUsd)

        val cny = Money.ofMinor(320000L, CurrencyCode.CNY)
        val formattedCny = Formatters.formatMoney(cny, AppLanguage.ENGLISH)
        assertEquals("¥ 3,200.00", formattedCny)
    }

    @Test
    fun testFormatNumberBangla() {
        val count = 250L
        val formatted = Formatters.formatNumber(count, AppLanguage.BANGLA)
        assertEquals("২৫০", formatted)
    }
}
