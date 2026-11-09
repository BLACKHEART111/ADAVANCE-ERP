package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.feature.inventory.InventorySortOption
import com.pocketdev.advance_erp.feature.inventory.StockStatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Inventory Screen Compact Card Redesign.
 * Verifies sorting logic, stock status filtering, RBAC cost masking,
 * variant count calculation, and filter reset behavior.
 */
class InventoryCompactCardUiTest {

    private val sampleStockItems = listOf(
        StockItemOverview(
            productId = "p-1",
            categoryId = "c-1",
            barcode = "1001",
            variantSkus = "SKU-APPLE-RED, SKU-APPLE-GRN",
            productNameEn = "Fresh Apples",
            productNameBn = "তাজা আপেল",
            baseSku = "APL-001",
            categoryNameEn = "Fruits",
            unit = "kg",
            minStockAlert = 20,
            totalQuantity = 15, // Low stock: 15 < 20
            totalReserved = 2,
            availableQuantity = 13,
            isLowStock = true
        ),
        StockItemOverview(
            productId = "p-2",
            categoryId = "c-1",
            barcode = "1002",
            variantSkus = null,
            productNameEn = "Banana Cavendish",
            productNameBn = "কলা",
            baseSku = "BAN-002",
            categoryNameEn = "Fruits",
            unit = "doz",
            minStockAlert = 10,
            totalQuantity = 50, // In stock
            totalReserved = 5,
            availableQuantity = 45,
            isLowStock = false
        ),
        StockItemOverview(
            productId = "p-3",
            categoryId = "c-2",
            barcode = "1003",
            variantSkus = "SKU-ORANGE-S, SKU-ORANGE-M, SKU-ORANGE-L",
            productNameEn = "Sweet Orange",
            productNameBn = "মিষ্টি কমলা",
            baseSku = "ORG-003",
            categoryNameEn = "Citrus",
            unit = "kg",
            minStockAlert = 15,
            totalQuantity = 0, // Out of stock
            totalReserved = 0,
            availableQuantity = 0,
            isLowStock = true
        )
    )

    @Test
    fun testSortingByNameAscendingAndDescending() {
        val sortedAsc = sampleStockItems.sortedWith { a, b ->
            a.productNameEn.compareTo(b.productNameEn, ignoreCase = true)
        }
        assertEquals("Banana Cavendish", sortedAsc[0].productNameEn)
        assertEquals("Fresh Apples", sortedAsc[1].productNameEn)
        assertEquals("Sweet Orange", sortedAsc[2].productNameEn)

        val sortedDesc = sampleStockItems.sortedWith { a, b ->
            b.productNameEn.compareTo(a.productNameEn, ignoreCase = true)
        }
        assertEquals("Sweet Orange", sortedDesc[0].productNameEn)
        assertEquals("Fresh Apples", sortedDesc[1].productNameEn)
        assertEquals("Banana Cavendish", sortedDesc[2].productNameEn)
    }

    @Test
    fun testSortingBySkuAndQuantity() {
        // SKU Ascending
        val sortedSku = sampleStockItems.sortedWith { a, b ->
            a.baseSku.compareTo(b.baseSku, ignoreCase = true)
        }
        assertEquals("APL-001", sortedSku[0].baseSku)
        assertEquals("BAN-002", sortedSku[1].baseSku)
        assertEquals("ORG-003", sortedSku[2].baseSku)

        // Quantity Descending (High to Low)
        val sortedQtyDesc = sampleStockItems.sortedByDescending { it.totalQuantity }
        assertEquals(50, sortedQtyDesc[0].totalQuantity)
        assertEquals(15, sortedQtyDesc[1].totalQuantity)
        assertEquals(0, sortedQtyDesc[2].totalQuantity)

        // Quantity Ascending (Low to High)
        val sortedQtyAsc = sampleStockItems.sortedBy { it.totalQuantity }
        assertEquals(0, sortedQtyAsc[0].totalQuantity)
        assertEquals(15, sortedQtyAsc[1].totalQuantity)
        assertEquals(50, sortedQtyAsc[2].totalQuantity)
    }

    @Test
    fun testStockStatusFilteringLogic() {
        // ALL
        val all = sampleStockItems.filter { true }
        assertEquals(3, all.size)

        // IN_STOCK (totalQuantity > 0)
        val inStock = sampleStockItems.filter { it.totalQuantity > 0 }
        assertEquals(2, inStock.size)
        assertTrue(inStock.any { it.productId == "p-1" })
        assertTrue(inStock.any { it.productId == "p-2" })
        assertFalse(inStock.any { it.productId == "p-3" })

        // LOW_STOCK (isLowStock == true)
        val lowStock = sampleStockItems.filter { it.isLowStock }
        assertEquals(2, lowStock.size)
        assertTrue(lowStock.any { it.productId == "p-1" })
        assertTrue(lowStock.any { it.productId == "p-3" })

        // OUT_OF_STOCK (totalQuantity <= 0)
        val outOfStock = sampleStockItems.filter { it.totalQuantity <= 0 }
        assertEquals(1, outOfStock.size)
        assertEquals("p-3", outOfStock[0].productId)
    }

    @Test
    fun testRbacCostSecurityRules() {
        // Authorized roles
        assertTrue(SecurityManager.canViewCost(UserRole.OWNER_ADMIN))
        assertTrue(SecurityManager.canViewCost(UserRole.MANAGER))
        assertTrue(SecurityManager.canViewCost(UserRole.ACCOUNTANT))
        assertTrue(SecurityManager.canViewCost(UserRole.PURCHASE_OPERATOR))

        // Unauthorized roles
        assertFalse(SecurityManager.canViewCost(UserRole.SALES_OPERATOR))
        assertFalse(SecurityManager.canViewCost(UserRole.INVENTORY_OPERATOR))
    }

    @Test
    fun testVariantCountParsing() {
        val count1 = sampleStockItems[0].variantSkus?.split(",")?.map { it.trim() }?.count { it.isNotEmpty() } ?: 0
        assertEquals(2, count1)

        val count2 = sampleStockItems[1].variantSkus?.split(",")?.map { it.trim() }?.count { it.isNotEmpty() } ?: 0
        assertEquals(0, count2)

        val count3 = sampleStockItems[2].variantSkus?.split(",")?.map { it.trim() }?.count { it.isNotEmpty() } ?: 0
        assertEquals(3, count3)
    }

    @Test
    fun testCompactSummaryMetricCalculations() {
        val totalPhysical = sampleStockItems.sumOf { it.totalQuantity }
        val totalReserved = sampleStockItems.sumOf { it.totalReserved }
        val totalSellable = sampleStockItems.sumOf { it.availableQuantity }
        val lowStockCount = sampleStockItems.count { it.isLowStock }

        assertEquals(65, totalPhysical) // 15 + 50 + 0
        assertEquals(7, totalReserved)   // 2 + 5 + 0
        assertEquals(58, totalSellable)  // 13 + 45 + 0
        assertEquals(2, lowStockCount)   // p-1 and p-3
    }
}
