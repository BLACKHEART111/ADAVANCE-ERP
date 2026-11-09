package com.pocketdev.advance_erp.core.di

import android.content.Context
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.dao.AppSettingsDao
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.BackupDao
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CashBankDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.ImportCostDao
import com.pocketdev.advance_erp.data.dao.ImportShipmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AdvanceErpDatabase {
        return AdvanceErpDatabase.buildDatabase(context)
    }

    @Provides
    fun provideBusinessProfileDao(db: AdvanceErpDatabase): BusinessProfileDao = db.businessProfileDao()

    @Provides
    fun provideProductCategoryDao(db: AdvanceErpDatabase): ProductCategoryDao = db.productCategoryDao()

    @Provides
    fun provideProductDao(db: AdvanceErpDatabase): ProductDao = db.productDao()

    @Provides
    fun provideProductVariantDao(db: AdvanceErpDatabase): ProductVariantDao = db.productVariantDao()

    @Provides
    fun provideProductImageDao(db: AdvanceErpDatabase): ProductImageDao = db.productImageDao()

    @Provides
    fun provideWarehouseDao(db: AdvanceErpDatabase): WarehouseDao = db.warehouseDao()

    @Provides
    fun provideStockBalanceDao(db: AdvanceErpDatabase): StockBalanceDao = db.stockBalanceDao()

    @Provides
    fun provideStockMovementDao(db: AdvanceErpDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun provideStockTransferDao(db: AdvanceErpDatabase): StockTransferDao = db.stockTransferDao()

    @Provides
    fun provideCustomerDao(db: AdvanceErpDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideSupplierDao(db: AdvanceErpDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideAppSettingsDao(db: AdvanceErpDatabase): AppSettingsDao = db.appSettingsDao()

    // Phase 02 DAOs
    @Provides
    fun providePurchaseOrderDao(db: AdvanceErpDatabase): PurchaseOrderDao = db.purchaseOrderDao()

    @Provides
    fun provideSupplierPaymentDao(db: AdvanceErpDatabase): SupplierPaymentDao = db.supplierPaymentDao()

    @Provides
    fun provideImportShipmentDao(db: AdvanceErpDatabase): ImportShipmentDao = db.importShipmentDao()

    @Provides
    fun provideImportCostDao(db: AdvanceErpDatabase): ImportCostDao = db.importCostDao()

    @Provides
    fun provideWarehouseReceiptDao(db: AdvanceErpDatabase): WarehouseReceiptDao = db.warehouseReceiptDao()

    @Provides
    fun provideInventoryCostLayerDao(db: AdvanceErpDatabase): InventoryCostLayerDao = db.inventoryCostLayerDao()

    @Provides
    fun provideAccountingDao(db: AdvanceErpDatabase): AccountingDao = db.accountingDao()

    @Provides
    fun provideSalesDao(db: AdvanceErpDatabase): SalesDao = db.salesDao()

    @Provides
    fun provideCustomerPaymentDao(db: AdvanceErpDatabase): CustomerPaymentDao = db.customerPaymentDao()

    @Provides
    fun provideSalesReturnDao(db: AdvanceErpDatabase): SalesReturnDao = db.salesReturnDao()

    // Phase 04 DAOs
    @Provides
    fun provideExpenseDao(db: AdvanceErpDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCashBankDao(db: AdvanceErpDatabase): CashBankDao = db.cashBankDao()

    @Provides
    fun provideAuditLogDao(db: AdvanceErpDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideBackupDao(db: AdvanceErpDatabase): BackupDao = db.backupDao()

    @Provides
    fun provideMinimalImportDao(db: AdvanceErpDatabase): com.pocketdev.advance_erp.data.dao.MinimalImportDao = db.minimalImportDao()

    // Phase 06 Part 3 DAOs
    @Provides
    fun provideFifoConsumptionDao(db: AdvanceErpDatabase): com.pocketdev.advance_erp.data.dao.FifoConsumptionDao = db.fifoConsumptionDao()

    @Provides
    fun provideInventoryCostAdjustmentDao(db: AdvanceErpDatabase): com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentDao = db.inventoryCostAdjustmentDao()

    @Provides
    fun provideInventoryReconciliationDao(db: AdvanceErpDatabase): com.pocketdev.advance_erp.data.dao.InventoryReconciliationDao = db.inventoryReconciliationDao()
}
