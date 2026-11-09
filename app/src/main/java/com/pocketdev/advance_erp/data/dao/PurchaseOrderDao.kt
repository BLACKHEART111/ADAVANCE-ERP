package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PurchaseOrder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PurchaseOrderItem>)

    @Update
    suspend fun updateOrder(order: PurchaseOrder)

    @Query("DELETE FROM purchase_orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("DELETE FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    @Update
    suspend fun updateItem(item: PurchaseOrderItem)

    @Query("DELETE FROM purchase_order_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("SELECT * FROM purchase_order_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): PurchaseOrderItem?

    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<PurchaseOrder>>

    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<PurchaseOrder>

    @Query("SELECT * FROM purchase_orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): PurchaseOrder?

    @Query("SELECT * FROM purchase_orders WHERE id = :orderId")
    fun getOrderByIdFlow(orderId: String): Flow<PurchaseOrder?>

    @Query("SELECT * FROM purchase_orders WHERE poNumber = :poNumber")
    suspend fun getOrderByNumber(poNumber: String): PurchaseOrder?

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItem>>

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItem>

    @Query("SELECT * FROM purchase_orders WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getOrdersBySupplier(supplierId: String): Flow<List<PurchaseOrder>>

    @Query("SELECT * FROM purchase_orders WHERE createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    suspend fun getOrdersInRange(fromMillis: Long, toMillis: Long): List<PurchaseOrder>

    @Query("SELECT * FROM purchase_orders WHERE createdAt >= :fromMillis AND createdAt <= :toMillis ORDER BY createdAt DESC")
    fun getOrdersInRangeFlow(fromMillis: Long, toMillis: Long): Flow<List<PurchaseOrder>>

    @Query("SELECT COUNT(*) FROM purchase_orders")
    suspend fun getOrderCount(): Int

    @Transaction
    suspend fun saveOrderWithItems(order: PurchaseOrder, items: List<PurchaseOrderItem>) {
        insertOrder(order)
        deleteItemsForOrder(order.id)
        insertItems(items)
    }

    @Query("UPDATE purchase_orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE purchase_orders SET paidAmountMinor = paidAmountMinor + :amountMinor, paidBdtAmountMinor = paidBdtAmountMinor + :bdtAmountMinor, status = :newStatus, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun recordPayment(orderId: String, amountMinor: Long, bdtAmountMinor: Long, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE purchase_orders SET isActualCostFinalized = 1, actualCostFinalizedAt = :finalizedAt, status = :status, updatedAt = :finalizedAt WHERE id = :orderId")
    suspend fun markActualCostFinalized(orderId: String, status: String, finalizedAt: Long = System.currentTimeMillis())

    @Query("UPDATE purchase_orders SET totalGrossWeightKg = :grossWeight, totalNetWeightKg = :netWeight, totalCbm = :cbm, totalCartons = :cartons, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updatePackingSummary(orderId: String, grossWeight: String, netWeight: String, cbm: String, cartons: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE purchase_order_items SET receivedQuantity = receivedQuantity + :addedQty WHERE id = :itemId")
    suspend fun incrementReceivedQuantity(itemId: String, addedQty: Int)
}
