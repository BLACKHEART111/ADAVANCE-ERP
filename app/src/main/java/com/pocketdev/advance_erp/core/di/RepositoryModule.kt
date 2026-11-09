package com.pocketdev.advance_erp.core.di

import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import com.pocketdev.advance_erp.data.repository.BackupRepositoryImpl
import com.pocketdev.advance_erp.data.repository.BusinessControlCenterRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ContactsRepositoryImpl
import com.pocketdev.advance_erp.data.repository.CostHistoryRepositoryImpl
import com.pocketdev.advance_erp.data.repository.DashboardRepositoryImpl
import com.pocketdev.advance_erp.data.repository.FinancialRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ImportRepositoryImpl
import com.pocketdev.advance_erp.data.repository.InventoryRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ProductRepositoryImpl
import com.pocketdev.advance_erp.data.repository.SalesRepositoryImpl
import com.pocketdev.advance_erp.data.repository.SettingsRepositoryImpl
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CostHistoryRepository
import com.pocketdev.advance_erp.domain.repository.DashboardRepository
import com.pocketdev.advance_erp.domain.repository.FinancialRepository
import com.pocketdev.advance_erp.domain.repository.ImportRepository
import com.pocketdev.advance_erp.domain.repository.InventoryRepository
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindImportRepository(impl: ImportRepositoryImpl): ImportRepository

    @Binds
    @Singleton
    abstract fun bindAccountingRepository(impl: AccountingRepositoryImpl): AccountingRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(impl: SalesRepositoryImpl): SalesRepository

    @Binds
    @Singleton
    abstract fun bindFinancialRepository(impl: FinancialRepositoryImpl): FinancialRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindCostHistoryRepository(impl: CostHistoryRepositoryImpl): CostHistoryRepository

    @Binds
    @Singleton
    abstract fun bindBusinessControlCenterRepository(impl: BusinessControlCenterRepositoryImpl): BusinessControlCenterRepository
}
