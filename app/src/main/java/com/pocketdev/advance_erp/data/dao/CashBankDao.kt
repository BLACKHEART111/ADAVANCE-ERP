package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CashBankDao {

    // Account Transfers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: AccountTransfer)

    @Query("SELECT * FROM account_transfers ORDER BY transferDate DESC, createdAt DESC")
    fun getAllTransfers(): Flow<List<AccountTransfer>>

    @Query("SELECT * FROM account_transfers ORDER BY transferDate DESC, createdAt DESC")
    suspend fun getAllTransfersSync(): List<AccountTransfer>

    @Query("SELECT COUNT(*) FROM account_transfers")
    suspend fun getTransferCount(): Int

    // Bank Reconciliations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(reconciliation: BankReconciliation)

    @Update
    suspend fun updateReconciliation(reconciliation: BankReconciliation)

    @Query("SELECT * FROM bank_reconciliations ORDER BY statementEndDate DESC, createdAt DESC")
    fun getAllReconciliations(): Flow<List<BankReconciliation>>

    @Query("SELECT * FROM bank_reconciliations WHERE id = :id")
    suspend fun getReconciliationById(id: String): BankReconciliation?

    @Query("SELECT COUNT(*) FROM bank_reconciliations")
    suspend fun getReconciliationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliationItems(items: List<BankReconciliationItem>)

    @Update
    suspend fun updateReconciliationItem(item: BankReconciliationItem)

    @Query("SELECT * FROM bank_reconciliation_items WHERE reconciliationId = :recId ORDER BY transactionDate ASC")
    fun getItemsForReconciliation(recId: String): Flow<List<BankReconciliationItem>>

    @Query("SELECT * FROM bank_reconciliation_items WHERE reconciliationId = :recId ORDER BY transactionDate ASC")
    suspend fun getItemsForReconciliationSync(recId: String): List<BankReconciliationItem>

    @Query("""
        UPDATE bank_reconciliation_items 
        SET isMatched = :isMatched, statementReference = :ref, matchedAt = :matchedAt 
        WHERE id = :itemId
    """)
    suspend fun setItemMatched(itemId: String, isMatched: Boolean, ref: String?, matchedAt: Long?)

    @Transaction
    suspend fun createReconciliationWithItems(
        reconciliation: BankReconciliation,
        items: List<BankReconciliationItem>
    ) {
        insertReconciliation(reconciliation)
        insertReconciliationItems(items)
    }
}
