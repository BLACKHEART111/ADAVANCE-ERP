package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode

object CbmCalculator {

    private val ONE_MILLION = BigDecimal("1000000")

    /**
     * Calculates Cubic Meters (CBM) from centimeter dimensions:
     * CBM = (Length × Width × Height × Carton Count) / 1,000,000
     */
    fun calculateCbm(
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        cartonCount: Int = 1
    ): BigDecimal {
        require(lengthCm > BigDecimal.ZERO) { "Length must be greater than 0: $lengthCm" }
        require(widthCm > BigDecimal.ZERO) { "Width must be greater than 0: $widthCm" }
        require(heightCm > BigDecimal.ZERO) { "Height must be greater than 0: $heightCm" }
        require(cartonCount > 0) { "Carton count must be at least 1: $cartonCount" }

        val volumeCm3 = lengthCm.multiply(widthCm).multiply(heightCm).multiply(BigDecimal(cartonCount))
        return volumeCm3.divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
    }

    fun parseDimension(dimStr: String): BigDecimal {
        val cleaned = dimStr.trim().replace(",", "")
        if (cleaned.isBlank()) return BigDecimal.ZERO
        return BigDecimal(cleaned)
    }
}
