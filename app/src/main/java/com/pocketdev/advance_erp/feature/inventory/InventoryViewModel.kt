package com.pocketdev.advance_erp.feature.inventory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.InventoryPdfGenerator
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.FifoConsumptionWithDetails
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentWithDetails
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationItemWithDetails
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.dao.StockMovementWithDetails
import com.pocketdev.advance_erp.data.dao.StockTransferWithDetails
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.CostHistoryRepository
import com.pocketdev.advance_erp.domain.repository.InventoryRepository
import com.pocketdev.advance_erp.domain.repository.InventoryValuationBreakdown
import com.pocketdev.advance_erp.domain.repository.ProductCostSummary
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.StockTransferDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InventoryTab {
    STOCK_OVERVIEW,
    MOVEMENTS,
    VALUATION,
    RECONCILIATION,
    COGS_TRACE,
    LOW_STOCK,
    TRANSFERS
}

enum class InventorySortOption(val titleEn: String, val titleBn: String) {
    NAME_ASC("Product Name (A-Z)", "পণ্যের নাম (অ-হ)"),
    NAME_DESC("Product Name (Z-A)", "পণ্যের নাম (হ-অ)"),
    SKU_ASC("SKU (A-Z)", "SKU কোড"),
    QUANTITY_DESC("Available Qty (High to Low)", "বিক্রয়যোগ্য পরিমাণ (বেশি থেকে কম)"),
    QUANTITY_ASC("Available Qty (Low to High)", "বিক্রয়যোগ্য পরিমাণ (কম থেকে বেশি)")
}

enum class StockStatusFilter(val titleEn: String, val titleBn: String) {
    ALL("All Stock", "সকল স্টক"),
    IN_STOCK("In Stock / Available", "মজুদ আছে"),
    LOW_STOCK("Low Stock Alert", "কম স্টক সতর্কতা"),
    OUT_OF_STOCK("Out of Stock (Zero)", "স্টক শেষ (০)")
}

data class InventoryUiState(
    val selectedTab: InventoryTab = InventoryTab.STOCK_OVERVIEW,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<ProductCategory> = emptyList(),
    val stockItems: List<StockItemOverview> = emptyList(),
    val movements: List<StockMovementWithDetails> = emptyList(),
    val lowStockItems: List<StockItemOverview> = emptyList(),
    val transfers: List<StockTransferWithDetails> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val allProducts: List<ProductWithDetails> = emptyList(),
    val selectedWarehouseId: String? = null,
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val showAdjustDialog: Boolean = false,
    val showTransferDialog: Boolean = false,
    val userMessage: String? = null,
    // Inventory Valuation Summary Card metrics
    val totalInventoryQuantity: Int = 0,
    val totalSellableStock: Int = 0,
    val totalDamagedStock: Int = 0,
    val totalInventoryValueMinor: Long = 0L,
    // Phase 06 Part 2 Additions
    val showScannerDialog: Boolean = false,
    val selectedProductForDetail: ProductWithDetails? = null,
    val selectedProductVariants: List<ProductVariant> = emptyList(),
    val selectedProductImages: List<ProductImage> = emptyList(),
    val preSelectedVariantId: String? = null,
    // Phase 06 Part 3 Additions
    val valuationBreakdown: InventoryValuationBreakdown? = null,
    val latestReconciliation: InventoryReconciliation? = null,
    val reconciliationItems: List<InventoryReconciliationItemWithDetails> = emptyList(),
    val fifoConsumptions: List<FifoConsumptionWithDetails> = emptyList(),
    val totalCogsMinor: Long = 0L,
    val selectedProductCostSummary: ProductCostSummary? = null,
    val selectedProductCostLayers: List<InventoryCostLayer> = emptyList(),
    val selectedProductCostAdjustments: List<InventoryCostAdjustmentWithDetails> = emptyList(),
    val showCostHistoryDialog: Boolean = false,
    val showCostAdjustmentDialog: Boolean = false,
    val selectedLayerForAdjustment: InventoryCostLayer? = null,
    val isReconciling: Boolean = false,
    // Compact Redesign Additions
    val sortOption: InventorySortOption = InventorySortOption.NAME_ASC,
    val stockStatusFilter: StockStatusFilter = StockStatusFilter.ALL,
    val userRole: UserRole = UserRole.OWNER_ADMIN,
    val canViewCost: Boolean = true,
    // Variant Stock
    val variantStockMap: Map<String, Int> = emptyMap()
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val costHistoryRepository: CostHistoryRepository,
    private val businessProfileDao: BusinessProfileDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val stockBalanceDao: StockBalanceDao
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(InventoryTab.STOCK_OVERVIEW)
    private val _selectedWarehouseId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _showAdjustDialog = MutableStateFlow(false)
    private val _showTransferDialog = MutableStateFlow(false)
    private val _showScannerDialog = MutableStateFlow(false)
    private val _selectedProductForDetail = MutableStateFlow<ProductWithDetails?>(null)
    private val _selectedProductVariants = MutableStateFlow<List<ProductVariant>>(emptyList())
    private val _selectedProductImages = MutableStateFlow<List<ProductImage>>(emptyList())
    private val _preSelectedVariantId = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(InventorySortOption.NAME_ASC)
    private val _stockStatusFilter = MutableStateFlow(StockStatusFilter.ALL)

    // Phase 06 Part 3 Flows
    private val _showCostHistoryDialog = MutableStateFlow(false)
    private val _showCostAdjustmentDialog = MutableStateFlow(false)
    private val _selectedLayerForAdjustment = MutableStateFlow<InventoryCostLayer?>(null)
    private val _isReconciling = MutableStateFlow(false)
    private val _selectedCostSummary = MutableStateFlow<ProductCostSummary?>(null)
    private val _selectedCostLayers = MutableStateFlow<List<InventoryCostLayer>>(emptyList())
    private val _selectedCostAdjustments = MutableStateFlow<List<InventoryCostAdjustmentWithDetails>>(emptyList())
    private val _variantStockMap = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val filterParamsFlow = combine(
        combine(_selectedTab, _selectedWarehouseId, _searchQuery) { tab, whId, query ->
            Triple(tab, whId, query)
        },
        combine(_selectedCategoryId, preferencesRepository.selectedLanguage, preferencesRepository.selectedCurrency) { catId, lang, curr ->
            Triple(catId, lang, curr)
        },
        combine(inventoryRepository.getAllWarehouses(), productRepository.getAllProducts(), productRepository.getAllCategories()) { whs, prods, cats ->
            Triple(whs, prods, cats)
        },
        combine(_showAdjustDialog, _showTransferDialog, _showScannerDialog, _userMessage) { showAdj, showTr, showScan, msg ->
            DialogFlags(showAdj, showTr, showScan, msg)
        },
        combine(
            combine(_selectedProductForDetail, _selectedProductVariants, _selectedProductImages, _preSelectedVariantId, _variantStockMap) { prod, vars, imgs, preVarId, varStock ->
                ProductDetailState(prod, vars, imgs, preVarId, varStock)
            },
            combine(_sortOption, _stockStatusFilter, preferencesRepository.selectedUserRole) { sort, status, role ->
                Triple(sort, status, role)
            }
        ) { detailState, (sort, status, role) ->
            Pair(detailState, Triple(sort, status, role))
        }
    ) { p1, p2, p3, p4, p5 ->
        val (tab, whId, query) = p1
        val (catId, lang, curr) = p2
        val (whs, prods, cats) = p3
        val (showAdj, showTr, showScan, msg) = p4
        val (detailState, sortRole) = p5
        val (prod, vars, imgs, preVarId) = detailState
        val (sort, statusFilter, role) = sortRole
        InventoryFilterParams(
            tab = tab,
            whId = whId,
            searchQuery = query,
            catId = catId,
            lang = lang,
            curr = curr,
            whs = whs,
            prods = prods,
            cats = cats,
            showAdj = showAdj,
            showTr = showTr,
            showScan = showScan,
            msg = msg,
            selectedProduct = prod,
            variants = vars,
            images = imgs,
            preSelectedVariantId = preVarId,
            sortOption = sort,
            stockStatusFilter = statusFilter,
            userRole = role,
            variantStock = detailState.variantStock
        )
    }

    val uiState: StateFlow<InventoryUiState> = filterParamsFlow.flatMapLatest { p ->
        val stockFlow = if (p.whId != null) {
            inventoryRepository.getStockOverviewByWarehouse(p.whId)
        } else {
            inventoryRepository.getStockOverview()
        }

        val movementsFlow = costHistoryRepository.getFilteredMovementLedger(
            productId = null,
            warehouseId = p.whId,
            movementType = null,
            limit = 100
        )

        val transfersFlow = if (p.whId != null) {
            inventoryRepository.getStockTransfersByWarehouse(p.whId)
        } else {
            inventoryRepository.getAllStockTransfers()
        }

        val totalPhysicalQtyFlow = if (p.whId != null) {
            inventoryRepository.getPhysicalQuantityByWarehouse(p.whId)
        } else {
            inventoryRepository.getTotalPhysicalQuantity()
        }

        val totalSellableQtyFlow = if (p.whId != null) {
            inventoryRepository.getSellableQuantityByWarehouse(p.whId)
        } else {
            inventoryRepository.getTotalSellableQuantity()
        }

        val totalDamagedQtyFlow = if (p.whId != null) {
            inventoryRepository.getDamagedQuantityByWarehouse(p.whId)
        } else {
            inventoryRepository.getTotalDamagedQuantity()
        }

        val valuationFlow = if (p.whId != null) {
            inventoryRepository.getInventoryValuationByWarehouseMinor(p.whId)
        } else {
            inventoryRepository.getTotalInventoryValuationMinor()
        }

        val valuationBreakdownFlow = costHistoryRepository.getInventoryValuationBreakdown()
        val reconciliationsFlow = costHistoryRepository.getAllReconciliations()
        val consumptionsFlow = costHistoryRepository.getAllFifoConsumptions(100)
        val totalCogsFlow = costHistoryRepository.getTotalCogsMinor()

        val metricsFlow = combine(
            totalPhysicalQtyFlow,
            totalSellableQtyFlow,
            totalDamagedQtyFlow,
            valuationFlow
        ) { physical, sellable, damaged, valuation ->
            InventoryMetrics(physical, sellable, damaged, valuation)
        }

        val auditStateFlow = combine(
            valuationBreakdownFlow,
            reconciliationsFlow,
            consumptionsFlow,
            totalCogsFlow
        ) { breakdown, reconciliations, consumptions, cogs ->
            AuditData(
                valuationBreakdown = breakdown,
                latestReconciliation = reconciliations.firstOrNull(),
                consumptions = consumptions,
                totalCogs = cogs
            )
        }

        val costHistoryDialogStateFlow = combine(
            _showCostHistoryDialog,
            _showCostAdjustmentDialog,
            _selectedLayerForAdjustment,
            _selectedCostSummary,
            combine(_selectedCostLayers, _selectedCostAdjustments, _isReconciling) { layers, adjs, isRec ->
                Triple(layers, adjs, isRec)
            }
        ) { showHist, showAdj, layer, summary, (layers, adjs, isRec) ->
            CostDialogState(
                showHistory = showHist,
                showAdjustment = showAdj,
                selectedLayer = layer,
                summary = summary,
                layers = layers,
                adjustments = adjs,
                isReconciling = isRec
            )
        }

        combine(
            stockFlow,
            movementsFlow,
            inventoryRepository.getLowStockItems(),
            transfersFlow,
            combine(metricsFlow, auditStateFlow, costHistoryDialogStateFlow) { m, a, c ->
                Triple(m, a, c)
            }
        ) { rawStock, movements, rawLowStock, transfers, (metrics, auditData, costState) ->
            // Filter products by search query and category
            val query = p.searchQuery.trim().lowercase()
            val textMatchedStock = rawStock.filter { item ->
                val matchesCategory = p.catId == null || item.categoryId == p.catId
                val matchesSearch = query.isEmpty() ||
                        item.productNameEn.lowercase().contains(query) ||
                        item.productNameBn.lowercase().contains(query) ||
                        item.baseSku.lowercase().contains(query) ||
                        (item.barcode?.lowercase()?.contains(query) == true) ||
                        (item.variantSkus?.lowercase()?.contains(query) == true)
                matchesCategory && matchesSearch
            }

            // Apply Stock Status Filter
            val statusFilteredStock = textMatchedStock.filter { item ->
                when (p.stockStatusFilter) {
                    StockStatusFilter.ALL -> true
                    StockStatusFilter.IN_STOCK -> item.totalQuantity > 0
                    StockStatusFilter.LOW_STOCK -> item.isLowStock
                    StockStatusFilter.OUT_OF_STOCK -> item.totalQuantity == 0
                }
            }

            // Apply Sorting
            val sortedStock = when (p.sortOption) {
                InventorySortOption.NAME_ASC -> statusFilteredStock.sortedBy { if (p.lang == AppLanguage.BANGLA) it.productNameBn.lowercase() else it.productNameEn.lowercase() }
                InventorySortOption.NAME_DESC -> statusFilteredStock.sortedByDescending { if (p.lang == AppLanguage.BANGLA) it.productNameBn.lowercase() else it.productNameEn.lowercase() }
                InventorySortOption.SKU_ASC -> statusFilteredStock.sortedBy { it.baseSku.lowercase() }
                InventorySortOption.QUANTITY_DESC -> statusFilteredStock.sortedByDescending { it.availableQuantity }
                InventorySortOption.QUANTITY_ASC -> statusFilteredStock.sortedBy { it.availableQuantity }
            }

            val filteredLowStock = rawLowStock.filter { item ->
                val matchesCategory = p.catId == null || item.categoryId == p.catId
                val matchesSearch = query.isEmpty() ||
                        item.productNameEn.lowercase().contains(query) ||
                        item.productNameBn.lowercase().contains(query) ||
                        item.baseSku.lowercase().contains(query) ||
                        (item.barcode?.lowercase()?.contains(query) == true) ||
                        (item.variantSkus?.lowercase()?.contains(query) == true)
                matchesCategory && matchesSearch
            }

            val canViewCost = SecurityManager.canViewCost(p.userRole)
            val securedValuationMinor = if (canViewCost) metrics.totalValuation else 0L

            InventoryUiState(
                selectedTab = p.tab,
                searchQuery = p.searchQuery,
                selectedCategoryId = p.catId,
                categories = p.cats,
                stockItems = sortedStock,
                movements = movements,
                lowStockItems = filteredLowStock,
                transfers = transfers,
                warehouses = p.whs,
                allProducts = p.prods,
                selectedWarehouseId = p.whId,
                language = p.lang,
                currency = p.curr,
                showAdjustDialog = p.showAdj,
                showTransferDialog = p.showTr,
                userMessage = p.msg,
                totalInventoryQuantity = metrics.totalPhysical,
                totalSellableStock = metrics.totalSellable,
                totalDamagedStock = metrics.totalDamaged,
                totalInventoryValueMinor = securedValuationMinor,
                showScannerDialog = p.showScan,
                selectedProductForDetail = p.selectedProduct,
                selectedProductVariants = p.variants,
                selectedProductImages = p.images,
                preSelectedVariantId = p.preSelectedVariantId,
                valuationBreakdown = if (canViewCost) auditData.valuationBreakdown else null,
                latestReconciliation = auditData.latestReconciliation,
                fifoConsumptions = if (canViewCost) auditData.consumptions else emptyList(),
                totalCogsMinor = if (canViewCost) auditData.totalCogs else 0L,
                selectedProductCostSummary = if (canViewCost) costState.summary else null,
                selectedProductCostLayers = if (canViewCost) costState.layers else emptyList(),
                selectedProductCostAdjustments = if (canViewCost) costState.adjustments else emptyList(),
                showCostHistoryDialog = costState.showHistory,
                showCostAdjustmentDialog = costState.showAdjustment,
                selectedLayerForAdjustment = costState.selectedLayer,
                isReconciling = costState.isReconciling,
                sortOption = p.sortOption,
                stockStatusFilter = p.stockStatusFilter,
                userRole = p.userRole,
                canViewCost = canViewCost,
                variantStockMap = p.variantStock
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    fun onTabSelect(tab: InventoryTab) {
        _selectedTab.value = tab
    }

    fun onWarehouseSelect(warehouseId: String?) {
        _selectedWarehouseId.value = warehouseId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun onSortOptionChange(sortOption: InventorySortOption) {
        _sortOption.value = sortOption
    }

    fun onStockStatusFilterChange(filter: StockStatusFilter) {
        _stockStatusFilter.value = filter
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedWarehouseId.value = null
        _stockStatusFilter.value = StockStatusFilter.ALL
        _sortOption.value = InventorySortOption.NAME_ASC
    }

    fun setAdjustDialogVisible(visible: Boolean) {
        _showAdjustDialog.value = visible
    }

    fun setTransferDialogVisible(visible: Boolean) {
        _showTransferDialog.value = visible
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    suspend fun loadVariantsForProduct(productId: String): List<ProductVariant> {
        return productRepository.getVariantsForProductSync(productId)
    }

    fun performAdjustment(
        warehouseId: String,
        productId: String,
        variantId: String?,
        quantityChange: Int,
        isIncrease: Boolean,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                inventoryRepository.adjustStock(
                    warehouseId = warehouseId,
                    productId = productId,
                    variantId = variantId,
                    quantityChange = quantityChange,
                    isIncrease = isIncrease,
                    reason = reason
                )
                _showAdjustDialog.value = false
                _userMessage.value = "Stock adjustment of $quantityChange units recorded successfully."
            } catch (e: Exception) {
                _userMessage.value = "Error adjusting stock: ${e.message}"
            }
        }
    }

    fun createStockTransfer(draft: StockTransferDraft) {
        viewModelScope.launch {
            try {
                val id = inventoryRepository.createStockTransfer(draft)
                _showTransferDialog.value = false
                _userMessage.value = "Stock transfer created successfully."
            } catch (e: Exception) {
                _userMessage.value = "Error creating transfer: ${e.message}"
            }
        }
    }

    fun dispatchStockTransfer(transferId: String) {
        viewModelScope.launch {
            try {
                inventoryRepository.dispatchStockTransfer(transferId)
                _userMessage.value = "Transfer dispatched and marked IN_TRANSIT."
            } catch (e: Exception) {
                _userMessage.value = "Error dispatching transfer: ${e.message}"
            }
        }
    }

    fun receiveStockTransfer(transferId: String) {
        viewModelScope.launch {
            try {
                inventoryRepository.receiveStockTransfer(transferId)
                _userMessage.value = "Transfer received and stock added to destination warehouse."
            } catch (e: Exception) {
                _userMessage.value = "Error receiving transfer: ${e.message}"
            }
        }
    }

    fun cancelStockTransfer(transferId: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                inventoryRepository.cancelStockTransfer(transferId, reason)
                _userMessage.value = "Transfer cancelled and reserved stock restored."
            } catch (e: Exception) {
                _userMessage.value = "Error cancelling transfer: ${e.message}"
            }
        }
    }

    fun setScannerDialogVisible(visible: Boolean) {
        _showScannerDialog.value = visible
    }

    fun openProductDetail(productId: String, preSelectedVariantId: String? = null) {
        viewModelScope.launch {
            try {
                val prod = productRepository.getProductWithDetailsById(productId)
                if (prod != null) {
                    val variants = productRepository.getVariantsForProductSync(productId)
                    val images = productRepository.getImagesForProductSync(productId)
                    // Load variant stock balances
                    val variantStockMap = mutableMapOf<String, Int>()
                    for (variant in variants) {
                        val stock = stockBalanceDao.getTotalVariantQuantity(productId, variant.id)
                        variantStockMap[variant.id] = stock
                    }
                    _selectedProductForDetail.value = prod
                    _selectedProductVariants.value = variants
                    _selectedProductImages.value = images
                    _variantStockMap.value = variantStockMap
                    _preSelectedVariantId.value = preSelectedVariantId
                } else {
                    _userMessage.value = "Product not found: $productId"
                }
            } catch (e: Exception) {
                _userMessage.value = "Error loading product detail: ${e.message}"
            }
        }
    }

    fun closeProductDetail() {
        _selectedProductForDetail.value = null
        _selectedProductVariants.value = emptyList()
        _selectedProductImages.value = emptyList()
        _variantStockMap.value = emptyMap()
        _preSelectedVariantId.value = null
    }

    suspend fun loadVariantImages(variantId: String): List<ProductImage> {
        return try {
            productRepository.getImagesForVariantSync(variantId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun onCodeScanned(code: String) {
        viewModelScope.launch {
            try {
                val trimmed = code.trim()
                if (trimmed.isEmpty()) return@launch
                val resolved = productRepository.resolveCode(trimmed)
                if (resolved != null) {
                    _showScannerDialog.value = false
                    val variantId = resolved.matchedVariant?.id
                    openProductDetail(resolved.product.id, variantId)
                } else {
                    val isBn = uiState.value.language == AppLanguage.BANGLA
                    _userMessage.value = if (isBn) "কোন পণ্য পাওয়া যায়নি (কোড: $trimmed)" else "No product found for scanned code: $trimmed"
                }
            } catch (e: Exception) {
                _userMessage.value = "Error resolving code: ${e.message}"
            }
        }
    }

    // Phase 06 Part 3: FIFO & Cost History Methods
    fun openCostHistory(productId: String) {
        viewModelScope.launch {
            try {
                val summary = costHistoryRepository.getProductCostSummary(productId).first()
                val layers = costHistoryRepository.getCostLayersForProduct(productId).first()
                val adjustments = costHistoryRepository.getCostAdjustmentsForProduct(productId).first()

                _selectedCostSummary.value = summary
                _selectedCostLayers.value = layers
                _selectedCostAdjustments.value = adjustments
                _showCostHistoryDialog.value = true
            } catch (e: Exception) {
                _userMessage.value = "Error loading cost history: ${e.message}"
            }
        }
    }

    fun closeCostHistory() {
        _showCostHistoryDialog.value = false
    }

    fun openCostAdjustment(layer: InventoryCostLayer) {
        _selectedLayerForAdjustment.value = layer
        _showCostAdjustmentDialog.value = true
    }

    fun closeCostAdjustment() {
        _showCostAdjustmentDialog.value = false
        _selectedLayerForAdjustment.value = null
    }

    fun performCostAdjustment(costLayerId: String, newUnitCostMinor: Long, reason: String) {
        viewModelScope.launch {
            try {
                val result = costHistoryRepository.performCostAdjustment(
                    costLayerId = costLayerId,
                    newUnitCostBdtMinor = newUnitCostMinor,
                    reason = reason
                )
                if (result.isSuccess) {
                    _showCostAdjustmentDialog.value = false
                    _selectedLayerForAdjustment.value = null
                    _userMessage.value = "Landed cost adjustment successfully posted and journalized."
                    // Refresh cost history dialog if open
                    val layer = _selectedCostLayers.value.firstOrNull { it.id == costLayerId }
                    if (layer != null) {
                        openCostHistory(layer.productId)
                    }
                } else {
                    _userMessage.value = "Adjustment failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _userMessage.value = "Adjustment error: ${e.message}"
            }
        }
    }

    fun runReconciliationAudit() {
        viewModelScope.launch {
            _isReconciling.value = true
            try {
                val result = costHistoryRepository.executeReconciliation(
                    warehouseId = _selectedWarehouseId.value,
                    notes = "Manual reconciliation audit triggered from Inventory Dashboard",
                    reconciledBy = "Admin Operator"
                )
                if (result.isSuccess) {
                    val rec = result.getOrThrow()
                    val statusStr = if (rec.status == "MATCHED") "Matched with 0 discrepancy" else "Discrepancies flagged"
                    _userMessage.value = "Reconciliation #${rec.reconciliationNumber} complete: $statusStr."
                } else {
                    _userMessage.value = "Reconciliation audit failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _userMessage.value = "Reconciliation error: ${e.message}"
            } finally {
                _isReconciling.value = false
            }
        }
    }

    fun exportValuationPdf(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val breakdown = state.valuationBreakdown ?: return@launch
                val profile = businessProfileDao.getProfileSync()
                val pdf = InventoryPdfGenerator.generateValuationPdf(
                    context = context,
                    profile = profile,
                    breakdown = breakdown,
                    language = state.language,
                    currency = state.currency
                )
                InventoryPdfGenerator.sharePdf(context, pdf, "Inventory Valuation Report")
            } catch (e: Exception) {
                _userMessage.value = "PDF export error: ${e.message}"
            }
        }
    }

    fun exportMovementLedgerPdf(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val profile = businessProfileDao.getProfileSync()
                val pdf = InventoryPdfGenerator.generateMovementLedgerPdf(
                    context = context,
                    profile = profile,
                    movements = state.movements,
                    language = state.language,
                    currency = state.currency
                )
                InventoryPdfGenerator.sharePdf(context, pdf, "Stock Movement Audit Ledger")
            } catch (e: Exception) {
                _userMessage.value = "Movement PDF export error: ${e.message}"
            }
        }
    }

    fun exportReconciliationPdf(context: Context) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val rec = state.latestReconciliation ?: return@launch
                val items = costHistoryRepository.getReconciliationItems(rec.id).first()
                val profile = businessProfileDao.getProfileSync()
                val pdf = InventoryPdfGenerator.generateReconciliationPdf(
                    context = context,
                    profile = profile,
                    reconciliation = rec,
                    items = items,
                    language = state.language,
                    currency = state.currency
                )
                InventoryPdfGenerator.sharePdf(context, pdf, "Inventory Reconciliation Report")
            } catch (e: Exception) {
                _userMessage.value = "Reconciliation PDF export error: ${e.message}"
            }
        }
    }
}

private data class DialogFlags(
    val showAdj: Boolean,
    val showTr: Boolean,
    val showScan: Boolean,
    val msg: String?
)

private data class ProductDetailState(
    val selectedProduct: ProductWithDetails?,
    val variants: List<ProductVariant>,
    val images: List<ProductImage>,
    val preSelectedVariantId: String?,
    val variantStock: Map<String, Int> = emptyMap()
)

private data class InventoryFilterParams(
    val tab: InventoryTab,
    val whId: String?,
    val searchQuery: String,
    val catId: String?,
    val lang: AppLanguage,
    val curr: CurrencyCode,
    val whs: List<Warehouse>,
    val prods: List<ProductWithDetails>,
    val cats: List<ProductCategory>,
    val showAdj: Boolean,
    val showTr: Boolean,
    val showScan: Boolean,
    val msg: String?,
    val selectedProduct: ProductWithDetails?,
    val variants: List<ProductVariant>,
    val images: List<ProductImage>,
    val preSelectedVariantId: String?,
    val sortOption: InventorySortOption,
    val stockStatusFilter: StockStatusFilter,
    val userRole: UserRole,
    val variantStock: Map<String, Int> = emptyMap()
)

private data class InventoryMetrics(
    val totalPhysical: Int,
    val totalSellable: Int,
    val totalDamaged: Int,
    val totalValuation: Long
)

private data class AuditData(
    val valuationBreakdown: InventoryValuationBreakdown,
    val latestReconciliation: InventoryReconciliation?,
    val consumptions: List<FifoConsumptionWithDetails>,
    val totalCogs: Long
)

private data class CostDialogState(
    val showHistory: Boolean,
    val showAdjustment: Boolean,
    val selectedLayer: InventoryCostLayer?,
    val summary: ProductCostSummary?,
    val layers: List<InventoryCostLayer>,
    val adjustments: List<InventoryCostAdjustmentWithDetails>,
    val isReconciling: Boolean
)
