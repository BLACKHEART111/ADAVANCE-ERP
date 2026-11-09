package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    // Chart of Accounts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: ChartOfAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<ChartOfAccount>)

    @Query("SELECT * FROM chart_of_accounts ORDER BY accountCode ASC")
    fun getAllAccounts(): Flow<List<ChartOfAccount>>

    @Query("SELECT * FROM chart_of_accounts ORDER BY accountCode ASC")
    suspend fun getAllAccountsSync(): List<ChartOfAccount>

    @Query("SELECT * FROM chart_of_accounts WHERE id = :id")
    suspend fun getAccountById(id: String): ChartOfAccount?

    @Query("SELECT * FROM chart_of_accounts WHERE accountCode = :code")
    suspend fun getAccountByCode(code: String): ChartOfAccount?

    // Journal Entries & Lines
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLine>)

    @Query("SELECT * FROM journal_entries ORDER BY entryDate DESC, createdAt DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId")
    suspend fun getJournalEntryById(entryId: String): JournalEntry?

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId ORDER BY isDebit DESC, accountCode ASC")
    fun getLinesForEntry(entryId: String): Flow<List<JournalLine>>

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId ORDER BY isDebit DESC, accountCode ASC")
    suspend fun getLinesForEntrySync(entryId: String): List<JournalLine>

    @Query("SELECT * FROM journal_lines WHERE accountId = :accountId ORDER BY id ASC")
    fun getLinesForAccount(accountId: String): Flow<List<JournalLine>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getEntryCount(): Int

    @Query("UPDATE journal_entries SET isReversed = 1, reversalEntryId = :reversalEntryId WHERE id = :originalEntryId")
    suspend fun markEntryReversed(originalEntryId: String, reversalEntryId: String)

    @Transaction
    suspend fun postJournalEntryWithLines(entry: JournalEntry, lines: List<JournalLine>) {
        insertJournalEntry(entry)
        insertJournalLines(lines)
    }

    @Query("""
        SELECT jl.* FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.isPosted = 1 AND je.isReversed = 0
    """)
    suspend fun getActivePostedLines(): List<JournalLine>

    @Query("""
        SELECT jl.* FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.isPosted = 1 AND je.isReversed = 0 AND je.entryDate <= :upToMillis
    """)
    suspend fun getActivePostedLinesUpTo(upToMillis: Long): List<JournalLine>

    @Query("""
        SELECT jl.* FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.isPosted = 1 AND je.isReversed = 0 AND je.entryDate >= :fromMillis AND je.entryDate <= :toMillis
    """)
    suspend fun getActivePostedLinesInRange(fromMillis: Long, toMillis: Long): List<JournalLine>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN jl.isDebit = 1 THEN jl.amountMinor ELSE -jl.amountMinor END), 0)
        FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.isPosted = 1 AND je.isReversed = 0 AND jl.accountCode = :accountCode
    """)
    suspend fun getNetDebitBalanceForAccount(accountCode: String): Long

    @Query("SELECT * FROM journal_entries WHERE referenceType = :refType AND referenceId = :refId AND isReversed = 0")
    suspend fun getActiveEntriesForReference(refType: String, refId: String): List<JournalEntry>
}
