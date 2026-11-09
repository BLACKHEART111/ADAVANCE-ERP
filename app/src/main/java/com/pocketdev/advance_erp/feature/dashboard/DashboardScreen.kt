package com.pocketdev.advance_erp.feature.dashboard

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCalendar
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.ERPLoadingState
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.ERPStatCard
import com.pocketdev.advance_erp.core.designsystem.ERPTransactionItem
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToStockAdjust: () -> Unit,
    onNavigateToImports: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (state.isLoading) {
        ERPLoadingState(modifier = modifier.fillMaxSize())
        return
    }

    val summary = state.summary
    val language = state.language
    val isBangla = language == AppLanguage.BANGLA
    val dateMetrics = summary?.dateWiseMetrics
    val closingBalances = summary?.closingBalances

    var showCalendarPicker by remember { mutableStateOf(false) }

    val dateFormat = remember(isBangla) {
        SimpleDateFormat("dd MMM yyyy", if (isBangla) Locale("bn", "BD") else Locale.US)
    }
    val selectedDateText = dateFormat.format(Date(state.selectedDateMillis))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Business Overview & Live DB Indicator Banner
        item {
            ERPCard(
                backgroundColor = DeepNavy,
                contentPadding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_overview),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBangla) "রিয়েল-টাইম ডাটাবেস মেট্রিক্স" else "Live Real-Time Database Metrics",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(StatusSuccess, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OverviewMetric(
                            label = stringResource(R.string.nav_products),
                            value = Formatters.formatNumber(summary?.productCount?.toLong() ?: 0L, language)
                        )
                        OverviewMetric(
                            label = stringResource(R.string.low_stock_warning),
                            value = Formatters.formatNumber(summary?.lowStockCount?.toLong() ?: 0L, language),
                            highlightColor = if ((summary?.lowStockCount ?: 0) > 0) StatusError else Color.White
                        )
                        OverviewMetric(
                            label = stringResource(R.string.customers),
                            value = Formatters.formatNumber(summary?.customerCount?.toLong() ?: 0L, language)
                        )
                        OverviewMetric(
                            label = stringResource(R.string.suppliers),
                            value = Formatters.formatNumber(summary?.supplierCount?.toLong() ?: 0L, language)
                        )
                    }
                }
            }
        }

        // 2. Calendar Date Control Bar (ERPCalendar Navigation)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.selectPreviousDay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Day", tint = PrimaryPurple)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCalendarPicker = !showCalendarPicker }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = DeepNavy, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "তারিখ: $selectedDateText" else "Selected: $selectedDateText",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepNavy
                                )
                            }

                            IconButton(
                                onClick = { viewModel.selectNextDay() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day", tint = PrimaryPurple)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.selectToday() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "আজ" else "Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (showCalendarPicker) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DeepNavy.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))
                        ERPCalendar(
                            language = language,
                            selectedTimestamp = state.selectedDateMillis,
                            onDateSelected = { millis ->
                                viewModel.selectDate(millis)
                                showCalendarPicker = false
                            }
                        )
                    }
                }
            }
        }

        // 3. Centralized PDF Export Quick Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.generateDailySnapshotPdf(context) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepNavy)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBangla) "দৈনিক পিডিএফ চিত্র" else "Daily PDF Snapshot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.generateMonthlyPerformancePdf(context) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepNavy)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBangla) "মাসিক রিপোর্ট" else "Monthly Review", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Date-wise Business Metrics Grid (10 Required Daily Indicators)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ERPSectionHeader(
                    title = if (isBangla) "$selectedDateText এর ব্যবসায়িক তথ্য" else "Business Metrics for $selectedDateText",
                    subtitle = if (isBangla) "নির্বাচিত দিনের মোট ক্রয়, বিক্রয়, নগদ প্রবাহ ও লাভ" else "Sales, procurement, net cash flow & daily profit"
                )

                // Row 1: Sales & Purchases on selected date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPStatCard(
                        title = if (isBangla) "দিনের বিক্রয় আয়" else "Date Sales Revenue",
                        value = Formatters.formatMoney(dateMetrics?.totalSales?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.PointOfSale,
                        accentColor = PrimaryPurple,
                        badgeText = "Day Sales",
                        modifier = Modifier.weight(1f)
                    )
                    ERPStatCard(
                        title = if (isBangla) "দিনের ক্রয় মূল্য" else "Date Purchases (PO)",
                        value = Formatters.formatMoney(dateMetrics?.totalPurchases?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ShoppingCart,
                        accentColor = DeepNavy,
                        badgeText = "Procurement",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Customer Collections & Supplier Payments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPStatCard(
                        title = if (isBangla) "গ্রাহক আদায় (কালেকশন)" else "Customer Collections",
                        value = Formatters.formatMoney(dateMetrics?.totalCollections?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ArrowDownward,
                        accentColor = StatusSuccess,
                        badgeText = "Cash In",
                        modifier = Modifier.weight(1f)
                    )
                    ERPStatCard(
                        title = if (isBangla) "সরবরাহকারী পরিশোধ" else "Supplier Payments",
                        value = Formatters.formatMoney(dateMetrics?.totalPayments?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ArrowUpward,
                        accentColor = StatusError,
                        badgeText = "Cash Out",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Operating Expenses & Sales Returns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPStatCard(
                        title = if (isBangla) "দৈনিক খরচ" else "Daily Expenses",
                        value = Formatters.formatMoney(dateMetrics?.totalExpenses?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ReceiptLong,
                        accentColor = Color(0xFFD97706),
                        badgeText = "Operating",
                        modifier = Modifier.weight(1f)
                    )
                    ERPStatCard(
                        title = if (isBangla) "বিক্রয় ফেরত" else "Sales Returns",
                        value = Formatters.formatMoney(dateMetrics?.totalReturns?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.SwapHoriz,
                        accentColor = Color(0xFF8B5CF6),
                        badgeText = "Returns",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Net Cash Flow & Daily Gross Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val netCashMinor = dateMetrics?.netCashFlow?.minorUnits ?: 0L
                    ERPStatCard(
                        title = if (isBangla) "নিট নগদ প্রবাহ" else "Net Cash Flow",
                        value = Formatters.formatMoney(netCashMinor, state.currency, language),
                        icon = Icons.Default.ReceiptLong,
                        accentColor = if (netCashMinor >= 0) StatusSuccess else StatusError,
                        badgeText = "In - Out",
                        modifier = Modifier.weight(1f)
                    )
                    if (state.canViewProfit) {
                        val dailyProfitMinor = dateMetrics?.dailyGrossProfit?.minorUnits ?: 0L
                        ERPStatCard(
                            title = if (isBangla) "দৈনিক মোট লাভ" else "Daily Gross Profit",
                            value = Formatters.formatMoney(dailyProfitMinor, state.currency, language),
                            icon = Icons.Default.CheckCircle,
                            accentColor = if (dailyProfitMinor >= 0) SecondaryTeal else StatusError,
                            badgeText = "Sales - COGS",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ERPStatCard(
                            title = if (isBangla) "দৈনিক লেনদেন সংখ্যা" else "Daily Transactions",
                            value = "${dateMetrics?.transactionCount ?: 0} events",
                            icon = Icons.Default.Assessment,
                            accentColor = DeepNavy,
                            badgeText = "Activity",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 5: Stock Movement & Activity Counts
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isBangla) "স্টক ইন" else "Stock In", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "+${dateMetrics?.stockInUnits ?: 0} units", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isBangla) "স্টক আউট" else "Stock Out", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "-${dateMetrics?.stockOutUnits ?: 0} units", fontWeight = FontWeight.Bold, color = StatusError, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isBangla) "লেনদেন সংখ্যা" else "Transactions", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "${dateMetrics?.transactionCount ?: 0} events", fontWeight = FontWeight.Bold, color = DeepNavy, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 5. Closing Balances as of Selected Date (Historical Cumulative)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ERPSectionHeader(
                    title = if (isBangla) "$selectedDateText পর্যন্ত সমাপনী স্থিতি" else "Closing Balances as of $selectedDateText",
                    subtitle = if (isBangla) "নগদ ও ব্যাংক, গ্রাহক পাওনা, সরবরাহকারী দেনা ও মজুদ মূল্যায়ন" else "Cumulative closing cash, receivables, payables & inventory"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPStatCard(
                        title = if (isBangla) "সমাপনী নগদ ও ব্যাংক" else "Closing Cash & Bank",
                        value = Formatters.formatMoney(closingBalances?.closingCashBank?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ReceiptLong,
                        accentColor = Color(0xFF0284C7),
                        badgeText = "Liquid Funds",
                        modifier = Modifier.weight(1f)
                    )
                    ERPStatCard(
                        title = if (isBangla) "গ্রাহক বকেয়া (পাওনা)" else "Customer Receivables",
                        value = Formatters.formatMoney(closingBalances?.customerOutstanding?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ArrowDownward,
                        accentColor = StatusSuccess,
                        badgeText = "AR Due",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ERPStatCard(
                        title = if (isBangla) "সরবরাহকারী দেনা" else "Supplier Payables",
                        value = Formatters.formatMoney(closingBalances?.supplierOutstanding?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.ArrowUpward,
                        accentColor = StatusError,
                        badgeText = "AP Due",
                        modifier = Modifier.weight(1f)
                    )
                    ERPStatCard(
                        title = if (isBangla) "মজুদ সম্পদ মূল্যায়ন" else "Inventory Valuation",
                        value = Formatters.formatMoney(closingBalances?.inventoryValuation?.minorUnits ?: 0L, state.currency, language),
                        icon = Icons.Default.Inventory2,
                        accentColor = SecondaryTeal,
                        badgeText = "FIFO Asset",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Date-filtered Transaction List
        item {
            Column {
                ERPSectionHeader(
                    title = if (isBangla) "$selectedDateText এর লেনদেন সমূহ" else "Transactions on $selectedDateText",
                    actionText = stringResource(R.string.view_all),
                    onActionClick = onNavigateToTransactions
                )

                val transactions = summary?.recentTransactions.orEmpty()
                if (transactions.isEmpty()) {
                    ERPCard {
                        ERPEmptyState(
                            icon = Icons.Default.ReceiptLong,
                            title = if (isBangla) "এই তারিখে কোনো লেনদেন নেই" else "No transactions on this date",
                            message = if (isBangla) "নির্বাচিত তারিখে কোনো বিক্রয় বা স্টক স্থানান্তর রেকর্ড করা হয়নি।" else "No movement or sales recorded on selected date."
                        )
                    }
                } else {
                    ERPCard {
                        Column {
                            transactions.forEachIndexed { index, tx ->
                                ERPTransactionItem(
                                    title = tx.title,
                                    subtitle = "${tx.subtitle} • ${Formatters.formatDate(tx.timestamp, language)}",
                                    amountText = Formatters.formatMoney(tx.amountMinor, tx.currency, language),
                                    isPositive = tx.isCredit,
                                    icon = if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    statusText = tx.statusText,
                                    statusColor = if (tx.isCredit) StatusSuccess else PrimaryPurple
                                )
                                if (index < transactions.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF Viewer / Action Dialog
    if (state.generatedPdf != null) {
        PdfViewerDialog(
            pdfFile = state.generatedPdf!!,
            reportTitle = state.generatedPdfTitle,
            isBangla = isBangla,
            onDismiss = { viewModel.clearGeneratedPdf() }
        )
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    highlightColor: Color = Color.White
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
