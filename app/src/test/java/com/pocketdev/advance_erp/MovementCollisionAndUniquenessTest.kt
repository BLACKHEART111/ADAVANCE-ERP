package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * PHASE 06 PART 1 — REQUIREMENT 1 & 8 TESTS
 * Tests 1, 2, 3, 4:
 * 1. Same-millisecond movement creation.
 * 2. Multiple movement creation within one receipt.
 * 3. Movement number uniqueness.
 * 4. Concurrent movement creation.
 */
class MovementCollisionAndUniquenessTest {

    @Test
    fun testSameMillisecondMovementCreationGeneratesUniqueIdentifiers() {
        // Generating multiple movement numbers in a tight loop within the exact same millisecond
        val count = 200
        val numbers = Array(count) { MovementNumberGenerator.generate("SM-2026") }
        val uniqueSet = numbers.toSet()

        assertEquals("All movement numbers generated in rapid succession must be strictly unique", count, uniqueSet.size)
        // Verify prefix and formatting
        assertTrue(numbers[0].startsWith("SM-2026-"))
    }

    @Test
    fun testMultipleMovementCreationWithinSingleReceipt() {
        // A warehouse receipt with multiple line items, plus sound, damaged and shortage movements
        val itemsCount = 10
        val receiptMovements = mutableListOf<StockMovement>()

        for (i in 0 until itemsCount) {
            // Sound stock movement
            val soundMovementNumber = MovementNumberGenerator.generate("SM-2026")
            receiptMovements.add(
                StockMovement(
                    id = MovementNumberGenerator.generateId("mov"),
                    movementNumber = soundMovementNumber,
                    warehouseId = "wh-main",
                    productId = "prod-$i",
                    movementType = StockMovementType.PURCHASE_RECEIPT.code,
                    quantity = 100,
                    unitCostMinor = 25000L,
                    referenceType = "WAREHOUSE_RECEIPT",
                    referenceId = "WR-2026-0001"
                )
            )

            // Damaged stock movement
            val dmgMovementNumber = MovementNumberGenerator.generate("DMG-2026")
            receiptMovements.add(
                StockMovement(
                    id = MovementNumberGenerator.generateId("mov-dmg"),
                    movementNumber = dmgMovementNumber,
                    warehouseId = "wh-main",
                    productId = "prod-$i",
                    movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                    quantity = 5,
                    unitCostMinor = 25000L,
                    referenceType = "WAREHOUSE_RECEIPT",
                    referenceId = "WR-2026-0001"
                )
            )
        }

        val movementNumbers = receiptMovements.map { it.movementNumber }
        val uniqueNumbers = movementNumbers.toSet()

        assertEquals(
            "Every sound and damaged movement created within the same receipt must have a unique movementNumber",
            receiptMovements.size,
            uniqueNumbers.size
        )
    }

    @Test
    fun testMovementNumberUniquenessAndFormatting() {
        val set = HashSet<String>()
        val total = 5000
        for (i in 0 until total) {
            val num = MovementNumberGenerator.generate("MOV")
            assertTrue("Generated movement number '$num' was already produced!", set.add(num))
        }
        assertEquals(total, set.size)
    }

    @Test
    fun testConcurrentMovementCreationUnderHighLoad() = runBlocking(Dispatchers.Default) {
        val threadCount = 20
        val movementsPerThread = 100
        val totalMovements = threadCount * movementsPerThread
        val generatedNumbers = ConcurrentHashMap.newKeySet<String>()

        val deferreds = (1..threadCount).map {
            async {
                for (j in 1..movementsPerThread) {
                    val num = MovementNumberGenerator.generate("MOV")
                    generatedNumbers.add(num)
                }
            }
        }

        deferreds.awaitAll()

        assertEquals(
            "Concurrent generation across $threadCount threads must result in zero collisions",
            totalMovements,
            generatedNumbers.size
        )
    }

    @Test
    fun testFailedTransactionRollbackInvariant() {
        // Simulating transactional integrity: if a duplicate movement number is inserted
        // into a uniqueness-enforcing repository, the entire transaction rolls back.
        val existingMovements = mutableMapOf<String, StockMovement>()
        val mockBalances = mutableMapOf("wh-1:prod-1" to 100)

        fun adjustStockWithTransaction(
            warehouseId: String,
            productId: String,
            qty: Int,
            movementNumber: String
        ): Result<Unit> {
            val key = "$warehouseId:$productId"
            val snapshotBalance = mockBalances[key] ?: 0

            return try {
                // Step 1: Update balance
                mockBalances[key] = snapshotBalance + qty

                // Step 2: Insert movement with uniqueness enforcement
                if (existingMovements.containsKey(movementNumber)) {
                    throw IllegalStateException("UNIQUE constraint failed: stock_movements.movementNumber '$movementNumber' already exists")
                }
                existingMovements[movementNumber] = StockMovement(
                    id = "mov-1",
                    movementNumber = movementNumber,
                    warehouseId = warehouseId,
                    productId = productId,
                    movementType = StockMovementType.STOCK_ADJUSTMENT_IN.code,
                    quantity = qty,
                    unitCostMinor = 1000L
                )
                Result.success(Unit)
            } catch (e: Exception) {
                // Rollback Step 1
                mockBalances[key] = snapshotBalance
                Result.failure(e)
            }
        }

        // First adjustment succeeds
        val res1 = adjustStockWithTransaction("wh-1", "prod-1", 50, "MOV-001")
        assertTrue(res1.isSuccess)
        assertEquals(150, mockBalances["wh-1:prod-1"])

        // Second adjustment with duplicate number MUST fail and rollback balance
        val res2 = adjustStockWithTransaction("wh-1", "prod-1", 30, "MOV-001")
        assertTrue(res2.isFailure)
        assertEquals("Balance must roll back to 150 on transaction failure", 150, mockBalances["wh-1:prod-1"])
    }
}
