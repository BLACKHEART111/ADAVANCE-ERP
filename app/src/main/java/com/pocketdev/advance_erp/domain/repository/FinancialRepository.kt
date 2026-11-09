package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.domain.model.AgingReport
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.model.CashFlowStatement
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import kotlinx.coroutines.flow.Flow

interface FinancialRepository {

    // 1. Expense Management
    fun getAllExpenses(): Flow<List<ExpenseVoucher>>
    suspend fun recordExpense(
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
    ): String

    suspend fun reverseExpense(expenseId: String, reason: String): String

    // 2. Cash & Bank Management
    fun getAllTransfers(): Flow<List<AccountTransfer>>
    suspend fun transferFunds(
        sourceAccountId: String,
        destAccountId: String,
        amountMinor: Long,
        transferDate: Long,
        reference: String?,
        notes: String?
    ): String

    suspend fun getCashBook(fromDate: Long, toDate: Long): List<CashBankBookEntry>
    suspend fun getBankBook(bankAccountId: String, fromDate: Long, toDate: Long): List<CashBankBookEntry>
    suspend fun getAccountBalanceMinor(accountId: String): Long

    // 3. Bank Reconciliation
    fun getAllReconciliations(): Flow<List<BankReconciliation>>
    fun getReconciliationItems(recId: String): Flow<List<BankReconciliationItem>>
    suspend fun createReconciliation(
        bankAccountId: String,
        startDate: Long,
        endDate: Long,
        stmtOpeningMinor: Long,
        stmtClosingMinor: Long,
        notes: String?
    ): String

    suspend fun matchReconciliationItem(itemId: String, isMatched: Boolean, stmtRef: String?)
    suspend fun finalizeReconciliation(recId: String)
    suspend fun recordReconciliationAdjustment(
        recId: String,
        desc: String,
        amountMinor: Long,
        isBankCharge: Boolean,
        bankAccountId: String
    ): String

    // 4. Financial Statements
    suspend fun getProfitAndLoss(fromDate: Long, toDate: Long): ProfitAndLossStatement
    suspend fun getBalanceSheet(asOfDate: Long): BalanceSheet
    suspend fun getCashFlowStatement(fromDate: Long, toDate: Long): CashFlowStatement
    suspend fun getReceivablesAging(): AgingReport
    suspend fun getPayablesAging(): AgingReport

    // 5. Audit Logging
    fun getAllAuditLogs(): Flow<List<AuditLog>>
    suspend fun logAudit(
        recordType: String,
        recordId: String,
        action: String,
        details: String
    )
}
