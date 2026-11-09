package com.pocketdev.advance_erp.feature.accounts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.AccountTransfer
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.BankReconciliation
import com.pocketdev.advance_erp.data.entity.BankReconciliationItem
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

enum class AccountsTab(val titleEn: String, val titleBn: String, val icon: ImageVector) {
    CASH_BANK("Cash & Bank", "নগদ ও ব্যাংক", Icons.Default.AccountBalance),
    EXPENSES("Expenses", "ব্যয় ব্যবস্থাপনা", Icons.Default.ReceiptLong),
    STATEMENTS("Financial Statements", "আর্থিক বিবরণী", Icons.Default.Assessment),
    RECONCILIATION("Bank Reconcile", "ব্যাংক সমন্বয়", Icons.Default.CheckCircle),
    CHART_OF_ACCOUNTS("Chart of Accounts", "হিসাব তালিকা", Icons.Default.FormatListNumbered),
    JOURNAL("Journal Entries", "জাবেদা দাখিলা", Icons.Default.MenuBook),
    TRIAL_BALANCE("Trial Balance", "রেওয়ামিল", Icons.Default.Equalizer),
    BACKUP_RESTORE("Backup & Safety", "ব্যাকআপ ও নিরাপত্তা", Icons.Default.Security)
}

@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel(),
    initialTab: AccountsTab = AccountsTab.CASH_BANK
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    // Dialog state
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var showNewJournalDialog by remember { mutableStateOf(false) }
    var showRecordExpenseDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showReconciliationDialog by remember { mutableStateOf(false) }
    var showCreateBackupDialog by remember { mutableStateOf(false) }

    var entryToReverse by remember { mutableStateOf<JournalEntry?>(null) }
    var expenseToReverse by remember { mutableStateOf<ExpenseVoucher?>(null) }
    var backupToRestore by remember { mutableStateOf<BackupRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isLoading && uiState.accounts.isEmpty()) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ERPBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Module Overview Header
            ERPCard(
                backgroundColor = DeepNavy,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(SecondaryTeal, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.accounts_ledger),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Phase 04 Complete Financial Engine & Local Backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Room DB v4",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(AccountsTab.entries) { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryPurple else Color.White)
                            .clickable {
                                selectedTab = tab
                                if (tab == AccountsTab.TRIAL_BALANCE) {
                                    viewModel.refreshTrialBalance()
                                } else if (tab == AccountsTab.STATEMENTS) {
                                    viewModel.loadFinancialStatements()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.language == AppLanguage.BANGLA) tab.titleBn else tab.titleEn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // ==========================================
                // TAB 1: CASH & BANK
                // ==========================================
                if (selectedTab == AccountsTab.CASH_BANK) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.cash_and_bank),
                                subtitle = "Cash Vault, Bank Books & Funds Transfers"
                            )
                            Button(
                                onClick = { showTransferDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer Funds", fontSize = 12.sp)
                            }
                        }
                    }

                    // Account Balances
                    item {
                        AccountBalanceCard(
                            accountName = "City Bank Corporate (Import LC)",
                            accountNumber = "coa-1020",
                            balanceMinor = uiState.trialBalance.find { it.accountId == "coa-1020" }?.netDebitMinor ?: 0L,
                            accountType = "Primary Import LC Bank",
                            color = SecondaryTeal,
                            language = uiState.language
                        )
                    }
                    item {
                        AccountBalanceCard(
                            accountName = "Islami Bank Bangladesh Ltd.",
                            accountNumber = "coa-1030",
                            balanceMinor = uiState.trialBalance.find { it.accountId == "coa-1030" }?.netDebitMinor ?: 0L,
                            accountType = "Corporate Reserve A/C",
                            color = PrimaryPurple,
                            language = uiState.language
                        )
                    }
                    item {
                        AccountBalanceCard(
                            accountName = "Cash in Hand & Tejgaon Vault",
                            accountNumber = "coa-1010",
                            balanceMinor = uiState.trialBalance.find { it.accountId == "coa-1010" }?.netDebitMinor ?: 0L,
                            accountType = "Liquid Petty Cash",
                            color = DeepNavy,
                            language = uiState.language
                        )
                    }

                    // Cash & Bank Book Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = "Cash & Bank Book",
                                subtitle = "Running Ledger Balance with Receipts & Payments"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.exportFinancialCsv(context, "CASH_BOOK") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cash CSV", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.exportFinancialCsv(context, "BANK_BOOK") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bank CSV", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (uiState.cashBookEntries.isEmpty() && uiState.bankBookEntries.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No cash/bank book transactions in the current period.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.cashBookEntries.take(15)) { entry ->
                            ERPCard(contentPadding = 10.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${entry.referenceNumber} • ${entry.description}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatDate(entry.date, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (entry.receiptMinor > 0L) {
                                            Text("+ ${Formatters.formatMoney(entry.receiptMinor, CurrencyCode.BDT, uiState.language)}", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (entry.paymentMinor > 0L) {
                                            Text("- ${Formatters.formatMoney(entry.paymentMinor, CurrencyCode.BDT, uiState.language)}", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text("Bal: ${Formatters.formatMoney(entry.runningBalanceMinor, CurrencyCode.BDT, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: EXPENSES
                // ==========================================
                if (selectedTab == AccountsTab.EXPENSES) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = "Expense Management",
                                subtitle = "Vouchers, Categories & Automated Double-Entry"
                            )
                            Button(
                                onClick = { showRecordExpenseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Expense", fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.expenses.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No expenses recorded yet. Click 'New Expense' to record office rent, travel, salaries, customs or admin vouchers.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.expenses) { exp ->
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(exp.voucherNumber, fontWeight = FontWeight.Bold, color = DeepNavy)
                                                if (exp.isReversed) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("VOID/REVERSED", color = StatusError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(exp.descriptionEn, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text("Category: ${exp.category} • ${Formatters.formatDate(exp.voucherDate, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                Formatters.formatMoney(exp.bdtAmountMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE11D48)
                                            )
                                            if (exp.currency != "BDT") {
                                                Text("${exp.currency} ${exp.amountMinor / 100.0} @ ${exp.exchangeRateToBdt}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                            if (!exp.isReversed) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { expenseToReverse = exp },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Reverse", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 3: FINANCIAL STATEMENTS
                // ==========================================
                if (selectedTab == AccountsTab.STATEMENTS) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = "Financial Statements",
                                subtitle = "P&L, Balance Sheet, Cash Flow & Aging Reports"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.exportProfitAndLossPdf(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("P&L PDF", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.exportBalanceSheetPdf(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Balance PDF", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Profit & Loss Card
                    uiState.profitAndLoss?.let { pnl ->
                        item {
                            ERPCard(backgroundColor = Color(0xFFF8FAFC), contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Profit & Loss Summary (Income Statement)", fontWeight = FontWeight.Bold, color = DeepNavy)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gross Sales Revenue", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(pnl.grossSalesMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Sales Revenue", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(pnl.netSalesRevenueMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cost of Goods Sold (COGS)", style = MaterialTheme.typography.bodySmall)
                                        Text("- ${Formatters.formatMoney(pnl.costOfGoodsSoldMinor, CurrencyCode.BDT, uiState.language)}", color = Color(0xFFE11D48))
                                    }
                                    Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gross Profit", fontWeight = FontWeight.Bold)
                                        Text(Formatters.formatMoney(pnl.grossProfitMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Black, color = SecondaryTeal)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Operating Expenses", style = MaterialTheme.typography.bodySmall)
                                        Text("- ${Formatters.formatMoney(pnl.totalOperatingExpensesMinor, CurrencyCode.BDT, uiState.language)}", color = Color(0xFFE11D48))
                                    }
                                    Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("NET PROFIT / (LOSS)", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                                        val netProfitColor = if (pnl.netProfitMinor >= 0) StatusSuccess else StatusError
                                        Text(Formatters.formatMoney(pnl.netProfitMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Black, color = netProfitColor, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Balance Sheet Card
                    uiState.balanceSheet?.let { bs ->
                        item {
                            ERPCard(backgroundColor = Color(0xFFF8FAFC), contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Balance Sheet (Financial Position)", fontWeight = FontWeight.Bold, color = DeepNavy)
                                        Box(
                                            modifier = Modifier
                                                .background(if (bs.isBalanced) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (bs.isBalanced) "Assets = Liab + Equity ✓" else "Discrepancy ✕", color = if (bs.isBalanced) StatusSuccess else StatusError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Assets", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalAssetsMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Liabilities", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalLiabilitiesMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Equity (Capital + Retained)", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(bs.totalEquityMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                    }
                                }
                            }
                        }
                    }

                    // Receivables & Payables Aging Card
                    item {
                        ERPCard(contentPadding = 14.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Working Capital Aging Breakdown", fontWeight = FontWeight.Bold, color = DeepNavy)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Customer Receivables Aging:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                uiState.receivablesAging?.buckets?.forEach { b ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(b.labelEn, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(b.amountMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Supplier Payables Aging:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                uiState.payablesAging?.buckets?.forEach { b ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(b.labelEn, style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.formatMoney(b.amountMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 4: BANK RECONCILIATION
                // ==========================================
                if (selectedTab == AccountsTab.RECONCILIATION) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = "Bank Reconciliation",
                                subtitle = "Statement Matching & Discrepancy Settlement"
                            )
                            Button(
                                onClick = { showReconciliationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Reconciliation", fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.reconciliations.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No reconciliation statements created yet. Start a reconciliation to match bank statement transactions against ERP journal records.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.reconciliations) { rec ->
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(rec.reconciliationNumber, fontWeight = FontWeight.Bold, color = DeepNavy)
                                            Text("Bank A/C: ${rec.bankAccountId} • ${rec.status}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text("Period: ${Formatters.formatDate(rec.statementStartDate, uiState.language)} - ${Formatters.formatDate(rec.statementEndDate, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            val isZero = rec.discrepancyMinor == 0L
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isZero) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isZero) "RECONCILED" else "DISCREPANCY", color = if (isZero) StatusSuccess else StatusError, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Text("ERP Bal: ${Formatters.formatMoney(rec.erpClosingBalanceMinor, CurrencyCode.BDT, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 5: CHART OF ACCOUNTS
                // ==========================================
                if (selectedTab == AccountsTab.CHART_OF_ACCOUNTS) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.chart_of_accounts),
                                subtitle = "Asset, Liability, Equity, Revenue & Expense"
                            )
                            Button(
                                onClick = { showNewAccountDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New A/C", fontSize = 12.sp)
                            }
                        }
                    }

                    items(uiState.accounts) { acc ->
                        ERPCard(contentPadding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(acc.accountCode, fontWeight = FontWeight.Black, color = DeepNavy, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(acc.nameEn, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(acc.nameBn, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val typeColor = when (acc.accountType) {
                                        "ASSET" -> StatusSuccess
                                        "LIABILITY" -> PrimaryPurple
                                        "EQUITY" -> DeepNavy
                                        "REVENUE" -> SecondaryTeal
                                        else -> Color(0xFFE11D48)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(acc.accountType, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Text("Normal: ${acc.normalBalance}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 6: JOURNAL ENTRIES
                // ==========================================
                if (selectedTab == AccountsTab.JOURNAL) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.journal_entries),
                                subtitle = "Audit Log • Total Debit = Total Credit"
                            )
                            Button(
                                onClick = { showNewJournalDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Entry", fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.journalEntries.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No journal entries posted yet. Supplier payments and warehouse receipts auto-post entries here.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.journalEntries) { entry ->
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(entry.entryNumber, fontWeight = FontWeight.Bold, color = DeepNavy)
                                                if (entry.isReversed) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("REVERSED", color = StatusError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(entry.descriptionEn, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text("Ref: ${entry.referenceType} • ${Formatters.formatDate(entry.entryDate, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                Formatters.formatMoney(entry.totalDebitMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                            if (!entry.isReversed && entry.referenceType != "REVERSAL") {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { entryToReverse = entry },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Reverse", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 7: TRIAL BALANCE
                // ==========================================
                if (selectedTab == AccountsTab.TRIAL_BALANCE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.trial_balance),
                                subtitle = "General Ledger Debit-Credit Reconciliation"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (uiState.isBalanced) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.isBalanced) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (uiState.isBalanced) StatusSuccess else StatusError,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.isBalanced) "Balanced" else "Out of Balance",
                                            color = if (uiState.isBalanced) StatusSuccess else StatusError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.exportFinancialCsv(context, "TRIAL_BALANCE") }) {
                                    Icon(Icons.Default.Download, contentDescription = "CSV", tint = PrimaryPurple)
                                }
                            }
                        }
                    }

                    // Trial Balance Summary Card
                    item {
                        ERPCard(backgroundColor = DeepNavy, contentPadding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Debit", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        Formatters.formatMoney(uiState.totalTrialDebit, CurrencyCode.BDT, uiState.language),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Credit", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        Formatters.formatMoney(uiState.totalTrialCredit, CurrencyCode.BDT, uiState.language),
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.trialBalance) { tb ->
                        if (tb.totalDebitMinor > 0L || tb.totalCreditMinor > 0L) {
                            ERPCard(contentPadding = 10.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${tb.accountCode} - ${tb.accountNameEn}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(tb.accountType, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Debit", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                if (tb.netDebitMinor > 0L) Formatters.formatMoney(tb.netDebitMinor, CurrencyCode.BDT, uiState.language) else "-",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DeepNavy
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Credit", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                if (tb.netCreditMinor > 0L) Formatters.formatMoney(tb.netCreditMinor, CurrencyCode.BDT, uiState.language) else "-",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SecondaryTeal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 8: BACKUP & RESTORE
                // ==========================================
                if (selectedTab == AccountsTab.BACKUP_RESTORE) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                modifier = Modifier.weight(1f),
                                title = "Backup & Safety System",
                                subtitle = "Offline Database Archive & Safety Snapshots"
                            )
                            Button(
                                onClick = { showCreateBackupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Backup", fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.backupRecords.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No offline backup records found. Create an instant backup snapshot to preserve SQLite database state with SHA-256 integrity.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.backupRecords) { bak ->
                            ERPCard(contentPadding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bak.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("Size: ${bak.fileSizeBytes / 1024} KB • Schema v${bak.schemaVersion} • SHA: ${bak.checksumSha256.take(8)}...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(Formatters.formatDate(bak.createdAt, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = { backupToRestore = bak },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Audit Logs Header & List
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        ERPSectionHeader(
                            title = "Immutable Audit Trail",
                            subtitle = "Financial Operations Log"
                        )
                    }

                    items(uiState.auditLogs.take(20)) { log ->
                        ERPCard(contentPadding = 10.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("[${log.recordType}] ${log.details}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("${log.operator} • ${Formatters.formatDate(log.timestamp, uiState.language)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(log.action, color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp)
        )
    }

    // Confirmation dialog for Journal Reversal
    entryToReverse?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToReverse = null },
            title = { Text("Confirm Journal Reversal", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to reverse journal entry ${entry.entryNumber}?\n\n" +
                    "A new balancing reversal entry will be posted, swapping debits and credits, preserving the complete immutable audit trail."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reverseJournalEntry(entry.id)
                        entryToReverse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Confirm Reversal")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToReverse = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for Expense Reversal
    expenseToReverse?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToReverse = null },
            title = { Text("Confirm Expense Voucher Void", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to reverse expense voucher ${exp.voucherNumber}?\n\n" +
                    "The offsetting journal entry will be posted automatically to balance the accounts."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reverseExpense(exp.id, "User voided voucher")
                        expenseToReverse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Void Voucher")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToReverse = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for Backup Restore
    backupToRestore?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("Confirm Database Restore", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to restore database from ${bak.fileName}?\n\n" +
                    "A safety snapshot of your current database will be saved before restoring. System will overwrite active database tables with this backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(File(bak.filePath))
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Record Expense Dialog
    if (showRecordExpenseDialog) {
        RecordExpenseDialog(
            accounts = uiState.accounts,
            onDismiss = { showRecordExpenseDialog = false },
            onConfirm = { cat, descEn, descBn, amtMinor, curr, rate, payAcc, expAcc, date, ref, notes ->
                viewModel.recordExpense(cat, descEn, descBn, amtMinor, curr, rate, payAcc, expAcc, date, ref, notes)
            }
        )
    }

    // Transfer Funds Dialog
    if (showTransferDialog) {
        AccountTransferDialog(
            accounts = uiState.accounts.filter { it.accountType == "ASSET" },
            onDismiss = { showTransferDialog = false },
            onConfirm = { src, dst, amt, date, ref, notes ->
                viewModel.transferFunds(src, dst, amt, date, ref, notes)
            }
        )
    }

    // Start Reconciliation Dialog
    if (showReconciliationDialog) {
        StartReconciliationDialog(
            bankAccounts = uiState.accounts.filter { it.accountCode in listOf("1020", "1030") },
            onDismiss = { showReconciliationDialog = false },
            onConfirm = { accId, start, end, openBal, closeBal, notes ->
                viewModel.startReconciliation(accId, start, end, openBal, closeBal, notes)
            }
        )
    }

    // Create Backup Dialog
    if (showCreateBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showCreateBackupDialog = false },
            onConfirm = { notes ->
                viewModel.createBackup(notes)
            }
        )
    }

    // New Journal Entry Dialog
    if (showNewJournalDialog) {
        NewJournalEntryDialog(
            accounts = uiState.accounts,
            onDismiss = { showNewJournalDialog = false },
            onConfirm = { descEn, descBn, lines ->
                viewModel.postManualJournalEntry(descEn, descBn, lines)
            }
        )
    }
}

@Composable
private fun AccountBalanceCard(
    accountName: String,
    accountNumber: String,
    balanceMinor: Long,
    accountType: String,
    color: Color,
    language: AppLanguage
) {
    ERPCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "$accountNumber • $accountType", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                text = Formatters.formatMoney(balanceMinor, CurrencyCode.BDT, language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RecordExpenseDialog(
    accounts: List<ChartOfAccount>,
    onDismiss: () -> Unit,
    onConfirm: (
        category: String,
        descEn: String,
        descBn: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: String,
        paymentAccountId: String,
        expenseAccountId: String,
        voucherDate: Long,
        ref: String?,
        notes: String?
    ) -> Unit
) {
    val categories = ExpenseCategory.entries
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var descEn by remember { mutableStateOf("") }
    var descBn by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BDT") }
    var exchangeRate by remember { mutableStateOf("1.0000") }
    var paymentAccountId by remember { mutableStateOf(accounts.find { it.accountCode == "1010" }?.id ?: accounts.firstOrNull()?.id ?: "") }
    var refNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Record Operating Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Expense Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSel = selectedCat == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) PrimaryPurple else Color(0xFFF1F5F9))
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(cat.titleEn, color = if (isSel) Color.White else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description (English)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text("Currency") },
                        modifier = Modifier.width(90.dp)
                    )
                }

                if (currency != "BDT") {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = exchangeRate,
                        onValueChange = { exchangeRate = it },
                        label = { Text("Rate to BDT (e.g. 17.20)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Payment Account:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                val paymentAccounts = accounts.filter { it.accountType in listOf("ASSET", "LIABILITY") }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(paymentAccounts.take(4)) { acc ->
                        val isSel = paymentAccountId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) SecondaryTeal else Color(0xFFF1F5F9))
                                .clickable { paymentAccountId = acc.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(acc.nameEn, color = if (isSel) Color.White else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("Receipt / Invoice Ref") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be positive" }
                                require(descEn.isNotBlank()) { "Description is required" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(
                                    selectedCat.name,
                                    descEn,
                                    descEn,
                                    minor,
                                    currency,
                                    exchangeRate,
                                    paymentAccountId,
                                    selectedCat.defaultAccountId,
                                    System.currentTimeMillis(),
                                    refNumber.ifBlank { null },
                                    null
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid expense details"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Post Expense")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTransferDialog(
    accounts: List<ChartOfAccount>,
    onDismiss: () -> Unit,
    onConfirm: (sourceId: String, destId: String, amountMinor: Long, date: Long, ref: String?, notes: String?) -> Unit
) {
    var sourceId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var destId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr by remember { mutableStateOf("") }
    var refStr by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Inter-Account Transfer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(10.dp))

                Text("From (Source Account):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts) { acc ->
                        val isSel = sourceId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) PrimaryPurple else Color(0xFFF1F5F9))
                                .clickable { sourceId = acc.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(acc.nameEn, color = if (isSel) Color.White else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("To (Destination Account):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts) { acc ->
                        val isSel = destId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) SecondaryTeal else Color(0xFFF1F5F9))
                                .clickable { destId = acc.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(acc.nameEn, color = if (isSel) Color.White else TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transfer Amount (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = refStr,
                    onValueChange = { refStr = it },
                    label = { Text("Reference (Cheque / Deposit Slip)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                require(sourceId != destId) { "Source and destination accounts must be different" }
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be positive" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(sourceId, destId, minor, System.currentTimeMillis(), refStr.ifBlank { null }, null)
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid transfer parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text("Execute Transfer")
                    }
                }
            }
        }
    }
}

@Composable
private fun StartReconciliationDialog(
    bankAccounts: List<ChartOfAccount>,
    onDismiss: () -> Unit,
    onConfirm: (bankAccountId: String, start: Long, end: Long, openBal: Long, closeBal: Long, notes: String?) -> Unit
) {
    var selectedAccId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: "") }
    var openBalStr by remember { mutableStateOf("0.00") }
    var closeBalStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Start Bank Reconciliation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Bank Account:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bankAccounts.forEach { acc ->
                        val isSel = selectedAccId == acc.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) SecondaryTeal.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .clickable { selectedAccId = acc.id }
                                .padding(8.dp)
                        ) {
                            Text(acc.nameEn, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = openBalStr,
                    onValueChange = { openBalStr = it },
                    label = { Text("Statement Opening Balance (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = closeBalStr,
                    onValueChange = { closeBalStr = it },
                    label = { Text("Statement Closing Balance (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val openBd = BigDecimal(openBalStr.trim())
                                val closeBd = BigDecimal(closeBalStr.trim())
                                val openMinor = openBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                val closeMinor = closeBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

                                val now = System.currentTimeMillis()
                                val monthStart = now - (30L * 86400000L)
                                onConfirm(selectedAccId, monthStart, now, openMinor, closeMinor, notes.ifBlank { null })
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid reconciliation balance"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Text("Create Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (notes: String?) -> Unit
) {
    var notes by remember { mutableStateOf("Manual offline archive snapshot") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Create Offline Safety Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This creates an immutable copy of advance_erp.db on local storage, flushes active SQLite WAL journals, and records a SHA-256 integrity checksum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Backup Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(notes)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Archive Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewJournalEntryDialog(
    accounts: List<ChartOfAccount>,
    onDismiss: () -> Unit,
    onConfirm: (descEn: String, descBn: String, lines: List<JournalLineDraft>) -> Unit
) {
    var descEn by remember { mutableStateOf("Manual Adjustment Entry") }
    var debitAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var creditAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr by remember { mutableStateOf("10000.00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("New Double-Entry Journal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepNavy)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Debit Account:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accounts.take(3).forEach { acc ->
                        val isSel = debitAccountId == acc.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .clickable { debitAccountId = acc.id }
                                .padding(8.dp)
                        ) {
                            Text("${acc.accountCode} - ${acc.nameEn}", style = MaterialTheme.typography.bodySmall, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Credit Account:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accounts.drop(1).take(3).forEach { acc ->
                        val isSel = creditAccountId == acc.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) SecondaryTeal.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .clickable { creditAccountId = acc.id }
                                .padding(8.dp)
                        ) {
                            Text("${acc.accountCode} - ${acc.nameEn}", style = MaterialTheme.typography.bodySmall, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be positive" }
                                require(debitAccountId != creditAccountId) { "Debit and Credit accounts must be different" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                val lines = listOf(
                                    JournalLineDraft(accountId = debitAccountId, isDebit = true, amountMinor = minor),
                                    JournalLineDraft(accountId = creditAccountId, isDebit = false, amountMinor = minor)
                                )
                                onConfirm(descEn, descEn, lines)
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid journal parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Post Entry")
                    }
                }
            }
        }
    }
}
