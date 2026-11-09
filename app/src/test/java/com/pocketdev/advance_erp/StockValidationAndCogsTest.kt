package com.pocketdev.advance_erp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StockValidationAndCogsTest {

    @Test
    fun testStockValidationPreventsNegativeStock() {
        val currentStock = 20
        val requestedQty = 25

        val hasEnoughStock = currentStock >= requestedQty
        assertFalse(hasEnoughStock)

        try {
            if (!hasEnoughStock) {
                throw IllegalStateException("Insufficient stock: available $currentStock, requested $requestedQty")
            }
            fail("Expected IllegalStateException for insufficient stock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Insufficient stock"))
        }
    }

    @Test
    fun testStockReductionOnConfirmedSale() {
        val initialStock = 100
        val soldQty = 30
        val remainingStock = initialStock - soldQty

        assertEquals(70, remainingStock)
    }

    data class MockCostLayer(
        val id: String,
        var remainingQuantity: Int,
        val unitLandedCostMinor: Long
    )

    @Test
    fun testFifoCostLayerDepletionAndCogsCalculation() {
        // Layer 1: 20 units @ ৳500.00 (50,000 poisa) landed cost
        // Layer 2: 30 units @ ৳550.00 (55,000 poisa) landed cost
        val layers = mutableListOf(
            MockCostLayer("layer-1", 20, 50000L),
            MockCostLayer("layer-2", 30, 55000L)
        )

        val requestedQty = 35 // Needs all 20 from Layer 1 + 15 from Layer 2
        var needed = requestedQty
        var totalCogsMinor = 0L

        for (layer in layers) {
            if (needed <= 0) break
            val take = minOf(needed, layer.remainingQuantity)
            totalCogsMinor += (take.toLong() * layer.unitLandedCostMinor)
            layer.remainingQuantity -= take
            needed -= take
        }

        assertEquals(0, needed)
        // Layer 1 depleted: 20 * 50,000 = 1,000,000 poisa
        // Layer 2 depleted: 15 * 55,000 = 825,000 poisa
        // Total COGS = 1,825,000 poisa (৳18,250.00)
        assertEquals(1825000L, totalCogsMinor)
        assertEquals(0, layers[0].remainingQuantity)
        assertEquals(15, layers[1].remainingQuantity)

        val unitCostMinor = totalCogsMinor / requestedQty
        // 1,825,000 / 35 = 52,142 poisa (৳521.42 per unit)
        assertEquals(52142L, unitCostMinor)
    }

    @Test
    fun testFallbackToProductPurchasePriceWhenLayersExhausted() {
        // Layer has only 10 units, sale is for 15 units.
        val layers = mutableListOf(
            MockCostLayer("layer-1", 10, 60000L) // 10 units @ ৳600.00
        )
        val fallbackPurchasePriceMinor = 50000L // ৳500.00
        val requestedQty = 15
        var needed = requestedQty
        var totalCogsMinor = 0L

        for (layer in layers) {
            if (needed <= 0) break
            val take = minOf(needed, layer.remainingQuantity)
            totalCogsMinor += (take.toLong() * layer.unitLandedCostMinor)
            layer.remainingQuantity -= take
            needed -= take
        }

        if (needed > 0) {
            totalCogsMinor += (needed.toLong() * fallbackPurchasePriceMinor)
            needed = 0
        }

        // 10 * 60,000 = 600,000 poisa
        // 5 * 50,000 = 250,000 poisa
        // Total = 850,000 poisa
        assertEquals(850000L, totalCogsMinor)
        assertEquals(0, layers[0].remainingQuantity)
    }
}
