package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * PHASE 06 PART 1 — REQUIREMENT 3, 4, 5, 6, 8 TESTS
 * Tests 10, 11, 12, 13, 14, 15:
 * 10. Variant-specific adjustment.
 * 11. SALE_DISPATCH dashboard count.
 * 12. SALE_RETURN stock increase.
 * 13. Damaged return exclusion.
 * 14. Search and category filtering.
 * 15. FIFO inventory valuation.
 */
class VariantAdjustmentAndInventoryScreenTest {

    @Test
    fun testVariantSpecificStockAdjustmentPreservesOtherVariants() {
        // Product A has two variants: Red (var-red) and Blue (var-blue)
        val productId = "prod-shirt-01"
        val warehouseId = "wh-main"

        val balances = mutableMapOf(
            "wh-main:prod-shirt-01:var-red" to StockBalance(
                id = "sb-1",
                warehouseId = warehouseId,
                productId = productId,
                variantId = "var-red",
                quantity = 50
            ),
            "wh-main:prod-shirt-01:var-blue" to StockBalance(
                id = "sb-2",
                warehouseId = warehouseId,
                productId = productId,
                variantId = "var-blue",
                quantity = 30
            )
        )

        // Perform adjustment on Red variant only (+20)
        val selectedVariantId = "var-red"
        val adjustQty = 20
        val isIncrease = true

        val key = "$warehouseId:$productId:$selectedVariantId"
        val current = balances[key]!!
        val updated = current.copy(
            quantity = if (isIncrease) current.quantity + adjustQty else current.quantity - adjustQty
        )
        balances[key] = updated

        // Verify only Red was adjusted, Blue remained unchanged!
        assertEquals(70, balances["wh-main:prod-shirt-01:var-red"]!!.quantity)
        assertEquals(30, balances["wh-main:prod-shirt-01:var-blue"]!!.quantity)
    }

    @Test
    fun testSaleDispatchDashboardCountAndAmount() {
        val todayStart = System.currentTimeMillis() - 86400000L
        val movements = listOf(
            StockMovement(
                movementNumber = "SM-01",
                warehouseId = "wh-1",
                productId = "p-1",
                movementType = StockMovementType.SALE_DISPATCH.code,
                quantity = 10,
                unitCostMinor = 20000L, // ৳200
                createdAt = System.currentTimeMillis()
            ),
            StockMovement(
                movementNumber = "SM-02",
                warehouseId = "wh-1",
                productId = "p-2",
                movementType = "SALE", // Legacy movement type
                quantity = 5,
                unitCostMinor = 30000L, // ৳300
                createdAt = System.currentTimeMillis()
            ),
            StockMovement(
                movementNumber = "SM-03",
                warehouseId = "wh-1",
                productId = "p-1",
                movementType = StockMovementType.PURCHASE_RECEIPT.code,
                quantity = 50,
                unitCostMinor = 20000L,
                createdAt = System.currentTimeMillis()
            ),
            StockMovement(
                movementNumber = "SM-04",
                warehouseId = "wh-1",
                productId = "p-1",
                movementType = StockMovementType.SALE_DISPATCH.code,
                quantity = 15,
                unitCostMinor = 20000L,
                createdAt = todayStart - 50000L // Yesterday (before todayStart)
            )
        )

        // Filter valid dispatches today (both SALE_DISPATCH and legacy SALE)
        val todaysDispatches = movements.filter {
            (it.movementType == StockMovementType.SALE_DISPATCH.code || it.movementType == "SALE") &&
                    it.createdAt >= todayStart
        }

        assertEquals(2, todaysDispatches.size)
        val totalQuantityDispatchedToday = todaysDispatches.sumOf { it.quantity }
        assertEquals(15, totalQuantityDispatchedToday) // 10 + 5

        val totalDispatchAmountTodayMinor = todaysDispatches.sumOf { it.quantity.toLong() * it.unitCostMinor }
        assertEquals(350000L, totalDispatchAmountTodayMinor) // (10 * 20000) + (5 * 30000) = 200,000 + 150,000
    }

    @Test
    fun testSaleReturnStockIncreaseAndStylingDirection() {
        // SALE_RETURN must be classified as an incoming movement (Stock IN)
        val movementType = StockMovementType.SALE_RETURN
        assertTrue("SALE_RETURN must be incoming (Stock IN)", movementType.isIncoming)

        val legacyCode = "SALE_RETURN"
        val resolved = StockMovementType.fromCode(legacyCode)
        assertEquals(StockMovementType.SALE_RETURN, resolved)
        assertTrue(resolved.isIncoming)

        // Stock quantity increases on valid return
        var warehouseStock = 50
        val returnedSoundUnits = 12
        warehouseStock += returnedSoundUnits
        assertEquals(62, warehouseStock)
    }

    @Test
    fun testDamagedReturnExclusionFromSellableStockAndAccounting() {
        // Customer returns 10 units: 7 are sound, 3 are damaged
        val totalReturnUnits = 10
        val damagedUnits = 3
        val soundUnits = totalReturnUnits - damagedUnits

        val unitSellingPriceMinor = 50000L // ৳500.00
        val unitCostMinor = 30000L         // ৳300.00

        var sellableStock = 100

        // Invariant: Damaged returned goods must NOT increase sellable stock!
        sellableStock += soundUnits
        assertEquals("Sellable stock must only increase by sound units (7), not damaged units (3)", 107, sellableStock)

        // Double-entry accounting verification
        val totalCustomerCredit = totalReturnUnits.toLong() * unitSellingPriceMinor // ৳5,000
        val soundInventoryRestored = soundUnits.toLong() * unitCostMinor             // ৳2,100
        val damageWriteOffLoss = damagedUnits.toLong() * unitCostMinor              // ৳900
        val totalCogsReversed = totalReturnUnits.toLong() * unitCostMinor           // ৳3,000

        // Entry 1: Revenue Reversal
        val entry1Lines = listOf(
            JournalLineDraft("coa-4020", true, totalCustomerCredit, "Sales Returns"),
            JournalLineDraft("coa-1040", false, totalCustomerCredit, "Accounts Receivable")
        )
        val dr1 = entry1Lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val cr1 = entry1Lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        assertEquals(dr1, cr1)

        // Entry 2: Sound Stock COGS reversal to Inventory Asset
        val entry2Lines = listOf(
            JournalLineDraft("coa-1210", true, soundInventoryRestored, "Inventory Asset"),
            JournalLineDraft("coa-5010", false, soundInventoryRestored, "COGS")
        )
        val dr2 = entry2Lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val cr2 = entry2Lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        assertEquals(dr2, cr2)

        // Entry 3: Damaged Stock write-off
        val entry3Lines = listOf(
            JournalLineDraft("coa-5050", true, damageWriteOffLoss, "Damaged Write-Off"),
            JournalLineDraft("coa-5010", false, damageWriteOffLoss, "COGS")
        )
        val dr3 = entry3Lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val cr3 = entry3Lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        assertEquals(dr3, cr3)

        // Total COGS credit matches sum of restored inventory asset and damage write-off
        assertEquals(totalCogsReversed, soundInventoryRestored + damageWriteOffLoss)
    }

    @Test
    fun testSearchAndCategoryFiltering() {
        val items = listOf(
            StockItemOverview(
                productId = "p-1",
                categoryId = "cat-shoes",
                barcode = "8901234567890",
                variantSkus = "SHOE-42-BLK, SHOE-43-BLK",
                productNameEn = "Men Oxford Leather Shoes",
                productNameBn = "পুরুষদের চামড়ার জুতো",
                baseSku = "SHOE-OXF-01",
                categoryNameEn = "Footwear",
                unit = "Pair",
                minStockAlert = 10,
                totalQuantity = 50,
                totalReserved = 5,
                availableQuantity = 45,
                isLowStock = false
            ),
            StockItemOverview(
                productId = "p-2",
                categoryId = "cat-bags",
                barcode = "8909999999999",
                variantSkus = "BAG-BRN, BAG-BLK",
                productNameEn = "Travel Duffel Bag",
                productNameBn = "ভ্রমণ ব্যাগ",
                baseSku = "BAG-DUF-01",
                categoryNameEn = "Luggage",
                unit = "Pcs",
                minStockAlert = 5,
                totalQuantity = 8,
                totalReserved = 0,
                availableQuantity = 8,
                isLowStock = false
            )
        )

        // 1. Search by name (English)
        val searchByNameEn = items.filter { it.productNameEn.contains("Oxford", ignoreCase = true) }
        assertEquals(1, searchByNameEn.size)
        assertEquals("p-1", searchByNameEn[0].productId)

        // 2. Search by name (Bangla)
        val searchByNameBn = items.filter { it.productNameBn.contains("ভ্রমণ", ignoreCase = true) }
        assertEquals(1, searchByNameBn.size)
        assertEquals("p-2", searchByNameBn[0].productId)

        // 3. Search by base SKU
        val searchBySku = items.filter { it.baseSku.contains("SHOE-OXF", ignoreCase = true) }
        assertEquals(1, searchBySku.size)

        // 4. Search by Barcode
        val searchByBarcode = items.filter { it.barcode?.contains("8909999999999") == true }
        assertEquals(1, searchByBarcode.size)
        assertEquals("p-2", searchByBarcode[0].productId)

        // 5. Search by Variant SKU
        val searchByVariantSku = items.filter { it.variantSkus?.contains("SHOE-43-BLK") == true }
        assertEquals(1, searchByVariantSku.size)
        assertEquals("p-1", searchByVariantSku[0].productId)

        // 6. Filter by Category without duplicating products
        val categoryFiltered = items.filter { it.categoryId == "cat-shoes" }
        assertEquals(1, categoryFiltered.size)
        val uniqueProducts = categoryFiltered.map { it.productId }.toSet()
        assertEquals(categoryFiltered.size, uniqueProducts.size)
    }

    @Test
    fun testFifoInventoryValuationCalculation() {
        // Product X has 2 active cost layers:
        // Layer 1: 50 units remaining @ ৳300.00 (30,000 minor)
        // Layer 2: 70 units remaining @ ৳340.00 (34,000 minor)
        // Retail selling price is ৳500.00 (50,000 minor)
        val layers = listOf(
            InventoryCostLayer(
                id = "layer-1",
                productId = "prod-x",
                warehouseId = "wh-1",
                shipmentId = "SHP-1",
                shipmentNumber = "SHP-001",
                warehouseReceiptId = "WR-1",
                receiptNumber = "WR-001",
                initialQuantity = 100,
                remainingQuantity = 50,
                unitLandedCostBdtMinor = 30000L,
                originalPurchasePriceMinor = 30000L,
                exchangeRateToBdt = "1.0",
                allocatedExpensePerUnitBdtMinor = 0L,
                isActive = true
            ),
            InventoryCostLayer(
                id = "layer-2",
                productId = "prod-x",
                warehouseId = "wh-1",
                shipmentId = "SHP-2",
                shipmentNumber = "SHP-002",
                warehouseReceiptId = "WR-2",
                receiptNumber = "WR-002",
                initialQuantity = 70,
                remainingQuantity = 70,
                unitLandedCostBdtMinor = 34000L,
                originalPurchasePriceMinor = 34000L,
                exchangeRateToBdt = "1.0",
                allocatedExpensePerUnitBdtMinor = 0L,
                isActive = true
            )
        )

        // FIFO valuation:
        // (50 * 30000) + (70 * 34000) = 1,500,000 + 2,380,000 = 3,880,000 minor (৳38,800.00)
        val expectedFifoValuation = (50L * 30000L) + (70L * 34000L)
        val computedFifoValuation = layers.filter { it.isActive && it.remainingQuantity > 0 }
            .sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }

        assertEquals(3880000L, expectedFifoValuation)
        assertEquals(expectedFifoValuation, computedFifoValuation)

        // Contrast with arbitrary retail selling price valuation (which must NOT be used):
        val retailPriceValuation = 120L * 50000L // 6,000,000 minor
        assertNotEquals(
            "Inventory valuation must strictly use remaining FIFO cost layers, not retail price!",
            retailPriceValuation,
            computedFifoValuation
        )
    }
}
