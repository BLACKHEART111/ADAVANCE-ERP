package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pocketdev.advance_erp.data.entity.ProductImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductImageDao {

    @Query("SELECT * FROM product_images WHERE productId = :productId AND variantId IS NULL ORDER BY isPrimary DESC, sortOrder ASC, createdAt ASC")
    fun getImagesForProduct(productId: String): Flow<List<ProductImage>>

    @Query("SELECT * FROM product_images WHERE productId = :productId AND variantId IS NULL ORDER BY isPrimary DESC, sortOrder ASC, createdAt ASC")
    suspend fun getImagesForProductSync(productId: String): List<ProductImage>

    @Query("SELECT * FROM product_images WHERE variantId = :variantId ORDER BY isPrimary DESC, sortOrder ASC, createdAt ASC")
    fun getImagesForVariant(variantId: String): Flow<List<ProductImage>>

    @Query("SELECT * FROM product_images WHERE variantId = :variantId ORDER BY isPrimary DESC, sortOrder ASC, createdAt ASC")
    suspend fun getImagesForVariantSync(variantId: String): List<ProductImage>

    @Query("SELECT * FROM product_images WHERE productId = :productId ORDER BY isPrimary DESC, sortOrder ASC")
    fun getAllImagesForProduct(productId: String): Flow<List<ProductImage>>

    @Query("SELECT * FROM product_images WHERE productId = :productId AND variantId IS NULL AND isPrimary = 1 LIMIT 1")
    fun getPrimaryImageForProduct(productId: String): Flow<ProductImage?>

    @Query("SELECT * FROM product_images WHERE variantId = :variantId AND isPrimary = 1 LIMIT 1")
    fun getPrimaryImageForVariant(variantId: String): Flow<ProductImage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ProductImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ProductImage>)

    @Query("DELETE FROM product_images WHERE id = :id")
    suspend fun deleteImage(id: String)

    @Query("DELETE FROM product_images WHERE productId = :productId AND variantId IS NULL")
    suspend fun deleteImagesForProduct(productId: String)

    @Query("DELETE FROM product_images WHERE variantId = :variantId")
    suspend fun deleteImagesForVariant(variantId: String)

    @Query("UPDATE product_images SET isPrimary = 0 WHERE productId = :productId AND variantId IS NULL")
    suspend fun clearProductPrimaryFlag(productId: String)

    @Query("UPDATE product_images SET isPrimary = 0 WHERE variantId = :variantId")
    suspend fun clearVariantPrimaryFlag(variantId: String)

    @Query("UPDATE product_images SET isPrimary = 1 WHERE id = :imageId")
    suspend fun setPrimaryFlag(imageId: String)

    @Transaction
    suspend fun setProductPrimaryImage(productId: String, imageId: String) {
        clearProductPrimaryFlag(productId)
        setPrimaryFlag(imageId)
    }

    @Transaction
    suspend fun setVariantPrimaryImage(variantId: String, imageId: String) {
        clearVariantPrimaryFlag(variantId)
        setPrimaryFlag(imageId)
    }

    @Query("UPDATE product_images SET sortOrder = :sortOrder WHERE id = :imageId")
    suspend fun updateSortOrder(imageId: String, sortOrder: Int)
}
