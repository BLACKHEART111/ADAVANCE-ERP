package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class LandedCostAllocationTest {

    /**
     * Requirement Test from User Prompt:
     * A shipment contains:
     * - Product A: 100 units
     * - Product B: 200 units
     * Import freight: ৳30,000
     * Allocate freight by quantity.
     * Expected:
     * - Product A freight allocation: ৳10,000
     * - Product B freight allocation: ৳20,000
     * Total allocated freight must equal ৳30,000 exactly.
     */
    @Test
    fun testPromptExampleQuantityAllocation() {
        val totalFreightBdtMinor = 3000000L // ৳30,000.00 = 3,000,000 poisha

        val itemA = AllocationItemInput(
            id = "prod-a",
            name = "Product A",
            quantity = 100
        )
        val itemB = AllocationItemInput(
            id = "prod-b",
            name = "Product B",
            quantity = 200
        )

        val results = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = totalFreightBdtMinor,
            items = listOf(itemA, itemB),
            method = AllocationMethod.QUANTITY
        )

        assertEquals(2, results.size)

        val allocA = results.find { it.itemId == "prod-a" }!!
        val allocB = results.find { it.itemId == "prod-b" }!!

        // Expected: Product A = ৳10,000 (1,000,000 poisha)
        assertEquals(1000000L, allocA.allocatedBdtMinor)
        assertEquals(10000L, allocA.unitAllocatedCostBdtMinor) // 1,000,000 / 100 = ৳100.00/pc

        // Expected: Product B = ৳20,000 (2,000,000 poisha)
        assertEquals(2000000L, allocB.allocatedBdtMinor)
        assertEquals(10000L, allocB.unitAllocatedCostBdtMinor) // 2,000,000 / 200 = ৳100.00/pc

        // Total allocated freight must equal ৳30,000 exactly
        val totalAllocated = allocA.allocatedBdtMinor + allocB.allocatedBdtMinor
        assertEquals(totalFreightBdtMinor, totalAllocated)
    }

    @Test
    fun testAllocationByGrossWeight() {
        val customsDutyMinor = 15000000L // ৳1,50,000.00

        val item1 = AllocationItemInput(
            id = "item-heavy",
            name = "Heavy Machinery Part",
            quantity = 10,
            grossWeightKg = BigDecimal("300.0")
        )
        val item2 = AllocationItemInput(
            id = "item-light",
            name = "Light Plastic Casing",
            quantity = 50,
            grossWeightKg = BigDecimal("100.0")
        )

        val results = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = customsDutyMinor,
            items = listOf(item1, item2),
            method = AllocationMethod.GROSS_WEIGHT
        )

        val alloc1 = results.find { it.itemId == "item-heavy" }!!
        val alloc2 = results.find { it.itemId == "item-light" }!!

        // Total weight = 400 kg. Item 1 = 300/400 = 75%, Item 2 = 100/400 = 25%
        assertEquals(11250000L, alloc1.allocatedBdtMinor) // ৳1,12,500.00
        assertEquals(3750000L, alloc2.allocatedBdtMinor)  // ৳37,500.00
        assertEquals(customsDutyMinor, alloc1.allocatedBdtMinor + alloc2.allocatedBdtMinor)
    }

    @Test
    fun testAllocationByCbm() {
        val seaFreightMinor = 8000000L // ৳80,000.00

        val item1 = AllocationItemInput(
            id = "cbm-large",
            name = "Bulky Pillows",
            quantity = 100,
            cbm = BigDecimal("2.5")
        )
        val item2 = AllocationItemInput(
            id = "cbm-compact",
            name = "Compact Smartwatches",
            quantity = 500,
            cbm = BigDecimal("0.5")
        )

        val results = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = seaFreightMinor,
            items = listOf(item1, item2),
            method = AllocationMethod.CBM
        )

        val alloc1 = results.find { it.itemId == "cbm-large" }!!
        val alloc2 = results.find { it.itemId == "cbm-compact" }!!

        // Total CBM = 3.0. Item 1 = 2.5/3.0 = 5/6, Item 2 = 0.5/3.0 = 1/6
        val totalAllocated = alloc1.allocatedBdtMinor + alloc2.allocatedBdtMinor
        assertEquals(seaFreightMinor, totalAllocated)
    }

    @Test
    fun testAllocationByPurchaseValue() {
        val insuranceCostMinor = 2500000L // ৳25,000.00

        val itemGold = AllocationItemInput(
            id = "val-high",
            name = "Premium Smartphones",
            quantity = 50,
            purchaseValueBdtMinor = 60000000L // ৳6,00,000.00
        )
        val itemSilver = AllocationItemInput(
            id = "val-low",
            name = "Charging Cables",
            quantity = 500,
            purchaseValueBdtMinor = 40000000L // ৳4,00,000.00
        )

        val results = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = insuranceCostMinor,
            items = listOf(itemGold, itemSilver),
            method = AllocationMethod.PURCHASE_VALUE
        )

        val alloc1 = results.find { it.itemId == "val-high" }!!
        val alloc2 = results.find { it.itemId == "val-low" }!!

        // Total Value = 10,00,000. Item 1 = 60%, Item 2 = 40%
        assertEquals(1500000L, alloc1.allocatedBdtMinor) // ৳15,000
        assertEquals(1000000L, alloc2.allocatedBdtMinor) // ৳10,000
        assertEquals(insuranceCostMinor, alloc1.allocatedBdtMinor + alloc2.allocatedBdtMinor)
    }

    @Test
    fun testRoundingRemainderExactReconciliation() {
        // ৳100.00 = 10,000 minor units split equally among 3 items
        // 10000 / 3 = 3333 with remainder 1
        // Remainder 1 must be distributed so sum == 10,000 exactly!
        val costMinor = 10000L
        val items = listOf(
            AllocationItemInput("i1", "Item 1", quantity = 1),
            AllocationItemInput("i2", "Item 2", quantity = 1),
            AllocationItemInput("i3", "Item 3", quantity = 1)
        )

        val results = LandedCostAllocationEngine.allocate(costMinor, items, AllocationMethod.QUANTITY)

        val sum = results.sumOf { it.allocatedBdtMinor }
        assertEquals(10000L, sum)

        // One item received 3334, two received 3333
        assertEquals(1, results.count { it.allocatedBdtMinor == 3334L })
        assertEquals(2, results.count { it.allocatedBdtMinor == 3333L })
    }
}
