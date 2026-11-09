package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
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
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PurchaseOrderDetailDialog(
    order: PurchaseOrder,
    supplier: Supplier?,
    items: List<PurchaseOrderItem>,
    products: List<Product>,
    payments: List<SupplierPayment>,
    shipments: List<ImportShipment>,
    cartons: List<ShipmentCarton> = emptyList(),
    costItems: List<ImportCostItem> = emptyList(),
    receipts: List<WarehouseReceipt> = emptyList(),
    damageRecords: List<DamageRecord> = emptyList(),
    shortageRecords: List<ShortageRecord> = emptyList(),
    additionalCosts: List<AdditionalProductCost> = emptyList(),
    completionRecord: PurchaseCompletionRecord? = null,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmPo: (orderId: String) -> Unit,
    onMakePayment: (order: PurchaseOrder) -> Unit,
    onCreateShipment: (order: PurchaseOrder) -> Unit,
    onAddCarton: (order: PurchaseOrder) -> Unit = {},
    onAddCostItem: (order: PurchaseOrder) -> Unit = {},
    onFinalizeActualCost: (orderId: String) -> Unit = {},
    onReceiveProducts: (order: PurchaseOrder) -> Unit = {},
    onPostReceiving: (receiptId: String) -> Unit = {},
    onAddAdditionalCost: (order: PurchaseOrder) -> Unit = {},
    onCompletePo: (order: PurchaseOrder) -> Unit = {},
    onSharePdf: (order: PurchaseOrder) -> Unit,
    onCancelPo: (orderId: String, reason: String?) -> Unit,
    onDuplicatePo: ((orderId: String) -> Unit)? = null,
    onSelectShipment: ((ImportShipment) -> Unit)? = null,
    onDeleteCarton: ((orderId: String, cartonNumber: String) -> Unit)? = null,
    onDeleteCostItem: ((costItemId: String) -> Unit)? = null,
    onUpdatePacking: ((cartons: Int, grossWeight: String, netWeight: String, cbm: String) -> Unit)? = null,
    onUpdatePoItem: ((itemId: String, quantity: Int, unitPriceMinor: Long, cartonQuantity: Int, grossWeightKg: String) -> Unit)? = null,
    onDeletePoItem: ((itemId: String) -> Unit)? = null
) {
    val isBangla = language == AppLanguage.BANGLA
    val productMap = remember(products) { products.associateBy { it.id } }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showEditPackingDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PurchaseOrderItem?>(null) }
    var deletingItemId by remember { mutableStateOf<String?>(null) }
    var deletingCartonNumber by remember { mutableStateOf<String?>(null) }
    var deletingCostItemId by remember { mutableStateOf<String?>(null) }

    val paymentTerm = remember(order.notes) {
        val notes = order.notes ?: ""
        when {
            notes.contains("100% Advance", ignoreCase = true) -> PaymentTermType.HUNDRED_PERCENT_ADVANCE
            notes.contains("50% Advance", ignoreCase = true) -> PaymentTermType.FIFTY_FIFTY
            notes.contains("Full Due", ignoreCase = true) -> PaymentTermType.FULL_DUE
            notes.contains("Custom", ignoreCase = true) -> PaymentTermType.CUSTOM_PARTIAL
            else -> PaymentTermType.THIRTY_SEVENTY
        }
    }
    val rateBd = BigDecimal(order.exchangeRateToBdt.ifBlank { "17.00" })
    val scheduledInstallments = remember(paymentTerm, order.totalAmountMinor, rateBd) {
        PaymentTermsCalculator.calculateSchedule(paymentTerm, order.totalAmountMinor, rateBd)
    }

    val remainingBalanceMinor = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
    val remainingBalanceBdtMinor = (order.totalBdtAmountMinor - order.paidBdtAmountMinor).coerceAtLeast(0L)

    val distinctRecordedCartons = remember(cartons) { cartons.map { it.cartonNumber }.distinct() }
    val recordedCartonsCount = distinctRecordedCartons.size
    val displayCartons = if (recordedCartonsCount > 0) recordedCartonsCount else order.totalCartons

    val recordedGrossWeight = remember(cartons) {
        cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, list ->
            val wStr = list.firstOrNull()?.cartonGrossWeightKg?.ifBlank { list.firstOrNull()?.grossWeightKg?.ifBlank { "0" } ?: "0" } ?: "0"
            acc.add(BigDecimal(wStr))
        }
    }
    val displayGrossWeight = if (recordedGrossWeight > BigDecimal.ZERO) {
        recordedGrossWeight.setScale(3, RoundingMode.HALF_UP).toPlainString()
    } else {
        BigDecimal(order.totalGrossWeightKg.ifBlank { "0" }).setScale(3, RoundingMode.HALF_UP).toPlainString()
    }

    val recordedNetWeight = remember(cartons) {
        cartons.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(BigDecimal(c.netWeightKg.ifBlank { "0" }))
        }
    }
    val displayNetWeight = if (recordedNetWeight > BigDecimal.ZERO) {
        recordedNetWeight.setScale(3, RoundingMode.HALF_UP).toPlainString()
    } else {
        BigDecimal(order.totalNetWeightKg.ifBlank { "0" }).setScale(3, RoundingMode.HALF_UP).toPlainString()
    }

    val recordedCbm = remember(cartons) {
        cartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, list ->
            acc.add(BigDecimal(list.firstOrNull()?.cbm?.ifBlank { "0" } ?: "0"))
        }
    }
    val displayCbmBd = if (recordedCbm > BigDecimal.ZERO) {
        recordedCbm
    } else {
        BigDecimal(order.totalCbm.ifBlank { "0" })
    }
    val displayCbmFormatted = displayCbmBd.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    val totalOrderedQuantity = items.sumOf { it.quantity }
    val totalReceivedQuantity = items.sumOf { it.receivedQuantity }
    val receivingProgress = if (totalOrderedQuantity > 0) {
        (totalReceivedQuantity.toFloat() / totalOrderedQuantity.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val paymentProgress = if (order.totalAmountMinor > 0) {
        (order.paidAmountMinor.toFloat() / order.totalAmountMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val canCancel = order.status != PurchaseOrderStatus.CANCELLED.name &&
            payments.isEmpty() &&
            shipments.isEmpty() &&
            receipts.isEmpty()

    val isCostFinalized = order.isActualCostFinalized || order.status == PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ERPBackground)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.poNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            PoStatusBadge(statusStr = order.status, isBangla = isBangla)
                        }
                        Text(
                            text = if (isBangla) "চীন ক্রয়াদেশ • ব্যাচ ও ল্যান্ডেড কস্ট ব্যবস্থাপনা" else "China Purchase Order • Batch & Landed Cost Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSharePdf(order) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share PO PDF",
                                tint = SecondaryTeal
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Completion Banner (if completed)
                    if (completionRecord != null || order.status == PurchaseOrderStatus.COMPLETED.name) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isBangla) "ক্রয়াদেশ সম্পূর্ণ করা হয়েছে (Order Completed)" else "Purchase Order Completed",
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (completionRecord != null) {
                                            Text(
                                                text = "Completed on ${Formatters.formatDate(completionRecord.completedAt, language)} • Disposition: ${completionRecord.unreceivedDisposition ?: "None"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            if (!completionRecord.notes.isNullOrBlank()) {
                                                Text(
                                                    text = "Audit Note: ${completionRecord.notes}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Supplier & FX Profile Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla) "সরবরাহকারী ও বিনিময় হার" else "Supplier & Foreign Exchange",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = supplier?.companyName ?: order.supplierId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${supplier?.country ?: "China"} • ${supplier?.phone ?: "N/A"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "1 ${order.currency} = ${order.exchangeRateToBdt} BDT",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                        Text(
                                            text = Formatters.formatDate(order.orderDate, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }

                                if (!order.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${if (isBangla) "মন্তব্য: " else "Notes: "}${order.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 3. Financial & Payment Summary Card with Installment Schedule
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isBangla) "পেমেন্ট ও বকেয়া সারসংক্ষেপ" else "Payment & Balance Summary",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavy
                                    )
                                    Text(
                                        text = "${(paymentProgress * 100).toInt()}% ${if (isBangla) "পরিশোধিত" else "Paid"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (paymentProgress >= 1f) StatusSuccess else PrimaryPurple
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { paymentProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (paymentProgress >= 1f) StatusSuccess else PrimaryPurple,
                                    trackColor = ERPBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Total PO Value
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "মোট মূল্য" else "Total PO Value",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(order.totalBdtAmountMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Total Paid
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isBangla) "পরিশোধিত" else "Total Paid",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(order.paidAmountMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(order.paidBdtAmountMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Balance Due
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (isBangla) "বকেয়া" else "Balance Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(remainingBalanceMinor)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingBalanceMinor > 0) StatusError else StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(remainingBalanceBdtMinor, CurrencyCode.BDT, language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (remainingBalanceBdtMinor > 0) StatusError else StatusSuccess,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Scheduled Payment Terms Breakdown
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${if (isBangla) "পেমেন্ট শর্ত: " else "Payment Terms: "}${if (isBangla) paymentTerm.labelBn else paymentTerm.labelEn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                                scheduledInstallments.forEach { inst ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isBangla) inst.titleBn else inst.titleEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${order.currency} ${Formatters.formatMinorUnit(inst.amountMinor)} (${Formatters.formatMoney(inst.bdtAmountMinor, CurrencyCode.BDT, language)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                // Linked payments list if any
                                if (payments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isBangla) "পরিশোধের ইতিহাস (${payments.size}):" else "Payment History (${payments.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    payments.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${p.paymentNumber} • ${p.paymentMethod}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = Formatters.formatDate(p.paymentDate, language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${p.currency} ${Formatters.formatMinorUnit(p.amountMinor)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                                Text(
                                                    text = Formatters.formatMoney(p.bdtAmountMinor, CurrencyCode.BDT, language),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Line Items Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isBangla) "পণ্য তালিকা (${items.size}):" else "Ordered Items (${items.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                items.forEachIndexed { idx, item ->
                                    val product = productMap[item.productId]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${product?.nameEn ?: item.productId}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "SKU: ${product?.baseSku ?: "N/A"} • ${item.quantity} units @ ${order.currency} ${Formatters.formatMinorUnit(item.unitPriceMinor)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            val itemGrossWeightBd = BigDecimal(item.grossWeightKg.ifBlank { "0" })
                                            if (item.cartonQuantity > 0 || itemGrossWeightBd > BigDecimal.ZERO) {
                                                Text(
                                                    text = "Packing: ${item.cartonQuantity} Cartons • ${item.grossWeightKg} KG Gross",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SecondaryTeal
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${order.currency} ${Formatters.formatMinorUnit(item.totalAmountMinor)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                            Text(
                                                text = Formatters.formatMoney(item.totalBdtAmountMinor, CurrencyCode.BDT, language),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                            if (order.status == PurchaseOrderStatus.DRAFT.name || order.status == PurchaseOrderStatus.CONFIRMED.name) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { editingItem = item },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Item",
                                                            tint = PrimaryPurple,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { deletingItemId = item.id },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Item",
                                                            tint = StatusError,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (idx < items.size - 1) {
                                        HorizontalDivider(color = ERPDivider)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Carton & Packing Management Card (Single + Mixed, Auto Weight Allocation)
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "কার্টন ও প্যাকিং ব্যবস্থাপনা" else "Carton & Packing Management",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = if (isBangla) "সিঙ্গেল ও মিক্সড কার্টন • স্বয়ংক্রিয় ওজন বিভাজন" else "Single & Mixed Cartons • Auto Weight Allocation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { showEditPackingDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBangla) "প্যাকিং এডিট" else "Edit Packing",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Button(
                                                onClick = { onAddCarton(order) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBangla) "+ কার্টন" else "+ Carton",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Packing Summary 2x2 Responsive Grid
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PackingMetricBox(
                                            label = if (isBangla) "মোট কার্টন" else "Total Cartons",
                                            primaryValue = "${displayCartons} CTN",
                                            subtitle = if (order.totalCartons > 0 && order.totalCartons != displayCartons) {
                                                "${recordedCartonsCount} of ${order.totalCartons} Planned"
                                            } else if (recordedCartonsCount > 0) {
                                                "$recordedCartonsCount Entered"
                                            } else "Planned Order Count",
                                            color = DeepNavy,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PackingMetricBox(
                                            label = if (isBangla) "গ্রস ওজন" else "Gross Weight",
                                            primaryValue = "${displayGrossWeight} KG",
                                            subtitle = "Shipping Basis",
                                            color = PrimaryPurple,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PackingMetricBox(
                                            label = if (isBangla) "নেট ওজন" else "Net Weight",
                                            primaryValue = "${displayNetWeight} KG",
                                            subtitle = "Product Mass",
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        PackingMetricBox(
                                            label = if (isBangla) "মোট সিবিএম" else "Total CBM",
                                            primaryValue = "${displayCbmFormatted} m³",
                                            subtitle = "Calculated Volume",
                                            color = SecondaryTeal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (cartons.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "যুক্তকৃত কার্টন তালিকা (${cartons.size}):" else "Entered Cartons (${cartons.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        cartons.forEach { c ->
                                            val p = productMap[c.productId]
                                            val pName = if (isBangla && !p?.nameBn.isNullOrBlank()) p?.nameBn!! else p?.nameEn ?: c.productId
                                            CartonItemCard(
                                                carton = c,
                                                productName = pName,
                                                onDelete = if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                                    { deletingCartonNumber = c.cartonNumber }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Landing Cost & Gross Weight Shipping Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "ল্যান্ডেড কস্ট ও শিপিং ক্যালকুলেশন" else "Landed Cost & Shipping Calculation",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = if (isBangla) "গ্রস ওয়েট ভিত্তিক শিপিং • ক্যাটাগরিভিত্তিক ব্যয় বিভাজন" else "Gross Weight Shipping • Category Allocation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onAddCostItem(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ খরচ যোগ" else "+ Add Cost",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Shipping Cost Calculator Display: Gross Weight * Rate/KG
                                val shippingRate = BigDecimal(order.shippingRatePerKgBdt.ifBlank { "120" })
                                val poGrossWeightBd = BigDecimal(displayGrossWeight.ifBlank { "0" })
                                val calculatedShipping = poGrossWeightBd.multiply(shippingRate).setScale(2, RoundingMode.HALF_UP)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBangla) "শিপিং খরচ হিসাব (গ্রস ওজন)" else "Shipping Cost Formula (Gross Weight)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavy
                                            )
                                            Text(
                                                text = "${displayGrossWeight} KG × ৳${shippingRate}/KG",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "৳ $calculatedShipping BDT",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                    }
                                }

                                if (costItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "নথিভুক্ত ল্যান্ডেড খরচ (${costItems.size}):" else "Recorded Landed Costs (${costItems.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        costItems.forEach { cost ->
                                            CostItemCard(
                                                cost = cost,
                                                language = language,
                                                isBangla = isBangla,
                                                onDelete = if (!order.isActualCostFinalized && order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                                    { deletingCostItemId = cost.id }
                                                } else null
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = ERPDivider)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Finalize Actual Landing Cost Action
                                if (isCostFinalized) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট ফাইনাল করা হয়েছে!" else "Actual Landing Cost Finalized!",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                                Text(
                                                    text = if (isBangla) "স্টক ইনভেন্টরি পোস্টিং ও FIFO লেয়ার আনলকড" else "Inventory Posting & FIFO Layers Unlocked",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isBangla) "প্রকৃত ব্যয় ফাইনাল করার পূর্বে স্টক পোস্টিং ব্লক থাকবে।" else "Stock posting is strictly BLOCKED until Actual Landing Cost is finalized.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = StatusError
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { onFinalizeActualCost(order.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট ফাইনাল করুন (Finalize Actual Cost)" else "Finalize Actual Landing Cost",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Warehouse Receiving Progress & Stock Posting Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "ওয়্যারহাউস মালামাল গ্রহণ ও স্টক পোস্টিং" else "Warehouse Receiving & Stock Posting",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = if (isBangla) "মেথড এ (গণনা) ও বি (কার্টনভিত্তিক) • আংশিক গ্রহণ" else "Method A (Count) & B (Carton) • Partial Receiving",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onReceiveProducts(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Warehouse, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ পণ্য গ্রহণ" else "Receive Products",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBangla) "ইনভেন্টরিতে পোস্টকৃত: $totalReceivedQuantity / $totalOrderedQuantity একক" else "Posted to Stock: $totalReceivedQuantity / $totalOrderedQuantity units",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else PrimaryPurple
                                    )
                                    Text(
                                        text = "${(receivingProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else SecondaryTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { receivingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (totalReceivedQuantity >= totalOrderedQuantity && totalOrderedQuantity > 0) StatusSuccess else SecondaryTeal,
                                    trackColor = ERPBackground
                                )

                                val draftReceiptsCount = receipts.count { it.status == "DRAFT" }
                                if (draftReceiptsCount > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isBangla) "⚠️ $draftReceiptsCount টি ড্রাফট চালান অপেক্ষমান (স্টক পোস্টিংয়ের জন্য ল্যান্ডেড কস্ট ফাইনাল আবশ্যক)" else "⚠️ $draftReceiptsCount draft receipt(s) pending final cost & stock posting",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Receipts List with Post Action
                                if (receipts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBangla) "ওয়্যারহাউস চালান তালিকা (${receipts.size}):" else "Warehouse Receipts (${receipts.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        receipts.forEach { rec ->
                                            WarehouseReceiptCard(
                                                receipt = rec,
                                                isCostFinalized = isCostFinalized,
                                                isBangla = isBangla,
                                                onPostReceiving = onPostReceiving
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. Damage & Shortage Discrepancy Accounting Card
                    if (damageRecords.isNotEmpty() || shortageRecords.isNotEmpty()) {
                        item {
                            ERPCard {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (isBangla) "ক্ষতিগ্রস্ত ও ঘাটতি পণ্যের হিসাব (Write-off & Claims)" else "Damage & Shortage Discrepancy Accounting",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavy
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Damage records: Dr 5050 Cr 1220
                                    damageRecords.forEach { dmg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Damage Write-off: ${dmg.damagedQuantity} pcs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusError
                                                )
                                                Text(
                                                    text = "Dr. 5050 Damaged Write-Off / Cr. 1220 In-Transit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "৳ ${Formatters.formatMinorUnit(dmg.totalLossMinor)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusError
                                            )
                                        }
                                    }

                                    // Shortage records: Dr 1050 Cr 1220
                                    shortageRecords.forEach { sht ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Shortage Claim: ${sht.missingQuantity} pcs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusWarning
                                                )
                                                Text(
                                                    text = "Dr. 1050 Supplier Claims Receivable / Cr. 1220 In-Transit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "৳ ${Formatters.formatMinorUnit(sht.totalAmountMinor)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusWarning
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. Post-Arrival Additional Costs Card
                    item {
                        ERPCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla) "অতিরিক্ত পরিচালন ব্যয়" else "Post-Arrival Operational Costs",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        Text(
                                            text = if (isBangla) "মূলধনীকরণ (অ্যাসেট) বনাম সাময়িক খরচ (P&L)" else "Capitalized to Inventory vs Period Expense",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name) {
                                        Button(
                                            onClick = { onAddAdditionalCost(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBangla) "+ খরচ" else "+ Add Cost",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (additionalCosts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        additionalCosts.forEach { ac ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = ac.category.replace("_", " "),
                                                            fontWeight = FontWeight.Bold,
                                                            color = DeepNavy,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    if (ac.isCapitalizable) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (ac.isCapitalizable) "CAPITALIZED" else "PERIOD EXPENSE",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (ac.isCapitalizable) Color(0xFF047857) else Color(0xFFB45309)
                                                            )
                                                        }
                                                    }
                                                    if (ac.description.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(ac.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "খরচের পরিমাণ:" else "Recorded Amount:",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                        Text(
                                                            text = "৳ ${Formatters.formatMinorUnit(ac.amountMinor)} BDT",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PrimaryPurple
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
                }

                // Bottom Action Bar - Responsive 2-Row Layout
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hasPrimaryActions = order.status == PurchaseOrderStatus.DRAFT.name ||
                            (remainingBalanceMinor > 0 && order.status != PurchaseOrderStatus.CANCELLED.name) ||
                            (order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name)

                        // Row 1: Primary Action Buttons (only rendered if actionable)
                        if (hasPrimaryActions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (order.status == PurchaseOrderStatus.DRAFT.name) {
                                    Button(
                                        onClick = { onConfirmPo(order.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "ক্রয়াদেশ নিশ্চিত করুন (Confirm PO)" else "Confirm Purchase Order",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    val showPaymentBtn = remainingBalanceMinor > 0 && order.status != PurchaseOrderStatus.CANCELLED.name
                                    val showCompleteBtn = order.status != PurchaseOrderStatus.COMPLETED.name && order.status != PurchaseOrderStatus.CANCELLED.name

                                    if (showPaymentBtn) {
                                        Button(
                                            onClick = { onMakePayment(order) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "পেমেন্ট পরিশোধ" else "Make Payment",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (showCompleteBtn) {
                                        Button(
                                            onClick = { onCompletePo(order) },
                                            modifier = (if (showPaymentBtn) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                                .defaultMinSize(minHeight = 44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "ক্রয়াদেশ সমাপ্ত" else "Complete PO",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 2: Secondary Utility Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onDuplicatePo != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDuplicatePo(order.id)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "কপি করুন" else "Duplicate",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (canCancel) {
                                OutlinedButton(
                                    onClick = { showCancelConfirmDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বাতিল" else "Cancel",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!hasPrimaryActions) {
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বন্ধ করুন" else "Close",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "বন্ধ করুন" else "Close",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel PO Confirmation Alert with Reason input
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ক্রয়াদেশ বাতিল নিশ্চিতকরণ" else "Confirm Cancel Purchase Order",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla)
                            "আপনি কি নিশ্চিত যে ক্রয়াদেশ ${order.poNumber} বাতিল করতে চান? এটি সরবরাহকারীর বকেয়া থেকে সংশ্লিষ্ট পরিমাণ প্রত্যাহার করবে।"
                        else
                            "Are you sure you want to cancel Purchase Order ${order.poNumber}? This will revert any supplier payable commitment."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text(if (isBangla) "বাতিলের কারণ (বাধ্যতামূলক নয়)" else "Cancellation Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelPo(order.id, cancelReason.ifBlank { null })
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, বাতিল করুন" else "Yes, Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(text = if (isBangla) "না" else "Dismiss")
                }
            }
        )
    }

    // Edit Packing Dialog (Allows resetting or modifying cartons e.g. 5 CTN -> 0)
    if (showEditPackingDialog) {
        var editCartonsStr by remember(order) { mutableStateOf(order.totalCartons.toString()) }
        var editGrossWeightStr by remember(order) { mutableStateOf(order.totalGrossWeightKg) }
        var editNetWeightStr by remember(order) { mutableStateOf(order.totalNetWeightKg) }
        var editCbmStr by remember(order) { mutableStateOf(order.totalCbm) }

        AlertDialog(
            onDismissRequest = { showEditPackingDialog = false },
            title = {
                Text(
                    text = if (isBangla) "প্যাকিং বিবরণ ও কার্টন সম্পাদনা" else "Edit Packing & Cartons",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "মোট কার্টন সংখ্যা বা প্যাকিং পরিমাপ পরিবর্তন করুন (যেমন ৫ কার্টন সরাতে ০ দিন):" 
                               else "Update total cartons or packing measurements (e.g. enter 0 to reset cartons):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editCartonsStr,
                        onValueChange = { editCartonsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "মোট কার্টন (Cartons)" else "Total Cartons") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGrossWeightStr,
                        onValueChange = { editGrossWeightStr = it },
                        label = { Text(if (isBangla) "মোট গ্রস ওজন (KG)" else "Total Gross Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNetWeightStr,
                        onValueChange = { editNetWeightStr = it },
                        label = { Text(if (isBangla) "মোট নেট ওজন (KG)" else "Total Net Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCbmStr,
                        onValueChange = { editCbmStr = it },
                        label = { Text(if (isBangla) "মোট সিবিএম (CBM)" else "Total CBM (m³)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ctn = editCartonsStr.toIntOrNull() ?: 0
                        onUpdatePacking?.invoke(ctn, editGrossWeightStr.trim(), editNetWeightStr.trim(), editCbmStr.trim())
                        showEditPackingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "সংরক্ষণ করুন" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPackingDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Edit PO Line Item Dialog (Allows modifying quantity from 100 pcs, unit price, cartons)
    editingItem?.let { item ->
        val product = productMap[item.productId]
        val prodName = if (isBangla && !product?.nameBn.isNullOrBlank()) product?.nameBn!! else product?.nameEn ?: item.productId
        var editQtyStr by remember(item) { mutableStateOf(item.quantity.toString()) }
        var editPriceStr by remember(item) {
            val priceMajor = BigDecimal(item.unitPriceMinor).movePointLeft(2).stripTrailingZeros().toPlainString()
            mutableStateOf(priceMajor)
        }
        var editCartonQtyStr by remember(item) { mutableStateOf(item.cartonQuantity.toString()) }
        var editGrossWeightStr by remember(item) { mutableStateOf(item.grossWeightKg) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = if (isBangla) "পণ্য আইটেম পরিবর্তন: $prodName" else "Edit Item: $prodName",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "অর্ডার পরিমাণ (pcs) ও কার্টন সংখ্যা পরিবর্তন করুন:" 
                               else "Update ordered quantity (pcs) and carton count:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = editQtyStr,
                        onValueChange = { editQtyStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "পরিমাণ (Pieces)" else "Quantity (Pieces)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPriceStr,
                        onValueChange = { editPriceStr = it },
                        label = { Text(if (isBangla) "ইউনিট মূল্য (${order.currency})" else "Unit Price (${order.currency})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCartonQtyStr,
                        onValueChange = { editCartonQtyStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "কার্টন সংখ্যা (Cartons)" else "Carton Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGrossWeightStr,
                        onValueChange = { editGrossWeightStr = it },
                        label = { Text(if (isBangla) "গ্রস ওজন (KG)" else "Gross Weight (KG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = editQtyStr.toIntOrNull() ?: item.quantity
                        val priceBd = editPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val priceMinor = priceBd.movePointRight(2).toLong()
                        val ctn = editCartonQtyStr.toIntOrNull() ?: item.cartonQuantity
                        onUpdatePoItem?.invoke(item.id, qty, priceMinor, ctn, editGrossWeightStr.trim())
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "সংরক্ষণ করুন" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete PO Line Item Confirmation Alert
    deletingItemId?.let { itemId ->
        val item = items.find { it.id == itemId }
        val product = productMap[item?.productId]
        val prodName = if (isBangla && !product?.nameBn.isNullOrBlank()) product?.nameBn!! else product?.nameEn ?: item?.productId ?: "Item"
        AlertDialog(
            onDismissRequest = { deletingItemId = null },
            title = {
                Text(
                    text = if (isBangla) "পণ্য মুছে ফেলতে চান?" else "Delete Item?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে '$prodName' পিও থেকে মুছে ফেলতে চান?" 
                           else "Are you sure you want to remove '$prodName' from this Purchase Order?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePoItem?.invoke(itemId)
                        deletingItemId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItemId = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Carton Confirmation Alert
    deletingCartonNumber?.let { ctnNum ->
        AlertDialog(
            onDismissRequest = { deletingCartonNumber = null },
            title = {
                Text(
                    text = if (isBangla) "কার্টন মুছে ফেলতে চান?" else "Delete Carton?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে কার্টন '$ctnNum' মুছে ফেলতে চান?" 
                           else "Are you sure you want to delete carton '$ctnNum'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCarton?.invoke(order.id, ctnNum)
                        deletingCartonNumber = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCartonNumber = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Cost Item Confirmation Alert
    deletingCostItemId?.let { cId ->
        val cost = costItems.find { it.id == cId }
        val costName = cost?.costCategory?.replace("_", " ") ?: "Cost Item"
        AlertDialog(
            onDismissRequest = { deletingCostItemId = null },
            title = {
                Text(
                    text = if (isBangla) "ব্যয় আইটেম মুছুন" else "Delete Cost Item?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিত যে ব্যয় '$costName' মুছে ফেলতে চান?" 
                           else "Are you sure you want to delete cost item '$costName'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCostItem?.invoke(cId)
                        deletingCostItemId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCostItemId = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun PoStatusBadge(statusStr: String, isBangla: Boolean) {
    val (bgColor, textColor, label) = when (statusStr) {
        PurchaseOrderStatus.DRAFT.name -> Triple(
            Color(0xFFE0E0E0),
            Color(0xFF424242),
            if (isBangla) PurchaseOrderStatus.DRAFT.labelBn else PurchaseOrderStatus.DRAFT.labelEn
        )
        PurchaseOrderStatus.CONFIRMED.name -> Triple(
            SecondaryTeal.copy(alpha = 0.15f),
            SecondaryTeal,
            if (isBangla) PurchaseOrderStatus.CONFIRMED.labelBn else PurchaseOrderStatus.CONFIRMED.labelEn
        )
        PurchaseOrderStatus.PARTIALLY_PAID.name -> Triple(
            StatusWarning.copy(alpha = 0.15f),
            Color(0xFFD97706),
            if (isBangla) PurchaseOrderStatus.PARTIALLY_PAID.labelBn else PurchaseOrderStatus.PARTIALLY_PAID.labelEn
        )
        PurchaseOrderStatus.PAID.name -> Triple(
            StatusSuccess.copy(alpha = 0.15f),
            StatusSuccess,
            if (isBangla) PurchaseOrderStatus.PAID.labelBn else PurchaseOrderStatus.PAID.labelEn
        )
        PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name -> Triple(
            PrimaryPurple.copy(alpha = 0.15f),
            PrimaryPurple,
            if (isBangla) "কস্ট ফাইনাল" else "Cost Finalized"
        )
        PurchaseOrderStatus.SHIPPED.name -> Triple(
            PrimaryPurple.copy(alpha = 0.15f),
            PrimaryPurple,
            if (isBangla) PurchaseOrderStatus.SHIPPED.labelBn else PurchaseOrderStatus.SHIPPED.labelEn
        )
        PurchaseOrderStatus.COMPLETED.name -> Triple(
            StatusSuccess.copy(alpha = 0.2f),
            StatusSuccess,
            if (isBangla) PurchaseOrderStatus.COMPLETED.labelBn else PurchaseOrderStatus.COMPLETED.labelEn
        )
        PurchaseOrderStatus.CANCELLED.name -> Triple(
            StatusError.copy(alpha = 0.15f),
            StatusError,
            if (isBangla) PurchaseOrderStatus.CANCELLED.labelBn else PurchaseOrderStatus.CANCELLED.labelEn
        )
        else -> Triple(Color.LightGray, Color.DarkGray, statusStr)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun PackingMetricBox(
    label: String,
    primaryValue: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CostItemCard(
    cost: ImportCostItem,
    language: AppLanguage,
    isBangla: Boolean,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cost.costCategory.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (cost.isEstimated) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (cost.isEstimated) "ESTIMATED" else "ACTUAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cost.isEstimated) Color(0xFFB45309) else Color(0xFF047857)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cost.allocationMethod.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Cost Item",
                                tint = StatusError,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (cost.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cost.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "বরাদ্দকৃত খরচ:" else "Allocated Cost:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${cost.currency} ${Formatters.formatMinorUnit(cost.amountMinor)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${Formatters.formatMoney(cost.bdtAmountMinor, CurrencyCode.BDT, language)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CartonItemCard(
    carton: ShipmentCarton,
    productName: String,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = carton.cartonNumber,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (carton.isMixedCarton) SecondaryTeal.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (carton.isMixedCarton) "MIXED CARTON" else "SINGLE PRODUCT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (carton.isMixedCarton) SecondaryTeal else PrimaryPurple
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${carton.quantity} units",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Carton",
                                tint = StatusError,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = productName,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gross: ${carton.grossWeightKg} KG • Net: ${carton.netWeightKg} KG",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryPurple
                )
                Text(
                    text = "${carton.cbm} CBM",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryTeal
                )
            }
        }
    }
}

@Composable
private fun WarehouseReceiptCard(
    receipt: WarehouseReceipt,
    isCostFinalized: Boolean,
    isBangla: Boolean,
    onPostReceiving: (String) -> Unit
) {
    val isPosted = receipt.status == "POSTED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Receipt Number on Left, Status Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.receiptNumber,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    style = MaterialTheme.typography.titleSmall
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (isPosted) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPosted) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isPosted) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPosted) {
                                if (isBangla) "স্টকে পোস্টকৃত" else "POSTED TO STOCK"
                            } else {
                                if (isBangla) "খসড়া (অপেক্ষমাণ)" else "DRAFT (PENDING)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPosted) Color(0xFF047857) else Color(0xFFB45309),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-row: Receiving Method Chip + Received By
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isBangla) {
                            if (receipt.receivingMethod == "CARTON_WISE") "পদ্ধতি: কার্টুনভিত্তিক" else "পদ্ধতি: গণনা (Physical Count)"
                        } else {
                            "Method: " + receipt.receivingMethod.replace("_", " ")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepNavy,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (receipt.receivedBy.isNotBlank()) {
                    Text(
                        text = receipt.receivedBy,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!receipt.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = receipt.notes ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (receipt.hasDiscrepancies) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (receipt.discrepancyApproved) {
                            if (isBangla) "ঘাটতি/ক্ষতি অনুমোদন সম্পন্ন" else "Discrepancies Approved"
                        } else {
                            if (isBangla) "ঘাটতি/ক্ষতি অনুমোদনের অপেক্ষায়" else "Discrepancies Pending Approval"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            }

            // Post to Stock Button (full width, comfortable height)
            if (receipt.status == "DRAFT") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onPostReceiving(receipt.id) },
                    enabled = isCostFinalized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCostFinalized) PrimaryPurple else Color.LightGray
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCostFinalized) Icons.Default.Inventory2 else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCostFinalized) {
                            if (isBangla) "স্টক ইনভেন্টরিতে পোস্ট করুন" else "Post Receipt to Inventory Stock"
                        } else {
                            if (isBangla) "পোস্টিং লক করা (প্রথমে ল্যান্ডেড কস্ট ফাইনাল করুন)" else "Posting Locked (Finalize Landed Cost First)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
