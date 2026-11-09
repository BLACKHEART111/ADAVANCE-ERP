package com.pocketdev.advance_erp.feature.accounts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.FinancialCsvExporter
import com.pocketdev.advance_erp.core.util.FinancialPdfGenerator
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.domain.model.AgingReport
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.model.CashFlowStatement
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.FinancialRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<ChartOfAccount> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val trialBalance: List<TrialBalanceItem> = emptyList(),
    val totalTrialDebit: Long = 0L,
    val totalTrialCredit: Long = 0L,
    val isBalanced: Boolean = true,
    // Phase 04 Financial Data:
    val expenses: List<ExpenseVoucher> = emptyList(),
    val transfers: List<AccountTransfer> = emptyList(),
    val reconciliations: List<BankReconciliation> = emptyList(),
    val currentReconciliationItems: List<BankReconciliationItem> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val backupRecords: List<BackupRecord> = emptyList(),
    val profitAndLoss: ProfitAndLossStatement? = null,
    val balanceSheet: BalanceSheet? = null,
    val cashFlow: CashFlowStatement? = null,
    val receivablesAging: AgingReport? = null,
    val payablesAging: AgingReport? = null,
    val cashBookEntries: List<CashBankBookEntry> = emptyList(),
    val bankBookEntries: List<CashBankBookEntry> = emptyList(),
    val selectedBankAccountId: String = "coa-1020",
    val businessProfile: BusinessProfile? = null,
    val language: AppLanguage = AppLanguage.BANGLA,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountingRepository: AccountingRepository,
    private val financialRepository: FinancialRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState(isLoading = true))
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadFinancialStatements()
    }

    private fun loadData() {
        viewModelScope.launch {
            val accountingFlow = combine(
                accountingRepository.getAllAccounts(),
                accountingRepository.getAllJournalEntries(),
                financialRepository.getAllExpenses(),
                financialRepository.getAllTransfers(),
                financialRepository.getAllReconciliations()
            ) { accounts, entries, expenses, transfers, reconciliations ->
                _uiState.value.copy(
                    accounts = accounts,
                    journalEntries = entries,
                    expenses = expenses,
                    transfers = transfers,
                    reconciliations = reconciliations
                )
            }

            combine(
                accountingFlow,
                preferencesRepository.selectedLanguage
            ) { state, lang ->
                state.copy(language = lang, isLoading = false)
            }.collect { state ->
                _uiState.value = state
                refreshTrialBalance()
            }
        }

        viewModelScope.launch {
            backupRepository.getAllBackupRecords().collect { backups ->
                _uiState.update { it.copy(backupRecords = backups) }
            }
        }

        viewModelScope.launch {
            financialRepository.getAllAuditLogs().collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }

        viewModelScope.launch {
            val profile = settingsRepository.getBusinessProfile().firstOrNull()
            _uiState.update { it.copy(businessProfile = profile) }
        }
    }

    fun refreshTrialBalance() {
        viewModelScope.launch {
            try {
                val tbItems = accountingRepository.getTrialBalance()
                val totalDebit = tbItems.sumOf { it.netDebitMinor }
                val totalCredit = tbItems.sumOf { it.netCreditMinor }
                _uiState.update {
                    it.copy(
                        trialBalance = tbItems,
                        totalTrialDebit = totalDebit,
                        totalTrialCredit = totalCredit,
                        isBalanced = (totalDebit == totalCredit)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFinancialStatements() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val startOfYear = now - (180L * 86400000L) // Last 6 months for overview

                val pnl = financialRepository.getProfitAndLoss(startOfYear, now)
                val bs = financialRepository.getBalanceSheet(now)
                val cf = financialRepository.getCashFlowStatement(startOfYear, now)
                val recAging = financialRepository.getReceivablesAging()
                val payAging = financialRepository.getPayablesAging()

                val cashBook = financialRepository.getCashBook(startOfYear, now)
                val bankBook = financialRepository.getBankBook(_uiState.value.selectedBankAccountId, startOfYear, now)

                _uiState.update {
                    it.copy(
                        profitAndLoss = pnl,
                        balanceSheet = bs,
                        cashFlow = cf,
                        receivablesAging = recAging,
                        payablesAging = payAging,
                        cashBookEntries = cashBook,
                        bankBookEntries = bankBook
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectBankAccountForBook(bankAccountId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfYear = now - (180L * 86400000L)
            val bankBook = financialRepository.getBankBook(bankAccountId, startOfYear, now)
            _uiState.update {
                it.copy(selectedBankAccountId = bankAccountId, bankBookEntries = bankBook)
            }
        }
    }

    // Expense Management
    fun recordExpense(
        category: String,
        descEn: String,
        descBn: String,
        amountMinor: Long,
        currency: String,
        exchangeRateToBdt: String,
        paymentAccountId: String,
        expenseAccountId: String,
        voucherDate: Long,
        referenceNumber: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                financialRepository.recordExpense(
                    category = category,
                    descEn = descEn,
                    descBn = descBn,
                    amountMinor = amountMinor,
                    currency = currency,
                    exchangeRateToBdt = exchangeRateToBdt,
                    paymentAccountId = paymentAccountId,
                    expenseAccountId = expenseAccountId,
                    voucherDate = voucherDate,
                    referenceNumber = referenceNumber,
                    notes = notes
                )
                refreshTrialBalance()
                loadFinancialStatements()
                _uiState.update { it.copy(isLoading = false, userMessage = "Expense voucher recorded and posted!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Expense Error: ${e.message}") }
            }
        }
    }

    fun reverseExpense(expenseId: String, reason: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                financialRepository.reverseExpense(expenseId, reason)
                refreshTrialBalance()
                loadFinancialStatements()
                _uiState.update { it.copy(isLoading = false, userMessage = "Expense voucher reversed successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Reversal Error: ${e.message}") }
            }
        }
    }

    // Cash & Bank Transfers
    fun transferFunds(
        sourceAccountId: String,
        destAccountId: String,
        amountMinor: Long,
        transferDate: Long,
        reference: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                financialRepository.transferFunds(
                    sourceAccountId = sourceAccountId,
                    destAccountId = destAccountId,
                    amountMinor = amountMinor,
                    transferDate = transferDate,
                    reference = reference,
                    notes = notes
                )
                refreshTrialBalance()
                loadFinancialStatements()
                _uiState.update { it.copy(isLoading = false, userMessage = "Funds transferred successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Transfer Error: ${e.message}") }
            }
        }
    }

    // Bank Reconciliation
    fun startReconciliation(
        bankAccountId: String,
        startDate: Long,
        endDate: Long,
        stmtOpeningMinor: Long,
        stmtClosingMinor: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val recId = financialRepository.createReconciliation(
                    bankAccountId = bankAccountId,
                    startDate = startDate,
                    endDate = endDate,
                    stmtOpeningMinor = stmtOpeningMinor,
                    stmtClosingMinor = stmtClosingMinor,
                    notes = notes
                )
                loadReconciliationItems(recId)
                _uiState.update { it.copy(isLoading = false, userMessage = "Bank reconciliation initiated!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Reconciliation Error: ${e.message}") }
            }
        }
    }

    fun loadReconciliationItems(recId: String) {
        viewModelScope.launch {
            financialRepository.getReconciliationItems(recId).collect { items ->
                _uiState.update { it.copy(currentReconciliationItems = items) }
            }
        }
    }

    fun matchReconciliationItem(itemId: String, isMatched: Boolean, ref: String?) {
        viewModelScope.launch {
            try {
                financialRepository.matchReconciliationItem(itemId, isMatched, ref)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun finalizeReconciliation(recId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                financialRepository.finalizeReconciliation(recId)
                _uiState.update { it.copy(isLoading = false, userMessage = "Reconciliation finalized!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Finalize Error: ${e.message}") }
            }
        }
    }

    // Backup & Restore
    fun createBackup(notes: String?) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val record = backupRepository.createBackup(isAutomatic = false, notes = notes)
                _uiState.update {
                    it.copy(isLoading = false, userMessage = "Offline backup created: ${record.fileName}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Backup Error: ${e.message}") }
            }
        }
    }

    fun restoreBackup(backupFile: File) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = backupRepository.restoreBackup(backupFile)
                if (result.isSuccess) {
                    refreshTrialBalance()
                    loadFinancialStatements()
                    _uiState.update {
                        it.copy(isLoading = false, userMessage = "Database restored successfully from ${backupFile.name}!")
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, userMessage = "Restore Failed: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Restore Error: ${e.message}") }
            }
        }
    }

    // Export Reports
    fun exportProfitAndLossPdf(context: Context) {
        val pnl = _uiState.value.profitAndLoss ?: return
        try {
            val file = FinancialPdfGenerator.generateProfitAndLossPdf(
                context = context,
                pnl = pnl,
                businessProfile = _uiState.value.businessProfile,
                isBangla = _uiState.value.language == AppLanguage.BANGLA
            )
            FinancialPdfGenerator.sharePdf(context, file, "Profit and Loss Statement")
        } catch (e: Exception) {
            _uiState.update { it.copy(userMessage = "PDF Export Error: ${e.message}") }
        }
    }

    fun exportBalanceSheetPdf(context: Context) {
        val bs = _uiState.value.balanceSheet ?: return
        try {
            val file = FinancialPdfGenerator.generateBalanceSheetPdf(
                context = context,
                bs = bs,
                businessProfile = _uiState.value.businessProfile,
                isBangla = _uiState.value.language == AppLanguage.BANGLA
            )
            FinancialPdfGenerator.sharePdf(context, file, "Balance Sheet")
        } catch (e: Exception) {
            _uiState.update { it.copy(userMessage = "PDF Export Error: ${e.message}") }
        }
    }

    fun exportFinancialCsv(context: Context, type: String) {
        try {
            val file = when (type) {
                "PNL" -> _uiState.value.profitAndLoss?.let { FinancialCsvExporter.exportProfitAndLossCsv(context, it) }
                "BALANCE_SHEET" -> _uiState.value.balanceSheet?.let { FinancialCsvExporter.exportBalanceSheetCsv(context, it) }
                "CASH_BOOK" -> FinancialCsvExporter.exportCashBankBookCsv(context, "Cash Book", _uiState.value.cashBookEntries)
                "BANK_BOOK" -> FinancialCsvExporter.exportCashBankBookCsv(context, "Bank Book", _uiState.value.bankBookEntries)
                "TRIAL_BALANCE" -> FinancialCsvExporter.exportTrialBalanceCsv(context, _uiState.value.trialBalance)
                "RECEIVABLES_AGING" -> _uiState.value.receivablesAging?.let { FinancialCsvExporter.exportAgingReportCsv(context, it) }
                "PAYABLES_AGING" -> _uiState.value.payablesAging?.let { FinancialCsvExporter.exportAgingReportCsv(context, it) }
                else -> null
            }
            if (file != null) {
                FinancialCsvExporter.shareCsv(context, file, "Export $type CSV")
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(userMessage = "CSV Export Error: ${e.message}") }
        }
    }

    fun postManualJournalEntry(
        descEn: String,
        descBn: String,
        lines: List<JournalLineDraft>
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                accountingRepository.postJournalEntry(
                    entryDate = System.currentTimeMillis(),
                    descEn = descEn,
                    descBn = descBn,
                    referenceType = "MANUAL",
                    referenceId = null,
                    lines = lines
                )
                refreshTrialBalance()
                loadFinancialStatements()
                _uiState.update { it.copy(isLoading = false, userMessage = "Journal Entry posted successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    fun reverseJournalEntry(entryId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                accountingRepository.reverseJournalEntry(entryId)
                refreshTrialBalance()
                loadFinancialStatements()
                _uiState.update { it.copy(isLoading = false, userMessage = "Journal Entry reversed successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Reversal Error: ${e.message}") }
            }
        }
    }

    fun createAccount(
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        desc: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                accountingRepository.createAccount(code, nameEn, nameBn, type, normalBalance, desc)
                refreshTrialBalance()
                _uiState.update { it.copy(isLoading = false, userMessage = "Account created in Chart of Accounts!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
