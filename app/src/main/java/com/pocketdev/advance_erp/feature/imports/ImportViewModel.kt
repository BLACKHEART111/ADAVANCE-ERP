package com.pocketdev.advance_erp.feature.imports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.ImportPdfGenerator
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.ImportRepository
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.PoRemainingItem
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class ImportUiState(
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val shipments: List<ImportShipment> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedShipmentCartons: List<ShipmentCarton> = emptyList(),
    val costItems: List<ImportCostItem> = emptyList(),
    val landedCostSnapshots: List<LandedCostSnapshot> = emptyList(),
    val warehouseReceipts: List<WarehouseReceipt> = emptyList(),
    val costLayers: List<InventoryCostLayer> = emptyList(),
    val selectedShipmentId: String? = null,
    val selectedOrderId: String? = null,
    val selectedPo: PurchaseOrder? = null,
    val selectedPoItems: List<PurchaseOrderItem> = emptyList(),
    val selectedPoPayments: List<SupplierPayment> = emptyList(),
    val selectedPoShipments: List<ImportShipment> = emptyList(),
    val selectedPoCartons: List<ShipmentCarton> = emptyList(),
    val selectedPoCostItems: List<ImportCostItem> = emptyList(),
    val selectedPoReceipts: List<WarehouseReceipt> = emptyList(),
    val selectedPoDamageRecords: List<DamageRecord> = emptyList(),
    val selectedPoShortageRecords: List<ShortageRecord> = emptyList(),
    val selectedPoAdditionalCosts: List<AdditionalProductCost> = emptyList(),
    val selectedPoCompletionRecord: PurchaseCompletionRecord? = null,
    val searchQuery: String = "",
    val poStatusFilter: PurchaseOrderStatus? = null,
    val poSupplierFilter: String? = null,
    val businessProfile: BusinessProfile? = null,
    val language: AppLanguage = AppLanguage.BANGLA,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importRepository: ImportRepository,
    private val productRepository: ProductRepository,
    private val contactsRepository: ContactsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val businessProfileDao: BusinessProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState(isLoading = true))
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val ordersShipmentsFlow = combine(
                importRepository.getAllPurchaseOrders(),
                importRepository.getAllShipments(),
                contactsRepository.getAllSuppliers()
            ) { orders, shipments, suppliers ->
                Triple(orders, shipments, suppliers)
            }

            val productsReceiptsLayersFlow = combine(
                productRepository.getAllProducts(),
                importRepository.getAllWarehouseReceipts(),
                importRepository.getAllCostLayers()
            ) { productsWithDetails, receipts, layers ->
                val products = productsWithDetails.map { pwd ->
                    Product(
                        id = pwd.id,
                        categoryId = pwd.categoryId,
                        nameEn = pwd.nameEn,
                        nameBn = pwd.nameBn,
                        baseSku = pwd.baseSku,
                        barcode = pwd.barcode,
                        description = pwd.description,
                        unit = pwd.unit,
                        purchasePriceMinor = pwd.purchasePriceMinor,
                        sellingPriceMinor = pwd.sellingPriceMinor,
                        minStockAlert = pwd.minStockAlert,
                        isActive = pwd.isActive,
                        createdAt = pwd.createdAt,
                        updatedAt = pwd.createdAt
                    )
                }
                Triple(products, receipts, layers)
            }

            viewModelScope.launch {
                productRepository.getAllCategories().collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }

            combine(
                ordersShipmentsFlow,
                productsReceiptsLayersFlow,
                preferencesRepository.selectedLanguage,
                businessProfileDao.getProfile()
            ) { (orders, shipments, suppliers), (products, receipts, layers), lang, profile ->
                val activeShipmentId = _uiState.value.selectedShipmentId ?: shipments.firstOrNull()?.id
                _uiState.value.copy(
                    purchaseOrders = orders,
                    shipments = shipments,
                    suppliers = suppliers,
                    products = products,
                    warehouseReceipts = receipts,
                    costLayers = layers,
                    selectedShipmentId = activeShipmentId,
                    businessProfile = profile,
                    language = lang,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
                if (newState.selectedShipmentId != null) {
                    loadShipmentDetails(newState.selectedShipmentId)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setPoStatusFilter(status: PurchaseOrderStatus?) {
        _uiState.update { it.copy(poStatusFilter = status) }
    }

    fun setPoSupplierFilter(supplierId: String?) {
        _uiState.update { it.copy(poSupplierFilter = supplierId) }
    }

    fun selectShipment(shipmentId: String) {
        _uiState.update { it.copy(selectedShipmentId = shipmentId) }
        loadShipmentDetails(shipmentId)
    }

    private fun loadShipmentDetails(shipmentId: String) {
        viewModelScope.launch {
            launch {
                importRepository.getCartonsForShipment(shipmentId).collect { cartons ->
                    _uiState.update { it.copy(selectedShipmentCartons = cartons) }
                }
            }
            launch {
                importRepository.getCostItemsForShipment(shipmentId).collect { costs ->
                    _uiState.update { it.copy(costItems = costs) }
                }
            }
            launch {
                importRepository.getLandedCostSnapshots(shipmentId).collect { snapshots ->
                    _uiState.update { it.copy(landedCostSnapshots = snapshots) }
                }
            }
        }
    }

    fun selectPurchaseOrder(order: PurchaseOrder?) {
        if (order == null) {
            _uiState.update {
                it.copy(
                    selectedPo = null,
                    selectedPoItems = emptyList(),
                    selectedPoPayments = emptyList(),
                    selectedPoShipments = emptyList(),
                    selectedPoCartons = emptyList(),
                    selectedPoCostItems = emptyList(),
                    selectedPoReceipts = emptyList(),
                    selectedPoDamageRecords = emptyList(),
                    selectedPoShortageRecords = emptyList(),
                    selectedPoAdditionalCosts = emptyList(),
                    selectedPoCompletionRecord = null
                )
            }
            return
        }

        _uiState.update { it.copy(selectedPo = order) }
        viewModelScope.launch {
            launch {
                importRepository.getPurchaseOrderByIdFlow(order.id).collect { updatedPo ->
                    if (updatedPo != null) {
                        _uiState.update { it.copy(selectedPo = updatedPo) }
                    }
                }
            }
            launch {
                importRepository.getItemsForOrder(order.id).collect { items ->
                    _uiState.update { it.copy(selectedPoItems = items) }
                }
            }
            launch {
                importRepository.getPaymentsForOrder(order.id).collect { payments ->
                    _uiState.update { it.copy(selectedPoPayments = payments) }
                }
            }
            launch {
                importRepository.getShipmentsForOrder(order.id).collect { shipments ->
                    _uiState.update { it.copy(selectedPoShipments = shipments) }
                }
            }
            launch {
                importRepository.getCartonsForOrder(order.id).collect { cartons ->
                    _uiState.update { it.copy(selectedPoCartons = cartons) }
                }
            }
            launch {
                importRepository.getCostItemsForOrder(order.id).collect { costs ->
                    _uiState.update { it.copy(selectedPoCostItems = costs) }
                }
            }
            launch {
                importRepository.getReceiptsForOrder(order.id).collect { receipts ->
                    _uiState.update { it.copy(selectedPoReceipts = receipts) }
                }
            }
            launch {
                importRepository.getDamageRecordsForOrder(order.id).collect { dmgs ->
                    _uiState.update { it.copy(selectedPoDamageRecords = dmgs) }
                }
            }
            launch {
                importRepository.getShortageRecordsForOrder(order.id).collect { shts ->
                    _uiState.update { it.copy(selectedPoShortageRecords = shts) }
                }
            }
            launch {
                importRepository.getAdditionalCostsForOrder(order.id).collect { acs ->
                    _uiState.update { it.copy(selectedPoAdditionalCosts = acs) }
                }
            }
            launch {
                importRepository.getCompletionRecordForOrder(order.id).collect { comp ->
                    _uiState.update { it.copy(selectedPoCompletionRecord = comp) }
                }
            }
        }
    }

    fun createPurchaseOrder(
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus = PurchaseOrderStatus.CONFIRMED
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.createPurchaseOrder(supplierId, currency, exchangeRate, items, notes, status)
                val msg = if (status == PurchaseOrderStatus.DRAFT) {
                    "Purchase Order saved as Draft successfully!"
                } else {
                    "Purchase Order confirmed successfully!"
                }
                _uiState.update { it.copy(isLoading = false, userMessage = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    fun confirmPurchaseOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.updatePurchaseOrderStatus(orderId, PurchaseOrderStatus.CONFIRMED)
                val updatedPo = importRepository.getPurchaseOrderById(orderId)
                if (updatedPo != null && _uiState.value.selectedPo?.id == orderId) {
                    _uiState.update { it.copy(selectedPo = updatedPo) }
                }
                _uiState.update { it.copy(isLoading = false, userMessage = "Purchase Order confirmed successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    fun cancelPurchaseOrder(orderId: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.cancelPurchaseOrder(orderId, reason)
                val updatedPo = importRepository.getPurchaseOrderById(orderId)
                if (updatedPo != null && _uiState.value.selectedPo?.id == orderId) {
                    _uiState.update { it.copy(selectedPo = updatedPo) }
                }
                _uiState.update { it.copy(isLoading = false, userMessage = "Purchase Order cancelled.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Cancel Error: ${e.message}") }
            }
        }
    }

    fun duplicatePurchaseOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val newId = importRepository.duplicatePurchaseOrder(orderId)
                _uiState.update { it.copy(isLoading = false, userMessage = "Purchase Order duplicated successfully as Draft!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Duplicate Error: ${e.message}") }
            }
        }
    }

    fun recordSupplierPayment(
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.recordSupplierPayment(
                    supplierId, orderId, amountMinor, currency, exchangeRate, method, paymentAccount, reference, notes
                )
                if (orderId != null) {
                    val updatedPo = importRepository.getPurchaseOrderById(orderId)
                    if (updatedPo != null && _uiState.value.selectedPo?.id == orderId) {
                        _uiState.update { it.copy(selectedPo = updatedPo) }
                    }
                }
                _uiState.update { it.copy(isLoading = false, userMessage = "Payment recorded and journal posted!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    suspend fun getPoRemainingItems(orderId: String): List<PoRemainingItem> =
        importRepository.getPoRemainingItems(orderId)

    fun createShipment(
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartons: List<ShipmentCartonDraft>,
        notes: String?,
        onSuccess: ((shipmentId: String, shipmentNumber: String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val shipmentId = importRepository.createShipment(
                    supplierId, orderId, method, origin, destination, trackingRef, shippingLine, cartons, notes
                )
                val createdShipment = importRepository.getShipmentById(shipmentId)
                val shpNumber = createdShipment?.shipmentNumber ?: shipmentId
                selectShipment(shipmentId)
                if (orderId != null) {
                    val updatedPo = importRepository.getPurchaseOrderById(orderId)
                    if (updatedPo != null && _uiState.value.selectedPo?.id == orderId) {
                        _uiState.update { it.copy(selectedPo = updatedPo) }
                    }
                    val poShipments = importRepository.getShipmentsForOrderSync(orderId)
                    _uiState.update { it.copy(selectedPoShipments = poShipments) }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Shipment $shpNumber created with ${cartons.size} cartons!"
                    )
                }
                onSuccess?.invoke(shipmentId, shpNumber)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
                onError?.invoke(e.message ?: "Failed to create shipment")
            }
        }
    }

    fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.updateShipmentStatus(shipmentId, status)
                val shipment = importRepository.getShipmentById(shipmentId)
                if (shipment?.purchaseOrderId != null) {
                    val updatedPo = importRepository.getPurchaseOrderById(shipment.purchaseOrderId)
                    if (updatedPo != null && _uiState.value.selectedPo?.id == shipment.purchaseOrderId) {
                        _uiState.update { it.copy(selectedPo = updatedPo) }
                    }
                    val poShipments = importRepository.getShipmentsForOrderSync(shipment.purchaseOrderId)
                    _uiState.update { it.copy(selectedPoShipments = poShipments) }
                }
                loadShipmentDetails(shipmentId)
                _uiState.update { it.copy(isLoading = false, userMessage = "Shipment status updated to ${status.labelEn}!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Status Error: ${e.message}") }
            }
        }
    }

    fun cancelShipment(shipmentId: String) {
        updateShipmentStatus(shipmentId, ShipmentStatus.CANCELLED)
    }

    fun addCartonToShipment(shipmentId: String, carton: ShipmentCartonDraft) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.addCartonToShipment(shipmentId, carton)
                loadShipmentDetails(shipmentId)
                _uiState.update { it.copy(isLoading = false, userMessage = "Carton ${carton.cartonNumber} added to shipment!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Carton Error: ${e.message}") }
            }
        }
    }

    fun addCostItem(
        shipmentId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.addCostItem(
                    shipmentId, category, description, amountMinor, currency, exchangeRate, method, paymentAccount, docRef
                )
                _uiState.update { it.copy(isLoading = false, userMessage = "Expense recorded and capitalized to in-transit inventory!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    fun calculateLandedCost(shipmentId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val snapshots = importRepository.calculateAndSaveLandedCost(shipmentId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        landedCostSnapshots = snapshots,
                        userMessage = "Landed cost allocated successfully across ${snapshots.size} products!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Allocation Error: ${e.message}") }
            }
        }
    }

    fun createAndPostWarehouseReceipt(
        shipmentId: String,
        warehouseId: String,
        items: List<ReceiptItemDraft>,
        notes: String?,
        allowDiscrepancy: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val receiptId = importRepository.createWarehouseReceipt(shipmentId, warehouseId, items, notes)
                importRepository.postWarehouseReceipt(receiptId, allowDiscrepancy)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Warehouse receipt posted! Stock increased and cost layers created."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Receiving Error: ${e.message}") }
            }
        }
    }

    // ==========================================
    // PDF Generation & Sharing
    // ==========================================

    fun sharePurchaseOrderPdf(context: Context, order: PurchaseOrder) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val items = importRepository.getItemsForOrderSync(order.id)
                val supplier = _uiState.value.suppliers.find { it.id == order.supplierId }
                val productMap = _uiState.value.products.associateBy { it.id }
                val pdfFile = ImportPdfGenerator.generatePurchaseOrderPdf(
                    context = context,
                    order = order,
                    items = items,
                    productsMap = productMap,
                    supplier = supplier,
                    businessProfile = _uiState.value.businessProfile,
                    isBangla = _uiState.value.language == AppLanguage.BANGLA
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun sharePaymentReceiptPdf(context: Context, payment: SupplierPayment) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val supplier = _uiState.value.suppliers.find { it.id == payment.supplierId }
                val order = payment.purchaseOrderId?.let { importRepository.getPurchaseOrderById(it) }
                val pdfFile = ImportPdfGenerator.generateSupplierPaymentReceiptPdf(
                    context = context,
                    payment = payment,
                    order = order,
                    supplier = supplier,
                    businessProfile = _uiState.value.businessProfile,
                    isBangla = _uiState.value.language == AppLanguage.BANGLA
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun sharePackingListPdf(context: Context, shipment: ImportShipment) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val supplier = _uiState.value.suppliers.find { it.id == shipment.supplierId }
                val productMap = _uiState.value.products.associateBy { it.id }
                val cartons = _uiState.value.selectedShipmentCartons
                val pdfFile = ImportPdfGenerator.generatePackingListPdf(
                    context = context,
                    shipment = shipment,
                    cartons = cartons,
                    productsMap = productMap,
                    supplier = supplier,
                    businessProfile = _uiState.value.businessProfile,
                    isBangla = _uiState.value.language == AppLanguage.BANGLA
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun shareLandedCostPdf(context: Context, shipment: ImportShipment) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val snapshots = _uiState.value.landedCostSnapshots
                val costItems = _uiState.value.costItems
                val pdfFile = ImportPdfGenerator.generateLandedCostReportPdf(
                    context = context,
                    shipment = shipment,
                    snapshots = snapshots,
                    costItems = costItems,
                    businessProfile = _uiState.value.businessProfile,
                    isBangla = _uiState.value.language == AppLanguage.BANGLA
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun shareReceivingReportPdf(context: Context, receipt: WarehouseReceipt) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val items = importRepository.getReceiptItemsSync(receipt.id)
                val productMap = _uiState.value.products.associateBy { it.id }
                val pdfFile = ImportPdfGenerator.generateWarehouseReceivingReportPdf(
                    context = context,
                    receipt = receipt,
                    items = items,
                    productsMap = productMap,
                    businessProfile = _uiState.value.businessProfile,
                    isBangla = _uiState.value.language == AppLanguage.BANGLA
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun adjustLandedCost(
        shipmentId: String,
        category: CostCategory,
        amountMinor: Long,
        exchangeRate: BigDecimal,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = importRepository.adjustLandedCost(
                    shipmentId = shipmentId,
                    newCostCategory = category,
                    newAmountMinor = amountMinor,
                    exchangeRate = exchangeRate,
                    reason = reason
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Actual landed cost adjusted! Variance: ৳${result.costVarianceBdtMinor / 100}. Inventory & COGS reconciled."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Adjustment Error: ${e.message}") }
            }
        }
    }

    fun shareCostVariancePdf(
        context: Context,
        shipment: ImportShipment,
        costVarianceBdtMinor: Long,
        oldTotalExpenseBdtMinor: Long,
        newTotalExpenseBdtMinor: Long,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val pdfFile = ImportPdfGenerator.generateCostVarianceReportPdf(
                    context = context,
                    shipment = shipment,
                    costVarianceBdtMinor = costVarianceBdtMinor,
                    oldTotalExpenseBdtMinor = oldTotalExpenseBdtMinor,
                    newTotalExpenseBdtMinor = newTotalExpenseBdtMinor,
                    reason = reason,
                    businessProfile = _uiState.value.businessProfile
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun shareDamageLossPdf(context: Context, receipt: WarehouseReceipt, shipment: ImportShipment) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val items = importRepository.getReceiptItemsSync(receipt.id)
                val productMap = _uiState.value.products.associateBy { it.id }
                val pdfFile = ImportPdfGenerator.generateDamageLossReportPdf(
                    context = context,
                    receipt = receipt,
                    shipment = shipment,
                    items = items,
                    productsMap = productMap,
                    businessProfile = _uiState.value.businessProfile
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun shareCostLayersPdf(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val layers = _uiState.value.costLayers
                val productMap = _uiState.value.products.associateBy { it.id }
                val pdfFile = ImportPdfGenerator.generateInventoryCostLayerReportPdf(
                    context = context,
                    layers = layers,
                    productsMap = productMap,
                    businessProfile = _uiState.value.businessProfile
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun shareCbmReportPdf(context: Context, shipment: ImportShipment) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val cartons = _uiState.value.selectedShipmentCartons
                val productMap = _uiState.value.products.associateBy { it.id }
                val pdfFile = ImportPdfGenerator.generateCbmReportPdf(
                    context = context,
                    shipment = shipment,
                    cartons = cartons,
                    productsMap = productMap,
                    businessProfile = _uiState.value.businessProfile
                )
                val intent = ImportPdfGenerator.createSharePdfIntent(context, pdfFile)
                context.startActivity(intent)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "PDF Error: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // ==========================================
    // Minimal Import Management System Actions
    // ==========================================

    fun addCartonToPurchase(orderId: String, carton: ShipmentCartonDraft) {
        addMixedCarton(
            orderId = orderId,
            cartonNumber = carton.cartonNumber,
            lengthCm = carton.lengthCm,
            widthCm = carton.widthCm,
            heightCm = carton.heightCm,
            grossWeightKg = carton.grossWeightKg,
            netWeightKg = carton.netWeightKg,
            lines = listOf(
                MixedCartonLineDraft(
                    productId = carton.productId,
                    variantId = carton.variantId,
                    quantity = carton.quantity
                )
            )
        )
    }

    fun addMixedCarton(
        orderId: String,
        cartonNumber: String,
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        grossWeightKg: BigDecimal,
        netWeightKg: BigDecimal,
        lines: List<MixedCartonLineDraft>
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.addMixedCarton(
                    orderId = orderId,
                    cartonNumber = cartonNumber,
                    lengthCm = lengthCm,
                    widthCm = widthCm,
                    heightCm = heightCm,
                    grossWeightKg = grossWeightKg,
                    netWeightKg = netWeightKg,
                    lines = lines
                )
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Mixed carton $cartonNumber saved with quantity-based weight allocation!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Carton Error: ${e.message}") }
            }
        }
    }

    fun deleteCarton(orderId: String, cartonNumber: String) {
        viewModelScope.launch {
            try {
                importRepository.deleteCartonByNumber(orderId, cartonNumber)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Carton $cartonNumber deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Delete carton error: ${e.message}") }
            }
        }
    }

    fun deleteCostItem(orderId: String, costItemId: String) {
        viewModelScope.launch {
            try {
                importRepository.deletePurchaseCostItem(costItemId)
                _uiState.update { it.copy(userMessage = "Cost item removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Delete cost error: ${e.message}") }
            }
        }
    }

    fun updateOrderPacking(orderId: String, totalCartons: Int, grossWeightKg: String, netWeightKg: String, cbm: String) {
        viewModelScope.launch {
            try {
                importRepository.updatePurchaseOrderPacking(orderId, totalCartons, grossWeightKg, netWeightKg, cbm)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Packing details updated!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Update packing error: ${e.message}") }
            }
        }
    }

    fun updatePoItem(orderId: String, itemId: String, quantity: Int, unitPriceMinor: Long, cartonQuantity: Int, grossWeightKg: String) {
        viewModelScope.launch {
            try {
                importRepository.updatePurchaseOrderItem(itemId, quantity, unitPriceMinor, cartonQuantity, grossWeightKg)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Item updated successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Update item error: ${e.message}") }
            }
        }
    }

    fun deletePoItem(orderId: String, itemId: String) {
        viewModelScope.launch {
            try {
                importRepository.deletePurchaseOrderItem(itemId)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Item removed from PO"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Delete item error: ${e.message}") }
            }
        }
    }

    fun addNewCategory(nameEn: String, nameBn: String, onCreated: ((ProductCategory) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val cat = productRepository.addCategory(nameEn, nameBn)
                _uiState.update { it.copy(userMessage = "Category '${cat.nameEn}' created successfully!") }
                onCreated?.invoke(cat)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Error creating category: ${e.message}") }
            }
        }
    }

    fun addPurchaseCostItem(
        orderId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        isEstimated: Boolean = false,
        paymentAccount: String = "City Bank Corporate (Import LC)",
        docRef: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.addPurchaseCostItem(
                    orderId = orderId,
                    category = category,
                    description = description,
                    amountMinor = amountMinor,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    method = method,
                    isEstimated = isEstimated,
                    paymentAccount = paymentAccount,
                    docRef = docRef,
                    notes = notes
                )
                val typeStr = if (isEstimated) "Estimated" else "Actual"
                _uiState.update { it.copy(isLoading = false, userMessage = "$typeStr expense added successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Cost Error: ${e.message}") }
            }
        }
    }

    fun finalizePurchaseActualCost(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val snapshots = importRepository.finalizePurchaseActualCost(orderId)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        landedCostSnapshots = snapshots,
                        userMessage = "Actual Landing Cost finalized successfully! Inventory posting is now unlocked."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Finalize Error: ${e.message}") }
            }
        }
    }

    fun createPurchaseReceiving(
        orderId: String,
        warehouseId: String,
        receivingMethod: String,
        items: List<ReceiptItemDraft>,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val receiptId = importRepository.createPurchaseReceiving(
                    orderId = orderId,
                    warehouseId = warehouseId,
                    receivingMethod = receivingMethod,
                    items = items,
                    notes = notes
                )
                _uiState.update { it.copy(isLoading = false, userMessage = "Receiving draft created ($receivingMethod). Cost must be finalized before posting stock.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Receiving Error: ${e.message}") }
            }
        }
    }

    fun postPurchaseReceiving(receiptId: String, allowDiscrepancy: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.postPurchaseReceiving(receiptId, allowDiscrepancy)
                _uiState.update { it.copy(isLoading = false, userMessage = "Stock received and posted to Inventory successfully with FIFO layers!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Posting Error: ${e.message}") }
            }
        }
    }

    fun addAdditionalCost(
        orderId: String,
        warehouseId: String?,
        category: AdditionalCostCategory,
        description: String,
        amountMinor: Long,
        isCapitalizable: Boolean,
        allocationMethod: String = "QUANTITY",
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.addAdditionalCost(
                    orderId = orderId,
                    warehouseId = warehouseId,
                    category = category,
                    description = description,
                    amountMinor = amountMinor,
                    isCapitalizable = isCapitalizable,
                    allocationMethod = allocationMethod,
                    notes = notes
                )
                val treat = if (isCapitalizable) "Capitalized to Inventory" else "Posted to P&L Period Expense"
                _uiState.update { it.copy(isLoading = false, userMessage = "Additional cost recorded ($treat)!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Cost Error: ${e.message}") }
            }
        }
    }

    fun completePurchase(
        orderId: String,
        disposition: String?,
        reason: String?,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                importRepository.completePurchase(orderId, disposition, reason, notes)
                val freshPo = importRepository.getPurchaseOrderById(orderId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedPo = if (it.selectedPo?.id == orderId && freshPo != null) freshPo else it.selectedPo,
                        userMessage = "Purchase Order completed successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Completion Error: ${e.message}") }
            }
        }
    }

    fun addNewProductFromPurchase(
        nameEn: String,
        nameBn: String,
        categoryId: String,
        baseSku: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        onCreated: (Product) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val newProd = Product(
                    id = "prod-${UUID.randomUUID()}",
                    categoryId = categoryId,
                    nameEn = nameEn,
                    nameBn = nameBn.ifBlank { nameEn },
                    baseSku = baseSku,
                    barcode = null,
                    description = "Import catalog item",
                    unit = unit,
                    purchasePriceMinor = purchasePriceMinor,
                    sellingPriceMinor = sellingPriceMinor,
                    minStockAlert = 10,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                productRepository.addProduct(newProd, emptyList(), 0, "")
                onCreated(newProd)
                _uiState.update { it.copy(isLoading = false, userMessage = "Product '${newProd.nameEn}' added to catalog!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Product Add Error: ${e.message}") }
            }
        }
    }
}
