package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ProductVariant
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVariantDao {

    @Query("SELECT * FROM product_variants WHERE productId = :productId AND isActive = 1")
    fun getVariantsForProduct(productId: String): Flow<List<ProductVariant>>

    @Query("SELECT * FROM product_variants WHERE isActive = 1")
    fun getAllVariants(): Flow<List<ProductVariant>>

    @Query("SELECT * FROM product_variants WHERE isActive = 1")
    suspend fun getAllVariantsSync(): List<ProductVariant>

    @Query("SELECT * FROM product_variants WHERE productId = :productId AND isActive = 1")
    suspend fun getVariantsForProductSync(productId: String): List<ProductVariant>

    @Query("SELECT * FROM product_variants WHERE id = :id LIMIT 1")
    suspend fun getVariantById(id: String): ProductVariant?

    @Query("SELECT * FROM product_variants WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getVariantByBarcode(barcode: String): ProductVariant?

    @Query("SELECT * FROM product_variants WHERE sku = :sku AND isActive = 1 LIMIT 1")
    suspend fun getVariantBySku(sku: String): ProductVariant?

    @Query("SELECT * FROM product_variants WHERE qrCode = :qrCode AND isActive = 1 LIMIT 1")
    suspend fun getVariantByQrCode(qrCode: String): ProductVariant?

    @Query("SELECT EXISTS(SELECT 1 FROM product_variants WHERE sku = :sku AND (:excludeVariantId IS NULL OR id != :excludeVariantId))")
    suspend fun isSkuTaken(sku: String, excludeVariantId: String? = null): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM product_variants WHERE barcode = :barcode AND (:excludeVariantId IS NULL OR id != :excludeVariantId))")
    suspend fun isBarcodeTaken(barcode: String, excludeVariantId: String? = null): Boolean

    @Query("SELECT sku FROM product_variants")
    suspend fun getAllExistingVariantSkus(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariant>)

    @Update
    suspend fun updateVariant(variant: ProductVariant)

    @Query("DELETE FROM product_variants WHERE id = :id")
    suspend fun deleteVariant(id: String)
}
