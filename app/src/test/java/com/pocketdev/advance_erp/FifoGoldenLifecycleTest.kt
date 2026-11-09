package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * ADVANCE ERP — PHASE 06 PART 3: FIFO INVENTORY COST ENGINE GOLDEN LIFECYCLE TESTS
 *
 * Verifies exact requirements from Section 5 & 18:
 * Scenario: Shoe A received in 3 sequential import batches:
 *   - Batch 1: 100 pairs @ 170 BDT
 *   - Batch 2: 150 pairs @ 185 BDT
 *   - Batch 3: 200 pairs @ 195 BDT
 * Total Received = 450 pairs, Initial Valuation = 83,750 BDT
 *
 * Sale 1: 180 pairs
 *   - Depletes 100 from Batch 1 @ 170 = 17,000 BDT (Batch 1 exhausted)
 *   - Depletes 80 from Batch 2 @ 185 = 14,800 BDT (Batch 2 remaining: 70)
 *   - Total COGS = 31,800 BDT
 *   - Remaining Stock = 270 pairs (70 @ 185 + 200 @ 195)
 *   - Remaining Valuation = 51,950 BDT
 *   - Conservation Law: Remaining (51,950) + COGS (31,800) == Initial (83,750)
 *
 * Sale 2: 90 pairs
 *   - Depletes 70 from Batch 2 @ 185 = 12,950 BDT (Batch 2 exhausted)
 *   - Depletes 20 from Batch 3 @ 195 = 3,900 BDT (Batch 3 remaining: 180)
 *   - Total COGS = 16,850 BDT
 *   - Remaining Stock = 180 pairs @ 195 = 35,100 BDT
 *   - Total Conservation Law: Remaining (35,100) + COGS 1 (31,800) + COGS 2 (16,850) == 83,750 BDT
 */
class FifoGoldenLifecycleTest {

    private fun createLayer(
        batchNumber: String,
        qty: Int,
        unitCostBdt: Long,
        receiptDate: Long
    ): InventoryCostLayer {
        return InventoryCostLayer(
            id = "layer-$batchNumber",
            productId = "prod-shoe-a",
            variantId = null,
            warehouseId = "wh-main",
            shipmentId = "shp-$batchNumber",
            shipmentNumber = "SHP-2026-$batchNumber",
            warehouseReceiptId = "rcpt-$batchNumber",
            receiptNumber = "RCPT-$batchNumber",
            initialQuantity = qty,
            remainingQuantity = qty,
            unitLandedCostBdtMinor = unitCostBdt * 100, // minor units (paisa)
            originalCurrency = "CNY",
            originalPurchasePriceMinor = (unitCostBdt / 17) * 100,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 1500L,
            receiptDate = receiptDate,
            isActive = true,
            createdAt = receiptDate
        )
    }

    data class DepletionResult(
        val updatedLayers: List<InventoryCostLayer>,
        val totalCogsMinor: Long,
        val unitsDepleted: Int,
        val breakdown: List<Pair<String, Int>> // layerId to qty taken
    )

    private fun depleteFifo(layers: List<InventoryCostLayer>, requestedQty: Int): DepletionResult {
        var needed = requestedQty
        var totalCogs = 0L
        val updated = mutableListOf<InventoryCostLayer>()
        val breakdown = mutableListOf<Pair<String, Int>>()

        // Sorted strictly by receiptDate ASC
        val sortedLayers = layers.sortedBy { it.receiptDate }

        for (layer in sortedLayers) {
            if (needed <= 0 || !layer.isActive || layer.remainingQuantity <= 0) {
                updated.add(layer)
                continue
            }
            val take = minOf(needed, layer.remainingQuantity)
            val cost = take.toLong() * layer.unitLandedCostBdtMinor
            totalCogs += cost
            needed -= take
            val rem = layer.remainingQuantity - take
            breakdown.add(layer.id to take)
            updated.add(layer.copy(remainingQuantity = rem, isActive = rem > 0))
        }

        return DepletionResult(
            updatedLayers = updated,
            totalCogsMinor = totalCogs,
            unitsDepleted = requestedQty - needed,
            breakdown = breakdown
        )
    }

    @Test
    fun testShoeAGoldenLifecycleInitialState() {
        val batch1 = createLayer("B1", 100, 170, 1000L)
        val batch2 = createLayer("B2", 150, 185, 2000L)
        val batch3 = createLayer("B3", 200, 195, 3000L)

        val layers = listOf(batch1, batch2, batch3)

        val totalQty = layers.sumOf { it.remainingQuantity }
        val totalValuationMinor = layers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }

        assertEquals("Total received stock must be 450 pairs", 450, totalQty)
        assertEquals(
            "Initial inventory valuation must be 83,750 BDT (8,375,000 paisa)",
            8375000L,
            totalValuationMinor
        )
    }

    @Test
    fun testShoeASale1Of180Pairs() {
        val batch1 = createLayer("B1", 100, 170, 1000L)
        val batch2 = createLayer("B2", 150, 185, 2000L)
        val batch3 = createLayer("B3", 200, 195, 3000L)

        val initialLayers = listOf(batch1, batch2, batch3)

        // Sale of 180 pairs
        val result = depleteFifo(initialLayers, 180)

        assertEquals("Must deplete exactly 180 units", 180, result.unitsDepleted)

        // COGS Verification: 100 @ 170 (17,000) + 80 @ 185 (14,800) = 31,800 BDT
        val expectedCogsMinor = (100L * 170 * 100) + (80L * 185 * 100) // 3,180,000 paisa
        assertEquals("Sale 1 COGS must be 31,800 BDT", 3180000L, result.totalCogsMinor)
        assertEquals("Sale 1 COGS in BDT must be 31,800", 31800L, result.totalCogsMinor / 100)

        // Layer 1 verification
        val l1 = result.updatedLayers.first { it.id == "layer-B1" }
        assertEquals("Batch 1 remaining qty must be 0", 0, l1.remainingQuantity)
        assertFalse("Batch 1 must be marked inactive", l1.isActive)

        // Layer 2 verification
        val l2 = result.updatedLayers.first { it.id == "layer-B2" }
        assertEquals("Batch 2 remaining qty must be 70", 70, l2.remainingQuantity)
        assertTrue("Batch 2 must remain active", l2.isActive)

        // Layer 3 verification
        val l3 = result.updatedLayers.first { it.id == "layer-B3" }
        assertEquals("Batch 3 remaining qty must be untouched (200)", 200, l3.remainingQuantity)
        assertTrue("Batch 3 must remain active", l3.isActive)

        // Remaining Stock & Valuation Verification
        val remainingStock = result.updatedLayers.sumOf { it.remainingQuantity }
        val remainingValuationMinor = result.updatedLayers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }

        assertEquals("Remaining stock must be 270 pairs", 270, remainingStock)
        // 70 @ 185 (12,950) + 200 @ 195 (39,000) = 51,950 BDT
        val expectedValuationMinor = (70L * 185 * 100) + (200L * 195 * 100)
        assertEquals("Remaining valuation must be 51,950 BDT", 5195000L, remainingValuationMinor)

        // Total Conservation Invariant
        val initialValuationMinor = 8375000L
        assertEquals(
            "Conservation of Value: Remaining Valuation + COGS == Initial Valuation",
            initialValuationMinor,
            remainingValuationMinor + result.totalCogsMinor
        )
    }

    @Test
    fun testShoeASale2Of90PairsExhaustingBatch2() {
        val batch1 = createLayer("B1", 100, 170, 1000L)
        val batch2 = createLayer("B2", 150, 185, 2000L)
        val batch3 = createLayer("B3", 200, 195, 3000L)

        // Execute Sale 1
        val sale1 = depleteFifo(listOf(batch1, batch2, batch3), 180)

        // Execute Sale 2 of 90 pairs
        val sale2 = depleteFifo(sale1.updatedLayers, 90)

        assertEquals("Sale 2 must deplete 90 units", 90, sale2.unitsDepleted)

        // COGS 2: 70 from Batch 2 @ 185 (12,950) + 20 from Batch 3 @ 195 (3,900) = 16,850 BDT
        val expectedCogs2Minor = (70L * 185 * 100) + (20L * 195 * 100)
        assertEquals("Sale 2 COGS must be 16,850 BDT", expectedCogs2Minor, sale2.totalCogsMinor)

        // Layer 2 now exhausted
        val l2 = sale2.updatedLayers.first { it.id == "layer-B2" }
        assertEquals("Batch 2 remaining qty must be 0", 0, l2.remainingQuantity)
        assertFalse("Batch 2 must be inactive", l2.isActive)

        // Layer 3 remaining: 200 - 20 = 180
        val l3 = sale2.updatedLayers.first { it.id == "layer-B3" }
        assertEquals("Batch 3 remaining qty must be 180", 180, l3.remainingQuantity)
        assertTrue("Batch 3 must be active", l3.isActive)

        // Remaining Valuation: 180 @ 195 = 35,100 BDT
        val finalValuationMinor = sale2.updatedLayers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }
        assertEquals("Final valuation must be 35,100 BDT", 180L * 195 * 100, finalValuationMinor)

        // Full Life-cycle Conservation Check
        val totalCogsMinor = sale1.totalCogsMinor + sale2.totalCogsMinor
        val initialValuationMinor = 8375000L
        assertEquals(
            "Full Lifecycle Conservation: Final Valuation + All COGS == Initial Valuation",
            initialValuationMinor,
            finalValuationMinor + totalCogsMinor
        )
    }
}
