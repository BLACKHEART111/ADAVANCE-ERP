package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import kotlinx.coroutines.flow.Flow

data class ProductWithDetails(
    val id: String,
    val categoryId: String,
    val categoryNameEn: String,
    val categoryNameBn: String,
    val nameEn: String,
    val nameBn: String,
    val baseSku: String,
    val barcode: String?,
    val qrCode: String? = null,
    val description: String?,
    val unit: String,
    val purchasePriceMinor: Long,
    val sellingPriceMinor: Long,
    val minStockAlert: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val totalStock: Int,
    val variantCount: Int,
    val primaryImageUri: String? = null
)

@Dao
interface ProductDao {

    @Query("""
        SELECT p.id, p.categoryId, c.nameEn as categoryNameEn, c.nameBn as categoryNameBn,
               p.nameEn, p.nameBn, p.baseSku, p.barcode, p.qrCode, p.description, p.unit,
               p.purchasePriceMinor, p.sellingPriceMinor, p.minStockAlert, p.isActive, p.createdAt,
               COALESCE((SELECT SUM(quantity) FROM stock_balances sb WHERE sb.productId = p.id), 0) as totalStock,
               (SELECT COUNT(*) FROM product_variants pv WHERE pv.productId = p.id) as variantCount,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        WHERE p.isActive = 1
        ORDER BY p.createdAt DESC
    """)
    fun getAllProductsWithDetails(): Flow<List<ProductWithDetails>>

    @Query("""
        SELECT p.id, p.categoryId, c.nameEn as categoryNameEn, c.nameBn as categoryNameBn,
               p.nameEn, p.nameBn, p.baseSku, p.barcode, p.qrCode, p.description, p.unit,
               p.purchasePriceMinor, p.sellingPriceMinor, p.minStockAlert, p.isActive, p.createdAt,
               COALESCE((SELECT SUM(quantity) FROM stock_balances sb WHERE sb.productId = p.id), 0) as totalStock,
               (SELECT COUNT(*) FROM product_variants pv WHERE pv.productId = p.id) as variantCount,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        WHERE p.isActive = 1 AND p.categoryId = :categoryId
        ORDER BY p.createdAt DESC
    """)
    fun getProductsByCategory(categoryId: String): Flow<List<ProductWithDetails>>

    @Query("""
        SELECT p.id, p.categoryId, c.nameEn as categoryNameEn, c.nameBn as categoryNameBn,
               p.nameEn, p.nameBn, p.baseSku, p.barcode, p.qrCode, p.description, p.unit,
               p.purchasePriceMinor, p.sellingPriceMinor, p.minStockAlert, p.isActive, p.createdAt,
               COALESCE((SELECT SUM(quantity) FROM stock_balances sb WHERE sb.productId = p.id), 0) as totalStock,
               (SELECT COUNT(*) FROM product_variants pv WHERE pv.productId = p.id) as variantCount,
               (SELECT pi.imageUri FROM product_images pi WHERE pi.productId = p.id AND pi.variantId IS NULL ORDER BY pi.isPrimary DESC, pi.sortOrder ASC LIMIT 1) as primaryImageUri
        FROM products p
        INNER JOIN product_categories c ON p.categoryId = c.id
        WHERE p.isActive = 1 AND (
            p.nameEn LIKE '%' || :query || '%' OR 
            p.nameBn LIKE '%' || :query || '%' OR 
            p.baseSku LIKE '%' || :query || '%' OR 
            p.barcode LIKE '%' || :query || '%' OR
            p.qrCode LIKE '%' || :query || '%'
        )
        ORDER BY p.createdAt DESC
    """)
    fun searchProducts(query: String): Flow<List<ProductWithDetails>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE baseSku = :sku AND isActive = 1 LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE qrCode = :qrCode AND isActive = 1 LIMIT 1")
    suspend fun getProductByQrCode(qrCode: String): Product?

    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE baseSku = :sku AND (:excludeProductId IS NULL OR id != :excludeProductId))")
    suspend fun isBaseSkuTaken(sku: String, excludeProductId: String? = null): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE barcode = :barcode AND (:excludeProductId IS NULL OR id != :excludeProductId))")
    suspend fun isBarcodeTaken(barcode: String, excludeProductId: String? = null): Boolean

    @Query("SELECT baseSku FROM products")
    suspend fun getAllExistingBaseSkus(): List<String>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    fun getProductCount(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(sb.quantity * p.purchasePriceMinor), 0)
        FROM stock_balances sb
        INNER JOIN products p ON sb.productId = p.id
    """)
    fun getTotalStockValuationMinor(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductFlow(id: String): Flow<Product?>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY nameEn ASC")
    suspend fun getAllProductsSync(): List<Product>

    @Query("UPDATE products SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteProduct(id: String)
}
