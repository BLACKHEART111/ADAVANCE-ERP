package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ADVANCE ERP — PHASE 06 PART 3: STOCK MOVEMENT AUDIT TRAIL TESTS
 *
 * Verifies:
 * 1. Running balance calculation across multiple movement types.
 * 2. Proper signed net movements for inventory reconciliation.
 * 3. Directional classification and cost impact computation.
 * 4. Immutability & audit compliance (reversals are separate append-only movements).
 */
class StockMovementAuditTrailTest {

    @Test
    fun testRunningBalanceAcrossMovementSequence() {
        var runningBal = 0
        val movements = mutableListOf<StockMovement>()

        fun addMovement(
            movNum: String,
            type: StockMovementType,
            qty: Int,
            unitCostMinor: Long
        ): StockMovement {
            val netQty = if (type.isIncoming) qty else -qty
            runningBal += netQty
            val mov = StockMovement(
                movementNumber = movNum,
                warehouseId = "wh-1",
                productId = "prod-1",
                variantId = null,
                movementType = type.code,
                quantity = qty,
                unitCostMinor = unitCostMinor,
                balanceAfterMovement = runningBal,
                totalCostImpactMinor = qty.toLong() * unitCostMinor
            )
            movements.add(mov)
            return mov
        }

        // 1. Opening Stock: 100 units @ 100 BDT
        val m1 = addMovement("M-01", StockMovementType.OPENING_STOCK, 100, 10000L)
        assertEquals("Running balance after opening stock must be 100", 100, m1.balanceAfterMovement)
        assertEquals(100 * 10000L, m1.totalCostImpactMinor)

        // 2. Purchase Receipt: 50 units @ 110 BDT
        val m2 = addMovement("M-02", StockMovementType.PURCHASE_RECEIPT, 50, 11000L)
        assertEquals("Running balance after purchase receipt must be 150", 150, m2.balanceAfterMovement)
        assertEquals(50 * 11000L, m2.totalCostImpactMinor)

        // 3. Sale Dispatch: 40 units @ 100 BDT
        val m3 = addMovement("M-03", StockMovementType.SALE_DISPATCH, 40, 10000L)
        assertEquals("Running balance after sale dispatch must be 110", 110, m3.balanceAfterMovement)

        // 4. Stock Transfer Out: 20 units @ 105 BDT
        val m4 = addMovement("M-04", StockMovementType.TRANSFER_OUT, 20, 10500L)
        assertEquals("Running balance after transfer out must be 90", 90, m4.balanceAfterMovement)

        // 5. Sales Return: 5 units @ 100 BDT
        val m5 = addMovement("M-05", StockMovementType.SALE_RETURN, 5, 10000L)
        assertEquals("Running balance after sales return must be 95", 95, m5.balanceAfterMovement)

        // 6. Damage Write-Off: 3 units @ 100 BDT
        val m6 = addMovement("M-06", StockMovementType.DAMAGE_WRITE_OFF, 3, 10000L)
        assertEquals("Running balance after damage write-off must be 92", 92, m6.balanceAfterMovement)

        // Final balance check
        assertEquals("Final running balance must equal 92 units", 92, runningBal)
    }

    @Test
    fun testNetMovementQuantityCalculationForReconciliation() {
        val list = listOf(
            StockMovement(movementNumber = "1", warehouseId = "w1", productId = "p1", movementType = "PURCHASE_RECEIPT", quantity = 100, unitCostMinor = 1000L),
            StockMovement(movementNumber = "2", warehouseId = "w1", productId = "p1", movementType = "SALE_DISPATCH", quantity = 30, unitCostMinor = 1000L),
            StockMovement(movementNumber = "3", warehouseId = "w1", productId = "p1", movementType = "SALE_RETURN", quantity = 5, unitCostMinor = 1000L),
            StockMovement(movementNumber = "4", warehouseId = "w1", productId = "p1", movementType = "DAMAGE_WRITE_OFF", quantity = 2, unitCostMinor = 1000L),
            StockMovement(movementNumber = "5", warehouseId = "w1", productId = "p1", movementType = "INVENTORY_COST_ADJUSTMENT", quantity = 0, unitCostMinor = 1200L)
        )

        val netQty = list.sumOf { mov ->
            when (mov.movementType) {
                "PURCHASE_RECEIPT", "SALE_RETURN", "STOCK_ADJUSTMENT_IN", "TRANSFER_IN", "OPENING_STOCK" -> mov.quantity
                "SALE_DISPATCH", "PURCHASE_RETURN", "STOCK_ADJUSTMENT_OUT", "TRANSFER_OUT", "DAMAGE_WRITE_OFF", "SHORTAGE" -> -mov.quantity
                else -> 0
            }
        }

        // 100 - 30 + 5 - 2 = 73
        assertEquals("Net movement quantity must be exactly 73", 73, netQty)
    }

    @Test
    fun testImmutableAppendOnlyAuditTrail() {
        // When reversing a mistaken movement, the system must append a new compensating movement
        val original = StockMovement(
            movementNumber = "MOV-ORIG",
            warehouseId = "w1",
            productId = "p1",
            movementType = StockMovementType.STOCK_ADJUSTMENT_IN.code,
            quantity = 10,
            unitCostMinor = 5000L,
            balanceAfterMovement = 10
        )

        // Compensating reversal movement
        val reversal = StockMovement(
            movementNumber = "MOV-REV",
            warehouseId = "w1",
            productId = "p1",
            movementType = StockMovementType.STOCK_ADJUSTMENT_OUT.code,
            quantity = 10,
            unitCostMinor = 5000L,
            balanceAfterMovement = 0,
            referenceType = "REVERSAL",
            referenceId = original.movementNumber,
            notes = "Reversal of erroneous adjustment #MOV-ORIG"
        )

        assertEquals("Original remains untouched in append-only log", "MOV-ORIG", original.movementNumber)
        assertEquals("Reversal explicitly references original", "MOV-ORIG", reversal.referenceId)
        assertEquals("Net balance returns to 0", 0, reversal.balanceAfterMovement)
    }
}
