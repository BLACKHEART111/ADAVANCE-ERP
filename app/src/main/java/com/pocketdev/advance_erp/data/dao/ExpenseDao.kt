package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseVoucher)

    @Update
    suspend fun updateExpense(expense: ExpenseVoucher)

    @Query("SELECT * FROM expense_vouchers ORDER BY voucherDate DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseVoucher>>

    @Query("SELECT * FROM expense_vouchers ORDER BY voucherDate DESC, createdAt DESC")
    suspend fun getAllExpensesSync(): List<ExpenseVoucher>

    @Query("SELECT * FROM expense_vouchers WHERE id = :id")
    suspend fun getExpenseById(id: String): ExpenseVoucher?

    @Query("SELECT COUNT(*) FROM expense_vouchers")
    suspend fun getExpenseCount(): Int

    @Query("SELECT * FROM expense_vouchers WHERE voucherDate BETWEEN :startDate AND :endDate ORDER BY voucherDate DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseVoucher>>

    @Query("SELECT * FROM expense_vouchers WHERE voucherDate BETWEEN :startDate AND :endDate ORDER BY voucherDate DESC")
    suspend fun getExpensesByDateRangeSync(startDate: Long, endDate: Long): List<ExpenseVoucher>

    @Query("SELECT COALESCE(SUM(bdtAmountMinor), 0) FROM expense_vouchers WHERE isPosted = 1 AND isReversed = 0")
    fun getTotalActiveExpenseBdtMinor(): Flow<Long>

    @Query("UPDATE expense_vouchers SET isReversed = 1, reversalJournalEntryId = :reversalEntryId WHERE id = :id")
    suspend fun markExpenseReversed(id: String, reversalEntryId: String)
}
