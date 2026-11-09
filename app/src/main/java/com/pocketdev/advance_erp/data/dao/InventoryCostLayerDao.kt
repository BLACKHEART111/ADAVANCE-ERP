package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryCostLayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostLayer(layer: InventoryCostLayer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostLayers(layers: List<InventoryCostLayer>)

    @Query("SELECT * FROM inventory_cost_layers ORDER BY receiptDate DESC, createdAt DESC")
    fun getAllCostLayers(): Flow<List<InventoryCostLayer>>

    @Query("SELECT * FROM inventory_cost_layers ORDER BY receiptDate DESC, createdAt DESC")
    suspend fun getAllCostLayersSync(): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId ORDER BY receiptDate DESC, createdAt DESC")
    fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId ORDER BY receiptDate DESC, createdAt DESC")
    suspend fun getCostLayersForProductSync(productId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId AND isActive = 1 ORDER BY receiptDate ASC, createdAt ASC")
    fun getActiveLayersForProduct(productId: String): Flow<List<InventoryCostLayer>>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId AND isActive = 1 ORDER BY receiptDate ASC, createdAt ASC")
    suspend fun getActiveLayersForProductSync(productId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId AND warehouseId = :warehouseId AND isActive = 1 ORDER BY receiptDate ASC, createdAt ASC")
    suspend fun getActiveLayersForProductAndWarehouseSync(productId: String, warehouseId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE productId = :productId AND variantId = :variantId AND warehouseId = :warehouseId AND isActive = 1 ORDER BY receiptDate ASC, createdAt ASC")
    suspend fun getActiveLayersForVariantAndWarehouseSync(productId: String, variantId: String, warehouseId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE warehouseId = :warehouseId AND isActive = 1 ORDER BY receiptDate ASC, createdAt ASC")
    suspend fun getActiveLayersForWarehouseSync(warehouseId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE shipmentId = :shipmentId")
    fun getLayersForShipment(shipmentId: String): Flow<List<InventoryCostLayer>>

    @Query("SELECT * FROM inventory_cost_layers WHERE shipmentId = :shipmentId")
    suspend fun getLayersForShipmentSync(shipmentId: String): List<InventoryCostLayer>

    @Query("SELECT * FROM inventory_cost_layers WHERE purchaseBatchId = :purchaseBatchId ORDER BY receiptDate DESC")
    fun getCostLayersForBatch(purchaseBatchId: String): Flow<List<InventoryCostLayer>>

    @Query("SELECT * FROM inventory_cost_layers WHERE id = :id LIMIT 1")
    suspend fun getCostLayerById(id: String): InventoryCostLayer?

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity * unitLandedCostBdtMinor), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0
    """)
    fun getTotalInventoryValuationMinor(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity * unitLandedCostBdtMinor), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0
    """)
    suspend fun getTotalFifoValuationSync(): Long

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity * unitLandedCostBdtMinor), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0 AND warehouseId = :warehouseId
    """)
    fun getInventoryValuationByWarehouseMinor(warehouseId: String): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity * unitLandedCostBdtMinor), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0 AND warehouseId = :warehouseId
    """)
    suspend fun getFifoValuationByWarehouseSync(warehouseId: String): Long

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0
    """)
    suspend fun getTotalFifoRemainingQtySync(): Int

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0 AND warehouseId = :warehouseId
    """)
    suspend fun getFifoRemainingQtyByWarehouseSync(warehouseId: String): Int

    @Query("""
        SELECT COALESCE(SUM(remainingQuantity), 0)
        FROM inventory_cost_layers
        WHERE isActive = 1 AND remainingQuantity > 0 AND productId = :productId AND warehouseId = :warehouseId
    """)
    suspend fun getFifoRemainingQtyForProductSync(productId: String, warehouseId: String): Int

    @Query("UPDATE inventory_cost_layers SET remainingQuantity = :remainingQuantity, isActive = :isActive WHERE id = :layerId")
    suspend fun updateRemainingQuantity(layerId: String, remainingQuantity: Int, isActive: Boolean)

    @Query("UPDATE inventory_cost_layers SET unitLandedCostBdtMinor = :unitCostMinor, allocatedExpensePerUnitBdtMinor = :allocatedExpenseMinor WHERE id = :layerId")
    suspend fun updateCostLayerUnitCost(layerId: String, unitCostMinor: Long, allocatedExpenseMinor: Long)
}
