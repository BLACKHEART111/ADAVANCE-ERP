package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.StockMovementWithDetails
import com.pocketdev.advance_erp.data.dao.StockTransferDao
import com.pocketdev.advance_erp.data.dao.StockTransferWithDetails
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import com.pocketdev.advance_erp.data.entity.StockTransferStatus
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.InventoryRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.StockTransferDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val stockTransferDao: StockTransferDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val warehouseDao: WarehouseDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val accountingRepository: AccountingRepository
) : InventoryRepository {

    override fun getStockOverview(): Flow<List<StockItemOverview>> =
        stockBalanceDao.getStockOverview()

    override fun getStockOverviewByWarehouse(warehouseId: String): Flow<List<StockItemOverview>> =
        stockBalanceDao.getStockOverviewByWarehouse(warehouseId)

    override fun getLowStockItems(): Flow<List<StockItemOverview>> =
        stockBalanceDao.getLowStockItems()

    override fun getAllWarehouses(): Flow<List<Warehouse>> =
        warehouseDao.getAllWarehouses()

    override fun getRecentStockMovements(limit: Int): Flow<List<StockMovementWithDetails>> =
        stockMovementDao.getRecentMovementsWithDetails(limit)

    override fun getRecentStockMovementsByWarehouse(warehouseId: String, limit: Int): Flow<List<StockMovementWithDetails>> =
        stockMovementDao.getRecentMovementsWithDetailsByWarehouse(warehouseId, limit)

    override suspend fun adjustStock(
        warehouseId: String,
        productId: String,
        variantId: String?,
        quantityChange: Int,
        isIncrease: Boolean,
        reason: String
    ) {
        require(quantityChange > 0) { "Quantity change must be positive" }
        require(reason.isNotBlank()) { "Adjustment reason is required" }

        db.withTransaction {
            val currentBalance = if (variantId != null) {
                stockBalanceDao.getVariantBalance(warehouseId, productId, variantId)
            } else {
                stockBalanceDao.getBalance(warehouseId, productId)
            }

            val existingQty = currentBalance?.quantity ?: 0
            val newQty = if (isIncrease) {
                existingQty + quantityChange
            } else {
                val rem = existingQty - quantityChange
                check(rem >= 0) { "Insufficient stock to decrease. Current: $existingQty, requested: $quantityChange" }
                rem
            }

            val updatedBalance = currentBalance?.copy(
                quantity = newQty,
                updatedAt = System.currentTimeMillis()
            ) ?: StockBalance(
                id = UUID.randomUUID().toString(),
                warehouseId = warehouseId,
                productId = productId,
                variantId = variantId,
                quantity = newQty,
                reservedQuantity = 0,
                updatedAt = System.currentTimeMillis()
            )
            stockBalanceDao.insertOrUpdate(updatedBalance)

            val product = productDao.getProductById(productId)
            val variant = variantId?.let { productVariantDao.getVariantById(it) }
            val unitCost = if (variant != null && variant.variantCostMinor > 0L) {
                variant.variantCostMinor
            } else {
                product?.purchasePriceMinor ?: 0L
            }

            val movementType = if (isIncrease) {
                StockMovementType.STOCK_ADJUSTMENT_IN.code
            } else {
                StockMovementType.STOCK_ADJUSTMENT_OUT.code
            }

            val movement = StockMovement(
                id = MovementNumberGenerator.generateId("adj"),
                movementNumber = MovementNumberGenerator.generate("ADJ"),
                warehouseId = warehouseId,
                productId = productId,
                variantId = variantId,
                movementType = movementType,
                quantity = quantityChange,
                unitCostMinor = unitCost,
                balanceAfterMovement = newQty,
                totalCostImpactMinor = quantityChange.toLong() * unitCost,
                referenceType = "MANUAL_ADJUSTMENT",
                referenceId = reason,
                notes = reason,
                createdAt = System.currentTimeMillis()
            )
            stockMovementDao.insertMovement(movement)

            // FIFO Cost Layer updates
            if (isIncrease) {
                val layer = InventoryCostLayer(
                    id = "icl-${UUID.randomUUID()}",
                    productId = productId,
                    variantId = variantId,
                    warehouseId = warehouseId,
                    shipmentId = "ADJ",
                    shipmentNumber = "MANUAL-ADJUSTMENT",
                    warehouseReceiptId = "ADJ-${UUID.randomUUID().toString().take(8)}",
                    receiptNumber = "ADJ-${System.currentTimeMillis() % 1000000}",
                    initialQuantity = quantityChange,
                    remainingQuantity = quantityChange,
                    unitLandedCostBdtMinor = unitCost,
                    originalCurrency = "BDT",
                    originalPurchasePriceMinor = unitCost,
                    exchangeRateToBdt = "1.0",
                    allocatedExpensePerUnitBdtMinor = 0L,
                    receiptDate = System.currentTimeMillis(),
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                inventoryCostLayerDao.insertCostLayer(layer)
            } else {
                var needed = quantityChange
                val activeLayers = if (variantId != null) {
                    inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(productId, variantId, warehouseId)
                } else {
                    inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(productId, warehouseId)
                }
                for (layer in activeLayers) {
                    if (needed <= 0) break
                    val take = minOf(needed, layer.remainingQuantity)
                    val newRemaining = layer.remainingQuantity - take
                    inventoryCostLayerDao.updateRemainingQuantity(
                        layerId = layer.id,
                        remainingQuantity = newRemaining,
                        isActive = newRemaining > 0
                    )
                    needed -= take
                }
            }

            // Balanced Double-Entry Accounting
            val totalAdjustmentMinor = quantityChange.toLong() * unitCost
            if (totalAdjustmentMinor > 0) {
                val isDamage = reason.contains("damage", ignoreCase = true) || reason.contains("নষ্ট", ignoreCase = true)
                val lines = if (isIncrease) {
                    listOf(
                        JournalLineDraft(
                            accountId = "coa-1210", // Inventory Asset
                            isDebit = true,
                            amountMinor = totalAdjustmentMinor,
                            description = "Stock Adjustment In (+): $reason"
                        ),
                        JournalLineDraft(
                            accountId = "coa-5010", // COGS adjustment reduction
                            isDebit = false,
                            amountMinor = totalAdjustmentMinor,
                            description = "Stock Adjustment Gain: $reason"
                        )
                    )
                } else {
                    listOf(
                        JournalLineDraft(
                            accountId = if (isDamage) "coa-5050" else "coa-5010", // Damaged Write-Off or COGS
                            isDebit = true,
                            amountMinor = totalAdjustmentMinor,
                            description = "Stock Adjustment Out (-): $reason"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1210", // Inventory Asset deduction
                            isDebit = false,
                            amountMinor = totalAdjustmentMinor,
                            description = "Inventory Reduction: $reason"
                        )
                    )
                }

                accountingRepository.postJournalEntry(
                    entryDate = System.currentTimeMillis(),
                    descEn = "Stock Adjustment: $reason",
                    descBn = "স্টক সমন্বয়: $reason",
                    referenceType = "STOCK_ADJUSTMENT",
                    referenceId = movement.id,
                    lines = lines
                )
            }
        }
    }

    override fun getAllStockTransfers(): Flow<List<StockTransferWithDetails>> =
        stockTransferDao.getAllTransfersWithDetails()

    override fun getStockTransfersByWarehouse(warehouseId: String): Flow<List<StockTransferWithDetails>> =
        stockTransferDao.getTransfersWithDetailsByWarehouse(warehouseId)

    override suspend fun getStockTransferById(id: String): StockTransfer? =
        stockTransferDao.getTransferById(id)

    override suspend fun createStockTransfer(draft: StockTransferDraft): String {
        require(draft.sourceWarehouseId != draft.destinationWarehouseId) {
            "Source and destination warehouses cannot be the same"
        }
        require(draft.quantity > 0) {
            "Transfer quantity must be greater than zero"
        }
        val product = productDao.getProductById(draft.productId)
            ?: throw IllegalArgumentException("Product not found: ${draft.productId}")

        val transferId = UUID.randomUUID().toString()
        val transferNumber = MovementNumberGenerator.generateTransferNumber()

        val transfer = StockTransfer(
            id = transferId,
            transferNumber = transferNumber,
            sourceWarehouseId = draft.sourceWarehouseId,
            destinationWarehouseId = draft.destinationWarehouseId,
            productId = draft.productId,
            variantId = draft.variantId,
            quantity = draft.quantity,
            transferDate = draft.transferDate,
            notes = draft.notes,
            status = StockTransferStatus.DRAFT.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        stockTransferDao.insertTransfer(transfer)
        return transferId
    }

    override suspend fun dispatchStockTransfer(transferId: String) {
        db.withTransaction {
            val transfer = stockTransferDao.getTransferById(transferId)
                ?: throw IllegalArgumentException("Stock transfer not found: $transferId")

            check(transfer.status == StockTransferStatus.DRAFT.name) {
                "Cannot dispatch transfer with status: ${transfer.status}. Only DRAFT transfers can be dispatched."
            }

            // 1. Validate sufficient available stock at source warehouse
            val sourceBalance = if (transfer.variantId != null) {
                stockBalanceDao.getVariantBalance(transfer.sourceWarehouseId, transfer.productId, transfer.variantId)
            } else {
                stockBalanceDao.getBalance(transfer.sourceWarehouseId, transfer.productId)
            }
            val available = (sourceBalance?.quantity ?: 0) - (sourceBalance?.reservedQuantity ?: 0)
            if (available < transfer.quantity) {
                throw IllegalStateException(
                    "Insufficient available stock at source warehouse. Available: $available, Required: ${transfer.quantity}"
                )
            }

            // 2. Deduct sellable stock from source warehouse
            val newSourceQty = sourceBalance!!.quantity - transfer.quantity
            stockBalanceDao.insertOrUpdate(
                sourceBalance.copy(quantity = newSourceQty, updatedAt = System.currentTimeMillis())
            )

            // 3. Deplete FIFO cost layers at source and record transfer cost layers
            var needed = transfer.quantity
            var totalValue = 0L
            val activeLayers = if (transfer.variantId != null) {
                inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(transfer.productId, transfer.variantId, transfer.sourceWarehouseId)
            } else {
                inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(transfer.productId, transfer.sourceWarehouseId)
            }

            val transferCostLayers = mutableListOf<StockTransferCostLayer>()
            for (layer in activeLayers) {
                if (needed <= 0) break
                val take = minOf(needed, layer.remainingQuantity)
                val newRemaining = layer.remainingQuantity - take
                inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, newRemaining > 0)

                transferCostLayers.add(
                    StockTransferCostLayer(
                        id = UUID.randomUUID().toString(),
                        transferId = transfer.id,
                        costLayerId = layer.id,
                        quantity = take,
                        unitCostMinor = layer.unitLandedCostBdtMinor
                    )
                )
                totalValue += take.toLong() * layer.unitLandedCostBdtMinor
                needed -= take
            }
            if (transferCostLayers.isNotEmpty()) {
                stockTransferDao.insertTransferCostLayers(transferCostLayers)
            }

            val avgUnitCost = if (transfer.quantity > 0 && totalValue > 0) {
                totalValue / transfer.quantity
            } else {
                productDao.getProductById(transfer.productId)?.purchasePriceMinor ?: 0L
            }

            // 4. Record TRANSFER_OUT movement audit
            val movement = StockMovement(
                id = MovementNumberGenerator.generateId("mov-tr-out"),
                movementNumber = MovementNumberGenerator.generate("MOV-TR-OUT"),
                warehouseId = transfer.sourceWarehouseId,
                productId = transfer.productId,
                variantId = transfer.variantId,
                movementType = StockMovementType.TRANSFER_OUT.code,
                quantity = transfer.quantity,
                unitCostMinor = avgUnitCost,
                balanceAfterMovement = newSourceQty,
                totalCostImpactMinor = totalValue,
                referenceType = "STOCK_TRANSFER",
                referenceId = transfer.id,
                stockTransferId = transfer.id,
                notes = "Stock transfer dispatched to warehouse #${transfer.destinationWarehouseId} (${transfer.transferNumber})",
                createdAt = System.currentTimeMillis()
            )
            stockMovementDao.insertMovement(movement)

            // 5. Update transfer status to IN_TRANSIT
            stockTransferDao.updateTransfer(
                transfer.copy(
                    status = StockTransferStatus.IN_TRANSIT.name,
                    dispatchedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun receiveStockTransfer(transferId: String) {
        db.withTransaction {
            val transfer = stockTransferDao.getTransferById(transferId)
                ?: throw IllegalArgumentException("Stock transfer not found: $transferId")

            check(transfer.status == StockTransferStatus.IN_TRANSIT.name) {
                "Cannot receive transfer with status: ${transfer.status}. Only IN_TRANSIT transfers can be received."
            }

            // 1. Add sellable stock to destination warehouse
            val destBalance = if (transfer.variantId != null) {
                stockBalanceDao.getVariantBalance(transfer.destinationWarehouseId, transfer.productId, transfer.variantId)
            } else {
                stockBalanceDao.getBalance(transfer.destinationWarehouseId, transfer.productId)
            }

            val existingQty = destBalance?.quantity ?: 0
            val updatedDestBalance = destBalance?.copy(
                quantity = existingQty + transfer.quantity,
                updatedAt = System.currentTimeMillis()
            ) ?: StockBalance(
                id = UUID.randomUUID().toString(),
                warehouseId = transfer.destinationWarehouseId,
                productId = transfer.productId,
                variantId = transfer.variantId,
                quantity = transfer.quantity,
                reservedQuantity = 0,
                updatedAt = System.currentTimeMillis()
            )
            stockBalanceDao.insertOrUpdate(updatedDestBalance)

            // 2. Replicate FIFO cost layers at destination warehouse
            val transferLayers = stockTransferDao.getCostLayersForTransfer(transfer.id)
            var totalValue = 0L
            for (tl in transferLayers) {
                val orig = inventoryCostLayerDao.getCostLayerById(tl.costLayerId)
                val destLayer = InventoryCostLayer(
                    id = "icl-${UUID.randomUUID()}",
                    productId = transfer.productId,
                    variantId = transfer.variantId,
                    warehouseId = transfer.destinationWarehouseId,
                    shipmentId = orig?.shipmentId ?: "TR-${transfer.transferNumber}",
                    shipmentNumber = orig?.shipmentNumber ?: transfer.transferNumber,
                    warehouseReceiptId = orig?.warehouseReceiptId ?: transfer.id,
                    receiptNumber = orig?.receiptNumber ?: transfer.transferNumber,
                    initialQuantity = tl.quantity,
                    remainingQuantity = tl.quantity,
                    unitLandedCostBdtMinor = tl.unitCostMinor,
                    originalCurrency = orig?.originalCurrency ?: "BDT",
                    originalPurchasePriceMinor = orig?.originalPurchasePriceMinor ?: tl.unitCostMinor,
                    exchangeRateToBdt = orig?.exchangeRateToBdt ?: "1.0",
                    allocatedExpensePerUnitBdtMinor = orig?.allocatedExpensePerUnitBdtMinor ?: 0L,
                    receiptDate = System.currentTimeMillis(),
                    isActive = true,
                    purchaseOrderId = orig?.purchaseOrderId,
                    purchaseBatchId = orig?.purchaseBatchId,
                    createdAt = System.currentTimeMillis()
                )
                inventoryCostLayerDao.insertCostLayer(destLayer)
                totalValue += tl.quantity.toLong() * tl.unitCostMinor
            }

            val avgUnitCost = if (transfer.quantity > 0 && totalValue > 0) {
                totalValue / transfer.quantity
            } else {
                productDao.getProductById(transfer.productId)?.purchasePriceMinor ?: 0L
            }

            // 3. Record TRANSFER_IN movement audit
            val movement = StockMovement(
                id = MovementNumberGenerator.generateId("mov-tr-in"),
                movementNumber = MovementNumberGenerator.generate("MOV-TR-IN"),
                warehouseId = transfer.destinationWarehouseId,
                productId = transfer.productId,
                variantId = transfer.variantId,
                movementType = StockMovementType.TRANSFER_IN.code,
                quantity = transfer.quantity,
                unitCostMinor = avgUnitCost,
                balanceAfterMovement = updatedDestBalance.quantity,
                totalCostImpactMinor = totalValue,
                referenceType = "STOCK_TRANSFER",
                referenceId = transfer.id,
                stockTransferId = transfer.id,
                notes = "Stock transfer received from warehouse #${transfer.sourceWarehouseId} (${transfer.transferNumber})",
                createdAt = System.currentTimeMillis()
            )
            stockMovementDao.insertMovement(movement)

            // 4. Update transfer status to RECEIVED
            stockTransferDao.updateTransfer(
                transfer.copy(
                    status = StockTransferStatus.RECEIVED.name,
                    receivedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun cancelStockTransfer(transferId: String, reason: String?) {
        db.withTransaction {
            val transfer = stockTransferDao.getTransferById(transferId)
                ?: throw IllegalArgumentException("Stock transfer not found: $transferId")

            check(transfer.status != StockTransferStatus.CANCELLED.name) {
                "Transfer is already cancelled"
            }
            check(transfer.status != StockTransferStatus.RECEIVED.name) {
                "Cannot cancel an already received transfer"
            }

            val updatedNotes = listOfNotNull(transfer.notes, reason?.let { "Cancel Reason: $it" })
                .joinToString(" | ")

            if (transfer.status == StockTransferStatus.DRAFT.name) {
                stockTransferDao.updateTransfer(
                    transfer.copy(
                        status = StockTransferStatus.CANCELLED.name,
                        cancelledAt = System.currentTimeMillis(),
                        notes = updatedNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@withTransaction
            }

            // If cancelling an IN_TRANSIT transfer: reverse source stock deduction and cost layer depletion
            if (transfer.status == StockTransferStatus.IN_TRANSIT.name) {
                // Restore source stock balance
                val sourceBalance = if (transfer.variantId != null) {
                    stockBalanceDao.getVariantBalance(transfer.sourceWarehouseId, transfer.productId, transfer.variantId)
                } else {
                    stockBalanceDao.getBalance(transfer.sourceWarehouseId, transfer.productId)
                }

                val restoredQty = (sourceBalance?.quantity ?: 0) + transfer.quantity
                stockBalanceDao.insertOrUpdate(
                    sourceBalance!!.copy(quantity = restoredQty, updatedAt = System.currentTimeMillis())
                )

                // Restore depleted FIFO cost layers
                val transferLayers = stockTransferDao.getCostLayersForTransfer(transfer.id)
                for (tl in transferLayers) {
                    val layer = inventoryCostLayerDao.getCostLayerById(tl.costLayerId)
                    if (layer != null) {
                        val newRemaining = layer.remainingQuantity + tl.quantity
                        inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, true)
                    }
                }

                // Record reversal audit movement
                val movement = StockMovement(
                    id = MovementNumberGenerator.generateId("mov-tr-cancel"),
                    movementNumber = MovementNumberGenerator.generate("MOV-TR-REV"),
                    warehouseId = transfer.sourceWarehouseId,
                    productId = transfer.productId,
                    variantId = transfer.variantId,
                    movementType = StockMovementType.TRANSFER_IN.code,
                    quantity = transfer.quantity,
                    unitCostMinor = 0L,
                    referenceType = "TRANSFER_CANCELLATION",
                    referenceId = transfer.id,
                    notes = "Transfer #${transfer.transferNumber} cancelled. Restored to source warehouse. ${reason ?: ""}",
                    createdAt = System.currentTimeMillis()
                )
                stockMovementDao.insertMovement(movement)

                stockTransferDao.updateTransfer(
                    transfer.copy(
                        status = StockTransferStatus.CANCELLED.name,
                        cancelledAt = System.currentTimeMillis(),
                        notes = updatedNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun getTotalPhysicalQuantity(): Flow<Int> =
        stockBalanceDao.getTotalPhysicalQuantity()

    override fun getPhysicalQuantityByWarehouse(warehouseId: String): Flow<Int> =
        stockBalanceDao.getPhysicalQuantityByWarehouse(warehouseId)

    override fun getTotalSellableQuantity(): Flow<Int> =
        stockBalanceDao.getTotalSellableQuantity()

    override fun getSellableQuantityByWarehouse(warehouseId: String): Flow<Int> =
        stockBalanceDao.getSellableQuantityByWarehouse(warehouseId)

    override fun getTotalDamagedQuantity(): Flow<Int> =
        stockMovementDao.getTotalDamagedQuantity()

    override fun getDamagedQuantityByWarehouse(warehouseId: String): Flow<Int> =
        stockMovementDao.getDamagedQuantityByWarehouse(warehouseId)

    override fun getTotalInventoryValuationMinor(): Flow<Long> =
        inventoryCostLayerDao.getTotalInventoryValuationMinor()

    override fun getInventoryValuationByWarehouseMinor(warehouseId: String): Flow<Long> =
        inventoryCostLayerDao.getInventoryValuationByWarehouseMinor(warehouseId)
}
