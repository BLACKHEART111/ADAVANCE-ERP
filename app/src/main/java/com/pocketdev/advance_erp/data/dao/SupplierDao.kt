package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {

    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY companyName ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: String): Supplier?

    @Query("SELECT COALESCE(SUM(outstandingPayableMinor), 0) FROM suppliers WHERE isActive = 1")
    fun getTotalPayableMinor(): Flow<Long>

    @Query("SELECT COUNT(*) FROM suppliers WHERE isActive = 1")
    fun getSupplierCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET outstandingPayableMinor = :payableMinor WHERE id = :supplierId")
    suspend fun updateOutstandingPayable(supplierId: String, payableMinor: Long)
}
