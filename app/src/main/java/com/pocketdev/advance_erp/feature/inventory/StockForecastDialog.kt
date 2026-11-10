package com.pocketdev.advance_erp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.ai.StockForecastItem
import com.pocketdev.advance_erp.core.ai.StockForecastSummary
import com.pocketdev.advance_erp.core.ai.StockVelocityLevel
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.util.Formatters

private enum class ForecastTab {
    ALL, URGENT, DEAD_STOCK, FAST_MOVING
}

@Composable
fun StockForecastDialog(
    summary: StockForecastSummary,
    isBangla: Boolean = true,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ForecastTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(summary, selectedTab, searchQuery) {
        summary.items.filter { item ->
            val matchesTab = when (selectedTab) {
                ForecastTab.ALL -> true
                ForecastTab.URGENT -> item.isUrgentReorder
                ForecastTab.DEAD_STOCK -> item.isDeadStock
                ForecastTab.FAST_MOVING -> item.velocityLevel == StockVelocityLevel.HIGH_VELOCITY
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.productName.contains(searchQuery, ignoreCase = true) ||
                item.sku.contains(searchQuery, ignoreCase = true) ||
                item.categoryName.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .widthIn(max = 720.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "AI স্টক পূর্বাভাস ও রিরর্ডার অ্যানালিটিক্স" else "AI Stock Forecast & Reorder Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isBangla) "বিগত ৩০ দিনের বিক্রির গতির ওপর ভিত্তি করে স্বয়ংক্রিয় বিশ্লেষণ" else "Automated run-out prediction based on 30-day velocity",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DeepNavy.copy(alpha = 0.1f))

                // 3 Top KPI Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Urgent Reorders
                    ForecastKpiCard(
                        title = if (isBangla) "জরুরি রিরর্ডার" else "Urgent Reorder",
                        count = "${summary.urgentReorderCount} টি",
                        sub = if (isBangla) "৭ দিনের মধ্যে শেষ হবে" else "Runout in ≤7 days",
                        color = Color(0xFFC53030),
                        modifier = Modifier.weight(1f)
                    )
                    // Dead Stock Capital
                    ForecastKpiCard(
                        title = if (isBangla) "অচল স্টক (ডেড স্টক)" else "Dead Stock Capital",
                        count = "৳" + Formatters.formatMinorUnit(summary.totalCapitalInDeadStockMinor),
                        sub = if (isBangla) "${summary.deadStockCount} টি পণ্যে মূলধন লকড" else "${summary.deadStockCount} items locked",
                        color = SecondaryTeal,
                        modifier = Modifier.weight(1.2f)
                    )
                    // Fast Moving
                    ForecastKpiCard(
                        title = if (isBangla) "হট সেলিং পণ্য" else "Fast Moving",
                        count = "${summary.fastMovingCount} টি",
                        sub = if (isBangla) "দৈনিক দ্রুত বিক্রি" else "High velocity",
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isBangla) "পণ্য বা SKU দিয়ে খুঁজুন..." else "Search product or SKU...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == ForecastTab.ALL,
                        onClick = { selectedTab = ForecastTab.ALL },
                        label = { Text(if (isBangla) "সকল (${summary.items.size})" else "All (${summary.items.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DeepNavy.copy(alpha = 0.15f))
                    )
                    FilterChip(
                        selected = selectedTab == ForecastTab.URGENT,
                        onClick = { selectedTab = ForecastTab.URGENT },
                        label = { Text(if (isBangla) "🚨 রিরর্ডার (${summary.urgentReorderCount})" else "🚨 Reorder (${summary.urgentReorderCount})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE))
                    )
                    FilterChip(
                        selected = selectedTab == ForecastTab.DEAD_STOCK,
                        onClick = { selectedTab = ForecastTab.DEAD_STOCK },
                        label = { Text(if (isBangla) "🧊 অচল স্টক (${summary.deadStockCount})" else "🧊 Dead (${summary.deadStockCount})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE0F7FA))
                    )
                    FilterChip(
                        selected = selectedTab == ForecastTab.FAST_MOVING,
                        onClick = { selectedTab = ForecastTab.FAST_MOVING },
                        label = { Text(if (isBangla) "⚡ ফাস্ট (${summary.fastMovingCount})" else "⚡ Fast (${summary.fastMovingCount})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8F5E9))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Items List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBangla) "কোনো পূর্বাভাস পণ্য পাওয়া যায়নি" else "No matching forecast items found",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(filteredItems, key = { it.productId }) { item ->
                        ForecastItemCard(item = item, isBangla = isBangla)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastKpiCard(
    title: String,
    count: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ForecastItemCard(
    item: StockForecastItem,
    isBangla: Boolean
) {
    val borderColor = when {
        item.isUrgentReorder -> Color(0xFFE57373)
        item.isDeadStock -> SecondaryTeal
        item.velocityLevel == StockVelocityLevel.HIGH_VELOCITY -> StatusSuccess
        else -> DeepNavy.copy(alpha = 0.1f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title & SKU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "SKU: ${item.sku} • ${item.categoryName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Days remaining badge
                if (item.isUrgentReorder) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = if (item.currentStock == 0) (if (isBangla) "স্টক শূন্য" else "Out of Stock")
                                   else if (item.daysUntilStockout != null) "${item.daysUntilStockout} দিন বাকি"
                                   else (if (isBangla) "কম স্টক" else "Low Stock"),
                            color = Color(0xFFC53030),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (item.isDeadStock) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F7FA)
                    ) {
                        Text(
                            text = if (isBangla) "অচল স্টক" else "Dead Stock",
                            color = SecondaryTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (item.velocityLevel == StockVelocityLevel.HIGH_VELOCITY) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = if (isBangla) "⚡ হট সেলিং" else "⚡ Fast Moving",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = if (isBangla) "বর্তমান স্টক" else "Current Stock",
                    value = "${item.currentStock} টি"
                )
                MetricColumn(
                    label = if (isBangla) "৩০ দিনের বিক্রি" else "30d Sold",
                    value = "${item.unitsSoldLast30Days} টি"
                )
                MetricColumn(
                    label = if (isBangla) "দৈনিক গতি" else "Daily Velocity",
                    value = "%.1f/দিন".format(item.dailyVelocity)
                )
                MetricColumn(
                    label = if (isBangla) "আটকে থাকা মূলধন" else "Locked Capital",
                    value = "৳" + Formatters.formatMinorUnit(item.capitalLockedMinor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Recommendation Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = when {
                    item.isUrgentReorder -> Color(0xFFFFF3E0)
                    item.isDeadStock -> Color(0xFFE0F2F1)
                    else -> ERPBackground
                }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (item.isUrgentReorder) Color(0xFFE65100) else PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) item.recommendationBn else item.recommendationEn,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = DeepNavy,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
