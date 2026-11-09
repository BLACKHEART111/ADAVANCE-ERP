package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * PHASE 07 — DATABASE MIGRATION 8 TO 9 TESTS
 *
 * Verifies:
 * 1. Migration version numbers (8 -> 9).
 * 2. Alteration of `customer_payments` table adding isVoided, voidedAt, voidReason, voidedBy.
 * 3. Alteration of `audit_logs` table adding documentNumber, operationType, previousStatus,
 *    newStatus, cancellationReason, reversalReference.
 * 4. Alteration of `sales_invoices` table adding cancellationReason, cancelledAt.
 * 5. Seeding of system fallback Uncategorized category (cat-uncategorized).
 */
class DatabaseMigration8To9Test {

    @Test
    fun testMigration8To9VersionNumbers() {
        val migration = DatabaseMigrations.MIGRATION_8_9
        assertEquals(8, migration.startVersion)
        assertEquals(9, migration.endVersion)
    }

    @Test
    fun testMigration8To9ExecutesExpectedSqlStatements() {
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

        DatabaseMigrations.MIGRATION_8_9.migrate(mockDb)

        assertTrue("Expected SQL statements executed in MIGRATION_8_9", executedSqlList.isNotEmpty())

        // 1. customer_payments void tracking columns
        assertTrue("Must add isVoided to customer_payments",
            executedSqlList.any { it.contains("ALTER TABLE customer_payments ADD COLUMN isVoided") })
        assertTrue("Must add voidedAt to customer_payments",
            executedSqlList.any { it.contains("ALTER TABLE customer_payments ADD COLUMN voidedAt") })
        assertTrue("Must add voidReason to customer_payments",
            executedSqlList.any { it.contains("ALTER TABLE customer_payments ADD COLUMN voidReason") })
        assertTrue("Must add voidedBy to customer_payments",
            executedSqlList.any { it.contains("ALTER TABLE customer_payments ADD COLUMN voidedBy") })

        // 2. audit_logs unified audit trail columns
        assertTrue("Must add documentNumber to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN documentNumber") })
        assertTrue("Must add operationType to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN operationType") })
        assertTrue("Must add previousStatus to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN previousStatus") })
        assertTrue("Must add newStatus to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN newStatus") })
        assertTrue("Must add cancellationReason to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN cancellationReason") })
        assertTrue("Must add reversalReference to audit_logs",
            executedSqlList.any { it.contains("ALTER TABLE audit_logs ADD COLUMN reversalReference") })

        // 3. sales_invoices cancellation columns
        assertTrue("Must add cancellationReason to sales_invoices",
            executedSqlList.any { it.contains("ALTER TABLE sales_invoices ADD COLUMN cancellationReason") })
        assertTrue("Must add cancelledAt to sales_invoices",
            executedSqlList.any { it.contains("ALTER TABLE sales_invoices ADD COLUMN cancelledAt") })

        // 4. Default Uncategorized category
        assertTrue("Must seed default uncategorized category",
            executedSqlList.any { it.contains("cat-uncategorized") && it.contains("UNCAT") })
    }
}
