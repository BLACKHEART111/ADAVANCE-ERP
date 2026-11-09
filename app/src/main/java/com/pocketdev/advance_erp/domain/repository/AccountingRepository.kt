package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import kotlinx.coroutines.flow.Flow

data class JournalLineDraft(
    val accountId: String,
    val isDebit: Boolean,
    val amountMinor: Long,
    val description: String? = null
)

interface AccountingRepository {

    fun getAllAccounts(): Flow<List<ChartOfAccount>>
    suspend fun getAccountById(id: String): ChartOfAccount?
    suspend fun createAccount(
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        description: String?
    ): String

    fun getAllJournalEntries(): Flow<List<JournalEntry>>
    fun getLinesForEntry(entryId: String): Flow<List<JournalLine>>
    suspend fun postJournalEntry(
        entryDate: Long,
        descEn: String,
        descBn: String,
        referenceType: String,
        referenceId: String?,
        lines: List<JournalLineDraft>
    ): String

    suspend fun reverseJournalEntry(entryId: String): String
    suspend fun getTrialBalance(): List<TrialBalanceItem>
}
