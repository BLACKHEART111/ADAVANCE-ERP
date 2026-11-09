package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import kotlinx.coroutines.flow.Flow

data class InventoryCostAdjustmentWithDetails(
    val id: String,
    val adjustmentNumber: String,
    val costLayerId: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val variantId: String?,
    val variantSku: String?,
    val warehouseId: String,
    val warehouseName: String,
    val purchaseOrderId: String?,
    val referenceType: String?,
    val referenceId: String?,
    val reason: String,
    val oldUnitCostBdtMinor: Long,
    val newUnitCostBdtMinor: Long,
    val unitVarianceBdtMinor: Long,
    val totalRemainingQty: Int,
    val totalSoldQty: Int,
    val inventoryAdjustmentAmountMinor: Long,
    val cogsAdjustmentAmountMinor: Long,
    val totalAdjustmentAmountMinor: Long,
    val journalEntryId: String?,
    val createdAt: Long
)

@Dao
interface InventoryCostAdjustmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdjustment(adjustment: InventoryCostAdjustment)

    @Query("SELECT * FROM inventory_cost_adjustments ORDER BY createdAt DESC")
    fun getAllAdjustments(): Flow<List<InventoryCostAdjustment>>

    @Query("SELECT * FROM inventory_cost_adjustments WHERE id = :id LIMIT 1")
    suspend fun getAdjustmentById(id: String): InventoryCostAdjustment?

    @Query("SELECT * FROM inventory_cost_adjustments WHERE adjustmentNumber = :adjustmentNumber LIMIT 1")
    suspend fun getAdjustmentByNumber(adjustmentNumber: String): InventoryCostAdjustment?

    @Query("SELECT * FROM inventory_cost_adjustments WHERE productId = :productId ORDER BY createdAt DESC")
    fun getAdjustmentsForProduct(productId: String): Flow<List<InventoryCostAdjustment>>

    @Query("SELECT * FROM inventory_cost_adjustments WHERE costLayerId = :costLayerId ORDER BY createdAt DESC")
    fun getAdjustmentsForLayer(costLayerId: String): Flow<List<InventoryCostAdjustment>>

    @Query("""
        SELECT ica.id, ica.adjustmentNumber, ica.costLayerId,
               ica.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               ica.variantId, pv.sku as variantSku,
               ica.warehouseId, w.name as warehouseName,
               ica.purchaseOrderId, ica.referenceType, ica.referenceId,
               ica.reason, ica.oldUnitCostBdtMinor, ica.newUnitCostBdtMinor, ica.unitVarianceBdtMinor,
               ica.totalRemainingQty, ica.totalSoldQty,
               ica.inventoryAdjustmentAmountMinor, ica.cogsAdjustmentAmountMinor, ica.totalAdjustmentAmountMinor,
               ica.journalEntryId, ica.createdAt
        FROM inventory_cost_adjustments ica
        INNER JOIN products p ON ica.productId = p.id
        LEFT JOIN product_variants pv ON ica.variantId = pv.id
        INNER JOIN warehouses w ON ica.warehouseId = w.id
        ORDER BY ica.createdAt DESC
    """)
    fun getAllAdjustmentsWithDetails(): Flow<List<InventoryCostAdjustmentWithDetails>>

    @Query("""
        SELECT ica.id, ica.adjustmentNumber, ica.costLayerId,
               ica.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               ica.variantId, pv.sku as variantSku,
               ica.warehouseId, w.name as warehouseName,
               ica.purchaseOrderId, ica.referenceType, ica.referenceId,
               ica.reason, ica.oldUnitCostBdtMinor, ica.newUnitCostBdtMinor, ica.unitVarianceBdtMinor,
               ica.totalRemainingQty, ica.totalSoldQty,
               ica.inventoryAdjustmentAmountMinor, ica.cogsAdjustmentAmountMinor, ica.totalAdjustmentAmountMinor,
               ica.journalEntryId, ica.createdAt
        FROM inventory_cost_adjustments ica
        INNER JOIN products p ON ica.productId = p.id
        LEFT JOIN product_variants pv ON ica.variantId = pv.id
        INNER JOIN warehouses w ON ica.warehouseId = w.id
        WHERE ica.productId = :productId
        ORDER BY ica.createdAt DESC
    """)
    fun getAdjustmentsWithDetailsForProduct(productId: String): Flow<List<InventoryCostAdjustmentWithDetails>>

    @Query("SELECT COUNT(*) FROM inventory_cost_adjustments")
    suspend fun getAdjustmentCount(): Int
}
