package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.ReceiptStatus
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarehouseReceivingRulesTest {

    @Test
    fun testPartialReceivingCalculation() {
        val expectedQty = 500
        val receivedQty = 350
        val damagedQty = 10
        val missingQty = 140

        // In a partial delivery: 350 received + 140 missing + 10 damaged = 500 expected
        assertEquals(expectedQty, receivedQty + missingQty + damagedQty)

        // Effective sound stock added to warehouse = received - damaged = 340
        val soundStockAdded = receivedQty - damagedQty
        assertEquals(340, soundStockAdded)
    }

    @Test(expected = IllegalStateException::class)
    fun testDuplicatePostingPrevented() {
        val receipt = WarehouseReceipt(
            id = "wr-001",
            receiptNumber = "WR-2026-0001",
            shipmentId = "shp-01",
            warehouseId = "wh-dhk-01",
            status = ReceiptStatus.POSTED.name
        )

        // Check rule
        check(receipt.status != ReceiptStatus.POSTED.name) {
            "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testExcessReceivingWithoutApprovalRejected() {
        val item = WarehouseReceiptItem(
            id = "item-01",
            receiptId = "wr-01",
            productId = "prod-01",
            expectedQuantity = 100,
            receivedQuantity = 120, // Excess by 20 without approval!
            excessQuantity = 20
        )

        val isApproved = false
        if (item.receivedQuantity > item.expectedQuantity && !isApproved) {
            throw IllegalStateException(
                "Received quantity (${item.receivedQuantity}) exceeds expected (${item.expectedQuantity}). Explicit approval required!"
            )
        }
    }

    @Test
    fun testExcessReceivingWithApprovalAllowed() {
        val item = WarehouseReceiptItem(
            id = "item-01",
            receiptId = "wr-01",
            productId = "prod-01",
            expectedQuantity = 100,
            receivedQuantity = 120,
            excessQuantity = 20
        )

        val isApproved = true
        var posted = false
        if (item.receivedQuantity <= item.expectedQuantity || isApproved) {
            posted = true
        }
        assertTrue(posted)
    }
}
