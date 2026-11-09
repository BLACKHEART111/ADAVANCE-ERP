package com.pocketdev.advance_erp.feature.inventory.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.pocketdev.advance_erp.data.dao.InventoryReconciliationItemWithDetails
import com.pocketdev.advance_erp.data.entity.InventoryReconciliation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryReconciliationView(
    reconciliation: InventoryReconciliation?,
    items: List<InventoryReconciliationItemWithDetails>,
    isReconciling: Boolean,
    language: AppLanguage,
    currency: CurrencyCode,
    onRunReconciliation: () -> Unit,
    onExportPdf: () -> Unit
) {
    var showOnlyMismatches by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top Action Bar: Run Audit & Export PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "ঐতিহাসিক মজুদ পুনর্মিলন নিরীক্ষা" else "Historical Cost Reconciliation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "5-way ledger audit: Stock Balance vs Movements vs FIFO vs Valuation vs Account 1210",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reconciliation != null) {
                    ERPActionButton(
                        text = "PDF",
                        onClick = onExportPdf,
                        icon = Icons.Default.PictureAsPdf,
                        variant = ERPButtonVariant.OUTLINED
                    )
                }

                ERPActionButton(
                    text = if (isReconciling) "Auditing..." else "Run Audit",
                    onClick = onRunReconciliation,
                    icon = if (isReconciling) null else Icons.Default.Refresh,
                    enabled = !isReconciling
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (reconciliation == null) {
            ERPEmptyState(
                icon = Icons.Default.Refresh,
                title = "No Reconciliation Audit Yet",
                message = "Click 'Run Audit' to reconcile stock balances, movements ledger, FIFO layers, and Account 1210."
            )
        } else {
            val isMatched = reconciliation.status == "MATCHED"

            // Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMatched) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        if (isMatched) StatusSuccess else StatusError,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isMatched) StatusSuccess else StatusError,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMatched) "ALL 5 BALANCES MATCHED PERFECTLY" else "DISCREPANCY DETECTED ACROSS BALANCES",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatched) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                        val auditDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(reconciliation.createdAt))
                        Text(
                            text = "Ref: ${reconciliation.reconciliationNumber} • Audited at: $auditDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5 Balances Grid
            ERPCard(contentPadding = 12.dp, backgroundColor = Color(0xFFF8FAFC)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "5-Way Audit Comparison",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("1. Stock Balance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                "${reconciliation.totalStockBalanceQty} u",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column {
                            Text("2. Net Movements", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                "${reconciliation.totalMovementQty} u",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column {
                            Text("3. FIFO Layers", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                "${reconciliation.totalFifoRemainingQty} u",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
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
                            Text("4. FIFO Valuation", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                Formatters.formatCurrency(reconciliation.totalFifoValuationMinor, currency, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                        }

                        Column {
                            Text("5. Acc 1210 (Inventory Asset)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                Formatters.formatCurrency(reconciliation.account1210BalanceMinor, currency, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Valuation Diff", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            val diffColor = if (reconciliation.valuationDiscrepancyMinor == 0L) StatusSuccess else StatusError
                            Text(
                                Formatters.formatCurrency(reconciliation.valuationDiscrepancyMinor, currency, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items List Filter & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audited Products (${items.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val mismatchCount = items.count { it.qtyMismatch }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showOnlyMismatches) StatusError else Color(0xFFF1F5F9))
                        .clickable { showOnlyMismatches = !showOnlyMismatches }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (showOnlyMismatches) "Showing Mismatches ($mismatchCount)" else "Show Mismatches Only ($mismatchCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showOnlyMismatches) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayList = if (showOnlyMismatches) items.filter { it.qtyMismatch } else items

            if (displayList.isEmpty()) {
                ERPEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "No Discrepancies",
                    message = "Every audited product line has identical stock balance, movement net and FIFO layer counts."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(displayList, key = { it.id }) { item ->
                        ERPCard(
                            contentPadding = 8.dp,
                            backgroundColor = if (item.qtyMismatch) Color(0xFFFEF2F2) else Color.White,
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = if (item.qtyMismatch) StatusError.copy(alpha = 0.5f) else ERPDivider,
                                shape = RoundedCornerShape(10.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productNameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "SKU: ${item.baseSku} • ${item.warehouseName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    if (item.qtyMismatch && item.details != null) {
                                        Text(
                                            text = item.details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusError,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Bal: ${item.stockBalanceQty} | Mov: ${item.movementNetQty} | FIFO: ${item.fifoRemainingQty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.qtyMismatch) StatusError else TextPrimary
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(item.fifoValuationMinor, currency, language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryTeal
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
