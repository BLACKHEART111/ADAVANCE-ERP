package com.pocketdev.advance_erp.feature.inventory.cogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.FifoConsumptionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CogsTraceabilityView(
    consumptions: List<FifoConsumptionWithDetails>,
    totalCogsMinor: Long,
    language: AppLanguage,
    currency: CurrencyCode
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary Header Card
        ERPCard(contentPadding = 12.dp, backgroundColor = Color(0xFFF8FAFC)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "মোট বিক্রিত পণ্যের ব্যয় (COGS)" else "Total Cost of Goods Sold (COGS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = Formatters.formatCurrency(totalCogsMinor, currency, language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Depletions",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "${consumptions.size} layer consumptions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (consumptions.isEmpty()) {
            ERPEmptyState(
                icon = Icons.Default.ReceiptLong,
                title = "No COGS Depletions Recorded",
                message = "Sales dispatches will automatically record detailed FIFO layer consumptions here."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(consumptions, key = { it.id }) { c ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(c.createdAt))

                    ERPCard(
                        contentPadding = 10.dp,
                        modifier = Modifier.border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.productNameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "SKU: ${c.baseSku} • Warehouse: ${c.warehouseName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = c.referenceType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = ERPDivider)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Depleted from Layer", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        "Batch #${c.receiptNumber ?: "UNKNOWN"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text("Qty Taken", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        "${c.quantity} units",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text("Layer Unit Cost", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        Formatters.formatCurrency(c.unitCostBdtMinor, currency, language),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total COGS Impact", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        Formatters.formatCurrency(c.totalCogsBdtMinor, currency, language),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Depleted at: $dateStr",
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
