package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.SalesCostLayerUsage
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import kotlinx.coroutines.flow.Flow

data class SalesInvoiceWithItems(
    val invoice: SalesInvoice,
    val items: List<SalesInvoiceItem>
)

@Dao
interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoice)

    @Update
    suspend fun updateInvoice(invoice: SalesInvoice)

    @Query("SELECT * FROM sales_invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): SalesInvoice?

    @Query("SELECT * FROM sales_invoices WHERE invoiceNumber = :number LIMIT 1")
    suspend fun getInvoiceByNumber(number: String): SalesInvoice?

    @Query("SELECT * FROM sales_invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<SalesInvoice>>

    @Query("SELECT * FROM sales_invoices ORDER BY invoiceDate DESC")
    suspend fun getAllInvoicesSync(): List<SalesInvoice>

    @Query("SELECT * FROM sales_invoices WHERE customerId = :customerId ORDER BY invoiceDate DESC")
    fun getInvoicesForCustomer(customerId: String): Flow<List<SalesInvoice>>

    @Query("SELECT * FROM sales_invoices WHERE customerId = :customerId ORDER BY invoiceDate DESC")
    suspend fun getInvoicesForCustomerSync(customerId: String): List<SalesInvoice>

    @Query("SELECT * FROM sales_invoices WHERE invoiceDate >= :fromMillis AND invoiceDate <= :toMillis ORDER BY invoiceDate DESC")
    suspend fun getInvoicesInRange(fromMillis: Long, toMillis: Long): List<SalesInvoice>

    @Query("SELECT * FROM sales_invoices WHERE customerId = :customerId AND status IN ('POSTED', 'PARTIALLY_PAID') AND dueAmountMinor > 0 ORDER BY invoiceDate ASC")
    suspend fun getUnpaidInvoicesForCustomer(customerId: String): List<SalesInvoice>

    @Query("SELECT * FROM sales_invoices WHERE status = :status ORDER BY invoiceDate DESC")
    fun getInvoicesByStatus(status: String): Flow<List<SalesInvoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<SalesInvoiceItem>)

    @Query("SELECT * FROM sales_invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoiceFlow(invoiceId: String): Flow<List<SalesInvoiceItem>>

    @Query("SELECT * FROM sales_invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoice(invoiceId: String): List<SalesInvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostLayerUsages(usages: List<SalesCostLayerUsage>)

    @Query("SELECT * FROM sales_cost_layer_usages WHERE invoiceId = :invoiceId")
    suspend fun getCostLayerUsagesForInvoice(invoiceId: String): List<SalesCostLayerUsage>

    @Query("UPDATE sales_invoices SET status = :status, paidAmountMinor = :paidMinor, dueAmountMinor = :dueMinor, updatedAt = :now WHERE id = :invoiceId")
    suspend fun updateInvoicePaymentStatus(invoiceId: String, status: String, paidMinor: Long, dueMinor: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE sales_invoices SET status = :status, updatedAt = :now WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE sales_invoice_items SET returnedQuantity = returnedQuantity + :returnedQty WHERE id = :itemId")
    suspend fun recordItemReturn(itemId: String, returnedQty: Int)

    @Query("SELECT COALESCE(SUM(totalAmountMinor), 0) FROM sales_invoices WHERE isPosted = 1")
    fun getTotalSalesRevenueMinor(): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalCogsMinor), 0) FROM sales_invoices WHERE isPosted = 1")
    fun getTotalCogsMinor(): Flow<Long>

    @Query("SELECT COALESCE(SUM(dueAmountMinor), 0) FROM sales_invoices WHERE isPosted = 1")
    fun getTotalReceivableMinor(): Flow<Long>

    @Query("SELECT * FROM sales_invoices WHERE isPosted = 1 AND invoiceDate >= :fromMillis AND invoiceDate <= :toMillis ORDER BY invoiceDate DESC")
    suspend fun getPostedInvoicesInRange(fromMillis: Long, toMillis: Long): List<SalesInvoice>

    @Query("DELETE FROM sales_invoices WHERE id = :invoiceId")
    suspend fun deleteInvoice(invoiceId: String)

    @Query("DELETE FROM sales_invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItems(invoiceId: String)

    @Query("SELECT COUNT(*) FROM sales_invoices WHERE customerId = :customerId")
    suspend fun getInvoiceCountForCustomer(customerId: String): Int
}
