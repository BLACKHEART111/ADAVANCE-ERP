package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountingRepositoryImpl @Inject constructor(
    private val database: AdvanceErpDatabase,
    private val accountingDao: AccountingDao
) : AccountingRepository {

    override fun getAllAccounts(): Flow<List<ChartOfAccount>> = accountingDao.getAllAccounts()

    override suspend fun getAccountById(id: String): ChartOfAccount? = accountingDao.getAccountById(id)

    override suspend fun createAccount(
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        description: String?
    ): String {
        val existing = accountingDao.getAccountByCode(code)
        require(existing == null) { "Account code already exists: $code" }

        val id = "coa-$code"
        val account = ChartOfAccount(
            id = id,
            accountCode = code,
            nameEn = nameEn,
            nameBn = nameBn,
            accountType = type.name,
            normalBalance = normalBalance.name,
            description = description,
            isActive = true
        )
        accountingDao.insertAccount(account)
        return id
    }

    override fun getAllJournalEntries(): Flow<List<JournalEntry>> = accountingDao.getAllJournalEntries()

    override fun getLinesForEntry(entryId: String): Flow<List<JournalLine>> = accountingDao.getLinesForEntry(entryId)

    override suspend fun postJournalEntry(
        entryDate: Long,
        descEn: String,
        descBn: String,
        referenceType: String,
        referenceId: String?,
        lines: List<JournalLineDraft>
    ): String {
        require(lines.size >= 2) { "Journal entry must contain at least 2 lines" }
        require(lines.all { it.amountMinor > 0L }) { "All journal line amounts must be strictly positive (> 0)" }
        require(lines.none { it.accountId.isBlank() }) { "Journal lines must specify valid account IDs" }

        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        require(totalDebit > 0L) { "Journal debit total must be greater than zero: $totalDebit" }
        require(totalDebit == totalCredit) {
            "Debit ($totalDebit) does not balance with Credit ($totalCredit) in minor units!"
        }

        val entryCount = accountingDao.getEntryCount()
        val entryNumber = "JE-2026-%04d".format(entryCount + 1)
        val entryId = "je-${UUID.randomUUID()}"

        val accountsMap = accountingDao.getAllAccountsSync().associateBy { it.id }

        val journalLines = lines.map { draft ->
            val account = accountsMap[draft.accountId]
                ?: throw IllegalArgumentException("Account not found: ${draft.accountId}")
            JournalLine(
                id = "jl-${UUID.randomUUID()}",
                journalEntryId = entryId,
                accountId = draft.accountId,
                accountCode = account.accountCode,
                accountName = account.nameEn,
                isDebit = draft.isDebit,
                amountMinor = draft.amountMinor,
                description = draft.description
            )
        }

        val entry = JournalEntry(
            id = entryId,
            entryNumber = entryNumber,
            entryDate = entryDate,
            descriptionEn = descEn,
            descriptionBn = descBn,
            referenceType = referenceType,
            referenceId = referenceId,
            totalDebitMinor = totalDebit,
            totalCreditMinor = totalCredit,
            isPosted = true,
            isReversed = false,
            createdAt = System.currentTimeMillis()
        )

        accountingDao.postJournalEntryWithLines(entry, journalLines)
        return entryId
    }

    override suspend fun reverseJournalEntry(entryId: String): String {
        val original = accountingDao.getJournalEntryById(entryId)
            ?: throw IllegalArgumentException("Journal entry not found: $entryId")

        require(!original.isReversed) { "Entry is already reversed: ${original.entryNumber}" }

        val originalLines = accountingDao.getLinesForEntrySync(entryId)
        require(originalLines.isNotEmpty()) { "No journal lines found for entry: $entryId" }

        val entryCount = accountingDao.getEntryCount()
        val reversalNumber = "JE-2026-%04d".format(entryCount + 1)
        val reversalId = "je-${UUID.randomUUID()}"

        // Swap debits and credits
        val reversalLines = originalLines.map { line ->
            JournalLine(
                id = "jl-${UUID.randomUUID()}",
                journalEntryId = reversalId,
                accountId = line.accountId,
                accountCode = line.accountCode,
                accountName = line.accountName,
                isDebit = !line.isDebit, // Swapped!
                amountMinor = line.amountMinor,
                description = "Reversal of ${original.entryNumber}: ${line.description ?: ""}"
            )
        }

        val reversalEntry = JournalEntry(
            id = reversalId,
            entryNumber = reversalNumber,
            entryDate = System.currentTimeMillis(),
            descriptionEn = "Reversal of ${original.entryNumber} - ${original.descriptionEn}",
            descriptionBn = "${original.entryNumber} এর বিপরীত দাখিলা - ${original.descriptionBn}",
            referenceType = "REVERSAL",
            referenceId = original.id,
            totalDebitMinor = original.totalCreditMinor,
            totalCreditMinor = original.totalDebitMinor,
            isPosted = true,
            isReversed = false,
            reversalOfEntryId = original.id,
            createdAt = System.currentTimeMillis()
        )

        accountingDao.postJournalEntryWithLines(reversalEntry, reversalLines)
        accountingDao.markEntryReversed(original.id, reversalId)

        return reversalId
    }

    override suspend fun getTrialBalance(): List<TrialBalanceItem> {
        val accounts = accountingDao.getAllAccountsSync()
        val activeLines = accountingDao.getActivePostedLines()

        val linesByAccount = activeLines.groupBy { it.accountId }

        return accounts.map { account ->
            val accountLines = linesByAccount[account.id] ?: emptyList()
            val totalDebit = accountLines.filter { it.isDebit }.sumOf { it.amountMinor }
            val totalCredit = accountLines.filter { !it.isDebit }.sumOf { it.amountMinor }

            val isDebitNormal = account.normalBalance == NormalBalance.DEBIT.name
            val netDebit: Long
            val netCredit: Long

            if (isDebitNormal) {
                val balance = totalDebit - totalCredit
                if (balance >= 0) {
                    netDebit = balance
                    netCredit = 0L
                } else {
                    netDebit = 0L
                    netCredit = -balance
                }
            } else {
                val balance = totalCredit - totalDebit
                if (balance >= 0) {
                    netCredit = balance
                    netDebit = 0L
                } else {
                    netCredit = 0L
                    netDebit = -balance
                }
            }

            TrialBalanceItem(
                accountId = account.id,
                accountCode = account.accountCode,
                accountNameEn = account.nameEn,
                accountNameBn = account.nameBn,
                accountType = account.accountType,
                normalBalance = account.normalBalance,
                totalDebitMinor = totalDebit,
                totalCreditMinor = totalCredit,
                netDebitMinor = netDebit,
                netCreditMinor = netCredit
            )
        }
    }
}
