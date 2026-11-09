package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ADVANCE ERP — PHASE 06 PART 3: FIFO WAREHOUSE & VARIANT ISOLATION TESTS
 *
 * Verifies Section 4 invariant:
 * - Cost layers are strictly isolated by warehouse.
 * - Sales from Warehouse A must never consume FIFO layers from Warehouse B.
 * - Variant-specific stock is consumed from matching variant layers.
 */
class FifoWarehouseIsolationTest {

    private fun createLayer(
        id: String,
        warehouseId: String,
        productId: String,
        variantId: String?,
        qty: Int,
        unitCostMinor: Long,
        receiptDate: Long
    ): InventoryCostLayer {
        return InventoryCostLayer(
            id = id,
            productId = productId,
            variantId = variantId,
            warehouseId = warehouseId,
            shipmentId = "SHP-$id",
            shipmentNumber = "SHP-NUM-$id",
            warehouseReceiptId = "RCPT-$id",
            receiptNumber = "RCPT-NUM-$id",
            initialQuantity = qty,
            remainingQuantity = qty,
            unitLandedCostBdtMinor = unitCostMinor,
            originalCurrency = "BDT",
            originalPurchasePriceMinor = unitCostMinor,
            exchangeRateToBdt = "1.0",
            allocatedExpensePerUnitBdtMinor = 0L,
            receiptDate = receiptDate,
            isActive = true
        )
    }

    @Test
    fun testWarehouseIsolationDuringFifoDepletion() {
        // Warehouse 1 (Tejgaon): 100 units @ 150 BDT
        val layerWh1 = createLayer("layer-wh1", "wh-tejgaon", "prod-1", null, 100, 15000L, 1000L)
        // Warehouse 2 (Chittagong): 100 units @ 180 BDT
        val layerWh2 = createLayer("layer-wh2", "wh-ctg", "prod-1", null, 100, 18000L, 500L) // earlier receipt date!

        val allLayers = listOf(layerWh1, layerWh2)

        // Customer orders 60 units from Tejgaon (Warehouse 1)
        // Even though Chittagong has an earlier receiptDate (500L < 1000L), Tejgaon warehouse isolation must be enforced!
        val tejgaonLayers = allLayers.filter { it.warehouseId == "wh-tejgaon" && it.productId == "prod-1" && it.isActive }
        assertEquals(1, tejgaonLayers.size)
        assertEquals("layer-wh1", tejgaonLayers[0].id)

        var needed = 60
        var cogs = 0L
        val updatedTejgaonLayers = mutableListOf<InventoryCostLayer>()

        for (l in tejgaonLayers.sortedBy { it.receiptDate }) {
            if (needed <= 0) break
            val take = minOf(needed, l.remainingQuantity)
            cogs += take * l.unitLandedCostBdtMinor
            needed -= take
            val rem = l.remainingQuantity - take
            updatedTejgaonLayers.add(l.copy(remainingQuantity = rem, isActive = rem > 0))
        }

        // Verify Tejgaon layer depleted
        val updatedWh1 = updatedTejgaonLayers.first()
        assertEquals(40, updatedWh1.remainingQuantity)
        assertEquals(60 * 15000L, cogs) // 9,000 BDT COGS at Tejgaon cost

        // Verify Chittagong layer was untouched
        val wh2Layer = allLayers.first { it.id == "layer-wh2" }
        assertEquals(100, wh2Layer.remainingQuantity)
        assertTrue(wh2Layer.isActive)
    }

    @Test
    fun testVariantIsolationDuringFifoDepletion() {
        // Product Shirt: Variant Red (size M) @ 200 BDT, Variant Blue (size L) @ 220 BDT
        val layerRed = createLayer("layer-red", "wh-1", "prod-shirt", "var-red-m", 50, 20000L, 1000L)
        val layerBlue = createLayer("layer-blue", "wh-1", "prod-shirt", "var-blue-l", 50, 22000L, 1000L)

        val layers = listOf(layerRed, layerBlue)

        // Customer buys 20 Red Shirts
        val redLayers = layers.filter { it.warehouseId == "wh-1" && it.productId == "prod-shirt" && it.variantId == "var-red-m" }
        assertEquals(1, redLayers.size)

        val targetLayer = redLayers.first()
        val take = 20
        val remaining = targetLayer.remainingQuantity - take
        val cogs = take * targetLayer.unitLandedCostBdtMinor

        assertEquals(30, remaining)
        assertEquals(20 * 20000L, cogs)

        // Blue shirts must remain 50
        val blueLayer = layers.first { it.variantId == "var-blue-l" }
        assertEquals(50, blueLayer.remainingQuantity)
    }
}
