package com.pocketdev.advance_erp.feature.imports

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warehouse
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt

enum class ImportModuleTab(val titleEn: String, val titleBn: String, val icon: ImageVector) {
    OVERVIEW("Overview", "সারসংক্ষেপ", Icons.Default.Dashboard),
    PURCHASE_ORDERS("Purchase Orders", "ক্রয়াদেশ (চীন)", Icons.Default.ShoppingCart),
    SHIPMENTS("Shipments", "আমদানি চালান", Icons.Default.FlightTakeoff),
    CARTON_TRACKING("Cartons & CBM", "কার্টন ট্র্যাকিং", Icons.Default.ViewInAr),
    FREIGHT_CUSTOMS("Cost Sheet", "আমদানি ব্যয়", Icons.Default.Shield),
    LANDED_COST("Landed Cost Engine", "ল্যান্ডেড কস্ট ইঞ্জিন", Icons.Default.Calculate),
    RECEIVING("Warehouse Receiving", "পণ্য গ্রহণ", Icons.Default.Warehouse),
    COST_LAYERS("Inventory Cost Layers", "কস্ট লেয়ার্স", Icons.Default.Layers)
}

@Composable
fun ImportManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
    initialTab: ImportModuleTab = ImportModuleTab.OVERVIEW
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val isBangla = uiState.language == AppLanguage.BANGLA

    // Dialog state controllers
    var showCreatePoDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showCreateShipmentDialog by remember { mutableStateOf(false) }
    var showAddCostDialog by remember { mutableStateOf(false) }
    var showReceivingDialog by remember { mutableStateOf(false) }
    var showAddCartonDialog by remember { mutableStateOf(false) }
    var showAddAdditionalCostDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Preselected entities for dialogs
    var preselectedOrderIdForPayment by remember { mutableStateOf<String?>(null) }
    var preselectedSupplierIdForPayment by remember { mutableStateOf<String?>(null) }
    var preselectedOrderIdForShipment by remember { mutableStateOf<String?>(null) }
    var preselectedSupplierIdForShipment by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isLoading && uiState.purchaseOrders.isEmpty() && uiState.shipments.isEmpty()) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    val selectedShipment = uiState.shipments.find { it.id == uiState.selectedShipmentId }
        ?: uiState.shipments.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ERPBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Module Header Banner
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
                            .background(PrimaryPurple, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_management),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.import_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(SecondaryTeal.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBangla) "সক্রিয়" else "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Module Navigation Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ImportModuleTab.entries) { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryPurple else Color.White)
                            .clickable { selectedTab = tab }
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
                                text = if (isBangla) tab.titleBn else tab.titleEn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pre-calculate filtered orders outside LazyColumn
            val supplierMap = remember(uiState.suppliers) { uiState.suppliers.associateBy { it.id } }
            val filteredOrders = remember(uiState.purchaseOrders, uiState.searchQuery, uiState.poStatusFilter, uiState.poSupplierFilter, uiState.shipments) {
                uiState.purchaseOrders.filter { po ->
                    val supplier = supplierMap[po.supplierId]
                    val query = uiState.searchQuery.trim()
                    val matchesSearch = query.isBlank() ||
                            po.poNumber.contains(query, ignoreCase = true) ||
                            (supplier?.companyName?.contains(query, ignoreCase = true) == true) ||
                            (po.notes?.contains(query, ignoreCase = true) == true) ||
                            uiState.shipments.any { it.purchaseOrderId == po.id && (it.shipmentNumber.contains(query, ignoreCase = true) || it.trackingReference?.contains(query, ignoreCase = true) == true) }

                    val matchesStatus = uiState.poStatusFilter == null || po.status == uiState.poStatusFilter?.name
                    val matchesSupplier = uiState.poSupplierFilter == null || po.supplierId == uiState.poSupplierFilter

                    matchesSearch && matchesStatus && matchesSupplier
                }
            }

            // Active Tab Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // ==========================================
                // TAB 0: OVERVIEW DASHBOARD
                // ==========================================
                if (selectedTab == ImportModuleTab.OVERVIEW) {
                    val totalPoValueBdt = uiState.purchaseOrders.sumOf { it.totalBdtAmountMinor }
                    val totalPaidBdt = uiState.purchaseOrders.sumOf { it.paidBdtAmountMinor }
                    val totalOutstandingBdt = (totalPoValueBdt - totalPaidBdt).coerceAtLeast(0L)
                    val activeShipmentsCount = uiState.shipments.count { it.status == "IN_TRANSIT" || it.status == "CUSTOMS" }
                    val totalCbmSum = uiState.shipments.fold(java.math.BigDecimal.ZERO) { acc, s -> acc.add(java.math.BigDecimal(s.totalCbm)) }
                    val totalExpensesBdt = uiState.costItems.sumOf { it.bdtAmountMinor }

                    // Quick Actions Row
                    item {
                        ERPCard(contentPadding = 12.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isBangla) "দ্রুত কার্যক্রম (Quick Actions)" else "Quick Actions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showCreatePoDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "+ ক্রয়াদেশ" else "+ New PO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            preselectedOrderIdForPayment = null
                                            preselectedSupplierIdForPayment = null
                                            showRecordPaymentDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "+ পেমেন্ট" else "+ Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            preselectedOrderIdForShipment = null
                                            preselectedSupplierIdForShipment = null
                                            showCreateShipmentDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(text = if (isBangla) "+ চালান" else "+ Shipment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Key KPI Cards Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PO Commitments Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.PURCHASE_ORDERS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "মোট ক্রয়াদেশ" else "Total POs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${uiState.purchaseOrders.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = Formatters.formatMoney(totalPoValueBdt, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            // Outstanding Payables Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.PURCHASE_ORDERS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Payments, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "বকেয়া দেনা" else "Outstanding",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = Formatters.formatMoney(totalOutstandingBdt, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (totalOutstandingBdt > 0) StatusError else StatusSuccess
                                    )
                                    Text(
                                        text = "${if (isBangla) "পরিশোধিত: " else "Paid: "} ${Formatters.formatMoney(totalPaidBdt, CurrencyCode.BDT, uiState.language)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Active Shipments Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.SHIPMENTS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "চলমান চালান" else "In-Transit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$activeShipmentsCount / ${uiState.shipments.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "${totalCbmSum.toPlainString()} m³ total volume",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SecondaryTeal
                                    )
                                }
                            }

                            // Capitalized Costs Card
                            ERPCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = ImportModuleTab.FREIGHT_CUSTOMS },
                                contentPadding = 14.dp
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "আমদানি ব্যয়" else "Import Costs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = Formatters.formatMoney(totalExpensesBdt, CurrencyCode.BDT, uiState.language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryPurple
                                    )
                                    Text(
                                        text = "${uiState.costItems.size} ${if (isBangla) "টি বিল ভাউচার" else "expense items"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Warehouse Receiving KPI
                    item {
                        ERPCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = ImportModuleTab.RECEIVING },
                            contentPadding = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warehouse, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "গুদামে পণ্য গ্রহণ ও কস্ট লেয়ার্স" else "Warehouse Receiving & Cost Layers",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${uiState.warehouseReceipts.size} ${if (isBangla) "টি চালান গৃহীত হয়েছে • ${uiState.costLayers.size} টি কস্ট ব্যাচ সক্রিয়" else "Receipts Posted • ${uiState.costLayers.size} Cost Batches Active"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusSuccess.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "FIFO কস্টিং" else "FIFO Costing",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: PURCHASE ORDERS
                // ==========================================
                if (selectedTab == ImportModuleTab.PURCHASE_ORDERS) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "ক্রয়াদেশ (চীন)" else "Purchase Orders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "China Factory Orders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showCreatePoDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "+ নতুন PO" else "+ New PO",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        preselectedOrderIdForPayment = null
                                        preselectedSupplierIdForPayment = null
                                        showRecordPaymentDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isBangla) "পরিশোধ" else "Pay",
                                        fontSize = 12.sp,
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Search & Filter Box
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(if (isBangla) "PO নম্বর বা সাপ্লায়ার খুঁজুন..." else "Search by PO # or Supplier...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Status Filter Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            val allOption = null
                            item {
                                FilterChipView(
                                    label = if (isBangla) "সকল (${uiState.purchaseOrders.size})" else "All (${uiState.purchaseOrders.size})",
                                    isSelected = uiState.poStatusFilter == null,
                                    onClick = { viewModel.setPoStatusFilter(null) }
                                )
                            }
                            items(PurchaseOrderStatus.entries) { st ->
                                val count = uiState.purchaseOrders.count { it.status == st.name }
                                val label = if (isBangla) "${st.labelBn} ($count)" else "${st.labelEn} ($count)"
                                FilterChipView(
                                    label = label,
                                    isSelected = uiState.poStatusFilter == st,
                                    onClick = {
                                        viewModel.setPoStatusFilter(if (uiState.poStatusFilter == st) null else st)
                                    }
                                )
                            }
                        }
                    }

                    if (filteredOrders.isEmpty()) {
                        item {
                            ERPCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentPadding = 24.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(PrimaryPurple.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "কোনো ক্রয়াদেশ পাওয়া যায়নি" else "No Purchase Orders Found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBangla) "অনুসন্ধান বা ফিল্টার পরিবর্তন করুন অথবা নতুন ক্রয়াদেশ তৈরি করুন।" else "Adjust your search filter or create a new purchase order.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredOrders) { po ->
                            val supplier = supplierMap[po.supplierId]
                            LivePurchaseOrderCard(
                                order = po,
                                supplierName = supplier?.companyName ?: "China Factory",
                                language = uiState.language,
                                onClick = {
                                    viewModel.selectPurchaseOrder(po)
                                },
                                onConfirmOrder = { orderId ->
                                    viewModel.confirmPurchaseOrder(orderId)
                                },
                                onSharePdf = {
                                    viewModel.sharePurchaseOrderPdf(context, po)
                                }
                            )
                        }
                    }
                }

                // ==========================================
                // TAB 2: SHIPMENTS
                // ==========================================
                if (selectedTab == ImportModuleTab.SHIPMENTS) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.import_shipments),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isBangla) "এয়ার কার্গো এবং সমুদ্র কনসাইনমেন্ট" else "Air Cargo & Sea Consignments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedShipment != null) {
                                    IconButton(
                                        onClick = { viewModel.sharePackingListPdf(context, selectedShipment) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Packing List", tint = SecondaryTeal)
                                    }
                                }
                                Button(
                                    onClick = {
                                        preselectedOrderIdForShipment = null
                                        preselectedSupplierIdForShipment = null
                                        showCreateShipmentDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "+ নতুন চালান" else "+ New Shipment",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.shipments.isEmpty()) {
                        item {
                            ERPCard(contentPadding = 20.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = PrimaryPurple.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isBangla) "কোনো চালান এখনও বুক করা হয়নি" else "No Import Shipments Created Yet",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepNavy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBangla) "আপনার প্রথম এয়ার কার্গো বা সমুদ্র কনসাইনমেন্ট বুক করতে নিচের বাটনে চাপ দিন।" else "Tap the button below to book and track your first consignment.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            preselectedOrderIdForShipment = null
                                            preselectedSupplierIdForShipment = null
                                            showCreateShipmentDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "প্রথম চালান তৈরি করুন" else "Create First Shipment",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.shipments) { shipment ->
                            val isSelected = shipment.id == uiState.selectedShipmentId
                            LiveShipmentCard(
                                shipment = shipment,
                                isSelected = isSelected,
                                onSelect = { viewModel.selectShipment(shipment.id) },
                                onSharePdf = { viewModel.sharePackingListPdf(context, shipment) }
                            )
                        }
                    }
                }

                // ==========================================
                // TAB 3: CARTONS & CBM TRACKING
                // ==========================================
                if (selectedTab == ImportModuleTab.CARTON_TRACKING) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                title = stringResource(R.string.carton_tracking),
                                subtitle = "Shipment: ${selectedShipment?.shipmentNumber ?: "None"} • Live CBM"
                            )
                            if (selectedShipment != null) {
                                Button(
                                    onClick = { showAddCartonDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Carton", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (selectedShipment != null) {
                        item {
                            ERPCard(backgroundColor = DeepNavy, contentPadding = 14.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Cartons", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                        Text("${selectedShipment.totalCartons}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Volume", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                        Text("${selectedShipment.totalCbm} m³", color = SecondaryTeal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Gross Weight", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                        Text("${selectedShipment.totalGrossWeightKg} kg", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }

                        if (uiState.selectedShipmentCartons.isEmpty()) {
                            item {
                                ERPCard {
                                    Text("No cartons found for ${selectedShipment.shipmentNumber}. Tap '+ Add Carton' to add.", color = TextMuted)
                                }
                            }
                        } else {
                            items(uiState.selectedShipmentCartons) { carton ->
                                val product = uiState.products.find { it.id == carton.productId }
                                ERPCard(contentPadding = 12.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(carton.cartonNumber, fontWeight = FontWeight.Bold, color = DeepNavy)
                                            Text("SKU: ${product?.nameEn ?: carton.productId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("Dims: ${carton.lengthCm}×${carton.widthCm}×${carton.heightCm} cm", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${carton.quantity} pcs", fontWeight = FontWeight.Bold, color = PrimaryPurple)
                                            Text("${carton.cbm} m³ • ${carton.grossWeightKg} kg", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            ERPCard { Text("Please select a shipment to view cartons.", color = TextMuted) }
                        }
                    }
                }

                // ==========================================
                // TAB 4: IMPORT COST SHEET
                // ==========================================
                if (selectedTab == ImportModuleTab.FREIGHT_CUSTOMS) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                title = "Import Cost Sheet",
                                subtitle = "Shipment: ${selectedShipment?.shipmentNumber ?: "None"}"
                            )
                            if (selectedShipment != null) {
                                Button(
                                    onClick = { showAddCostDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Cost", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (selectedShipment != null) {
                        val totalBdt = uiState.costItems.sumOf { it.bdtAmountMinor }
                        item {
                            ERPCard(backgroundColor = Color(0xFFF1F5F9)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Import Expenses (BDT):", fontWeight = FontWeight.Bold, color = DeepNavy)
                                    Text(
                                        Formatters.formatMoney(totalBdt, CurrencyCode.BDT, uiState.language),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }

                        if (uiState.costItems.isEmpty()) {
                            item {
                                ERPCard {
                                    Text("No cost records for this shipment yet. Tap '+ Add Cost' to record freight, duty, or handling.", color = TextMuted)
                                }
                            }
                        } else {
                            items(uiState.costItems) { cost ->
                                ERPCard(contentPadding = 12.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(cost.description, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Category: ${cost.costCategory} • Method: ${cost.allocationMethod}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text("A/C: ${cost.paymentAccount}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                Formatters.formatMoney(cost.bdtAmountMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavy
                                            )
                                            Text("${cost.currency} ${cost.amountMinor / 100}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 5: LANDED COST ENGINE
                // ==========================================
                if (selectedTab == ImportModuleTab.LANDED_COST) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                title = stringResource(R.string.landed_cost),
                                subtitle = "Shipment: ${selectedShipment?.shipmentNumber ?: "None"}"
                            )
                            if (selectedShipment != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (uiState.landedCostSnapshots.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.shareLandedCostPdf(context, selectedShipment) }) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Landed Cost Report", tint = PrimaryPurple)
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.calculateLandedCost(selectedShipment.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Engine", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedShipment != null) {
                        if (uiState.landedCostSnapshots.isEmpty()) {
                            item {
                                ERPCard {
                                    Column {
                                        Text("No Landed Cost Snapshots generated yet.", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Tap 'Run Engine' above to automatically distribute all freight, customs and port costs across products by CBM, Weight and Value.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.landedCostSnapshots) { snapshot ->
                                ERPCard(contentPadding = 14.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(snapshot.productNameEn, fontWeight = FontWeight.Bold, color = DeepNavy)
                                                Text("SKU: ${snapshot.sku} • Qty: ${snapshot.totalReceivedQuantity} pcs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("True Unit Landed Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(
                                                    "${Formatters.formatMoney(snapshot.unitLandedCostBdtMinor, CurrencyCode.BDT, uiState.language)} / pc",
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = PrimaryPurple
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = ERPDivider)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("FOB Factory Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(Formatters.formatMoney(snapshot.totalPurchaseCostBdtMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Column {
                                                Text("Allocated Freight & Duty", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(Formatters.formatMoney(snapshot.totalAllocatedCostsBdtMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = SecondaryTeal)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Total Batch Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(Formatters.formatMoney(snapshot.totalLandedCostBdtMinor, CurrencyCode.BDT, uiState.language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 6: WAREHOUSE RECEIVING
                // ==========================================
                if (selectedTab == ImportModuleTab.RECEIVING) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ERPSectionHeader(
                                title = stringResource(R.string.warehouse_receiving),
                                subtitle = "Check-in to Central Warehouse • Stock-in & Audit"
                            )
                            if (selectedShipment != null) {
                                Button(
                                    onClick = { showReceivingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Inspect & Receive", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (uiState.warehouseReceipts.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No warehouse receipts posted yet. Tap 'Inspect & Receive' to check-in an arrived shipment into Room DB.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.warehouseReceipts) { receipt ->
                            ERPCard(contentPadding = 14.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(receipt.receiptNumber, fontWeight = FontWeight.Bold, color = DeepNavy)
                                        Text("Warehouse: WH-DHK-01 • ${Formatters.formatDate(receipt.receiptDate, uiState.language)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(receipt.notes ?: "Import check-in complete", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.shareReceivingReportPdf(context, receipt) }) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Receiving Report", tint = SecondaryTeal)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "স্টকে পোস্টকৃত" else "POSTED TO STOCK",
                                                color = StatusSuccess,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 7: INVENTORY COST LAYERS
                // ==========================================
                if (selectedTab == ImportModuleTab.COST_LAYERS) {
                    item {
                        ERPSectionHeader(
                            title = "Inventory Batch Cost Layers",
                            subtitle = "Traceable FIFO / Batch Costing with Landed Cost History"
                        )
                    }

                    if (uiState.costLayers.isEmpty()) {
                        item {
                            ERPCard {
                                Text("No inventory cost layers created yet. Post a warehouse receipt to generate batch cost layers.", color = TextMuted)
                            }
                        }
                    } else {
                        items(uiState.costLayers) { layer ->
                            val product = uiState.products.find { it.id == layer.productId }
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(product?.nameEn ?: layer.productId, fontWeight = FontWeight.Bold, color = DeepNavy)
                                            Text("Batch: ${layer.receiptNumber} • ${layer.shipmentNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Unit Landed Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(
                                                Formatters.formatMoney(layer.unitLandedCostBdtMinor, CurrencyCode.BDT, uiState.language),
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Initial: ${layer.initialQuantity} pcs", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("Remaining: ${layer.remainingQuantity} pcs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                        Text(Formatters.formatDate(layer.receiptDate, uiState.language), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                // Notice Banner
                item {
                    ERPCard(backgroundColor = Color(0xFFF8FAFC)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SecondaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.import_feature_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
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

    // ==========================================
    // DIALOGS
    // ==========================================

    if (showCreatePoDialog) {
        CreatePurchaseOrderDialog(
            suppliers = uiState.suppliers,
            products = uiState.products,
            categories = uiState.categories,
            isBangla = isBangla,
            onDismiss = { showCreatePoDialog = false },
            onAddNewProduct = viewModel::addNewProductFromPurchase,
            onAddNewCategory = viewModel::addNewCategory,
            onSavePo = { supId, cur, rate, items, notes, status ->
                viewModel.createPurchaseOrder(supId, cur, rate, items, notes, status)
            }
        )
    }

    if (showRecordPaymentDialog) {
        RecordSupplierPaymentDialog(
            suppliers = uiState.suppliers,
            orders = uiState.purchaseOrders,
            preselectedSupplierId = preselectedSupplierIdForPayment,
            preselectedOrderId = preselectedOrderIdForPayment,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirm = { supId, ordId, amt, cur, rate, meth, acc, ref, notes ->
                viewModel.recordSupplierPayment(supId, ordId, amt, cur, rate, meth, acc, ref, notes)
            }
        )
    }

    if (showCreateShipmentDialog) {
        CreateShipmentDialog(
            suppliers = uiState.suppliers,
            orders = uiState.purchaseOrders,
            products = uiState.products,
            preselectedSupplierId = preselectedSupplierIdForShipment,
            preselectedOrderId = preselectedOrderIdForShipment,
            isBangla = isBangla,
            loadPoRemainingItems = viewModel::getPoRemainingItems,
            onDismiss = { showCreateShipmentDialog = false },
            onConfirm = { supId, ordId, meth, orig, dest, trk, shipLine, cartons, notes, onSuccess, onError ->
                viewModel.createShipment(
                    supplierId = supId,
                    orderId = ordId,
                    method = meth,
                    origin = orig,
                    destination = dest,
                    trackingRef = trk,
                    shippingLine = shipLine,
                    cartons = cartons,
                    notes = notes,
                    onSuccess = { shipmentId, shipmentNumber ->
                        onSuccess(shipmentId, shipmentNumber)
                        selectedTab = ImportModuleTab.SHIPMENTS
                    },
                    onError = { err ->
                        onError(err)
                    }
                )
            }
        )
    }

    if (showAddCartonDialog) {
        val activeOrderId = uiState.selectedPo?.id
        val activeShipment = selectedShipment
        AddCartonDialog(
            shipment = activeShipment,
            orderId = activeOrderId,
            products = uiState.products,
            language = uiState.language,
            onDismiss = { showAddCartonDialog = false },
            onConfirm = { carton ->
                if (activeOrderId != null) {
                    viewModel.addCartonToPurchase(activeOrderId, carton)
                } else if (activeShipment != null) {
                    viewModel.addCartonToShipment(activeShipment.id, carton)
                }
            },
            onConfirmMixed = { cartonNumber, l, w, h, gw, nw, lines ->
                if (activeOrderId != null) {
                    viewModel.addMixedCarton(activeOrderId, cartonNumber, l, w, h, gw, nw, lines)
                }
            }
        )
    }

    if (showAddCostDialog) {
        val activeOrderId = uiState.selectedPo?.id
        val activeShipmentId = selectedShipment?.id
        val grossWeight = uiState.selectedPo?.totalGrossWeightKg?.let { java.math.BigDecimal(it.ifBlank { "0" }) }
            ?: selectedShipment?.totalGrossWeightKg?.let { java.math.BigDecimal(it.ifBlank { "0" }) }
            ?: java.math.BigDecimal("350.0")
        AddCostItemDialog(
            shipmentId = activeShipmentId,
            orderId = activeOrderId,
            totalGrossWeightKg = grossWeight,
            isBangla = isBangla,
            onDismiss = { showAddCostDialog = false },
            onConfirm = { cat, desc, amt, cur, rate, meth, acc, ref, isEst, notes ->
                if (activeOrderId != null) {
                    viewModel.addPurchaseCostItem(
                        orderId = activeOrderId,
                        category = cat,
                        description = desc,
                        amountMinor = amt,
                        currency = cur,
                        exchangeRate = rate,
                        method = meth,
                        isEstimated = isEst,
                        paymentAccount = acc,
                        docRef = ref,
                        notes = notes
                    )
                } else if (activeShipmentId != null) {
                    viewModel.addCostItem(activeShipmentId, cat, desc, amt, cur, rate, meth, acc, ref)
                }
            }
        )
    }

    if (showReceivingDialog) {
        val activePo = uiState.selectedPo
        val activeShipment = selectedShipment
        WarehouseReceivingDialog(
            shipment = activeShipment,
            order = activePo,
            poItems = uiState.selectedPoItems,
            cartons = if (activePo != null) uiState.selectedPoCartons else uiState.selectedShipmentCartons,
            products = uiState.products,
            isCostFinalized = activePo?.isActualCostFinalized ?: false,
            isBangla = isBangla,
            onDismiss = { showReceivingDialog = false },
            onSaveDraft = { whId, meth, items, notes ->
                if (activePo != null) {
                    viewModel.createPurchaseReceiving(activePo.id, whId, meth, items, notes)
                }
            },
            onConfirmPost = { whId, items, notes, allowDiscrepancy ->
                if (activePo != null) {
                    viewModel.createPurchaseReceiving(activePo.id, whId, "PHYSICAL_COUNT", items, notes)
                } else if (activeShipment != null) {
                    viewModel.createAndPostWarehouseReceipt(activeShipment.id, whId, items, notes, allowDiscrepancy)
                }
            }
        )
    }

    if (showAddAdditionalCostDialog && uiState.selectedPo != null) {
        AddAdditionalCostDialog(
            orderId = uiState.selectedPo!!.id,
            isBangla = isBangla,
            onDismiss = { showAddAdditionalCostDialog = false },
            onConfirm = { cat, desc, amt, isCap, meth, notes ->
                viewModel.addAdditionalCost(
                    orderId = uiState.selectedPo!!.id,
                    warehouseId = null,
                    category = cat,
                    description = desc,
                    amountMinor = amt,
                    isCapitalizable = isCap,
                    allocationMethod = meth,
                    notes = notes
                )
            }
        )
    }

    if (showCompletionDialog && uiState.selectedPo != null) {
        val po = uiState.selectedPo!!
        val totalOrdered = uiState.selectedPoItems.sumOf { it.quantity }
        val totalReceived = uiState.selectedPoItems.sumOf { it.receivedQuantity }
        val dmgQty = uiState.selectedPoDamageRecords.sumOf { it.damagedQuantity }
        val misQty = uiState.selectedPoShortageRecords.sumOf { it.missingQuantity }
        val goodQty = (totalReceived - dmgQty - misQty).coerceAtLeast(0)
        val remainingDue = (po.totalAmountMinor - po.paidAmountMinor).coerceAtLeast(0L)

        PurchaseCompletionDialog(
            order = po,
            totalOrdered = totalOrdered,
            totalReceived = totalReceived,
            goodQuantity = goodQty,
            damagedQuantity = dmgQty,
            missingQuantity = misQty,
            paymentDueMinor = remainingDue,
            isCostFinalized = po.isActualCostFinalized,
            isBangla = isBangla,
            onDismiss = { showCompletionDialog = false },
            onConfirmComplete = { disp, reas, nts ->
                viewModel.completePurchase(po.id, disp, reas, nts)
            }
        )
    }

    // Purchase Order Detail Dialog
    if (uiState.selectedPo != null) {
        val selectedPo = uiState.selectedPo!!
        val supplier = uiState.suppliers.find { it.id == selectedPo.supplierId }
        PurchaseOrderDetailDialog(
            order = selectedPo,
            supplier = supplier,
            items = uiState.selectedPoItems,
            products = uiState.products,
            payments = uiState.selectedPoPayments,
            shipments = uiState.selectedPoShipments,
            cartons = uiState.selectedPoCartons,
            costItems = uiState.selectedPoCostItems,
            receipts = uiState.selectedPoReceipts,
            damageRecords = uiState.selectedPoDamageRecords,
            shortageRecords = uiState.selectedPoShortageRecords,
            additionalCosts = uiState.selectedPoAdditionalCosts,
            completionRecord = uiState.selectedPoCompletionRecord,
            language = uiState.language,
            onDismiss = { viewModel.selectPurchaseOrder(null) },
            onConfirmPo = { orderId ->
                viewModel.confirmPurchaseOrder(orderId)
            },
            onMakePayment = { po ->
                preselectedSupplierIdForPayment = po.supplierId
                preselectedOrderIdForPayment = po.id
                showRecordPaymentDialog = true
            },
            onCreateShipment = { po ->
                preselectedSupplierIdForShipment = po.supplierId
                preselectedOrderIdForShipment = po.id
                showCreateShipmentDialog = true
            },
            onAddCarton = { showAddCartonDialog = true },
            onAddCostItem = { showAddCostDialog = true },
            onFinalizeActualCost = { orderId ->
                viewModel.finalizePurchaseActualCost(orderId)
            },
            onReceiveProducts = { showReceivingDialog = true },
            onPostReceiving = { receiptId ->
                viewModel.postPurchaseReceiving(receiptId)
            },
            onAddAdditionalCost = { showAddAdditionalCostDialog = true },
            onCompletePo = { showCompletionDialog = true },
            onSharePdf = { po ->
                viewModel.sharePurchaseOrderPdf(context, po)
            },
            onCancelPo = { orderId, reason ->
                viewModel.cancelPurchaseOrder(orderId, reason)
            },
            onDuplicatePo = { orderId ->
                viewModel.duplicatePurchaseOrder(orderId)
            },
            onSelectShipment = { shp ->
                viewModel.selectPurchaseOrder(null)
                viewModel.selectShipment(shp.id)
                selectedTab = ImportModuleTab.SHIPMENTS
            },
            onDeleteCarton = viewModel::deleteCarton,
            onDeleteCostItem = { costId -> viewModel.deleteCostItem(selectedPo.id, costId) },
            onUpdatePacking = { ctn, gross, net, cbm -> viewModel.updateOrderPacking(selectedPo.id, ctn, gross, net, cbm) },
            onUpdatePoItem = { itemId, q, p, c, g -> viewModel.updatePoItem(selectedPo.id, itemId, q, p, c, g) },
            onDeletePoItem = { itemId -> viewModel.deletePoItem(selectedPo.id, itemId) }
        )
    }
}

@Composable
private fun FilterChipView(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryPurple else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun LivePurchaseOrderCard(
    order: PurchaseOrder,
    supplierName: String,
    language: AppLanguage,
    onClick: () -> Unit,
    onConfirmOrder: (String) -> Unit = {},
    onSharePdf: () -> Unit = {}
) {
    val isBangla = language == AppLanguage.BANGLA
    ERPCard(
        contentPadding = 14.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.poNumber,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF",
                            tint = SecondaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                val statusColor = when (order.status) {
                    "PAID", "COMPLETED" -> StatusSuccess
                    "PARTIALLY_PAID", "CONFIRMED" -> SecondaryTeal
                    "SHIPPED" -> PrimaryPurple
                    "DRAFT" -> StatusWarning
                    "CANCELLED" -> StatusError
                    else -> StatusWarning
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val statusLabel = if (isBangla) {
                        when (order.status) {
                            "DRAFT" -> "খসড়া"
                            "CONFIRMED" -> "নিশ্চিতকৃত"
                            "PARTIALLY_PAID" -> "আংশিক পরিশোধিত"
                            "PAID" -> "পরিশোধিত"
                            "SHIPPED" -> "জাহাজীকরণ সম্পন্ন"
                            "COMPLETED" -> "সম্পন্ন"
                            "CANCELLED" -> "বাতিল"
                            else -> order.status
                        }
                    } else order.status
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(supplierName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = ERPDivider)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total (${order.currency})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}", fontWeight = FontWeight.Bold, color = DeepNavy)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BDT Equivalent", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        Formatters.formatMoney(order.totalBdtAmountMinor, CurrencyCode.BDT, language),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }

            // Quick Confirm Action for Draft POs
            if (order.status == "DRAFT") {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = ERPDivider.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "খসড়া অবস্থায় আছে" else "Order is in Draft state",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Button(
                        onClick = { onConfirmOrder(order.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "PO নিশ্চিত করুন" else "Confirm PO",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveShipmentCard(
    shipment: ImportShipment,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onSharePdf: () -> Unit = {}
) {
    ERPCard(
        contentPadding = 14.dp,
        modifier = Modifier.clickable(onClick = onSelect),
        backgroundColor = if (isSelected) Color(0xFFF0FDF4) else Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shipment.shipmentNumber, fontWeight = FontWeight.Bold, color = DeepNavy, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onSharePdf, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share Packing List", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(shipment.shippingLine ?: shipment.shippingMethod, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                val statusColor = when (shipment.status) {
                    "COMPLETED", "RECEIVED" -> StatusSuccess
                    "IN_TRANSIT" -> PrimaryPurple
                    "CUSTOMS" -> StatusWarning
                    else -> SecondaryTeal
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(shipment.status, color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(shipment.origin.substringBefore("("), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                Text(shipment.destination.substringBefore("("), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = ERPDivider)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${shipment.totalCartons} Cartons", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("${shipment.totalCbm} m³", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal, fontWeight = FontWeight.Bold)
                Text("${shipment.totalGrossWeightKg} kg", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
