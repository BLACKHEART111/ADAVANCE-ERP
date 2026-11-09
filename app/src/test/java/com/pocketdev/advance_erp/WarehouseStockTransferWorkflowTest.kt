package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import com.pocketdev.advance_erp.data.entity.StockTransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

/**
 * PHASE 06 PART 1 — REQUIREMENT 2 & 8 TESTS
 * Tests 5, 6, 7, 8, 9:
 * 5. Warehouse transfer Draft → Dispatch → Receive.
 * 6. Insufficient source stock.
 * 7. Duplicate transfer dispatch/receipt.
 * 8. Transfer cancellation and reversal.
 * 9. FIFO cost preservation during transfer.
 */
class WarehouseStockTransferWorkflowTest {

    // Simple test simulation engine to verify exact state transitions and FIFO rules
    class TransferTestHarness {
        val balances = mutableMapOf<String, Int>() // warehouseId:productId -> qty
        val costLayers = mutableListOf<InventoryCostLayer>()
        val transfers = mutableMapOf<String, StockTransfer>()
        val transferCostLayers = mutableListOf<StockTransferCostLayer>()

        fun setInitialStock(warehouseId: String, productId: String, qty: Int, unitCostMinor: Long) {
            balances["$warehouseId:$productId"] = qty
            costLayers.add(
                InventoryCostLayer(
                    id = "layer-${UUID.randomUUID()}",
                    productId = productId,
                    warehouseId = warehouseId,
                    shipmentId = "SHP-001",
                    shipmentNumber = "SHP-2026-001",
                    warehouseReceiptId = "WR-001",
                    receiptNumber = "WR-2026-001",
                    initialQuantity = qty,
                    remainingQuantity = qty,
                    unitLandedCostBdtMinor = unitCostMinor,
                    originalPurchasePriceMinor = unitCostMinor,
                    exchangeRateToBdt = "1.0",
                    allocatedExpensePerUnitBdtMinor = 0L,
                    receiptDate = System.currentTimeMillis() - 100000L,
                    isActive = true
                )
            )
        }

        fun createTransfer(
            sourceWarehouseId: String,
            destWarehouseId: String,
            productId: String,
            quantity: Int
        ): String {
            require(sourceWarehouseId != destWarehouseId) { "Source and destination cannot be the same" }
            require(quantity > 0) { "Quantity must be positive" }

            val id = UUID.randomUUID().toString()
            transfers[id] = StockTransfer(
                id = id,
                transferNumber = MovementNumberGenerator.generateTransferNumber(),
                sourceWarehouseId = sourceWarehouseId,
                destinationWarehouseId = destWarehouseId,
                productId = productId,
                quantity = quantity,
                status = StockTransferStatus.DRAFT.name
            )
            return id
        }

        fun dispatchTransfer(transferId: String) {
            val transfer = transfers[transferId] ?: throw IllegalArgumentException("Transfer not found")
            check(transfer.status == StockTransferStatus.DRAFT.name) {
                "Only DRAFT transfers can be dispatched. Current: ${transfer.status}"
            }

            val sourceKey = "${transfer.sourceWarehouseId}:${transfer.productId}"
            val currentStock = balances[sourceKey] ?: 0
            if (currentStock < transfer.quantity) {
                throw IllegalStateException("Insufficient stock at source warehouse: available $currentStock, requested ${transfer.quantity}")
            }

            // Deduct source stock
            balances[sourceKey] = currentStock - transfer.quantity

            // Deplete FIFO cost layers at source
            var needed = transfer.quantity
            val sourceLayers = costLayers
                .filter { it.warehouseId == transfer.sourceWarehouseId && it.productId == transfer.productId && it.isActive && it.remainingQuantity > 0 }
                .sortedBy { it.receiptDate }

            for (layer in sourceLayers) {
                if (needed <= 0) break
                val take = minOf(needed, layer.remainingQuantity)
                val idx = costLayers.indexOf(layer)
                val newRemaining = layer.remainingQuantity - take
                costLayers[idx] = layer.copy(remainingQuantity = newRemaining, isActive = newRemaining > 0)

                transferCostLayers.add(
                    StockTransferCostLayer(
                        transferId = transfer.id,
                        costLayerId = layer.id,
                        quantity = take,
                        unitCostMinor = layer.unitLandedCostBdtMinor
                    )
                )
                needed -= take
            }

            transfers[transferId] = transfer.copy(
                status = StockTransferStatus.IN_TRANSIT.name,
                dispatchedAt = System.currentTimeMillis()
            )
        }

        fun receiveTransfer(transferId: String) {
            val transfer = transfers[transferId] ?: throw IllegalArgumentException("Transfer not found")
            check(transfer.status == StockTransferStatus.IN_TRANSIT.name) {
                "Only IN_TRANSIT transfers can be received. Current: ${transfer.status}"
            }

            // Add stock to destination
            val destKey = "${transfer.destinationWarehouseId}:${transfer.productId}"
            val currentStock = balances[destKey] ?: 0
            balances[destKey] = currentStock + transfer.quantity

            // Replicate FIFO cost layers at destination
            val usedLayers = transferCostLayers.filter { it.transferId == transferId }
            for (ul in usedLayers) {
                val orig = costLayers.find { it.id == ul.costLayerId }
                costLayers.add(
                    InventoryCostLayer(
                        id = "dest-layer-${UUID.randomUUID()}",
                        productId = transfer.productId,
                        warehouseId = transfer.destinationWarehouseId,
                        shipmentId = orig?.shipmentId ?: "TR-${transfer.transferNumber}",
                        shipmentNumber = orig?.shipmentNumber ?: transfer.transferNumber,
                        warehouseReceiptId = orig?.warehouseReceiptId ?: transfer.id,
                        receiptNumber = orig?.receiptNumber ?: transfer.transferNumber,
                        initialQuantity = ul.quantity,
                        remainingQuantity = ul.quantity,
                        unitLandedCostBdtMinor = ul.unitCostMinor,
                        originalPurchasePriceMinor = orig?.originalPurchasePriceMinor ?: ul.unitCostMinor,
                        exchangeRateToBdt = orig?.exchangeRateToBdt ?: "1.0",
                        allocatedExpensePerUnitBdtMinor = orig?.allocatedExpensePerUnitBdtMinor ?: 0L,
                        receiptDate = System.currentTimeMillis(),
                        isActive = true
                    )
                )
            }

            transfers[transferId] = transfer.copy(
                status = StockTransferStatus.RECEIVED.name,
                receivedAt = System.currentTimeMillis()
            )
        }

        fun cancelTransfer(transferId: String) {
            val transfer = transfers[transferId] ?: throw IllegalArgumentException("Transfer not found")
            check(transfer.status != StockTransferStatus.CANCELLED.name) { "Transfer is already cancelled" }
            check(transfer.status != StockTransferStatus.RECEIVED.name) { "Cannot cancel an already received transfer" }

            if (transfer.status == StockTransferStatus.IN_TRANSIT.name) {
                // Restore source stock balance
                val sourceKey = "${transfer.sourceWarehouseId}:${transfer.productId}"
                balances[sourceKey] = (balances[sourceKey] ?: 0) + transfer.quantity

                // Restore FIFO layers
                val usedLayers = transferCostLayers.filter { it.transferId == transferId }
                for (ul in usedLayers) {
                    val idx = costLayers.indexOfFirst { it.id == ul.costLayerId }
                    if (idx >= 0) {
                        val layer = costLayers[idx]
                        costLayers[idx] = layer.copy(
                            remainingQuantity = layer.remainingQuantity + ul.quantity,
                            isActive = true
                        )
                    }
                }
            }

            transfers[transferId] = transfer.copy(
                status = StockTransferStatus.CANCELLED.name,
                cancelledAt = System.currentTimeMillis()
            )
        }

        fun calculateValuation(warehouseId: String? = null): Long {
            return costLayers
                .filter { it.isActive && it.remainingQuantity > 0 && (warehouseId == null || it.warehouseId == warehouseId) }
                .sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }
        }
    }

    @Test
    fun testCompleteWorkflowDraftToDispatchToReceive() {
        val harness = TransferTestHarness()
        // Source warehouse: 100 units @ ৳400 (40,000 minor)
        harness.setInitialStock("wh-central", "prod-1", 100, 40000L)
        assertEquals(100, harness.balances["wh-central:prod-1"])
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // 1. Create Draft
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 30)
        val draft = harness.transfers[transferId]!!
        assertEquals(StockTransferStatus.DRAFT.name, draft.status)
        // Invariant: Draft must NOT affect stock!
        assertEquals(100, harness.balances["wh-central:prod-1"])
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // 2. Dispatch
        harness.dispatchTransfer(transferId)
        val dispatched = harness.transfers[transferId]!!
        assertEquals(StockTransferStatus.IN_TRANSIT.name, dispatched.status)
        assertNotNull(dispatched.dispatchedAt)
        // Source stock reduced by 30
        assertEquals(70, harness.balances["wh-central:prod-1"])
        // Destination stock must NOT yet be increased while in-transit!
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // 3. Receive
        harness.receiveTransfer(transferId)
        val received = harness.transfers[transferId]!!
        assertEquals(StockTransferStatus.RECEIVED.name, received.status)
        assertNotNull(received.receivedAt)
        // Destination stock now increased by 30
        assertEquals(30, harness.balances["wh-branch:prod-1"])
        assertEquals(70, harness.balances["wh-central:prod-1"])
    }

    @Test
    fun testInsufficientSourceStockPreventsDispatch() {
        val harness = TransferTestHarness()
        harness.setInitialStock("wh-central", "prod-1", 20, 50000L)

        // Request 50 units when only 20 are available
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 50)

        try {
            harness.dispatchTransfer(transferId)
            fail("Expected IllegalStateException due to insufficient source stock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Insufficient stock"))
        }

        // Verify stock untouched
        assertEquals(20, harness.balances["wh-central:prod-1"])
        assertEquals(StockTransferStatus.DRAFT.name, harness.transfers[transferId]!!.status)
    }

    @Test
    fun testPreventDuplicateDispatchAndDuplicateReceipt() {
        val harness = TransferTestHarness()
        harness.setInitialStock("wh-central", "prod-1", 100, 30000L)
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 25)

        // First dispatch succeeds
        harness.dispatchTransfer(transferId)
        assertEquals(StockTransferStatus.IN_TRANSIT.name, harness.transfers[transferId]!!.status)

        // Duplicate dispatch must be rejected
        try {
            harness.dispatchTransfer(transferId)
            fail("Expected IllegalStateException on duplicate dispatch")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Only DRAFT transfers can be dispatched"))
        }

        // Receive succeeds
        harness.receiveTransfer(transferId)
        assertEquals(StockTransferStatus.RECEIVED.name, harness.transfers[transferId]!!.status)

        // Duplicate receipt must be rejected
        try {
            harness.receiveTransfer(transferId)
            fail("Expected IllegalStateException on duplicate receipt")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Only IN_TRANSIT transfers can be received"))
        }
    }

    @Test
    fun testTransferCancellationAndReversalFromInTransit() {
        val harness = TransferTestHarness()
        harness.setInitialStock("wh-central", "prod-1", 80, 45000L)
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 35)

        harness.dispatchTransfer(transferId)
        assertEquals(45, harness.balances["wh-central:prod-1"])
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // Cancel while In-Transit -> Reversal
        harness.cancelTransfer(transferId)
        val cancelled = harness.transfers[transferId]!!
        assertEquals(StockTransferStatus.CANCELLED.name, cancelled.status)
        assertNotNull(cancelled.cancelledAt)

        // Source stock fully restored
        assertEquals(80, harness.balances["wh-central:prod-1"])
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // Destination never received
        assertEquals(0, harness.balances["wh-branch:prod-1"] ?: 0)

        // FIFO layers restored at source
        val activeSourceLayers = harness.costLayers.filter { it.warehouseId == "wh-central" && it.isActive }
        assertEquals(80, activeSourceLayers.sumOf { it.remainingQuantity })
    }

    @Test
    fun testCannotCancelAlreadyReceivedTransfer() {
        val harness = TransferTestHarness()
        harness.setInitialStock("wh-central", "prod-1", 50, 40000L)
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 20)

        harness.dispatchTransfer(transferId)
        harness.receiveTransfer(transferId)

        try {
            harness.cancelTransfer(transferId)
            fail("Expected IllegalStateException when cancelling a received transfer")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Cannot cancel an already received transfer"))
        }
    }

    @Test
    fun testFifoCostPreservationAndValuationNonDuplication() {
        val harness = TransferTestHarness()
        // Central warehouse has 100 units @ ৳350.00 (35,000 minor)
        harness.setInitialStock("wh-central", "prod-1", 100, 35000L)
        val initialTotalValuation = harness.calculateValuation()
        assertEquals(3500000L, initialTotalValuation) // ৳35,000.00

        // Transfer 40 units to Branch warehouse
        val transferId = harness.createTransfer("wh-central", "wh-branch", "prod-1", 40)
        harness.dispatchTransfer(transferId)

        // During transit:
        // Central valuation reduced to 60 * 35000 = 2,100,000
        assertEquals(2100000L, harness.calculateValuation("wh-central"))
        // Branch has 0 valuation (not yet received)
        assertEquals(0L, harness.calculateValuation("wh-branch"))
        // In-transit stock must NOT duplicate company inventory valuation!
        assertEquals(2100000L, harness.calculateValuation())

        // Receive transfer at destination
        harness.receiveTransfer(transferId)

        // After receipt:
        // Central has 60 units @ 35000 = 2,100,000
        assertEquals(2100000L, harness.calculateValuation("wh-central"))
        // Branch has 40 units @ 35000 = 1,400,000 (preserved original cost!)
        assertEquals(1400000L, harness.calculateValuation("wh-branch"))
        // Total company valuation = 2,100,000 + 1,400,000 = 3,500,000 (exact preservation!)
        val finalTotalValuation = harness.calculateValuation()
        assertEquals(initialTotalValuation, finalTotalValuation)
    }
}
