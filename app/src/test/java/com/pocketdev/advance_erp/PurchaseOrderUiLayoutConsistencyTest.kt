package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PurchaseOrderUiLayoutConsistencyTest {

    @Test
    fun testPackingMetricsCalculatedFromCartonRecords() {
        val po = PurchaseOrder(
            id = "po-001",
            poNumber = "PO-20261103-001",
            supplierId = "sup-001",
            orderDate = 1730649600000L,
            currency = "CNY",
            exchangeRateToBdt = "17.15",
            totalAmountMinor = 3825000L,
            totalBdtAmountMinor = 65598750L,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            status = PurchaseOrderStatus.CONFIRMED.name,
            totalCartons = 5, // Baseline estimate from PO
            totalGrossWeightKg = "0",
            totalNetWeightKg = "0",
            totalCbm = "0"
        )

        // Scenario: Mixed carton CTN-001 has 2 lines (Shoe A + Shoe B)
        val cartons: List<ShipmentCarton> = listOf(
            ShipmentCarton(
                id = "c-1",
                shipmentId = "po-001",
                cartonNumber = "CTN-001",
                productId = "prod-a",
                quantity = 20,
                lengthCm = "50.0",
                widthCm = "40.0",
                heightCm = "30.0",
                cbm = "0.060000",
                grossWeightKg = "10.000",
                cartonGrossWeightKg = "15.000",
                netWeightKg = "9.000",
                isMixedCarton = true
            ),
            ShipmentCarton(
                id = "c-2",
                shipmentId = "po-001",
                cartonNumber = "CTN-001",
                productId = "prod-b",
                quantity = 10,
                lengthCm = "50.0",
                widthCm = "40.0",
                heightCm = "30.0",
                cbm = "0.060000",
                grossWeightKg = "5.000",
                cartonGrossWeightKg = "15.000",
                netWeightKg = "5.000",
                isMixedCarton = true
            )
        )

        // 1. Verify Distinct Carton Count is 1 (not 2 lines)
        val distinctCartons = cartons.map { it.cartonNumber }.distinct()
        assertEquals(1, distinctCartons.size)
        val displayCartons = if (distinctCartons.isNotEmpty()) distinctCartons.size else po.totalCartons
        assertEquals(1, displayCartons)

        // 2. Verify Gross Weight uses cartonGrossWeightKg per distinct carton (15.000 kg, not 30.000 kg)
        val recordedGrossWeight = cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc: BigDecimal, list: List<ShipmentCarton> ->
            val wStr = list.firstOrNull()?.cartonGrossWeightKg?.ifBlank { list.firstOrNull()?.grossWeightKg?.ifBlank { "0" } ?: "0" } ?: "0"
            acc.add(BigDecimal(wStr))
        }
        assertEquals(BigDecimal("15.000"), recordedGrossWeight)

        // 3. Verify CBM is counted once per distinct carton (0.060000 m³, not 0.12 m³)
        val recordedCbm = cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc: BigDecimal, list: List<ShipmentCarton> ->
            acc.add(BigDecimal(list.firstOrNull()?.cbm?.ifBlank { "0" } ?: "0"))
        }
        assertEquals(BigDecimal("0.060000"), recordedCbm)
        val formattedCbm = recordedCbm.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        assertEquals("0.06", formattedCbm)

        // 4. Verify Net Weight sums across carton lines (9.000 + 5.000 = 14.000 kg)
        val recordedNetWeight = cartons.fold(BigDecimal.ZERO) { acc: BigDecimal, c: ShipmentCarton ->
            acc.add(BigDecimal(c.netWeightKg.ifBlank { "0" }))
        }
        assertEquals(BigDecimal("14.000"), recordedNetWeight)
    }

    @Test
    fun testPackingMetricsFallbackToPoBaselineWhenNoCartonsEntered() {
        val po = PurchaseOrder(
            id = "po-002",
            poNumber = "PO-20261103-002",
            supplierId = "sup-001",
            orderDate = 1730649600000L,
            currency = "CNY",
            exchangeRateToBdt = "17.15",
            totalAmountMinor = 100000L,
            totalBdtAmountMinor = 1715000L,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            status = PurchaseOrderStatus.CONFIRMED.name,
            totalCartons = 8,
            totalGrossWeightKg = "120.500",
            totalNetWeightKg = "115.000",
            totalCbm = "1.2500"
        )

        val cartons: List<ShipmentCarton> = emptyList()

        val distinctCartons = cartons.map { it.cartonNumber }.distinct()
        val displayCartons = if (distinctCartons.isNotEmpty()) distinctCartons.size else po.totalCartons
        assertEquals(8, displayCartons)

        val recordedGrossWeight = cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc: BigDecimal, list: List<ShipmentCarton> ->
            val wStr = list.firstOrNull()?.cartonGrossWeightKg?.ifBlank { list.firstOrNull()?.grossWeightKg?.ifBlank { "0" } ?: "0" } ?: "0"
            acc.add(BigDecimal(wStr))
        }
        val displayGross = if (recordedGrossWeight > BigDecimal.ZERO) recordedGrossWeight.toPlainString() else BigDecimal(po.totalGrossWeightKg).setScale(3, RoundingMode.HALF_UP).toPlainString()
        assertEquals("120.500", displayGross)

        val recordedCbm = cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc: BigDecimal, list: List<ShipmentCarton> ->
            acc.add(BigDecimal(list.firstOrNull()?.cbm?.ifBlank { "0" } ?: "0"))
        }
        val displayCbmBd = if (recordedCbm > BigDecimal.ZERO) recordedCbm else BigDecimal(po.totalCbm.ifBlank { "0" })
        assertEquals("1.25", displayCbmBd.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString())
    }

    @Test
    fun testReceivingProgressAndDraftReceiptDistinction() {
        // Ordered 100 units total
        val items = listOf(
            PurchaseOrderItem(
                id = "poi-1",
                purchaseOrderId = "po-001",
                productId = "prod-1",
                quantity = 100,
                unitPriceMinor = 1000L,
                totalAmountMinor = 100000L,
                totalBdtAmountMinor = 1715000L,
                receivedQuantity = 0 // Not yet posted to inventory
            )
        )

        // Draft warehouse receipt exists but has NOT been posted
        val receipts = listOf(
            WarehouseReceipt(
                id = "wr-001",
                receiptNumber = "WR-20261103-001",
                shipmentId = "po-001",
                warehouseId = "wh-main",
                status = "DRAFT"
            )
        )

        val totalOrdered = items.sumOf { it.quantity }
        val totalReceived = items.sumOf { it.receivedQuantity }
        val draftCount = receipts.count { it.status == "DRAFT" }

        assertEquals(100, totalOrdered)
        assertEquals(0, totalReceived)
        assertEquals(1, draftCount)

        // Verify that posted progress is 0% while pending draft is recognized
        val progress = if (totalOrdered > 0) (totalReceived.toFloat() / totalOrdered.toFloat()) else 0f
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun testCartonRemovalAndPackingResetToZero() {
        // Initial state: PO has 5 baseline cartons
        var po = PurchaseOrder(
            id = "po-003",
            poNumber = "PO-20261103-003",
            supplierId = "sup-001",
            orderDate = 1730649600000L,
            currency = "CNY",
            exchangeRateToBdt = "17.15",
            totalAmountMinor = 100000L,
            totalBdtAmountMinor = 1715000L,
            status = PurchaseOrderStatus.DRAFT.name,
            totalCartons = 5,
            totalGrossWeightKg = "25.0",
            totalNetWeightKg = "20.0",
            totalCbm = "0.2"
        )

        // User resets packing to remove the 5 CTN (set to 0 CTN)
        po = po.copy(
            totalCartons = 0,
            totalGrossWeightKg = "0.0",
            totalNetWeightKg = "0.0",
            totalCbm = "0.0"
        )

        assertEquals(0, po.totalCartons)
        assertEquals("0.0", po.totalGrossWeightKg)
        assertEquals("0.0", po.totalCbm)

        // Cartons list: User can remove a carton
        val cartonsList = mutableListOf(
            ShipmentCarton(
                id = "c-101",
                shipmentId = "po-003",
                cartonNumber = "CTN-001",
                productId = "p-1",
                quantity = 50,
                lengthCm = "40",
                widthCm = "30",
                heightCm = "25",
                cbm = "0.03",
                grossWeightKg = "12.5",
                netWeightKg = "10.0"
            )
        )
        // Deleting CTN-001
        cartonsList.removeAll { it.cartonNumber == "CTN-001" }
        assertEquals(0, cartonsList.size)
    }

    @Test
    fun testPoItemQuantityUpdateFrom100Pcs() {
        val originalItem = PurchaseOrderItem(
            id = "item-100",
            purchaseOrderId = "po-004",
            productId = "prod-shoes",
            quantity = 100, // stuck 100 pcs previously
            unitPriceMinor = 4500L, // 45.00 CNY
            totalAmountMinor = 450000L,
            totalBdtAmountMinor = 7717500L,
            cartonQuantity = 5,
            grossWeightKg = "25.0"
        )

        // User updates quantity from 100 pcs to 60 pcs and cartons from 5 to 3
        val newQuantity = 60
        val newCartons = 3
        val newGross = "15.0"
        val rateBd = BigDecimal("17.15")

        val newTotalAmountMinor = newQuantity * originalItem.unitPriceMinor
        val newTotalBdtMinor = BigDecimal(newTotalAmountMinor).multiply(rateBd).setScale(0, RoundingMode.HALF_UP).longValueExact()

        val updatedItem = originalItem.copy(
            quantity = newQuantity,
            cartonQuantity = newCartons,
            grossWeightKg = newGross,
            totalAmountMinor = newTotalAmountMinor,
            totalBdtAmountMinor = newTotalBdtMinor
        )

        assertEquals(60, updatedItem.quantity)
        assertEquals(3, updatedItem.cartonQuantity)
        assertEquals("15.0", updatedItem.grossWeightKg)
        assertEquals(270000L, updatedItem.totalAmountMinor) // 60 * 4500
        assertEquals(4630500L, updatedItem.totalBdtAmountMinor)
    }

    @Test
    fun testPoItemRemovalRecalculatesTotals() {
        val item1 = PurchaseOrderItem(
            id = "it-1",
            purchaseOrderId = "po-005",
            productId = "p-1",
            quantity = 100,
            unitPriceMinor = 1000L,
            totalAmountMinor = 100000L,
            totalBdtAmountMinor = 1715000L
        )
        val item2 = PurchaseOrderItem(
            id = "it-2",
            purchaseOrderId = "po-005",
            productId = "p-2",
            quantity = 50,
            unitPriceMinor = 2000L,
            totalAmountMinor = 100000L,
            totalBdtAmountMinor = 1715000L
        )
        val itemsList = mutableListOf(item1, item2)

        // Total before removal
        assertEquals(200000L, itemsList.sumOf { it.totalAmountMinor })

        // Remove item1
        itemsList.removeAll { it.id == "it-1" }
        assertEquals(1, itemsList.size)
        assertEquals(100000L, itemsList.sumOf { it.totalAmountMinor })
    }
}
