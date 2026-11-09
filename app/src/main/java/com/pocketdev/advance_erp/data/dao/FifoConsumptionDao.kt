package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import kotlinx.coroutines.flow.Flow

data class FifoConsumptionWithDetails(
    val id: String,
    val invoiceId: String?,
    val invoiceItemId: String?,
    val costLayerId: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val variantId: String?,
    val variantSku: String?,
    val warehouseId: String,
    val warehouseName: String,
    val quantity: Int,
    val unitCostBdtMinor: Long,
    val totalCogsBdtMinor: Long,
    val referenceType: String,
    val referenceId: String?,
    val createdAt: Long,
    val receiptNumber: String?,
    val shipmentNumber: String?
)

@Dao
interface FifoConsumptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumption(consumption: FifoConsumption)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumptions(consumptions: List<FifoConsumption>)

    @Query("SELECT * FROM fifo_consumptions ORDER BY createdAt DESC")
    fun getAllConsumptions(): Flow<List<FifoConsumption>>

    @Query("SELECT * FROM fifo_consumptions WHERE invoiceId = :invoiceId ORDER BY createdAt ASC")
    fun getConsumptionsForInvoice(invoiceId: String): Flow<List<FifoConsumption>>

    @Query("SELECT * FROM fifo_consumptions WHERE invoiceId = :invoiceId ORDER BY createdAt ASC")
    suspend fun getConsumptionsForInvoiceSync(invoiceId: String): List<FifoConsumption>

    @Query("SELECT * FROM fifo_consumptions WHERE productId = :productId ORDER BY createdAt DESC")
    fun getConsumptionsForProduct(productId: String): Flow<List<FifoConsumption>>

    @Query("SELECT * FROM fifo_consumptions WHERE costLayerId = :costLayerId ORDER BY createdAt DESC")
    fun getConsumptionsForLayer(costLayerId: String): Flow<List<FifoConsumption>>

    @Query("SELECT * FROM fifo_consumptions WHERE costLayerId = :costLayerId ORDER BY createdAt DESC")
    suspend fun getConsumptionsForLayerSync(costLayerId: String): List<FifoConsumption>

    @Query("""
        SELECT fc.id, fc.invoiceId, fc.invoiceItemId, fc.costLayerId,
               fc.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               fc.variantId, pv.sku as variantSku,
               fc.warehouseId, w.name as warehouseName,
               fc.quantity, fc.unitCostBdtMinor, fc.totalCogsBdtMinor,
               fc.referenceType, fc.referenceId, fc.createdAt,
               icl.receiptNumber as receiptNumber, icl.shipmentNumber as shipmentNumber
        FROM fifo_consumptions fc
        INNER JOIN products p ON fc.productId = p.id
        LEFT JOIN product_variants pv ON fc.variantId = pv.id
        INNER JOIN warehouses w ON fc.warehouseId = w.id
        LEFT JOIN inventory_cost_layers icl ON fc.costLayerId = icl.id
        ORDER BY fc.createdAt DESC
        LIMIT :limit
    """)
    fun getRecentConsumptionsWithDetails(limit: Int = 100): Flow<List<FifoConsumptionWithDetails>>

    @Query("""
        SELECT fc.id, fc.invoiceId, fc.invoiceItemId, fc.costLayerId,
               fc.productId, p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               fc.variantId, pv.sku as variantSku,
               fc.warehouseId, w.name as warehouseName,
               fc.quantity, fc.unitCostBdtMinor, fc.totalCogsBdtMinor,
               fc.referenceType, fc.referenceId, fc.createdAt,
               icl.receiptNumber as receiptNumber, icl.shipmentNumber as shipmentNumber
        FROM fifo_consumptions fc
        INNER JOIN products p ON fc.productId = p.id
        LEFT JOIN product_variants pv ON fc.variantId = pv.id
        INNER JOIN warehouses w ON fc.warehouseId = w.id
        LEFT JOIN inventory_cost_layers icl ON fc.costLayerId = icl.id
        WHERE fc.productId = :productId
        ORDER BY fc.createdAt DESC
    """)
    fun getConsumptionsWithDetailsForProduct(productId: String): Flow<List<FifoConsumptionWithDetails>>

    @Query("""
        SELECT COALESCE(SUM(totalCogsBdtMinor), 0)
        FROM fifo_consumptions
    """)
    fun getTotalCogsMinor(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM fifo_consumptions
        WHERE costLayerId = :costLayerId
    """)
    suspend fun getTotalConsumedQuantityForLayer(costLayerId: String): Int
}
