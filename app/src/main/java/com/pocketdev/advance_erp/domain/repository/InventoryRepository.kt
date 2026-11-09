package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.dao.StockMovementWithDetails
import com.pocketdev.advance_erp.data.dao.StockTransferWithDetails
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.Warehouse
import kotlinx.coroutines.flow.Flow

data class StockTransferDraft(
    val sourceWarehouseId: String,
    val destinationWarehouseId: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val transferDate: Long = System.currentTimeMillis(),
    val notes: String? = null
)

interface InventoryRepository {
    fun getStockOverview(): Flow<List<StockItemOverview>>
    fun getStockOverviewByWarehouse(warehouseId: String): Flow<List<StockItemOverview>>
    fun getLowStockItems(): Flow<List<StockItemOverview>>
    fun getAllWarehouses(): Flow<List<Warehouse>>
    fun getRecentStockMovements(limit: Int = 50): Flow<List<StockMovementWithDetails>>
    fun getRecentStockMovementsByWarehouse(warehouseId: String, limit: Int = 50): Flow<List<StockMovementWithDetails>>

    suspend fun adjustStock(
        warehouseId: String,
        productId: String,
        variantId: String?,
        quantityChange: Int,
        isIncrease: Boolean,
        reason: String
    )

    // Stock Transfer Workflow
    fun getAllStockTransfers(): Flow<List<StockTransferWithDetails>>
    fun getStockTransfersByWarehouse(warehouseId: String): Flow<List<StockTransferWithDetails>>
    suspend fun getStockTransferById(id: String): StockTransfer?
    suspend fun createStockTransfer(draft: StockTransferDraft): String
    suspend fun dispatchStockTransfer(transferId: String)
    suspend fun receiveStockTransfer(transferId: String)
    suspend fun cancelStockTransfer(transferId: String, reason: String? = null)

    // Valuation & Stock Metrics
    fun getTotalPhysicalQuantity(): Flow<Int>
    fun getPhysicalQuantityByWarehouse(warehouseId: String): Flow<Int>
    fun getTotalSellableQuantity(): Flow<Int>
    fun getSellableQuantityByWarehouse(warehouseId: String): Flow<Int>
    fun getTotalDamagedQuantity(): Flow<Int>
    fun getDamagedQuantityByWarehouse(warehouseId: String): Flow<Int>
    fun getTotalInventoryValuationMinor(): Flow<Long>
    fun getInventoryValuationByWarehouseMinor(warehouseId: String): Flow<Long>
}
