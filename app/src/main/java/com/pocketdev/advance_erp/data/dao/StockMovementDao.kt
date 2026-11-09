package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

data class StockMovementWithDetails(
    val id: String,
    val movementNumber: String,
    val warehouseId: String,
    val warehouseName: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val variantId: String?,
    val variantSku: String?,
    val movementType: String,
    val quantity: Int,
    val unitCostMinor: Long,
    val balanceAfterMovement: Int = 0,
    val totalCostImpactMinor: Long = 0L,
    val referenceType: String?,
    val referenceId: String?,
    val purchaseOrderId: String? = null,
    val purchaseBatchId: String? = null,
    val salesInvoiceId: String? = null,
    val stockTransferId: String? = null,
    val operatorName: String? = null,
    val notes: String?,
    val createdAt: Long
)

@Dao
interface StockMovementDao {

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        ORDER BY sm.createdAt DESC
        LIMIT :limit
    """)
    fun getRecentMovementsWithDetails(limit: Int = 50): Flow<List<StockMovementWithDetails>>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE sm.createdAt >= :fromMillis AND sm.createdAt <= :toMillis
        ORDER BY sm.createdAt DESC
    """)
    fun getMovementsInRange(fromMillis: Long, toMillis: Long): Flow<List<StockMovementWithDetails>>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE sm.createdAt >= :fromMillis AND sm.createdAt <= :toMillis
        ORDER BY sm.createdAt DESC
    """)
    suspend fun getMovementsInRangeSync(fromMillis: Long, toMillis: Long): List<StockMovementWithDetails>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        ORDER BY sm.createdAt DESC
    """)
    suspend fun getAllMovementsWithDetailsSync(): List<StockMovementWithDetails>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE sm.warehouseId = :warehouseId
        ORDER BY sm.createdAt DESC
        LIMIT :limit
    """)
    fun getRecentMovementsWithDetailsByWarehouse(warehouseId: String, limit: Int = 50): Flow<List<StockMovementWithDetails>>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE sm.productId = :productId
        ORDER BY sm.createdAt DESC
    """)
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementWithDetails>>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE (:productId IS NULL OR sm.productId = :productId)
          AND (:warehouseId IS NULL OR sm.warehouseId = :warehouseId)
          AND (:movementType IS NULL OR sm.movementType = :movementType)
        ORDER BY sm.createdAt DESC
        LIMIT :limit
    """)
    fun getFilteredMovementLedger(
        productId: String? = null,
        warehouseId: String? = null,
        movementType: String? = null,
        limit: Int = 200
    ): Flow<List<StockMovementWithDetails>>

    @Query("""
        SELECT sm.id, sm.movementNumber, sm.warehouseId, w.name as warehouseName,
               sm.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               sm.variantId, pv.sku as variantSku, sm.movementType, sm.quantity, sm.unitCostMinor,
               sm.balanceAfterMovement, sm.totalCostImpactMinor,
               sm.referenceType, sm.referenceId,
               sm.purchaseOrderId, sm.purchaseBatchId, sm.salesInvoiceId, sm.stockTransferId, sm.operatorName,
               sm.notes, sm.createdAt
        FROM stock_movements sm
        INNER JOIN warehouses w ON sm.warehouseId = w.id
        INNER JOIN products p ON sm.productId = p.id
        LEFT JOIN product_variants pv ON sm.variantId = pv.id
        WHERE sm.productId = :productId
        ORDER BY sm.createdAt ASC
    """)
    suspend fun getMovementsForProductSync(productId: String): List<StockMovementWithDetails>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN movementType IN ('PURCHASE_RECEIPT', 'SALE_RETURN', 'STOCK_ADJUSTMENT_IN', 'TRANSFER_IN', 'OPENING_STOCK', 'PURCHASE_RECEIVE') THEN quantity
                WHEN movementType IN ('SALE_DISPATCH', 'PURCHASE_RETURN', 'STOCK_ADJUSTMENT_OUT', 'TRANSFER_OUT', 'DAMAGE_WRITE_OFF', 'SHORTAGE', 'SALE') THEN -quantity
                ELSE 0
            END
        ), 0)
        FROM stock_movements
        WHERE productId = :productId AND warehouseId = :warehouseId
    """)
    suspend fun getNetMovementQuantityForProduct(productId: String, warehouseId: String): Int

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN movementType IN ('PURCHASE_RECEIPT', 'SALE_RETURN', 'STOCK_ADJUSTMENT_IN', 'TRANSFER_IN', 'OPENING_STOCK', 'PURCHASE_RECEIVE') THEN quantity
                WHEN movementType IN ('SALE_DISPATCH', 'PURCHASE_RETURN', 'STOCK_ADJUSTMENT_OUT', 'TRANSFER_OUT', 'DAMAGE_WRITE_OFF', 'SHORTAGE', 'SALE') THEN -quantity
                ELSE 0
            END
        ), 0)
        FROM stock_movements
        WHERE (:warehouseId IS NULL OR warehouseId = :warehouseId)
    """)
    suspend fun getTotalNetMovementQuantity(warehouseId: String? = null): Int

    @Query("SELECT * FROM stock_movements ORDER BY createdAt ASC")
    suspend fun getAllMovementsSync(): List<StockMovement>

    @Query("""
        SELECT COALESCE(SUM(quantity * unitCostMinor), 0)
        FROM stock_movements
        WHERE movementType IN ('SALE_DISPATCH', 'SALE') AND createdAt >= :since
    """)
    fun getSalesDispatchAmountSince(since: Long): Flow<Long>

    @Query("""
        SELECT COUNT(*)
        FROM stock_movements
        WHERE movementType IN ('SALE_DISPATCH', 'SALE') AND createdAt >= :since
    """)
    fun getSalesDispatchCountSince(since: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM stock_movements
        WHERE movementType IN ('SALE_DISPATCH', 'SALE') AND createdAt >= :since
    """)
    fun getSalesDispatchQuantitySince(since: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM stock_movements
        WHERE movementType = 'DAMAGE_WRITE_OFF'
    """)
    fun getTotalDamagedQuantity(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM stock_movements
        WHERE movementType = 'DAMAGE_WRITE_OFF' AND warehouseId = :warehouseId
    """)
    fun getDamagedQuantityByWarehouse(warehouseId: String): Flow<Int>

    @Query("SELECT * FROM stock_movements WHERE movementNumber = :movementNumber LIMIT 1")
    suspend fun getMovementByNumber(movementNumber: String): StockMovement?

    @Query("""
        SELECT * FROM stock_movements 
        WHERE salesInvoiceId = :invoiceId 
        ORDER BY createdAt ASC
    """)
    suspend fun getMovementsForInvoiceSync(invoiceId: String): List<StockMovement>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0) FROM stock_movements
        WHERE salesInvoiceId = :invoiceId 
          AND productId = :productId 
          AND (:variantId IS NULL OR variantId = :variantId)
          AND movementType IN ('SALE_DISPATCH', 'SALE')
    """)
    suspend fun getDispatchedQuantityForInvoiceItem(invoiceId: String, productId: String, variantId: String?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovement)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovements(movements: List<StockMovement>)
}
