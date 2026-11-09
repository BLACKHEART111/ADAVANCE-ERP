package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PurchaseOrderTotalsTest {

    @Test
    fun testPurchaseOrderLineTotalsAndCurrencyConversion() {
        // Item 1: 500 units @ ¥ 42.50 CNY (= 4,250 fen)
        val qty1 = 500L
        val unitPriceCnyMinor1 = 4250L
        val lineTotalCnyMinor1 = qty1 * unitPriceCnyMinor1 // 2,125,000 fen = ¥ 21,250.00

        // Item 2: 200 units @ ¥ 85.00 CNY (= 8,500 fen)
        val qty2 = 200L
        val unitPriceCnyMinor2 = 8500L
        val lineTotalCnyMinor2 = qty2 * unitPriceCnyMinor2 // 1,700,000 fen = ¥ 17,000.00

        val totalPoCnyMinor = lineTotalCnyMinor1 + lineTotalCnyMinor2 // 3,825,000 fen = ¥ 38,250.00
        assertEquals(3825000L, totalPoCnyMinor)

        // Exchange rate at transaction time: 1 CNY = 17.15 BDT
        val exchangeRate = BigDecimal("17.15")
        val poTotalBdtMinor = BigDecimal(totalPoCnyMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        // 38,250.00 * 17.15 = 655,987.50 BDT = 65,598,750 poisha
        assertEquals(65598750L, poTotalBdtMinor)
    }

    @Test
    fun testUsdPurchaseOrderConversion() {
        // 100 units @ $ 55.00 USD (= 5,500 cents) = $ 5,500.00 USD
        val qty = 100L
        val unitPriceUsdMinor = 5500L
        val totalUsdMinor = qty * unitPriceUsdMinor

        // Rate: 1 USD = 121.50 BDT
        val exchangeRate = BigDecimal("121.50")
        val totalBdtMinor = BigDecimal(totalUsdMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        // $ 5,500 * 121.50 = ৳ 6,68,250.00 = 66,825,000 poisha
        assertEquals(66825000L, totalBdtMinor)
    }

    @Test
    fun testSupplierOutstandingPayableAfterPartialPayment() {
        val totalPoBdtMinor = 65598750L // ৳ 6,55,987.50
        val advancePaymentBdtMinor = 20000000L // ৳ 2,00,000.00

        val remainingPayableMinor = totalPoBdtMinor - advancePaymentBdtMinor
        assertEquals(45598750L, remainingPayableMinor) // ৳ 4,55,987.50
    }
}
