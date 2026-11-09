package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CustomerPayment)

    @Update
    suspend fun updatePayment(payment: CustomerPayment)

    @Query("SELECT * FROM customer_payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: String): CustomerPayment?

    @Query("SELECT * FROM customer_payments WHERE paymentNumber = :number LIMIT 1")
    suspend fun getPaymentByNumber(number: String): CustomerPayment?

    @Query("SELECT * FROM customer_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<CustomerPayment>>

    @Query("SELECT * FROM customer_payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsSync(): List<CustomerPayment>

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>>

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForCustomerSync(customerId: String): List<CustomerPayment>

    @Query("SELECT * FROM customer_payments WHERE isPosted = 1 AND paymentDate >= :fromMillis AND paymentDate <= :toMillis ORDER BY paymentDate DESC")
    suspend fun getPaymentsInRange(fromMillis: Long, toMillis: Long): List<CustomerPayment>

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId AND unallocatedCreditMinor > 0 ORDER BY paymentDate ASC")
    suspend fun getUnallocatedPaymentsForCustomer(customerId: String): List<CustomerPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<PaymentAllocation>)

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun getAllocationsForPayment(paymentId: String): List<PaymentAllocation>

    @Query("SELECT * FROM payment_allocations WHERE invoiceId = :invoiceId")
    fun getAllocationsForInvoice(invoiceId: String): Flow<List<PaymentAllocation>>

    @Query("SELECT * FROM payment_allocations WHERE invoiceId = :invoiceId")
    suspend fun getAllocationsForInvoiceSync(invoiceId: String): List<PaymentAllocation>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM customer_payments WHERE isPosted = 1 AND isVoided = 0")
    fun getTotalCollectedMinor(): Flow<Long>

    @Query("SELECT COALESCE(SUM(unallocatedCreditMinor), 0) FROM customer_payments WHERE isPosted = 1 AND isVoided = 0 AND customerId = :customerId")
    fun getCustomerAdvanceCreditMinor(customerId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(unallocatedCreditMinor), 0) FROM customer_payments WHERE isPosted = 1 AND isVoided = 0 AND customerId = :customerId")
    suspend fun getCustomerAdvanceCreditMinorSync(customerId: String): Long

    @Query("DELETE FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun deleteAllocationsForPayment(paymentId: String)

    @Query("DELETE FROM payment_allocations WHERE invoiceId = :invoiceId")
    suspend fun deleteAllocationsForInvoice(invoiceId: String)

    @Query("SELECT COUNT(*) FROM customer_payments WHERE customerId = :customerId")
    suspend fun getPaymentCountForCustomer(customerId: String): Int
}
