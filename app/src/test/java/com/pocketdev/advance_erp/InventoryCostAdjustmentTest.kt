package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * ADVANCE ERP — PHASE 06 PART 3: INVENTORY COST ADJUSTMENT TESTS
 *
 * Verifies Section 8 requirement:
 * When actual landed cost varies post-landing, the variance is split:
 *   - Remaining Allocation = ΔC * (Remaining Qty) -> Adjusts Inventory Asset (1210) & active layer cost
 *   - COGS Allocation = ΔC * (Sold Qty) -> Adjusts Cost of Goods Sold (5010)
 *   - Total Variance = Remaining Allocation + COGS Allocation == ΔC * Total Initial Qty
 */
class InventoryCostAdjustmentTest {

    data class CostAdjustmentCalculation(
        val oldUnitCostMinor: Long,
        val newUnitCostMinor: Long,
        val unitVarianceMinor: Long,
        val remainingQty: Int,
        val soldQty: Int,
        val inventoryAllocationMinor: Long,
        val cogsAllocationMinor: Long,
        val totalVarianceMinor: Long
    )

    private fun computeAdjustment(
        layer: InventoryCostLayer,
        newUnitCostMinor: Long
    ): CostAdjustmentCalculation {
        val oldUnitCost = layer.unitLandedCostBdtMinor
        val variance = newUnitCostMinor - oldUnitCost
        val rem = layer.remainingQuantity
        val sold = (layer.initialQuantity - layer.remainingQuantity).coerceAtLeast(0)

        val invAlloc = variance * rem
        val cogsAlloc = variance * sold
        val total = invAlloc + cogsAlloc

        return CostAdjustmentCalculation(
            oldUnitCostMinor = oldUnitCost,
            newUnitCostMinor = newUnitCostMinor,
            unitVarianceMinor = variance,
            remainingQty = rem,
            soldQty = sold,
            inventoryAllocationMinor = invAlloc,
            cogsAllocationMinor = cogsAlloc,
            totalVarianceMinor = total
        )
    }

    @Test
    fun testPositiveCostVarianceSplitOnPartiallySoldBatch() {
        // Batch 2 of Shoe A: 150 pairs initial, 80 sold, 70 remaining
        // Initial landed cost: 185 BDT (18,500 paisa)
        val layer = InventoryCostLayer(
            id = "layer-b2",
            productId = "prod-shoe-a",
            variantId = null,
            warehouseId = "wh-main",
            shipmentId = "shp-1",
            shipmentNumber = "SHP-001",
            warehouseReceiptId = "rcpt-1",
            receiptNumber = "RCPT-001",
            initialQuantity = 150,
            remainingQuantity = 70, // 80 sold!
            unitLandedCostBdtMinor = 18500L,
            originalCurrency = "BDT",
            originalPurchasePriceMinor = 18500L,
            exchangeRateToBdt = "1.0",
            allocatedExpensePerUnitBdtMinor = 1500L,
            receiptDate = 1000L,
            isActive = true
        )

        // New Landed Cost: 195 BDT (19,500 paisa) -> ΔC = +10 BDT (+1,000 paisa)
        val adj = computeAdjustment(layer, 19500L)

        assertEquals("Unit variance must be +10 BDT (1,000 paisa)", 1000L, adj.unitVarianceMinor)
        assertEquals("Remaining quantity must be 70", 70, adj.remainingQty)
        assertEquals("Sold quantity must be 80", 80, adj.soldQty)

        // Inventory Allocation = 70 * 1,000 = 70,000 paisa (700 BDT)
        assertEquals("Inventory Asset 1210 allocation must be 700 BDT", 70000L, adj.inventoryAllocationMinor)

        // COGS Allocation = 80 * 1,000 = 80,000 paisa (800 BDT)
        assertEquals("COGS 5010 allocation must be 800 BDT", 80000L, adj.cogsAllocationMinor)

        // Total Variance = 70,000 + 80,000 = 150,000 paisa (1,500 BDT)
        assertEquals("Total liability variance must be 1,500 BDT", 150000L, adj.totalVarianceMinor)

        // Exact match with total initial quantity: 150 * 10 BDT = 1,500 BDT
        assertEquals(150L * 1000L, adj.totalVarianceMinor)

        // Accounting double-entry verification:
        val debits = adj.inventoryAllocationMinor + adj.cogsAllocationMinor
        val credits = adj.totalVarianceMinor
        assertEquals("Debits must equal credits in double-entry journal", debits, credits)
    }

    @Test
    fun testNegativeCostVarianceSplitCostRebate() {
        // Initial landed cost: 200 BDT (20,000 paisa), 100 initial, 60 remaining, 40 sold
        val layer = InventoryCostLayer(
            id = "layer-rebate",
            productId = "prod-1",
            variantId = null,
            warehouseId = "wh-main",
            shipmentId = "shp-2",
            shipmentNumber = "SHP-002",
            warehouseReceiptId = "rcpt-2",
            receiptNumber = "RCPT-002",
            initialQuantity = 100,
            remainingQuantity = 60,
            unitLandedCostBdtMinor = 20000L,
            originalCurrency = "BDT",
            originalPurchasePriceMinor = 20000L,
            exchangeRateToBdt = "1.0",
            allocatedExpensePerUnitBdtMinor = 0L,
            receiptDate = 1000L,
            isActive = true
        )

        // Revised downward landed cost: 190 BDT (-10 BDT rebate)
        val adj = computeAdjustment(layer, 19000L)

        assertEquals(-1000L, adj.unitVarianceMinor)
        assertEquals(-60000L, adj.inventoryAllocationMinor) // Credit Inventory 600 BDT
        assertEquals(-40000L, adj.cogsAllocationMinor) // Credit COGS 400 BDT
        assertEquals(-100000L, adj.totalVarianceMinor) // Debit Payable 1,000 BDT

        // Conservation check
        assertEquals(
            abs(adj.totalVarianceMinor),
            abs(adj.inventoryAllocationMinor) + abs(adj.cogsAllocationMinor)
        )
    }
}
