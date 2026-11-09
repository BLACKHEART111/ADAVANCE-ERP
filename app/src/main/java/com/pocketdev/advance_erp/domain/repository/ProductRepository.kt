package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import kotlinx.coroutines.flow.Flow

data class ResolvedProductItem(
    val product: ProductWithDetails,
    val matchedVariant: ProductVariant? = null,
    val matchedCode: String,
    val matchType: String
)

interface ProductRepository {
    fun getAllProducts(): Flow<List<ProductWithDetails>>
    fun getProductsByCategory(categoryId: String): Flow<List<ProductWithDetails>>
    fun searchProducts(query: String): Flow<List<ProductWithDetails>>
    suspend fun getProductById(id: String): Product?
    suspend fun getProductWithDetailsById(id: String): ProductWithDetails?
    fun getAllCategories(): Flow<List<ProductCategory>>
    fun getVariantsForProduct(productId: String): Flow<List<ProductVariant>>
    suspend fun getVariantsForProductSync(productId: String): List<ProductVariant>
    fun getImagesForProduct(productId: String): Flow<List<ProductImage>>
    fun getImagesForVariant(variantId: String): Flow<List<ProductImage>>
    suspend fun getImagesForProductSync(productId: String): List<ProductImage>
    suspend fun getImagesForVariantSync(variantId: String): List<ProductImage>

    suspend fun addProduct(
        product: Product,
        variants: List<ProductVariant>,
        initialStock: Int,
        warehouseId: String
    )

    suspend fun addProductWithMedia(
        product: Product,
        variants: List<ProductVariant>,
        images: List<ProductImage>,
        initialStock: Int,
        warehouseId: String,
        variantInitialStock: Map<String, Int> = emptyMap()
    )

    /** Returns total on-hand quantity per variant (variantId -> qty) across all warehouses. */
    suspend fun getVariantStockMap(productId: String): Map<String, Int>

    suspend fun updateProduct(product: Product)

    suspend fun updateProductWithMedia(
        product: Product,
        variants: List<ProductVariant>,
        imagesToInsert: List<ProductImage>,
        deletedImageIds: List<String>,
        variantAddedStock: Map<String, Int> = emptyMap(),
        additionalBaseStock: Int = 0,
        warehouseId: String = ""
    )

    suspend fun deleteProduct(id: String)
    suspend fun addCategory(nameEn: String, nameBn: String): ProductCategory
    suspend fun getProductCountForCategory(categoryId: String): Int
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun reassignCategoryAndHardDelete(fromCategoryId: String, toCategoryId: String): Result<Unit>
    suspend fun getOrCreateUncategorizedCategory(): ProductCategory

    // Photo Management
    suspend fun insertImage(image: ProductImage)
    suspend fun setPrimaryProductImage(productId: String, imageId: String)
    suspend fun setPrimaryVariantImage(variantId: String, imageId: String)
    suspend fun deleteImage(imageId: String)
    suspend fun reorderImages(images: List<ProductImage>)

    // SKU & Barcode Generation & Validation
    suspend fun isBaseSkuAvailable(sku: String, excludeProductId: String? = null): Boolean
    suspend fun isVariantSkuAvailable(sku: String, excludeVariantId: String? = null): Boolean
    suspend fun isBarcodeAvailable(barcode: String, excludeId: String? = null): Boolean
    suspend fun generateUniqueBaseSku(categoryCodeOrName: String, productNameEn: String): String
    suspend fun generateUniqueVariantSku(baseSku: String, color: String?, size: String?): String
    suspend fun generateUniqueBarcode(): String

    // Code Resolution for Scanner & Search
    suspend fun resolveCode(code: String): ResolvedProductItem?

    // Variant Stock Balance
    suspend fun getVariantStockQuantity(productId: String, variantId: String): Int
}
