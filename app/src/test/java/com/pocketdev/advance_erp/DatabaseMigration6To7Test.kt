package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * PHASE 06 PART 2 — DATABASE MIGRATION 6 TO 7 TESTS
 *
 * Verifies:
 * 1. Migration version start (6) and end (7).
 * 2. Re-creation of `product_images` table with `variantId` foreign key referencing `product_variants(id)` ON DELETE CASCADE.
 * 3. Safe copy of existing images from `product_images` to `product_images_new`.
 * 4. Index on `variantId` in `product_images`.
 * 5. Addition of `qrCode` column to `products` and `product_variants`.
 * 6. Index on `qrCode` and `barcode` for fast lookup and offline scanning.
 */
class DatabaseMigration6To7Test {

    @Test
    fun testMigration6To7VersionNumbers() {
        val migration = DatabaseMigrations.MIGRATION_6_7
        assertEquals(6, migration.startVersion)
        assertEquals(7, migration.endVersion)
    }

    @Test
    fun testMigration6To7ExecutesExpectedSqlStatements() {
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

        DatabaseMigrations.MIGRATION_6_7.migrate(mockDb)

        assertTrue("Expected SQL statements executed in MIGRATION_6_7", executedSqlList.isNotEmpty())

        // 1. Table recreation for product_images with variantId and CASCADE foreign keys
        val createTableSql = executedSqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS product_images_new") }
        assertNotNull("Must create product_images_new table", createTableSql)
        assertTrue("Must include variantId column", createTableSql!!.contains("variantId TEXT"))
        assertTrue("Must include products FK", createTableSql.contains("FOREIGN KEY(productId) REFERENCES products(id)"))
        assertTrue("Must include product_variants FK", createTableSql.contains("FOREIGN KEY(variantId) REFERENCES product_variants(id)"))
        assertTrue("Must include ON DELETE CASCADE", createTableSql.contains("ON DELETE CASCADE"))

        // 2. Data copy and table rename
        val copyDataSql = executedSqlList.firstOrNull { it.contains("INSERT INTO product_images_new") }
        assertNotNull("Must copy existing data", copyDataSql)
        assertTrue("Must copy existing columns into new table", copyDataSql!!.contains("SELECT id, productId, NULL, imageUri, isPrimary, sortOrder"))

        val dropSql = executedSqlList.firstOrNull { it.contains("DROP TABLE product_images") }
        assertNotNull("Must drop old table", dropSql)

        val renameSql = executedSqlList.firstOrNull { it.contains("ALTER TABLE product_images_new RENAME TO product_images") }
        assertNotNull("Must rename new table", renameSql)

        // 3. New variantId index on product_images
        val variantIndexSql = executedSqlList.firstOrNull { it.contains("index_product_images_variantId") }
        assertNotNull("Must create index on product_images(variantId)", variantIndexSql)

        // 4. Products qrCode column and index
        val productQrColSql = executedSqlList.firstOrNull { it.contains("ALTER TABLE products ADD COLUMN qrCode TEXT") }
        assertNotNull("Must add qrCode to products", productQrColSql)

        val productQrIndexSql = executedSqlList.firstOrNull { it.contains("index_products_qrCode") }
        assertNotNull("Must index products(qrCode)", productQrIndexSql)

        // 5. Product variants qrCode column and index
        val variantQrColSql = executedSqlList.firstOrNull { it.contains("ALTER TABLE product_variants ADD COLUMN qrCode TEXT") }
        assertNotNull("Must add qrCode to product_variants", variantQrColSql)

        val variantQrIndexSql = executedSqlList.firstOrNull { it.contains("index_product_variants_qrCode") }
        assertNotNull("Must index product_variants(qrCode)", variantQrIndexSql)

        // 6. Product variants barcode index
        val variantBarcodeIndexSql = executedSqlList.firstOrNull { it.contains("index_product_variants_barcode") }
        assertNotNull("Must index product_variants(barcode)", variantBarcodeIndexSql)
    }

    @Test
    fun testCompleteContinuousMigrationFromVersion1To7() {
        val migrations = listOf(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7
        )

        for (i in 0 until migrations.size - 1) {
            assertEquals(
                "Migration chain must be contiguous at step $i",
                migrations[i].endVersion,
                migrations[i + 1].startVersion
            )
        }

        assertEquals(1, migrations.first().startVersion)
        assertEquals(7, migrations.last().endVersion)
    }
}
