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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.data.entity.CostCategory
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AddCostItemDialog(
    shipmentId: String? = null,
    orderId: String? = null,
    totalGrossWeightKg: BigDecimal = BigDecimal("350.0"),
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?,
        isEstimated: Boolean,
        notes: String?
    ) -> Unit
) {
    var isEstimated by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(CostCategory.INTERNATIONAL_FREIGHT) }
    var description by remember { mutableStateOf("International Freight & Air/Sea Consignment") }
    var amountStr by remember { mutableStateOf("42000.00") }
    var selectedCurrency by remember { mutableStateOf("BDT") }
    var exchangeRateStr by remember { mutableStateOf("1.00") }
    var selectedMethod by remember { mutableStateOf(selectedCategory.defaultAllocationMethod) }
    var paymentAccount by remember { mutableStateOf("City Bank Corporate (Import LC)") }
    var docRef by remember { mutableStateOf("BILL-INV-2026-01") }
    var notes by remember { mutableStateOf("") }
    var shippingRatePerKgStr by remember { mutableStateOf("120") }
    var showShippingCalculator by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        CostCategory.INTERNATIONAL_FREIGHT,
        CostCategory.CUSTOMS_DUTY,
        CostCategory.CF_CHARGE,
        CostCategory.PORT_CHARGE,
        CostCategory.CHINA_DOMESTIC_TRANSPORT,
        CostCategory.LOCAL_TRANSPORTATION,
        CostCategory.LOADING_UNLOADING,
        CostCategory.OTHER_EXPENSES
    )

    val methods = listOf(
        AllocationMethod.GROSS_WEIGHT,
        AllocationMethod.QUANTITY,
        AllocationMethod.PURCHASE_VALUE,
        AllocationMethod.CBM,
        AllocationMethod.MANUAL
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "আমদানি খরচ যুক্ত করুন" else "Add Import Expense",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (isBangla) "প্রাক্কলিত বা প্রকৃত বিল • ক্যাটাগরি-ভিত্তিক বণ্টন পদ্ধতি" else "Estimated or Actual Expense • Category-wise Allocation",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Estimated vs Actual Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isEstimated) PrimaryPurple else Color.White)
                            .border(1.dp, if (!isEstimated) PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { isEstimated = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "প্রকৃত খরচ (Actual Bill)" else "Actual Bill / Expense",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isEstimated) Color.White else TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEstimated) PrimaryPurple else Color.White)
                            .border(1.dp, if (isEstimated) PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { isEstimated = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "প্রাক্কলিত খরচ (Estimated)" else "Estimated Expense",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEstimated) Color.White else TextPrimary
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Category Selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isBangla) "খরচের ক্যাটাগরি নির্বাচন করুন:" else "Cost Category:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                categories.forEach { cat ->
                                    val isSel = cat == selectedCategory
                                    val title = if (isBangla) cat.titleBn else cat.titleEn
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFF8F9FA))
                                            .border(1.dp, if (isSel) PrimaryPurple else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedCategory = cat
                                                selectedMethod = cat.defaultAllocationMethod
                                                description = if (isBangla) cat.titleBn else cat.titleEn
                                                showShippingCalculator = cat == CostCategory.INTERNATIONAL_FREIGHT ||
                                                        cat == CostCategory.SEA_FREIGHT ||
                                                        cat == CostCategory.AIR_FREIGHT
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) PrimaryPurple else TextPrimary
                                            )
                                            Text(
                                                text = "Default: ${cat.defaultAllocationMethod.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Gross Weight Shipping Calculator (Mandatory Section 6 rule)
                    if (showShippingCalculator) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBangla) "গ্রস ওয়েট শিপিং ক্যালকুলেটর (Gross Weight × Rate)" else "Gross Weight Shipping Calculator",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }

                                    Text(
                                        text = "Total Gross Weight: ${totalGrossWeightKg.toPlainString()} KG",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = shippingRatePerKgStr,
                                            onValueChange = {
                                                shippingRatePerKgStr = it
                                                val rate = BigDecimal(it.trim().ifEmpty { "0" })
                                                val total = totalGrossWeightKg.multiply(rate)
                                                amountStr = total.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                            },
                                            label = { Text("Rate (BDT/KG)", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        Button(
                                            onClick = {
                                                val rate = BigDecimal(shippingRatePerKgStr.trim().ifEmpty { "0" })
                                                val total = totalGrossWeightKg.multiply(rate)
                                                amountStr = total.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                                selectedMethod = AllocationMethod.GROSS_WEIGHT
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                        ) {
                                            Text("Calculate Total", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Allocation Method Selector (Configurable per category)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isBangla) "বণ্টন পদ্ধতি (Allocation Method):" else "Allocation Method:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                                methods.forEach { m ->
                                    val isSel = m == selectedMethod
                                    val title = if (isBangla) m.titleBn else m.titleEn
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SecondaryTeal.copy(alpha = 0.15f) else Color(0xFFF8F9FA))
                                            .border(1.dp, if (isSel) SecondaryTeal else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .clickable { selectedMethod = m }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) SecondaryTeal else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Amount & Currency
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = amountStr,
                                        onValueChange = { amountStr = it },
                                        label = { Text("Amount") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = selectedCurrency,
                                        onValueChange = { selectedCurrency = it },
                                        label = { Text("Currency") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = docRef,
                                    onValueChange = { docRef = it },
                                    label = { Text(if (isBangla) "বিল / রেফারেন্স নম্বর" else "Bill / Reference Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text(if (isBangla) "মন্তব্য (বাধ্যতামূলক নয়)" else "Notes (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isBangla) "বাতিল" else "Cancel")
                        }

                        Button(
                            onClick = {
                                try {
                                    val amtBd = BigDecimal(amountStr.trim().ifEmpty { "0" })
                                    require(amtBd > BigDecimal.ZERO) { "Amount must be greater than zero" }
                                    val fx = BigDecimal(exchangeRateStr.trim().ifEmpty { "1.00" })
                                    val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

                                    onConfirm(
                                        selectedCategory,
                                        description.trim().ifEmpty { selectedCategory.titleEn },
                                        minor,
                                        selectedCurrency,
                                        fx,
                                        selectedMethod,
                                        paymentAccount,
                                        docRef.trim().ifEmpty { null },
                                        isEstimated,
                                        notes.trim().ifEmpty { null }
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Validation error"
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text(text = if (isBangla) "খরচ সংরক্ষণ করুন" else "Save Expense")
                        }
                    }
                }
            }
        }
    }
}
