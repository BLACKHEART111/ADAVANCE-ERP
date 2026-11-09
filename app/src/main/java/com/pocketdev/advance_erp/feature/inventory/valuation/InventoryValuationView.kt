package com.pocketdev.advance_erp.feature.inventory.valuation

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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
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
import com.pocketdev.advance_erp.domain.repository.InventoryValuationBreakdown

@Composable
fun InventoryValuationView(
    breakdown: InventoryValuationBreakdown?,
    language: AppLanguage,
    currency: CurrencyCode,
    onExportPdf: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BANGLA) "মজুদ মূল্যায়ন প্রতিবেদন" else "Inventory Valuation Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Sellable vs Damaged vs In-Transit Valuation (FIFO Cost Model)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            ERPActionButton(
                text = "Export PDF",
                onClick = onExportPdf,
                icon = Icons.Default.PictureAsPdf,
                variant = ERPButtonVariant.OUTLINED
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (breakdown == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading valuation metrics...", color = TextMuted)
            }
        } else {
            // Three Category Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sellable Stock
                ERPCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = 8.dp,
                    backgroundColor = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "বিক্রয়যোগ্য মজুদ" else "Sellable Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) {
                                "${Formatters.formatNumber(breakdown.sellableQuantity.toLong(), language)} টি"
                            } else {
                                "${breakdown.sellableQuantity} units"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatCurrency(breakdown.sellableValuationMinor, currency, language),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Damaged Stock
                ERPCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = 8.dp,
                    backgroundColor = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "নষ্ট ও অবলোপন" else "Damaged Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) {
                                "${Formatters.formatNumber(breakdown.damagedQuantity.toLong(), language)} টি"
                            } else {
                                "${breakdown.damagedQuantity} units"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7F1D1D),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatCurrency(breakdown.damagedValuationMinor, currency, language),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusError,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Total FIFO Valuation
                ERPCard(
                    modifier = Modifier.weight(1.15f),
                    contentPadding = 8.dp,
                    backgroundColor = Color(0xFFFAF5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "মোট মূল্যায়ন (FIFO)" else "Total Valuation",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) {
                                "${Formatters.formatNumber((breakdown.sellableQuantity + breakdown.inTransitQuantity).toLong(), language)} টি"
                            } else {
                                "${breakdown.sellableQuantity + breakdown.inTransitQuantity} units"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF581C87),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatCurrency(breakdown.totalValuationMinor, currency, language),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warehouse Breakdown Section
            Text(
                text = if (language == AppLanguage.BANGLA) "ওয়্যারহাউস-ভিত্তিক বিবরণ" else "Warehouse Valuation Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (breakdown.warehouseBreakdown.isEmpty()) {
                ERPCard(contentPadding = 12.dp) {
                    Text("No warehouse records found.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(breakdown.warehouseBreakdown, key = { it.warehouseId }) { wh ->
                        ERPCard(contentPadding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wh.warehouseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Physical: ${wh.physicalQuantity} u • Sellable: ${wh.sellableQuantity} u • Damaged: ${wh.damagedQuantity} u",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Text(
                                    text = Formatters.formatCurrency(wh.valuationMinor, currency, language),
                                    style = MaterialTheme.typography.titleMedium,
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
