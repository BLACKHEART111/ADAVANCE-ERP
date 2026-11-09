package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.PoRemainingItem
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * UNIT TESTS — SHIPMENT CREATION WORKFLOW & MULTI-SHIPMENT VALIDATION
 * Covers:
 * 1. Shipment creation from PO details (preselected PO)
 * 2. Shipment creation with manual PO selection
 * 3. Partial shipment remaining quantity calculation
 * 4. Over-shipment rejection against remaining unshipped quantities
 * 5. Cancelled shipment quantity restoration
 * 6. Carton draft validation (dimensions, positive qty, weights, CBM calculation)
 */
class ShipmentCreationWorkflowTest {

    private val purchaseOrders = mutableMapOf<String, PurchaseOrder>()
    private val purchaseOrderItems = mutableMapOf<String, MutableList<PurchaseOrderItem>>()
    private val shipments = mutableMapOf<String, ImportShipment>()
    private val cartons = mutableMapOf<String, MutableList<ShipmentCarton>>()
    private val products = mutableMapOf<String, Product>()
    private val suppliers = mutableMapOf<String, Supplier>()

    @Before
    fun setUp() {
        purchaseOrders.clear()
        purchaseOrderItems.clear()
        shipments.clear()
        cartons.clear()
        products.clear()
        suppliers.clear()

        // Setup test supplier
        val sup = Supplier(
            id = "sup-shenzhen-001",
            supplierCode = "SUP-SZ-001",
            companyName = "Shenzhen Electronics Tech Co., Ltd.",
            contactPerson = "Chen Wei",
            phone = "+86 755 8888 9999",
            address = "Bao'an District, Shenzhen, China",
            country = "China"
        )
        suppliers[sup.id] = sup

        // Setup test product
        val prod = Product(
            id = "prod-earbuds-001",
            categoryId = "cat-electronics",
            nameEn = "Wireless Bluetooth Earbuds Pro",
            nameBn = "ওয়্যারলেস ব্লুটুথ ইয়ারবাডস প্রো",
            baseSku = "WBE-PRO-01",
            unit = "pcs",
            purchasePriceMinor = 1500L,
            sellingPriceMinor = 3500L
        )
        products[prod.id] = prod
    }

    // Helper: calculate remaining items for PO matching ImportRepository logic
    private fun getPoRemainingItems(orderId: String): List<PoRemainingItem> {
        val items = purchaseOrderItems[orderId] ?: emptyList()
        val orderShipments = shipments.values
            .filter { it.purchaseOrderId == orderId && it.status != ShipmentStatus.CANCELLED.name }
        val allCartons = orderShipments.flatMap { cartons[it.id] ?: emptyList() }
        val shippedByProduct = allCartons.groupBy { it.productId to it.variantId }
            .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

        return items.map { item ->
            val shipped = shippedByProduct[item.productId to item.variantId] ?: 0
            val rem = (item.quantity - shipped).coerceAtLeast(0)
            PoRemainingItem(
                poItem = item,
                product = products[item.productId],
                orderedQty = item.quantity,
                shippedQty = shipped,
                remainingQty = rem
            )
        }
    }

    // Helper: create shipment matching ImportRepository.createShipment logic
    private fun createShipment(
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartonDrafts: List<ShipmentCartonDraft>,
        notes: String?
    ): String {
        require(cartonDrafts.isNotEmpty()) { "Shipment must contain at least 1 carton" }
        require(!trackingRef.isNullOrBlank()) { "Tracking reference must not be blank" }

        if (orderId != null) {
            val order = purchaseOrders[orderId] ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            val orderItems = purchaseOrderItems[orderId] ?: emptyList()
            val existingShipments = shipments.values
                .filter { it.purchaseOrderId == orderId && it.status != ShipmentStatus.CANCELLED.name }
            val existingCartons = existingShipments.flatMap { cartons[it.id] ?: emptyList() }

            val alreadyShippedByProduct = existingCartons.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }
            val newShippedByProduct = cartonDrafts.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

            newShippedByProduct.forEach { (key, newQty) ->
                val matchingItem = orderItems.find { it.productId == key.first && it.variantId == key.second }
                val orderedQty = matchingItem?.quantity ?: 0
                require(orderedQty > 0) {
                    val prodName = products[key.first]?.nameEn ?: key.first
                    "Product $prodName is not part of Purchase Order ${order.poNumber}"
                }
                val prevShipped = alreadyShippedByProduct[key] ?: 0
                val remainingUnshipped = (orderedQty - prevShipped).coerceAtLeast(0)
                require(newQty <= remainingUnshipped) {
                    val prodName = products[key.first]?.nameEn ?: key.first
                    "Shipment quantity ($newQty) exceeds remaining unshipped quantity ($remainingUnshipped) for product $prodName"
                }
            }
        }

        val shipmentId = "shp-${UUID.randomUUID()}"
        val shipmentNumber = "SHP-2026-%04d".format(shipments.size + 1)
        var totalQty = 0
        var totalGrossWeight = BigDecimal.ZERO
        var totalNetWeight = BigDecimal.ZERO
        var totalCbm = BigDecimal.ZERO

        val cartonEntities = cartonDrafts.mapIndexed { idx, draft ->
            require(draft.quantity > 0) { "Carton quantity must be positive" }
            require(draft.lengthCm > BigDecimal.ZERO && draft.widthCm > BigDecimal.ZERO && draft.heightCm > BigDecimal.ZERO) {
                "Carton dimensions must be greater than zero"
            }
            require(draft.grossWeightKg >= BigDecimal.ZERO && draft.netWeightKg >= BigDecimal.ZERO) {
                "Weights cannot be negative"
            }

            val cbm = CbmCalculator.calculateCbm(draft.lengthCm, draft.widthCm, draft.heightCm, 1)
            totalQty += draft.quantity
            totalGrossWeight = totalGrossWeight.add(draft.grossWeightKg)
            totalNetWeight = totalNetWeight.add(draft.netWeightKg)
            totalCbm = totalCbm.add(cbm)

            ShipmentCarton(
                id = "ctn-${UUID.randomUUID()}",
                shipmentId = shipmentId,
                cartonNumber = draft.cartonNumber,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                lengthCm = draft.lengthCm.toPlainString(),
                widthCm = draft.widthCm.toPlainString(),
                heightCm = draft.heightCm.toPlainString(),
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = cbm.toPlainString()
            )
        }

        val shipment = ImportShipment(
            id = shipmentId,
            shipmentNumber = shipmentNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = method.name,
            origin = origin,
            destination = destination,
            trackingReference = trackingRef,
            shippingLine = shippingLine,
            shippingDate = System.currentTimeMillis(),
            estimatedArrivalDate = System.currentTimeMillis() + 5 * 24 * 3600 * 1000L,
            status = ShipmentStatus.IN_TRANSIT.name,
            totalCartons = cartonDrafts.size,
            totalQuantity = totalQty,
            totalGrossWeightKg = totalGrossWeight.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            totalNetWeightKg = totalNetWeight.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            totalCbm = totalCbm.setScale(4, RoundingMode.HALF_UP).toPlainString(),
            notes = notes
        )

        shipments[shipmentId] = shipment
        cartons[shipmentId] = cartonEntities.toMutableList()

        if (orderId != null) {
            val existing = purchaseOrders[orderId]!!
            purchaseOrders[orderId] = existing.copy(status = PurchaseOrderStatus.SHIPPED.name)
        }

        return shipmentId
    }

    private fun cancelShipment(shipmentId: String) {
        val shp = shipments[shipmentId] ?: return
        shipments[shipmentId] = shp.copy(status = ShipmentStatus.CANCELLED.name)

        val orderId = shp.purchaseOrderId
        if (orderId != null) {
            val activeShipments = shipments.values
                .filter { it.purchaseOrderId == orderId && it.id != shipmentId && it.status != ShipmentStatus.CANCELLED.name }
            if (activeShipments.isEmpty()) {
                val ord = purchaseOrders[orderId]!!
                purchaseOrders[orderId] = ord.copy(status = PurchaseOrderStatus.CONFIRMED.name)
            }
        }
    }

    @Test
    fun testShipmentCreationFromPoDetailsWithRemainingQuantityCalculation() {
        // Setup Confirmed PO with 1,000 units
        val poId = "po-2026-0001"
        val po = PurchaseOrder(
            id = poId,
            poNumber = "PO-2026-0001",
            supplierId = "sup-shenzhen-001",
            status = PurchaseOrderStatus.CONFIRMED.name,
            totalAmountMinor = 1500000L,
            totalBdtAmountMinor = 25500000L
        )
        purchaseOrders[poId] = po
        purchaseOrderItems[poId] = mutableListOf(
            PurchaseOrderItem(
                id = "poi-1",
                purchaseOrderId = poId,
                productId = "prod-earbuds-001",
                quantity = 1000,
                unitPriceMinor = 1500L,
                totalAmountMinor = 1500000L,
                totalBdtAmountMinor = 25500000L
            )
        )

        // 1. Initial remaining check
        val initialRem = getPoRemainingItems(poId)
        assertEquals(1, initialRem.size)
        assertEquals(1000, initialRem[0].orderedQty)
        assertEquals(0, initialRem[0].shippedQty)
        assertEquals(1000, initialRem[0].remainingQty)

        // 2. Create first shipment of 600 units across 2 cartons (300 each)
        val cartonsDraft = listOf(
            ShipmentCartonDraft(
                cartonNumber = "CTN-01/02",
                productId = "prod-earbuds-001",
                quantity = 300,
                lengthCm = BigDecimal("60"),
                widthCm = BigDecimal("40"),
                heightCm = BigDecimal("40"),
                grossWeightKg = BigDecimal("20.0"),
                netWeightKg = BigDecimal("18.0")
            ),
            ShipmentCartonDraft(
                cartonNumber = "CTN-02/02",
                productId = "prod-earbuds-001",
                quantity = 300,
                lengthCm = BigDecimal("60"),
                widthCm = BigDecimal("40"),
                heightCm = BigDecimal("40"),
                grossWeightKg = BigDecimal("20.0"),
                netWeightKg = BigDecimal("18.0")
            )
        )

        val shipmentId = createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = poId,
            method = ShippingMethod.AIR,
            origin = "Shenzhen Bao'an Airport (SZX)",
            destination = "Dhaka Hazrat Shahjalal (DAC)",
            trackingRef = "AWB-784-99281726",
            shippingLine = "China Southern Air Cargo",
            cartonDrafts = cartonsDraft,
            notes = "First consignment - 600 pcs"
        )

        assertNotNull(shipmentId)
        val createdShipment = shipments[shipmentId]
        assertNotNull(createdShipment)
        assertEquals("SHP-2026-0001", createdShipment!!.shipmentNumber)
        assertEquals(2, createdShipment.totalCartons)
        assertEquals(600, createdShipment.totalQuantity)
        assertEquals("40.00", createdShipment.totalGrossWeightKg)
        assertEquals(ShipmentStatus.IN_TRANSIT.name, createdShipment.status)

        // Check CBM calculation (2 cartons of 60x40x40 cm = 0.096 m3 each -> 0.1920 m3 total)
        assertEquals("0.1920", createdShipment.totalCbm)

        // Verify remaining quantity is now 400
        val afterFirstShipment = getPoRemainingItems(poId)
        assertEquals(1, afterFirstShipment.size)
        assertEquals(1000, afterFirstShipment[0].orderedQty)
        assertEquals(600, afterFirstShipment[0].shippedQty)
        assertEquals(400, afterFirstShipment[0].remainingQty)
    }

    @Test
    fun testShipmentCreationWithManualPoSelection() {
        // Setup 2 POs
        val po1 = PurchaseOrder(
            id = "po-1",
            poNumber = "PO-2026-0001",
            supplierId = "sup-shenzhen-001",
            status = PurchaseOrderStatus.CONFIRMED.name,
            totalAmountMinor = 750000L,
            totalBdtAmountMinor = 12750000L
        )
        val po2 = PurchaseOrder(
            id = "po-2",
            poNumber = "PO-2026-0002",
            supplierId = "sup-shenzhen-001",
            status = PurchaseOrderStatus.CONFIRMED.name,
            totalAmountMinor = 1200000L,
            totalBdtAmountMinor = 20400000L
        )
        purchaseOrders[po1.id] = po1
        purchaseOrders[po2.id] = po2

        purchaseOrderItems[po1.id] = mutableListOf(
            PurchaseOrderItem(
                id = "item-1",
                purchaseOrderId = po1.id,
                productId = "prod-earbuds-001",
                quantity = 500,
                unitPriceMinor = 1500L,
                totalAmountMinor = 750000L,
                totalBdtAmountMinor = 12750000L
            )
        )
        purchaseOrderItems[po2.id] = mutableListOf(
            PurchaseOrderItem(
                id = "item-2",
                purchaseOrderId = po2.id,
                productId = "prod-earbuds-001",
                quantity = 800,
                unitPriceMinor = 1500L,
                totalAmountMinor = 1200000L,
                totalBdtAmountMinor = 20400000L
            )
        )

        // Manually select PO-2 and ship 800 units
        val cartonDraft = listOf(
            ShipmentCartonDraft(
                cartonNumber = "CTN-01/01",
                productId = "prod-earbuds-001",
                quantity = 800,
                lengthCm = BigDecimal("80"),
                widthCm = BigDecimal("60"),
                heightCm = BigDecimal("50"),
                grossWeightKg = BigDecimal("45.0"),
                netWeightKg = BigDecimal("40.5")
            )
        )

        val shipmentId = createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = po2.id,
            method = ShippingMethod.SEA,
            origin = "Shenzhen Yantian Port, China",
            destination = "Chattogram Port, Bangladesh",
            trackingRef = "CONT-COSU662910",
            shippingLine = "COSCO Shipping Lines",
            cartonDrafts = cartonDraft,
            notes = "Full container load for PO-2"
        )

        val shipment = shipments[shipmentId]!!
        assertEquals(po2.id, shipment.purchaseOrderId)
        assertEquals(800, shipment.totalQuantity)
        assertEquals(PurchaseOrderStatus.SHIPPED.name, purchaseOrders[po2.id]?.status)

        // PO-1 must remain untouched with 500 remaining
        assertEquals(500, getPoRemainingItems(po1.id)[0].remainingQty)
        // PO-2 must now have 0 remaining
        assertEquals(0, getPoRemainingItems(po2.id)[0].remainingQty)
    }

    @Test
    fun testMultiShipmentRemainingQuantityValidation() {
        val poId = "po-multi-01"
        purchaseOrders[poId] = PurchaseOrder(
            id = poId,
            poNumber = "PO-MULTI-01",
            supplierId = "sup-shenzhen-001",
            status = "CONFIRMED",
            totalAmountMinor = 1500000L,
            totalBdtAmountMinor = 25500000L
        )
        purchaseOrderItems[poId] = mutableListOf(
            PurchaseOrderItem(
                id = "poi-m",
                purchaseOrderId = poId,
                productId = "prod-earbuds-001",
                quantity = 1000,
                unitPriceMinor = 1500L,
                totalAmountMinor = 1500000L,
                totalBdtAmountMinor = 25500000L
            )
        )

        // 1. Shipment 1: 600 units
        createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = poId,
            method = ShippingMethod.AIR,
            origin = "SZX",
            destination = "DAC",
            trackingRef = "AWB-111",
            shippingLine = "Airline 1",
            cartonDrafts = listOf(
                ShipmentCartonDraft(
                    cartonNumber = "CTN-1",
                    productId = "prod-earbuds-001",
                    quantity = 600,
                    lengthCm = BigDecimal("50"),
                    widthCm = BigDecimal("50"),
                    heightCm = BigDecimal("50"),
                    grossWeightKg = BigDecimal("30"),
                    netWeightKg = BigDecimal("27")
                )
            ),
            notes = "Shipment 1"
        )

        // Remaining is 400
        assertEquals(400, getPoRemainingItems(poId)[0].remainingQty)

        // 2. Attempt to create Shipment 2 with 500 units (exceeds remaining 400)
        try {
            createShipment(
                supplierId = "sup-shenzhen-001",
                orderId = poId,
                method = ShippingMethod.AIR,
                origin = "SZX",
                destination = "DAC",
                trackingRef = "AWB-222",
                shippingLine = "Airline 2",
                cartonDrafts = listOf(
                    ShipmentCartonDraft(
                        cartonNumber = "CTN-2",
                        productId = "prod-earbuds-001",
                        quantity = 500,
                        lengthCm = BigDecimal("50"),
                        widthCm = BigDecimal("50"),
                        heightCm = BigDecimal("50"),
                        grossWeightKg = BigDecimal("25"),
                        netWeightKg = BigDecimal("22.5")
                    )
                ),
                notes = "Shipment 2 invalid"
            )
            fail("Expected over-shipment exception was not thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("exceeds remaining unshipped quantity (400)"))
        }

        // 3. Create Shipment 2 with exactly 400 units -> succeeds
        val shipmentId2 = createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = poId,
            method = ShippingMethod.AIR,
            origin = "SZX",
            destination = "DAC",
            trackingRef = "AWB-222",
            shippingLine = "Airline 2",
            cartonDrafts = listOf(
                ShipmentCartonDraft(
                    cartonNumber = "CTN-2",
                    productId = "prod-earbuds-001",
                    quantity = 400,
                    lengthCm = BigDecimal("50"),
                    widthCm = BigDecimal("50"),
                    heightCm = BigDecimal("50"),
                    grossWeightKg = BigDecimal("20"),
                    netWeightKg = BigDecimal("18")
                )
            ),
            notes = "Shipment 2 final"
        )
        assertNotNull(shipmentId2)

        // Total shipped is 1000, remaining is 0
        val finalRem = getPoRemainingItems(poId)[0]
        assertEquals(1000, finalRem.shippedQty)
        assertEquals(0, finalRem.remainingQty)
    }

    @Test
    fun testOverShipmentRejectedWhenProductNotInPo() {
        val poId = "po-prod-check"
        purchaseOrders[poId] = PurchaseOrder(
            id = poId,
            poNumber = "PO-CHECK",
            supplierId = "sup-shenzhen-001",
            totalAmountMinor = 150000L,
            totalBdtAmountMinor = 2550000L
        )
        purchaseOrderItems[poId] = mutableListOf(
            PurchaseOrderItem(
                id = "poi-1",
                purchaseOrderId = poId,
                productId = "prod-earbuds-001",
                quantity = 100,
                unitPriceMinor = 1500L,
                totalAmountMinor = 150000L,
                totalBdtAmountMinor = 2550000L
            )
        )

        // Setup an unrelated product
        val otherProd = Product(
            id = "prod-unrelated",
            categoryId = "cat-misc",
            nameEn = "USB Cable",
            nameBn = "তার",
            baseSku = "USB-01",
            purchasePriceMinor = 100L,
            sellingPriceMinor = 300L
        )
        products[otherProd.id] = otherProd

        try {
            createShipment(
                supplierId = "sup-shenzhen-001",
                orderId = poId,
                method = ShippingMethod.AIR,
                origin = "SZX",
                destination = "DAC",
                trackingRef = "AWB-333",
                shippingLine = "Airline",
                cartonDrafts = listOf(
                    ShipmentCartonDraft(
                        cartonNumber = "CTN-1",
                        productId = "prod-unrelated",
                        quantity = 50,
                        lengthCm = BigDecimal("30"),
                        widthCm = BigDecimal("20"),
                        heightCm = BigDecimal("10"),
                        grossWeightKg = BigDecimal("5"),
                        netWeightKg = BigDecimal("4.5")
                    )
                ),
                notes = "Invalid product"
            )
            fail("Expected exception for unlinked product was not thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("is not part of Purchase Order"))
        }
    }

    @Test
    fun testCancelledShipmentRestoresAvailableQuantity() {
        val poId = "po-cancel-test"
        purchaseOrders[poId] = PurchaseOrder(
            id = poId,
            poNumber = "PO-CANCEL-01",
            supplierId = "sup-shenzhen-001",
            status = "CONFIRMED",
            totalAmountMinor = 1500000L,
            totalBdtAmountMinor = 25500000L
        )
        purchaseOrderItems[poId] = mutableListOf(
            PurchaseOrderItem(
                id = "poi-c",
                purchaseOrderId = poId,
                productId = "prod-earbuds-001",
                quantity = 1000,
                unitPriceMinor = 1500L,
                totalAmountMinor = 1500000L,
                totalBdtAmountMinor = 25500000L
            )
        )

        // 1. Create shipment 1 of 600 units
        val shp1Id = createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = poId,
            method = ShippingMethod.AIR,
            origin = "SZX",
            destination = "DAC",
            trackingRef = "AWB-444",
            shippingLine = "Airline",
            cartonDrafts = listOf(
                ShipmentCartonDraft(
                    cartonNumber = "CTN-1",
                    productId = "prod-earbuds-001",
                    quantity = 600,
                    lengthCm = BigDecimal("50"),
                    widthCm = BigDecimal("50"),
                    heightCm = BigDecimal("50"),
                    grossWeightKg = BigDecimal("30"),
                    netWeightKg = BigDecimal("27")
                )
            ),
            notes = "Shipment 1"
        )

        assertEquals(400, getPoRemainingItems(poId)[0].remainingQty)
        assertEquals(PurchaseOrderStatus.SHIPPED.name, purchaseOrders[poId]?.status)

        // 2. Cancel shipment 1
        cancelShipment(shp1Id)
        assertEquals(ShipmentStatus.CANCELLED.name, shipments[shp1Id]?.status)

        // 3. Verify PO status is restored to CONFIRMED and available quantity is restored to 1,000!
        assertEquals(PurchaseOrderStatus.CONFIRMED.name, purchaseOrders[poId]?.status)
        val restoredRem = getPoRemainingItems(poId)[0]
        assertEquals(0, restoredRem.shippedQty)
        assertEquals(1000, restoredRem.remainingQty)

        // 4. Now a shipment of 1,000 units can be created successfully!
        val shp2Id = createShipment(
            supplierId = "sup-shenzhen-001",
            orderId = poId,
            method = ShippingMethod.SEA,
            origin = "Yantian",
            destination = "Chattogram",
            trackingRef = "CONT-555",
            shippingLine = "COSCO",
            cartonDrafts = listOf(
                ShipmentCartonDraft(
                    cartonNumber = "CTN-1",
                    productId = "prod-earbuds-001",
                    quantity = 1000,
                    lengthCm = BigDecimal("100"),
                    widthCm = BigDecimal("80"),
                    heightCm = BigDecimal("60"),
                    grossWeightKg = BigDecimal("60"),
                    netWeightKg = BigDecimal("54")
                )
            ),
            notes = "Replacement shipment"
        )

        assertNotNull(shp2Id)
        assertEquals(0, getPoRemainingItems(poId)[0].remainingQty)
    }

    @Test
    fun testCartonDraftValidationRules() {
        // Zero or negative quantity
        try {
            createShipment(
                supplierId = "sup-shenzhen-001",
                orderId = null,
                method = ShippingMethod.AIR,
                origin = "SZX",
                destination = "DAC",
                trackingRef = "AWB-666",
                shippingLine = "Line",
                cartonDrafts = listOf(
                    ShipmentCartonDraft(
                        cartonNumber = "CTN-1",
                        productId = "prod-earbuds-001",
                        quantity = 0,
                        lengthCm = BigDecimal("10"),
                        widthCm = BigDecimal("10"),
                        heightCm = BigDecimal("10"),
                        grossWeightKg = BigDecimal("1"),
                        netWeightKg = BigDecimal("0.9")
                    )
                ),
                notes = null
            )
            fail("Expected exception on zero quantity")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("positive"))
        }

        // Zero dimension
        try {
            createShipment(
                supplierId = "sup-shenzhen-001",
                orderId = null,
                method = ShippingMethod.AIR,
                origin = "SZX",
                destination = "DAC",
                trackingRef = "AWB-777",
                shippingLine = "Line",
                cartonDrafts = listOf(
                    ShipmentCartonDraft(
                        cartonNumber = "CTN-1",
                        productId = "prod-earbuds-001",
                        quantity = 10,
                        lengthCm = BigDecimal.ZERO,
                        widthCm = BigDecimal("10"),
                        heightCm = BigDecimal("10"),
                        grossWeightKg = BigDecimal("1"),
                        netWeightKg = BigDecimal("0.9")
                    )
                ),
                notes = null
            )
            fail("Expected exception on zero dimension")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("greater than zero"))
        }

        // Blank tracking ref
        try {
            createShipment(
                supplierId = "sup-shenzhen-001",
                orderId = null,
                method = ShippingMethod.AIR,
                origin = "SZX",
                destination = "DAC",
                trackingRef = "  ",
                shippingLine = "Line",
                cartonDrafts = listOf(
                    ShipmentCartonDraft(
                        cartonNumber = "CTN-1",
                        productId = "prod-earbuds-001",
                        quantity = 10,
                        lengthCm = BigDecimal("10"),
                        widthCm = BigDecimal("10"),
                        heightCm = BigDecimal("10"),
                        grossWeightKg = BigDecimal("1"),
                        netWeightKg = BigDecimal("0.9")
                    )
                ),
                notes = null
            )
            fail("Expected exception on blank tracking reference")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Tracking reference"))
        }
    }
}
