package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.dao.FifoConsumptionDao
import com.pocketdev.advance_erp.data.dao.FifoConsumptionWithDetails
import com.pocketdev.advance_erp.data.dao.ImportShipmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentWithDetails
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationDao
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationItemWithDetails
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.StockMovementWithDetails
import com.pocketdev.advance_erp.data.dao.StockTransferDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import com.pocketdev.advance_erp.data.entity.InventoryReconciliationItem
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.CostHistoryRepository
import com.pocketdev.advance_erp.domain.repository.InventoryValuationBreakdown
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.ProductCostSummary
import com.pocketdev.advance_erp.domain.repository.WarehouseValuationItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CostHistoryRepositoryImpl @Inject constructor(
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val fifoConsumptionDao: FifoConsumptionDao,
    private val inventoryCostAdjustmentDao: InventoryCostAdjustmentDao,
    private val inventoryReconciliationDao: InventoryReconciliationDao,
    private val stockMovementDao: StockMovementDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockTransferDao: StockTransferDao,
    private val importShipmentDao: ImportShipmentDao,
    private val productDao: ProductDao,
    private val warehouseDao: WarehouseDao,
    private val accountingDao: AccountingDao,
    private val accountingRepository: AccountingRepository
) : CostHistoryRepository {

    override fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>> =
        inventoryCostLayerDao.getCostLayersForProduct(productId)

    override fun getAllCostLayers(): Flow<List<InventoryCostLayer>> =
        inventoryCostLayerDao.getAllCostLayers()

    override suspend fun getCostLayerById(id: String): InventoryCostLayer? =
        inventoryCostLayerDao.getCostLayerById(id)

    override fun getProductCostSummary(productId: String): Flow<ProductCostSummary?> {
        val productFlow: Flow<Product?> = productDao.getProductFlow(productId)
        val layersFlow: Flow<List<InventoryCostLayer>> = inventoryCostLayerDao.getCostLayersForProduct(productId)

        return combine(productFlow, layersFlow) { product: Product?, layers: List<InventoryCostLayer> ->
            if (product == null) return@combine null

            val activeLayers = layers.filter { it.isActive && it.remainingQuantity > 0 }
            val depletedLayers = layers.filter { !it.isActive || it.remainingQuantity <= 0 }

            val totalRemaining = activeLayers.sumOf { it.remainingQuantity }
            val totalValuation = activeLayers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }
            val weightedAvgCost = if (totalRemaining > 0) totalValuation / totalRemaining else product.purchasePriceMinor
            val latestFifoCost = activeLayers.minByOrNull { it.receiptDate }?.unitLandedCostBdtMinor ?: product.purchasePriceMinor

            ProductCostSummary(
                productId = product.id,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                baseSku = product.baseSku,
                totalRemainingQuantity = totalRemaining,
                weightedAverageCostMinor = weightedAvgCost,
                latestFifoCostMinor = latestFifoCost,
                totalValuationMinor = totalValuation,
                activeLayerCount = activeLayers.size,
                depletedLayerCount = depletedLayers.size
            )
        }
    }

    override fun getAllFifoConsumptions(limit: Int): Flow<List<FifoConsumptionWithDetails>> =
        fifoConsumptionDao.getRecentConsumptionsWithDetails(limit)

    override fun getConsumptionsForInvoice(invoiceId: String): Flow<List<FifoConsumption>> =
        fifoConsumptionDao.getConsumptionsForInvoice(invoiceId)

    override fun getConsumptionsForProduct(productId: String): Flow<List<FifoConsumptionWithDetails>> =
        fifoConsumptionDao.getConsumptionsWithDetailsForProduct(productId)

    override fun getConsumptionsForCostLayer(costLayerId: String): Flow<List<FifoConsumption>> =
        fifoConsumptionDao.getConsumptionsForLayer(costLayerId)

    override fun getTotalCogsMinor(): Flow<Long> =
        fifoConsumptionDao.getTotalCogsMinor()

    override fun getFilteredMovementLedger(
        productId: String?,
        warehouseId: String?,
        movementType: String?,
        limit: Int
    ): Flow<List<StockMovementWithDetails>> =
        stockMovementDao.getFilteredMovementLedger(productId, warehouseId, movementType, limit)

    override suspend fun getMovementsForProductSync(productId: String): List<StockMovementWithDetails> =
        stockMovementDao.getMovementsForProductSync(productId)

    override fun getInventoryValuationBreakdown(): Flow<InventoryValuationBreakdown> {
        val totalValuationFlow = inventoryCostLayerDao.getTotalInventoryValuationMinor()
        val totalDamagedQtyFlow = stockMovementDao.getTotalDamagedQuantity()
        val sellableQtyFlow = stockBalanceDao.getTotalSellableQuantity()
        val warehousesFlow = warehouseDao.getAllWarehouses()

        return combine(
            totalValuationFlow,
            totalDamagedQtyFlow,
            sellableQtyFlow,
            warehousesFlow
        ) { sellableValuation, damagedQty, sellableQty, warehouses ->
            val warehouseItems = warehouses.map { w ->
                val physical = stockBalanceDao.getBalance(w.id, "")?.quantity ?: 0
                val sellable = stockBalanceDao.getBalance(w.id, "")?.let { it.quantity - it.reservedQuantity } ?: 0
                WarehouseValuationItem(
                    warehouseId = w.id,
                    warehouseName = w.name,
                    physicalQuantity = physical,
                    sellableQuantity = sellable,
                    damagedQuantity = 0,
                    valuationMinor = 0L
                )
            }

            // In-transit calculation: shipments not received or transfers in-transit
            val inTransitValuation = 0L
            val inTransitQty = 0

            val damagedValuation = damagedQty.toLong() * (if (sellableQty > 0) sellableValuation / sellableQty else 0L)

            InventoryValuationBreakdown(
                sellableValuationMinor = sellableValuation,
                sellableQuantity = sellableQty,
                damagedValuationMinor = damagedValuation,
                damagedQuantity = damagedQty,
                inTransitValuationMinor = inTransitValuation,
                inTransitQuantity = inTransitQty,
                totalValuationMinor = sellableValuation + inTransitValuation,
                warehouseBreakdown = warehouseItems
            )
        }
    }

    override suspend fun performCostAdjustment(
        costLayerId: String,
        newUnitCostBdtMinor: Long,
        reason: String,
        referenceType: String?,
        referenceId: String?
    ): Result<InventoryCostAdjustment> {
        val layer = inventoryCostLayerDao.getCostLayerById(costLayerId)
            ?: return Result.failure(IllegalArgumentException("Inventory cost layer '$costLayerId' not found"))

        val oldUnitCost = layer.unitLandedCostBdtMinor
        val unitVariance = newUnitCostBdtMinor - oldUnitCost
        if (unitVariance == 0L) {
            return Result.failure(IllegalArgumentException("New unit cost is identical to existing landed cost"))
        }

        val remainingQty = layer.remainingQuantity
        val initialQty = layer.initialQuantity
        val soldQty = (initialQty - remainingQty).coerceAtLeast(0)

        val invAdjustmentAmount = unitVariance * remainingQty
        val cogsAdjustmentAmount = unitVariance * soldQty
        val totalAdjustmentAmount = invAdjustmentAmount + cogsAdjustmentAmount

        val adjustmentNumber = MovementNumberGenerator.generate("ICA")
        val adjustmentId = "ica-${UUID.randomUUID()}"

        // Balanced Double-Entry Accounting
        val lines = mutableListOf<JournalLineDraft>()
        if (totalAdjustmentAmount > 0) {
            // Cost increase: Debit Inventory 1210 for remaining stock, Debit COGS 5010 for sold stock, Credit Payable/Bank
            if (invAdjustmentAmount > 0) {
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-1210", // Inventory Asset
                        isDebit = true,
                        amountMinor = invAdjustmentAmount,
                        description = "Landed cost adjustment for remaining stock ($remainingQty units @ +$unitVariance BDT)"
                    )
                )
            }
            if (cogsAdjustmentAmount > 0) {
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-5010", // Cost of Goods Sold
                        isDebit = true,
                        amountMinor = cogsAdjustmentAmount,
                        description = "COGS adjustment for previously sold units ($soldQty units @ +$unitVariance BDT)"
                    )
                )
            }
            lines.add(
                JournalLineDraft(
                    accountId = "coa-2010", // Accounts Payable (China Suppliers)
                    isDebit = false,
                    amountMinor = totalAdjustmentAmount,
                    description = "Additional landed cost liability for adjustment #$adjustmentNumber"
                )
            )
        } else {
            // Cost decrease (rebate/discount): Credit Inventory 1210, Credit COGS 5010, Debit Payable/Bank
            val absTotal = abs(totalAdjustmentAmount)
            val absInv = abs(invAdjustmentAmount)
            val absCogs = abs(cogsAdjustmentAmount)

            lines.add(
                JournalLineDraft(
                    accountId = "coa-2010", // Accounts Payable reduction
                    isDebit = true,
                    amountMinor = absTotal,
                    description = "Cost variance reduction/rebate #$adjustmentNumber"
                )
            )
            if (absInv > 0) {
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-1210", // Inventory Asset reduction
                        isDebit = false,
                        amountMinor = absInv,
                        description = "Inventory valuation reduction for remaining stock ($remainingQty units)"
                    )
                )
            }
            if (absCogs > 0) {
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-5010", // COGS reduction
                        isDebit = false,
                        amountMinor = absCogs,
                        description = "COGS reduction for previously sold units ($soldQty units)"
                    )
                )
            }
        }

        val journalEntryId = if (lines.isNotEmpty()) {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Inventory Cost Adjustment #$adjustmentNumber: $reason",
                descBn = "মজুদ ব্যয় সমন্বয় #$adjustmentNumber: $reason",
                referenceType = "INVENTORY_COST_ADJUSTMENT",
                referenceId = adjustmentNumber,
                lines = lines
            )
        } else null

        // 1. Update the Cost Layer in Database
        val updatedAllocatedExpense = (layer.allocatedExpensePerUnitBdtMinor + unitVariance).coerceAtLeast(0L)
        inventoryCostLayerDao.updateCostLayerUnitCost(
            layerId = layer.id,
            unitCostMinor = newUnitCostBdtMinor,
            allocatedExpenseMinor = updatedAllocatedExpense
        )

        // 2. Record InventoryCostAdjustment
        val adjustmentEntity = InventoryCostAdjustment(
            id = adjustmentId,
            adjustmentNumber = adjustmentNumber,
            costLayerId = layer.id,
            productId = layer.productId,
            variantId = layer.variantId,
            warehouseId = layer.warehouseId,
            purchaseOrderId = layer.purchaseOrderId,
            referenceType = referenceType ?: "MANUAL_ADJUSTMENT",
            referenceId = referenceId,
            reason = reason,
            oldUnitCostBdtMinor = oldUnitCost,
            newUnitCostBdtMinor = newUnitCostBdtMinor,
            unitVarianceBdtMinor = unitVariance,
            totalRemainingQty = remainingQty,
            totalSoldQty = soldQty,
            inventoryAdjustmentAmountMinor = invAdjustmentAmount,
            cogsAdjustmentAmountMinor = cogsAdjustmentAmount,
            totalAdjustmentAmountMinor = totalAdjustmentAmount,
            journalEntryId = journalEntryId,
            createdAt = System.currentTimeMillis()
        )
        inventoryCostAdjustmentDao.insertAdjustment(adjustmentEntity)

        // 3. Record StockMovement Audit for Cost Impact (0 quantity change)
        val currentBalance = stockBalanceDao.getBalance(layer.warehouseId, layer.productId)?.quantity ?: 0
        val costMovement = StockMovement(
            id = MovementNumberGenerator.generateId("mov-cost-adj"),
            movementNumber = MovementNumberGenerator.generate("MOV-CADJ"),
            warehouseId = layer.warehouseId,
            productId = layer.productId,
            variantId = layer.variantId,
            movementType = StockMovementType.INVENTORY_COST_ADJUSTMENT.code,
            quantity = 0,
            unitCostMinor = newUnitCostBdtMinor,
            balanceAfterMovement = currentBalance,
            totalCostImpactMinor = invAdjustmentAmount,
            referenceType = "INVENTORY_COST_ADJUSTMENT",
            referenceId = adjustmentNumber,
            purchaseOrderId = layer.purchaseOrderId,
            purchaseBatchId = layer.purchaseBatchId,
            notes = "Cost adjustment for Batch #${layer.receiptNumber}: $reason. Unit variance: ${unitVariance} BDT. Inventory impact: ${invAdjustmentAmount} BDT, COGS impact: ${cogsAdjustmentAmount} BDT",
            createdAt = System.currentTimeMillis()
        )
        stockMovementDao.insertMovement(costMovement)

        return Result.success(adjustmentEntity)
    }

    override fun getAllCostAdjustments(): Flow<List<InventoryCostAdjustmentWithDetails>> =
        inventoryCostAdjustmentDao.getAllAdjustmentsWithDetails()

    override fun getCostAdjustmentsForProduct(productId: String): Flow<List<InventoryCostAdjustmentWithDetails>> =
        inventoryCostAdjustmentDao.getAdjustmentsWithDetailsForProduct(productId)

    override suspend fun executeReconciliation(
        warehouseId: String?,
        notes: String?,
        reconciledBy: String
    ): Result<InventoryReconciliation> {
        val reconciliationNumber = MovementNumberGenerator.generate("REC")
        val reconciliationId = "rec-${UUID.randomUUID()}"

        val allWarehouses = if (warehouseId != null) {
            val wh = warehouseDao.getWarehouseById(warehouseId)
            if (wh != null) listOf(wh) else warehouseDao.getAllWarehousesSync()
        } else {
            warehouseDao.getAllWarehousesSync()
        }

        val allProducts = productDao.getAllProductsSync()
        val items = mutableListOf<InventoryReconciliationItem>()

        var totalStockBalanceQty = 0
        var totalMovementNetQty = 0
        var totalFifoQty = 0
        var totalFifoValuation = 0L

        for (wh in allWarehouses) {
            for (prod in allProducts) {
                val stockBal = stockBalanceDao.getBalance(wh.id, prod.id)?.quantity ?: 0
                val netMovements = stockMovementDao.getNetMovementQuantityForProduct(prod.id, wh.id)
                val fifoRemaining = inventoryCostLayerDao.getFifoRemainingQtyForProductSync(prod.id, wh.id)

                val layers = inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(prod.id, wh.id)
                val valuation = layers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }

                // Only include if there is any history or stock
                if (stockBal > 0 || netMovements != 0 || fifoRemaining > 0 || valuation > 0L) {
                    val qtyMismatch = (stockBal != netMovements) || (stockBal != fifoRemaining)
                    val details = if (qtyMismatch) {
                        "Discrepancy: StockBal=$stockBal, MovementNet=$netMovements, FifoRemaining=$fifoRemaining"
                    } else null

                    items.add(
                        InventoryReconciliationItem(
                            id = "item-${UUID.randomUUID()}",
                            reconciliationId = reconciliationId,
                            productId = prod.id,
                            variantId = null,
                            warehouseId = wh.id,
                            stockBalanceQty = stockBal,
                            movementNetQty = netMovements,
                            fifoRemainingQty = fifoRemaining,
                            fifoValuationMinor = valuation,
                            qtyMismatch = qtyMismatch,
                            details = details
                        )
                    )

                    totalStockBalanceQty += stockBal
                    totalMovementNetQty += netMovements
                    totalFifoQty += fifoRemaining
                    totalFifoValuation += valuation
                }
            }
        }

        val account1210Balance = accountingDao.getNetDebitBalanceForAccount("1210")
        val qtyDiscrepancy = totalStockBalanceQty - totalFifoQty
        val valuationDiscrepancy = totalFifoValuation - account1210Balance

        val hasDiscrepancy = (qtyDiscrepancy != 0) || (valuationDiscrepancy != 0L) || items.any { it.qtyMismatch }
        val status = if (hasDiscrepancy) "DISCREPANCY_DETECTED" else "MATCHED"

        val reconciliation = InventoryReconciliation(
            id = reconciliationId,
            reconciliationNumber = reconciliationNumber,
            warehouseId = warehouseId,
            status = status,
            totalStockBalanceQty = totalStockBalanceQty,
            totalMovementQty = totalMovementNetQty,
            totalFifoRemainingQty = totalFifoQty,
            totalFifoValuationMinor = totalFifoValuation,
            account1210BalanceMinor = account1210Balance,
            quantityDiscrepancy = qtyDiscrepancy,
            valuationDiscrepancyMinor = valuationDiscrepancy,
            notes = notes ?: "Automated 5-way reconciliation audit run",
            reconciledBy = reconciledBy,
            createdAt = System.currentTimeMillis()
        )

        inventoryReconciliationDao.saveFullReconciliation(reconciliation, items)
        return Result.success(reconciliation)
    }

    override fun getAllReconciliations(): Flow<List<InventoryReconciliation>> =
        inventoryReconciliationDao.getAllReconciliations()

    override fun getReconciliationItems(reconciliationId: String): Flow<List<InventoryReconciliationItemWithDetails>> =
        inventoryReconciliationDao.getItemsForReconciliation(reconciliationId)

    override suspend fun getReconciliationById(id: String): InventoryReconciliation? =
        inventoryReconciliationDao.getReconciliationById(id)
}
