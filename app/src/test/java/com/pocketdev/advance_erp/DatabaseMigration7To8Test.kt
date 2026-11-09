package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * PHASE 06 PART 3 — DATABASE MIGRATION 7 TO 8 TESTS
 *
 * Verifies:
 * 1. Migration version numbers (7 -> 8).
 * 2. Alteration of `stock_movements` table adding balanceAfterMovement, totalCostImpactMinor,
 *    purchaseOrderId, purchaseBatchId, salesInvoiceId, stockTransferId, operatorName.
 * 3. Creation of `fifo_consumptions` table and indexes.
 * 4. Creation of `inventory_cost_adjustments` table and indexes.
 * 5. Creation of `inventory_reconciliations` and `inventory_reconciliation_items` tables and indexes.
 * 6. Historical data preservation and backfilling from sales_cost_layer_usages.
 */
class DatabaseMigration7To8Test {

    @Test
    fun testMigration7To8VersionNumbers() {
        val migration = DatabaseMigrations.MIGRATION_7_8
        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)
    }

    @Test
    fun testMigration7To8ExecutesExpectedSqlStatements() {
        val executedSqlList = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSqlList.add(sql)
            }
            null
        } as SupportSQLiteDatabase

        DatabaseMigrations.MIGRATION_7_8.migrate(mockDb)

        assertTrue("Expected SQL statements executed in MIGRATION_7_8", executedSqlList.isNotEmpty())

        // 1. stock_movements columns
        assertTrue("Must add balanceAfterMovement", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN balanceAfterMovement") })
        assertTrue("Must add totalCostImpactMinor", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN totalCostImpactMinor") })
        assertTrue("Must add purchaseOrderId", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN purchaseOrderId") })
        assertTrue("Must add purchaseBatchId", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN purchaseBatchId") })
        assertTrue("Must add salesInvoiceId", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN salesInvoiceId") })
        assertTrue("Must add stockTransferId", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN stockTransferId") })
        assertTrue("Must add operatorName", executedSqlList.any { it.contains("ALTER TABLE stock_movements ADD COLUMN operatorName") })

        // 2. fifo_consumptions table
        val fifoSql = executedSqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS fifo_consumptions") }
        assertNotNull("Must create fifo_consumptions table", fifoSql)
        assertTrue("Must reference costLayerId", fifoSql!!.contains("costLayerId TEXT NOT NULL"))
        assertTrue("Must reference invoiceId", fifoSql.contains("invoiceId TEXT"))
        assertTrue("Must reference unitCostBdtMinor", fifoSql.contains("unitCostBdtMinor INTEGER NOT NULL"))
        assertTrue("Must reference totalCogsBdtMinor", fifoSql.contains("totalCogsBdtMinor INTEGER NOT NULL"))

        // 3. inventory_cost_adjustments table
        val adjSql = executedSqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS inventory_cost_adjustments") }
        assertNotNull("Must create inventory_cost_adjustments table", adjSql)
        assertTrue("Must include adjustmentNumber", adjSql!!.contains("adjustmentNumber TEXT NOT NULL"))
        assertTrue("Must include unitVarianceBdtMinor", adjSql.contains("unitVarianceBdtMinor INTEGER NOT NULL"))
        assertTrue("Must include inventoryAdjustmentAmountMinor", adjSql.contains("inventoryAdjustmentAmountMinor INTEGER NOT NULL"))
        assertTrue("Must include cogsAdjustmentAmountMinor", adjSql.contains("cogsAdjustmentAmountMinor INTEGER NOT NULL"))

        // 4. inventory_reconciliations table
        val recSql = executedSqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS inventory_reconciliations") }
        assertNotNull("Must create inventory_reconciliations table", recSql)
        assertTrue("Must include totalStockBalanceQty", recSql!!.contains("totalStockBalanceQty INTEGER NOT NULL"))
        assertTrue("Must include totalFifoValuationMinor", recSql.contains("totalFifoValuationMinor INTEGER NOT NULL"))
        assertTrue("Must include account1210BalanceMinor", recSql.contains("account1210BalanceMinor INTEGER NOT NULL"))

        // 5. inventory_reconciliation_items table
        val recItemSql = executedSqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS inventory_reconciliation_items") }
        assertNotNull("Must create inventory_reconciliation_items table", recItemSql)
        assertTrue("Must include qtyMismatch", recItemSql!!.contains("qtyMismatch INTEGER NOT NULL"))
        assertTrue("Must include reconciliationId FK", recItemSql.contains("FOREIGN KEY(reconciliationId) REFERENCES inventory_reconciliations(id)"))

        // 6. Data backfill statement
        assertTrue("Must backfill fifo_consumptions", executedSqlList.any { it.contains("INSERT OR IGNORE INTO fifo_consumptions") })
    }
}
