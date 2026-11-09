package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.StockBalance
import kotlinx.coroutines.flow.Flow

data class StockItemOverview(
    val productId: String,
    val categoryId: String = "",
    val barcode: String? = null,
    val variantSkus: String? = null,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val categoryNameEn: String,
    val unit: String,
    val minStockAlert: Int,
    val totalQuantity: Int,
    val totalReserved: Int,
    val availableQuantity: Int,
    val isLowStock: Boolean,
    val primaryImageUri: String? = null
)

@Dao
interface StockBalanceDao {

    @Query("""
        SELECT p.id as productId, p.categoryId as categoryId, p.barcode as barcode,
               (SELECT GROUP_CONCAT(pv.sku, ', ') FROM product_variants pv WHERE pv.productId = p.id) as variantSkus,
               p.nameEn as productNameEn, p.nameBn as productNameBn,
               p.baseSku as baseSku, c.nameEn as categoryNameEn, p.unit as unit,
               p.minStockAlert as minStockAlert,
               COALESCE(SUM(sb.quantity), 0) as totalQuantity,
               COALESCE(SUM(sb.reservedQuantity), 0) as totalReserved,
               COALESCE(SUM(sb.quantity - sb.reservedQuantity), 0) as availableQuantity,
               CASE WHEN COALESCE(SUM(sb.quantity), 0) <= p.minStockAlert THEN 1 ELSE 0 END as isLowStock,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        LEFT JOIN stock_balances sb ON sb.productId = p.id
        WHERE p.isActive = 1
        GROUP BY p.id
        ORDER BY isLowStock DESC, totalQuantity ASC
    """)
    fun getStockOverview(): Flow<List<StockItemOverview>>

    @Query("""
        SELECT p.id as productId, p.categoryId as categoryId, p.barcode as barcode,
               (SELECT GROUP_CONCAT(pv.sku, ', ') FROM product_variants pv WHERE pv.productId = p.id) as variantSkus,
               p.nameEn as productNameEn, p.nameBn as productNameBn,
               p.baseSku as baseSku, c.nameEn as categoryNameEn, p.unit as unit,
               p.minStockAlert as minStockAlert,
               COALESCE(SUM(sb.quantity), 0) as totalQuantity,
               COALESCE(SUM(sb.reservedQuantity), 0) as totalReserved,
               COALESCE(SUM(sb.quantity - sb.reservedQuantity), 0) as availableQuantity,
               CASE WHEN COALESCE(SUM(sb.quantity), 0) <= p.minStockAlert THEN 1 ELSE 0 END as isLowStock,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        LEFT JOIN stock_balances sb ON sb.productId = p.id AND sb.warehouseId = :warehouseId
        WHERE p.isActive = 1
        GROUP BY p.id
        ORDER BY isLowStock DESC, totalQuantity ASC
    """)
    fun getStockOverviewByWarehouse(warehouseId: String): Flow<List<StockItemOverview>>

    @Query("""
        SELECT p.id as productId, p.categoryId as categoryId, p.barcode as barcode,
               (SELECT GROUP_CONCAT(pv.sku, ', ') FROM product_variants pv WHERE pv.productId = p.id) as variantSkus,
               p.nameEn as productNameEn, p.nameBn as productNameBn,
               p.baseSku as baseSku, c.nameEn as categoryNameEn, p.unit as unit,
               p.minStockAlert as minStockAlert,
               COALESCE(SUM(sb.quantity), 0) as totalQuantity,
               COALESCE(SUM(sb.reservedQuantity), 0) as totalReserved,
               COALESCE(SUM(sb.quantity - sb.reservedQuantity), 0) as availableQuantity,
               1 as isLowStock,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        LEFT JOIN stock_balances sb ON sb.productId = p.id
        WHERE p.isActive = 1
        GROUP BY p.id
        HAVING COALESCE(SUM(sb.quantity), 0) <= p.minStockAlert
        ORDER BY totalQuantity ASC
    """)
    fun getLowStockItems(): Flow<List<StockItemOverview>>

    @Query("SELECT * FROM stock_balances WHERE warehouseId = :warehouseId AND productId = :productId LIMIT 1")
    suspend fun getBalance(warehouseId: String, productId: String): StockBalance?

    @Query("SELECT * FROM stock_balances WHERE warehouseId = :warehouseId AND productId = :productId AND variantId = :variantId LIMIT 1")
    suspend fun getVariantBalance(warehouseId: String, productId: String, variantId: String): StockBalance?

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_balances WHERE productId = :productId AND variantId = :variantId")
    suspend fun getTotalVariantQuantity(productId: String, variantId: String): Int

    @Query("SELECT * FROM stock_balances")
    fun getAllBalances(): Flow<List<StockBalance>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_balances")
    fun getTotalPhysicalQuantity(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_balances WHERE warehouseId = :warehouseId")
    fun getPhysicalQuantityByWarehouse(warehouseId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity - reservedQuantity), 0) FROM stock_balances")
    fun getTotalSellableQuantity(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity - reservedQuantity), 0) FROM stock_balances WHERE warehouseId = :warehouseId")
    fun getSellableQuantityByWarehouse(warehouseId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(balance: StockBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(balances: List<StockBalance>)
}
