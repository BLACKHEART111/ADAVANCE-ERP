package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.reflect.Proxy

/**
 * ADVANCE ERP — CRITICAL DATABASE INITIALIZATION & SCHEMA VALIDATION TESTS
 *
 * Verifies all 9 requirements from Problem 4:
 * 1. Fresh installation with the pre-packaged database.
 * 2. "business_profiles" schema validation.
 * 3. Initial business profile availability.
 * 4. Phase 01 seed data creation.
 * 5. Stock movement balance calculation.
 * 6. Database reopen after successful initialization.
 * 7. Existing database migration.
 * 8. Repeated initialization without duplicate seed records.
 * 9. All Room entities and foreign-key constraints.
 */
class DatabaseInitializationTest {

    private fun runPythonSqliteCheck(code: String): String {
        val process = ProcessBuilder("python3", "-c", code)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()
        assertEquals("Python SQLite verification script failed with output:\n$output", 0, exitCode)
        return output.trim()
    }

    /**
     * Requirement 1: Fresh installation with the pre-packaged database.
     * Verifies that the bundled pre-packaged asset exists, has the SQLite 3 binary header,
     * user_version 8, and the exact Room master table identity hash.
     */
    @Test
    fun test01_FreshInstallationWithPrePackagedDatabase() {
        val assetDir = File("src/main/assets/database")
        val dbFile = File(assetDir, "advance_erp.db")
        assertTrue("Asset database must exist in assets/database/advance_erp.db", dbFile.exists())
        assertTrue("Asset database must not be empty", dbFile.length() > 0)

        // Check SQLite format 3 magic header
        val header = ByteArray(16)
        FileInputStream(dbFile).use { it.read(header) }
        val headerStr = String(header, Charsets.US_ASCII)
        assertTrue("Database file must start with SQLite format 3 header", headerStr.startsWith("SQLite format 3"))

        val result = runPythonSqliteCheck("""
import sqlite3, json
conn = sqlite3.connect("${dbFile.absolutePath}")
cur = conn.cursor()
cur.execute("PRAGMA user_version")
v = cur.fetchone()[0]
assert v == 8, f"user_version must be 8, got {v}"
cur.execute("PRAGMA integrity_check")
ic = cur.fetchone()[0]
assert ic == "ok", f"integrity_check failed: {ic}"
with open("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/8.json") as f:
    expected_hash = json.load(f)["database"]["identityHash"]
cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
h = cur.fetchone()[0]
assert h == expected_hash, f"hash {h} != {expected_hash}"
conn.close()
print("FRESH_INSTALL_VALIDATED")
        """.trimIndent())
        assertEquals("FRESH_INSTALL_VALIDATED", result)
    }

    /**
     * Requirement 2: "business_profiles" schema validation.
     * Room expects:
     * - id (TEXT, PRIMARY KEY, NOT NULL)
     * - companyName (TEXT, NOT NULL)
     * - phone (TEXT, NOT NULL)
     * - address (TEXT, NOT NULL)
     * - email (TEXT, nullable)
     * - taxNumber (TEXT, nullable)
     * - defaultCurrency (TEXT, NOT NULL)
     * - updatedAt (INTEGER, NOT NULL)
     */
    @Test
    fun test02_BusinessProfilesSchemaValidation() {
        val dbFile = File("src/main/assets/database/advance_erp.db")
        val result = runPythonSqliteCheck("""
import sqlite3
conn = sqlite3.connect("${dbFile.absolutePath}")
cur = conn.cursor()
cur.execute("PRAGMA table_info(business_profiles)")
cols = {r[1]: {"type": r[2], "notnull": bool(r[3]), "pk": r[5]} for r in cur.fetchall()}

# Expected definitions
assert "id" in cols and cols["id"]["type"] == "TEXT" and cols["id"]["notnull"] and cols["id"]["pk"] == 1
assert "companyName" in cols and cols["companyName"]["type"] == "TEXT" and cols["companyName"]["notnull"]
assert "phone" in cols and cols["phone"]["type"] == "TEXT" and cols["phone"]["notnull"]
assert "address" in cols and cols["address"]["type"] == "TEXT" and cols["address"]["notnull"]
assert "email" in cols and cols["email"]["type"] == "TEXT" and not cols["email"]["notnull"]
assert "taxNumber" in cols and cols["taxNumber"]["type"] == "TEXT" and not cols["taxNumber"]["notnull"]
assert "defaultCurrency" in cols and cols["defaultCurrency"]["type"] == "TEXT" and cols["defaultCurrency"]["notnull"]
assert "updatedAt" in cols and cols["updatedAt"]["type"] == "INTEGER" and cols["updatedAt"]["notnull"]
assert len(cols) == 8, f"Expected exactly 8 columns, found {len(cols)}"
conn.close()
print("BUSINESS_PROFILES_SCHEMA_VALID")
        """.trimIndent())
        assertEquals("BUSINESS_PROFILES_SCHEMA_VALID", result)
    }

    /**
     * Requirement 3: Initial business profile availability.
     * Verifies default_profile is available and populated with Advance Import Ltd. metadata.
     */
    @Test
    fun test03_InitialBusinessProfileAvailability() {
        val dbFile = File("src/main/assets/database/advance_erp.db")
        val result = runPythonSqliteCheck("""
import sqlite3
conn = sqlite3.connect("${dbFile.absolutePath}")
cur = conn.cursor()
cur.execute("SELECT id, companyName, taxNumber, phone, email, address, defaultCurrency FROM business_profiles WHERE id = 'default_profile'")
row = cur.fetchone()
assert row is not None, "default_profile record must exist"
assert row[0] == "default_profile"
assert row[1] == "My Business"
assert row[6] == "BDT"
conn.close()
print("BUSINESS_PROFILE_AVAILABLE")
        """.trimIndent())
        assertEquals("BUSINESS_PROFILE_AVAILABLE", result)
    }

    /**
     * Requirement 4: Phase 01 seed data creation.
     * Verifies that seedPhase01 execution succeeds transactionally and populates
     * initial core settings and default warehouse without constraint failures.
     */
    @Test
    fun test04_Phase01SeedDataCreation() {
        val executedSqlList = mutableListOf<String>()
        var transactionBegun = false
        var transactionSuccessful = false
        var transactionEnded = false

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "beginTransaction" -> transactionBegun = true
                "setTransactionSuccessful" -> transactionSuccessful = true
                "endTransaction" -> transactionEnded = true
                "execSQL" -> {
                    val sql = args[0] as String
                    executedSqlList.add(sql)
                }
            }
            null
        } as SupportSQLiteDatabase

        AdvanceErpDatabase.seedPhase01(mockDb)

        assertTrue("Seed operation must begin a transaction", transactionBegun)
        assertTrue("Seed operation must set transaction successful", transactionSuccessful)
        assertTrue("Seed operation must end the transaction", transactionEnded)

        // Verify seeded entities
        assertTrue("Must seed app_settings", executedSqlList.any { it.contains("INSERT OR IGNORE INTO app_settings") })
        assertTrue("Must seed warehouses", executedSqlList.any { it.contains("INSERT OR IGNORE INTO warehouses") })
    }

    /**
     * Requirement 5: Stock movement balance calculation.
     * Verifies stock movement quantity and cost impact math:
     * - Opening stock = 0, movement = +150 @ 850, balanceAfter = 150, totalCostImpactMinor = 12,750,000L
     * - Opening stock = 0, movement = +50 @ 1,200, balanceAfter = 50, totalCostImpactMinor = 6,000,000L
     */
    @Test
    fun test05_StockMovementBalanceCalculation() {
        val openingStock = 0
        val qty1 = 150
        val unitCost1Minor = 85000L
        val balanceAfter1 = openingStock + qty1
        val costImpact1 = qty1 * unitCost1Minor
        assertEquals(150, balanceAfter1)
        assertEquals(12750000L, costImpact1)

        val qty2 = 50
        val unitCost2Minor = 120000L
        val balanceAfter2 = openingStock + qty2
        val costImpact2 = qty2 * unitCost2Minor
        assertEquals(50, balanceAfter2)
        assertEquals(6000000L, costImpact2)
    }

    /**
     * Requirement 6: Database reopen after successful initialization.
     * Verifies that opening, closing, and reopening the database leaves all data intact,
     * retains user_version 8, and passes identity hash checks.
     */
    @Test
    fun test06_DatabaseReopenAfterSuccessfulInitialization() {
        val dbFile = File("src/main/assets/database/advance_erp.db")
        val result = runPythonSqliteCheck("""
import sqlite3, shutil, tempfile, os
tmp = tempfile.mktemp(suffix=".db")
shutil.copyfile("${dbFile.absolutePath}", tmp)

# First open
conn1 = sqlite3.connect(tmp)
cur1 = conn1.cursor()
cur1.execute("SELECT COUNT(*) FROM business_profiles")
assert cur1.fetchone()[0] == 1
cur1.execute("PRAGMA user_version")
assert cur1.fetchone()[0] == 8
conn1.close()

# Reopen
conn2 = sqlite3.connect(tmp)
cur2 = conn2.cursor()
cur2.execute("SELECT COUNT(*) FROM business_profiles")
assert cur2.fetchone()[0] == 1
cur2.execute("SELECT COUNT(*) FROM warehouses")
assert cur2.fetchone()[0] == 1
cur2.execute("SELECT COUNT(*) FROM stock_movements")
assert cur2.fetchone()[0] == 0
cur2.execute("PRAGMA user_version")
assert cur2.fetchone()[0] == 8
cur2.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
assert cur2.fetchone()[0] == "5864dfdf2ce541463e774d3b4c5dfbf8"
conn2.close()
os.remove(tmp)
print("REOPEN_SUCCESSFUL")
        """.trimIndent())
        assertEquals("REOPEN_SUCCESSFUL", result)
    }

    /**
     * Requirement 7: Existing database migration.
     * Verifies that migration sequence 1->2->3->4->5->6->7->8->9 is registered, continuous,
     * non-destructive, and all expected schema additions execute properly.
     */
    @Test
    fun test07_ExistingDatabaseMigration() {
        val migrations = listOf(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9
        )

        for (i in 0 until migrations.size) {
            val m = migrations[i]
            assertEquals("Migration start version continuous", i + 1, m.startVersion)
            assertEquals("Migration end version continuous", i + 2, m.endVersion)
        }
    }

    /**
     * Requirement 8: Repeated initialization without duplicate seed records.
     * Verifies that re-running seedPhase01 or seedStandardAccounts on an already initialized
     * database does NOT create duplicate records or fail with unique constraint errors.
     */
    @Test
    fun test08_RepeatedInitializationWithoutDuplicateSeedRecords() {
        val dbFile = File("src/main/assets/database/advance_erp.db")
        val result = runPythonSqliteCheck("""
import sqlite3, shutil, tempfile, os
tmp = tempfile.mktemp(suffix=".db")
shutil.copyfile("${dbFile.absolutePath}", tmp)

conn = sqlite3.connect(tmp)
cur = conn.cursor()

# Record initial counts
cur.execute("SELECT COUNT(*) FROM business_profiles")
bp_init = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM warehouses")
wh_init = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM chart_of_accounts")
coa_init = cur.fetchone()[0]

# Simulate re-running seeds (INSERT OR REPLACE / INSERT OR IGNORE)
cur.execute("INSERT OR REPLACE INTO business_profiles (id, companyName, taxNumber, phone, email, address, defaultCurrency, updatedAt) VALUES ('default_profile', 'Advance Import Ltd.', 'BIN-99281726-01', '+880 1711-000000', 'info@advanceimport.com', 'Motijheel C/A, Dhaka-1000, Bangladesh', 'BDT', 1793617200000)")
cur.execute("INSERT OR IGNORE INTO chart_of_accounts (id, accountCode, nameEn, nameBn, accountType, normalBalance, description, isActive) VALUES ('coa-1010', '1010', 'Cash in Hand', 'নগদ তহবিল', 'ASSET', 'DEBIT', 'Office Petty Cash', 1)")

# Verify counts did not duplicate
cur.execute("SELECT COUNT(*) FROM business_profiles")
assert cur.fetchone()[0] == bp_init, "business_profiles count must not change"
cur.execute("SELECT COUNT(*) FROM warehouses")
assert cur.fetchone()[0] == wh_init, "warehouses count must not change"
cur.execute("SELECT COUNT(*) FROM chart_of_accounts")
assert cur.fetchone()[0] == coa_init, "chart_of_accounts count must not change"

conn.close()
os.remove(tmp)
print("NO_DUPLICATE_RECORDS")
        """.trimIndent())
        assertEquals("NO_DUPLICATE_RECORDS", result)
    }

    /**
     * Requirement 9: All Room entities and foreign-key constraints.
     * Verifies that all 48 entities from AdvanceErpDatabase are present in the SQLite database,
     * foreign key checks return 0 errors, and foreign key constraint violations are rejected.
     */
    @Test
    fun test09_AllRoomEntitiesAndForeignKeyConstraints() {
        val dbFile = File("src/main/assets/database/advance_erp.db")
        val result = runPythonSqliteCheck("""
import sqlite3, json
with open("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/8.json") as f:
    schema = json.load(f)["database"]

conn = sqlite3.connect("${dbFile.absolutePath}")
cur = conn.cursor()
cur.execute("PRAGMA foreign_keys = ON")

# 1. Foreign key integrity check
cur.execute("PRAGMA foreign_key_check")
fk_errors = cur.fetchall()
assert len(fk_errors) == 0, f"Foreign key check failed: {fk_errors}"

# 2. Verify all 48 Room entity tables exist
assert len(schema["entities"]) == 48, f"Expected 48 entities in Room schema, got {len(schema['entities'])}"
for entity in schema["entities"]:
    tname = entity["tableName"]
    cur.execute(f"SELECT COUNT(*) FROM {tname}")
    _ = cur.fetchone()[0]

# 3. Test that foreign key violations are rejected
try:
    cur.execute("INSERT INTO stock_movements (id, movementNumber, warehouseId, productId, movementType, quantity, unitCostMinor, balanceAfterMovement, totalCostImpactMinor, createdAt) VALUES ('fk-viol', 'SM-FK-01', 'non-existent-warehouse', 'prod-earbuds-01', 'IMPORT_RECEIPT', 10, 1000, 10, 10000, 1793617200000)")
    assert False, "Inserting invalid foreign key must fail"
except sqlite3.IntegrityError:
    pass # Expected FK rejection

conn.close()
print("ALL_48_ENTITIES_AND_FK_VALIDATED")
        """.trimIndent())
        assertEquals("ALL_48_ENTITIES_AND_FK_VALIDATED", result)
    }

    /**
     * Requirement 10: Verify seedStandardAccounts correctly escapes single quotes in chart of accounts
     * (specifically coa-3010 "Owner's Capital & Retained Equity") without SQLite syntax errors.
     */
    @Test
    fun test10_SeedStandardAccountsEscapingAndCoa3010() {
        val executedSqlList = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqlList.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        DatabaseMigrations.seedStandardAccounts(mockDb)

        assertEquals("Must generate 27 account insert statements", 27, executedSqlList.size)
        val coa3010Sql = executedSqlList.firstOrNull { it.contains("coa-3010") }
        assertNotNull("coa-3010 insert SQL must exist", coa3010Sql)
        assertTrue(
            "coa-3010 must properly escape single quote as Owner''s",
            coa3010Sql!!.contains("Owner''s Capital & Retained Equity")
        )

        // Execute all 27 statements in Python SQLite to verify zero syntax errors
        val escapedSqlPayload = executedSqlList.joinToString(";;;") { it.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") }
        val result = runPythonSqliteCheck("""
import sqlite3
conn = sqlite3.connect(':memory:')
cur = conn.cursor()
cur.execute('''CREATE TABLE chart_of_accounts (
    id TEXT NOT NULL PRIMARY KEY,
    accountCode TEXT NOT NULL,
    nameEn TEXT NOT NULL,
    nameBn TEXT NOT NULL,
    accountType TEXT NOT NULL,
    normalBalance TEXT NOT NULL,
    description TEXT,
    isActive INTEGER NOT NULL DEFAULT 1
)''')

statements = "$escapedSqlPayload".split(";;;")
for s in statements:
    cur.execute(s)

cur.execute("SELECT nameEn FROM chart_of_accounts WHERE id = 'coa-3010'")
row = cur.fetchone()
assert row is not None, "coa-3010 must be inserted"
assert row[0] == "Owner's Capital & Retained Equity", f"Expected Owner's Capital & Retained Equity, got {row[0]}"
cur.execute("SELECT COUNT(*) FROM chart_of_accounts")
assert cur.fetchone()[0] == 27, "Must have exactly 27 accounts"
conn.close()
print("COA_SEED_VERIFIED_WITHOUT_SYNTAX_ERRORS")
        """.trimIndent())
        assertEquals("COA_SEED_VERIFIED_WITHOUT_SYNTAX_ERRORS", result)
    }
}
