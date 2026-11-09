package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.dao.AuditLogDao
import com.pocketdev.advance_erp.data.dao.CashBankDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.ReconciliationStatus
import com.pocketdev.advance_erp.domain.model.AccountSummaryItem
import com.pocketdev.advance_erp.domain.model.AgingBucket
import com.pocketdev.advance_erp.domain.model.AgingReport
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.model.CashFlowStatement
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.FinancialRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialRepositoryImpl @Inject constructor(
    private val database: AdvanceErpDatabase,
    private val accountingDao: AccountingDao,
    private val expenseDao: ExpenseDao,
    private val cashBankDao: CashBankDao,
    private val auditLogDao: AuditLogDao,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val accountingRepository: AccountingRepository
) : FinancialRepository {

    override fun getAllExpenses(): Flow<List<ExpenseVoucher>> = expenseDao.getAllExpenses()

    override suspend fun recordExpense(
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
    ): String {
        require(amountMinor > 0L) { "Expense amount must be greater than zero: $amountMinor" }

        val bdtAmountMinor = if (currency.equals("BDT", ignoreCase = true)) {
            amountMinor
        } else {
            val rate = BigDecimal(exchangeRateToBdt.ifBlank { "1.0" })
            BigDecimal.valueOf(amountMinor)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }

        val count = expenseDao.getExpenseCount()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(voucherDate))
        val voucherNumber = "EXP-$dateFormat-%04d".format(count + 1)
        val voucherId = "exp-${UUID.randomUUID()}"

        // Create balanced journal entry: Debit Expense Account, Credit Payment Account
        val journalLines = listOf(
            JournalLineDraft(
                accountId = expenseAccountId,
                isDebit = true,
                amountMinor = bdtAmountMinor,
                description = "$descEn [$voucherNumber]"
            ),
            JournalLineDraft(
                accountId = paymentAccountId,
                isDebit = false,
                amountMinor = bdtAmountMinor,
                description = "Paid for $descEn [$voucherNumber]"
            )
        )

        val journalEntryId = accountingRepository.postJournalEntry(
            entryDate = voucherDate,
            descEn = "Expense Voucher $voucherNumber: $descEn",
            descBn = "ব্যয় ভাউচার $voucherNumber: $descBn",
            referenceType = "EXPENSE",
            referenceId = voucherId,
            lines = journalLines
        )

        val voucher = ExpenseVoucher(
            id = voucherId,
            voucherNumber = voucherNumber,
            category = category,
            descriptionEn = descEn,
            descriptionBn = descBn,
            amountMinor = amountMinor,
            currency = currency.uppercase(),
            exchangeRateToBdt = exchangeRateToBdt,
            bdtAmountMinor = bdtAmountMinor,
            paymentAccountId = paymentAccountId,
            expenseAccountId = expenseAccountId,
            voucherDate = voucherDate,
            referenceNumber = referenceNumber,
            isPosted = true,
            isReversed = false,
            journalEntryId = journalEntryId,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        expenseDao.insertExpense(voucher)

        logAudit(
            recordType = "EXPENSE",
            recordId = voucherId,
            action = "POST",
            details = "Posted voucher $voucherNumber for $currency ${amountMinor / 100.0} (BDT ${bdtAmountMinor / 100.0})"
        )

        return voucherId
    }

    override suspend fun reverseExpense(expenseId: String, reason: String): String {
        val voucher = expenseDao.getExpenseById(expenseId)
            ?: throw IllegalArgumentException("Expense voucher not found: $expenseId")

        require(!voucher.isReversed) { "Expense voucher is already reversed: ${voucher.voucherNumber}" }
        val jeId = voucher.journalEntryId
            ?: throw IllegalStateException("No journal entry associated with voucher: ${voucher.voucherNumber}")

        val reversalJeId = accountingRepository.reverseJournalEntry(jeId)
        expenseDao.markExpenseReversed(expenseId, reversalJeId)

        logAudit(
            recordType = "EXPENSE",
            recordId = expenseId,
            action = "REVERSE",
            details = "Reversed voucher ${voucher.voucherNumber}: $reason. Reversal JE: $reversalJeId"
        )

        return reversalJeId
    }

    override fun getAllTransfers(): Flow<List<AccountTransfer>> = cashBankDao.getAllTransfers()

    override suspend fun transferFunds(
        sourceAccountId: String,
        destAccountId: String,
        amountMinor: Long,
        transferDate: Long,
        reference: String?,
        notes: String?
    ): String {
        require(sourceAccountId != destAccountId) { "Source and destination accounts cannot be the same" }
        require(amountMinor > 0L) { "Transfer amount must be greater than zero: $amountMinor" }

        val sourceAccount = accountingDao.getAccountById(sourceAccountId)
            ?: throw IllegalArgumentException("Source account not found: $sourceAccountId")
        val destAccount = accountingDao.getAccountById(destAccountId)
            ?: throw IllegalArgumentException("Destination account not found: $destAccountId")

        val count = cashBankDao.getTransferCount()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(transferDate))
        val transferNumber = "TRF-$dateFormat-%04d".format(count + 1)
        val transferId = "trf-${UUID.randomUUID()}"

        // Balanced journal entry: Debit Destination Account, Credit Source Account
        val journalLines = listOf(
            JournalLineDraft(
                accountId = destAccountId,
                isDebit = true,
                amountMinor = amountMinor,
                description = "Funds transfer received from ${sourceAccount.nameEn} [$transferNumber]"
            ),
            JournalLineDraft(
                accountId = sourceAccountId,
                isDebit = false,
                amountMinor = amountMinor,
                description = "Funds transfer sent to ${destAccount.nameEn} [$transferNumber]"
            )
        )

        val journalEntryId = accountingRepository.postJournalEntry(
            entryDate = transferDate,
            descEn = "Inter-Account Transfer $transferNumber: ${sourceAccount.nameEn} -> ${destAccount.nameEn}",
            descBn = "আন্তঃঅ্যাকাউন্ট তহবিল স্থানান্তর $transferNumber: ${sourceAccount.nameBn} -> ${destAccount.nameBn}",
            referenceType = "TRANSFER",
            referenceId = transferId,
            lines = journalLines
        )

        val transfer = AccountTransfer(
            id = transferId,
            transferNumber = transferNumber,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amountMinor = amountMinor,
            transferDate = transferDate,
            referenceNumber = reference,
            notes = notes,
            journalEntryId = journalEntryId,
            createdAt = System.currentTimeMillis()
        )

        cashBankDao.insertTransfer(transfer)

        logAudit(
            recordType = "TRANSFER",
            recordId = transferId,
            action = "TRANSFER",
            details = "Transferred BDT ${amountMinor / 100.0} from ${sourceAccount.nameEn} to ${destAccount.nameEn} [$transferNumber]"
        )

        return transferId
    }

    override suspend fun getCashBook(fromDate: Long, toDate: Long): List<CashBankBookEntry> {
        return getBookForAccount("coa-1010", fromDate, toDate)
    }

    override suspend fun getBankBook(bankAccountId: String, fromDate: Long, toDate: Long): List<CashBankBookEntry> {
        return getBookForAccount(bankAccountId, fromDate, toDate)
    }

    private suspend fun getBookForAccount(accountId: String, fromDate: Long, toDate: Long): List<CashBankBookEntry> {
        val account = accountingDao.getAccountById(accountId) ?: return emptyList()
        val allPostedLines = accountingDao.getActivePostedLines().filter { it.accountId == accountId }
        val entriesMap = accountingDao.getAllJournalEntries().let { flow ->
            // Fetch sync by reading active entries
            emptyMap<String, Long>()
        }

        // Get journal entries to associate entry dates
        val activeEntries = accountingDao.getAllJournalEntries()
        // Sort chronologically
        var runningBal = 0L
        val bookEntries = mutableListOf<CashBankBookEntry>()

        for (line in allPostedLines) {
            val je = accountingDao.getJournalEntryById(line.journalEntryId)
            val entryDate = je?.entryDate ?: je?.createdAt ?: System.currentTimeMillis()
            val receipt = if (line.isDebit) line.amountMinor else 0L
            val payment = if (!line.isDebit) line.amountMinor else 0L
            runningBal += (receipt - payment)

            if (entryDate in fromDate..toDate) {
                bookEntries.add(
                    CashBankBookEntry(
                        id = line.id,
                        date = entryDate,
                        referenceNumber = je?.entryNumber ?: "JE",
                        description = line.description ?: je?.descriptionEn ?: "Transaction",
                        receiptMinor = receipt,
                        paymentMinor = payment,
                        runningBalanceMinor = runningBal,
                        accountCode = account.accountCode,
                        accountName = account.nameEn
                    )
                )
            }
        }

        return bookEntries.sortedBy { it.date }
    }

    override suspend fun getAccountBalanceMinor(accountId: String): Long {
        val lines = accountingDao.getActivePostedLines().filter { it.accountId == accountId }
        val debit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val credit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }
        val account = accountingDao.getAccountById(accountId)
        return if (account?.accountType in listOf("ASSET", "EXPENSE")) {
            debit - credit
        } else {
            credit - debit
        }
    }

    // 3. Bank Reconciliation
    override fun getAllReconciliations(): Flow<List<BankReconciliation>> = cashBankDao.getAllReconciliations()

    override fun getReconciliationItems(recId: String): Flow<List<BankReconciliationItem>> =
        cashBankDao.getItemsForReconciliation(recId)

    override suspend fun createReconciliation(
        bankAccountId: String,
        startDate: Long,
        endDate: Long,
        stmtOpeningMinor: Long,
        stmtClosingMinor: Long,
        notes: String?
    ): String {
        val count = cashBankDao.getReconciliationCount()
        val dateFormat = SimpleDateFormat("yyyyMM", Locale.US).format(Date(endDate))
        val recNumber = "BRC-$dateFormat-%04d".format(count + 1)
        val recId = "brc-${UUID.randomUUID()}"

        val allLines = accountingDao.getActivePostedLines().filter { it.accountId == bankAccountId }
        val recItems = mutableListOf<BankReconciliationItem>()

        var erpBalance = stmtOpeningMinor
        for (line in allLines) {
            val je = accountingDao.getJournalEntryById(line.journalEntryId)
            val entryDate = je?.entryDate ?: je?.createdAt ?: System.currentTimeMillis()
            if (entryDate in startDate..endDate) {
                if (line.isDebit) {
                    erpBalance += line.amountMinor
                } else {
                    erpBalance -= line.amountMinor
                }

                recItems.add(
                    BankReconciliationItem(
                        id = "bri-${UUID.randomUUID()}",
                        reconciliationId = recId,
                        journalLineId = line.id,
                        transactionDate = entryDate,
                        description = line.description ?: je?.descriptionEn ?: "Bank Transaction",
                        amountMinor = line.amountMinor,
                        isDebit = line.isDebit,
                        isMatched = false,
                        statementReference = null,
                        matchedAt = null
                    )
                )
            }
        }

        val initialDiscrepancy = stmtClosingMinor - erpBalance

        val rec = BankReconciliation(
            id = recId,
            reconciliationNumber = recNumber,
            bankAccountId = bankAccountId,
            statementStartDate = startDate,
            statementEndDate = endDate,
            statementOpeningBalanceMinor = stmtOpeningMinor,
            statementClosingBalanceMinor = stmtClosingMinor,
            erpClosingBalanceMinor = erpBalance,
            reconciledBalanceMinor = stmtOpeningMinor,
            discrepancyMinor = initialDiscrepancy,
            status = if (initialDiscrepancy == 0L) ReconciliationStatus.RECONCILED.name else ReconciliationStatus.IN_PROGRESS.name,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        cashBankDao.createReconciliationWithItems(rec, recItems)

        logAudit(
            recordType = "RECONCILIATION",
            recordId = recId,
            action = "CREATE",
            details = "Created bank reconciliation $recNumber for account $bankAccountId, discrepancy: BDT ${initialDiscrepancy / 100.0}"
        )

        return recId
    }

    override suspend fun matchReconciliationItem(itemId: String, isMatched: Boolean, stmtRef: String?) {
        val matchedAt = if (isMatched) System.currentTimeMillis() else null
        cashBankDao.setItemMatched(itemId, isMatched, stmtRef, matchedAt)
    }

    override suspend fun finalizeReconciliation(recId: String) {
        val rec = cashBankDao.getReconciliationById(recId)
            ?: throw IllegalArgumentException("Reconciliation not found: $recId")

        val items = cashBankDao.getItemsForReconciliationSync(recId)
        val matchedItems = items.filter { it.isMatched }

        val matchedNet = matchedItems.sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        val reconciledBalance = rec.statementOpeningBalanceMinor + matchedNet
        val discrepancy = rec.statementClosingBalanceMinor - reconciledBalance

        val status = if (discrepancy == 0L) ReconciliationStatus.RECONCILED.name else ReconciliationStatus.DISCREPANCY.name

        val updated = rec.copy(
            reconciledBalanceMinor = reconciledBalance,
            discrepancyMinor = discrepancy,
            status = status,
            reconciledAt = if (status == ReconciliationStatus.RECONCILED.name) System.currentTimeMillis() else null
        )

        cashBankDao.updateReconciliation(updated)

        logAudit(
            recordType = "RECONCILIATION",
            recordId = recId,
            action = "MATCH",
            details = "Finalized reconciliation ${rec.reconciliationNumber}. Status: $status, Discrepancy: BDT ${discrepancy / 100.0}"
        )
    }

    override suspend fun recordReconciliationAdjustment(
        recId: String,
        desc: String,
        amountMinor: Long,
        isBankCharge: Boolean,
        bankAccountId: String
    ): String {
        require(amountMinor > 0L) { "Adjustment amount must be positive" }

        val bankAccount = accountingDao.getAccountById(bankAccountId)
            ?: throw IllegalArgumentException("Bank account not found: $bankAccountId")

        val lines = if (isBankCharge) {
            // Debit Bank Charges 5150, Credit Bank Account
            listOf(
                JournalLineDraft(
                    accountId = "coa-5150",
                    isDebit = true,
                    amountMinor = amountMinor,
                    description = "$desc [Bank Reconciliation Adjustment]"
                ),
                JournalLineDraft(
                    accountId = bankAccountId,
                    isDebit = false,
                    amountMinor = amountMinor,
                    description = "Bank fee deduction [Reconciliation Adjustment]"
                )
            )
        } else {
            // Interest/credit: Debit Bank Account, Credit Revenue 4010
            listOf(
                JournalLineDraft(
                    accountId = bankAccountId,
                    isDebit = true,
                    amountMinor = amountMinor,
                    description = "Bank interest/credit [Reconciliation Adjustment]"
                ),
                JournalLineDraft(
                    accountId = "coa-4010",
                    isDebit = false,
                    amountMinor = amountMinor,
                    description = "$desc [Reconciliation Adjustment]"
                )
            )
        }

        val jeId = accountingRepository.postJournalEntry(
            entryDate = System.currentTimeMillis(),
            descEn = "Bank Reconciliation Adjustment: $desc",
            descBn = "ব্যাংক সমন্বয় দাখিলা: $desc",
            referenceType = "RECONCILIATION",
            referenceId = recId,
            lines = lines
        )

        logAudit(
            recordType = "RECONCILIATION",
            recordId = recId,
            action = "POST",
            details = "Posted reconciliation adjustment JE $jeId for BDT ${amountMinor / 100.0}"
        )

        return jeId
    }

    // 4. Financial Statements
    override suspend fun getProfitAndLoss(fromDate: Long, toDate: Long): ProfitAndLossStatement {
        val accounts = accountingDao.getAllAccountsSync().associateBy { it.id }
        val activeLines = accountingDao.getActivePostedLines()

        val linesInRange = activeLines.filter { line ->
            // Filter by journal entry date
            val je = accountingDao.getJournalEntryById(line.journalEntryId)
            val date = je?.entryDate ?: je?.createdAt ?: 0L
            date in fromDate..toDate
        }

        val linesByAccount = linesInRange.groupBy { it.accountId }

        // Revenue accounts
        val grossSalesMinor = linesByAccount["coa-4010"]?.let { list ->
            list.filter { !it.isDebit }.sumOf { it.amountMinor } - list.filter { it.isDebit }.sumOf { it.amountMinor }
        } ?: 0L

        val salesReturnsMinor = linesByAccount["coa-4020"]?.let { list ->
            list.filter { it.isDebit }.sumOf { it.amountMinor } - list.filter { !it.isDebit }.sumOf { it.amountMinor }
        } ?: 0L

        val salesDiscountsMinor = linesByAccount["coa-4030"]?.let { list ->
            list.filter { it.isDebit }.sumOf { it.amountMinor } - list.filter { !it.isDebit }.sumOf { it.amountMinor }
        } ?: 0L

        val netSalesRevenue = grossSalesMinor - salesReturnsMinor - salesDiscountsMinor

        val cogsMinor = linesByAccount["coa-5010"]?.let { list ->
            list.filter { it.isDebit }.sumOf { it.amountMinor } - list.filter { !it.isDebit }.sumOf { it.amountMinor }
        } ?: 0L

        val grossProfit = netSalesRevenue - cogsMinor

        // Operating Expenses (5020, 5030, 5040, 5110, 5120, 5130, 5140, 5150, 5190)
        val opExpAccountCodes = listOf("5020", "5030", "5040", "5110", "5120", "5130", "5140", "5150", "5190")
        val operatingExpenses = mutableListOf<AccountSummaryItem>()
        var totalOpExpenses = 0L

        for (code in opExpAccountCodes) {
            val accId = "coa-$code"
            val acc = accounts[accId]
            val lines = linesByAccount[accId] ?: emptyList()
            val netDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor } - lines.filter { !it.isDebit }.sumOf { it.amountMinor }
            if (acc != null && netDebit != 0L) {
                operatingExpenses.add(
                    AccountSummaryItem(
                        accountId = acc.id,
                        accountCode = acc.accountCode,
                        nameEn = acc.nameEn,
                        nameBn = acc.nameBn,
                        amountMinor = netDebit
                    )
                )
                totalOpExpenses += netDebit
            }
        }

        val operatingProfit = grossProfit - totalOpExpenses

        // FX Gain / Loss (5090)
        val fxNetDebit = linesByAccount["coa-5090"]?.let { list ->
            list.filter { it.isDebit }.sumOf { it.amountMinor } - list.filter { !it.isDebit }.sumOf { it.amountMinor }
        } ?: 0L
        // Net Debit is FX Loss; Net Credit is FX Gain
        val fxGainLoss = -fxNetDebit

        val netProfit = operatingProfit + fxGainLoss

        return ProfitAndLossStatement(
            fromDate = fromDate,
            toDate = toDate,
            grossSalesMinor = grossSalesMinor,
            salesReturnsMinor = salesReturnsMinor,
            salesDiscountsMinor = salesDiscountsMinor,
            netSalesRevenueMinor = netSalesRevenue,
            costOfGoodsSoldMinor = cogsMinor,
            grossProfitMinor = grossProfit,
            operatingExpenses = operatingExpenses,
            totalOperatingExpensesMinor = totalOpExpenses,
            operatingProfitMinor = operatingProfit,
            foreignExchangeGainLossMinor = fxGainLoss,
            netProfitMinor = netProfit
        )
    }

    override suspend fun getBalanceSheet(asOfDate: Long): BalanceSheet {
        val accounts = accountingDao.getAllAccountsSync().associateBy { it.id }
        val activeLines = accountingDao.getActivePostedLines()

        val linesUpToDate = activeLines.filter { line ->
            val je = accountingDao.getJournalEntryById(line.journalEntryId)
            val date = je?.entryDate ?: je?.createdAt ?: 0L
            date <= asOfDate
        }

        val linesByAccount = linesUpToDate.groupBy { it.accountId }

        // Assets (Debit normal)
        val assetAccounts = accounts.values.filter { it.accountType == "ASSET" }.sortedBy { it.accountCode }
        val currentAssets = mutableListOf<AccountSummaryItem>()
        var totalAssets = 0L

        for (acc in assetAccounts) {
            val lines = linesByAccount[acc.id] ?: emptyList()
            val netBalance = lines.filter { it.isDebit }.sumOf { it.amountMinor } - lines.filter { !it.isDebit }.sumOf { it.amountMinor }
            currentAssets.add(
                AccountSummaryItem(
                    accountId = acc.id,
                    accountCode = acc.accountCode,
                    nameEn = acc.nameEn,
                    nameBn = acc.nameBn,
                    amountMinor = netBalance
                )
            )
            totalAssets += netBalance
        }

        // Liabilities (Credit normal)
        val liabilityAccounts = accounts.values.filter { it.accountType == "LIABILITY" }.sortedBy { it.accountCode }
        val currentLiabilities = mutableListOf<AccountSummaryItem>()
        var totalLiabilities = 0L

        for (acc in liabilityAccounts) {
            val lines = linesByAccount[acc.id] ?: emptyList()
            val netBalance = lines.filter { !it.isDebit }.sumOf { it.amountMinor } - lines.filter { it.isDebit }.sumOf { it.amountMinor }
            currentLiabilities.add(
                AccountSummaryItem(
                    accountId = acc.id,
                    accountCode = acc.accountCode,
                    nameEn = acc.nameEn,
                    nameBn = acc.nameBn,
                    amountMinor = netBalance
                )
            )
            totalLiabilities += netBalance
        }

        // Equity: Owner Equity 3010 + Retained Earnings 3020 + Cumulative Net Profit
        val equityAccounts = accounts.values.filter { it.accountType == "EQUITY" && it.accountCode != "3020" }.sortedBy { it.accountCode }
        val equityItems = mutableListOf<AccountSummaryItem>()
        var directEquity = 0L

        for (acc in equityAccounts) {
            val lines = linesByAccount[acc.id] ?: emptyList()
            val netBalance = lines.filter { !it.isDebit }.sumOf { it.amountMinor } - lines.filter { it.isDebit }.sumOf { it.amountMinor }
            equityItems.add(
                AccountSummaryItem(
                    accountId = acc.id,
                    accountCode = acc.accountCode,
                    nameEn = acc.nameEn,
                    nameBn = acc.nameBn,
                    amountMinor = netBalance
                )
            )
            directEquity += netBalance
        }

        // P&L Net Profit up to asOfDate represents cumulative retained earnings from operations
        val pnl = getProfitAndLoss(0L, asOfDate)
        val retainedEarnings = pnl.netProfitMinor
        val totalEquity = directEquity + retainedEarnings
        val totalLiabilitiesAndEquity = totalLiabilities + totalEquity

        val isBalanced = (totalAssets == totalLiabilitiesAndEquity)

        return BalanceSheet(
            asOfDate = asOfDate,
            currentAssets = currentAssets,
            totalCurrentAssetsMinor = totalAssets,
            totalAssetsMinor = totalAssets,
            currentLiabilities = currentLiabilities,
            totalCurrentLiabilitiesMinor = totalLiabilities,
            totalLiabilitiesMinor = totalLiabilities,
            equityItems = equityItems,
            retainedEarningsMinor = retainedEarnings,
            totalEquityMinor = totalEquity,
            totalLiabilitiesAndEquityMinor = totalLiabilitiesAndEquity,
            isBalanced = isBalanced
        )
    }

    override suspend fun getCashFlowStatement(fromDate: Long, toDate: Long): CashFlowStatement {
        // Customer payments received in period
        val customerPayments = customerPaymentDao.getAllPaymentsSync()
            .filter { it.paymentDate in fromDate..toDate && it.isPosted }
        val cashInflowCustomers = customerPayments.sumOf { it.amountMinor }

        // Supplier payments made in period
        val supplierPayments = supplierPaymentDao.getAllPaymentsSync()
            .filter { it.paymentDate in fromDate..toDate }
        val cashOutflowSuppliers = supplierPayments.sumOf { it.bdtAmountMinor }

        // Operating expenses paid in cash/bank
        val expenses = expenseDao.getAllExpensesSync()
            .filter { it.voucherDate in fromDate..toDate && it.isPosted && !it.isReversed }
        val cashOutflowExpenses = expenses.sumOf { it.bdtAmountMinor }

        val netOperations = cashInflowCustomers - cashOutflowSuppliers - cashOutflowExpenses
        val netInvesting = 0L
        val netFinancing = 0L
        val netChange = netOperations + netInvesting + netFinancing

        // Cash & Bank accounts: 1010, 1020, 1030
        val cashBankIds = listOf("coa-1010", "coa-1020", "coa-1030")
        val allPostedLines = accountingDao.getActivePostedLines().filter { it.accountId in cashBankIds }

        var openingBal = 0L
        var closingBal = 0L

        for (line in allPostedLines) {
            val je = accountingDao.getJournalEntryById(line.journalEntryId)
            val date = je?.entryDate ?: je?.createdAt ?: 0L
            val net = if (line.isDebit) line.amountMinor else -line.amountMinor
            if (date < fromDate) {
                openingBal += net
            }
            if (date <= toDate) {
                closingBal += net
            }
        }

        return CashFlowStatement(
            fromDate = fromDate,
            toDate = toDate,
            cashInflowCustomersMinor = cashInflowCustomers,
            cashOutflowSuppliersMinor = cashOutflowSuppliers,
            cashOutflowExpensesMinor = cashOutflowExpenses,
            netCashFromOperationsMinor = netOperations,
            netCashFromInvestingMinor = netInvesting,
            netCashFromFinancingMinor = netFinancing,
            netChangeInCashMinor = netChange,
            openingCashBankMinor = openingBal,
            closingCashBankMinor = closingBal
        )
    }

    override suspend fun getReceivablesAging(): AgingReport {
        val invoices = salesDao.getAllInvoicesSync().filter { it.isPosted && it.dueAmountMinor > 0L }
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        var bucket0To30 = 0L
        var bucket31To60 = 0L
        var bucket61To90 = 0L
        var bucket90Plus = 0L
        val total = invoices.sumOf { it.dueAmountMinor }

        for (inv in invoices) {
            val ageDays = (now - inv.invoiceDate) / dayMs
            when {
                ageDays <= 30 -> bucket0To30 += inv.dueAmountMinor
                ageDays <= 60 -> bucket31To60 += inv.dueAmountMinor
                ageDays <= 90 -> bucket61To90 += inv.dueAmountMinor
                else -> bucket90Plus += inv.dueAmountMinor
            }
        }

        fun pct(amt: Long) = if (total > 0L) (amt.toDouble() / total.toDouble()) * 100.0 else 0.0

        val buckets = listOf(
            AgingBucket("0-30 Days (Current)", "০-৩০ দিন (বর্তমান)", bucket0To30, pct(bucket0To30)),
            AgingBucket("31-60 Days", "৩১-৬০ দিন", bucket31To60, pct(bucket31To60)),
            AgingBucket("61-90 Days", "৬১-৯০ দিন", bucket61To90, pct(bucket61To90)),
            AgingBucket("90+ Days (Overdue)", "৯০+ দিন (অতিরিক্ত বকেয়া)", bucket90Plus, pct(bucket90Plus))
        )

        return AgingReport("RECEIVABLES", total, buckets)
    }

    override suspend fun getPayablesAging(): AgingReport {
        val pos = purchaseOrderDao.getAllOrdersSync().filter { 
            val due = it.totalBdtAmountMinor - it.paidBdtAmountMinor
            due > 0L && it.status != "CANCELLED"
        }
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        var bucket0To30 = 0L
        var bucket31To60 = 0L
        var bucket61To90 = 0L
        var bucket90Plus = 0L
        val total = pos.sumOf { it.totalBdtAmountMinor - it.paidBdtAmountMinor }

        for (po in pos) {
            val due = po.totalBdtAmountMinor - po.paidBdtAmountMinor
            val ageDays = (now - po.orderDate) / dayMs
            when {
                ageDays <= 30 -> bucket0To30 += due
                ageDays <= 60 -> bucket31To60 += due
                ageDays <= 90 -> bucket61To90 += due
                else -> bucket90Plus += due
            }
        }

        fun pct(amt: Long) = if (total > 0L) (amt.toDouble() / total.toDouble()) * 100.0 else 0.0

        val buckets = listOf(
            AgingBucket("0-30 Days (Current)", "০-৩০ দিন (বর্তমান)", bucket0To30, pct(bucket0To30)),
            AgingBucket("31-60 Days", "৩১-৬০ দিন", bucket31To60, pct(bucket31To60)),
            AgingBucket("61-90 Days", "৬১-৯০ দিন", bucket61To90, pct(bucket61To90)),
            AgingBucket("90+ Days (Overdue)", "৯০+ দিন (বিলম্বিত প্রদেয়)", bucket90Plus, pct(bucket90Plus))
        )

        return AgingReport("PAYABLES", total, buckets)
    }

    // 5. Audit Logging
    override fun getAllAuditLogs(): Flow<List<AuditLog>> = auditLogDao.getAllAuditLogs()

    override suspend fun logAudit(
        recordType: String,
        recordId: String,
        action: String,
        details: String
    ) {
        val log = AuditLog(
            id = "audit-${UUID.randomUUID()}",
            recordType = recordType,
            recordId = recordId,
            action = action,
            details = details,
            operator = "SYSTEM_ADMIN",
            timestamp = System.currentTimeMillis()
        )
        auditLogDao.insertAuditLog(log)
    }
}
