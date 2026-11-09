package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostAllocation
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

data class PurchaseOrderItemDraft(
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val unitPriceMinor: Long,
    val cartonQuantity: Int = 0,
    val pcsPerCarton: Int = 0,
    val grossWeightKg: BigDecimal = BigDecimal.ZERO,
    val netWeightKg: BigDecimal = BigDecimal.ZERO,
    val cbm: BigDecimal = BigDecimal.ZERO
)

data class MixedCartonLineDraft(
    val productId: String,
    val variantId: String? = null,
    val productName: String = "",
    val quantity: Int
)

data class ShipmentCartonDraft(
    val cartonNumber: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val lengthCm: BigDecimal,
    val widthCm: BigDecimal,
    val heightCm: BigDecimal,
    val grossWeightKg: BigDecimal,
    val netWeightKg: BigDecimal
)

data class ReceiptItemDraft(
    val cartonId: String? = null,
    val productId: String,
    val variantId: String? = null,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val damagedQuantity: Int = 0,
    val missingQuantity: Int = 0,
    val excessQuantity: Int = 0,
    val conditionNotes: String? = null
)

data class PoRemainingItem(
    val poItem: PurchaseOrderItem,
    val product: Product?,
    val orderedQty: Int,
    val shippedQty: Int,
    val remainingQty: Int
)

data class CostAdjustmentResult(
    val shipmentId: String,
    val previousTotalExpenseBdtMinor: Long,
    val actualTotalExpenseBdtMinor: Long,
    val costVarianceBdtMinor: Long,
    val unitVarianceBdtMinor: Long,
    val remainingInventoryAdjustmentBdtMinor: Long,
    val cogsAdjustmentBdtMinor: Long,
    val unallocatedVarianceBdtMinor: Long,
    val journalEntryNumber: String
)

interface ImportRepository {

    // Purchase Orders
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>>
    suspend fun getPurchaseOrderById(id: String): PurchaseOrder?
    fun getPurchaseOrderByIdFlow(id: String): Flow<PurchaseOrder?>
    suspend fun getPoRemainingItems(orderId: String): List<PoRemainingItem>
    fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItem>>
    suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItem>
    suspend fun createPurchaseOrder(
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus = PurchaseOrderStatus.CONFIRMED
    ): String
    suspend fun duplicatePurchaseOrder(orderId: String): String
    suspend fun updatePurchaseOrderStatus(orderId: String, status: PurchaseOrderStatus)
    suspend fun cancelPurchaseOrder(orderId: String, reason: String? = null)

    // Supplier Payments
    fun getAllSupplierPayments(): Flow<List<SupplierPayment>>
    fun getPaymentsForOrder(orderId: String): Flow<List<SupplierPayment>>
    suspend fun getPaymentsForOrderSync(orderId: String): List<SupplierPayment>
    suspend fun recordSupplierPayment(
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?
    ): String

    // Shipments & Cartons
    fun getAllShipments(): Flow<List<ImportShipment>>
    suspend fun getShipmentById(id: String): ImportShipment?
    fun getShipmentsForOrder(orderId: String): Flow<List<ImportShipment>>
    suspend fun getShipmentsForOrderSync(orderId: String): List<ImportShipment>
    fun getCartonsForShipment(shipmentId: String): Flow<List<ShipmentCarton>>
    suspend fun createShipment(
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartons: List<ShipmentCartonDraft>,
        notes: String?
    ): String
    suspend fun addCartonToShipment(shipmentId: String, carton: ShipmentCartonDraft): String
    suspend fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus)

    // Cost Management & Landed Cost Engine
    fun getCostItemsForShipment(shipmentId: String): Flow<List<ImportCostItem>>
    suspend fun getCostItemsForShipmentSync(shipmentId: String): List<ImportCostItem>
    suspend fun addCostItem(
        shipmentId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?
    ): String
    suspend fun deleteCostItem(costItemId: String)
    suspend fun calculateAndSaveLandedCost(shipmentId: String): List<LandedCostSnapshot>
    suspend fun adjustLandedCost(
        shipmentId: String,
        newCostCategory: CostCategory,
        newAmountMinor: Long,
        exchangeRate: BigDecimal,
        reason: String
    ): CostAdjustmentResult
    fun getLandedCostSnapshots(shipmentId: String): Flow<List<LandedCostSnapshot>>
    fun getAllocationsForShipment(shipmentId: String): Flow<List<LandedCostAllocation>>

    // Warehouse Receiving & Inventory Cost Layers
    fun getAllWarehouseReceipts(): Flow<List<WarehouseReceipt>>
    fun getReceiptsForShipment(shipmentId: String): Flow<List<WarehouseReceipt>>
    fun getReceiptItems(receiptId: String): Flow<List<WarehouseReceiptItem>>
    suspend fun getReceiptItemsSync(receiptId: String): List<WarehouseReceiptItem>
    suspend fun createWarehouseReceipt(
        shipmentId: String,
        warehouseId: String,
        items: List<ReceiptItemDraft>,
        notes: String?
    ): String
    suspend fun postWarehouseReceipt(receiptId: String, allowDiscrepancy: Boolean = false)
    fun getAllCostLayers(): Flow<List<InventoryCostLayer>>
    fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>>
    suspend fun getCostLayersForShipmentSync(shipmentId: String): List<InventoryCostLayer>

    // Minimal Import Management System Enhancements (Purchase-Centric Workflow)
    suspend fun addMixedCarton(
        orderId: String,
        cartonNumber: String,
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        grossWeightKg: BigDecimal,
        netWeightKg: BigDecimal,
        lines: List<MixedCartonLineDraft>
    ): List<String>

    suspend fun deleteCarton(cartonId: String)
    suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String)
    suspend fun deletePurchaseCostItem(costItemId: String)
    suspend fun updatePurchaseOrderPacking(orderId: String, totalCartons: Int, grossWeightKg: String, netWeightKg: String, cbm: String)
    suspend fun updatePurchaseOrderItem(itemId: String, quantity: Int, unitPriceMinor: Long, cartonQuantity: Int, grossWeightKg: String)
    suspend fun deletePurchaseOrderItem(itemId: String)

    fun getCartonsForOrder(orderId: String): Flow<List<ShipmentCarton>>
    suspend fun getCartonsForOrderSync(orderId: String): List<ShipmentCarton>

    suspend fun addPurchaseCostItem(
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
    ): String

    fun getCostItemsForOrder(orderId: String): Flow<List<ImportCostItem>>
    suspend fun getCostItemsForOrderSync(orderId: String): List<ImportCostItem>
    suspend fun finalizePurchaseActualCost(orderId: String): List<LandedCostSnapshot>

    suspend fun createPurchaseReceiving(
        orderId: String,
        warehouseId: String,
        receivingMethod: String, // PHYSICAL_COUNT or CARTON_WISE
        items: List<ReceiptItemDraft>,
        notes: String?
    ): String

    suspend fun postPurchaseReceiving(receiptId: String, allowDiscrepancy: Boolean = false)
    fun getReceiptsForOrder(orderId: String): Flow<List<WarehouseReceipt>>

    suspend fun addAdditionalCost(
        orderId: String,
        warehouseId: String?,
        category: AdditionalCostCategory,
        description: String,
        amountMinor: Long,
        isCapitalizable: Boolean,
        allocationMethod: String = "QUANTITY",
        notes: String? = null
    ): String

    fun getAdditionalCostsForOrder(orderId: String): Flow<List<AdditionalProductCost>>
    suspend fun getAdditionalCostsForOrderSync(orderId: String): List<AdditionalProductCost>

    suspend fun completePurchase(
        orderId: String,
        disposition: String?, // SHORTAGE, CANCELLED_BALANCE, SUPPLIER_CLAIM
        reason: String?,
        notes: String? = null
    ): PurchaseCompletionRecord

    fun getCompletionRecordForOrder(orderId: String): Flow<PurchaseCompletionRecord?>
    fun getDamageRecordsForOrder(orderId: String): Flow<List<DamageRecord>>
    fun getShortageRecordsForOrder(orderId: String): Flow<List<ShortageRecord>>
}
