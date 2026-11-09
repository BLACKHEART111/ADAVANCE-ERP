package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE customerCode = :code LIMIT 1")
    suspend fun getCustomerByCode(code: String): Customer?

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM customers WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR customerCode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR customerCode LIKE '%' || :query || '%') ORDER BY name ASC")
    suspend fun searchCustomersSync(query: String): List<Customer>

    @Query("SELECT COALESCE(SUM(outstandingBalanceMinor), 0) FROM customers WHERE isActive = 1")
    fun getTotalReceivableMinor(): Flow<Long>

    @Query("SELECT COUNT(*) FROM customers WHERE isActive = 1")
    fun getCustomerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET outstandingBalanceMinor = :balanceMinor WHERE id = :customerId")
    suspend fun updateOutstandingBalance(customerId: String, balanceMinor: Long)

    @Query("UPDATE customers SET isActive = 0 WHERE id = :customerId")
    suspend fun archiveCustomer(customerId: String)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun hardDeleteCustomer(customerId: String)
}
