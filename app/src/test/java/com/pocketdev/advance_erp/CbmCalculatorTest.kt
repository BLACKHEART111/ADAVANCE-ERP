package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.CbmCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CbmCalculatorTest {

    @Test
    fun testStandardCartonCbm() {
        // Carton 50cm x 40cm x 30cm = 60,000 cm3
        // CBM = 60,000 / 1,000,000 = 0.060000 m3
        val l = BigDecimal("50.0")
        val w = BigDecimal("40.0")
        val h = BigDecimal("30.0")

        val cbm = CbmCalculator.calculateCbm(l, w, h, 1)
        assertEquals(BigDecimal("0.060000"), cbm)
    }

    @Test
    fun testMultipleCartonsCbm() {
        // 10 cartons of 60cm x 50cm x 40cm = 120,000 * 10 = 1,200,000 cm3
        // CBM = 1,200,000 / 1,000,000 = 1.200000 m3
        val l = BigDecimal("60.0")
        val w = BigDecimal("50.0")
        val h = BigDecimal("40.0")

        val cbm = CbmCalculator.calculateCbm(l, w, h, 10)
        assertEquals(BigDecimal("1.200000"), cbm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroDimensionThrowsException() {
        CbmCalculator.calculateCbm(BigDecimal.ZERO, BigDecimal("40.0"), BigDecimal("30.0"), 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeDimensionThrowsException() {
        CbmCalculator.calculateCbm(BigDecimal("-10.0"), BigDecimal("40.0"), BigDecimal("30.0"), 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroCartonCountThrowsException() {
        CbmCalculator.calculateCbm(BigDecimal("50.0"), BigDecimal("40.0"), BigDecimal("30.0"), 0)
    }
}
