package com.pocketdev.advance_erp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.dao.AppSettingsDao
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.BackupDao
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CashBankDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.FifoConsumptionDao
import com.pocketdev.advance_erp.data.dao.ImportCostDao
import com.pocketdev.advance_erp.data.dao.ImportShipmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationDao
import com.pocketdev.advance_erp.data.dao.MinimalImportDao
import com.pocketdev.advance_erp.data.dao.ProductCategoryDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductImageDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.StockTransferDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.dao.WarehouseReceiptDao
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.AppSettingsEntity
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import com.pocketdev.advance_erp.data.entity.InventoryReconciliationItem
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.LandedCostAllocation
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.SalesCostLayerUsage
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem

@Database(
    entities = [
        BusinessProfile::class,
        ProductCategory::class,
        Product::class,
        ProductVariant::class,
        ProductImage::class,
        Warehouse::class,
        StockBalance::class,
        StockMovement::class,
        Customer::class,
        Supplier::class,
        AppSettingsEntity::class,
        // Phase 02 Entities:
        PurchaseOrder::class,
        PurchaseOrderItem::class,
        SupplierPayment::class,
        ImportShipment::class,
        ShipmentCarton::class,
        ImportCostItem::class,
        LandedCostAllocation::class,
        LandedCostSnapshot::class,
        WarehouseReceipt::class,
        WarehouseReceiptItem::class,
        InventoryCostLayer::class,
        ChartOfAccount::class,
        JournalEntry::class,
        JournalLine::class,
        // Phase 03 Entities:
        SalesInvoice::class,
        SalesInvoiceItem::class,
        SalesCostLayerUsage::class,
        CustomerPayment::class,
        PaymentAllocation::class,
        SalesReturn::class,
        SalesReturnItem::class,
        // Phase 04 Entities:
        ExpenseVoucher::class,
        AccountTransfer::class,
        BankReconciliation::class,
        BankReconciliationItem::class,
        AuditLog::class,
        BackupRecord::class,
        // Phase 05 Minimal Import Entities:
        DamageRecord::class,
        ShortageRecord::class,
        AdditionalProductCost::class,
        PurchaseCompletionRecord::class,
        // Phase 06 Inventory & Stock Transfer Entities:
        StockTransfer::class,
        StockTransferCostLayer::class,
        // Phase 06 Part 3 FIFO & Audit Entities:
        FifoConsumption::class,
        InventoryCostAdjustment::class,
        InventoryReconciliation::class,
        InventoryReconciliationItem::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AdvanceErpDatabase : RoomDatabase() {

    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun productCategoryDao(): ProductCategoryDao
    abstract fun productDao(): ProductDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun productImageDao(): ProductImageDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun stockBalanceDao(): StockBalanceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockTransferDao(): StockTransferDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun appSettingsDao(): AppSettingsDao

    // Phase 02 DAOs:
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun supplierPaymentDao(): SupplierPaymentDao
    abstract fun importShipmentDao(): ImportShipmentDao
    abstract fun importCostDao(): ImportCostDao
    abstract fun warehouseReceiptDao(): WarehouseReceiptDao
    abstract fun inventoryCostLayerDao(): InventoryCostLayerDao
    abstract fun accountingDao(): AccountingDao

    // Phase 03 DAOs:
    abstract fun salesDao(): SalesDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun salesReturnDao(): SalesReturnDao

    // Phase 04 DAOs:
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashBankDao(): CashBankDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun backupDao(): BackupDao

    // Phase 05 DAOs:
    abstract fun minimalImportDao(): MinimalImportDao

    // Phase 06 Part 3 DAOs:
    abstract fun fifoConsumptionDao(): FifoConsumptionDao
    abstract fun inventoryCostAdjustmentDao(): InventoryCostAdjustmentDao
    abstract fun inventoryReconciliationDao(): InventoryReconciliationDao

    companion object {
        const val DATABASE_NAME = "advance_erp.db"

        fun buildDatabase(context: Context): AdvanceErpDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AdvanceErpDatabase::class.java,
                DATABASE_NAME
            )
                .createFromAsset("database/advance_erp.db")
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6,
                    DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8,
                    DatabaseMigrations.MIGRATION_8_9
                )
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.setForeignKeyConstraintsEnabled(true)
                        seedIfEmpty(db)
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedIfEmpty(db)
                    }
                })
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        fun seedIfEmpty(db: SupportSQLiteDatabase) {
            removeLegacyDemoData(db)
            resetBusinessDataOnce(db)
            // Check if business profile exists
            val profileCursor = db.query("SELECT COUNT(*) FROM business_profiles")
            var profileCount = 0
            if (profileCursor.moveToFirst()) {
                profileCount = profileCursor.getInt(0)
            }
            profileCursor.close()

            if (profileCount == 0) {
                seedBusinessProfile(db)
            }

            // Check if already seeded Phase 01 (warehouses)
            val cursor = db.query("SELECT COUNT(*) FROM warehouses")
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()

            if (count == 0) {
                seedPhase01(db)
            }

            // Ensure all standard accounts exist if not already populated
            val coaCursor = db.query("SELECT COUNT(*) FROM chart_of_accounts")
            var coaCount = 0
            if (coaCursor.moveToFirst()) {
                coaCount = coaCursor.getInt(0)
            }
            coaCursor.close()

            if (coaCount < 27) {
                DatabaseMigrations.seedStandardAccounts(db)
            }

            // Ensure fallback Uncategorized category exists
            db.execSQL("""
                INSERT OR IGNORE INTO product_categories (id, nameEn, nameBn, code, description)
                VALUES ('cat-uncategorized', 'Uncategorized', 'শ্রেণীবিহীন', 'UNCAT', 'Default fallback category for unassigned products')
            """.trimIndent())
        }

        fun seedBusinessProfile(db: SupportSQLiteDatabase) {
            db.execSQL("""
                INSERT OR IGNORE INTO business_profiles
                (id, companyName, taxNumber, phone, email, address, defaultCurrency, updatedAt)
                VALUES ('default_profile', 'My Business', '', '', '', '', 'BDT', 0)
            """.trimIndent())
        }

        /** Minimal, non-demo defaults: app settings and one empty default warehouse. */
        fun seedPhase01(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                db.execSQL("""
                    INSERT OR IGNORE INTO app_settings
                    (id, languageCode, currencyCode, isDarkMode, lastSyncTime, dbVersion)
                    VALUES (1, 'bn', 'BDT', 0, 0, 9)
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO warehouses
                    (id, name, code, location, isDefault, isActive)
                    VALUES ('wh-dhk-01', 'Main Warehouse', 'WH-MAIN', '', 1, 1)
                """.trimIndent())
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        private const val CLEANUP_FLAG = "clean_start_v1"

        /**
         * One-time clean start: earlier builds shipped example/test records. Clears all transactional and
         * master data once (keeps chart of accounts, settings, business profile and one default warehouse).
         */
        fun resetBusinessDataOnce(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_cleanup_flags (flag TEXT NOT NULL PRIMARY KEY)")
                val c = db.query("SELECT COUNT(*) FROM app_cleanup_flags WHERE flag = '$CLEANUP_FLAG'")
                val done = c.moveToFirst() && c.getInt(0) > 0
                c.close()
                if (done) return

                val keep = setOf(
                    "chart_of_accounts", "app_settings", "business_profiles", "warehouses",
                    "app_cleanup_flags", "room_master_table", "android_metadata", "sqlite_sequence"
                )
                val tables = mutableListOf<String>()
                val tc = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
                while (tc.moveToNext()) tables.add(tc.getString(0))
                tc.close()

                db.execSQL("PRAGMA foreign_keys=OFF")
                db.beginTransaction()
                try {
                    for (t in tables) {
                        if (t in keep) continue
                        db.execSQL("DELETE FROM `$t`")
                    }
                    db.execSQL("DELETE FROM warehouses WHERE id != 'wh-dhk-01' AND isDefault = 0")
                    db.execSQL("UPDATE warehouses SET isDefault = 1 WHERE id = 'wh-dhk-01'")
                    db.execSQL("INSERT OR REPLACE INTO app_cleanup_flags (flag) VALUES ('$CLEANUP_FLAG')")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            } catch (_: Exception) {
                // Leave data untouched if anything unexpected happens.
            }
        }

        /** Removes the old bundled example records from existing installs (only rows not referenced elsewhere). */
        fun removeLegacyDemoData(db: SupportSQLiteDatabase) {
            val statements = listOf(
                "DELETE FROM stock_movements WHERE id IN ('mov-01','mov-02')",
                "DELETE FROM stock_balances WHERE id IN ('bal-01','bal-02','bal-03','bal-04','bal-05')",
                "DELETE FROM product_variants WHERE id IN ('var-earbuds-blk','var-earbuds-wht','var-jkt-l-nvy','var-jkt-xl-blk')",
                "DELETE FROM products WHERE id IN ('prod-earbuds-01','prod-jacket-02','prod-kettle-03')",
                "DELETE FROM product_categories WHERE id IN ('cat-elec','cat-fashion','cat-home')",
                "DELETE FROM customers WHERE id IN ('cust-dhk-101','cust-ntr-102')",
                "DELETE FROM suppliers WHERE id IN ('sup-gz-001','sup-sz-002')",
                "DELETE FROM warehouses WHERE id = 'wh-ctg-01'",
                "UPDATE warehouses SET name='Main Warehouse', code='WH-MAIN', location='' WHERE id='wh-dhk-01' AND code='WH-DHK-01'",
                "UPDATE business_profiles SET companyName='My Business', taxNumber='', phone='', email='', address='' WHERE companyName='Advance Import Ltd.' AND taxNumber='BIN-99281726-01'"
            )
            for (sql in statements) {
                try { db.execSQL(sql) } catch (_: Exception) { /* referenced by user data; keep */ }
            }
        }
    }
}
