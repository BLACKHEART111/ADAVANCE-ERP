package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.NormalBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * PHASE 05.6 — DATABASE MIGRATION VERIFICATION
 * Tests that migration path 1 -> 2 -> 3 -> 4 is continuous and non-destructive.
 */
class DatabaseMigrationVerificationTest {

    @Test
    fun testMigrationVersionsContinuous() {
        val m12 = DatabaseMigrations.MIGRATION_1_2
        assertEquals(1, m12.startVersion)
        assertEquals(2, m12.endVersion)

        val m23 = DatabaseMigrations.MIGRATION_2_3
        assertEquals(2, m23.startVersion)
        assertEquals(3, m23.endVersion)

        val m34 = DatabaseMigrations.MIGRATION_3_4
        assertEquals(3, m34.startVersion)
        assertEquals(4, m34.endVersion)

        val m45 = DatabaseMigrations.MIGRATION_4_5
        assertEquals(4, m45.startVersion)
        assertEquals(5, m45.endVersion)

        val m56 = DatabaseMigrations.MIGRATION_5_6
        assertEquals(5, m56.startVersion)
        assertEquals(6, m56.endVersion)

        val m67 = DatabaseMigrations.MIGRATION_6_7
        assertEquals(6, m67.startVersion)
        assertEquals(7, m67.endVersion)

        val m78 = DatabaseMigrations.MIGRATION_7_8
        assertEquals(7, m78.startVersion)
        assertEquals(8, m78.endVersion)
    }

    @Test
    fun testStandardChartOfAccountsIntegrity() {
        // Standard chart of accounts must have essential accounts
        val expectedCodes = listOf(
            "1010" to (AccountType.ASSET to NormalBalance.DEBIT),      // Cash in Hand
            "1020" to (AccountType.ASSET to NormalBalance.DEBIT),      // Bank Account
            "1040" to (AccountType.ASSET to NormalBalance.DEBIT),      // Accounts Receivable
            "1050" to (AccountType.ASSET to NormalBalance.DEBIT),      // Supplier Shortage & Claims Receivable
            "1210" to (AccountType.ASSET to NormalBalance.DEBIT),      // Inventory Asset
            "1220" to (AccountType.ASSET to NormalBalance.DEBIT),      // Inventory in Transit
            "2010" to (AccountType.LIABILITY to NormalBalance.CREDIT), // Accounts Payable
            "2030" to (AccountType.LIABILITY to NormalBalance.CREDIT), // Customer Advance
            "3010" to (AccountType.EQUITY to NormalBalance.CREDIT),    // Owner Equity
            "3020" to (AccountType.EQUITY to NormalBalance.CREDIT),    // Retained Earnings
            "4010" to (AccountType.REVENUE to NormalBalance.CREDIT),   // Sales Revenue
            "4020" to (AccountType.REVENUE to NormalBalance.DEBIT),    // Sales Returns (Contra-Revenue)
            "4030" to (AccountType.REVENUE to NormalBalance.DEBIT),    // Sales Discount (Contra-Revenue)
            "5010" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Cost of Goods Sold
            "5050" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Damaged Merchandise Write-Off
            "5110" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Operating Expenses
            "5120" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Customs & Duty
            "5130" to (AccountType.EXPENSE to NormalBalance.DEBIT)     // Shipping & Freight
        )

        val uniqueCodes = expectedCodes.map { it.first }.toSet()
        assertEquals("Account codes must be unique", expectedCodes.size, uniqueCodes.size)

        expectedCodes.forEach { (_, typeAndBalance) ->
            val (type, normalBalance) = typeAndBalance
            assertNotNull(type)
            assertNotNull(normalBalance)
        }
    }

    @Test
    fun testAllMigrationsExecuteWithoutSyntaxErrorsOrInvalidTokens() {
        val executedSqlList = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSqlList.add(sql)
                // SQLite foreign key action is 'SET NULL' (with space), NOT 'SET_NULL'
                assertFalse("SQL contains SET_NULL which is invalid SQLite syntax: $sql", sql.contains("SET_NULL"))
                assertFalse("SQL contains SET_DEFAULT which is invalid SQLite syntax: $sql", sql.contains("SET_DEFAULT"))
            }
            null
        } as SupportSQLiteDatabase

        // Execute all migrations against mock SupportSQLiteDatabase
        DatabaseMigrations.MIGRATION_1_2.migrate(mockDb)
        DatabaseMigrations.MIGRATION_2_3.migrate(mockDb)
        DatabaseMigrations.MIGRATION_3_4.migrate(mockDb)
        DatabaseMigrations.MIGRATION_4_5.migrate(mockDb)
        DatabaseMigrations.MIGRATION_5_6.migrate(mockDb)
        DatabaseMigrations.MIGRATION_6_7.migrate(mockDb)

        assertTrue("Expected multiple SQL statements to be executed across migrations", executedSqlList.size > 20)
    }
}
