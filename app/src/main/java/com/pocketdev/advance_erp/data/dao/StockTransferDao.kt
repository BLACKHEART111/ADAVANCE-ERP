package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import kotlinx.coroutines.flow.Flow

data class StockTransferWithDetails(
    val id: String,
    val transferNumber: String,
    val sourceWarehouseId: String,
    val sourceWarehouseName: String,
    val destinationWarehouseId: String,
    val destinationWarehouseName: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val variantId: String?,
    val variantSku: String?,
    val quantity: Int,
    val transferDate: Long,
    val notes: String?,
    val status: String,
    val dispatchedAt: Long?,
    val receivedAt: Long?,
    val cancelledAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface StockTransferDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfer(transfer: StockTransfer)

    @Update
    suspend fun updateTransfer(transfer: StockTransfer)

    @Query("""
        SELECT st.*, 
               w1.name as sourceWarehouseName, 
               w2.name as destinationWarehouseName,
               p.nameEn as productNameEn, 
               p.nameBn as productNameBn, 
               p.baseSku as baseSku,
               pv.sku as variantSku
        FROM stock_transfers st
        INNER JOIN warehouses w1 ON st.sourceWarehouseId = w1.id
        INNER JOIN warehouses w2 ON st.destinationWarehouseId = w2.id
        INNER JOIN products p ON st.productId = p.id
        LEFT JOIN product_variants pv ON st.variantId = pv.id
        ORDER BY st.createdAt DESC
    """)
    fun getAllTransfersWithDetails(): Flow<List<StockTransferWithDetails>>

    @Query("""
        SELECT st.*, 
               w1.name as sourceWarehouseName, 
               w2.name as destinationWarehouseName,
               p.nameEn as productNameEn, 
               p.nameBn as productNameBn, 
               p.baseSku as baseSku,
               pv.sku as variantSku
        FROM stock_transfers st
        INNER JOIN warehouses w1 ON st.sourceWarehouseId = w1.id
        INNER JOIN warehouses w2 ON st.destinationWarehouseId = w2.id
        INNER JOIN products p ON st.productId = p.id
        LEFT JOIN product_variants pv ON st.variantId = pv.id
        WHERE st.sourceWarehouseId = :warehouseId OR st.destinationWarehouseId = :warehouseId
        ORDER BY st.createdAt DESC
    """)
    fun getTransfersWithDetailsByWarehouse(warehouseId: String): Flow<List<StockTransferWithDetails>>

    @Query("SELECT * FROM stock_transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<StockTransfer>>

    @Query("SELECT * FROM stock_transfers WHERE sourceWarehouseId = :whId OR destinationWarehouseId = :whId ORDER BY createdAt DESC")
    fun getTransfersByWarehouse(whId: String): Flow<List<StockTransfer>>

    @Query("SELECT * FROM stock_transfers WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: String): StockTransfer?

    @Query("SELECT * FROM stock_transfers WHERE transferNumber = :transferNumber LIMIT 1")
    suspend fun getTransferByNumber(transferNumber: String): StockTransfer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferCostLayers(layers: List<StockTransferCostLayer>)

    @Query("SELECT * FROM stock_transfer_cost_layers WHERE transferId = :transferId")
    suspend fun getCostLayersForTransfer(transferId: String): List<StockTransferCostLayer>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_transfers WHERE productId = :productId AND status = 'IN_TRANSIT'")
    fun getInTransitQuantityForProduct(productId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_transfers WHERE status = 'IN_TRANSIT'")
    fun getTotalInTransitQuantity(): Flow<Int>
}
