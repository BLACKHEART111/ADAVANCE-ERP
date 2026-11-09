package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ProductCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductCategoryDao {

    @Query("SELECT * FROM product_categories ORDER BY nameEn ASC")
    fun getAllCategories(): Flow<List<ProductCategory>>

    @Query("SELECT * FROM product_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): ProductCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ProductCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ProductCategory>)

    @Update
    suspend fun updateCategory(category: ProductCategory)

    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    suspend fun getProductCountForCategory(categoryId: String): Int

    @Query("DELETE FROM product_categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE products SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId")
    suspend fun reassignCategoryProducts(sourceCategoryId: String, targetCategoryId: String)

    @Query("SELECT * FROM product_categories WHERE code = :code LIMIT 1")
    suspend fun getCategoryByCode(code: String): ProductCategory?
}
