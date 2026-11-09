package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import com.pocketdev.advance_erp.data.entity.InventoryReconciliationItem
import kotlinx.coroutines.flow.Flow

data class InventoryReconciliationItemWithDetails(
    val id: String,
    val reconciliationId: String,
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val baseSku: String,
    val variantId: String?,
    val variantSku: String?,
    val warehouseId: String,
    val warehouseName: String,
    val stockBalanceQty: Int,
    val movementNetQty: Int,
    val fifoRemainingQty: Int,
    val fifoValuationMinor: Long,
    val qtyMismatch: Boolean,
    val details: String?
)

@Dao
interface InventoryReconciliationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReconciliation(reconciliation: InventoryReconciliation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliationItems(items: List<InventoryReconciliationItem>)

    @Transaction
    suspend fun saveFullReconciliation(
        reconciliation: InventoryReconciliation,
        items: List<InventoryReconciliationItem>
    ) {
        insertReconciliation(reconciliation)
        insertReconciliationItems(items)
    }

    @Query("SELECT * FROM inventory_reconciliations ORDER BY createdAt DESC")
    fun getAllReconciliations(): Flow<List<InventoryReconciliation>>

    @Query("SELECT * FROM inventory_reconciliations WHERE id = :id LIMIT 1")
    suspend fun getReconciliationById(id: String): InventoryReconciliation?

    @Query("SELECT * FROM inventory_reconciliations WHERE reconciliationNumber = :number LIMIT 1")
    suspend fun getReconciliationByNumber(number: String): InventoryReconciliation?

    @Query("""
        SELECT iri.id, iri.reconciliationId, iri.productId,
               p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               iri.variantId, pv.sku as variantSku,
               iri.warehouseId, w.name as warehouseName,
               iri.stockBalanceQty, iri.movementNetQty, iri.fifoRemainingQty, iri.fifoValuationMinor,
               iri.qtyMismatch, iri.details
        FROM inventory_reconciliation_items iri
        INNER JOIN products p ON iri.productId = p.id
        LEFT JOIN product_variants pv ON iri.variantId = pv.id
        INNER JOIN warehouses w ON iri.warehouseId = w.id
        WHERE iri.reconciliationId = :reconciliationId
        ORDER BY iri.qtyMismatch DESC, p.nameEn ASC
    """)
    fun getItemsForReconciliation(reconciliationId: String): Flow<List<InventoryReconciliationItemWithDetails>>

    @Query("""
        SELECT iri.id, iri.reconciliationId, iri.productId,
               p.nameEn as productNameEn, p.nameBn as productNameBn, p.baseSku as baseSku,
               iri.variantId, pv.sku as variantSku,
               iri.warehouseId, w.name as warehouseName,
               iri.stockBalanceQty, iri.movementNetQty, iri.fifoRemainingQty, iri.fifoValuationMinor,
               iri.qtyMismatch, iri.details
        FROM inventory_reconciliation_items iri
        INNER JOIN products p ON iri.productId = p.id
        LEFT JOIN product_variants pv ON iri.variantId = pv.id
        INNER JOIN warehouses w ON iri.warehouseId = w.id
        WHERE iri.reconciliationId = :reconciliationId
        ORDER BY iri.qtyMismatch DESC, p.nameEn ASC
    """)
    suspend fun getItemsForReconciliationSync(reconciliationId: String): List<InventoryReconciliationItemWithDetails>

    @Query("SELECT COUNT(*) FROM inventory_reconciliations")
    suspend fun getReconciliationCount(): Int
}
