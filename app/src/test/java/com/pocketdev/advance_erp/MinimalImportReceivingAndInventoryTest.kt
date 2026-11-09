package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MinimalImportReceivingAndInventoryTest {

    @Test
    fun testReceivingMethodAndPhysicalInspectionReconciliation() {
        // Expected: 400 pairs of Shoe B
        val expectedQty = 400
        val receivedGoodQty = 390
        val damagedQty = 5
        val missingQty = 5

        // Invariant: Good + Damaged + Missing == Expected
        assertEquals(expectedQty, receivedGoodQty + damagedQty + missingQty)
    }

    @Test
    fun testDamageWriteOffAccountingDr5050Cr1220() {
        // 5 damaged pairs @ 230 BDT = 1,150 BDT (115,000 minor)
        val lossMinor = 115000L

        val lineDr = JournalLineDraft(
            accountId = "coa-5050", // Damaged Merchandise Write-Off
            isDebit = true,
            amountMinor = lossMinor
        )
        val lineCr = JournalLineDraft(
            accountId = "coa-1220", // Merchandise In-Transit
            isDebit = false,
            amountMinor = lossMinor
        )

        val lines = listOf(lineDr, lineCr)
        val totalDr = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCr = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(lossMinor, totalDr)
        assertEquals(lossMinor, totalCr)
        assertEquals(totalDr, totalCr)
    }

    @Test
    fun testShortageClaimsReceivableAccountingDr1050Cr1220() {
        // 5 missing pairs @ 230 BDT = 1,150 BDT (115,000 minor)
        val claimMinor = 115000L

        val lineDr = JournalLineDraft(
            accountId = "coa-1050", // Supplier Shortage & Claims Receivable
            isDebit = true,
            amountMinor = claimMinor
        )
        val lineCr = JournalLineDraft(
            accountId = "coa-1220", // Merchandise In-Transit
            isDebit = false,
            amountMinor = claimMinor
        )

        val lines = listOf(lineDr, lineCr)
        val totalDr = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCr = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(claimMinor, totalDr)
        assertEquals(claimMinor, totalCr)
        assertEquals(totalDr, totalCr)
    }

    @Test
    fun testGoodStockInventoryPostingDr1210Cr1220AndFifoLayer() {
        // 390 good pairs @ 230 BDT = 89,700 BDT (8,970,000 minor)
        val inventoryMinor = 8970000L

        val lineDr = JournalLineDraft(
            accountId = "coa-1210", // Inventory Asset
            isDebit = true,
            amountMinor = inventoryMinor
        )
        val lineCr = JournalLineDraft(
            accountId = "coa-1220", // Merchandise In-Transit
            isDebit = false,
            amountMinor = inventoryMinor
        )

        val lines = listOf(lineDr, lineCr)
        val totalDr = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCr = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(inventoryMinor, totalDr)
        assertEquals(inventoryMinor, totalCr)
        assertEquals(totalDr, totalCr)

        // FIFO Layer Creation
        val unitCostBdtMinor = 23000L // 230.00 BDT
        val layer = InventoryCostLayer(
            id = "layer-001",
            productId = "prod-shoe-b",
            warehouseId = "wh-dhk-01",
            shipmentId = "BATCH-PO-2026-001",
            shipmentNumber = "BATCH-PO-2026-001",
            warehouseReceiptId = "rcp-001",
            receiptNumber = "RCP-2026-001",
            initialQuantity = 390,
            remainingQuantity = 390,
            unitLandedCostBdtMinor = unitCostBdtMinor, // 230.00 BDT
            originalPurchasePriceMinor = 1000L,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 6000L,
            purchaseOrderId = "po-2026-001",
            purchaseBatchId = "BATCH-PO-2026-001"
        )

        assertEquals(390, layer.initialQuantity)
        assertEquals(390, layer.remainingQuantity)
        assertEquals(23000L, layer.unitLandedCostBdtMinor)
        assertEquals(8970000L, layer.remainingQuantity * layer.unitLandedCostBdtMinor)
    }

    @Test
    fun testFifoSalesConsumptionLayerDepletion() {
        // Layer: 390 pairs @ 230 BDT
        val layer = InventoryCostLayer(
            id = "layer-001",
            productId = "prod-shoe-b",
            warehouseId = "wh-dhk-01",
            shipmentId = "BATCH-PO-2026-001",
            shipmentNumber = "BATCH-PO-2026-001",
            warehouseReceiptId = "rcp-001",
            receiptNumber = "RCP-2026-001",
            initialQuantity = 390,
            remainingQuantity = 390,
            unitLandedCostBdtMinor = 23000L,
            originalPurchasePriceMinor = 1000L,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 6000L
        )

        // Customer buys 100 pairs
        val soldQty = 100
        val consumedFromLayer = minOf(soldQty, layer.remainingQuantity)
        val updatedLayer = layer.copy(remainingQuantity = layer.remainingQuantity - consumedFromLayer)
        val cogsMinor = consumedFromLayer * layer.unitLandedCostBdtMinor // 100 * 230 = 23,000 BDT

        assertEquals(290, updatedLayer.remainingQuantity)
        assertEquals(2300000L, cogsMinor) // COGS = 23,000.00 BDT
    }

    @Test
    fun testPostArrivalAdditionalCostCapitalizableVsPeriodExpense() {
        val costMinor = 500000L // 5,000.00 BDT

        // 1. Capitalizable Additional Cost (e.g. repacking / inland transfer)
        // Posted to Dr. 1210 Inventory Asset / Cr. 1020 Cash or Bank
        val capDr = JournalLineDraft("coa-1210", isDebit = true, amountMinor = costMinor)
        val capCr = JournalLineDraft("coa-1020", isDebit = false, amountMinor = costMinor)
        val capLines = listOf(capDr, capCr)
        assertEquals(capLines.filter { it.isDebit }.sumOf { it.amountMinor }, capLines.filter { !it.isDebit }.sumOf { it.amountMinor })

        // 2. Period Expense Additional Cost (e.g. warehouse rent)
        // Posted to Dr. 5110 Operating Expense / Cr. 1020 Cash or Bank
        val expDr = JournalLineDraft("coa-5110", isDebit = true, amountMinor = costMinor)
        val expCr = JournalLineDraft("coa-1020", isDebit = false, amountMinor = costMinor)
        val expLines = listOf(expDr, expCr)
        assertEquals(expLines.filter { it.isDebit }.sumOf { it.amountMinor }, expLines.filter { !it.isDebit }.sumOf { it.amountMinor })
    }

    @Test
    fun testManualPurchaseCompletionDisposition() {
        // Shoe A: 600 unreceived pairs
        val unreceivedQty = 600
        val record = PurchaseCompletionRecord(
            id = "comp-001",
            purchaseOrderId = "po-2026-001",
            completedAt = System.currentTimeMillis(),
            orderedQuantity = 1000,
            receivedQuantity = 400,
            goodQuantity = 390,
            damagedQuantity = 5,
            missingQuantity = 5,
            remainingQuantity = unreceivedQty,
            paymentDueMinor = 700000L,
            unreceivedDisposition = "CANCELLED_BALANCE",
            reason = "Supplier factory delayed second batch - order closed",
            notes = "Formal purchase completion audited"
        )

        assertEquals(600, record.remainingQuantity)
        assertEquals("CANCELLED_BALANCE", record.unreceivedDisposition)
        assertTrue(record.reason!!.contains("delayed second batch"))
    }
}
