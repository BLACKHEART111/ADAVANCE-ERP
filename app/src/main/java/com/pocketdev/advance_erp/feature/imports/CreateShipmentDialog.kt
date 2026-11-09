package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.PoRemainingItem
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class CartonDraftState(
    var cartonNumber: String,
    var productId: String,
    var quantityStr: String = "50",
    var lengthStr: String = "50",
    var widthStr: String = "40",
    var heightStr: String = "30",
    var grossWeightStr: String = "15.0"
)

@Composable
fun CreateShipmentDialog(
    suppliers: List<Supplier>,
    orders: List<PurchaseOrder>,
    products: List<Product>,
    preselectedSupplierId: String? = null,
    preselectedOrderId: String? = null,
    isBangla: Boolean = false,
    loadPoRemainingItems: suspend (String) -> List<PoRemainingItem>,
    onDismiss: () -> Unit,
    onConfirm: (
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartons: List<ShipmentCartonDraft>,
        notes: String?,
        onSuccess: (shipmentId: String, shipmentNumber: String) -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Shippable orders (Confirmed, Partially Paid, Paid, Shipped)
    val shippableOrders = remember(orders) {
        orders.filter {
            it.status != PurchaseOrderStatus.DRAFT.name &&
                    it.status != PurchaseOrderStatus.CANCELLED.name
        }
    }

    val initialOrder = shippableOrders.find { it.id == preselectedOrderId }
        ?: if (preselectedOrderId != null) orders.find { it.id == preselectedOrderId } else null

    var selectedOrderId by remember { mutableStateOf(initialOrder?.id) }
    var selectedSupplierId by remember {
        mutableStateOf(
            preselectedSupplierId ?: initialOrder?.supplierId ?: suppliers.firstOrNull()?.id ?: ""
        )
    }

    var poRemainingItems by remember { mutableStateOf<List<PoRemainingItem>>(emptyList()) }
    var isLoadingPoItems by remember { mutableStateOf(false) }

    var selectedMethod by remember { mutableStateOf(ShippingMethod.AIR) }
    var origin by remember { mutableStateOf("Shenzhen Bao'an Airport (SZX)") }
    var destination by remember { mutableStateOf("Dhaka Hazrat Shahjalal (DAC)") }
    var shippingLine by remember { mutableStateOf("China Southern Air Cargo") }
    var trackingRef by remember { mutableStateOf("AWB-784-99281726") }
    var notes by remember { mutableStateOf("Fragile electronic consignment") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cartonList = remember {
        mutableStateListOf(
            CartonDraftState(
                cartonNumber = "CTN-01/01",
                productId = products.firstOrNull()?.id ?: "",
                quantityStr = "50",
                lengthStr = "50",
                widthStr = "40",
                heightStr = "30",
                grossWeightStr = "15.0"
            )
        )
    }

    // Load PO remaining items whenever selectedOrderId changes
    LaunchedEffect(selectedOrderId) {
        val ordId = selectedOrderId
        if (ordId != null) {
            isLoadingPoItems = true
            try {
                val remItems = loadPoRemainingItems(ordId)
                poRemainingItems = remItems

                // Update supplier to match PO
                val ord = orders.find { it.id == ordId }
                if (ord != null) {
                    selectedSupplierId = ord.supplierId
                }

                // Auto-configure the first carton's product and quantity if possible
                val firstAvailable = remItems.firstOrNull { it.remainingQty > 0 } ?: remItems.firstOrNull()
                if (firstAvailable != null && cartonList.isNotEmpty()) {
                    val currentCtn = cartonList[0]
                    // If current carton product doesn't belong to this PO, switch it
                    if (remItems.none { it.poItem.productId == currentCtn.productId }) {
                        currentCtn.productId = firstAvailable.poItem.productId
                        currentCtn.quantityStr = if (firstAvailable.remainingQty > 0) {
                            minOf(firstAvailable.remainingQty, 50).toString()
                        } else "1"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error loading PO items: ${e.message}"
            } finally {
                isLoadingPoItems = false
            }
        } else {
            poRemainingItems = emptyList()
        }
    }

    // Pre-calculate live totals
    val totalShipmentQty = cartonList.sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }
    val totalShipmentCbm = try {
        cartonList.fold(BigDecimal.ZERO) { acc, c ->
            val l = BigDecimal(c.lengthStr.trim().ifEmpty { "0" })
            val w = BigDecimal(c.widthStr.trim().ifEmpty { "0" })
            val h = BigDecimal(c.heightStr.trim().ifEmpty { "0" })
            acc.add(CbmCalculator.calculateCbm(l, w, h, 1))
        }.setScale(4, RoundingMode.HALF_UP).toPlainString()
    } catch (e: Exception) {
        "0.0000"
    }

    val totalShipmentGrossWeight = try {
        cartonList.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(BigDecimal(c.grossWeightStr.trim().ifEmpty { "0" }))
        }.setScale(1, RoundingMode.HALF_UP).toPlainString()
    } catch (e: Exception) {
        "0.0"
    }

    // Dropdown expanded states
    var showPoMenu by remember { mutableStateOf(false) }
    var showSupplierMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ==========================================
                // 1. FIXED DIALOG HEADER
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryPurple.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedMethod == ShippingMethod.SEA) Icons.Default.LocalShipping else Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBangla) "নতুন আমদানি চালান তৈরি করুন" else "Create Import Shipment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Text(
                                text = if (isBangla) "চীন ফ্যাক্টরি ট্র্যাকিং ও কার্টন মেজারমেন্ট" else "Factory Consignment Tracking & Carton CBM",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = ERPDivider)

                // ==========================================
                // 2. SCROLLABLE FORM BODY
                // ==========================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ------------------------------------------
                    // SECTION A: PURCHASE ORDER SELECTION
                    // ------------------------------------------
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBangla) "ক্রয়াদেশ সংযোগ (Purchase Order Link)" else "Purchase Order Link",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (preselectedOrderId != null) {
                                    // Opened from PO Details: Locked PO Badge
                                    val po = orders.find { it.id == preselectedOrderId }
                                    val sup = suppliers.find { it.id == po?.supplierId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(PrimaryPurple.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = po?.poNumber ?: preselectedOrderId,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple
                                                )
                                                Text(
                                                    text = sup?.companyName ?: "Supplier",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(SecondaryTeal.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = po?.status ?: "CONFIRMED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal
                                            )
                                        }
                                    }
                                } else {
                                    // Opened from Shipments Tab: Interactive PO Selector
                                    Box {
                                        val currentPo = orders.find { it.id == selectedOrderId }
                                        val sup = suppliers.find { it.id == currentPo?.supplierId }
                                        val poDisplayText = if (currentPo != null) {
                                            "${currentPo.poNumber} • ${sup?.companyName ?: "Supplier"} (${currentPo.currency})"
                                        } else {
                                            if (isBangla) "[ কোনো PO নেই / স্ট্যান্ডঅ্যালোন চালান ]" else "[ Standalone Shipment / No PO ]"
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .clickable { showPoMenu = true }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (currentPo != null) Icons.Default.ShoppingCart else Icons.Default.LocalShipping,
                                                    contentDescription = null,
                                                    tint = if (currentPo != null) PrimaryPurple else TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = poDisplayText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (currentPo != null) TextPrimary else TextMuted
                                                )
                                            }
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
                                        }

                                        DropdownMenu(
                                            expanded = showPoMenu,
                                            onDismissRequest = { showPoMenu = false },
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (isBangla) "কোনো PO নেই (স্বতন্ত্র চালান)" else "Standalone (No PO)",
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    selectedOrderId = null
                                                    showPoMenu = false
                                                }
                                            )
                                            shippableOrders.forEach { po ->
                                                val poSup = suppliers.find { it.id == po.supplierId }
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = "${po.poNumber} • ${poSup?.companyName ?: "Supplier"}",
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Status: ${po.status} • Currency: ${po.currency}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = TextSecondary
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedOrderId = po.id
                                                        selectedSupplierId = po.supplierId
                                                        showPoMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // If Standalone, show Supplier Selector
                                    if (selectedOrderId == null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (isBangla) "সাপ্লায়ার নির্বাচন করুন:" else "Supplier Selection:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            val currentSup = suppliers.find { it.id == selectedSupplierId }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                                                    .background(Color.White, RoundedCornerShape(8.dp))
                                                    .clickable { showSupplierMenu = true }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = currentSup?.companyName ?: (if (isBangla) "সাপ্লায়ার বাছাই করুন" else "Select Supplier"),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = TextPrimary
                                                    )
                                                }
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
                                            }

                                            DropdownMenu(
                                                expanded = showSupplierMenu,
                                                onDismissRequest = { showSupplierMenu = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                suppliers.forEach { sup ->
                                                    DropdownMenuItem(
                                                        text = { Text(sup.companyName, fontWeight = FontWeight.SemiBold) },
                                                        onClick = {
                                                            selectedSupplierId = sup.id
                                                            showSupplierMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // ------------------------------------------
                                // PO REMAINING QUANTITY SUMMARY (If PO selected)
                                // ------------------------------------------
                                if (selectedOrderId != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (isLoadingPoItems) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isBangla) "PO আইটেম ও অবশিষ্ট হিসাব লোড হচ্ছে..." else "Loading PO remaining items...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    } else if (poRemainingItems.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "অর্ডারের আইটেম ও অবশিষ্ট প্রেরণযোগ্য হিসাব:" else "PO Line Items & Available to Ship:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavy
                                            )
                                            poRemainingItems.forEach { rem ->
                                                val prodName = rem.product?.nameEn ?: rem.poItem.productId
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = prodName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "Ordered: ${rem.orderedQty} • Shipped: ${rem.shippedQty}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (rem.remainingQty > 0) SecondaryTeal.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (rem.remainingQty > 0) {
                                                                "Remaining: ${rem.remainingQty} pcs"
                                                            } else {
                                                                "Fully Shipped"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (rem.remainingQty > 0) SecondaryTeal else TextMuted
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // SECTION B: SHIPPING METHOD
                    // ------------------------------------------
                    item {
                        Column {
                            Text(
                                text = if (isBangla) "শিপিং মাধ্যম (Shipping Method)" else "Shipping Method",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShippingMethod.entries.forEach { sm ->
                                    val isSel = selectedMethod == sm
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) PrimaryPurple else Color(0xFFF1F5F9))
                                            .clickable {
                                                selectedMethod = sm
                                                if (sm == ShippingMethod.SEA) {
                                                    origin = "Shenzhen Yantian Port, China"
                                                    destination = "Chattogram Port, Bangladesh"
                                                    shippingLine = "COSCO Shipping Lines"
                                                    trackingRef = "CONT-COSU662910"
                                                } else {
                                                    origin = "Shenzhen Bao'an Airport (SZX)"
                                                    destination = "Dhaka Hazrat Shahjalal (DAC)"
                                                    shippingLine = "China Southern Air Cargo"
                                                    trackingRef = "AWB-784-99281726"
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isBangla) sm.labelBn else sm.labelEn,
                                            color = if (isSel) Color.White else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // SECTION C: ORIGIN & DESTINATION
                    // ------------------------------------------
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = origin,
                                onValueChange = { origin = it },
                                label = { Text(if (isBangla) "উৎস (চীন)" else "Origin (China)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { destination = it },
                                label = { Text(if (isBangla) "গন্তব্য (বাংলাদেশ)" else "Destination (BD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // ------------------------------------------
                    // SECTION D: CARRIER & TRACKING REFERENCE
                    // ------------------------------------------
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = shippingLine,
                                onValueChange = { shippingLine = it },
                                label = { Text(if (isBangla) "শিপিং লাইন / এয়ারলাইন" else "Carrier / Shipping Line") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = trackingRef,
                                onValueChange = { trackingRef = it },
                                label = { Text(if (isBangla) "AWB / কন্টেইনার নং" else "AWB / Container / BL") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // ------------------------------------------
                    // SECTION E: CARTONS & LIVE CBM SUMMARY HEADER
                    // ------------------------------------------
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBangla) "কার্টনের তালিকা ও ডাইমেনশন" else "Cartons & Package Dimensions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Total: $totalShipmentQty pcs • $totalShipmentCbm m³ • $totalShipmentGrossWeight kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    val nextNum = cartonList.size + 1
                                    val defaultProdId = if (poRemainingItems.isNotEmpty()) {
                                        poRemainingItems.firstOrNull { it.remainingQty > 0 }?.poItem?.productId
                                            ?: poRemainingItems.first().poItem.productId
                                    } else {
                                        products.firstOrNull()?.id ?: ""
                                    }
                                    cartonList.add(
                                        CartonDraftState(
                                            cartonNumber = "CTN-%02d/%02d".format(nextNum, maxOf(nextNum, 10)),
                                            productId = defaultProdId,
                                            quantityStr = "50",
                                            lengthStr = "50",
                                            widthStr = "40",
                                            heightStr = "30",
                                            grossWeightStr = "15.0"
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "+ কার্টন" else "+ Add Carton",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ------------------------------------------
                    // SECTION F: CARTON ITEMS
                    // ------------------------------------------
                    itemsIndexed(cartonList) { index, carton ->
                        // Calculate live CBM for this carton
                        val cbm = try {
                            val l = BigDecimal(carton.lengthStr.trim().ifEmpty { "0" })
                            val w = BigDecimal(carton.widthStr.trim().ifEmpty { "0" })
                            val h = BigDecimal(carton.heightStr.trim().ifEmpty { "0" })
                            CbmCalculator.calculateCbm(l, w, h, 1).setScale(4, RoundingMode.HALF_UP).toPlainString()
                        } catch (e: Exception) {
                            "0.0000"
                        }

                        // Product dropdown state for this carton
                        var showProductMenu by remember { mutableStateOf(false) }

                        // Check remaining validation for this product
                        val matchingPoItem = poRemainingItems.find { it.poItem.productId == carton.productId }
                        val totalEnteredForThisProduct = cartonList
                            .filter { it.productId == carton.productId }
                            .sumOf { it.quantityStr.trim().toIntOrNull() ?: 0 }

                        val isOverShipped = matchingPoItem != null && totalEnteredForThisProduct > matchingPoItem.remainingQty

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(
                                width = if (isOverShipped) 1.5.dp else 1.dp,
                                color = if (isOverShipped) StatusError else ERPDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = carton.cartonNumber,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(SecondaryTeal.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "CBM: $cbm m³",
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (cartonList.size > 1) {
                                        IconButton(
                                            onClick = { cartonList.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Carton",
                                                tint = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Product Selector inside Carton
                                Box {
                                    val currentProd = products.find { it.id == carton.productId }
                                        ?: poRemainingItems.find { it.poItem.productId == carton.productId }?.product
                                    val prodLabel = currentProd?.nameEn
                                        ?: (if (carton.productId.isNotEmpty()) carton.productId else "Select Product")

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .clickable { showProductMenu = true }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isBangla) "পণ্য নির্বাচন:" else "Product:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = prodLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
                                    }

                                    DropdownMenu(
                                        expanded = showProductMenu,
                                        onDismissRequest = { showProductMenu = false },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        if (poRemainingItems.isNotEmpty()) {
                                            // Show products from selected PO with remaining quantities
                                            poRemainingItems.forEach { rem ->
                                                val pName = rem.product?.nameEn ?: rem.poItem.productId
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(pName, fontWeight = FontWeight.SemiBold)
                                                            Text(
                                                                text = "Rem: ${rem.remainingQty} pcs",
                                                                color = if (rem.remainingQty > 0) SecondaryTeal else TextMuted,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        carton.productId = rem.poItem.productId
                                                        showProductMenu = false
                                                    }
                                                )
                                            }
                                        } else {
                                            // Standalone: show all catalog products
                                            products.forEach { prod ->
                                                DropdownMenuItem(
                                                    text = { Text(prod.nameEn, fontWeight = FontWeight.SemiBold) },
                                                    onClick = {
                                                        carton.productId = prod.id
                                                        showProductMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quantity & Weight
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = carton.quantityStr,
                                        onValueChange = { carton.quantityStr = it },
                                        label = { Text(if (isBangla) "পরিমাণ (পিস)" else "Qty (pcs)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        isError = isOverShipped,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = carton.grossWeightStr,
                                        onValueChange = { carton.grossWeightStr = it },
                                        label = { Text(if (isBangla) "ওজন (কেজি)" else "Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                if (isOverShipped && matchingPoItem != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBangla) {
                                            "⚠ অবশিষ্ট প্রেরণের চেয়ে বেশি! সর্বোচ্চ ${matchingPoItem.remainingQty} পিস বাকি।"
                                        } else {
                                            "⚠ Total entered ($totalEnteredForThisProduct pcs) exceeds remaining PO qty (${matchingPoItem.remainingQty} pcs max)!"
                                        },
                                        color = StatusError,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Dimensions: L x W x H
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = carton.lengthStr,
                                        onValueChange = { carton.lengthStr = it },
                                        label = { Text("L (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = carton.widthStr,
                                        onValueChange = { carton.widthStr = it },
                                        label = { Text("W (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = carton.heightStr,
                                        onValueChange = { carton.heightStr = it },
                                        label = { Text("H (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // SECTION G: NOTES
                    // ------------------------------------------
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isBangla) "চালানের মন্তব্য / নির্দেশনা" else "Shipment Remarks / Instructions") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // ------------------------------------------
                    // SECTION H: ERROR MESSAGE DISPLAY
                    // ------------------------------------------
                    if (errorMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = StatusError,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider)

                // ==========================================
                // 3. FIXED BOTTOM ACTION BAR
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            errorMessage = null

                            // 1. Basic validation
                            if (cartonList.isEmpty()) {
                                errorMessage = "Please add at least 1 carton"
                                return@Button
                            }

                            if (trackingRef.isBlank()) {
                                errorMessage = "Please enter tracking reference (AWB or Container #)"
                                return@Button
                            }

                            if (selectedSupplierId.isBlank()) {
                                errorMessage = "Please select a valid supplier"
                                return@Button
                            }

                            // 2. Draft parsing & validation
                            val drafts = mutableListOf<ShipmentCartonDraft>()
                            try {
                                cartonList.forEachIndexed { idx, c ->
                                    val l = BigDecimal(c.lengthStr.trim())
                                    val w = BigDecimal(c.widthStr.trim())
                                    val h = BigDecimal(c.heightStr.trim())
                                    val wt = BigDecimal(c.grossWeightStr.trim())
                                    val qty = c.quantityStr.trim().toIntOrNull()
                                        ?: throw IllegalArgumentException("Carton ${c.cartonNumber}: Invalid quantity")

                                    require(qty > 0) { "Carton ${c.cartonNumber}: Quantity must be greater than 0" }
                                    require(l > BigDecimal.ZERO && w > BigDecimal.ZERO && h > BigDecimal.ZERO) {
                                        "Carton ${c.cartonNumber}: Dimensions (L, W, H) must be greater than 0"
                                    }
                                    require(wt >= BigDecimal.ZERO) {
                                        "Carton ${c.cartonNumber}: Weight cannot be negative"
                                    }
                                    require(c.productId.isNotBlank()) {
                                        "Carton ${c.cartonNumber}: Please select a product"
                                    }

                                    drafts.add(
                                        ShipmentCartonDraft(
                                            cartonNumber = c.cartonNumber,
                                            productId = c.productId,
                                            quantity = qty,
                                            lengthCm = l,
                                            widthCm = w,
                                            heightCm = h,
                                            grossWeightKg = wt,
                                            netWeightKg = wt.multiply(BigDecimal("0.9"))
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid carton parameters"
                                return@Button
                            }

                            // 3. Multi-shipment remaining PO quantity validation
                            if (selectedOrderId != null && poRemainingItems.isNotEmpty()) {
                                val groupedByProduct = drafts.groupBy { it.productId }
                                for ((prodId, prodDrafts) in groupedByProduct) {
                                    val totalDraftQty = prodDrafts.sumOf { it.quantity }
                                    val poRem = poRemainingItems.find { it.poItem.productId == prodId }
                                    if (poRem != null && totalDraftQty > poRem.remainingQty) {
                                        val pName = poRem.product?.nameEn ?: prodId
                                        errorMessage = if (isBangla) {
                                            "পণ্য $pName এর জন্য মোট পরিমাণ ($totalDraftQty পিস) অর্ডারের বাকি পরিমাণের (${poRem.remainingQty} পিস) চেয়ে বেশি!"
                                        } else {
                                            "Total shipment quantity ($totalDraftQty pcs) exceeds remaining unshipped quantity (${poRem.remainingQty} pcs) for $pName!"
                                        }
                                        return@Button
                                    }
                                }
                            }

                            // 4. Submit to ViewModel
                            isSubmitting = true
                            onConfirm(
                                selectedSupplierId,
                                selectedOrderId,
                                selectedMethod,
                                origin,
                                destination,
                                trackingRef,
                                shippingLine,
                                drafts,
                                notes,
                                { shipmentId, shipmentNumber ->
                                    isSubmitting = false
                                    onDismiss()
                                },
                                { err ->
                                    isSubmitting = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isBangla) "চালান তৈরি করুন" else "Create Shipment",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
