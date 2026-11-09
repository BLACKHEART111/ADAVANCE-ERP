package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MinimalImportDao {

    // Damage Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamageRecord(record: DamageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamageRecords(records: List<DamageRecord>)

    @Query("SELECT * FROM damage_records WHERE purchaseOrderId = :orderId ORDER BY createdAt DESC")
    fun getDamageRecordsForOrder(orderId: String): Flow<List<DamageRecord>>

    @Query("SELECT * FROM damage_records WHERE purchaseOrderId = :orderId")
    suspend fun getDamageRecordsForOrderSync(orderId: String): List<DamageRecord>

    @Query("SELECT * FROM damage_records WHERE warehouseReceiptId = :receiptId")
    suspend fun getDamageRecordsForReceiptSync(receiptId: String): List<DamageRecord>

    @Query("SELECT COUNT(*) FROM damage_records")
    suspend fun getDamageRecordCount(): Int

    // Shortage Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortageRecord(record: ShortageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortageRecords(records: List<ShortageRecord>)

    @Query("SELECT * FROM shortage_records WHERE purchaseOrderId = :orderId ORDER BY createdAt DESC")
    fun getShortageRecordsForOrder(orderId: String): Flow<List<ShortageRecord>>

    @Query("SELECT * FROM shortage_records WHERE purchaseOrderId = :orderId")
    suspend fun getShortageRecordsForOrderSync(orderId: String): List<ShortageRecord>

    @Query("SELECT * FROM shortage_records WHERE warehouseReceiptId = :receiptId")
    suspend fun getShortageRecordsForReceiptSync(receiptId: String): List<ShortageRecord>

    @Query("SELECT COUNT(*) FROM shortage_records")
    suspend fun getShortageRecordCount(): Int

    // Additional Product Costs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdditionalCost(cost: AdditionalProductCost)

    @Query("SELECT * FROM additional_product_costs WHERE purchaseOrderId = :orderId ORDER BY expenseDate DESC")
    fun getAdditionalCostsForOrder(orderId: String): Flow<List<AdditionalProductCost>>

    @Query("SELECT * FROM additional_product_costs WHERE purchaseOrderId = :orderId")
    suspend fun getAdditionalCostsForOrderSync(orderId: String): List<AdditionalProductCost>

    @Query("SELECT COUNT(*) FROM additional_product_costs")
    suspend fun getAdditionalCostCount(): Int

    // Purchase Completion Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionRecord(record: PurchaseCompletionRecord)

    @Query("SELECT * FROM purchase_completion_records WHERE purchaseOrderId = :orderId")
    suspend fun getCompletionRecordForOrder(orderId: String): PurchaseCompletionRecord?

    @Query("SELECT * FROM purchase_completion_records WHERE purchaseOrderId = :orderId")
    fun getCompletionRecordForOrderFlow(orderId: String): Flow<PurchaseCompletionRecord?>
}
