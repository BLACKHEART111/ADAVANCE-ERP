package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MinimalImportCartonPackingTest {

    @Test
    fun testCbmCalculationFormula() {
        // Formula: CBM = (Length × Width × Height) / 1,000,000
        val lengthCm = BigDecimal("50.0")
        val widthCm = BigDecimal("40.0")
        val heightCm = BigDecimal("30.0")
        val cartonCount = 10

        // Single carton CBM: 50 * 40 * 30 / 1,000,000 = 60,000 / 1,000,000 = 0.06 m³
        val singleCbm = CbmCalculator.calculateCbm(lengthCm, widthCm, heightCm, 1)
        assertEquals(BigDecimal("0.060000"), singleCbm)

        // 10 cartons: 0.6 m³
        val totalCbm = CbmCalculator.calculateCbm(lengthCm, widthCm, heightCm, cartonCount)
        assertEquals(BigDecimal("0.600000"), totalCbm)
    }

    @Test
    fun testMixedCartonAutomaticQuantityBasedWeightAllocation() {
        // Golden Example from User Prompt:
        // Carton CTN-001:
        // - Shoe A: 20 Pair
        // - Shoe B: 10 Pair
        // - Total: 30 Pair
        // - Gross Weight: 15 KG
        //
        // Formula:
        // Product Allocated Weight = Carton Gross Weight × (Product Quantity / Total Carton Quantity)
        // Result:
        // - Shoe A = 15 × (20 / 30) = 10 KG
        // - Shoe B = 15 × (10 / 30) = 5 KG

        val grossWeight = BigDecimal("15.000")
        val netWeight = BigDecimal("14.000")
        val lines = listOf(
            MixedCartonLineInput(productId = "prod-shoe-a", productName = "Shoe A", quantity = 20),
            MixedCartonLineInput(productId = "prod-shoe-b", productName = "Shoe B", quantity = 10)
        )

        val result = CartonWeightAllocationEngine.allocateWeights(grossWeight, netWeight, lines)

        assertEquals(2, result.size)

        val shoeA = result.find { it.productId == "prod-shoe-a" }!!
        val shoeB = result.find { it.productId == "prod-shoe-b" }!!

        assertEquals(BigDecimal("10.000"), shoeA.allocatedGrossWeightKg)
        assertEquals(BigDecimal("5.000"), shoeB.allocatedGrossWeightKg)

        // Verify exact sum equals carton gross weight
        val sumGross = result.map { it.allocatedGrossWeightKg }.reduce(BigDecimal::add)
        assertEquals(grossWeight, sumGross)

        val sumNet = result.map { it.allocatedNetWeightKg }.reduce(BigDecimal::add)
        assertEquals(netWeight, sumNet)
    }

    @Test
    fun testMixedCartonResidualRoundingPrecision() {
        // Test with non-clean division: 3 products with quantities 7, 7, 7 (Total 21)
        // Gross Weight = 10.000 KG
        // 10 * 7 / 21 = 3.333333...
        // Must sum exactly to 10.000 without residual drift
        val grossWeight = BigDecimal("10.000")
        val netWeight = BigDecimal("9.500")
        val lines = listOf(
            MixedCartonLineInput(productId = "prod-1", productName = "Item 1", quantity = 7),
            MixedCartonLineInput(productId = "prod-2", productName = "Item 2", quantity = 7),
            MixedCartonLineInput(productId = "prod-3", productName = "Item 3", quantity = 7)
        )

        val result = CartonWeightAllocationEngine.allocateWeights(grossWeight, netWeight, lines)

        assertEquals(3, result.size)

        val sumGross = result.map { it.allocatedGrossWeightKg }.reduce(BigDecimal::add)
        assertEquals(grossWeight, sumGross)

        val sumNet = result.map { it.allocatedNetWeightKg }.reduce(BigDecimal::add)
        assertEquals(netWeight, sumNet)
    }
}
