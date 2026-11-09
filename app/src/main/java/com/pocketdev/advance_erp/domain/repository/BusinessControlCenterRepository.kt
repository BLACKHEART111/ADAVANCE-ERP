package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.model.SavedFilterPreset
import com.pocketdev.advance_erp.domain.model.TransactionDetailRecord
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import kotlinx.coroutines.flow.Flow

interface BusinessControlCenterRepository {
    fun getTransactions(filterState: ControlCenterFilterState): Flow<List<UnifiedTransactionItem>>
    fun getSummaryDashboard(filterState: ControlCenterFilterState): Flow<BusinessSummaryDashboardData>
    suspend fun getTransactionDetails(rawType: String, id: String): TransactionDetailRecord?
    suspend fun getCustomerLedgerStatement(customerId: String, fromDate: Long, toDate: Long): PartyLedgerStatement?
    suspend fun getSupplierLedgerStatement(supplierId: String, fromDate: Long, toDate: Long): PartyLedgerStatement?
    fun getAllCustomers(): Flow<List<Customer>>
    fun getAllSuppliers(): Flow<List<Supplier>>
    fun getAllWarehouses(): Flow<List<Warehouse>>
    fun getAllCategories(): Flow<List<ProductCategory>>
    fun getBusinessProfile(): Flow<BusinessProfile?>
    fun getSavedPresets(): Flow<List<SavedFilterPreset>>
    suspend fun savePreset(name: String, filterState: ControlCenterFilterState): String
    suspend fun deletePreset(presetId: String)
}
