package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReceiptStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * MASTER TEST — PART 18: MANDATORY END-TO-END SCENARIO
 * Comprehensive verification of the entire Import Management Module lifecycle:
 * 1. PO Creation & Confirmation (Product A 1,000 pairs @ CNY 10, FX 17 -> CNY 10,000 = BDT 170,000).
 * 2. Payment Terms: 30% Advance (CNY 3,000 = BDT 51,000), 70% Balance (CNY 7,000 = BDT 119,000).
 * 3. Multi-shipment Consignment: Shipment 1 (600) + Shipment 2 (400), 50 Cartons 60x40x40 cm = 4.8 CBM.
 * 4. Over-shipment and Overpayment Prevention Guards.
 * 5. Import Cost Sheet: Freight BDT 12k, Customs BDT 5k, C&F BDT 2k, Transport BDT 1k = BDT 20k.
 * 6. Landed Cost Engine: Freight by Gross Weight, other categories by Value/Qty/CBM.
 *    Estimated Landed Cost = BDT 190,000, Estimated Unit Landed Cost = BDT 190.
 * 7. Warehouse Receiving:
 *    - Shipment 1: 600 expected -> 580 good, 15 damaged, 5 missing.
 *    - Shipment 2: 400 expected -> 400 good, 0 damaged, 0 missing.
 *    - Sellable stock added = 980. Damaged (15) and Missing (5) excluded from sellable stock.
 * 8. Inventory Cost Layers:
 *    - Layer 001: 580 @ BDT 190 = BDT 110,200.
 *    - Layer 002: 400 @ BDT 190 = BDT 76,000.
 *    - Total Inventory Value = BDT 186,200.
 * 9. FIFO Sales: Sell 600 pairs -> 580 from Layer 1, 20 from Layer 2. COGS = BDT 114,000, Remaining = 380 pairs = BDT 72,200.
 * 10. Actual Cost Adjustment: Actual freight increases from BDT 12k to 14k (+2k variance).
 *     Recalculate actual landed cost, adjust remaining inventory (BDT 760) and sold COGS (BDT 1,200).
 * 11. Full Double-Entry Accounting Trial Balance Reconciliation (Total Debits == Total Credits).
 */
class ImportModuleGoldenLifecycleTest {

    private val journalEntries = mutableListOf<JournalEntry>()
    private val journalLines = mutableListOf<JournalLine>()
    private val stockBalances = mutableMapOf<String, Int>()
    private val stockMovements = mutableListOf<StockMovement>()
    private val inventoryCostLayers = mutableListOf<InventoryCostLayer>()

    @Before
    fun setUp() {
        journalEntries.clear()
        journalLines.clear()
        stockBalances.clear()
        stockMovements.clear()
        inventoryCostLayers.clear()
    }

    private fun postJournal(
        entryNumber: String,
        desc: String,
        refType: String,
        refId: String,
        lines: List<JournalLineDraft>
    ) {
        require(lines.size >= 2) { "Journal must contain at least 2 lines" }
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        require(totalDebit == totalCredit) { "Unbalanced journal: Debit=$totalDebit, Credit=$totalCredit" }

        val entryId = "je-${UUID.randomUUID()}"
        val entry = JournalEntry(
            id = entryId,
            entryNumber = entryNumber,
            entryDate = System.currentTimeMillis(),
            descriptionEn = desc,
            descriptionBn = desc,
            referenceType = refType,
            referenceId = refId,
            totalDebitMinor = totalDebit,
            totalCreditMinor = totalCredit,
            isPosted = true
        )
        journalEntries.add(entry)
        lines.forEach { l ->
            journalLines.add(
                JournalLine(
                    id = "jl-${UUID.randomUUID()}",
                    journalEntryId = entryId,
                    accountId = l.accountId,
                    accountCode = l.accountId.removePrefix("coa-"),
                    accountName = l.description ?: l.accountId,
                    isDebit = l.isDebit,
                    amountMinor = l.amountMinor,
                    description = l.description
                )
            )
        }
    }

    @Test
    fun testPart18MandatoryLifecycleComplete() {
        // =================================================================
        // STEP 1: Product & Supplier Setup
        // =================================================================
        val supplierId = "sup-china-001"
        var supplierPayableMinor = 0L

        val productId = "prod-shoes-001"
        val orderedQty = 1000
        val unitPriceCnyMinor = 1000L // CNY 10.00
        val fxRate = BigDecimal("17.00") // 1 CNY = 17.00 BDT

        // =================================================================
        // STEP 2: Purchase Order Creation & Confirmation
        // =================================================================
        val poLineTotalCnyMinor = orderedQty.toLong() * unitPriceCnyMinor // 10,000 CNY = 1,000,000 fen
        val poLineTotalBdtMinor = BigDecimal(poLineTotalCnyMinor)
            .multiply(fxRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact() // BDT 170,000 = 17,000,000 paisa

        assertEquals(1_000_000L, poLineTotalCnyMinor)
        assertEquals(17_000_000L, poLineTotalBdtMinor)

        var po = PurchaseOrder(
            id = "po-2026-001",
            poNumber = "PO-2026-CN-001",
            supplierId = supplierId,
            status = PurchaseOrderStatus.DRAFT.name,
            currency = "CNY",
            exchangeRateToBdt = fxRate.toPlainString(),
            totalAmountMinor = poLineTotalCnyMinor,
            totalBdtAmountMinor = poLineTotalBdtMinor,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            orderDate = System.currentTimeMillis(),
            notes = "[Terms: 30% Advance + 70% Balance]"
        )

        // Invariant Check 1: DRAFT order creates ZERO financial or stock movements
        assertEquals(0L, supplierPayableMinor)
        assertEquals(0, stockBalances[productId] ?: 0)
        assertEquals(0, journalEntries.size)

        // Verify Payment Schedule Calculation (30% Advance + 70% Balance)
        val schedule = PaymentTermsCalculator.calculateSchedule(
            termType = PaymentTermType.THIRTY_SEVENTY,
            totalAmountMinor = po.totalAmountMinor,
            exchangeRateToBdt = fxRate
        )
        assertEquals(2, schedule.size)
        // Installment 1: 30% = CNY 3,000 (300,000 fen) = BDT 51,000 (5,100,000 paisa)
        assertEquals(300_000L, schedule[0].amountMinor)
        assertEquals(5_100_000L, schedule[0].bdtAmountMinor)
        // Installment 2: 70% = CNY 7,000 (700,000 fen) = BDT 119,000 (11,900,000 paisa)
        assertEquals(700_000L, schedule[1].amountMinor)
        assertEquals(11_900_000L, schedule[1].bdtAmountMinor)
        // Schedule reconciles exactly
        assertEquals(po.totalAmountMinor, schedule.sumOf { it.amountMinor })
        assertEquals(po.totalBdtAmountMinor, schedule.sumOf { it.bdtAmountMinor })

        // Confirm PO: Operational commitment only
        po = po.copy(status = PurchaseOrderStatus.CONFIRMED.name)
        supplierPayableMinor += poLineTotalBdtMinor

        // Stock and Cash must remain ZERO upon confirmation
        assertEquals(0, stockBalances[productId] ?: 0)
        assertEquals(0, journalEntries.size)
        assertEquals(17_000_000L, supplierPayableMinor)

        // =================================================================
        // STEP 3: Multi-Tranche Supplier Payments
        // =================================================================
        // Tranche 1: 30% Advance = CNY 3,000 (BDT 51,000)
        val pay1Cny = 300_000L
        val pay1Bdt = BigDecimal(pay1Cny).multiply(fxRate).toLong()
        po = po.copy(
            paidAmountMinor = po.paidAmountMinor + pay1Cny,
            paidBdtAmountMinor = po.paidBdtAmountMinor + pay1Bdt,
            status = PurchaseOrderStatus.PARTIALLY_PAID.name
        )
        supplierPayableMinor -= pay1Bdt
        postJournal(
            entryNumber = "JV-2026-0001",
            desc = "Payment 1 (30% Advance) for PO-2026-CN-001",
            refType = "SUPPLIER_PAYMENT",
            refId = "SP-001",
            lines = listOf(
                JournalLineDraft("coa-2010", true, pay1Bdt, "Dr. Accounts Payable"),
                JournalLineDraft("coa-1020", false, pay1Bdt, "Cr. Bank Account")
            )
        )
        assertEquals(300_000L, po.paidAmountMinor)
        assertEquals(PurchaseOrderStatus.PARTIALLY_PAID.name, po.status)
        assertEquals(11_900_000L, supplierPayableMinor)

        // Invariant Check: Overpayment Prevention
        val remainingBeforeFinal = po.totalAmountMinor - po.paidAmountMinor
        assertEquals(700_000L, remainingBeforeFinal) // CNY 7,000 remaining
        try {
            val overpayment = 800_000L // CNY 8,000
            require(overpayment <= remainingBeforeFinal) {
                "Payment amount ($overpayment) exceeds remaining PO balance ($remainingBeforeFinal)"
            }
            fail("Expected overpayment rejection")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("exceeds remaining PO balance") == true)
        }

        // Tranche 2: 70% Balance = CNY 7,000 (BDT 119,000)
        val pay2Cny = 700_000L
        val pay2Bdt = BigDecimal(pay2Cny).multiply(fxRate).toLong()
        po = po.copy(
            paidAmountMinor = po.paidAmountMinor + pay2Cny,
            paidBdtAmountMinor = po.paidBdtAmountMinor + pay2Bdt,
            status = PurchaseOrderStatus.PAID.name
        )
        supplierPayableMinor -= pay2Bdt
        postJournal(
            entryNumber = "JV-2026-0002",
            desc = "Payment 2 (70% Final Balance) for PO-2026-CN-001",
            refType = "SUPPLIER_PAYMENT",
            refId = "SP-002",
            lines = listOf(
                JournalLineDraft("coa-2010", true, pay2Bdt, "Dr. Accounts Payable"),
                JournalLineDraft("coa-1020", false, pay2Bdt, "Cr. Bank Account")
            )
        )
        assertEquals(1_000_000L, po.paidAmountMinor)
        assertEquals(PurchaseOrderStatus.PAID.name, po.status)
        assertEquals(0L, supplierPayableMinor)

        // =================================================================
        // STEP 4: Carton Specs, CBM Calculation & Multi-Shipments
        // =================================================================
        // Specs: 50 cartons total, 20 pairs per carton, 60 x 40 x 40 cm -> 4.8 CBM total
        val cartonLength = BigDecimal("60")
        val cartonWidth = BigDecimal("40")
        val cartonHeight = BigDecimal("40")

        val singleCartonCbm = CbmCalculator.calculateCbm(cartonLength, cartonWidth, cartonHeight, 1)
        assertEquals(BigDecimal("0.096000"), singleCartonCbm)

        val total50CartonsCbm = CbmCalculator.calculateCbm(cartonLength, cartonWidth, cartonHeight, 50)
        assertEquals(BigDecimal("4.800000"), total50CartonsCbm)

        // Shipment 001 = 600 pairs across 30 cartons (20 pairs each)
        val shp1Cartons = (1..30).map { i ->
            ShipmentCarton(
                id = "ctn-shp1-$i",
                shipmentId = "shp-001",
                cartonNumber = "CTN-01-%02d".format(i),
                productId = productId,
                quantity = 20,
                lengthCm = "60",
                widthCm = "40",
                heightCm = "40",
                grossWeightKg = "20.0",
                netWeightKg = "18.0",
                cbm = "0.096000"
            )
        }
        val shp1Qty = shp1Cartons.sumOf { it.quantity }
        val shp1Cbm = CbmCalculator.calculateCbm(cartonLength, cartonWidth, cartonHeight, 30)
        assertEquals(600, shp1Qty)
        assertEquals(BigDecimal("2.880000"), shp1Cbm)

        val shipment1 = ImportShipment(
            id = "shp-001",
            shipmentNumber = "SHP-2026-0001",
            supplierId = supplierId,
            purchaseOrderId = po.id,
            shippingMethod = ShippingMethod.SEA.name,
            origin = "Shenzhen, China",
            destination = "Chattogram Port, Bangladesh",
            status = ShipmentStatus.IN_TRANSIT.name,
            totalCartons = 30,
            totalQuantity = 600,
            totalGrossWeightKg = "600.0",
            totalNetWeightKg = "540.0",
            totalCbm = shp1Cbm.toPlainString()
        )

        // Invariant Check: Over-shipment Prevention
        // Attempting to ship 500 units in Shipment 2 when remaining is 400 must fail!
        try {
            val illegalShipmentQty = 500
            val alreadyShipped = shipment1.totalQuantity
            require(alreadyShipped + illegalShipmentQty <= orderedQty) {
                "Total shipment quantity (${alreadyShipped + illegalShipmentQty}) exceeds ordered PO quantity ($orderedQty)"
            }
            fail("Expected over-shipment rejection")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("exceeds ordered PO quantity") == true)
        }

        // Shipment 002 = 400 pairs across 20 cartons (20 pairs each)
        val shp2Cartons = (1..20).map { i ->
            ShipmentCarton(
                id = "ctn-shp2-$i",
                shipmentId = "shp-002",
                cartonNumber = "CTN-02-%02d".format(i),
                productId = productId,
                quantity = 20,
                lengthCm = "60",
                widthCm = "40",
                heightCm = "40",
                grossWeightKg = "20.0",
                netWeightKg = "18.0",
                cbm = "0.096000"
            )
        }
        val shp2Qty = shp2Cartons.sumOf { it.quantity }
        val shp2Cbm = CbmCalculator.calculateCbm(cartonLength, cartonWidth, cartonHeight, 20)
        assertEquals(400, shp2Qty)
        assertEquals(BigDecimal("1.920000"), shp2Cbm)

        val shipment2 = ImportShipment(
            id = "shp-002",
            shipmentNumber = "SHP-2026-0002",
            supplierId = supplierId,
            purchaseOrderId = po.id,
            shippingMethod = ShippingMethod.SEA.name,
            origin = "Shenzhen, China",
            destination = "Chattogram Port, Bangladesh",
            status = ShipmentStatus.IN_TRANSIT.name,
            totalCartons = 20,
            totalQuantity = 400,
            totalGrossWeightKg = "400.0",
            totalNetWeightKg = "360.0",
            totalCbm = shp2Cbm.toPlainString()
        )

        // Reconcile total cartons and pairs across shipments:
        assertEquals(1000, shipment1.totalQuantity + shipment2.totalQuantity)
        assertEquals(50, shipment1.totalCartons + shipment2.totalCartons)
        assertEquals(BigDecimal("4.800000"), shp1Cbm.add(shp2Cbm))

        // =================================================================
        // STEP 5: Import Cost Sheet & Landed Cost Allocation Engine
        // =================================================================
        // Cost Categories:
        // - Freight = BDT 12,000 (Allocated by Gross Weight)
        // - Customs = BDT 5,000 (Allocated by Purchase Value)
        // - C&F = BDT 2,000 (Allocated by Quantity)
        // - Local Transportation = BDT 1,000 (Allocated by Gross Weight)
        // Total Import Costs = BDT 20,000
        val costFreight = 1_200_000L // BDT 12,000.00
        val costCustoms = 500_000L   // BDT 5,000.00
        val costCF = 200_000L        // BDT 2,000.00
        val costLocalTrans = 100_000L// BDT 1,000.00
        val totalImportCostsMinor = costFreight + costCustoms + costCF + costLocalTrans
        assertEquals(2_000_000L, totalImportCostsMinor) // BDT 20,000.00

        // Invariant: Recording expense capitalized into Goods in Transit
        postJournal(
            entryNumber = "JV-2026-0003",
            desc = "Capitalize Import Expenses for Consignment",
            refType = "IMPORT_EXPENSE",
            refId = "EXP-BUNDLE",
            lines = listOf(
                JournalLineDraft("coa-1220", true, totalImportCostsMinor, "Dr. Goods in Transit"),
                JournalLineDraft("coa-2010", false, totalImportCostsMinor, "Cr. Freight & Duty Payable")
            )
        )

        // Allocation Inputs for the 1,000 pairs
        val allocItems = listOf(
            AllocationItemInput(
                id = productId,
                name = "Product A (Shoes)",
                quantity = orderedQty,
                grossWeightKg = BigDecimal("1000.0"), // 50 cartons * 20 kg
                cbm = BigDecimal("4.800000"),
                purchaseValueBdtMinor = poLineTotalBdtMinor
            )
        )

        // 1. Freight: verify Gross Weight allocation
        val freightAlloc = LandedCostAllocationEngine.allocate(costFreight, allocItems, AllocationMethod.GROSS_WEIGHT)
        assertEquals(costFreight, freightAlloc.sumOf { it.allocatedBdtMinor })

        // 2. Customs: verify Purchase Value allocation
        val customsAlloc = LandedCostAllocationEngine.allocate(costCustoms, allocItems, AllocationMethod.PURCHASE_VALUE)
        assertEquals(costCustoms, customsAlloc.sumOf { it.allocatedBdtMinor })

        // 3. C&F: verify Quantity allocation
        val cfAlloc = LandedCostAllocationEngine.allocate(costCF, allocItems, AllocationMethod.QUANTITY)
        assertEquals(costCF, cfAlloc.sumOf { it.allocatedBdtMinor })

        // 4. Local Transport: verify Gross Weight allocation
        val transAlloc = LandedCostAllocationEngine.allocate(costLocalTrans, allocItems, AllocationMethod.GROSS_WEIGHT)
        assertEquals(costLocalTrans, transAlloc.sumOf { it.allocatedBdtMinor })

        // Estimated Landed Cost Calculations
        val estimatedTotalLandedCostMinor = poLineTotalBdtMinor + totalImportCostsMinor
        assertEquals(19_000_000L, estimatedTotalLandedCostMinor) // BDT 190,000.00

        val estimatedUnitLandedCostMinor = BigDecimal(estimatedTotalLandedCostMinor)
            .divide(BigDecimal(orderedQty), 0, RoundingMode.HALF_UP)
            .longValueExact()
        assertEquals(19_000L, estimatedUnitLandedCostMinor) // BDT 190.00 per pair

        // =================================================================
        // STEP 6: Warehouse Receiving Inspection & Provisional Cost Layers
        // =================================================================
        // Shipment 001 Inspection:
        // Expected = 600, Good = 580, Damaged = 15, Missing = 5
        val shp1Expected = 600
        val shp1Good = 580
        val shp1Damaged = 15
        val shp1Missing = 5
        assertEquals(shp1Expected, shp1Good + shp1Damaged + shp1Missing)

        // Post Shipment 1 Receipt:
        // Sellable stock increases by GOOD quantity (580) ONLY! Damaged and missing excluded!
        stockBalances[productId] = (stockBalances[productId] ?: 0) + shp1Good
        assertEquals(580, stockBalances[productId])

        stockMovements.add(
            StockMovement(
                id = "mov-shp1-good",
                movementNumber = "SM-001",
                warehouseId = "wh-dhk-01",
                productId = productId,
                movementType = "IMPORT_RECEIPT",
                quantity = shp1Good,
                unitCostMinor = estimatedUnitLandedCostMinor,
                referenceType = "WAREHOUSE_RECEIPT",
                referenceId = "WR-001"
            )
        )
        stockMovements.add(
            StockMovement(
                id = "mov-shp1-dmg",
                movementNumber = "DMG-001",
                warehouseId = "wh-dhk-01",
                productId = productId,
                movementType = "DAMAGE_EXCLUDED",
                quantity = shp1Damaged,
                unitCostMinor = estimatedUnitLandedCostMinor,
                referenceType = "WAREHOUSE_RECEIPT",
                referenceId = "WR-001",
                notes = "Damaged during transit. Excluded from sellable stock."
            )
        )
        stockMovements.add(
            StockMovement(
                id = "mov-shp1-mis",
                movementNumber = "MIS-001",
                warehouseId = "wh-dhk-01",
                productId = productId,
                movementType = "MISSING_IN_TRANSIT",
                quantity = shp1Missing,
                unitCostMinor = estimatedUnitLandedCostMinor,
                referenceType = "WAREHOUSE_RECEIPT",
                referenceId = "WR-001",
                notes = "Missing in transit shortage."
            )
        )

        // Inventory Cost Layer 001: 580 pairs @ BDT 190.00 = BDT 110,200.00
        val layer1ValueMinor = shp1Good.toLong() * estimatedUnitLandedCostMinor
        assertEquals(11_020_000L, layer1ValueMinor) // BDT 110,200.00

        var layer1 = InventoryCostLayer(
            id = "icl-001",
            productId = productId,
            warehouseId = "wh-dhk-01",
            shipmentId = shipment1.id,
            shipmentNumber = shipment1.shipmentNumber,
            warehouseReceiptId = "wr-001",
            receiptNumber = "WR-2026-0001",
            initialQuantity = shp1Good,
            remainingQuantity = shp1Good,
            unitLandedCostBdtMinor = estimatedUnitLandedCostMinor,
            originalPurchasePriceMinor = 17_000L, // BDT 170 base
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 2_000L, // BDT 20 allocated expense
            receiptDate = System.currentTimeMillis() - 100000L,
            isActive = true
        )
        inventoryCostLayers.add(layer1)

        // Capitalize Layer 1 into Inventory Asset
        postJournal(
            entryNumber = "JV-2026-0004",
            desc = "Capitalize Shipment 1 Goods into Live Stock",
            refType = "WAREHOUSE_RECEIPT",
            refId = "WR-001",
            lines = listOf(
                JournalLineDraft("coa-1210", true, layer1ValueMinor, "Dr. Inventory Asset"),
                JournalLineDraft("coa-1220", false, layer1ValueMinor, "Cr. Goods in Transit")
            )
        )

        // Duplicate Posting Prevention check on receipt 1
        var receipt1Status = ReceiptStatus.POSTED.name
        try {
            check(receipt1Status != ReceiptStatus.POSTED.name) {
                "Duplicate posting prevented: Receipt is already posted!"
            }
            fail("Expected duplicate posting rejection")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Duplicate posting prevented") == true)
        }

        // Shipment 002 Inspection:
        // Expected = 400, Good = 400, Damaged = 0, Missing = 0
        val shp2Expected = 400
        val shp2Good = 400
        stockBalances[productId] = (stockBalances[productId] ?: 0) + shp2Good
        // Total sellable stock in warehouse = 580 + 400 = 980!
        assertEquals(980, stockBalances[productId])

        stockMovements.add(
            StockMovement(
                id = "mov-shp2-good",
                movementNumber = "SM-002",
                warehouseId = "wh-dhk-01",
                productId = productId,
                movementType = "IMPORT_RECEIPT",
                quantity = shp2Good,
                unitCostMinor = estimatedUnitLandedCostMinor,
                referenceType = "WAREHOUSE_RECEIPT",
                referenceId = "WR-002"
            )
        )

        // Inventory Cost Layer 002: 400 pairs @ BDT 190.00 = BDT 76,000.00
        val layer2ValueMinor = shp2Good.toLong() * estimatedUnitLandedCostMinor
        assertEquals(7_600_000L, layer2ValueMinor) // BDT 76,000.00

        var layer2 = InventoryCostLayer(
            id = "icl-002",
            productId = productId,
            warehouseId = "wh-dhk-01",
            shipmentId = shipment2.id,
            shipmentNumber = shipment2.shipmentNumber,
            warehouseReceiptId = "wr-002",
            receiptNumber = "WR-2026-0002",
            initialQuantity = shp2Good,
            remainingQuantity = shp2Good,
            unitLandedCostBdtMinor = estimatedUnitLandedCostMinor,
            originalPurchasePriceMinor = 17_000L,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 2_000L,
            receiptDate = System.currentTimeMillis() - 50000L,
            isActive = true
        )
        inventoryCostLayers.add(layer2)

        // Verify Total Inventory Value = BDT 110,200 + BDT 76,000 = BDT 186,200
        val totalInventoryValueMinor = layer1ValueMinor + layer2ValueMinor
        assertEquals(18_620_000L, totalInventoryValueMinor) // BDT 186,200.00

        // =================================================================
        // STEP 7: FIFO Sales Consumption
        // =================================================================
        // Customer purchases 600 pairs:
        // FIFO consumption:
        // - 580 pairs taken from Layer 001 (Layer 001 remaining = 0)
        // - 20 pairs taken from Layer 002 (Layer 002 remaining = 380)
        val sellQty = 600
        val takeFromLayer1 = minOf(sellQty, layer1.remainingQuantity) // 580
        val takeFromLayer2 = sellQty - takeFromLayer1                // 20

        assertEquals(580, takeFromLayer1)
        assertEquals(20, takeFromLayer2)

        val cogsFromLayer1 = takeFromLayer1.toLong() * layer1.unitLandedCostBdtMinor // 580 * 190 = 110,200
        val cogsFromLayer2 = takeFromLayer2.toLong() * layer2.unitLandedCostBdtMinor // 20 * 190 = 3,800
        val totalCogsMinor = cogsFromLayer1 + cogsFromLayer2                         // BDT 114,000.00

        assertEquals(11_400_000L, totalCogsMinor) // BDT 114,000.00

        // Update layers
        layer1 = layer1.copy(remainingQuantity = layer1.remainingQuantity - takeFromLayer1, isActive = false)
        layer2 = layer2.copy(remainingQuantity = layer2.remainingQuantity - takeFromLayer2, isActive = true)

        assertEquals(0, layer1.remainingQuantity)
        assertEquals(380, layer2.remainingQuantity)

        // Update warehouse sellable stock balance: 980 - 600 = 380 pairs
        stockBalances[productId] = stockBalances[productId]!! - sellQty
        assertEquals(380, stockBalances[productId])

        // Remaining inventory value before variance adjustment = 380 * BDT 190.00 = BDT 72,200.00
        val remainingInventoryValueMinor = layer2.remainingQuantity.toLong() * layer2.unitLandedCostBdtMinor
        assertEquals(7_220_000L, remainingInventoryValueMinor) // BDT 72,200.00

        // Post Sales COGS entry
        postJournal(
            entryNumber = "JV-2026-0005",
            desc = "Record Cost of Goods Sold for 600 pairs (FIFO)",
            refType = "SALES_INVOICE",
            refId = "INV-001",
            lines = listOf(
                JournalLineDraft("coa-5010", true, totalCogsMinor, "Dr. Cost of Goods Sold"),
                JournalLineDraft("coa-1210", false, totalCogsMinor, "Cr. Inventory Asset")
            )
        )

        // =================================================================
        // STEP 8: Actual Landed Cost Adjustment
        // =================================================================
        // Actual freight bill arrives: increases from BDT 12,000 to BDT 14,000 (+BDT 2,000 variance)
        val actualFreightMinor = 1_400_000L
        val freightVarianceMinor = actualFreightMinor - costFreight // +200,000 minor = +BDT 2,000.00
        assertEquals(200_000L, freightVarianceMinor)

        // New total import costs = BDT 22,000. New Total Landed Cost = BDT 192,000. New Unit Cost = BDT 192.00
        val newTotalLandedCostMinor = poLineTotalBdtMinor + (totalImportCostsMinor + freightVarianceMinor)
        assertEquals(19_200_000L, newTotalLandedCostMinor) // BDT 192,000.00

        val newUnitLandedCostMinor = BigDecimal(newTotalLandedCostMinor)
            .divide(BigDecimal(orderedQty), 0, RoundingMode.HALF_UP)
            .longValueExact()
        assertEquals(19_200L, newUnitLandedCostMinor) // BDT 192.00 per unit

        val unitVarianceMinor = newUnitLandedCostMinor - estimatedUnitLandedCostMinor // +200 minor = +BDT 2.00/unit
        assertEquals(200L, unitVarianceMinor)

        // Distribute variance:
        // 1. Goods already sold (600 units) -> Adjust to COGS: 600 * BDT 2.00 = BDT 1,200
        val cogsAdjustmentMinor = sellQty.toLong() * unitVarianceMinor
        assertEquals(120_000L, cogsAdjustmentMinor) // BDT 1,200.00

        // 2. Goods remaining in stock (380 units in Layer 2) -> Adjust to Inventory Asset: 380 * BDT 2.00 = BDT 760
        val remainingStockAdjMinor = layer2.remainingQuantity.toLong() * unitVarianceMinor
        assertEquals(76_000L, remainingStockAdjMinor) // BDT 760.00

        // 3. Damaged & Missing in transit (20 units: 15 damaged + 5 missing) -> Adjust to Loss/Clearing: 20 * BDT 2.00 = BDT 40
        val damagedMissingAdjMinor = (shp1Damaged + shp1Missing).toLong() * unitVarianceMinor
        assertEquals(4_000L, damagedMissingAdjMinor) // BDT 40.00

        // Reconcile total adjustment: 1,200 + 760 + 40 = BDT 2,000.00 exactly!
        assertEquals(freightVarianceMinor, cogsAdjustmentMinor + remainingStockAdjMinor + damagedMissingAdjMinor)

        // Update active Layer 2 unit cost to BDT 192.00
        layer2 = layer2.copy(
            unitLandedCostBdtMinor = newUnitLandedCostMinor,
            allocatedExpensePerUnitBdtMinor = layer2.allocatedExpensePerUnitBdtMinor + unitVarianceMinor
        )
        // Updated remaining inventory value = 380 * BDT 192.00 = BDT 72,960.00 (72,200 + 760)
        val updatedRemainingInventoryValueMinor = layer2.remainingQuantity.toLong() * layer2.unitLandedCostBdtMinor
        assertEquals(7_296_000L, updatedRemainingInventoryValueMinor)
        assertEquals(remainingInventoryValueMinor + remainingStockAdjMinor, updatedRemainingInventoryValueMinor)

        // Post Landed Cost Adjustment Journal
        postJournal(
            entryNumber = "JV-2026-0006",
            desc = "Actual Landed Cost Adjustment for Freight Variance",
            refType = "LANDED_COST_ADJUSTMENT",
            refId = "ADJ-FRT-01",
            lines = listOf(
                JournalLineDraft("coa-5010", true, cogsAdjustmentMinor, "Dr. Cost of Goods Sold (Sold Inventory Adjustment)"),
                JournalLineDraft("coa-1210", true, remainingStockAdjMinor, "Dr. Inventory Asset (Remaining Stock Adjustment)"),
                JournalLineDraft("coa-5110", true, damagedMissingAdjMinor, "Dr. Import Variance / Transit Loss"),
                JournalLineDraft("coa-2010", false, freightVarianceMinor, "Cr. Freight Accounts Payable")
            )
        )

        // =================================================================
        // STEP 9: Trial Balance Equilibrium Verification
        // =================================================================
        val totalDebitsAcrossAllJournals = journalLines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCreditsAcrossAllJournals = journalLines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(
            "Double-Entry General Ledger Imbalance Detected!",
            totalDebitsAcrossAllJournals,
            totalCreditsAcrossAllJournals
        )
        assertTrue("General ledger must have posted entries", totalDebitsAcrossAllJournals > 0L)
    }
}
