package com.pocketdev.advance_erp.feature.inventory.costhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
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
import com.pocketdev.advance_erp.data.dao.InventoryCostAdjustmentWithDetails
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.domain.repository.ProductCostSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductCostHistoryDialog(
    summary: ProductCostSummary?,
    layers: List<InventoryCostLayer>,
    adjustments: List<InventoryCostAdjustmentWithDetails> = emptyList(),
    language: AppLanguage,
    currency: CurrencyCode,
    onAdjustLayer: ((layer: InventoryCostLayer) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "DEPLETED", "ADJUSTMENTS"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মজুদ ব্যয় ইতিহাস ও ফিফো লেয়ার" else "Inventory Cost History & FIFO Layers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (summary != null) {
                            Text(
                                text = "${summary.productNameEn} (SKU: ${summary.baseSku})",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Metric Cards
                if (summary != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricCard(
                            modifier = Modifier.weight(1f),
                            label = if (language == AppLanguage.BANGLA) "হাতে মজুদ" else "Stock On Hand",
                            value = "${summary.totalRemainingQuantity}",
                            valueColor = TextPrimary,
                            caption = if (language == AppLanguage.BANGLA) "${summary.activeLayerCount} সক্রিয় লেয়ার" else "${summary.activeLayerCount} active layers",
                            captionColor = SecondaryTeal
                        )
                        SummaryMetricCard(
                            modifier = Modifier.weight(1f),
                            label = if (language == AppLanguage.BANGLA) "সর্বশেষ ফিফো খরচ" else "Latest FIFO Cost",
                            value = Formatters.formatCurrency(summary.latestFifoCostMinor, currency, language),
                            valueColor = PrimaryPurple,
                            caption = if (language == AppLanguage.BANGLA) "পরবর্তী বিক্রয়ে" else "Next to deplete",
                            captionColor = TextMuted
                        )
                        SummaryMetricCard(
                            modifier = Modifier.weight(1.2f),
                            label = if (language == AppLanguage.BANGLA) "মোট ফিফো মূল্য" else "Total FIFO Valuation",
                            value = Formatters.formatCurrency(summary.totalValuationMinor, currency, language),
                            valueColor = Color(0xFF0F766E),
                            caption = (if (language == AppLanguage.BANGLA) "গড়: " else "Avg: ") +
                                Formatters.formatCurrency(summary.weightedAverageCostMinor, currency, language),
                            captionColor = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All Layers (${layers.size})",
                        "ACTIVE" to "Active (${layers.count { it.isActive && it.remainingQuantity > 0 }})",
                        "DEPLETED" to "Depleted (${layers.count { !it.isActive || it.remainingQuantity <= 0 }})",
                        "ADJUSTMENTS" to "Adjustments (${adjustments.size})"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryPurple else Color(0xFFF1F5F9))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = ERPDivider)
                Spacer(modifier = Modifier.height(8.dp))

                // Layer List / Adjustment List
                if (selectedFilter == "ADJUSTMENTS") {
                    if (adjustments.isEmpty()) {
                        ERPEmptyState(
                            icon = Icons.Default.Tune,
                            title = "No Cost Adjustments",
                            message = "No landed cost variance adjustments recorded for this product."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(adjustments, key = { it.id }) { adj ->
                                ERPCard(contentPadding = 10.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = adj.adjustmentNumber,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(adj.createdAt))
                                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Reason: ${adj.reason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Old: ${Formatters.formatCurrency(adj.oldUnitCostBdtMinor, currency, language)} → New: ${Formatters.formatCurrency(adj.newUnitCostBdtMinor, currency, language)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextSecondary
                                            )
                                            val isPositive = adj.unitVarianceBdtMinor >= 0
                                            val varSign = if (isPositive) "+" else ""
                                            Text(
                                                "Var: $varSign${Formatters.formatCurrency(adj.unitVarianceBdtMinor, currency, language)}/u",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) StatusSuccess else StatusError
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Inv Allocated: ${Formatters.formatCurrency(adj.inventoryAdjustmentAmountMinor, currency, language)} (${adj.totalRemainingQty} rem)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondaryTeal
                                            )
                                            Text(
                                                "COGS Allocated: ${Formatters.formatCurrency(adj.cogsAdjustmentAmountMinor, currency, language)} (${adj.totalSoldQty} sold)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val filteredLayers = when (selectedFilter) {
                        "ACTIVE" -> layers.filter { it.isActive && it.remainingQuantity > 0 }
                        "DEPLETED" -> layers.filter { !it.isActive || it.remainingQuantity <= 0 }
                        else -> layers
                    }

                    if (filteredLayers.isEmpty()) {
                        ERPEmptyState(
                            icon = Icons.Default.Layers,
                            title = "No Cost Layers Found",
                            message = "No receipt cost layers match the selected filter."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredLayers, key = { it.id }) { layer ->
                                val isActive = layer.isActive && layer.remainingQuantity > 0
                                val receiptDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(layer.receiptDate))

                                ERPCard(
                                    contentPadding = 10.dp,
                                    modifier = Modifier.border(
                                        width = 1.dp,
                                        color = if (isActive) SecondaryTeal.copy(alpha = 0.5f) else ERPDivider,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Header Row: Receipt / Batch & Status
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(
                                                    text = "Batch/Receipt: ${layer.receiptNumber}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Received: $receiptDateStr • Shipment: ${layer.shipmentNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isActive) StatusSuccess.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isActive) "ACTIVE" else "DEPLETED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isActive) StatusSuccess else TextMuted,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                if (isActive && onAdjustLayer != null) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    IconButton(
                                                        onClick = { onAdjustLayer(layer) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Adjust Cost",
                                                            tint = SecondaryTeal,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = ERPDivider.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Cost & Quantity Grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Remaining / Initial", maxLines = 1, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(
                                                    "${layer.remainingQuantity} / ${layer.initialQuantity}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) TextPrimary else TextMuted
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Unit Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(
                                                    Formatters.formatCurrency(layer.unitLandedCostBdtMinor, currency, language),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text("Valuation", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                val layerValuation = layer.remainingQuantity.toLong() * layer.unitLandedCostBdtMinor
                                                Text(
                                                    Formatters.formatCurrency(layerValuation, currency, language),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) Color(0xFF0F766E) else TextMuted
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Foreign Currency & Landed Expense Details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Purchase: ${layer.originalPurchasePriceMinor / 100.0} ${layer.originalCurrency} @ ${layer.exchangeRateToBdt}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                "Logistics: ${Formatters.formatCurrency(layer.allocatedExpensePerUnitBdtMinor, currency, language)}/u",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Close Button
                ERPActionButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CostAdjustmentInputDialog(
    layer: InventoryCostLayer,
    language: AppLanguage,
    currency: CurrencyCode,
    onConfirm: (costLayerId: String, newUnitCostMinor: Long, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCostInput by remember { mutableStateOf((layer.unitLandedCostBdtMinor / 100.0).toString()) }
    var reasonInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val newCostMinor = ((newCostInput.toDoubleOrNull() ?: 0.0) * 100).toLong()
    val variance = newCostMinor - layer.unitLandedCostBdtMinor
    val remainingQty = layer.remainingQuantity
    val soldQty = (layer.initialQuantity - layer.remainingQuantity).coerceAtLeast(0)
    val invImpact = variance * remainingQty
    val cogsImpact = variance * soldQty

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Inventory Landed Cost Adjustment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Batch #${layer.receiptNumber} (${layer.remainingQuantity} remaining, $soldQty already sold)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newCostInput,
                    onValueChange = {
                        newCostInput = it
                        errorText = null
                    },
                    label = { Text("New Landed Unit Cost (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = {
                        reasonInput = it
                        errorText = null
                    },
                    label = { Text("Adjustment Reason / Audit Note") },
                    placeholder = { Text("e.g. Customs duty assessment revised, shipping tariff adjustment") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Variance Split Preview
                ERPCard(contentPadding = 8.dp, backgroundColor = Color(0xFFF8FAFC)) {
                    Column {
                        val isPos = variance >= 0
                        val sign = if (isPos) "+" else ""
                        Text(
                            "Unit Variance: $sign${Formatters.formatCurrency(variance, currency, language)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPos) StatusSuccess else StatusError
                        )
                        Text(
                            "• Inventory Asset (1210): $sign${Formatters.formatCurrency(invImpact, currency, language)} ($remainingQty units)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTeal
                        )
                        Text(
                            "• COGS Expense (5010): $sign${Formatters.formatCurrency(cogsImpact, currency, language)} ($soldQty units sold)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurple
                        )
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorText!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ERPActionButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = ERPButtonVariant.OUTLINED
                    )

                    ERPActionButton(
                        text = "Post Adjustment",
                        onClick = {
                            if (variance == 0L) {
                                errorText = "New unit cost cannot equal current cost"
                                return@ERPActionButton
                            }
                            if (reasonInput.isBlank()) {
                                errorText = "Please enter an adjustment reason"
                                return@ERPActionButton
                            }
                            onConfirm(layer.id, newCostMinor, reasonInput)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun SummaryMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    caption: String,
    captionColor: Color
) {
    ERPCard(
        modifier = modifier,
        contentPadding = 8.dp,
        backgroundColor = Color(0xFFF8FAFC)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(caption, style = MaterialTheme.typography.labelSmall, color = captionColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
