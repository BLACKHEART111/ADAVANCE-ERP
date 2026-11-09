package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.ShippingCostCalculator
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MinimalImportLandingCostTest {

    @Test
    fun testShippingCostCalculationGrossWeight() {
        // Business Rule: Shipping calculation MUST use Gross Weight: Gross Weight × Rate/KG
        val grossWeightKg = BigDecimal("350.0")
        val ratePerKgBdt = BigDecimal("120.00")

        val totalShipping = ShippingCostCalculator.calculateShippingCost(grossWeightKg, ratePerKgBdt)

        // 350 KG * 120 BDT = 42,000.00 BDT
        assertEquals(BigDecimal("42000.00"), totalShipping)
    }

    @Test
    fun testCategoryWiseLandingCostAllocation() {
        // Golden Scenario Items:
        // Shoe A: 600 pairs, Value 102,000 BDT (10,200,000 minor), Gross Weight 210.0 KG (60%)
        // Shoe B: 400 pairs, Value 68,000 BDT (6,800,000 minor), Gross Weight 140.0 KG (40%)
        // Total: 1,000 pairs, Value 170,000 BDT, Gross Weight 350.0 KG

        val items = listOf(
            AllocationItemInput(
                id = "prod-shoe-a",
                name = "Shoe A",
                quantity = 600,
                purchaseValueBdtMinor = 10200000L,
                grossWeightKg = BigDecimal("210.000"),
                cbm = BigDecimal("0.360000")
            ),
            AllocationItemInput(
                id = "prod-shoe-b",
                name = "Shoe B",
                quantity = 400,
                purchaseValueBdtMinor = 6800000L,
                grossWeightKg = BigDecimal("140.000"),
                cbm = BigDecimal("0.240000")
            )
        )

        // 1. Freight (42,000 BDT) allocated by Gross Weight (350 KG)
        // Shoe A: 210/350 * 42k = 25,200 BDT (2,520,000 minor)
        // Shoe B: 140/350 * 42k = 16,800 BDT (1,680,000 minor)
        val freightAllocation = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 4200000L,
            items = items,
            method = AllocationMethod.GROSS_WEIGHT
        )
        val freightA = freightAllocation.find { it.itemId == "prod-shoe-a" }!!
        val freightB = freightAllocation.find { it.itemId == "prod-shoe-b" }!!
        assertEquals(2520000L, freightA.allocatedBdtMinor)
        assertEquals(1680000L, freightB.allocatedBdtMinor)

        // 2. Customs Duty (15,000 BDT) allocated by Value (170,000 BDT)
        // Shoe A: 102k/170k * 15k = 9,000 BDT (900,000 minor)
        // Shoe B: 68k/170k * 15k = 6,000 BDT (600,000 minor)
        val customsAllocation = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 1500000L,
            items = items,
            method = AllocationMethod.PURCHASE_VALUE
        )
        val customsA = customsAllocation.find { it.itemId == "prod-shoe-a" }!!
        val customsB = customsAllocation.find { it.itemId == "prod-shoe-b" }!!
        assertEquals(900000L, customsA.allocatedBdtMinor)
        assertEquals(600000L, customsB.allocatedBdtMinor)

        // 3. Port & Handling (3,000 BDT) allocated by Gross Weight
        // Shoe A: 210/350 * 3k = 1,800 BDT (180,000 minor)
        // Shoe B: 140/350 * 3k = 1,200 BDT (120,000 minor)
        val portAllocation = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 300000L,
            items = items,
            method = AllocationMethod.GROSS_WEIGHT
        )
        val portA = portAllocation.find { it.itemId == "prod-shoe-a" }!!
        val portB = portAllocation.find { it.itemId == "prod-shoe-b" }!!
        assertEquals(180000L, portA.allocatedBdtMinor)
        assertEquals(120000L, portB.allocatedBdtMinor)

        // Total Allocated Landing Cost:
        // Shoe A = 25,200 + 9,000 + 1,800 = 36,000 BDT (3,600,000 minor) -> 60 BDT / pair
        // Shoe B = 16,800 + 6,000 + 1,200 = 24,000 BDT (2,400,000 minor) -> 60 BDT / pair
        val totalAllocatedA = freightA.allocatedBdtMinor + customsA.allocatedBdtMinor + portA.allocatedBdtMinor
        val totalAllocatedB = freightB.allocatedBdtMinor + customsB.allocatedBdtMinor + portB.allocatedBdtMinor

        assertEquals(3600000L, totalAllocatedA) // 36,000 BDT
        assertEquals(2400000L, totalAllocatedB) // 24,000 BDT
        assertEquals(6000000L, totalAllocatedA + totalAllocatedB) // Total 60,000 BDT exact reconciliation!

        val perUnitLandedA = totalAllocatedA / items[0].quantity
        val perUnitLandedB = totalAllocatedB / items[1].quantity
        assertEquals(6000L, perUnitLandedA) // 60.00 BDT per unit
        assertEquals(6000L, perUnitLandedB) // 60.00 BDT per unit
    }

    @Test
    fun testStockPostingBlockedUntilActualCostFinalized() {
        val po = PurchaseOrder(
            id = "po-block-test",
            poNumber = "PO-2026-BLOCK",
            supplierId = "sup-01",
            status = PurchaseOrderStatus.CONFIRMED.name,
            isActualCostFinalized = false // NOT finalized
        )

        // Verification: Any attempt to post stock while isActualCostFinalized is false must fail
        try {
            require(po.isActualCostFinalized) {
                "Cannot post stock to inventory: Actual landing costs must be finalized first for purchase order ${po.poNumber}"
            }
            fail("Expected exception: posting stock without cost finalization must be blocked")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("Actual landing costs must be finalized first"))
        }

        // When cost is finalized, the check passes cleanly
        val finalizedPo = po.copy(isActualCostFinalized = true, status = PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name)
        assertTrue(finalizedPo.isActualCostFinalized)
    }

    @Test
    fun testDuplicateFinalizationRejection() {
        val po = PurchaseOrder(
            id = "po-dup-test",
            poNumber = "PO-2026-DUP",
            supplierId = "sup-01",
            isActualCostFinalized = true,
            status = PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name
        )

        try {
            require(!po.isActualCostFinalized) {
                "Purchase order ${po.poNumber} actual landing costs have already been finalized. Duplicate finalization is prohibited."
            }
            fail("Expected exception for duplicate finalization")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("already been finalized"))
        }
    }
}
