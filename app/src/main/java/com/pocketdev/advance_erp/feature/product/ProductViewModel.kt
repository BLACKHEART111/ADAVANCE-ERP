package com.pocketdev.advance_erp.feature.product

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.ProductImageStorage
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.InventoryRepository
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.ResolvedProductItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NewVariantDraft(
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val color: String,
    val size: String,
    val costMinor: Long,
    val priceMinor: Long,
    val barcode: String? = null,
    val qrCode: String? = null,
    val photos: List<GalleryItemDraft> = emptyList(),
    val initialStock: Int = 0,
    val existingStock: Int = 0,
    val isExistingVariant: Boolean = false
)

data class ExistingProductRestockBundle(
    val product: ProductWithDetails,
    val productPhotos: List<GalleryItemDraft>,
    val variants: List<NewVariantDraft>,
    val baseStock: Int
)

data class ProductUiState(
    val products: List<ProductWithDetails> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val isLoading: Boolean = false,
    val selectedProductForDetail: ProductWithDetails? = null,
    val selectedProductVariants: List<ProductVariant> = emptyList(),
    val selectedProductImages: List<ProductImage> = emptyList(),
    val preSelectedVariantId: String? = null,
    val variantStockMap: Map<String, Int> = emptyMap(),
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val imageStorage: ProductImageStorage
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _selectedProduct = MutableStateFlow<ProductWithDetails?>(null)
    private val _selectedVariants = MutableStateFlow<List<ProductVariant>>(emptyList())
    private val _selectedProductImages = MutableStateFlow<List<ProductImage>>(emptyList())
    private val _preSelectedVariantId = MutableStateFlow<String?>(null)
    private val _variantStockMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _showAddDialog = MutableStateFlow(false)
    private val _showEditDialog = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val filterParamsFlow = combine(
        combine(_searchQuery, _selectedCategoryId) { q, cat -> Pair(q, cat) },
        combine(preferencesRepository.selectedLanguage, preferencesRepository.selectedCurrency) { l, c -> Pair(l, c) },
        combine(productRepository.getAllCategories(), inventoryRepository.getAllWarehouses()) { cats, whs -> Pair(cats, whs) }
    ) { queryCat, langCurr, catsWhs ->
        val (query, catId) = queryCat
        val (lang, curr) = langCurr
        val (cats, whs) = catsWhs
        FilterParams(query, catId, lang, curr, cats, whs)
    }

    val uiState: StateFlow<ProductUiState> = filterParamsFlow.flatMapLatest { params ->
        val productsFlow = when {
            params.query.isNotBlank() -> productRepository.searchProducts(params.query)
            params.catId != null -> productRepository.getProductsByCategory(params.catId)
            else -> productRepository.getAllProducts()
        }

        val dialogFlow = combine(_showAddDialog, _showEditDialog, _userMessage) { showAdd, showEdit, msg ->
            Triple(showAdd, showEdit, msg)
        }
        val detailFlow = combine(_selectedProduct, _selectedVariants, _selectedProductImages, _preSelectedVariantId, _variantStockMap) { selProd, selVars, selImgs, preVarId, varStock ->
            ProductDetailUiState(selProd, selVars, selImgs, preVarId, varStock)
        }

        combine(
            productsFlow,
            detailFlow,
            dialogFlow
        ) { products, detail, dialog ->
            ProductUiState(
                products = products,
                categories = params.cats,
                warehouses = params.whs,
                selectedCategoryId = params.catId,
                searchQuery = params.query,
                language = params.lang,
                currency = params.curr,
                isLoading = false,
                selectedProductForDetail = detail.selectedProduct,
                selectedProductVariants = detail.selectedVariants,
                selectedProductImages = detail.selectedProductImages,
                preSelectedVariantId = detail.preSelectedVariantId,
                variantStockMap = detail.variantStockMap,
                showAddDialog = dialog.first,
                showEditDialog = dialog.second,
                userMessage = dialog.third
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun openProductDetail(product: ProductWithDetails, preSelectedVariantId: String? = null) {
        _selectedProduct.value = product
        _preSelectedVariantId.value = preSelectedVariantId
        viewModelScope.launch {
            productRepository.getVariantsForProduct(product.id).collect {
                _selectedVariants.value = it
                _variantStockMap.value = try {
                    productRepository.getVariantStockMap(product.id)
                } catch (_: Exception) {
                    emptyMap()
                }
            }
        }
        viewModelScope.launch {
            productRepository.getImagesForProduct(product.id).collect {
                _selectedProductImages.value = it
            }
        }
    }

    fun closeProductDetail() {
        _selectedProduct.value = null
        _selectedVariants.value = emptyList()
        _selectedProductImages.value = emptyList()
        _variantStockMap.value = emptyMap()
        _preSelectedVariantId.value = null
    }

    fun setAddDialogVisible(visible: Boolean) {
        _showAddDialog.value = visible
    }

    fun setEditDialogVisible(visible: Boolean) {
        _showEditDialog.value = visible
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    suspend fun loadVariantImages(variantId: String): List<ProductImage> {
        return productRepository.getImagesForVariantSync(variantId)
    }

    suspend fun loadProductsForCategory(categoryId: String): List<ProductWithDetails> {
        return productRepository.getProductsByCategory(categoryId).firstOrNull() ?: emptyList()
    }

    suspend fun loadProductRestockBundle(productId: String): ExistingProductRestockBundle? {
        val prod = productRepository.getProductWithDetailsById(productId) ?: return null
        val pImgs = productRepository.getImagesForProductSync(productId).map {
            GalleryItemDraft(
                id = it.id,
                uriString = it.imageUri,
                isPrimary = it.isPrimary,
                sortOrder = it.sortOrder
            )
        }
        val pVariants = productRepository.getVariantsForProductSync(productId)
        val varStockMap = try {
            productRepository.getVariantStockMap(productId)
        } catch (_: Exception) {
            emptyMap()
        }
        val variantDrafts = pVariants.map { v ->
            val vImgs = productRepository.getImagesForVariantSync(v.id).map {
                GalleryItemDraft(
                    id = it.id,
                    uriString = it.imageUri,
                    isPrimary = it.isPrimary,
                    sortOrder = it.sortOrder
                )
            }
            NewVariantDraft(
                id = v.id,
                sku = v.sku,
                color = v.color ?: "",
                size = v.size ?: "",
                costMinor = if (v.variantCostMinor > 0L) v.variantCostMinor else prod.purchasePriceMinor,
                priceMinor = if (v.variantPriceMinor > 0L) v.variantPriceMinor else prod.sellingPriceMinor,
                barcode = v.barcode,
                qrCode = v.qrCode,
                photos = vImgs,
                initialStock = 0,
                existingStock = varStockMap[v.id] ?: 0,
                isExistingVariant = true
            )
        }
        return ExistingProductRestockBundle(
            product = prod,
            productPhotos = pImgs,
            variants = variantDrafts,
            baseStock = prod.totalStock
        )
    }

    fun createCategory(nameEn: String, nameBn: String, onCreated: ((ProductCategory) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val cat = productRepository.addCategory(nameEn, nameBn)
                _userMessage.value = "Category '${cat.nameEn}' created"
                onCreated?.invoke(cat)
            } catch (e: Exception) {
                _userMessage.value = "Error creating category: ${e.message}"
            }
        }
    }

    suspend fun getProductCountForCategory(categoryId: String): Int {
        return productRepository.getProductCountForCategory(categoryId)
    }

    fun deleteCategory(categoryId: String, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = productRepository.deleteCategory(categoryId)
            if (res.isSuccess) {
                _userMessage.value = "Category deleted successfully"
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Failed to delete category"
            }
            onResult?.invoke(res)
        }
    }

    fun reassignCategory(fromCategoryId: String, toCategoryId: String, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = productRepository.reassignCategoryAndHardDelete(fromCategoryId, toCategoryId)
            if (res.isSuccess) {
                _userMessage.value = "Products reassigned and category deleted successfully"
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Failed to reassign category"
            }
            onResult?.invoke(res)
        }
    }

    // --- SKU & Barcode Generation Helpers ---

    suspend fun generateBaseSku(categoryId: String, productNameEn: String): String {
        val cat = uiState.value.categories.firstOrNull { it.id == categoryId }
        val catCodeOrName = cat?.code ?: cat?.nameEn ?: "GEN"
        return productRepository.generateUniqueBaseSku(catCodeOrName, productNameEn)
    }

    suspend fun generateVariantSku(baseSku: String, color: String?, size: String?): String {
        return productRepository.generateUniqueVariantSku(baseSku, color, size)
    }

    suspend fun generateBarcode(): String {
        return productRepository.generateUniqueBarcode()
    }

    suspend fun isBaseSkuAvailable(sku: String, excludeId: String? = null): Boolean {
        return productRepository.isBaseSkuAvailable(sku, excludeId)
    }

    suspend fun isVariantSkuAvailable(sku: String, excludeId: String? = null): Boolean {
        return productRepository.isVariantSkuAvailable(sku, excludeId)
    }

    suspend fun isBarcodeAvailable(barcode: String, excludeId: String? = null): Boolean {
        return productRepository.isBarcodeAvailable(barcode, excludeId)
    }

    // --- Product Creation with Photos ---

    fun createProductWithMedia(
        nameEn: String,
        nameBn: String,
        baseSku: String,
        barcode: String?,
        categoryId: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>,
        variants: List<NewVariantDraft>,
        initialStock: Int,
        warehouseId: String
    ) {
        viewModelScope.launch {
            try {
                val productId = UUID.randomUUID().toString()
                val product = Product(
                    id = productId,
                    categoryId = categoryId,
                    nameEn = nameEn.trim(),
                    nameBn = if (nameBn.isNotBlank()) nameBn.trim() else nameEn.trim(),
                    baseSku = baseSku.trim().uppercase(),
                    barcode = barcode?.trim()?.ifBlank { null },
                    qrCode = QrCodeGenerator.createProductQrPayload(productId, baseSku.trim().uppercase()),
                    unit = unit,
                    purchasePriceMinor = purchasePriceMinor,
                    sellingPriceMinor = sellingPriceMinor,
                    minStockAlert = minStockAlert,
                    isActive = true
                )

                // Save product images to app internal storage
                val productImagesList = mutableListOf<ProductImage>()
                for ((idx, draft) in productPhotos.withIndex()) {
                    val localPath = try {
                        imageStorage.saveImageFromUri(Uri.parse(draft.uriString))
                    } catch (_: Exception) {
                        draft.uriString
                    }
                    productImagesList.add(
                        ProductImage(
                            id = UUID.randomUUID().toString(),
                            productId = productId,
                            variantId = null,
                            imageUri = localPath,
                            isPrimary = draft.isPrimary,
                            sortOrder = idx,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // Process variants and variant images
                val variantEntities = mutableListOf<ProductVariant>()
                val variantStock = mutableMapOf<String, Int>()
                val allImages = productImagesList.toMutableList()

                for (v in variants) {
                    val variantId = v.id.ifBlank { UUID.randomUUID().toString() }
                    val effectivePrice = if (v.priceMinor > 0L) v.priceMinor else sellingPriceMinor
                    val effectiveCost = if (v.costMinor > 0L) v.costMinor else purchasePriceMinor
                    val vEntity = ProductVariant(
                        id = variantId,
                        productId = productId,
                        sku = v.sku.trim().uppercase(),
                        color = v.color.trim().ifBlank { null },
                        size = v.size.trim().ifBlank { null },
                        variantPriceMinor = effectivePrice,
                        variantCostMinor = effectiveCost,
                        barcode = v.barcode?.trim()?.ifBlank { null },
                        qrCode = QrCodeGenerator.createVariantQrPayload(variantId, productId, v.sku.trim().uppercase())
                    )
                    variantEntities.add(vEntity)
                    if (v.initialStock > 0) variantStock[variantId] = v.initialStock

                    for ((vIdx, vDraft) in v.photos.withIndex()) {
                        val vLocalPath = try {
                            imageStorage.saveImageFromUri(Uri.parse(vDraft.uriString))
                        } catch (_: Exception) {
                            vDraft.uriString
                        }
                        allImages.add(
                            ProductImage(
                                id = UUID.randomUUID().toString(),
                                productId = productId,
                                variantId = variantId,
                                imageUri = vLocalPath,
                                isPrimary = vDraft.isPrimary,
                                sortOrder = vIdx,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                productRepository.addProductWithMedia(
                    product = product,
                    variants = variantEntities,
                    images = allImages,
                    initialStock = initialStock,
                    warehouseId = warehouseId,
                    variantInitialStock = variantStock
                )

                _showAddDialog.value = false
                _userMessage.value = "Product '${product.nameEn}' created successfully"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create product: ${e.message}"
            }
        }
    }

    // --- Product Updating with Photos ---

    fun updateProductWithMedia(
        productId: String,
        nameEn: String,
        nameBn: String,
        baseSku: String,
        barcode: String?,
        categoryId: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>,
        variants: List<NewVariantDraft>,
        deletedImageIds: List<String>,
        additionalStock: Int = 0,
        warehouseId: String = ""
    ) {
        viewModelScope.launch {
            try {
                val existingProduct = productRepository.getProductById(productId) ?: return@launch
                val updatedProduct = existingProduct.copy(
                    categoryId = categoryId,
                    nameEn = nameEn.trim(),
                    nameBn = if (nameBn.isNotBlank()) nameBn.trim() else nameEn.trim(),
                    baseSku = baseSku.trim().uppercase(),
                    barcode = barcode?.trim()?.ifBlank { null },
                    unit = unit,
                    purchasePriceMinor = purchasePriceMinor,
                    sellingPriceMinor = sellingPriceMinor,
                    minStockAlert = minStockAlert,
                    updatedAt = System.currentTimeMillis()
                )

                val newImages = mutableListOf<ProductImage>()
                for ((idx, draft) in productPhotos.withIndex()) {
                    if (draft.id.startsWith("draft-")) {
                        val localPath = try {
                            imageStorage.saveImageFromUri(Uri.parse(draft.uriString))
                        } catch (_: Exception) {
                            draft.uriString
                        }
                        newImages.add(
                            ProductImage(
                                id = UUID.randomUUID().toString(),
                                productId = productId,
                                variantId = null,
                                imageUri = localPath,
                                isPrimary = draft.isPrimary,
                                sortOrder = idx,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                val variantAddedStock = mutableMapOf<String, Int>()
                val variantEntities = variants.map { v ->
                    val variantId = v.id.ifBlank { UUID.randomUUID().toString() }
                    val effectivePrice = if (v.priceMinor > 0L) v.priceMinor else sellingPriceMinor
                    val effectiveCost = if (v.costMinor > 0L) v.costMinor else purchasePriceMinor
                    if (v.initialStock > 0) {
                        variantAddedStock[variantId] = v.initialStock
                    }
                    for ((vIdx, vDraft) in v.photos.withIndex()) {
                        if (vDraft.id.startsWith("draft-") || !v.isExistingVariant) {
                            val vLocalPath = try {
                                imageStorage.saveImageFromUri(Uri.parse(vDraft.uriString))
                            } catch (_: Exception) {
                                vDraft.uriString
                            }
                            newImages.add(
                                ProductImage(
                                    id = UUID.randomUUID().toString(),
                                    productId = productId,
                                    variantId = variantId,
                                    imageUri = vLocalPath,
                                    isPrimary = vDraft.isPrimary,
                                    sortOrder = vIdx,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    ProductVariant(
                        id = variantId,
                        productId = productId,
                        sku = v.sku.trim().uppercase(),
                        color = v.color.trim().ifBlank { null },
                        size = v.size.trim().ifBlank { null },
                        variantPriceMinor = effectivePrice,
                        variantCostMinor = effectiveCost,
                        barcode = v.barcode?.trim()?.ifBlank { null },
                        qrCode = v.qrCode ?: QrCodeGenerator.createVariantQrPayload(variantId, productId, v.sku.trim().uppercase())
                    )
                }

                val targetWhId = warehouseId.ifBlank {
                    uiState.value.warehouses.firstOrNull()?.id ?: ""
                }

                productRepository.updateProductWithMedia(
                    product = updatedProduct,
                    variants = variantEntities,
                    imagesToInsert = newImages,
                    deletedImageIds = deletedImageIds,
                    variantAddedStock = variantAddedStock,
                    additionalBaseStock = additionalStock,
                    warehouseId = targetWhId
                )

                _showAddDialog.value = false
                _showEditDialog.value = false
                val totalAdded = if (variantAddedStock.isNotEmpty()) variantAddedStock.values.sum() else additionalStock
                _userMessage.value = if (totalAdded > 0) {
                    "Product '${updatedProduct.nameEn}' updated (+$totalAdded ${updatedProduct.unit} added)"
                } else {
                    "Product '${updatedProduct.nameEn}' updated successfully"
                }

                // Refresh selected product if currently opened
                productRepository.getProductWithDetailsById(productId)?.let {
                    _selectedProduct.value = it
                    _variantStockMap.value = try {
                        productRepository.getVariantStockMap(productId)
                    } catch (_: Exception) {
                        emptyMap()
                    }
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to update product: ${e.message}"
            }
        }
    }

    // --- Product Deletion (Soft Delete) ---

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
                closeProductDetail()
                _showEditDialog.value = false
                _userMessage.value = "Product deleted successfully"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete product: ${e.message}"
            }
        }
    }

    // --- Barcode / QR Scanner Code Resolution ---

    fun resolveScannedCode(code: String, onResult: (ResolvedProductItem?) -> Unit) {
        viewModelScope.launch {
            val resolved = productRepository.resolveCode(code)
            onResult(resolved)
        }
    }
}

private data class ProductDetailUiState(
    val selectedProduct: ProductWithDetails?,
    val selectedVariants: List<ProductVariant>,
    val selectedProductImages: List<ProductImage>,
    val preSelectedVariantId: String?,
    val variantStockMap: Map<String, Int> = emptyMap()
)

private data class FilterParams(
    val query: String,
    val catId: String?,
    val lang: AppLanguage,
    val curr: CurrencyCode,
    val cats: List<ProductCategory>,
    val whs: List<Warehouse>
)
