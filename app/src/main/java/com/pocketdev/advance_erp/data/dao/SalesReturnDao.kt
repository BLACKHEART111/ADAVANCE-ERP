package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesReturnDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(salesReturn: SalesReturn)

    @Query("SELECT * FROM sales_returns WHERE id = :id LIMIT 1")
    suspend fun getReturnById(id: String): SalesReturn?

    @Query("SELECT * FROM sales_returns WHERE returnNumber = :number LIMIT 1")
    suspend fun getReturnByNumber(number: String): SalesReturn?

    @Query("SELECT * FROM sales_returns ORDER BY returnDate DESC")
    fun getAllReturns(): Flow<List<SalesReturn>>

    @Query("SELECT * FROM sales_returns ORDER BY returnDate DESC")
    suspend fun getAllReturnsSync(): List<SalesReturn>

    @Query("SELECT * FROM sales_returns WHERE invoiceId = :invoiceId ORDER BY returnDate DESC")
    fun getReturnsForInvoice(invoiceId: String): Flow<List<SalesReturn>>

    @Query("SELECT * FROM sales_returns WHERE customerId = :customerId ORDER BY returnDate DESC")
    fun getReturnsForCustomer(customerId: String): Flow<List<SalesReturn>>

    @Query("SELECT * FROM sales_returns WHERE customerId = :customerId ORDER BY returnDate DESC")
    suspend fun getReturnsForCustomerSync(customerId: String): List<SalesReturn>

    @Query("SELECT * FROM sales_returns WHERE isPosted = 1 AND returnDate >= :fromMillis AND returnDate <= :toMillis ORDER BY returnDate DESC")
    suspend fun getReturnsInRange(fromMillis: Long, toMillis: Long): List<SalesReturn>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<SalesReturnItem>)

    @Query("SELECT * FROM sales_return_items WHERE returnId = :returnId")
    suspend fun getItemsForReturn(returnId: String): List<SalesReturnItem>

    @Query("SELECT COALESCE(SUM(totalReturnAmountMinor), 0) FROM sales_returns WHERE isPosted = 1")
    fun getTotalSalesReturnMinor(): Flow<Long>

    @Query("SELECT COUNT(*) FROM sales_returns WHERE customerId = :customerId")
    suspend fun getReturnCountForCustomer(customerId: String): Int
}
