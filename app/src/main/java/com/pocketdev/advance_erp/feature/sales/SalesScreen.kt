package com.pocketdev.advance_erp.feature.sales

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCalendar
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesReturn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    modifier: Modifier = Modifier,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBangla = uiState.language == AppLanguage.BANGLA
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Dialog state
    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var showPosDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var collectDueCustomerId by remember { mutableStateOf<String?>(null) }
    var showProcessReturnDialog by remember { mutableStateOf(false) }
    var showDispatchDeliveryDialog by remember { mutableStateOf(false) }
    var showDirectExchangeDialog by remember { mutableStateOf(false) }
    var showCustomerLedgerDialog by remember { mutableStateOf(false) }
    var ledgerCustomer by remember { mutableStateOf<Customer?>(null) }
    var remainingDeliveryItems by remember { mutableStateOf<List<com.pocketdev.advance_erp.domain.repository.UndeliveredItemInfo>>(emptyList()) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val tabs = listOf(
        Pair(if (isBangla) "বিক্রয় চালান" else "Invoices", Icons.Default.ReceiptLong),
        Pair(if (isBangla) "গ্রাহক ও লেজার" else "Customers", Icons.Default.ContactPage),
        Pair(if (isBangla) "পেমেন্ট ও কালেকশন" else "Payments", Icons.Default.Payments),
        Pair(if (isBangla) "বিক্রয় ফেরত" else "Returns", Icons.Default.Replay),
        Pair(if (isBangla) "রিপোর্ট ও লাভ" else "Reports", Icons.Default.Assessment)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ERPBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FloatingActionButton(
                        onClick = { showPosDialog = true },
                        containerColor = DeepNavy,
                        contentColor = Color.White
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PointOfSale, contentDescription = "POS Mode")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("POS", fontWeight = FontWeight.Bold)
                        }
                    }
                    FloatingActionButton(
                        onClick = { showCreateInvoiceDialog = true },
                        containerColor = StatusSuccess,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Sale")
                    }
                }
            } else if (selectedTabIndex == 2) {
                FloatingActionButton(
                    onClick = {
                        collectDueCustomerId = null
                        viewModel.refreshNextPaymentNumber()
                        showRecordPaymentDialog = true
                    },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Payment, contentDescription = "Record Payment")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DeepNavy,
                contentColor = Color.White,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = SecondaryTeal,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == index) SecondaryTeal else Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.first,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else {
                when (selectedTabIndex) {
                    0 -> InvoicesTab(
                        invoices = uiState.invoices,
                        searchQuery = searchQuery,
                        statusFilter = selectedStatusFilter,
                        isBangla = isBangla,
                        onSearchChange = { searchQuery = it },
                        onStatusFilterChange = { selectedStatusFilter = it },
                        onSelectInvoice = { inv -> viewModel.selectInvoice(inv) }
                    )
                    1 -> CustomersTab(
                        customers = uiState.customers,
                        isBangla = isBangla,
                        onCollectDue = { cust ->
                            viewModel.clearSelectedInvoice()
                            collectDueCustomerId = cust.id
                            viewModel.refreshNextPaymentNumber()
                            showRecordPaymentDialog = true
                        },
                        onViewLedger = { cust ->
                            ledgerCustomer = cust
                            viewModel.loadCustomerLedger(cust.id)
                            showCustomerLedgerDialog = true
                        }
                    )
                    2 -> PaymentsTab(
                        payments = uiState.payments,
                        customers = uiState.customers,
                        isBangla = isBangla,
                        language = uiState.language,
                        onShareReceipt = { pay ->
                            try {
                                val pdf = viewModel.generatePaymentReceiptPdf(context, pay)
                                context.startActivity(InvoicePdfGenerator.createSharePdfIntent(context, pdf))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onVoidPayment = { paymentId, reason ->
                            viewModel.voidCustomerPayment(paymentId, reason)
                        },
                        onRecordPaymentClick = {
                            collectDueCustomerId = null
                            viewModel.refreshNextPaymentNumber()
                            showRecordPaymentDialog = true
                        }
                    )
                    3 -> ReturnsTab(
                        returns = uiState.returns,
                        isBangla = isBangla
                    )
                    4 -> ReportsTab(
                        summary = uiState.salesReport,
                        productReport = uiState.productWiseReport,
                        customerReport = uiState.customerWiseReport,
                        isBangla = isBangla
                    )
                }
            }
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Invoice Detail Dialog
    if (uiState.selectedInvoice != null) {
        InvoiceDetailDialog(
            invoice = uiState.selectedInvoice!!,
            items = uiState.selectedInvoiceItems,
            allocations = uiState.selectedInvoiceAllocations,
            isBangla = isBangla,
            onDismiss = { viewModel.clearSelectedInvoice() },
            onPostInvoice = {
                viewModel.postInvoice(uiState.selectedInvoice!!.id)
            },
            onRecordPayment = {
                showRecordPaymentDialog = true
            },
            onProcessReturn = {
                showProcessReturnDialog = true
            },
            onSharePdf = { ctx ->
                try {
                    val file = viewModel.generateInvoicePdf(ctx, uiState.selectedInvoice!!)
                    ctx.startActivity(InvoicePdfGenerator.createSharePdfIntent(ctx, file))
                } catch (e: Exception) {
                    Toast.makeText(ctx, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onDispatchDelivery = {
                coroutineScope.launch {
                    val rem = viewModel.getRemainingUndeliveredItems(uiState.selectedInvoice!!.id)
                    remainingDeliveryItems = rem
                    showDispatchDeliveryDialog = true
                }
            },
            onDirectExchange = {
                showDirectExchangeDialog = true
            },
            onConvertToSalesOrder = {
                viewModel.convertQuotationToSalesOrder(uiState.selectedInvoice!!.id)
            },
            onConvertToInvoice = {
                viewModel.convertSalesOrderToInvoice(uiState.selectedInvoice!!.id)
            },
            onPrintThermalReceipt = { ctx ->
                try {
                    val file = viewModel.generateThermalReceiptPdf(ctx, uiState.selectedInvoice!!)
                    ctx.startActivity(InvoicePdfGenerator.createSharePdfIntent(ctx, file))
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Receipt error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onCancelOrDeleteInvoice = { reason ->
                viewModel.cancelOrDeleteInvoice(uiState.selectedInvoice!!.id, reason)
            }
        )
    }

    // Create Invoice Dialog
    if (showCreateInvoiceDialog) {
        CreateInvoiceDialog(
            customers = uiState.customers,
            warehouses = uiState.warehouses,
            products = uiState.products,
            variants = uiState.variants,
            stockBalances = uiState.stockBalances,
            customerSummary = uiState.selectedCustomerSummary,
            isBangla = isBangla,
            onDismiss = { showCreateInvoiceDialog = false },
            onCustomerSelected = { custId ->
                viewModel.loadCustomerSummary(custId)
            },
            onSubmitDraft = { draft ->
                viewModel.createInvoice(draft) {
                    showCreateInvoiceDialog = false
                }
            },
            onConfirmSale = { draft ->
                viewModel.createAndConfirmInvoice(draft) {
                    showCreateInvoiceDialog = false
                }
            }
        )
    }

    // Record Payment Dialog
    if (showRecordPaymentDialog) {
        RecordCustomerPaymentDialog(
            customers = uiState.customers,
            invoices = uiState.invoices,
            preselectedInvoice = uiState.selectedInvoice,
            preselectedCustomerId = collectDueCustomerId,
            nextPaymentNumber = uiState.nextPaymentNumber,
            isBangla = isBangla,
            onDismiss = { showRecordPaymentDialog = false; collectDueCustomerId = null },
            onSubmit = { draft ->
                viewModel.recordPayment(draft) {
                    showRecordPaymentDialog = false
                    collectDueCustomerId = null
                }
            }
        )
    }

    // Process Return Dialog
    if (showProcessReturnDialog && uiState.selectedInvoice != null) {
        ProcessSalesReturnDialog(
            invoice = uiState.selectedInvoice!!,
            items = uiState.selectedInvoiceItems,
            isBangla = isBangla,
            onDismiss = { showProcessReturnDialog = false },
            onSubmit = { draft ->
                viewModel.processReturn(draft) {
                    showProcessReturnDialog = false
                }
            }
        )
    }

    // Customer Ledger Dialog
    if (showCustomerLedgerDialog && ledgerCustomer != null) {
        CustomerLedgerDialog(
            customer = ledgerCustomer!!,
            ledger = uiState.selectedCustomerLedger,
            isBangla = isBangla,
            onDismiss = {
                showCustomerLedgerDialog = false
                ledgerCustomer = null
            },
            onShareWhatsAppStatement = { cust ->
                viewModel.openDueCollectionStatementShare(cust)
            },
            onSendDueReminder = { cust ->
                viewModel.openDueReminder(cust)
            }
        )
    }

    // Due Collection Statement & WhatsApp Share Dialog
    if (uiState.activeDueCollectionData != null) {
        DueCollectionStatementShareDialog(
            data = uiState.activeDueCollectionData!!,
            initialIsBangla = isBangla,
            onDismiss = {
                viewModel.dismissDueCollectionDialog()
            }
        )
    }

    // Polite Due Reminder & WhatsApp Share Dialog
    if (uiState.activeDueReminderData != null) {
        DueReminderDialog(
            data = uiState.activeDueReminderData!!,
            initialIsBangla = isBangla,
            onDismiss = {
                viewModel.dismissDueReminderDialog()
            }
        )
    }

    // POS Rapid Terminal Dialog
    if (showPosDialog) {
        PosDialog(
            products = uiState.products,
            variants = uiState.variants,
            warehouses = uiState.warehouses,
            customers = uiState.customers,
            salesViewModel = viewModel,
            onDismiss = { showPosDialog = false }
        )
    }

    // Dispatch Delivery Challan Dialog
    if (showDispatchDeliveryDialog && uiState.selectedInvoice != null) {
        DispatchDeliveryDialog(
            invoice = uiState.selectedInvoice!!,
            remainingItems = remainingDeliveryItems,
            onDismiss = { showDispatchDeliveryDialog = false },
            onDispatch = { draft ->
                viewModel.dispatchDelivery(draft) {
                    showDispatchDeliveryDialog = false
                }
            }
        )
    }

    // Direct Exchange Dialog
    if (showDirectExchangeDialog && uiState.selectedInvoice != null) {
        DirectExchangeDialog(
            invoice = uiState.selectedInvoice!!,
            invoiceItems = uiState.selectedInvoiceItems,
            allProducts = uiState.products,
            allVariants = uiState.variants,
            onDismiss = { showDirectExchangeDialog = false },
            onExchangeConfirmed = { draft ->
                viewModel.processDirectExchange(draft) {
                    showDirectExchangeDialog = false
                }
            }
        )
    }
}

@Composable
private fun InvoicesTab(
    invoices: List<SalesInvoice>,
    searchQuery: String,
    statusFilter: String,
    isBangla: Boolean,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onSelectInvoice: (SalesInvoice) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    val filtered = remember(invoices, searchQuery, statusFilter) {
        invoices.filter { inv ->
            val matchesSearch = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = if (statusFilter == "ALL") true else inv.status == statusFilter
            matchesSearch && matchesStatus
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(if (isBangla) "চালান নং অথবা গ্রাহকের নামে খুঁজুন..." else "Search by Invoice # or Customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        // Status Filter Chips
        item {
            val statusList = listOf("ALL", "DRAFT", "POSTED", "PARTIALLY_PAID", "PAID")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(statusList) { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { onStatusFilterChange(st) },
                        label = { Text(st) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "কোন বিক্রয় চালান পাওয়া যায়নি" else "No Sales Invoices Found",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        items(filtered) { inv ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectInvoice(inv) },
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inv.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val badgeColor = when (inv.status) {
                            "PAID" -> StatusSuccess
                            "POSTED" -> SecondaryTeal
                            "PARTIALLY_PAID" -> Color(0xFFF59E0B)
                            else -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(inv.status, color = badgeColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${inv.customerName} • ${dateFormat.format(Date(inv.invoiceDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DeepNavy.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Bill:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("৳" + Formatters.formatMinorUnit(inv.totalAmountMinor), fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Paid:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("৳" + Formatters.formatMinorUnit(inv.paidAmountMinor), fontWeight = FontWeight.SemiBold, color = StatusSuccess)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance Due:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                "৳" + Formatters.formatMinorUnit(inv.dueAmountMinor),
                                fontWeight = FontWeight.Bold,
                                color = if (inv.dueAmountMinor > 0) Color(0xFFC53030) else StatusSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(
    customers: List<Customer>,
    isBangla: Boolean,
    onCollectDue: (Customer) -> Unit,
    onViewLedger: (Customer) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(customers) { cust ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = cust.name,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Code: ${cust.customerCode} • ${cust.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (cust.outstandingBalanceMinor > 0) {
                            Button(
                                onClick = { onCollectDue(cust) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "বকেয়া আদায়" else "Collect Due",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Button(
                            onClick = { onViewLedger(cust) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBangla) "লেজার দেখুন" else "View Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DeepNavy.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBangla) "ক্রেডিট লিমিট:" else "Credit Limit:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text("৳" + Formatters.formatMinorUnit(cust.creditLimitMinor), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBangla) "মোট বকেয়া:" else "Outstanding Receivable:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                "৳" + Formatters.formatMinorUnit(cust.outstandingBalanceMinor),
                                fontWeight = FontWeight.Bold,
                                color = if (cust.outstandingBalanceMinor > 0) Color(0xFFC53030) else StatusSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class PaymentDateFilter {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM_DATE
}

fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun PaymentsTab(
    payments: List<CustomerPayment>,
    customers: List<Customer>,
    isBangla: Boolean,
    language: AppLanguage = AppLanguage.BANGLA,
    onShareReceipt: (CustomerPayment) -> Unit,
    onVoidPayment: (paymentId: String, reason: String) -> Unit = { _, _ -> },
    onRecordPaymentClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("ALL") }
    var selectedDateFilter by remember { mutableStateOf(PaymentDateFilter.ALL) }
    var selectedCustomDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    var paymentToVoid by remember { mutableStateOf<CustomerPayment?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val customersMap = remember(customers) { customers.associateBy { it.id } }

    val filteredPayments = remember(
        payments,
        searchQuery,
        selectedMethodFilter,
        selectedDateFilter,
        selectedCustomDateMillis,
        customersMap
    ) {
        payments.filter { pay ->
            val cust = customersMap[pay.customerId]
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            val matchesSearch = if (q.isBlank()) {
                true
            } else {
                val matchesNumber = pay.paymentNumber.lowercase(Locale.ROOT).contains(q)
                val matchesCustName = pay.customerName.lowercase(Locale.ROOT).contains(q)
                val matchesPhone = cust?.phone?.lowercase(Locale.ROOT)?.contains(q) == true
                val matchesRef = pay.referenceNote?.lowercase(Locale.ROOT)?.contains(q) == true

                val payDate = Date(pay.paymentDate)
                val d1 = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(payDate)
                val d2 = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(payDate)
                val d3 = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(payDate)
                val d4 = SimpleDateFormat("dd MMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val d5 = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val d6 = SimpleDateFormat("MMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val dBnDigits1 = Formatters.toBanglaDigits(d1)
                val dBnDigits2 = Formatters.toBanglaDigits(d2)
                val dBnDigits3 = Formatters.toBanglaDigits(d3)
                val matchesDateText = d1.contains(q) || d2.contains(q) || d3.contains(q) ||
                                      d4.contains(q) || d5.contains(q) || d6.contains(q) ||
                                      dBnDigits1.contains(q) || dBnDigits2.contains(q) || dBnDigits3.contains(q)

                matchesNumber || matchesCustName || matchesPhone || matchesRef || matchesDateText
            }

            val matchesMethod = when (selectedMethodFilter) {
                "ALL" -> true
                "VOIDED" -> pay.isVoided
                "MFS" -> !pay.isVoided && (pay.paymentMethod.equals("BKASH", ignoreCase = true) || pay.paymentMethod.equals("NAGAD", ignoreCase = true))
                else -> !pay.isVoided && pay.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
            }

            val matchesDate = when (selectedDateFilter) {
                PaymentDateFilter.ALL -> true
                PaymentDateFilter.TODAY -> isSameCalendarDay(pay.paymentDate, System.currentTimeMillis())
                PaymentDateFilter.YESTERDAY -> {
                    val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                    isSameCalendarDay(pay.paymentDate, yest)
                }
                PaymentDateFilter.THIS_WEEK -> {
                    val weekStart = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -6)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val nowEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    pay.paymentDate in weekStart..nowEnd
                }
                PaymentDateFilter.THIS_MONTH -> {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val monthEnd = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    pay.paymentDate in monthStart..monthEnd
                }
                PaymentDateFilter.CUSTOM_DATE -> isSameCalendarDay(pay.paymentDate, selectedCustomDateMillis)
            }

            matchesSearch && matchesMethod && matchesDate
        }
    }

    val totalCollectedMinor = remember(filteredPayments) {
        filteredPayments.filter { !it.isVoided }.sumOf { it.amountMinor }
    }

    val activeDateLabel = remember(selectedDateFilter, selectedCustomDateMillis, isBangla) {
        val targetMillis = when (selectedDateFilter) {
            PaymentDateFilter.TODAY -> System.currentTimeMillis()
            PaymentDateFilter.YESTERDAY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
            PaymentDateFilter.CUSTOM_DATE -> selectedCustomDateMillis
            else -> selectedCustomDateMillis
        }
        val prefix = when (selectedDateFilter) {
            PaymentDateFilter.TODAY -> if (isBangla) "আজ, " else "Today, "
            PaymentDateFilter.YESTERDAY -> if (isBangla) "গতকাল, " else "Yesterday, "
            else -> ""
        }
        val formatted = if (isBangla) {
            val bnDate = SimpleDateFormat("dd MMM yyyy", Locale("bn", "BD")).format(Date(targetMillis))
            Formatters.toBanglaDigits(bnDate)
        } else {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(targetMillis))
        }
        "$prefix$formatted"
    }

    val activeDateSummaryBadge = remember(selectedDateFilter, selectedCustomDateMillis, isBangla) {
        when (selectedDateFilter) {
            PaymentDateFilter.ALL -> ""
            PaymentDateFilter.TODAY -> if (isBangla) " (আজ)" else " (Today)"
            PaymentDateFilter.YESTERDAY -> if (isBangla) " (গতকাল)" else " (Yesterday)"
            PaymentDateFilter.THIS_WEEK -> if (isBangla) " (এই সপ্তাহ)" else " (This Week)"
            PaymentDateFilter.THIS_MONTH -> if (isBangla) " (এই মাস)" else " (This Month)"
            PaymentDateFilter.CUSTOM_DATE -> {
                val formatted = if (isBangla) {
                    Formatters.toBanglaDigits(SimpleDateFormat("dd MMM", Locale.US).format(Date(selectedCustomDateMillis)))
                } else {
                    SimpleDateFormat("dd MMM", Locale.US).format(Date(selectedCustomDateMillis))
                }
                " ($formatted)"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Search Bar for Payments with Date Filter Icon
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (isBangla) "রশিদ নং, নাম, ফোন বা তারিখ দিয়ে খুঁজুন..."
                        else "Search by Receipt #, Customer, Phone, or Date..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = { showCalendarPicker = !showCalendarPicker }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Date Filter",
                                tint = if (selectedDateFilter != PaymentDateFilter.ALL || showCalendarPicker) PrimaryPurple else TextMuted
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }

        // Date Preset Filter Chips
        item {
            val dateFilterOptions = listOf(
                PaymentDateFilter.ALL to (if (isBangla) "সব সময়" else "All Time"),
                PaymentDateFilter.TODAY to (if (isBangla) "আজ" else "Today"),
                PaymentDateFilter.YESTERDAY to (if (isBangla) "গতকাল" else "Yesterday"),
                PaymentDateFilter.THIS_WEEK to (if (isBangla) "এই সপ্তাহ" else "This Week"),
                PaymentDateFilter.THIS_MONTH to (if (isBangla) "এই মাস" else "This Month"),
                PaymentDateFilter.CUSTOM_DATE to (
                    if (selectedDateFilter == PaymentDateFilter.CUSTOM_DATE) {
                        "📅 " + (if (isBangla) Formatters.toBanglaDigits(SimpleDateFormat("dd MMM", Locale.US).format(Date(selectedCustomDateMillis)))
                                 else SimpleDateFormat("dd MMM", Locale.US).format(Date(selectedCustomDateMillis)))
                    } else {
                        if (isBangla) "📅 নির্দিষ্ট তারিখ" else "📅 Pick Date"
                    }
                )
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dateFilterOptions) { (filter, label) ->
                    val isSelected = selectedDateFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (filter == PaymentDateFilter.CUSTOM_DATE) {
                                selectedDateFilter = PaymentDateFilter.CUSTOM_DATE
                                showCalendarPicker = !showCalendarPicker
                            } else {
                                selectedDateFilter = filter
                                showCalendarPicker = false
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryPurple else DeepNavy.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Day Navigation Strip for specific date, today, or yesterday
        if (selectedDateFilter == PaymentDateFilter.CUSTOM_DATE ||
            selectedDateFilter == PaymentDateFilter.TODAY ||
            selectedDateFilter == PaymentDateFilter.YESTERDAY) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryPurple.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val base = when (selectedDateFilter) {
                                        PaymentDateFilter.CUSTOM_DATE -> selectedCustomDateMillis
                                        PaymentDateFilter.YESTERDAY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                                        else -> System.currentTimeMillis()
                                    }
                                    val prev = Calendar.getInstance().apply {
                                        timeInMillis = base
                                        add(Calendar.DAY_OF_YEAR, -1)
                                    }.timeInMillis
                                    selectedCustomDateMillis = prev
                                    selectedDateFilter = PaymentDateFilter.CUSTOM_DATE
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Day",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showCalendarPicker = !showCalendarPicker }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeDateLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }

                            IconButton(
                                onClick = {
                                    val base = when (selectedDateFilter) {
                                        PaymentDateFilter.CUSTOM_DATE -> selectedCustomDateMillis
                                        PaymentDateFilter.YESTERDAY -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                                        else -> System.currentTimeMillis()
                                    }
                                    val next = Calendar.getInstance().apply {
                                        timeInMillis = base
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }.timeInMillis
                                    selectedCustomDateMillis = next
                                    selectedDateFilter = PaymentDateFilter.CUSTOM_DATE
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Day",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                selectedDateFilter = PaymentDateFilter.ALL
                                showCalendarPicker = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBangla) "তারিখ মুছুন" else "Clear Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Inline Calendar Card when showCalendarPicker is toggled
        if (showCalendarPicker) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "নির্দিষ্ট তারিখ নির্বাচন করুন" else "Select Specific Date",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                            }
                            IconButton(
                                onClick = { showCalendarPicker = false },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Calendar",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DeepNavy.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))
                        ERPCalendar(
                            language = language,
                            selectedTimestamp = selectedCustomDateMillis,
                            onDateSelected = { millis ->
                                selectedCustomDateMillis = millis
                                selectedDateFilter = PaymentDateFilter.CUSTOM_DATE
                                showCalendarPicker = false
                            }
                        )
                    }
                }
            }
        }

        // Method Filter Chips
        item {
            val filterOptions = listOf(
                "ALL" to if (isBangla) "সকল (${payments.size})" else "All (${payments.size})",
                "CASH" to if (isBangla) "ক্যাশ" else "Cash",
                "BANK" to if (isBangla) "ব্যাংক" else "Bank",
                "MFS" to "bKash/Nagad",
                "CHEQUE" to if (isBangla) "চেক" else "Cheque",
                "VOIDED" to if (isBangla) "বাতিলকৃত" else "Voided"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedMethodFilter == key,
                        onClick = { selectedMethodFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedMethodFilter == key,
                            borderColor = if (selectedMethodFilter == key) PrimaryPurple else DeepNavy.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Summary Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "মোট রসিদ: ${filteredPayments.size} টি$activeDateSummaryBadge"
                           else "Total Receipts: ${filteredPayments.size}$activeDateSummaryBadge",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (isBangla) "মোট আদায়: " else "Total Collected: "}৳" + Formatters.formatMinorUnit(totalCollectedMinor),
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (filteredPayments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val emptyText = if (searchQuery.isNotBlank()) {
                            if (isBangla) "\"$searchQuery\" এর জন্য কোনো পেমেন্ট রশিদ পাওয়া যায়নি"
                            else "No payment receipts found matching \"$searchQuery\""
                        } else if (selectedDateFilter != PaymentDateFilter.ALL) {
                            if (isBangla) "নির্বাচিত তারিখে কোনো পেমেন্ট রশিদ পাওয়া যায়নি"
                            else "No payment receipts found for the selected date"
                        } else {
                            if (isBangla) "কোন পেমেন্ট রশিদ পাওয়া যায়নি" else "No Payment Receipts Found"
                        }
                        Text(
                            text = emptyText,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || selectedMethodFilter != "ALL" || selectedDateFilter != PaymentDateFilter.ALL) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedMethodFilter = "ALL"
                                selectedDateFilter = PaymentDateFilter.ALL
                                showCalendarPicker = false
                            }) {
                                Text(if (isBangla) "সকল ফিল্টার রিসেট করুন" else "Reset All Filters", color = PrimaryPurple)
                            }
                        }
                    }
                }
            }
        }

        items(filteredPayments, key = { it.id }) { pay ->
            val cust = customersMap[pay.customerId]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryPurple.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = pay.paymentNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (pay.isAdvance) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SecondaryTeal.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "অগ্রিম" else "ADVANCE",
                                        color = SecondaryTeal,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (pay.isVoided) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(StatusError.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "বাতিল" else "VOID",
                                        color = StatusError,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pay.customerName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (!cust?.phone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${cust!!.phone})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "${pay.paymentMethod} • ${dateFormat.format(Date(pay.paymentDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        if (!pay.referenceNote.isNullOrBlank()) {
                            Text(
                                text = "${if (isBangla) "রেফারেন্স: " else "Ref: "}${pay.referenceNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (pay.isVoided && !pay.voidReason.isNullOrBlank()) {
                            Text(
                                text = "${if (isBangla) "বাতিলের কারণ: " else "Void Reason: "}${pay.voidReason}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "৳" + Formatters.formatMinorUnit(pay.amountMinor),
                            fontWeight = FontWeight.Bold,
                            color = if (pay.isVoided) TextMuted else StatusSuccess,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onShareReceipt(pay) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Receipt PDF", tint = PrimaryPurple)
                            }
                            if (!pay.isVoided) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { paymentToVoid = pay; voidReason = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = "Void Payment", tint = StatusError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (paymentToVoid != null) {
        AlertDialog(
            onDismissRequest = { paymentToVoid = null },
            title = {
                Text(
                    text = if (isBangla) "পেমেন্ট বাতিল (Void) করুন" else "Void Payment Receipt",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla)
                            "⚠️ সতর্কতা: #${paymentToVoid!!.paymentNumber} পেমেন্টটি বাতিল করলে:\n• ইনভয়েসে সমন্বয়কৃত বকেয়া পুনরায় বৃদ্ধি পাবে।\n• ক্যাশ/ব্যাংক এবং সাধারণ জাবেদা রিভার্স হবে।\n• গ্রাহকের বকেয়া ব্যালেন্স পুনঃগণনা করা হবে।\n\nবাতিলের কারণ লিখুন:"
                        else
                            "⚠️ Warning: Voiding #${paymentToVoid!!.paymentNumber} will:\n• Reverse invoice payment allocations & restore dues.\n• Reverse Cash/Bank debit & AR credit in General Ledger.\n• Recalculate customer outstanding balance.\n\nPlease provide a cancellation reason:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(if (isBangla) "বাতিলের কারণ" else "Cancellation Reason") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pId = paymentToVoid!!.id
                        val r = voidReason.trim()
                        if (r.isBlank()) return@TextButton
                        paymentToVoid = null
                        onVoidPayment(pId, r)
                    },
                    enabled = voidReason.isNotBlank()
                ) {
                    Text(if (isBangla) "হ্যাঁ, বাতিল করুন" else "Confirm Void", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToVoid = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ReturnsTab(
    returns: List<SalesReturn>,
    isBangla: Boolean
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (returns.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text(if (isBangla) "কোন বিক্রয় ফেরত রেকর্ড পাওয়া যায়নি" else "No Sales Return Records", color = TextMuted)
                }
            }
        }

        items(returns) { ret ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(ret.returnNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Inv #${ret.invoiceNumber} • ${ret.customerName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text(
                            text = "৳" + Formatters.formatMinorUnit(ret.totalReturnAmountMinor),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC53030),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Refund Mode: ${ret.refundOption}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = dateFormat.format(Date(ret.returnDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (!ret.reason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Reason: ${ret.reason}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    summary: com.pocketdev.advance_erp.domain.repository.SalesReportSummary?,
    productReport: List<com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem>,
    customerReport: List<com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem>,
    isBangla: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Summary KPI Banner
        if (summary != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DeepNavy
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "গত ৩০ দিনের বিক্রয় ও লাভ বিশ্লেষণ" else "30-Day Sales & Profit Performance",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.15f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Gross Revenue:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("৳" + Formatters.formatMinorUnit(summary.totalGrossSalesMinor), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("COGS (Total Cost):", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text("৳" + Formatters.formatMinorUnit(summary.totalCogsMinor), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gross Profit:", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                Text("৳" + Formatters.formatMinorUnit(summary.totalGrossProfitMinor), color = StatusSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Profit Margin:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(String.format(Locale.US, "%.1f%%", summary.grossProfitMarginPercent), color = SecondaryTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Product-wise Performance
        item {
            Text(
                text = if (isBangla) "পণ্যভিত্তিক বিক্রয় ও মোট লাভ" else "Product-wise Revenue & Gross Profit",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        items(productReport) { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(item.productNameEn, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${item.unitsSold} units sold • SKU: ${item.sku}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("৳" + Formatters.formatMinorUnit(item.revenueMinor), fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Profit: ৳${Formatters.formatMinorUnit(item.grossProfitMinor)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Customer-wise breakdown
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBangla) "শীর্ষ গ্রাহক ও বকেয়া হিসাব" else "Top Customers & Receivables",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        items(customerReport) { cust ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(cust.customerName, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${cust.invoiceCount} invoices • ${cust.customerPhone}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Purchased: ৳" + Formatters.formatMinorUnit(cust.totalPurchasedMinor), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            text = "Due: ৳${Formatters.formatMinorUnit(cust.outstandingBalanceMinor)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cust.outstandingBalanceMinor > 0) Color(0xFFC53030) else StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
