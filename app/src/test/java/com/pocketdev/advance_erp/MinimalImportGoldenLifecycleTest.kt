package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.core.engine.ShippingCostCalculator
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MinimalImportGoldenLifecycleTest {

    // Double-entry ledger journal lines for end-to-end trial balance audit
    private val trialBalanceLines = mutableListOf<JournalLineDraft>()

    private fun postJournal(desc: String, lines: List<JournalLineDraft>) {
        require(lines.size >= 2) { "Journal must have at least 2 lines" }
        val dr = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val cr = lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        require(dr == cr) { "Unbalanced journal for '$desc': Dr=$dr, Cr=$cr" }
        trialBalanceLines.addAll(lines)
    }

    @Test
    fun testCompleteSection17GoldenLifecycleWorkflow() {
        // ==============================================================
        // STAGE 1: Purchase Order Creation & Confirmation
        // ==============================================================
        val fxRate = BigDecimal("17.00")
        val po = PurchaseOrder(
            id = "po-golden-001",
            poNumber = "PO-2026-GOLDEN-01",
            supplierId = "sup-guangzhou-01",
            status = PurchaseOrderStatus.DRAFT.name,
            currency = "CNY",
            exchangeRateToBdt = "17.0000",
            totalAmountMinor = 1000000L, // 10,000.00 CNY
            totalBdtAmountMinor = 17000000L, // 170,000.00 BDT
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            totalGrossWeightKg = "350.000",
            totalNetWeightKg = "330.000",
            totalCartons = 20,
            shippingRatePerKgBdt = "120.00"
        )

        val shoeAItem = PurchaseOrderItem(
            id = "poi-shoe-a",
            purchaseOrderId = po.id,
            productId = "prod-shoe-a",
            quantity = 600,
            unitPriceMinor = 1000L, // 10.00 CNY
            totalAmountMinor = 600000L, // 6,000.00 CNY
            totalBdtAmountMinor = 10200000L, // 102,000.00 BDT
            cartonQuantity = 12,
            grossWeightKg = "210.000"
        )

        val shoeBItem = PurchaseOrderItem(
            id = "poi-shoe-b",
            purchaseOrderId = po.id,
            productId = "prod-shoe-b",
            quantity = 400,
            unitPriceMinor = 1000L, // 10.00 CNY
            totalAmountMinor = 400000L, // 4,000.00 CNY
            totalBdtAmountMinor = 6800000L, // 68,000.00 BDT
            cartonQuantity = 8,
            grossWeightKg = "140.000"
        )

        assertEquals(1000000L, shoeAItem.totalAmountMinor + shoeBItem.totalAmountMinor)
        assertEquals(17000000L, shoeAItem.totalBdtAmountMinor + shoeBItem.totalBdtAmountMinor)

        // Confirm PO (Commitment confirmed, NO financial postings yet)
        val confirmedPo = po.copy(status = PurchaseOrderStatus.CONFIRMED.name)
        assertEquals(PurchaseOrderStatus.CONFIRMED.name, confirmedPo.status)
        assertEquals(0, trialBalanceLines.size) // No ledger changes on confirmation

        // Batch shipment auto-created 1:1
        val batchShipmentNumber = "BATCH-${po.poNumber}"
        assertEquals("BATCH-PO-2026-GOLDEN-01", batchShipmentNumber)

        // ==============================================================
        // STAGE 2: Supplier Payment Inside PO Details (30% Advance)
        // ==============================================================
        val schedule = PaymentTermsCalculator.calculateSchedule(PaymentTermType.THIRTY_SEVENTY, po.totalAmountMinor, fxRate)
        val advanceInstallment = schedule[0]
        assertEquals(300000L, advanceInstallment.amountMinor) // 3,000.00 CNY
        assertEquals(5100000L, advanceInstallment.bdtAmountMinor) // 51,000.00 BDT

        // Record 30% advance payment:
        // Dr. 2010 Supplier Payable / Advance (Asset/Liability offset)
        // Cr. 1020 Bank Corporate Account
        postJournal(
            desc = "PO Advance Payment 30%",
            lines = listOf(
                JournalLineDraft("coa-2010", isDebit = true, amountMinor = 5100000L),
                JournalLineDraft("coa-1020", isDebit = false, amountMinor = 5100000L)
            )
        )

        val paidPo = confirmedPo.copy(
            paidAmountMinor = 300000L,
            paidBdtAmountMinor = 5100000L,
            status = PurchaseOrderStatus.PARTIALLY_PAID.name
        )
        val remainingDueMinor = paidPo.totalAmountMinor - paidPo.paidAmountMinor
        assertEquals(700000L, remainingDueMinor) // 7,000.00 CNY remaining due

        // ==============================================================
        // STAGE 3: Carton & Packing (Single + Mixed with Auto Allocation)
        // ==============================================================
        val mixedResult = CartonWeightAllocationEngine.allocateWeights(
            cartonGrossWeightKg = BigDecimal("15.000"),
            cartonNetWeightKg = BigDecimal("14.000"),
            lines = listOf(
                MixedCartonLineInput("prod-shoe-a", null, "Shoe A", 20),
                MixedCartonLineInput("prod-shoe-b", null, "Shoe B", 10)
            )
        )
        assertEquals(BigDecimal("10.000"), mixedResult.find { it.productId == "prod-shoe-a" }!!.allocatedGrossWeightKg)
        assertEquals(BigDecimal("5.000"), mixedResult.find { it.productId == "prod-shoe-b" }!!.allocatedGrossWeightKg)

        // ==============================================================
        // STAGE 4: Shipping & Landing Cost Incurrence
        // ==============================================================
        // Gross weight shipping: 350 KG * 120 BDT = 42,000.00 BDT (4,200,000 minor)
        val shippingCostMinor = 4200000L
        val customsCostMinor = 1500000L // 15,000.00 BDT
        val portCostMinor = 300000L // 3,000.00 BDT
        val totalLandedCostMinor = shippingCostMinor + customsCostMinor + portCostMinor // 60,000.00 BDT (6,000,000 minor)

        // Landed costs journaled into 1220 Merchandise In-Transit:
        postJournal(
            desc = "Landed Cost Items: Freight, Customs, Port",
            lines = listOf(
                JournalLineDraft("coa-1220", isDebit = true, amountMinor = totalLandedCostMinor),
                JournalLineDraft("coa-1020", isDebit = false, amountMinor = totalLandedCostMinor)
            )
        )

        // Also purchase commitment capitalized to 1220 Merchandise In-Transit / 2010 AP
        postJournal(
            desc = "PO Merchandise In-Transit Capitalization",
            lines = listOf(
                JournalLineDraft("coa-1220", isDebit = true, amountMinor = 17000000L),
                JournalLineDraft("coa-2010", isDebit = false, amountMinor = 17000000L)
            )
        )

        // ==============================================================
        // STAGE 5: Finalize Actual Landing Cost (Category Allocation)
        // ==============================================================
        val allocItems = listOf(
            AllocationItemInput("prod-shoe-a", "Shoe A", 600, BigDecimal("210.000"), BigDecimal.ZERO, 10200000L),
            AllocationItemInput("prod-shoe-b", "Shoe B", 400, BigDecimal("140.000"), BigDecimal.ZERO, 6800000L)
        )

        val allocFreight = LandedCostAllocationEngine.allocate(shippingCostMinor, allocItems, AllocationMethod.GROSS_WEIGHT)
        val allocCustoms = LandedCostAllocationEngine.allocate(customsCostMinor, allocItems, AllocationMethod.PURCHASE_VALUE)
        val allocPort = LandedCostAllocationEngine.allocate(portCostMinor, allocItems, AllocationMethod.GROSS_WEIGHT)

        val shoeATotalLanded = allocFreight.find { it.itemId == "prod-shoe-a" }!!.allocatedBdtMinor +
                allocCustoms.find { it.itemId == "prod-shoe-a" }!!.allocatedBdtMinor +
                allocPort.find { it.itemId == "prod-shoe-a" }!!.allocatedBdtMinor

        val shoeBTotalLanded = allocFreight.find { it.itemId == "prod-shoe-b" }!!.allocatedBdtMinor +
                allocCustoms.find { it.itemId == "prod-shoe-b" }!!.allocatedBdtMinor +
                allocPort.find { it.itemId == "prod-shoe-b" }!!.allocatedBdtMinor

        assertEquals(3600000L, shoeATotalLanded) // 36,000.00 BDT -> 60.00 BDT / pair
        assertEquals(2400000L, shoeBTotalLanded) // 24,000.00 BDT -> 60.00 BDT / pair
        assertEquals(6000000L, shoeATotalLanded + shoeBTotalLanded) // Exact 60k reconciliation!

        val unitLandedCostA = (10200000L + shoeATotalLanded) / 600 // 170 + 60 = 230.00 BDT (23,000 minor)
        val unitLandedCostB = (6800000L + shoeBTotalLanded) / 400 // 170 + 60 = 230.00 BDT (23,000 minor)
        assertEquals(23000L, unitLandedCostA)
        assertEquals(23000L, unitLandedCostB)

        // Mark PO as ACTUAL_COST_FINALIZED
        val finalizedPo = paidPo.copy(
            isActualCostFinalized = true,
            status = PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name,
            actualCostFinalizedAt = System.currentTimeMillis()
        )
        assertTrue(finalizedPo.isActualCostFinalized)

        // ==============================================================
        // STAGE 6: Partial Warehouse Receiving (Shoe B: 400 Expected)
        // Inspection Result: 390 Good, 5 Damaged, 5 Missing
        // ==============================================================
        val goodQty = 390
        val damagedQty = 5
        val missingQty = 5
        assertEquals(400, goodQty + damagedQty + missingQty)

        // Accounting for Receiving:
        // 1. Damaged Write-off: 5 * 230 = 1,150 BDT (115,000 minor) -> Dr. 5050 / Cr. 1220
        postJournal(
            desc = "Damage Write-off (5 pairs @ 230 BDT)",
            lines = listOf(
                JournalLineDraft("coa-5050", isDebit = true, amountMinor = 115000L),
                JournalLineDraft("coa-1220", isDebit = false, amountMinor = 115000L)
            )
        )

        // 2. Shortage Claim: 5 * 230 = 1,150 BDT (115,000 minor) -> Dr. 1050 / Cr. 1220
        postJournal(
            desc = "Supplier Shortage Claim (5 pairs @ 230 BDT)",
            lines = listOf(
                JournalLineDraft("coa-1050", isDebit = true, amountMinor = 115000L),
                JournalLineDraft("coa-1220", isDebit = false, amountMinor = 115000L)
            )
        )

        // 3. Good Stock Posting: 390 * 230 = 89,700 BDT (8,970,000 minor) -> Dr. 1210 / Cr. 1220
        postJournal(
            desc = "Good Stock Inventory Posting (390 pairs @ 230 BDT)",
            lines = listOf(
                JournalLineDraft("coa-1210", isDebit = true, amountMinor = 8970000L),
                JournalLineDraft("coa-1220", isDebit = false, amountMinor = 8970000L)
            )
        )

        // FIFO Layer Created:
        val fifoLayer = InventoryCostLayer(
            id = "layer-golden-shoe-b",
            productId = "prod-shoe-b",
            warehouseId = "wh-dhk-01",
            shipmentId = batchShipmentNumber,
            shipmentNumber = batchShipmentNumber,
            warehouseReceiptId = "rcp-golden",
            receiptNumber = "RCP-GOLDEN",
            initialQuantity = 390,
            remainingQuantity = 390,
            unitLandedCostBdtMinor = 23000L,
            originalPurchasePriceMinor = 1000L,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 6000L,
            purchaseOrderId = po.id,
            purchaseBatchId = batchShipmentNumber
        )
        assertEquals(390, fifoLayer.initialQuantity)
        assertEquals(23000L, fifoLayer.unitLandedCostBdtMinor)
        assertEquals(8970000L, fifoLayer.remainingQuantity * fifoLayer.unitLandedCostBdtMinor)

        // ==============================================================
        // STAGE 7: FIFO Sales Consumption
        // Sell 100 pairs of Shoe B @ 400 BDT (40,000 BDT revenue)
        // COGS = 100 * 230 BDT = 23,000 BDT (2,300,000 minor)
        // ==============================================================
        val soldQty = 100
        val cogsMinor = soldQty * fifoLayer.unitLandedCostBdtMinor // 23,000.00 BDT
        postJournal(
            desc = "Sales COGS Recognition (100 pairs @ 230 BDT)",
            lines = listOf(
                JournalLineDraft("coa-5010", isDebit = true, amountMinor = cogsMinor),
                JournalLineDraft("coa-1210", isDebit = false, amountMinor = cogsMinor)
            )
        )
        postJournal(
            desc = "Sales Revenue (100 pairs @ 400 BDT)",
            lines = listOf(
                JournalLineDraft("coa-1020", isDebit = true, amountMinor = 4000000L),
                JournalLineDraft("coa-4010", isDebit = false, amountMinor = 4000000L)
            )
        )

        val remainingLayerQty = fifoLayer.remainingQuantity - soldQty
        assertEquals(290, remainingLayerQty)

        // ==============================================================
        // STAGE 8: Post-Arrival Additional Cost (Capitalized to Inventory)
        // 5,000.00 BDT (500,000 minor) handling charge
        // ==============================================================
        postJournal(
            desc = "Post-Arrival Capitalized Handling Cost",
            lines = listOf(
                JournalLineDraft("coa-1210", isDebit = true, amountMinor = 500000L),
                JournalLineDraft("coa-1020", isDebit = false, amountMinor = 500000L)
            )
        )

        // ==============================================================
        // STAGE 9: Manual Purchase Completion
        // Shoe A: 600 unreceived pairs -> Cancelled unfulfilled balance
        // ==============================================================
        val completion = PurchaseCompletionRecord(
            id = "comp-golden",
            purchaseOrderId = po.id,
            completedAt = System.currentTimeMillis(),
            orderedQuantity = 1000,
            receivedQuantity = 400,
            goodQuantity = 390,
            damagedQuantity = 5,
            missingQuantity = 5,
            remainingQuantity = 600,
            paymentDueMinor = 700000L,
            unreceivedDisposition = "CANCELLED_BALANCE",
            reason = "Supplier delayed remaining 600 pairs, agreement cancelled",
            notes = "Formal manual PO completion verified"
        )
        assertEquals(600, completion.remainingQuantity)
        assertEquals("CANCELLED_BALANCE", completion.unreceivedDisposition)

        val completedPo = finalizedPo.copy(status = PurchaseOrderStatus.COMPLETED.name)
        assertEquals(PurchaseOrderStatus.COMPLETED.name, completedPo.status)

        // ==============================================================
        // STAGE 10: Double-Entry Trial Balance Verification
        // Verify that Total Debits == Total Credits across all stages!
        // ==============================================================
        val totalTrialDebits = trialBalanceLines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalTrialCredits = trialBalanceLines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertTrue(totalTrialDebits > 0L)
        assertTrue(totalTrialCredits > 0L)
        assertEquals(totalTrialDebits, totalTrialCredits)
    }
}
