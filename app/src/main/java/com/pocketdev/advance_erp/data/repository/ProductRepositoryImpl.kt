package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.BarcodeGenerator
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.core.util.ProductImageStorage
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.core.util.SkuGenerator
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.ProductCategoryDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductImageDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.ResolvedProductItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val productDao: ProductDao,
    private val categoryDao: ProductCategoryDao,
    private val variantDao: ProductVariantDao,
    private val imageDao: ProductImageDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val imageStorage: ProductImageStorage
) : ProductRepository {

    override fun getAllProducts(): Flow<List<ProductWithDetails>> {
        return productDao.getAllProductsWithDetails()
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<ProductWithDetails>> {
        return productDao.getProductsByCategory(categoryId)
    }

    override fun searchProducts(query: String): Flow<List<ProductWithDetails>> {
        return productDao.searchProducts(query)
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)
    }

    override suspend fun getProductWithDetailsById(id: String): ProductWithDetails? {
        val all = productDao.getAllProductsWithDetails().firstOrNull() ?: emptyList()
        return all.firstOrNull { it.id == id }
    }

    override fun getAllCategories(): Flow<List<ProductCategory>> {
        return categoryDao.getAllCategories()
    }

    override fun getVariantsForProduct(productId: String): Flow<List<ProductVariant>> {
        return variantDao.getVariantsForProduct(productId)
    }

    override suspend fun getVariantsForProductSync(productId: String): List<ProductVariant> {
        return variantDao.getVariantsForProductSync(productId)
    }

    override fun getImagesForProduct(productId: String): Flow<List<ProductImage>> {
        return imageDao.getImagesForProduct(productId)
    }

    override fun getImagesForVariant(variantId: String): Flow<List<ProductImage>> {
        return imageDao.getImagesForVariant(variantId)
    }

    override suspend fun getImagesForProductSync(productId: String): List<ProductImage> {
        return imageDao.getImagesForProductSync(productId)
    }

    override suspend fun getImagesForVariantSync(variantId: String): List<ProductImage> {
        return imageDao.getImagesForVariantSync(variantId)
    }

    override suspend fun addProduct(
        product: Product,
        variants: List<ProductVariant>,
        initialStock: Int,
        warehouseId: String
    ) {
        addProductWithMedia(product, variants, emptyList(), initialStock, warehouseId)
    }

    override suspend fun addProductWithMedia(
        product: Product,
        variants: List<ProductVariant>,
        images: List<ProductImage>,
        initialStock: Int,
        warehouseId: String,
        variantInitialStock: Map<String, Int>
    ) {
        db.withTransaction {
            val ensuredProduct = if (product.qrCode.isNullOrBlank()) {
                product.copy(qrCode = QrCodeGenerator.createProductQrPayload(product.id, product.baseSku))
            } else {
                product
            }
            productDao.insertProduct(ensuredProduct)

            val ensuredVariants = variants.map { v ->
                if (v.qrCode.isNullOrBlank()) {
                    v.copy(qrCode = QrCodeGenerator.createVariantQrPayload(v.id, product.id, v.sku))
                } else {
                    v
                }
            }
            if (ensuredVariants.isNotEmpty()) {
                variantDao.insertVariants(ensuredVariants)
            }

            if (images.isNotEmpty()) {
                imageDao.insertImages(images)
            }

            if (warehouseId.isNotBlank()) {
                val perVariant = ensuredVariants
                    .mapNotNull { v -> (variantInitialStock[v.id] ?: 0).takeIf { it > 0 }?.let { v to it } }
                if (perVariant.isNotEmpty()) {
                    // Opening stock recorded separately for each variant
                    perVariant.forEach { (v, qty) ->
                        val unitCost = if (v.variantCostMinor > 0) v.variantCostMinor else product.purchasePriceMinor
                        recordOpeningStock(product, v.id, v.sku, qty, unitCost, warehouseId)
                    }
                } else if (initialStock > 0) {
                    recordOpeningStock(
                        product,
                        ensuredVariants.firstOrNull()?.id,
                        product.baseSku,
                        initialStock,
                        product.purchasePriceMinor,
                        warehouseId
                    )
                }
            }
        }
    }

    private suspend fun recordOpeningStock(
        product: Product,
        variantId: String?,
        reference: String,
        quantity: Int,
        unitCostMinor: Long,
        warehouseId: String
    ) {
        val now = System.currentTimeMillis()
        val existingBalance = if (variantId != null) {
            stockBalanceDao.getVariantBalance(warehouseId, product.id, variantId)
        } else {
            stockBalanceDao.getBalance(warehouseId, product.id)
        }
        val previousQty = existingBalance?.quantity ?: 0
        val newQty = previousQty + quantity

        val updatedBalance = existingBalance?.copy(
            quantity = newQty,
            updatedAt = now
        ) ?: StockBalance(
            id = UUID.randomUUID().toString(),
            warehouseId = warehouseId,
            productId = product.id,
            variantId = variantId,
            quantity = newQty,
            reservedQuantity = 0,
            updatedAt = now
        )
        stockBalanceDao.insertOrUpdate(updatedBalance)

        stockMovementDao.insertMovement(
            StockMovement(
                id = MovementNumberGenerator.generateId("mov"),
                movementNumber = MovementNumberGenerator.generate("SM-2026"),
                warehouseId = warehouseId,
                productId = product.id,
                variantId = variantId,
                movementType = if (previousQty > 0) StockMovementType.STOCK_ADJUSTMENT_IN.code else StockMovementType.OPENING_STOCK.code,
                quantity = quantity,
                unitCostMinor = unitCostMinor,
                balanceAfterMovement = newQty,
                totalCostImpactMinor = quantity.toLong() * unitCostMinor,
                referenceType = if (previousQty > 0) "RESTOCK_ADD" else "INITIAL_STOCK",
                referenceId = reference,
                notes = if (previousQty > 0) {
                    "Stock added: +$quantity ${product.unit} (Previous: $previousQty, New Total: $newQty)"
                } else {
                    "Initial opening stock recorded"
                },
                createdAt = now
            )
        )

        inventoryCostLayerDao.insertCostLayer(
            InventoryCostLayer(
                id = "icl-${UUID.randomUUID()}",
                productId = product.id,
                variantId = variantId,
                warehouseId = warehouseId,
                shipmentId = if (previousQty > 0) "RESTOCK" else "INIT",
                shipmentNumber = if (previousQty > 0) "STOCK-ADD" else "OPENING-STOCK",
                warehouseReceiptId = if (previousQty > 0) "RESTOCK-${UUID.randomUUID().toString().take(8)}" else "INIT",
                receiptNumber = if (previousQty > 0) "RESTOCK-${now % 1000000}" else "OPENING-STOCK",
                initialQuantity = quantity,
                remainingQuantity = quantity,
                unitLandedCostBdtMinor = unitCostMinor,
                originalCurrency = "BDT",
                originalPurchasePriceMinor = unitCostMinor,
                exchangeRateToBdt = "1.0",
                allocatedExpensePerUnitBdtMinor = 0L,
                receiptDate = now,
                isActive = true,
                createdAt = now
            )
        )
    }

    override suspend fun getVariantStockMap(productId: String): Map<String, Int> {
        return variantDao.getVariantsForProductSync(productId).associate { v ->
            v.id to stockBalanceDao.getTotalVariantQuantity(productId, v.id)
        }
    }

    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    override suspend fun updateProductWithMedia(
        product: Product,
        variants: List<ProductVariant>,
        imagesToInsert: List<ProductImage>,
        deletedImageIds: List<String>,
        variantAddedStock: Map<String, Int>,
        additionalBaseStock: Int,
        warehouseId: String
    ) {
        db.withTransaction {
            productDao.updateProduct(product)

            val ensuredVariants = variants.map { v ->
                if (v.qrCode.isNullOrBlank()) {
                    v.copy(qrCode = QrCodeGenerator.createVariantQrPayload(v.id, product.id, v.sku))
                } else {
                    v
                }
            }

            for (v in ensuredVariants) {
                if (variantDao.getVariantById(v.id) != null) {
                    variantDao.updateVariant(v)
                } else {
                    variantDao.insertVariant(v)
                }
            }

            if (imagesToInsert.isNotEmpty()) {
                imageDao.insertImages(imagesToInsert)
            }

            for (id in deletedImageIds) {
                imageDao.deleteImage(id)
            }

            if (warehouseId.isNotBlank()) {
                val perVariant = ensuredVariants
                    .mapNotNull { v -> (variantAddedStock[v.id] ?: 0).takeIf { it > 0 }?.let { v to it } }
                if (perVariant.isNotEmpty()) {
                    perVariant.forEach { (v, addedQty) ->
                        val unitCost = if (v.variantCostMinor > 0) v.variantCostMinor else product.purchasePriceMinor
                        recordOpeningStock(product, v.id, v.sku, addedQty, unitCost, warehouseId)
                    }
                } else if (additionalBaseStock > 0) {
                    recordOpeningStock(
                        product,
                        ensuredVariants.firstOrNull()?.id,
                        product.baseSku,
                        additionalBaseStock,
                        product.purchasePriceMinor,
                        warehouseId
                    )
                }
            }
        }
    }

    override suspend fun deleteProduct(id: String) {
        productDao.softDeleteProduct(id)
    }

    override suspend fun addCategory(nameEn: String, nameBn: String): ProductCategory {
        val slug = nameEn.trim().lowercase().replace(Regex("[^a-z0-9]"), "-").take(20).ifBlank { "cat" }
        val uniqueCode = "$slug-${UUID.randomUUID().toString().take(4)}"
        val category = ProductCategory(
            id = "cat-" + UUID.randomUUID().toString().take(8),
            nameEn = nameEn.trim(),
            nameBn = nameBn.trim().ifBlank { nameEn.trim() },
            code = uniqueCode
        )
        categoryDao.insertCategory(category)
        return category
    }

    override suspend fun getProductCountForCategory(categoryId: String): Int {
        return categoryDao.getProductCountForCategory(categoryId)
    }

    override suspend fun getOrCreateUncategorizedCategory(): ProductCategory {
        val existing = categoryDao.getCategoryByCode("UNCAT")
        if (existing != null) return existing
        val uncat = ProductCategory(
            id = "cat-uncategorized",
            nameEn = "Uncategorized",
            nameBn = "শ্রেণীবিহীন",
            code = "UNCAT",
            description = "Default fallback category for unassigned products"
        )
        categoryDao.insertCategory(uncat)
        return uncat
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val cat = categoryDao.getCategoryById(categoryId)
                ?: return Result.failure(IllegalArgumentException("Category not found: $categoryId"))
            if (cat.code.equals("UNCAT", ignoreCase = true) || cat.id == "cat-uncategorized") {
                return Result.failure(IllegalStateException("Cannot delete system default Uncategorized category."))
            }
            val linkedCount = categoryDao.getProductCountForCategory(categoryId)
            if (linkedCount > 0) {
                return Result.failure(
                    IllegalStateException("Cannot delete category '${cat.nameEn}'. It has $linkedCount linked products. Please reassign them first.")
                )
            }
            categoryDao.deleteCategory(categoryId)
            db.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "CATEGORY",
                    recordId = categoryId,
                    action = "DELETE",
                    details = "Deleted empty category '${cat.nameEn}' (${cat.code})",
                    operator = "SYSTEM_ADMIN",
                    documentNumber = cat.code,
                    operationType = "HARD_DELETE",
                    previousStatus = "ACTIVE",
                    newStatus = "DELETED"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reassignCategoryAndHardDelete(fromCategoryId: String, toCategoryId: String): Result<Unit> {
        return try {
            val fromCat = categoryDao.getCategoryById(fromCategoryId)
                ?: return Result.failure(IllegalArgumentException("Source category not found: $fromCategoryId"))
            if (fromCat.code.equals("UNCAT", ignoreCase = true) || fromCat.id == "cat-uncategorized") {
                return Result.failure(IllegalStateException("Cannot delete system default Uncategorized category."))
            }
            val toCat = categoryDao.getCategoryById(toCategoryId)
                ?: return Result.failure(IllegalArgumentException("Target category not found: $toCategoryId"))

            db.withTransaction {
                val linkedCount = categoryDao.getProductCountForCategory(fromCategoryId)
                categoryDao.reassignCategoryProducts(fromCategoryId, toCategoryId)
                categoryDao.deleteCategory(fromCategoryId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CATEGORY",
                        recordId = fromCategoryId,
                        action = "REASSIGN_AND_DELETE",
                        details = "Reassigned $linkedCount products from '${fromCat.nameEn}' to '${toCat.nameEn}' and deleted category.",
                        operator = "SYSTEM_ADMIN",
                        documentNumber = fromCat.code,
                        operationType = "REASSIGN_DELETE",
                        previousStatus = "ACTIVE",
                        newStatus = "DELETED"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Photo Management ---

    override suspend fun insertImage(image: ProductImage) {
        imageDao.insertImage(image)
    }

    override suspend fun setPrimaryProductImage(productId: String, imageId: String) {
        imageDao.setProductPrimaryImage(productId, imageId)
    }

    override suspend fun setPrimaryVariantImage(variantId: String, imageId: String) {
        imageDao.setVariantPrimaryImage(variantId, imageId)
    }

    override suspend fun deleteImage(imageId: String) {
        imageDao.deleteImage(imageId)
    }

    override suspend fun reorderImages(images: List<ProductImage>) {
        db.withTransaction {
            images.forEachIndexed { index, img ->
                imageDao.updateSortOrder(img.id, index)
            }
        }
    }

    // --- SKU & Barcode Generation & Validation ---

    override suspend fun isBaseSkuAvailable(sku: String, excludeProductId: String?): Boolean {
        if (!SkuGenerator.isValidSku(sku)) return false
        val taken = productDao.isBaseSkuTaken(sku.trim().uppercase(), excludeProductId)
        return !taken
    }

    override suspend fun isVariantSkuAvailable(sku: String, excludeVariantId: String?): Boolean {
        if (!SkuGenerator.isValidSku(sku)) return false
        val taken = variantDao.isSkuTaken(sku.trim().uppercase(), excludeVariantId)
        return !taken
    }

    override suspend fun isBarcodeAvailable(barcode: String, excludeId: String?): Boolean {
        val clean = barcode.trim()
        if (clean.isBlank()) return true
        val takenProduct = productDao.isBarcodeTaken(clean, excludeId)
        val takenVariant = variantDao.isBarcodeTaken(clean, excludeId)
        return !takenProduct && !takenVariant
    }

    override suspend fun generateUniqueBaseSku(categoryCodeOrName: String, productNameEn: String): String {
        val existing = productDao.getAllExistingBaseSkus().toSet()
        return SkuGenerator.generateProductSku(categoryCodeOrName, productNameEn, existing)
    }

    override suspend fun generateUniqueVariantSku(baseSku: String, color: String?, size: String?): String {
        val existing = variantDao.getAllExistingVariantSkus().toSet()
        return SkuGenerator.generateVariantSku(baseSku, color, size, existing)
    }

    override suspend fun generateUniqueBarcode(): String {
        while (true) {
            val candidate = BarcodeGenerator.generateStandardBarcode()
            if (isBarcodeAvailable(candidate)) {
                return candidate
            }
        }
    }

    // --- Code Resolution for Scanner & Search ---

    override suspend fun resolveCode(code: String): ResolvedProductItem? {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return null

        // 1. Try QR Structured Payload
        val qr = QrCodeGenerator.parsePayload(trimmed)
        if (qr.type == QrCodeGenerator.QrType.PRODUCT) {
            val prod = getProductWithDetailsById(qr.targetId)
                ?: productDao.getProductBySku(qr.targetId)?.let { getProductWithDetailsById(it.id) }
                ?: (if (!qr.code.isNullOrBlank()) productDao.getProductBySku(qr.code)?.let { getProductWithDetailsById(it.id) } else null)
                ?: productDao.getProductByQrCode(trimmed)?.let { getProductWithDetailsById(it.id) }

            if (prod != null) {
                return ResolvedProductItem(prod, null, trimmed, "QR_PRODUCT")
            }
        } else if (qr.type == QrCodeGenerator.QrType.VARIANT) {
            val variant = variantDao.getVariantById(qr.targetId)
                ?: variantDao.getVariantBySku(qr.targetId)
                ?: (if (!qr.code.isNullOrBlank()) variantDao.getVariantBySku(qr.code) else null)
                ?: variantDao.getVariantByQrCode(trimmed)

            if (variant != null) {
                val prod = getProductWithDetailsById(variant.productId)
                if (prod != null) {
                    return ResolvedProductItem(prod, variant, trimmed, "QR_VARIANT")
                }
            } else if (!qr.secondaryId.isNullOrBlank()) {
                val prod = getProductWithDetailsById(qr.secondaryId)
                if (prod != null) {
                    val prodVariants = variantDao.getVariantsForProductSync(prod.id)
                    val matchedVar = prodVariants.firstOrNull { 
                        it.id == qr.targetId || (qr.code != null && it.sku.equals(qr.code, ignoreCase = true))
                    }
                    return ResolvedProductItem(prod, matchedVar, trimmed, "QR_VARIANT")
                }
            }
        }

        val normalized = Formatters.toEnglishDigits(trimmed)

        // 2. Try Exact Product Barcode
        val prodByBarcode = productDao.getProductByBarcode(trimmed)
            ?: (if (normalized != trimmed) productDao.getProductByBarcode(normalized) else null)
        if (prodByBarcode != null) {
            val details = getProductWithDetailsById(prodByBarcode.id)
            if (details != null) {
                return ResolvedProductItem(details, null, trimmed, "PRODUCT_BARCODE")
            }
        }

        // 3. Try Exact Variant Barcode
        val variantByBarcode = variantDao.getVariantByBarcode(trimmed)
            ?: (if (normalized != trimmed) variantDao.getVariantByBarcode(normalized) else null)
        if (variantByBarcode != null) {
            val details = getProductWithDetailsById(variantByBarcode.productId)
            if (details != null) {
                return ResolvedProductItem(details, variantByBarcode, trimmed, "VARIANT_BARCODE")
            }
        }

        // 4. Try Exact Product Base SKU
        val prodBySku = productDao.getProductBySku(trimmed)
            ?: productDao.getProductBySku(trimmed.uppercase())
            ?: (if (normalized != trimmed) productDao.getProductBySku(normalized) ?: productDao.getProductBySku(normalized.uppercase()) else null)
        if (prodBySku != null) {
            val details = getProductWithDetailsById(prodBySku.id)
            if (details != null) {
                return ResolvedProductItem(details, null, trimmed, "PRODUCT_SKU")
            }
        }

        // 5. Try Exact Variant SKU
        val variantBySku = variantDao.getVariantBySku(trimmed)
            ?: variantDao.getVariantBySku(trimmed.uppercase())
            ?: (if (normalized != trimmed) variantDao.getVariantBySku(normalized) ?: variantDao.getVariantBySku(normalized.uppercase()) else null)
        if (variantBySku != null) {
            val details = getProductWithDetailsById(variantBySku.productId)
            if (details != null) {
                return ResolvedProductItem(details, variantBySku, trimmed, "VARIANT_SKU")
            }
        }

        // 6. Try Exact QR Code Field on Product or Variant
        val prodByQr = productDao.getProductByQrCode(trimmed)
        if (prodByQr != null) {
            val details = getProductWithDetailsById(prodByQr.id)
            if (details != null) {
                return ResolvedProductItem(details, null, trimmed, "PRODUCT_QR")
            }
        }

        val variantByQr = variantDao.getVariantByQrCode(trimmed)
        if (variantByQr != null) {
            val details = getProductWithDetailsById(variantByQr.productId)
            if (details != null) {
                return ResolvedProductItem(details, variantByQr, trimmed, "VARIANT_QR")
            }
        }

        return null
    }

    override suspend fun getVariantStockQuantity(productId: String, variantId: String): Int {
        return try {
            stockBalanceDao.getTotalVariantQuantity(productId, variantId)
        } catch (_: Exception) {
            0
        }
    }
}
