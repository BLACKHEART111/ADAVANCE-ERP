package com.pocketdev.advance_erp.feature.controlcenter.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ReportCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlCenterSummaryCards(
    summary: BusinessSummaryDashboardData,
    canViewProfit: Boolean,
    language: AppLanguage,
    currencyCode: CurrencyCode,
    onCategoryClick: (ReportCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Primary 4 Executive KPIs (Always Visible in 2x2 grid)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Total Sales
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট বিক্রয়" else "Total Sales",
                amountMinor = summary.totalSalesMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF10B981),
                onClick = { onCategoryClick(ReportCategory.SALES) }
            )

            // 2. Total Received (Collections)
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট টাকা গ্রহণ" else "Total Received",
                amountMinor = summary.totalReceivedMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Default.ArrowDownward,
                accentColor = Color(0xFF059669),
                onClick = { onCategoryClick(ReportCategory.RECEIPT) }
            )

            // 3. Total Purchase
            ControlCenterCardItem(
                modifier = Modifier.weight(1f),
                title = if (isBn) "মোট ক্রয়" else "Total Purchase",
                amountMinor = summary.totalPurchaseMinor,
                currencyCode = currencyCode,
                language = language,
                icon = Icons.Default.ShoppingCart,
                accentColor = Color(0xFF8B5CF6),
                onClick = { onCategoryClick(ReportCategory.PURCHASE) }
            )

            // 4. Net Profit or Outstanding Payable
            if (canViewProfit) {
                ControlCenterCardItem(
                    modifier = Modifier.weight(1f),
                    title = if (isBn) "নিট মুনাফা" else "Net Profit",
                    amountMinor = summary.netProfitMinor,
                    currencyCode = currencyCode,
                    language = language,
                    icon = if (summary.netProfitMinor >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    accentColor = if (summary.netProfitMinor >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                    onClick = { onCategoryClick(ReportCategory.SALES) }
                )
            } else {
                ControlCenterCardItem(
                    modifier = Modifier.weight(1f),
                    title = if (isBn) "মোট দেনা (Payable)" else "Total Payable",
                    amountMinor = summary.totalOutstandingPayableMinor,
                    currencyCode = currencyCode,
                    language = language,
                    icon = Icons.Default.MonetizationOn,
                    accentColor = Color(0xFFEF4444),
                    onClick = { onCategoryClick(ReportCategory.SUPPLIER_LEDGER) }
                )
            }
        }

        // Expandable 8 Additional KPIs
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 5. Total Paid
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট টাকা পরিশোধ" else "Total Paid",
                        amountMinor = summary.totalPaidMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Default.ArrowUpward,
                        accentColor = Color(0xFF0284C7),
                        onClick = { onCategoryClick(ReportCategory.PAYMENT) }
                    )

                    // 6. Total Expenses
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট ব্যয়" else "Total Expenses",
                        amountMinor = summary.totalExpensesMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Default.MoneyOff,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { onCategoryClick(ReportCategory.EXPENSE) }
                    )

                    // 7. Total Sales Return
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "বিক্রয় ফেরত" else "Sales Return",
                        amountMinor = summary.totalSalesReturnMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Default.Replay,
                        accentColor = Color(0xFFEF4444),
                        onClick = { onCategoryClick(ReportCategory.SALES_RETURN) }
                    )

                    // 8. Total Purchase Return
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "ক্রয় ফেরত" else "Purchase Return",
                        amountMinor = summary.totalPurchaseReturnMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Default.Replay,
                        accentColor = Color(0xFFF97316),
                        onClick = { onCategoryClick(ReportCategory.PURCHASE_RETURN) }
                    )

                    // 9. Total Delivery
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(ReportCategory.DELIVERY) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBn) "মোট ডেলিভারি" else "Total Deliveries",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${summary.totalDeliveryCount} ${if (isBn) "টি সম্পন্ন" else "Orders"}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06B6D4),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // 10. Total Outstanding Receivable
                    ControlCenterCardItem(
                        modifier = Modifier.weight(1f),
                        title = if (isBn) "মোট পাওনা (Due)" else "Receivables",
                        amountMinor = summary.totalOutstandingReceivableMinor,
                        currencyCode = currencyCode,
                        language = language,
                        icon = Icons.Default.AttachMoney,
                        accentColor = Color(0xFFE11D48),
                        onClick = { onCategoryClick(ReportCategory.CUSTOMER_LEDGER) }
                    )

                    // 11. Gross Profit (Permission checked)
                    if (canViewProfit) {
                        ControlCenterCardItem(
                            modifier = Modifier.weight(1f),
                            title = if (isBn) "মোট মুনাফা (Gross)" else "Gross Profit",
                            amountMinor = summary.grossProfitMinor,
                            currencyCode = currencyCode,
                            language = language,
                            icon = if (summary.grossProfitMinor >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            accentColor = if (summary.grossProfitMinor >= 0) Color(0xFF10B981) else Color(0xFFDC2626),
                            onClick = { onCategoryClick(ReportCategory.SALES) }
                        )
                    } else {
                        LockedProfitCard(
                            modifier = Modifier.weight(1f),
                            title = if (isBn) "মোট মুনাফা" else "Gross Profit",
                            isBn = isBn
                        )
                    }

                    // 12. Outstanding Payable (if not already shown in primary 4)
                    if (canViewProfit) {
                        ControlCenterCardItem(
                            modifier = Modifier.weight(1f),
                            title = if (isBn) "মোট দেনা (Payable)" else "Payables",
                            amountMinor = summary.totalOutstandingPayableMinor,
                            currencyCode = currencyCode,
                            language = language,
                            icon = Icons.Default.MonetizationOn,
                            accentColor = Color(0xFFEF4444),
                            onClick = { onCategoryClick(ReportCategory.SUPPLIER_LEDGER) }
                        )
                    }
                }
            }
        }

        // Toggle Expand / Collapse Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) {
                    if (isBn) "সূচক সংক্ষেপ করুন" else "Collapse Metrics"
                } else {
                    if (isBn) "সকল ১২টি সূচক দেখুন" else "View All 12 Metrics"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ControlCenterCardItem(
    modifier: Modifier = Modifier,
    title: String,
    amountMinor: Long,
    currencyCode: CurrencyCode,
    language: AppLanguage,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = Formatters.formatMoney(amountMinor, currencyCode, language),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LockedProfitCard(
    modifier: Modifier = Modifier,
    title: String,
    isBn: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.LightGray.copy(alpha = 0.25f),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isBn) "শুধু অ্যাডমিন/মালিক" else "Owner / Admin Only",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }
    }
}
