package com.pocketdev.advance_erp.feature.controlcenter

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterFilterDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterSummaryCards
import com.pocketdev.advance_erp.feature.controlcenter.components.ControlCenterTransactionDetailDialog
import com.pocketdev.advance_erp.feature.controlcenter.components.CustomDateRangeDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessControlCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessControlCenterViewModel = hiltViewModel(),
    initialCategory: ReportCategory? = null
) {
    val context = LocalContext.current
    val filterState by viewModel.filterState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summaryDashboard.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val selectedDetail by viewModel.selectedTransactionDetail.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val isBn = language == AppLanguage.BANGLA
    val canViewProfit = SecurityManager.canViewProfit(userRole)

    var showFilterDialog by remember { mutableStateOf(false) }
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showPresetsMenu by remember { mutableStateOf(false) }

    // Init category if passed
    remember(initialCategory) {
        if (initialCategory != null && filterState.category != initialCategory) {
            viewModel.selectCategory(initialCategory)
        }
        true
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    fun formatCustomDate(millis: Long?): String {
        if (millis == null) return if (isBn) "নির্বাচন" else "Select"
        val formatted = dateFormat.format(Date(millis))
        return if (isBn) Formatters.toBanglaDigits(formatted) else formatted
    }

    // Helper for Custom Date Picker - End Date
    fun openEndDatePicker(startDateMillis: Long?) {
        val cal = Calendar.getInstance()
        val currentEnd = filterState.customEndDate ?: System.currentTimeMillis()
        val initialEnd = if (startDateMillis != null && currentEnd < startDateMillis) startDateMillis else currentEnd
        cal.timeInMillis = initialEnd

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val endCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val start = startDateMillis ?: filterState.customStartDate ?: (endCal.timeInMillis - 7L * 86400000L)
                viewModel.setCustomDateRange(start, endCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শেষের তারিখ নির্বাচন করুন (End Date)" else "Select End Date")
        if (startDateMillis != null) {
            datePicker.datePicker.minDate = startDateMillis
        }
        datePicker.show()
    }

    // Helper for Custom Date Picker - Start Date
    fun openCustomDatePicker(isStart: Boolean, chainToEnd: Boolean = false) {
        if (!isStart) {
            openEndDatePicker(filterState.customStartDate)
            return
        }
        val cal = Calendar.getInstance()
        val currentMillis = filterState.customStartDate ?: (System.currentTimeMillis() - 7L * 86400000L)
        cal.timeInMillis = currentMillis

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val selectedStart = selectedCal.timeInMillis
                if (chainToEnd) {
                    openEndDatePicker(selectedStart)
                } else {
                    viewModel.setCustomStartDate(selectedStart)
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শুরুর তারিখ নির্বাচন করুন (Start Date)" else "Select Start Date")
        datePicker.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
    ) {
        // Compact Top Action Strip (Replaces tall duplicate title block)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subtitle / Scope indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurple.copy(alpha = 0.12f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isBn) "কেন্দ্রীয় লেজার ও স্টেটমেন্ট" else "Central Ledger & Reports",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (isBn) "রিয়েলটাইম ফিল্টারিং ও এক্সপোর্ট" else "Real-time filters & export",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Top Action buttons: Presets Menu + Export Button (Fixed height, no vertical stretch)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        // Presets Menu Button
                        Box {
                            IconButton(
                                onClick = { showPresetsMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Saved Presets",
                                    tint = if (savedPresets.isNotEmpty()) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showPresetsMenu,
                                onDismissRequest = { showPresetsMenu = false }
                            ) {
                                if (savedPresets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(if (isBn) "কোনো সংরক্ষিত প্রিসেট নেই" else "No saved presets yet") },
                                        onClick = { showPresetsMenu = false }
                                    )
                                } else {
                                    savedPresets.forEach { preset ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(preset.name, fontWeight = FontWeight.Bold)
                                                    Text(preset.filterState.category.titleEn, style = MaterialTheme.typography.labelSmall)
                                                }
                                            },
                                            onClick = {
                                                viewModel.applyPreset(preset)
                                                showPresetsMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Export & Share Button (Compact & Sturdy)
                        Button(
                            onClick = { showExportSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "এক্সপোর্ট" else "Export", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Date Range Horizontal Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "তারিখ (Date Range):" else "Date Range:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filterState.dateRangeOption == DateRangeOption.CUSTOM) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Start Date Pill (Clickable directly for Start Date)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryPurple.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { openCustomDatePicker(isStart = true, chainToEnd = false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = PrimaryPurple
                                    )
                                    Text(
                                        text = "${if (isBn) "শুরু:" else "Start:"} ${formatCustomDate(filterState.customStartDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "→",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // End Date Pill (Clickable directly for End Date)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SecondaryTeal.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { openCustomDatePicker(isStart = false, chainToEnd = false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = SecondaryTeal
                                    )
                                    Text(
                                        text = "${if (isBn) "শেষ:" else "End:"} ${formatCustomDate(filterState.customEndDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Edit Icon Button to open CustomDateRangeDialog
                            IconButton(
                                onClick = { showCustomDateRangeDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Edit Date Range",
                                    modifier = Modifier.size(13.dp),
                                    tint = PrimaryPurple
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(DateRangeOption.entries.toTypedArray()) { option ->
                        val isSelected = filterState.dateRangeOption == option
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (option == DateRangeOption.CUSTOM) {
                                    showCustomDateRangeDialog = true
                                } else {
                                    viewModel.selectDateRange(option)
                                }
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                }
                            } else if (option == DateRangeOption.CUSTOM) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = PrimaryPurple
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = if (isBn) option.titleBn else option.titleEn,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = PrimaryPurple,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Category Filter Horizontal Row
                Text(
                    text = if (isBn) "ক্যাটাগরি (Report Category):" else "Report Category:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(ReportCategory.entries.toTypedArray()) { cat ->
                        val isSelected = filterState.category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = if (isBn) cat.titleBn else cat.titleEn,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = SecondaryTeal,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = SecondaryTeal,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Main Scrollable Content (Compact Spacing & High Information Density)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Business Summary Dashboard Cards (Collapsible / Expandable)
            item {
                ControlCenterSummaryCards(
                    summary = summary,
                    canViewProfit = canViewProfit,
                    language = language,
                    currencyCode = currencyCode,
                    onCategoryClick = { viewModel.selectCategory(it) }
                )
            }

            // 2. Compact Search & Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(if (isBn) "ইনভয়েস, পিও, পার্টি বা পণ্য খুঁজুন..." else "Search ref, party, items...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )

                    // Filter Dialog Trigger Button with Active Count Badge
                    BadgedBox(
                        badge = {
                            if (filterState.activeFilterCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ) {
                                    Text(filterState.activeFilterCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterState.activeFilterCount > 0) PrimaryPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (filterState.activeFilterCount > 0) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { showFilterDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (filterState.activeFilterCount > 0) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Reset Filters Button (if active)
                    if (filterState.activeFilterCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { viewModel.resetFilters() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // 3. Transactions Count & Active Filter Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "লেনদেন তালিকা (${transactions.size} টি রেকর্ড)" else "Ledger Entries (${transactions.size} Records)",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val totalAmt = transactions.sumOf { it.amountMinor }
                    Text(
                        text = "${if (isBn) "মোট: " else "Sum: "} ${Formatters.formatMoney(totalAmt, currencyCode, language)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }

            // 4. Empty State
            if (transactions.isEmpty()) {
                item {
                    ERPEmptyState(
                        icon = Icons.Default.FilterList,
                        title = if (isBn) "কোনো তথ্য পাওয়া যায়নি" else "No Records Found",
                        message = if (isBn) "নির্বাচিত তারিখ ও ফিল্টার অনুযায়ী কোনো লেনদেন নেই।" else "Try adjusting the date range or clear some filters.",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                // 5. Transaction Cards (Compact & Readable)
                items(transactions, key = { it.id }) { item ->
                    UnifiedTransactionCard(
                        item = item,
                        language = language,
                        currencyCode = currencyCode,
                        dateFormat = dateFormat,
                        onClick = { viewModel.openTransactionDetail(item.rawType, item.id) }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        ControlCenterFilterDialog(
            currentFilter = filterState,
            customers = customers,
            suppliers = suppliers,
            warehouses = warehouses,
            language = language,
            onApply = { viewModel.updateFilter(it) },
            onReset = { viewModel.resetFilters() },
            onSavePreset = { viewModel.savePreset(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Custom Date Range Dialog
    if (showCustomDateRangeDialog) {
        CustomDateRangeDialog(
            initialStartDate = filterState.customStartDate,
            initialEndDate = filterState.customEndDate,
            language = language,
            onApply = { start, end ->
                viewModel.setCustomDateRange(start, end)
            },
            onDismiss = { showCustomDateRangeDialog = false }
        )
    }

    // Detail Dialog
    selectedDetail?.let { detail ->
        ControlCenterTransactionDetailDialog(
            record = detail,
            language = language,
            currencyCode = currencyCode,
            onDismiss = { viewModel.dismissTransactionDetail() }
        )
    }

    // Export Options Modal Bottom Sheet
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isBn) "রিপোর্ট এক্সপোর্ট ও শেয়ারিং অপশন" else "Export & Share Business Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBn) "পিডিএফ, এক্সেল বা সিএসভি ফরম্যাটে ডাউনলোড ও শেয়ার করুন" else "Generate professional PDF & XLSX workbooks directly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 1. Share PDF
                ExportActionItem(
                    title = if (isBn) "পিডিএফ রিপোর্ট শেয়ার করুন (PDF Report)" else "Share PDF Document",
                    subtitle = if (isBn) "WhatsApp, Gmail বা অন্য অ্যাপে শেয়ার করুন" else "A4 printable report with header & summary",
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFFDC2626),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndSharePdf(context)
                    }
                )

                // 2. Share Excel XLSX
                ExportActionItem(
                    title = if (isBn) "এক্সেল ওয়ার্কবুক শেয়ার করুন (Excel XLSX)" else "Share Excel (XLSX) Workbook",
                    subtitle = if (isBn) "ক্যাটাগরিভিত্তিক আলাদা শীট ও বিশ্লেষণ" else "Multi-sheet spreadsheet with summary & ledgers",
                    icon = Icons.Default.TableChart,
                    iconColor = Color(0xFF16A34A),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareExcel(context)
                    }
                )

                // 3. Share CSV
                ExportActionItem(
                    title = if (isBn) "সিএসভি ফাইল শেয়ার করুন (CSV Export)" else "Share CSV Data File",
                    subtitle = if (isBn) "সহজ ডাটা এনালাইসিসের জন্য" else "Plain comma-separated transaction records",
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF0284C7),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportAndShareCsv(context)
                    }
                )

                // 4. Print Report
                ExportActionItem(
                    title = if (isBn) "সরাসরি প্রিন্ট করুন (Print Report)" else "Print Report Directly",
                    subtitle = if (isBn) "Android PrintManager ব্যবহার করে প্রিন্ট" else "Send formatted document to WiFi or Bluetooth printer",
                    icon = Icons.Default.Print,
                    iconColor = Color(0xFF7C3AED),
                    onClick = {
                        showExportSheet = false
                        viewModel.printReport(context)
                    }
                )

                // 5. Save to Downloads
                ExportActionItem(
                    title = if (isBn) "ডিভাইসে ডাউনলোড করুন (Save to Downloads)" else "Save to Downloads Folder",
                    subtitle = if (isBn) "মোবাইলের ইন্টারনাল স্টোরেজে সংরক্ষণ" else "Save PDF & XLSX directly to device storage",
                    icon = Icons.Default.Download,
                    iconColor = Color(0xFFD97706),
                    onClick = {
                        showExportSheet = false
                        viewModel.savePdfToStorage(context)
                        viewModel.saveExcelToStorage(context)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun UnifiedTransactionCard(
    item: UnifiedTransactionItem,
    language: AppLanguage,
    currencyCode: CurrencyCode,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Reference & Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = item.referenceNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(item.statusColorHex.toInt()).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(item.statusColorHex.toInt()),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(item.date)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Entity Name & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.primaryEntityName,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = item.category.titleEn,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            // Particulars
            if (item.particulars.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.particulars,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Bottom Financial Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method & Warehouse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.paymentMethod?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.warehouseName != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${item.warehouseName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sign = if (item.isInflow) "+" else "-"
                    val color = if (item.isInflow) Color(0xFF16A34A) else Color(0xFFDC2626)
                    Text(
                        text = "$sign${Formatters.formatMoney(item.amountMinor, currencyCode, language)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(7.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
