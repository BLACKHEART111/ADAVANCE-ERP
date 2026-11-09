package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: WarehouseReceipt)

    @Update
    suspend fun updateReceipt(receipt: WarehouseReceipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<WarehouseReceiptItem>)

    @Query("DELETE FROM warehouse_receipts WHERE id = :receiptId")
    suspend fun deleteReceipt(receiptId: String)

    @Query("DELETE FROM warehouse_receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteReceiptItems(receiptId: String)

    @Query("SELECT * FROM warehouse_receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<WarehouseReceipt>>

    @Query("SELECT * FROM warehouse_receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: String): WarehouseReceipt?

    @Query("SELECT * FROM warehouse_receipts WHERE shipmentId = :shipmentId")
    fun getReceiptsByShipment(shipmentId: String): Flow<List<WarehouseReceipt>>

    @Query("SELECT * FROM warehouse_receipts WHERE shipmentId = :shipmentId")
    suspend fun getReceiptsByShipmentSync(shipmentId: String): List<WarehouseReceipt>

    @Query("SELECT * FROM warehouse_receipts WHERE purchaseOrderId = :orderId ORDER BY createdAt DESC")
    fun getReceiptsByOrder(orderId: String): Flow<List<WarehouseReceipt>>

    @Query("SELECT * FROM warehouse_receipts WHERE purchaseOrderId = :orderId ORDER BY createdAt DESC")
    suspend fun getReceiptsByOrderSync(orderId: String): List<WarehouseReceipt>

    @Query("SELECT * FROM warehouse_receipt_items WHERE receiptId = :receiptId")
    fun getItemsForReceipt(receiptId: String): Flow<List<WarehouseReceiptItem>>

    @Query("SELECT * FROM warehouse_receipt_items WHERE receiptId = :receiptId")
    suspend fun getItemsForReceiptSync(receiptId: String): List<WarehouseReceiptItem>

    @Query("SELECT COUNT(*) FROM warehouse_receipts")
    suspend fun getReceiptCount(): Int

    @Transaction
    suspend fun saveReceiptWithItems(receipt: WarehouseReceipt, items: List<WarehouseReceiptItem>) {
        insertReceipt(receipt)
        deleteReceiptItems(receipt.id)
        insertReceiptItems(items)
    }

    @Query("UPDATE warehouse_receipts SET status = :status, postedAt = :postedAt WHERE id = :receiptId")
    suspend fun markReceiptPosted(receiptId: String, status: String, postedAt: Long = System.currentTimeMillis())
}
