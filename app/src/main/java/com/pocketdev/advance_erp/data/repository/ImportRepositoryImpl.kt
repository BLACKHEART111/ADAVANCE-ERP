package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.ImportCostDao
import com.pocketdev.advance_erp.data.dao.ImportShipmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.MinimalImportDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.dao.WarehouseReceiptDao
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
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReceiptStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.CostAdjustmentResult
import com.pocketdev.advance_erp.domain.repository.ImportRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.PoRemainingItem
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepositoryImpl @Inject constructor(
    private val database: AdvanceErpDatabase,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val importShipmentDao: ImportShipmentDao,
    private val importCostDao: ImportCostDao,
    private val warehouseReceiptDao: WarehouseReceiptDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val minimalImportDao: MinimalImportDao,
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val accountingRepository: AccountingRepository
) : ImportRepository {

    // ==========================================
    // 1. Purchase Orders
    // ==========================================

    override fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>> = purchaseOrderDao.getAllOrders()

    override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? = purchaseOrderDao.getOrderById(id)
    override fun getPurchaseOrderByIdFlow(id: String): Flow<PurchaseOrder?> = purchaseOrderDao.getOrderByIdFlow(id)

    override fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItem>> = purchaseOrderDao.getItemsForOrder(orderId)

    override suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItem> =
        purchaseOrderDao.getItemsForOrderSync(orderId)

    override suspend fun getPoRemainingItems(orderId: String): List<PoRemainingItem> {
        val items = purchaseOrderDao.getItemsForOrderSync(orderId)
        val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
            .filter { it.status != ShipmentStatus.CANCELLED.name }
        val existingCartons = existingShipments.flatMap { importShipmentDao.getCartonsForShipmentSync(it.id) }
        val shippedByProduct = existingCartons.groupBy { it.productId to it.variantId }
            .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

        return items.map { item ->
            val shipped = shippedByProduct[item.productId to item.variantId] ?: 0
            val remaining = (item.quantity - shipped).coerceAtLeast(0)
            val product = productDao.getProductById(item.productId)
            PoRemainingItem(
                poItem = item,
                product = product,
                orderedQty = item.quantity,
                shippedQty = shipped,
                remainingQty = remaining
            )
        }
    }

    override suspend fun createPurchaseOrder(
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus
    ): String {
        require(items.isNotEmpty()) { "Purchase order must contain at least 1 item" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive: $exchangeRate" }

        val orderCount = purchaseOrderDao.getOrderCount()
        val poNumber = "PO-2026-CN-%03d".format(orderCount + 1)
        val orderId = "po-${UUID.randomUUID()}"

        var totalOriginalMinor = 0L
        var totalBdtMinor = 0L

        val poItems = items.map { draft ->
            require(draft.quantity > 0) { "Quantity must be greater than 0" }
            require(draft.unitPriceMinor > 0) { "Unit price must be greater than 0" }

            val lineOriginalMinor = draft.quantity.toLong() * draft.unitPriceMinor
            val lineBdtMinor = BigDecimal(lineOriginalMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            totalOriginalMinor += lineOriginalMinor
            totalBdtMinor += lineBdtMinor

            PurchaseOrderItem(
                id = "poi-${UUID.randomUUID()}",
                purchaseOrderId = orderId,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                unitPriceMinor = draft.unitPriceMinor,
                totalAmountMinor = lineOriginalMinor,
                totalBdtAmountMinor = lineBdtMinor,
                receivedQuantity = 0,
                cartonQuantity = draft.cartonQuantity,
                pcsPerCarton = draft.pcsPerCarton,
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = draft.cbm.toPlainString()
            )
        }

        val totalCartonsSum = poItems.sumOf { it.cartonQuantity }
        val totalGrossWeightSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.grossWeightKg.ifBlank { "0" })) }
        val totalNetWeightSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.netWeightKg.ifBlank { "0" })) }
        val totalCbmSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.cbm.ifBlank { "0" })) }

        val order = PurchaseOrder(
            id = orderId,
            poNumber = poNumber,
            supplierId = supplierId,
            status = status.name,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            totalAmountMinor = totalOriginalMinor,
            totalBdtAmountMinor = totalBdtMinor,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            orderDate = System.currentTimeMillis(),
            notes = notes,
            isActualCostFinalized = false,
            totalCartons = totalCartonsSum,
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // IMPORTANT: Creating a PO does NOT increase inventory stock! Stock increases only on warehouse receiving.
        purchaseOrderDao.saveOrderWithItems(order, poItems)

        // One Purchase = One Batch: Automatically create linked batch shipment
        val batchNumber = "BATCH-$poNumber"
        val batchShipment = ImportShipment(
            id = "shp-${UUID.randomUUID()}",
            shipmentNumber = batchNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = ShippingMethod.SEA.name,
            origin = "Guangzhou, China",
            destination = "Chattogram Port, Bangladesh",
            status = ShipmentStatus.DRAFT.name,
            totalCartons = totalCartonsSum,
            totalQuantity = poItems.sumOf { it.quantity },
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            notes = "Auto-generated batch for $poNumber",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        importShipmentDao.insertShipment(batchShipment)

        // Update supplier outstanding payable if PO is confirmed
        if (status == PurchaseOrderStatus.CONFIRMED) {
            val supplier = supplierDao.getSupplierById(supplierId)
            if (supplier != null) {
                supplierDao.updateOutstandingPayable(supplierId, supplier.outstandingPayableMinor + totalBdtMinor)
            }
        }

        return orderId
    }

    override suspend fun updatePurchaseOrderStatus(orderId: String, status: PurchaseOrderStatus) {
        val currentOrder = purchaseOrderDao.getOrderById(orderId)
        val oldStatus = currentOrder?.status
        purchaseOrderDao.updateOrderStatus(orderId, status.name)

        // When a DRAFT order is confirmed, add its BDT amount to the supplier outstanding payable
        if (status == PurchaseOrderStatus.CONFIRMED && oldStatus == PurchaseOrderStatus.DRAFT.name && currentOrder != null) {
            val supplier = supplierDao.getSupplierById(currentOrder.supplierId)
            if (supplier != null) {
                supplierDao.updateOutstandingPayable(currentOrder.supplierId, supplier.outstandingPayableMinor + currentOrder.totalBdtAmountMinor)
            }
        }
    }

    override suspend fun duplicatePurchaseOrder(orderId: String): String {
        val originalOrder = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
        val originalItems = purchaseOrderDao.getItemsForOrderSync(orderId)

        val poCount = purchaseOrderDao.getOrderCount()
        val newPoNumber = "PO-%s-%04d".format(
            java.text.SimpleDateFormat("yyyyMM", java.util.Locale.US).format(java.util.Date()),
            poCount + 1
        )
        val newOrderId = "po-${UUID.randomUUID()}"

        val newOrder = originalOrder.copy(
            id = newOrderId,
            poNumber = newPoNumber,
            status = PurchaseOrderStatus.DRAFT.name,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            orderDate = System.currentTimeMillis(),
            notes = "Duplicated from ${originalOrder.poNumber}. ${originalOrder.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newItems = originalItems.map { item ->
            item.copy(
                id = "poi-${UUID.randomUUID()}",
                purchaseOrderId = newOrderId,
                receivedQuantity = 0
            )
        }

        database.withTransaction {
            purchaseOrderDao.insertOrder(newOrder)
            purchaseOrderDao.insertItems(newItems)
        }
        return newOrderId
    }

    override suspend fun cancelPurchaseOrder(orderId: String, reason: String?) {
        val order = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
        check(order.status != PurchaseOrderStatus.CANCELLED.name) {
            "Purchase Order ${order.poNumber} is already cancelled"
        }

        val existingPayments = supplierPaymentDao.getPaymentsByOrderSync(orderId)
        check(existingPayments.isEmpty()) {
            "Cannot cancel Purchase Order: linked supplier payments exist (${existingPayments.size} payment(s))"
        }

        val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
        check(existingShipments.isEmpty()) {
            "Cannot cancel Purchase Order: linked shipments exist (${existingShipments.size} shipment(s))"
        }

        // Revert supplier outstanding payable if order was confirmed
        if (order.status == PurchaseOrderStatus.CONFIRMED.name) {
            val supplier = supplierDao.getSupplierById(order.supplierId)
            if (supplier != null) {
                val newPayable = (supplier.outstandingPayableMinor - order.totalBdtAmountMinor).coerceAtLeast(0L)
                supplierDao.updateOutstandingPayable(order.supplierId, newPayable)
            }
        }

        val updatedNotes = if (!reason.isNullOrBlank()) {
            "${order.notes ?: ""}\n[Cancellation Reason: $reason]".trim()
        } else order.notes

        database.withTransaction {
            purchaseOrderDao.updateOrder(
                order.copy(
                    status = PurchaseOrderStatus.CANCELLED.name,
                    notes = updatedNotes,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ==========================================
    // 2. Supplier Payments
    // ==========================================

    override fun getAllSupplierPayments(): Flow<List<SupplierPayment>> = supplierPaymentDao.getAllPayments()

    override fun getPaymentsForOrder(orderId: String): Flow<List<SupplierPayment>> = supplierPaymentDao.getPaymentsByOrder(orderId)

    override suspend fun getPaymentsForOrderSync(orderId: String): List<SupplierPayment> =
        supplierPaymentDao.getPaymentsByOrderSync(orderId)

    override suspend fun recordSupplierPayment(
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?
    ): String {
        require(amountMinor > 0) { "Payment amount must be positive" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        var targetOrder: PurchaseOrder? = null
        if (orderId != null) {
            targetOrder = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            val remainingBalance = targetOrder.totalAmountMinor - targetOrder.paidAmountMinor
            require(amountMinor <= remainingBalance) {
                "Payment amount ($amountMinor) exceeds remaining PO balance ($remainingBalance)"
            }
        }

        val paymentCount = supplierPaymentDao.getPaymentCount()
        val paymentNumber = "SP-2026-%04d".format(paymentCount + 1)
        val paymentId = "sp-${UUID.randomUUID()}"

        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val payment = SupplierPayment(
            id = paymentId,
            paymentNumber = paymentNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            paymentMethod = method.name,
            paymentAccount = paymentAccount,
            referenceNumber = reference,
            notes = notes,
            paymentDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        supplierPaymentDao.insertPayment(payment)

        // Deduct from supplier outstanding payable
        val supplier = supplierDao.getSupplierById(supplierId)
        if (supplier != null) {
            val newPayable = (supplier.outstandingPayableMinor - bdtAmountMinor).coerceAtLeast(0L)
            supplierDao.updateOutstandingPayable(supplierId, newPayable)
        }

        // If linked to a PO, update PO paid amount and status
        if (orderId != null && targetOrder != null) {
            val newPaidAmount = targetOrder.paidAmountMinor + amountMinor
            val newPaidBdt = targetOrder.paidBdtAmountMinor + bdtAmountMinor
            val newStatus = if (newPaidAmount >= targetOrder.totalAmountMinor) {
                PurchaseOrderStatus.PAID.name
            } else {
                PurchaseOrderStatus.PARTIALLY_PAID.name
            }
            purchaseOrderDao.recordPayment(orderId, amountMinor, bdtAmountMinor, newStatus)
        }

        // Automatic Double-Entry Accounting:
        // Dr. Accounts Payable (2010)
        // Cr. Bank (1020)
        try {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Supplier Payment $paymentNumber to ${supplier?.companyName ?: supplierId}",
                descBn = "সরবরাহকারী পেমেন্ট $paymentNumber (${supplier?.companyName ?: ""})",
                referenceType = "SUPPLIER_PAYMENT",
                referenceId = paymentNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-2010", // Accounts Payable
                        isDebit = true,
                        amountMinor = bdtAmountMinor,
                        description = "Debit Accounts Payable for remittance $paymentNumber"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Credit Bank Account for remittance $paymentNumber"
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paymentId
    }

    // ==========================================
    // 3. Import Shipments & Cartons
    // ==========================================

    override fun getAllShipments(): Flow<List<ImportShipment>> = importShipmentDao.getAllShipments()

    override suspend fun getShipmentById(id: String): ImportShipment? = importShipmentDao.getShipmentById(id)

    override fun getShipmentsForOrder(orderId: String): Flow<List<ImportShipment>> = importShipmentDao.getShipmentsForOrder(orderId)

    override suspend fun getShipmentsForOrderSync(orderId: String): List<ImportShipment> = importShipmentDao.getShipmentsByOrderIdSync(orderId)

    override fun getCartonsForShipment(shipmentId: String): Flow<List<ShipmentCarton>> = importShipmentDao.getCartonsForShipment(shipmentId)

    override suspend fun createShipment(
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartons: List<ShipmentCartonDraft>,
        notes: String?
    ): String {
        require(cartons.isNotEmpty()) { "Shipment must contain at least 1 carton" }

        // Multi-shipment quantity validation against PO if linked
        if (orderId != null) {
            val order = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            val orderItems = purchaseOrderDao.getItemsForOrderSync(orderId)
            val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
                .filter { it.status != ShipmentStatus.CANCELLED.name }
            val existingCartons = existingShipments.flatMap { importShipmentDao.getCartonsForShipmentSync(it.id) }

            val alreadyShippedByProduct = existingCartons.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }
            val newShippedByProduct = cartons.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

            newShippedByProduct.forEach { (key, newQty) ->
                val matchingOrderItem = orderItems.find { it.productId == key.first && it.variantId == key.second }
                val orderedQty = matchingOrderItem?.quantity ?: 0
                require(orderedQty > 0) {
                    val prodName = productDao.getProductById(key.first)?.nameEn ?: key.first
                    "Product $prodName is not part of Purchase Order ${order.poNumber}"
                }
                val prevShipped = alreadyShippedByProduct[key] ?: 0
                val remainingUnshipped = (orderedQty - prevShipped).coerceAtLeast(0)
                require(newQty <= remainingUnshipped) {
                    val prodName = productDao.getProductById(key.first)?.nameEn ?: key.first
                    "Shipment quantity ($newQty) exceeds remaining unshipped quantity ($remainingUnshipped) for product $prodName"
                }
            }
        }

        val shipmentCount = importShipmentDao.getShipmentCount()
        val shipmentNumber = "SHP-2026-%04d".format(shipmentCount + 1)
        val shipmentId = "shp-${UUID.randomUUID()}"

        var totalQty = 0
        var totalGrossWeight = BigDecimal.ZERO
        var totalNetWeight = BigDecimal.ZERO
        var totalCbm = BigDecimal.ZERO

        val cartonEntities = cartons.map { draft ->
            require(draft.quantity > 0) { "Carton quantity must be positive" }
            require(draft.lengthCm > BigDecimal.ZERO && draft.widthCm > BigDecimal.ZERO && draft.heightCm > BigDecimal.ZERO) {
                "Carton dimensions must be greater than zero"
            }
            require(draft.grossWeightKg >= BigDecimal.ZERO && draft.netWeightKg >= BigDecimal.ZERO) {
                "Weights cannot be negative"
            }

            val cbm = CbmCalculator.calculateCbm(draft.lengthCm, draft.widthCm, draft.heightCm, 1)

            totalQty += draft.quantity
            totalGrossWeight = totalGrossWeight.add(draft.grossWeightKg)
            totalNetWeight = totalNetWeight.add(draft.netWeightKg)
            totalCbm = totalCbm.add(cbm)

            ShipmentCarton(
                id = "ctn-${UUID.randomUUID()}",
                shipmentId = shipmentId,
                cartonNumber = draft.cartonNumber,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                lengthCm = draft.lengthCm.toPlainString(),
                widthCm = draft.widthCm.toPlainString(),
                heightCm = draft.heightCm.toPlainString(),
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = cbm.toPlainString()
            )
        }

        val shipment = ImportShipment(
            id = shipmentId,
            shipmentNumber = shipmentNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = method.name,
            origin = origin,
            destination = destination,
            trackingReference = trackingRef,
            shippingLine = shippingLine,
            shippingDate = System.currentTimeMillis(),
            estimatedArrivalDate = System.currentTimeMillis() + (if (method == ShippingMethod.AIR) 5L else 20L) * 24 * 3600 * 1000,
            status = ShipmentStatus.IN_TRANSIT.name,
            totalCartons = cartons.size,
            totalQuantity = totalQty,
            totalGrossWeightKg = totalGrossWeight.toPlainString(),
            totalNetWeightKg = totalNetWeight.toPlainString(),
            totalCbm = totalCbm.toPlainString(),
            notes = notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        importShipmentDao.saveShipmentWithCartons(shipment, cartonEntities)

        if (orderId != null) {
            purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.SHIPPED.name)
        }

        return shipmentId
    }

    override suspend fun addCartonToShipment(shipmentId: String, carton: ShipmentCartonDraft): String {
        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        require(carton.quantity > 0) { "Carton quantity must be positive" }
        require(carton.lengthCm > BigDecimal.ZERO && carton.widthCm > BigDecimal.ZERO && carton.heightCm > BigDecimal.ZERO) {
            "Carton dimensions must be greater than zero"
        }
        require(carton.grossWeightKg >= BigDecimal.ZERO && carton.netWeightKg >= BigDecimal.ZERO) {
            "Weights cannot be negative"
        }

        val cbm = CbmCalculator.calculateCbm(carton.lengthCm, carton.widthCm, carton.heightCm, 1)
        val cartonId = "ctn-${UUID.randomUUID()}"
        val cartonEntity = ShipmentCarton(
            id = cartonId,
            shipmentId = shipmentId,
            cartonNumber = carton.cartonNumber,
            productId = carton.productId,
            variantId = carton.variantId,
            quantity = carton.quantity,
            lengthCm = carton.lengthCm.toPlainString(),
            widthCm = carton.widthCm.toPlainString(),
            heightCm = carton.heightCm.toPlainString(),
            grossWeightKg = carton.grossWeightKg.toPlainString(),
            netWeightKg = carton.netWeightKg.toPlainString(),
            cbm = cbm.toPlainString()
        )

        importShipmentDao.insertCarton(cartonEntity)

        // Recalculate shipment totals
        val allCartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        val totalQty = allCartons.sumOf { it.quantity }
        val totalGross = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg)) }
        val totalNet = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.netWeightKg)) }
        val totalCbm = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.cbm)) }

        val updatedShipment = shipment.copy(
            totalCartons = allCartons.size,
            totalQuantity = totalQty,
            totalGrossWeightKg = totalGross.toPlainString(),
            totalNetWeightKg = totalNet.toPlainString(),
            totalCbm = totalCbm.toPlainString(),
            updatedAt = System.currentTimeMillis()
        )
        importShipmentDao.updateShipment(updatedShipment)

        return cartonId
    }

    override suspend fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus) {
        val arrivalDate = if (status == ShipmentStatus.ARRIVED || status == ShipmentStatus.RECEIVED) {
            System.currentTimeMillis()
        } else null
        importShipmentDao.updateShipmentStatus(shipmentId, status.name, arrivalDate)

        if (status == ShipmentStatus.CANCELLED) {
            val shipment = importShipmentDao.getShipmentById(shipmentId)
            val orderId = shipment?.purchaseOrderId
            if (orderId != null) {
                val activeShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
                    .filter { it.id != shipmentId && it.status != ShipmentStatus.CANCELLED.name }
                if (activeShipments.isEmpty()) {
                    purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.CONFIRMED.name)
                }
            }
        }
    }

    // ==========================================
    // 4. Cost Management & Landed Cost Engine
    // ==========================================

    override fun getCostItemsForShipment(shipmentId: String): Flow<List<ImportCostItem>> = importCostDao.getCostItemsForShipment(shipmentId)

    override suspend fun addCostItem(
        shipmentId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?
    ): String {
        require(amountMinor > 0) { "Cost amount must be greater than zero" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        val costId = "cost-${UUID.randomUUID()}"
        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val costItem = ImportCostItem(
            id = costId,
            shipmentId = shipmentId,
            costCategory = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = paymentAccount,
            documentReference = docRef,
            allocationMethod = method.name,
            isPosted = true,
            createdAt = System.currentTimeMillis()
        )

        importCostDao.insertCostItem(costItem)

        // Automatic Accounting:
        // Dr. In-Transit Inventory (1220)
        // Cr. Bank (1020)
        try {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Import Expense: $description ($currency $amountMinor)",
                descBn = "আমদানি ব্যয়: $description",
                referenceType = "IMPORT_EXPENSE",
                referenceId = costId,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1220", // In-Transit Inventory
                        isDebit = true,
                        amountMinor = bdtAmountMinor,
                        description = "Capitalize import expense $description"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Payment for import expense $description"
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return costId
    }

    override suspend fun deleteCostItem(costItemId: String) {
        importCostDao.deleteCostItem(costItemId)
    }

    override suspend fun calculateAndSaveLandedCost(shipmentId: String): List<LandedCostSnapshot> {
        val costItems = importCostDao.getCostItemsForShipmentSync(shipmentId)
        val cartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        require(cartons.isNotEmpty()) { "Cannot calculate landed cost without cartons in shipment" }

        // Group cartons by product & variant
        val productGroups = cartons.groupBy { it.productId to it.variantId }

        val allocationInputs = productGroups.map { (key, groupCartons) ->
            val productId = key.first
            val variantId = key.second

            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null

            val totalQty = groupCartons.sumOf { it.quantity }
            val totalGrossWeight = groupCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg)) }
            val totalCbm = groupCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.cbm)) }

            val unitPurchaseCostMinor = variant?.variantCostMinor ?: product?.purchasePriceMinor ?: 0L
            val totalPurchaseValueBdt = totalQty.toLong() * unitPurchaseCostMinor

            val name = "${product?.nameEn ?: productId}${if (variant != null) " - ${variant.color ?: ""}" else ""}"

            AllocationItemInput(
                id = if (variantId != null) "$productId:$variantId" else productId,
                name = name,
                quantity = totalQty,
                grossWeightKg = totalGrossWeight,
                cbm = totalCbm,
                purchaseValueBdtMinor = totalPurchaseValueBdt
            )
        }

        val allAllocations = mutableListOf<LandedCostAllocation>()
        val totalAllocatedPerItem = mutableMapOf<String, Long>()

        // For each cost item, run the engine
        costItems.forEach { costItem ->
            val method = AllocationMethod.valueOf(costItem.allocationMethod)
            val results = LandedCostAllocationEngine.allocate(costItem.bdtAmountMinor, allocationInputs, method)

            results.forEach { res ->
                val parts = res.itemId.split(":")
                val productId = parts[0]
                val variantId = if (parts.size > 1) parts[1] else null

                allAllocations.add(
                    LandedCostAllocation(
                        id = "lca-${UUID.randomUUID()}",
                        shipmentId = shipmentId,
                        costItemId = costItem.id,
                        productId = productId,
                        variantId = variantId,
                        allocationBasis = costItem.allocationMethod,
                        basisValue = res.basisValue.toPlainString(),
                        basisPercentage = res.basisPercentage.toPlainString(),
                        allocatedBdtMinor = res.allocatedBdtMinor,
                        createdAt = System.currentTimeMillis()
                    )
                )

                totalAllocatedPerItem[res.itemId] = (totalAllocatedPerItem[res.itemId] ?: 0L) + res.allocatedBdtMinor
            }
        }

        // Create immutable snapshots per product
        val snapshots = allocationInputs.map { input ->
            val parts = input.id.split(":")
            val productId = parts[0]
            val variantId = if (parts.size > 1) parts[1] else null

            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null

            val totalAllocatedBdt = totalAllocatedPerItem[input.id] ?: 0L
            val totalPurchaseBdt = input.purchaseValueBdtMinor
            val totalLandedBdt = totalPurchaseBdt + totalAllocatedBdt
            val unitLandedBdt = if (input.quantity > 0) {
                totalLandedBdt / input.quantity.toLong()
            } else 0L

            LandedCostSnapshot(
                id = "lcs-${UUID.randomUUID()}",
                shipmentId = shipmentId,
                productId = productId,
                variantId = variantId,
                productNameEn = product?.nameEn ?: productId,
                productNameBn = product?.nameBn ?: "",
                sku = variant?.sku ?: product?.baseSku ?: "",
                totalReceivedQuantity = input.quantity,
                totalPurchaseCostBdtMinor = totalPurchaseBdt,
                totalAllocatedCostsBdtMinor = totalAllocatedBdt,
                totalLandedCostBdtMinor = totalLandedBdt,
                unitLandedCostBdtMinor = unitLandedBdt,
                snapshotDate = System.currentTimeMillis(),
                isFinal = true
            )
        }

        importCostDao.replaceAllocationsAndSnapshots(shipmentId, allAllocations, snapshots)
        return snapshots
    }

    override suspend fun getCostItemsForShipmentSync(shipmentId: String): List<ImportCostItem> =
        importCostDao.getCostItemsForShipmentSync(shipmentId)

    override suspend fun adjustLandedCost(
        shipmentId: String,
        newCostCategory: CostCategory,
        newAmountMinor: Long,
        exchangeRate: BigDecimal,
        reason: String
    ): CostAdjustmentResult {
        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        val existingCosts = importCostDao.getCostItemsForShipmentSync(shipmentId)
        val oldTotalExpenseBdt = existingCosts.sumOf { it.bdtAmountMinor }

        val bdtAmountMinor = BigDecimal(newAmountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val costId = "cost-${UUID.randomUUID()}"
        val costItem = ImportCostItem(
            id = costId,
            shipmentId = shipmentId,
            costCategory = newCostCategory.name,
            description = "Cost Adjustment: $reason",
            amountMinor = newAmountMinor,
            currency = "BDT",
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = "Accounts Payable (Landed Cost Clearing)",
            documentReference = MovementNumberGenerator.generate("ADJ"),
            allocationMethod = newCostCategory.defaultAllocationMethod.name,
            isPosted = true
        )
        importCostDao.insertCostItem(costItem)

        val newTotalExpenseBdt = oldTotalExpenseBdt + bdtAmountMinor
        val varianceBdtMinor = bdtAmountMinor

        val layers = inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)
        val totalInitialQty = layers.sumOf { it.initialQuantity }

        var unitVariance = 0L
        var remainingInventoryAdjustment = 0L
        var cogsAdjustment = 0L
        val journalEntryNumber = MovementNumberGenerator.generate("JV-ADJ")

        if (totalInitialQty > 0 && varianceBdtMinor != 0L) {
            unitVariance = BigDecimal(varianceBdtMinor)
                .divide(BigDecimal(totalInitialQty), 0, RoundingMode.HALF_UP)
                .longValueExact()

            layers.forEach { layer ->
                val soldQty = layer.initialQuantity - layer.remainingQuantity
                val remQty = layer.remainingQuantity

                val layerRemAdj = remQty.toLong() * unitVariance
                val layerSoldAdj = soldQty.toLong() * unitVariance

                remainingInventoryAdjustment += layerRemAdj
                cogsAdjustment += layerSoldAdj

                inventoryCostLayerDao.updateCostLayerUnitCost(
                    layerId = layer.id,
                    unitCostMinor = layer.unitLandedCostBdtMinor + unitVariance,
                    allocatedExpenseMinor = layer.allocatedExpensePerUnitBdtMinor + unitVariance
                )
            }

            val unallocated = varianceBdtMinor - (remainingInventoryAdjustment + cogsAdjustment)
            cogsAdjustment += unallocated

            try {
                val lines = mutableListOf<JournalLineDraft>()
                if (remainingInventoryAdjustment > 0L) {
                    lines.add(
                        JournalLineDraft(
                            accountId = "coa-1210",
                            isDebit = true,
                            amountMinor = remainingInventoryAdjustment,
                            description = "Landed cost adjustment for remaining inventory ($shipmentId)"
                        )
                    )
                }
                if (cogsAdjustment > 0L) {
                    lines.add(
                        JournalLineDraft(
                            accountId = "coa-5010",
                            isDebit = true,
                            amountMinor = cogsAdjustment,
                            description = "Landed cost adjustment for sold inventory ($shipmentId)"
                        )
                    )
                }
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-2010",
                        isDebit = false,
                        amountMinor = varianceBdtMinor,
                        description = "Landed cost adjustment payable/clearing ($shipmentId)"
                    )
                )

                if (lines.isNotEmpty()) {
                    accountingRepository.postJournalEntry(
                        entryDate = System.currentTimeMillis(),
                        descEn = "Actual Landed Cost Adjustment for ${shipment.shipmentNumber}",
                        descBn = "প্রকৃত আমদানি ব্যয় সমন্বয় (${shipment.shipmentNumber})",
                        referenceType = "LANDED_COST_ADJUSTMENT",
                        referenceId = shipment.shipmentNumber,
                        lines = lines
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return CostAdjustmentResult(
            shipmentId = shipmentId,
            previousTotalExpenseBdtMinor = oldTotalExpenseBdt,
            actualTotalExpenseBdtMinor = newTotalExpenseBdt,
            costVarianceBdtMinor = varianceBdtMinor,
            unitVarianceBdtMinor = unitVariance,
            remainingInventoryAdjustmentBdtMinor = remainingInventoryAdjustment,
            cogsAdjustmentBdtMinor = cogsAdjustment,
            unallocatedVarianceBdtMinor = 0L,
            journalEntryNumber = journalEntryNumber
        )
    }

    override fun getLandedCostSnapshots(shipmentId: String): Flow<List<LandedCostSnapshot>> = importCostDao.getSnapshotsForShipment(shipmentId)

    override fun getAllocationsForShipment(shipmentId: String): Flow<List<LandedCostAllocation>> = importCostDao.getAllocationsForShipment(shipmentId)

    // ==========================================
    // 5. Warehouse Receiving & Cost Layers
    // ==========================================

    override fun getAllWarehouseReceipts(): Flow<List<WarehouseReceipt>> = warehouseReceiptDao.getAllReceipts()

    override fun getReceiptsForShipment(shipmentId: String): Flow<List<WarehouseReceipt>> = warehouseReceiptDao.getReceiptsByShipment(shipmentId)

    override fun getReceiptItems(receiptId: String): Flow<List<WarehouseReceiptItem>> = warehouseReceiptDao.getItemsForReceipt(receiptId)

    override suspend fun getReceiptItemsSync(receiptId: String): List<WarehouseReceiptItem> = warehouseReceiptDao.getItemsForReceiptSync(receiptId)

    override suspend fun createWarehouseReceipt(
        shipmentId: String,
        warehouseId: String,
        items: List<ReceiptItemDraft>,
        notes: String?
    ): String {
        require(items.isNotEmpty()) { "Receipt must contain at least 1 item" }

        val receiptCount = warehouseReceiptDao.getReceiptCount()
        val receiptNumber = "WR-2026-%04d".format(receiptCount + 1)
        val receiptId = "wr-${UUID.randomUUID()}"

        // Lookup snapshots to set estimated landed cost
        val snapshots = importCostDao.getSnapshotsForShipmentSync(shipmentId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        var hasDiscrepancy = false

        val itemEntities = items.map { draft ->
            val key = if (draft.variantId != null) "${draft.productId}:${draft.variantId}" else draft.productId
            val snapshot = snapshots[key]
            val unitLandedCost = snapshot?.unitLandedCostBdtMinor ?: 0L

            if (draft.receivedQuantity != draft.expectedQuantity || draft.damagedQuantity > 0 || draft.missingQuantity > 0) {
                hasDiscrepancy = true
            }

            WarehouseReceiptItem(
                id = "wri-${UUID.randomUUID()}",
                receiptId = receiptId,
                cartonId = draft.cartonId,
                productId = draft.productId,
                variantId = draft.variantId,
                expectedQuantity = draft.expectedQuantity,
                receivedQuantity = draft.receivedQuantity,
                damagedQuantity = draft.damagedQuantity,
                missingQuantity = draft.missingQuantity,
                excessQuantity = draft.excessQuantity,
                unitLandedCostMinor = unitLandedCost,
                conditionNotes = draft.conditionNotes
            )
        }

        val receipt = WarehouseReceipt(
            id = receiptId,
            receiptNumber = receiptNumber,
            shipmentId = shipmentId,
            warehouseId = warehouseId,
            receiptDate = System.currentTimeMillis(),
            status = ReceiptStatus.DRAFT.name,
            receivedBy = "Central Warehouse In-Charge",
            hasDiscrepancies = hasDiscrepancy,
            discrepancyApproved = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        warehouseReceiptDao.saveReceiptWithItems(receipt, itemEntities)
        return receiptId
    }

    override suspend fun postWarehouseReceipt(receiptId: String, allowDiscrepancy: Boolean) {
        val receipt = warehouseReceiptDao.getReceiptById(receiptId)
            ?: throw IllegalArgumentException("Warehouse receipt not found: $receiptId")

        // CRITICAL CHECK: Prevent duplicate posting
        check(receipt.status != ReceiptStatus.POSTED.name) {
            "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
        }

        val items = warehouseReceiptDao.getItemsForReceiptSync(receiptId)
        require(items.isNotEmpty()) { "Cannot post empty warehouse receipt" }

        // Check for excess discrepancies without approval
        val excessItem = items.find { it.receivedQuantity > it.expectedQuantity }
        if (excessItem != null && !allowDiscrepancy && !receipt.discrepancyApproved) {
            throw IllegalStateException(
                "Received quantity (${excessItem.receivedQuantity}) exceeds expected (${excessItem.expectedQuantity}). Explicit approval required!"
            )
        }

        val shipment = importShipmentDao.getShipmentById(receipt.shipmentId)
        val linkedPoId = receipt.purchaseOrderId ?: shipment?.purchaseOrderId
        if (linkedPoId != null) {
            val linkedPo = purchaseOrderDao.getOrderById(linkedPoId)
            if (linkedPo != null && !linkedPo.isActualCostFinalized) {
                throw IllegalStateException("Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!")
            }
        }

        // ATOMIC DATABASE TRANSACTION
        database.withTransaction {
            var totalCapitalizedValueBdt = 0L

            items.forEach { item ->
                // Effective sound sellable stock:
                val goodQuantity = if (item.receivedQuantity + item.damagedQuantity + item.missingQuantity == item.expectedQuantity) {
                    item.receivedQuantity
                } else {
                    (item.receivedQuantity - item.damagedQuantity).coerceAtLeast(0)
                }

                var balanceAfterReceipt = (if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                } else {
                    stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                })?.quantity ?: 0

                if (goodQuantity > 0) {
                    // 1. Update StockBalance with sound sellable quantity
                    val existingBalance = if (item.variantId != null) {
                        stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                    } else {
                        stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                    }

                    val newBalance = if (existingBalance != null) {
                        existingBalance.copy(
                            quantity = existingBalance.quantity + goodQuantity,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        StockBalance(
                            id = "bal-${UUID.randomUUID()}",
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = goodQuantity,
                            reservedQuantity = 0,
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                    stockBalanceDao.insertOrUpdate(newBalance)
                    balanceAfterReceipt = newBalance.quantity

                    // 2. Audit Trail: Create StockMovement for sound stock
                    val movementNumber = MovementNumberGenerator.generate("SM-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov"),
                            movementNumber = movementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.PURCHASE_RECEIPT.code,
                            quantity = goodQuantity,
                            unitCostMinor = item.unitLandedCostMinor,
                            balanceAfterMovement = newBalance.quantity,
                            totalCostImpactMinor = goodQuantity.toLong() * item.unitLandedCostMinor,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Import receiving check-in from ${shipment?.shipmentNumber ?: receipt.shipmentId}",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // 3. Batch Cost Layer for sound stock
                    val product = productDao.getProductById(item.productId)
                    val unitCost = item.unitLandedCostMinor
                    totalCapitalizedValueBdt += (goodQuantity.toLong() * unitCost)

                    inventoryCostLayerDao.insertCostLayer(
                        InventoryCostLayer(
                            id = "icl-${UUID.randomUUID()}",
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = receipt.warehouseId,
                            shipmentId = receipt.shipmentId,
                            shipmentNumber = shipment?.shipmentNumber ?: "SHP-UNKNOWN",
                            warehouseReceiptId = receipt.id,
                            receiptNumber = receipt.receiptNumber,
                            initialQuantity = goodQuantity,
                            remainingQuantity = goodQuantity,
                            unitLandedCostBdtMinor = unitCost,
                            originalCurrency = shipment?.let { "CNY" } ?: "BDT",
                            originalPurchasePriceMinor = product?.purchasePriceMinor ?: 0L,
                            exchangeRateToBdt = "17.0000",
                            allocatedExpensePerUnitBdtMinor = (unitCost - (product?.purchasePriceMinor ?: 0L)).coerceAtLeast(0L),
                            receiptDate = receipt.receiptDate,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 4. Record Damaged Stock (Excluded from sellable stock)
                if (item.damagedQuantity > 0) {
                    val dmgMovementNumber = MovementNumberGenerator.generate("DMG-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-dmg"),
                            movementNumber = dmgMovementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                            quantity = item.damagedQuantity,
                            unitCostMinor = item.unitLandedCostMinor,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = item.damagedQuantity.toLong() * item.unitLandedCostMinor,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Damaged during transit. Excluded from sellable stock. Notes: ${item.conditionNotes ?: "Damaged"}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 5. Record Missing Stock (Excluded from stock)
                if (item.missingQuantity > 0) {
                    val misMovementNumber = MovementNumberGenerator.generate("MIS-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-mis"),
                            movementNumber = misMovementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SHORTAGE.code,
                            quantity = item.missingQuantity,
                            unitCostMinor = item.unitLandedCostMinor,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = item.missingQuantity.toLong() * item.unitLandedCostMinor,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Missing / Shortage in transit. Excluded from stock.",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // 4. Mark receipt as POSTED
            warehouseReceiptDao.markReceiptPosted(
                receiptId = receiptId,
                status = ReceiptStatus.POSTED.name,
                postedAt = System.currentTimeMillis()
            )

            // 5. Update shipment status
            importShipmentDao.updateShipmentStatus(
                shipmentId = receipt.shipmentId,
                status = ShipmentStatus.COMPLETED.name,
                arrivalDate = receipt.receiptDate,
                updatedAt = System.currentTimeMillis()
            )

            // 6. Double-Entry Accounting Event:
            // Dr. Inventory Asset (Warehouse) (1210)
            // Cr. In-Transit Inventory (1220)
            if (totalCapitalizedValueBdt > 0L) {
                try {
                    accountingRepository.postJournalEntry(
                        entryDate = receipt.receiptDate,
                        descEn = "Inventory Capitalization for ${receipt.receiptNumber}",
                        descBn = "মজুদ পণ্য ক্যাপিটালাইজেশন (${receipt.receiptNumber})",
                        referenceType = "WAREHOUSE_RECEIPT",
                        referenceId = receipt.receiptNumber,
                        lines = listOf(
                            JournalLineDraft(
                                accountId = "coa-1210", // Inventory Asset
                                isDebit = true,
                                amountMinor = totalCapitalizedValueBdt,
                                description = "Capitalize received goods into warehouse inventory"
                            ),
                            JournalLineDraft(
                                accountId = "coa-1220", // In-Transit Inventory
                                isDebit = false,
                                amountMinor = totalCapitalizedValueBdt,
                                description = "Clear in-transit inventory after receiving check-in"
                            )
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getAllCostLayers(): Flow<List<InventoryCostLayer>> = inventoryCostLayerDao.getAllCostLayers()

    override fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>> = inventoryCostLayerDao.getActiveLayersForProduct(productId)

    override suspend fun getCostLayersForShipmentSync(shipmentId: String): List<InventoryCostLayer> =
        inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)

    // ==========================================
    // 5. Minimal Import Management System Enhancements
    // ==========================================

    override suspend fun addMixedCarton(
        orderId: String,
        cartonNumber: String,
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        grossWeightKg: BigDecimal,
        netWeightKg: BigDecimal,
        lines: List<MixedCartonLineDraft>
    ): List<String> {
        require(lines.isNotEmpty()) { "Mixed carton must have at least one product line" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        // Run allocation engine
        val lineInputs = lines.map {
            MixedCartonLineInput(
                productId = it.productId,
                variantId = it.variantId,
                productName = it.productName,
                quantity = it.quantity
            )
        }
        val allocated = CartonWeightAllocationEngine.allocateWeights(
            cartonGrossWeightKg = grossWeightKg,
            cartonNetWeightKg = netWeightKg,
            lines = lineInputs
        )

        val cbm = CbmCalculator.calculateCbm(lengthCm, widthCm, heightCm, 1)
        val totalQty = lines.sumOf { it.quantity }

        val insertedIds = mutableListOf<String>()
        allocated.forEach { alloc ->
            val cartonId = "ctn-${UUID.randomUUID()}"
            val cartonEntity = ShipmentCarton(
                id = cartonId,
                shipmentId = batchShipment.id,
                cartonNumber = cartonNumber,
                productId = alloc.productId,
                variantId = alloc.variantId,
                quantity = alloc.quantity,
                lengthCm = lengthCm.toPlainString(),
                widthCm = widthCm.toPlainString(),
                heightCm = heightCm.toPlainString(),
                grossWeightKg = alloc.allocatedGrossWeightKg.toPlainString(),
                netWeightKg = alloc.allocatedNetWeightKg.toPlainString(),
                cbm = cbm.toPlainString(),
                purchaseOrderId = orderId,
                isMixedCarton = true,
                totalCartonQuantity = totalQty,
                cartonGrossWeightKg = grossWeightKg.toPlainString()
            )
            importShipmentDao.insertCarton(cartonEntity)
            insertedIds.add(cartonId)
        }

        // Refresh totals on purchase order and batch shipment
        val allCartons = importShipmentDao.getCartonsForOrderSync(orderId)
        val distinctCartonsCount = allCartons.map { it.cartonNumber }.distinct().size
        val totalGross = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg.ifBlank { "0" })) }
        val totalNet = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.netWeightKg.ifBlank { "0" })) }
        val totalCbm = allCartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, cList ->
            acc.add(BigDecimal(cList.first().cbm.ifBlank { "0" }))
        }

        purchaseOrderDao.updatePackingSummary(
            orderId = orderId,
            grossWeight = totalGross.toPlainString(),
            netWeight = totalNet.toPlainString(),
            cbm = totalCbm.toPlainString(),
            cartons = distinctCartonsCount
        )

        return insertedIds
    }

    override fun getCartonsForOrder(orderId: String): Flow<List<ShipmentCarton>> =
        importShipmentDao.getCartonsForOrder(orderId)

    override suspend fun getCartonsForOrderSync(orderId: String): List<ShipmentCarton> =
        importShipmentDao.getCartonsForOrderSync(orderId)

    override suspend fun addPurchaseCostItem(
        orderId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        isEstimated: Boolean,
        paymentAccount: String,
        docRef: String?,
        notes: String?
    ): String {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val costId = "cost-${UUID.randomUUID()}"
        val costItem = ImportCostItem(
            id = costId,
            shipmentId = batchShipment.id,
            purchaseOrderId = orderId,
            costCategory = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = paymentAccount,
            documentReference = docRef,
            allocationMethod = method.name,
            isPosted = false,
            isEstimated = isEstimated,
            isPaid = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        importCostDao.insertCostItem(costItem)
        return costId
    }

    override fun getCostItemsForOrder(orderId: String): Flow<List<ImportCostItem>> =
        importCostDao.getCostItemsForOrder(orderId)

    override suspend fun getCostItemsForOrderSync(orderId: String): List<ImportCostItem> =
        importCostDao.getCostItemsForOrderSync(orderId)

    override suspend fun finalizePurchaseActualCost(orderId: String): List<LandedCostSnapshot> {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        check(!po.isActualCostFinalized) {
            "Duplicate finalization prevented: Purchase order ${po.poNumber} actual costs are already finalized!"
        }

        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val actualCosts = importCostDao.getActualCostItemsForOrderSync(orderId)
        require(actualCosts.isNotEmpty()) {
            "Cannot finalize landed cost: No actual cost items recorded for purchase ${po.poNumber}!"
        }

        // Get product items for allocation inputs
        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        require(poItems.isNotEmpty()) { "Purchase order has no items to allocate costs" }

        val cartons = importShipmentDao.getCartonsForOrderSync(orderId)

        val allocationInputs = poItems.map { item ->
            val product = productDao.getProductById(item.productId)
            val variant = if (item.variantId != null) productVariantDao.getVariantById(item.variantId) else null

            // Weight: from cartons if entered, else from poItem summary
            val itemCartons = cartons.filter { it.productId == item.productId && (item.variantId == null || it.variantId == item.variantId) }
            val grossWeight = if (itemCartons.isNotEmpty()) {
                itemCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg.ifBlank { "0" })) }
            } else {
                BigDecimal(item.grossWeightKg.ifBlank { "0" })
            }

            val cbm = if (itemCartons.isNotEmpty()) {
                itemCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.cbm.ifBlank { "0" })) }
            } else {
                BigDecimal(item.cbm.ifBlank { "0" })
            }

            val name = "${product?.nameEn ?: item.productId}${if (variant != null) " - ${variant.color ?: ""}" else ""}"

            AllocationItemInput(
                id = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId,
                name = name,
                quantity = item.quantity,
                grossWeightKg = grossWeight,
                cbm = cbm,
                purchaseValueBdtMinor = item.totalBdtAmountMinor
            )
        }

        val allAllocations = mutableListOf<LandedCostAllocation>()
        val totalAllocatedPerItem = mutableMapOf<String, Long>()
        val totalActualCostBdt = actualCosts.sumOf { it.bdtAmountMinor }
        var totalAllocatedExpense = 0L

        actualCosts.forEach { costItem ->
            val method = AllocationMethod.valueOf(costItem.allocationMethod)
            val results = LandedCostAllocationEngine.allocate(costItem.bdtAmountMinor, allocationInputs, method)

            results.forEach { res ->
                val parts = res.itemId.split(":")
                val productId = parts[0]
                val variantId = if (parts.size > 1) parts[1] else null

                allAllocations.add(
                    LandedCostAllocation(
                        id = "lca-${UUID.randomUUID()}",
                        shipmentId = batchShipment.id,
                        costItemId = costItem.id,
                        productId = productId,
                        variantId = variantId,
                        allocationBasis = costItem.allocationMethod,
                        basisValue = res.basisValue.toPlainString(),
                        basisPercentage = res.basisPercentage.toPlainString(),
                        allocatedBdtMinor = res.allocatedBdtMinor,
                        createdAt = System.currentTimeMillis()
                    )
                )

                totalAllocatedPerItem[res.itemId] = (totalAllocatedPerItem[res.itemId] ?: 0L) + res.allocatedBdtMinor
                totalAllocatedExpense += res.allocatedBdtMinor
            }
        }

        // Verify reconciliation: total allocated must match total actual cost exactly
        check(totalAllocatedExpense == totalActualCostBdt) {
            "Cost allocation mismatch: Total allocated ($totalAllocatedExpense) != Total actual ($totalActualCostBdt)"
        }

        // Build finalized immutable snapshots
        val snapshots = allocationInputs.map { input ->
            val parts = input.id.split(":")
            val productId = parts[0]
            val variantId = if (parts.size > 1) parts[1] else null

            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null

            val allocatedBdt = totalAllocatedPerItem[input.id] ?: 0L
            val purchaseBdt = input.purchaseValueBdtMinor
            val landedBdt = purchaseBdt + allocatedBdt
            val unitLandedBdt = if (input.quantity > 0) landedBdt / input.quantity.toLong() else 0L

            LandedCostSnapshot(
                id = "lcs-${UUID.randomUUID()}",
                shipmentId = batchShipment.id,
                productId = productId,
                variantId = variantId,
                productNameEn = product?.nameEn ?: productId,
                productNameBn = product?.nameBn ?: "",
                sku = variant?.sku ?: product?.baseSku ?: "",
                totalReceivedQuantity = input.quantity,
                totalPurchaseCostBdtMinor = purchaseBdt,
                totalAllocatedCostsBdtMinor = allocatedBdt,
                totalLandedCostBdtMinor = landedBdt,
                unitLandedCostBdtMinor = unitLandedBdt,
                snapshotDate = System.currentTimeMillis(),
                isFinal = true
            )
        }

        importCostDao.replaceAllocationsAndSnapshots(batchShipment.id, allAllocations, snapshots)

        // Mark PO and Batch as ACTUAL_COST_FINALIZED
        purchaseOrderDao.markActualCostFinalized(orderId, PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name)
        importShipmentDao.updateShipmentStatus(batchShipment.id, ShipmentStatus.ARRIVED.name)

        return snapshots
    }

    override suspend fun createPurchaseReceiving(
        orderId: String,
        warehouseId: String,
        receivingMethod: String,
        items: List<ReceiptItemDraft>,
        notes: String?
    ): String {
        require(items.isNotEmpty()) { "Receiving must contain at least 1 item" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val receiptCount = warehouseReceiptDao.getReceiptCount()
        val receiptNumber = "WR-2026-%04d".format(receiptCount + 1)
        val receiptId = "wr-${UUID.randomUUID()}"

        // Snapshots if finalized
        val snapshots = importCostDao.getSnapshotsForShipmentSync(batchShipment.id).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        var hasDiscrepancy = false
        val itemEntities = items.map { draft ->
            val key = if (draft.variantId != null) "${draft.productId}:${draft.variantId}" else draft.productId
            val snapshot = snapshots[key]
            val unitLandedCost = snapshot?.unitLandedCostBdtMinor ?: 0L

            if (draft.receivedQuantity != draft.expectedQuantity || draft.damagedQuantity > 0 || draft.missingQuantity > 0) {
                hasDiscrepancy = true
            }

            WarehouseReceiptItem(
                id = "wri-${UUID.randomUUID()}",
                receiptId = receiptId,
                cartonId = draft.cartonId,
                productId = draft.productId,
                variantId = draft.variantId,
                expectedQuantity = draft.expectedQuantity,
                receivedQuantity = draft.receivedQuantity,
                damagedQuantity = draft.damagedQuantity,
                missingQuantity = draft.missingQuantity,
                excessQuantity = draft.excessQuantity,
                unitLandedCostMinor = unitLandedCost,
                conditionNotes = draft.conditionNotes
            )
        }

        val receipt = WarehouseReceipt(
            id = receiptId,
            receiptNumber = receiptNumber,
            shipmentId = batchShipment.id,
            purchaseOrderId = orderId,
            receivingMethod = receivingMethod,
            warehouseId = warehouseId,
            receiptDate = System.currentTimeMillis(),
            status = ReceiptStatus.DRAFT.name,
            receivedBy = "Warehouse Receiving Manager",
            hasDiscrepancies = hasDiscrepancy,
            discrepancyApproved = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        warehouseReceiptDao.saveReceiptWithItems(receipt, itemEntities)
        return receiptId
    }

    override suspend fun postPurchaseReceiving(receiptId: String, allowDiscrepancy: Boolean) {
        val receipt = warehouseReceiptDao.getReceiptById(receiptId)
            ?: throw IllegalArgumentException("Warehouse receipt not found: $receiptId")

        check(receipt.status != ReceiptStatus.POSTED.name) {
            "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
        }

        val orderId = receipt.purchaseOrderId
            ?: importShipmentDao.getShipmentById(receipt.shipmentId)?.purchaseOrderId
            ?: throw IllegalStateException("Receipt is not associated with a Purchase Order: $receiptId")

        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        // CRITICAL BUSINESS RULE: Posting stock is strictly blocked until Actual Landing Cost is finalized!
        check(po.isActualCostFinalized) {
            "Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!"
        }

        val items = warehouseReceiptDao.getItemsForReceiptSync(receiptId)
        require(items.isNotEmpty()) { "Cannot post empty warehouse receipt" }

        val batchShipment = importShipmentDao.getShipmentById(receipt.shipmentId)
        val snapshots = importCostDao.getSnapshotsForShipmentSync(receipt.shipmentId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }
        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        database.withTransaction {
            var totalGoodValueBdt = 0L
            var totalDamageValueBdt = 0L
            var totalMissingValueBdt = 0L

            items.forEach { item ->
                val key = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                val snapshot = snapshots[key]
                val poItem = poItems[key]

                // Calculate precise unit landed cost: purchase cost + allocated landed cost
                val unitPurchaseCostBdt = if (poItem != null && poItem.quantity > 0) {
                    poItem.totalBdtAmountMinor / poItem.quantity.toLong()
                } else {
                    productDao.getProductById(item.productId)?.purchasePriceMinor ?: 0L
                }
                val unitAllocatedCostBdt = if (snapshot != null && snapshot.totalReceivedQuantity > 0) {
                    snapshot.totalAllocatedCostsBdtMinor / snapshot.totalReceivedQuantity.toLong()
                } else 0L

                val finalUnitCost = unitPurchaseCostBdt + unitAllocatedCostBdt

                val goodQuantity = (item.receivedQuantity - item.damagedQuantity - item.missingQuantity).coerceAtLeast(0)

                var balanceAfterReceipt = (if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                } else {
                    stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                })?.quantity ?: 0

                // 1. Post Sound / Good Quantity into Stock
                if (goodQuantity > 0) {
                    val existingBalance = if (item.variantId != null) {
                        stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                    } else {
                        stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                    }

                    val newBalance = if (existingBalance != null) {
                        existingBalance.copy(
                            quantity = existingBalance.quantity + goodQuantity,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        StockBalance(
                            id = "bal-${UUID.randomUUID()}",
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = goodQuantity,
                            reservedQuantity = 0,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    stockBalanceDao.insertOrUpdate(newBalance)
                    balanceAfterReceipt = newBalance.quantity

                    // Stock Movement
                    val movementNumber = MovementNumberGenerator.generate("SM-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov"),
                            movementNumber = movementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.PURCHASE_RECEIPT.code,
                            quantity = goodQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = newBalance.quantity,
                            totalCostImpactMinor = goodQuantity.toLong() * finalUnitCost,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Good stock check-in from ${po.poNumber}",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // FIFO Cost Layer
                    val goodValue = goodQuantity.toLong() * finalUnitCost
                    totalGoodValueBdt += goodValue

                    inventoryCostLayerDao.insertCostLayer(
                        InventoryCostLayer(
                            id = "icl-${UUID.randomUUID()}",
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = receipt.warehouseId,
                            shipmentId = receipt.shipmentId,
                            shipmentNumber = batchShipment?.shipmentNumber ?: "BATCH-${po.poNumber}",
                            warehouseReceiptId = receipt.id,
                            receiptNumber = receipt.receiptNumber,
                            initialQuantity = goodQuantity,
                            remainingQuantity = goodQuantity,
                            unitLandedCostBdtMinor = finalUnitCost,
                            originalCurrency = po.currency,
                            originalPurchasePriceMinor = poItem?.unitPriceMinor ?: 0L,
                            exchangeRateToBdt = po.exchangeRateToBdt,
                            allocatedExpensePerUnitBdtMinor = unitAllocatedCostBdt,
                            receiptDate = receipt.receiptDate,
                            isActive = true,
                            purchaseOrderId = po.id,
                            purchaseBatchId = "BATCH-${po.poNumber}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 2. Damaged Stock Write-Off
                if (item.damagedQuantity > 0) {
                    val dmgLoss = item.damagedQuantity.toLong() * finalUnitCost
                    totalDamageValueBdt += dmgLoss
                    val curQty = stockBalanceDao.getBalance(receipt.warehouseId, item.productId)?.quantity ?: 0

                    val dmgNumber = MovementNumberGenerator.generate("DMG-2026")
                    minimalImportDao.insertDamageRecord(
                        DamageRecord(
                            id = MovementNumberGenerator.generateId("dmg"),
                            recordNumber = dmgNumber,
                            purchaseOrderId = po.id,
                            warehouseReceiptId = receipt.id,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            damagedQuantity = item.damagedQuantity,
                            unitCostMinor = finalUnitCost,
                            totalLossMinor = dmgLoss,
                            reason = item.conditionNotes ?: "Damaged during transit / handling",
                            disposition = "WRITE_OFF",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-dmg"),
                            movementNumber = dmgNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                            quantity = item.damagedQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = dmgLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Damaged during transit. Written off: ${item.conditionNotes ?: ""}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 3. Missing Stock Shortage Discrepancy
                if (item.missingQuantity > 0) {
                    val misLoss = item.missingQuantity.toLong() * finalUnitCost
                    totalMissingValueBdt += misLoss

                    val shtNumber = MovementNumberGenerator.generate("SHT-2026")
                    minimalImportDao.insertShortageRecord(
                        ShortageRecord(
                            id = MovementNumberGenerator.generateId("sht"),
                            recordNumber = shtNumber,
                            purchaseOrderId = po.id,
                            warehouseReceiptId = receipt.id,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            missingQuantity = item.missingQuantity,
                            unitCostMinor = finalUnitCost,
                            totalAmountMinor = misLoss,
                            disposition = "SUPPLIER_CLAIM",
                            claimReference = "CLM-${po.poNumber}",
                            notes = "Missing / Short shipment from supplier",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-mis"),
                            movementNumber = shtNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SHORTAGE.code,
                            quantity = item.missingQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = misLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Missing / Shortage in transit. Excluded from stock.",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // Update PO item received quantity
                if (poItem != null) {
                    purchaseOrderDao.incrementReceivedQuantity(poItem.id, item.receivedQuantity)
                }
            }

            // 4. Mark receipt as POSTED
            warehouseReceiptDao.markReceiptPosted(
                receiptId = receiptId,
                status = ReceiptStatus.POSTED.name,
                postedAt = System.currentTimeMillis()
            )

            // 5. Update PO and batch shipment status
            val updatedPoItems = purchaseOrderDao.getItemsForOrderSync(orderId)
            val totalOrdered = updatedPoItems.sumOf { it.quantity }
            val totalRecv = updatedPoItems.sumOf { it.receivedQuantity }
            val newPoStatus = if (totalRecv >= totalOrdered && totalOrdered > 0) {
                PurchaseOrderStatus.RECEIVED.name
            } else {
                PurchaseOrderStatus.PARTIALLY_RECEIVED.name
            }
            purchaseOrderDao.updateOrderStatus(orderId, newPoStatus)
            importShipmentDao.updateShipmentStatus(receipt.shipmentId, ShipmentStatus.COMPLETED.name, arrivalDate = receipt.receiptDate)

            // 6. Post Double-Entry Accounting Journals
            // Good stock: Dr. 1210 Inventory Asset, Cr. 1220 In-Transit Inventory
            if (totalGoodValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Inventory Capitalization for ${receipt.receiptNumber}",
                    descBn = "মজুদ পণ্য ক্যাপিটালাইজেশন (${receipt.receiptNumber})",
                    referenceType = "WAREHOUSE_RECEIPT",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-1210", // Inventory Asset
                            isDebit = true,
                            amountMinor = totalGoodValueBdt,
                            description = "Capitalize good units into warehouse stock"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalGoodValueBdt,
                            description = "Clear in-transit inventory for received good units"
                        )
                    )
                )
            }

            // Damaged stock: Dr. 5050 Damaged Merchandise Write-Off, Cr. 1220 In-Transit Inventory
            if (totalDamageValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Damaged Goods Write-Off for ${receipt.receiptNumber}",
                    descBn = "নষ্ট পণ্য অবলোপন হিসাব (${receipt.receiptNumber})",
                    referenceType = "DAMAGE_WRITE_OFF",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-5050", // Damaged Merchandise Write-Off
                            isDebit = true,
                            amountMinor = totalDamageValueBdt,
                            description = "Loss on damaged goods during transit"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalDamageValueBdt,
                            description = "Clear in-transit inventory for damaged units"
                        )
                    )
                )
            }

            // Missing stock: Dr. 1050 Supplier Shortage & Claims Receivable, Cr. 1220 In-Transit Inventory
            if (totalMissingValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Shortage Discrepancy Claim for ${receipt.receiptNumber}",
                    descBn = "ঘাটতি ও সরবরাহকারী দাবি (${receipt.receiptNumber})",
                    referenceType = "SHORTAGE_CLAIM",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-1050", // Supplier Shortage & Claims Receivable
                            isDebit = true,
                            amountMinor = totalMissingValueBdt,
                            description = "Supplier claim receivable for missing units in shipment"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalMissingValueBdt,
                            description = "Clear in-transit inventory for missing shortage units"
                        )
                    )
                )
            }
        }
    }

    override fun getReceiptsForOrder(orderId: String): Flow<List<WarehouseReceipt>> =
        warehouseReceiptDao.getReceiptsByOrder(orderId)

    override suspend fun addAdditionalCost(
        orderId: String,
        warehouseId: String?,
        category: AdditionalCostCategory,
        description: String,
        amountMinor: Long,
        isCapitalizable: Boolean,
        allocationMethod: String,
        notes: String?
    ): String {
        require(amountMinor > 0) { "Cost amount must be positive" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        val costCount = minimalImportDao.getAdditionalCostCount()
        val costNumber = "AC-2026-%04d".format(costCount + 1)
        val costId = "ac-${UUID.randomUUID()}"

        val costEntity = AdditionalProductCost(
            id = costId,
            costNumber = costNumber,
            purchaseOrderId = orderId,
            warehouseId = warehouseId,
            category = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = "BDT",
            exchangeRateToBdt = "1.0000",
            bdtAmountMinor = amountMinor,
            isCapitalizable = isCapitalizable,
            allocationMethod = allocationMethod,
            expenseDate = System.currentTimeMillis(),
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        minimalImportDao.insertAdditionalCost(costEntity)

        // Double-entry accounting for additional cost
        if (isCapitalizable) {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Capitalized Additional Logistics: $description",
                descBn = "ক্যাপিটালাইজড অতিরিক্ত পরিচালন ব্যয়: $description",
                referenceType = "ADDITIONAL_COST",
                referenceId = costNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1210", // Inventory Asset
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = "Capitalize additional operational cost into inventory"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Bank payment for additional logistics: $description"
                    )
                )
            )
        } else {
            // Period Expense (e.g. Warehouse Rent) -> Profit & Loss
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Period Expense: $description",
                descBn = "পিরিয়ড ব্যয়: $description",
                referenceType = "ADDITIONAL_COST",
                referenceId = costNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5110", // Office Rent & Warehouse Utilities
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = "Operating period expense (P&L)"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // Bank
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Bank payment for rent/period expense: $description"
                    )
                )
            )
        }

        return costId
    }

    override fun getAdditionalCostsForOrder(orderId: String): Flow<List<AdditionalProductCost>> =
        minimalImportDao.getAdditionalCostsForOrder(orderId)

    override suspend fun getAdditionalCostsForOrderSync(orderId: String): List<AdditionalProductCost> =
        minimalImportDao.getAdditionalCostsForOrderSync(orderId)

    override suspend fun completePurchase(
        orderId: String,
        disposition: String?,
        reason: String?,
        notes: String?
    ): PurchaseCompletionRecord {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        val totalOrdered = poItems.sumOf { it.quantity }
        val totalReceived = poItems.sumOf { it.receivedQuantity }
        val remaining = (totalOrdered - totalReceived).coerceAtLeast(0)

        val receipts = warehouseReceiptDao.getReceiptsByOrderSync(orderId)
        val allReceiptItems = receipts.flatMap { warehouseReceiptDao.getItemsForReceiptSync(it.id) }
        val goodQty = allReceiptItems.sumOf { (it.receivedQuantity - it.damagedQuantity - it.missingQuantity).coerceAtLeast(0) }
        val damagedQty = allReceiptItems.sumOf { it.damagedQuantity }
        val missingQty = allReceiptItems.sumOf { it.missingQuantity }

        if (remaining > 0) {
            require(!disposition.isNullOrBlank()) {
                "Purchase has $remaining unreceived units. A disposition reason (Shortage, Cancelled Balance, or Supplier Claim) is required!"
            }
        }

        val paymentDue = (po.totalAmountMinor - po.paidAmountMinor).coerceAtLeast(0L)

        val record = PurchaseCompletionRecord(
            id = "comp-${UUID.randomUUID()}",
            purchaseOrderId = orderId,
            completedAt = System.currentTimeMillis(),
            orderedQuantity = totalOrdered,
            receivedQuantity = totalReceived,
            goodQuantity = goodQty,
            damagedQuantity = damagedQty,
            missingQuantity = missingQty,
            remainingQuantity = remaining,
            paymentDueMinor = paymentDue,
            unreceivedDisposition = disposition,
            reason = reason,
            notes = notes
        )

        minimalImportDao.insertCompletionRecord(record)
        purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.COMPLETED.name)

        return record
    }

    override fun getCompletionRecordForOrder(orderId: String): Flow<PurchaseCompletionRecord?> =
        minimalImportDao.getCompletionRecordForOrderFlow(orderId)

    override fun getDamageRecordsForOrder(orderId: String): Flow<List<DamageRecord>> =
        minimalImportDao.getDamageRecordsForOrder(orderId)

    override fun getShortageRecordsForOrder(orderId: String): Flow<List<ShortageRecord>> =
        minimalImportDao.getShortageRecordsForOrder(orderId)

    override suspend fun deleteCarton(cartonId: String) {
        importShipmentDao.deleteCartonById(cartonId)
    }

    override suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String) {
        importShipmentDao.deleteCartonByNumber(orderId, cartonNumber)
    }

    override suspend fun deletePurchaseCostItem(costItemId: String) {
        importCostDao.deleteCostItem(costItemId)
    }

    override suspend fun updatePurchaseOrderPacking(
        orderId: String,
        totalCartons: Int,
        grossWeightKg: String,
        netWeightKg: String,
        cbm: String
    ) {
        purchaseOrderDao.updatePackingSummary(
            orderId = orderId,
            grossWeight = grossWeightKg,
            netWeight = netWeightKg,
            cbm = cbm,
            cartons = totalCartons,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun updatePurchaseOrderItem(
        itemId: String,
        quantity: Int,
        unitPriceMinor: Long,
        cartonQuantity: Int,
        grossWeightKg: String
    ) {
        val item = purchaseOrderDao.getItemById(itemId) ?: return
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return
        val rateBd = BigDecimal(po.exchangeRateToBdt.ifBlank { "17.00" })
        val totalAmountMinor = quantity.toLong() * unitPriceMinor
        val totalBdtAmountMinor = BigDecimal(totalAmountMinor)
            .multiply(rateBd)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val updatedItem = item.copy(
            quantity = quantity,
            unitPriceMinor = unitPriceMinor,
            totalAmountMinor = totalAmountMinor,
            totalBdtAmountMinor = totalBdtAmountMinor,
            cartonQuantity = cartonQuantity,
            grossWeightKg = grossWeightKg
        )
        purchaseOrderDao.updateItem(updatedItem)

        val allItems = purchaseOrderDao.getItemsForOrderSync(po.id)
        val newTotalMinor = allItems.sumOf { it.totalAmountMinor }
        val newTotalBdtMinor = allItems.sumOf { it.totalBdtAmountMinor }
        val newCartons = allItems.sumOf { it.cartonQuantity }
        val newGross = allItems.fold(BigDecimal.ZERO) { acc, itm -> acc.add(BigDecimal(itm.grossWeightKg.ifBlank { "0" })) }.toPlainString()

        purchaseOrderDao.updateOrder(
            po.copy(
                totalAmountMinor = newTotalMinor,
                totalBdtAmountMinor = newTotalBdtMinor,
                totalCartons = newCartons,
                totalGrossWeightKg = newGross,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePurchaseOrderItem(itemId: String) {
        val item = purchaseOrderDao.getItemById(itemId) ?: return
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return
        purchaseOrderDao.deleteItemById(itemId)

        val allItems = purchaseOrderDao.getItemsForOrderSync(po.id)
        val newTotalMinor = allItems.sumOf { it.totalAmountMinor }
        val newTotalBdtMinor = allItems.sumOf { it.totalBdtAmountMinor }
        val newCartons = allItems.sumOf { it.cartonQuantity }
        val newGross = allItems.fold(BigDecimal.ZERO) { acc, itm -> acc.add(BigDecimal(itm.grossWeightKg.ifBlank { "0" })) }.toPlainString()

        purchaseOrderDao.updateOrder(
            po.copy(
                totalAmountMinor = newTotalMinor,
                totalBdtAmountMinor = newTotalBdtMinor,
                totalCartons = newCartons,
                totalGrossWeightKg = newGross,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
