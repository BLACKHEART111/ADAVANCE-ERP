package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.dao.FifoConsumptionWithDetails
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentWithDetails
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationItemWithDetails
import com.pocketdev.advance_erp.data.dao.StockMovementWithDetails
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import kotlinx.coroutines.flow.Flow

data class ProductCostSummary(
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val totalRemainingQuantity: Int,
    val weightedAverageCostMinor: Long,
    val latestFifoCostMinor: Long,
    val totalValuationMinor: Long,
    val activeLayerCount: Int,
    val depletedLayerCount: Int
)

data class WarehouseValuationItem(
    val warehouseId: String,
    val warehouseName: String,
    val physicalQuantity: Int,
    val sellableQuantity: Int,
    val damagedQuantity: Int,
    val valuationMinor: Long
)

data class InventoryValuationBreakdown(
    val sellableValuationMinor: Long,
    val sellableQuantity: Int,
    val damagedValuationMinor: Long,
    val damagedQuantity: Int,
    val inTransitValuationMinor: Long,
    val inTransitQuantity: Int,
    val totalValuationMinor: Long,
    val warehouseBreakdown: List<WarehouseValuationItem>
)

interface CostHistoryRepository {

    // Cost Layers & Product Cost History
    fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>>
    fun getAllCostLayers(): Flow<List<InventoryCostLayer>>
    suspend fun getCostLayerById(id: String): InventoryCostLayer?
    fun getProductCostSummary(productId: String): Flow<ProductCostSummary?>

    // FIFO Consumption History & COGS Traceability
    fun getAllFifoConsumptions(limit: Int = 100): Flow<List<FifoConsumptionWithDetails>>
    fun getConsumptionsForInvoice(invoiceId: String): Flow<List<FifoConsumption>>
    fun getConsumptionsForProduct(productId: String): Flow<List<FifoConsumptionWithDetails>>
    fun getConsumptionsForCostLayer(costLayerId: String): Flow<List<FifoConsumption>>
    fun getTotalCogsMinor(): Flow<Long>

    // Stock Movement Permanent Audit Trail
    fun getFilteredMovementLedger(
        productId: String? = null,
        warehouseId: String? = null,
        movementType: String? = null,
        limit: Int = 200
    ): Flow<List<StockMovementWithDetails>>
    suspend fun getMovementsForProductSync(productId: String): List<StockMovementWithDetails>

    // Valuation Breakdown
    fun getInventoryValuationBreakdown(): Flow<InventoryValuationBreakdown>

    // Additional Inventory Cost Adjustment (Split remaining vs sold COGS)
    suspend fun performCostAdjustment(
        costLayerId: String,
        newUnitCostBdtMinor: Long,
        reason: String,
        referenceType: String? = null,
        referenceId: String? = null
    ): Result<InventoryCostAdjustment>
    fun getAllCostAdjustments(): Flow<List<InventoryCostAdjustmentWithDetails>>
    fun getCostAdjustmentsForProduct(productId: String): Flow<List<InventoryCostAdjustmentWithDetails>>

    // Historical Cost Reconciliation (5-way balance audit)
    suspend fun executeReconciliation(
        warehouseId: String? = null,
        notes: String? = null,
        reconciledBy: String = "System Admin"
    ): Result<InventoryReconciliation>
    fun getAllReconciliations(): Flow<List<InventoryReconciliation>>
    fun getReconciliationItems(reconciliationId: String): Flow<List<InventoryReconciliationItemWithDetails>>
    suspend fun getReconciliationById(id: String): InventoryReconciliation?
}
