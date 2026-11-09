package com.pocketdev.advance_erp.feature.controlcenter.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.AmountRangeFilter
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.DeliveryReturnFilter
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import com.pocketdev.advance_erp.domain.model.PaymentStatusFilter
import com.pocketdev.advance_erp.domain.model.SortOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DialogChipSelectedColor = Color(0xFF6D28D9) // Vivid Deep Purple

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlCenterFilterDialog(
    currentFilter: ControlCenterFilterState,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    warehouses: List<Warehouse>,
    language: AppLanguage,
    onApply: (ControlCenterFilterState) -> Unit,
    onReset: () -> Unit,
    onSavePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val context = LocalContext.current

    var dateRangeOption by remember { mutableStateOf(currentFilter.dateRangeOption) }
    var customStartDate by remember { mutableStateOf(currentFilter.customStartDate) }
    var customEndDate by remember { mutableStateOf(currentFilter.customEndDate) }

    var selectedCustomerId by remember { mutableStateOf(currentFilter.selectedCustomerId) }
    var selectedSupplierId by remember { mutableStateOf(currentFilter.selectedSupplierId) }
    var selectedWarehouseId by remember { mutableStateOf(currentFilter.selectedWarehouseId) }
    var paymentStatus by remember { mutableStateOf(currentFilter.paymentStatus) }
    var deliveryReturn by remember { mutableStateOf(currentFilter.deliveryReturn) }
    var financialMetric by remember { mutableStateOf(currentFilter.financialMetric) }
    var amountFilter by remember { mutableStateOf(currentFilter.amountFilter) }
    var minAmountText by remember { mutableStateOf(currentFilter.customMinAmountMinor?.let { (it / 100).toString() } ?: "") }
    var maxAmountText by remember { mutableStateOf(currentFilter.customMaxAmountMinor?.let { (it / 100).toString() } ?: "") }
    var sortBy by remember { mutableStateOf(currentFilter.sortBy) }

    var showSavePresetField by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    fun formatFilterDate(millis: Long?): String {
        if (millis == null) return if (isBn) "তারিখ নির্বাচন" else "Select Date"
        val formatted = dateFormat.format(Date(millis))
        return if (isBn) Formatters.toBanglaDigits(formatted) else formatted
    }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = customStartDate ?: (System.currentTimeMillis() - 7L * 86400000L)
        }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val newStart = selectedCal.timeInMillis
                customStartDate = newStart
                if (customEndDate != null && customEndDate!! < newStart) {
                    customEndDate = newStart
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শুরুর তারিখ নির্বাচন করুন (Start Date)" else "Select Start Date")
        datePicker.show()
    }

    fun openEndDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = customEndDate ?: System.currentTimeMillis()
        }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val newEnd = selectedCal.timeInMillis
                customEndDate = newEnd
                if (customStartDate != null && customStartDate!! > newEnd) {
                    customStartDate = newEnd
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle(if (isBn) "শেষের তারিখ নির্বাচন করুন (End Date)" else "Select End Date")
        if (customStartDate != null) {
            datePicker.datePicker.minDate = customStartDate!!
        }
        datePicker.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = DialogChipSelectedColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "উন্নত ফিল্টার অপশন" else "Advanced Filter Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Date Range Section
                Text(
                    text = if (isBn) "তারিখ নির্বাচন (Date Range)" else "Date Range Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DateRangeOption.entries.forEach { opt ->
                        FilterOptionChip(
                            selected = dateRangeOption == opt,
                            label = if (isBn) opt.titleBn else opt.titleEn,
                            onClick = {
                                dateRangeOption = opt
                                if (opt == DateRangeOption.CUSTOM && customStartDate == null) {
                                    val now = System.currentTimeMillis()
                                    customStartDate = now - 7L * 86400000L
                                    customEndDate = now
                                }
                            },
                            selectedColor = DialogChipSelectedColor
                        )
                    }
                }

                // If CUSTOM is selected, show interactive Start & End date pickers
                if (dateRangeOption == DateRangeOption.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { openStartDatePicker() },
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryPurple.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = PrimaryPurple
                                    )
                                    Text(
                                        text = if (isBn) "শুরুর তারিখ ✎" else "Start Date ✎",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatFilterDate(customStartDate),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        // End Date Card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { openEndDatePicker() },
                            shape = RoundedCornerShape(8.dp),
                            color = SecondaryTeal.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = SecondaryTeal
                                    )
                                    Text(
                                        text = if (isBn) "শেষের তারিখ ✎" else "End Date ✎",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatFilterDate(customEndDate),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // 1. Customer Selection Dropdown
                Text(
                    text = if (isBn) "কাস্টমার ফিল্টার" else "Customer Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                ) {
                    val custName = customers.firstOrNull { it.id == selectedCustomerId }?.name ?: if (isBn) "সকল কাস্টমার" else "All Customers"
                    OutlinedTextField(
                        value = custName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সকল কাস্টমার" else "All Customers") },
                            onClick = {
                                selectedCustomerId = null
                                customerDropdownExpanded = false
                            }
                        )
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.customerCode})") },
                                onClick = {
                                    selectedCustomerId = c.id
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Supplier Selection Dropdown
                Text(
                    text = if (isBn) "সাপ্লায়ার ফিল্টার" else "Supplier Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                ) {
                    val suppName = suppliers.firstOrNull { it.id == selectedSupplierId }?.companyName ?: if (isBn) "সকল সাপ্লায়ার" else "All Suppliers"
                    OutlinedTextField(
                        value = suppName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সকল সাপ্লায়ার" else "All Suppliers") },
                            onClick = {
                                selectedSupplierId = null
                                supplierDropdownExpanded = false
                            }
                        )
                        suppliers.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.companyName} (${s.supplierCode})") },
                                onClick = {
                                    selectedSupplierId = s.id
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Warehouse Selection Dropdown
                Text(
                    text = if (isBn) "ওয়্যারহাউস ফিল্টার" else "Warehouse Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                ExposedDropdownMenuBox(
                    expanded = warehouseDropdownExpanded,
                    onExpandedChange = { warehouseDropdownExpanded = !warehouseDropdownExpanded }
                ) {
                    val whName = warehouses.firstOrNull { it.id == selectedWarehouseId }?.name ?: if (isBn) "সকল ওয়্যারহাউস" else "All Warehouses"
                    OutlinedTextField(
                        value = whName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = warehouseDropdownExpanded,
                        onDismissRequest = { warehouseDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সকল ওয়্যারহাউস" else "All Warehouses") },
                            onClick = {
                                selectedWarehouseId = null
                                warehouseDropdownExpanded = false
                            }
                        )
                        warehouses.forEach { w ->
                            DropdownMenuItem(
                                text = { Text("${w.name} (${w.location})") },
                                onClick = {
                                    selectedWarehouseId = w.id
                                    warehouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Payment Status Filter Chips (High Contrast & Clear Selected Indicator)
                Text(
                    text = if (isBn) "পেমেন্ট স্ট্যাটাস ফিল্টার" else "Payment Status Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentStatusFilter.entries.forEach { option ->
                        FilterOptionChip(
                            selected = paymentStatus == option,
                            label = if (isBn) option.titleBn else option.titleEn,
                            onClick = { paymentStatus = option },
                            selectedColor = DialogChipSelectedColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Delivery & Return Filter
                Text(
                    text = if (isBn) "ডেলিভারি ও রিটার্ন ফিল্টার" else "Delivery & Return Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeliveryReturnFilter.entries.forEach { option ->
                        FilterOptionChip(
                            selected = deliveryReturn == option,
                            label = if (isBn) option.titleBn else option.titleEn,
                            onClick = { deliveryReturn = option },
                            selectedColor = Color(0xFF0D9488) // Vivid Teal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Financial Metric Filter
                Text(
                    text = if (isBn) "আর্থিক হিসাব ফিল্টার" else "Financial Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinancialMetricFilter.entries.forEach { option ->
                        FilterOptionChip(
                            selected = financialMetric == option,
                            label = if (isBn) option.titleBn else option.titleEn,
                            onClick = { financialMetric = option },
                            selectedColor = Color(0xFF0284C7) // Sky Blue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Amount Range Filter
                Text(
                    text = if (isBn) "টাকার পরিমাণ ফিল্টার" else "Amount Filter",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AmountRangeFilter.entries.forEach { option ->
                        FilterOptionChip(
                            selected = amountFilter == option,
                            label = if (isBn) option.titleBn else option.titleEn,
                            onClick = { amountFilter = option },
                            selectedColor = Color(0xFFD97706) // Amber
                        )
                    }
                }

                if (amountFilter == AmountRangeFilter.CUSTOM) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minAmountText,
                            onValueChange = { minAmountText = it },
                            label = { Text(if (isBn) "সর্বনিম্ন ৳" else "Min ৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = maxAmountText,
                            onValueChange = { maxAmountText = it },
                            label = { Text(if (isBn) "সর্বোচ্চ ৳" else "Max ৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Sorting Options
                Text(
                    text = if (isBn) "তথ্য সাজানোর সুবিধা" else "Sorting Options",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SortOption.entries.forEach { option ->
                        FilterOptionChip(
                            selected = sortBy == option,
                            label = if (isBn) option.titleBn else option.titleEn,
                            onClick = { sortBy = option },
                            selectedColor = Color(0xFF475569) // Slate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Preset Section
                if (showSavePresetField) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text(if (isBn) "ফিল্টার প্রিসেটের নাম দিন" else "Preset Name (e.g. Cash Sales)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (presetName.isNotBlank()) {
                                onSavePreset(presetName)
                                showSavePresetField = false
                                presetName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogChipSelectedColor)
                    ) {
                        Text(if (isBn) "প্রিসেট সেভ করুন" else "Save Preset")
                    }
                } else {
                    TextButton(
                        onClick = { showSavePresetField = true }
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "এই ফিল্টারটি প্রিসেট হিসেবে সেভ করুন" else "Save as Filter Preset", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Action Buttons: Reset & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onReset()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBn) "রিসেট" else "Reset")
                    }

                    Button(
                        onClick = {
                            val minVal = minAmountText.toLongOrNull()?.let { it * 100 }
                            val maxVal = maxAmountText.toLongOrNull()?.let { it * 100 }
                            val newFilter = currentFilter.copy(
                                dateRangeOption = dateRangeOption,
                                customStartDate = if (dateRangeOption == DateRangeOption.CUSTOM) customStartDate else null,
                                customEndDate = if (dateRangeOption == DateRangeOption.CUSTOM) customEndDate else null,
                                selectedCustomerId = selectedCustomerId,
                                selectedSupplierId = selectedSupplierId,
                                selectedWarehouseId = selectedWarehouseId,
                                paymentStatus = paymentStatus,
                                deliveryReturn = deliveryReturn,
                                financialMetric = financialMetric,
                                amountFilter = amountFilter,
                                customMinAmountMinor = minVal,
                                customMaxAmountMinor = maxVal,
                                sortBy = sortBy
                            )
                            onApply(newFilter)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogChipSelectedColor)
                    ) {
                        Text(if (isBn) "ফিল্টার প্রয়োগ করুন" else "Apply Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    selectedColor: Color
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        } else null,
        label = {
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = selectedColor,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            selectedBorderColor = selectedColor,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
