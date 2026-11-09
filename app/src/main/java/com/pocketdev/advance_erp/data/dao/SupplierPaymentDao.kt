package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SupplierPayment)

    @Query("SELECT * FROM supplier_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsSync(): List<SupplierPayment>

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY paymentDate DESC")
    fun getPaymentsBySupplier(supplierId: String): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments WHERE purchaseOrderId = :orderId ORDER BY paymentDate DESC")
    fun getPaymentsByOrder(orderId: String): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments WHERE purchaseOrderId = :orderId ORDER BY paymentDate DESC")
    suspend fun getPaymentsByOrderSync(orderId: String): List<SupplierPayment>

    @Query("SELECT COUNT(*) FROM supplier_payments")
    suspend fun getPaymentCount(): Int

    @Query("SELECT * FROM supplier_payments WHERE paymentDate >= :fromMillis AND paymentDate <= :toMillis ORDER BY paymentDate DESC")
    suspend fun getPaymentsInRange(fromMillis: Long, toMillis: Long): List<SupplierPayment>
}
