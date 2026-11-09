package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.Supplier
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun RecordSupplierPaymentDialog(
    suppliers: List<Supplier>,
    orders: List<PurchaseOrder>,
    preselectedSupplierId: String? = null,
    preselectedOrderId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?
    ) -> Unit
) {
    val initialOrder = orders.find { it.id == preselectedOrderId }
    val initialSupplierId = preselectedSupplierId ?: initialOrder?.supplierId ?: suppliers.firstOrNull()?.id ?: ""
    val initialAmount = if (initialOrder != null) {
        val remaining = (initialOrder.totalAmountMinor - initialOrder.paidAmountMinor).coerceAtLeast(0L)
        val major = remaining / 100
        val minor = remaining % 100
        String.format(java.util.Locale.US, "%d.%02d", major, minor)
    } else "15000.00"

    var selectedSupplierId by remember { mutableStateOf(initialSupplierId) }
    var selectedOrderId by remember { mutableStateOf(preselectedOrderId ?: orders.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf(initialAmount) }
    var selectedCurrency by remember { mutableStateOf(initialOrder?.currency ?: "CNY") }
    var exchangeRateStr by remember { mutableStateOf(initialOrder?.exchangeRateToBdt ?: "17.00") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BANK_TT) }
    var paymentAccount by remember { mutableStateOf("City Bank Corporate (Import LC)") }
    var referenceNumber by remember { mutableStateOf("SWIFT-TT-882910") }
    var notes by remember { mutableStateOf(if (initialOrder != null) "Payment for ${initialOrder.poNumber}" else "Factory Advance TT Payment") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Supplier Advance / Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = "Posts to General Ledger (Dr Accounts Payable, Cr Bank)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Supplier Selection
                    item {
                        Text("Select Supplier:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            suppliers.forEach { sup ->
                                val isSel = sup.id == selectedSupplierId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                    .clickable { selectedSupplierId = sup.id }
                                    .padding(8.dp)
                                ) {
                                    Text(sup.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Link PO (Optional)
                    item {
                        Text("Link to Purchase Order (Optional):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedOrderId == null) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                    .clickable { selectedOrderId = null }
                                    .padding(8.dp)
                            ) {
                                Text("General Supplier Advance (No specific PO)", style = MaterialTheme.typography.bodySmall)
                            }

                            orders.filter { it.supplierId == selectedSupplierId }.forEach { po ->
                                val isPoSel = selectedOrderId == po.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPoSel) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                        .clickable {
                                            selectedOrderId = po.id
                                            selectedCurrency = po.currency
                                            exchangeRateStr = po.exchangeRateToBdt
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${po.poNumber} (${po.status})", style = MaterialTheme.typography.bodySmall, fontWeight = if (isPoSel) FontWeight.Bold else FontWeight.Normal)
                                    Text("${po.currency} ${po.totalAmountMinor / 100}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }

                    // Amount & Currency
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                label = { Text("Amount ($selectedCurrency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exchangeRateStr,
                                onValueChange = { exchangeRateStr = it },
                                label = { Text("Rate to BDT") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    // Payment Method Chips
                    item {
                        Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PaymentMethod.entries.forEach { pm ->
                                val isSel = selectedMethod == pm
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple else Color(0xFFE2E8F0))
                                        .clickable { selectedMethod = pm }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(pm.labelEn.substringBefore(" "), color = if (isSel) Color.White else TextPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Payment Account & Reference
                    item {
                        OutlinedTextField(
                            value = paymentAccount,
                            onValueChange = { paymentAccount = it },
                            label = { Text("Payment Account / Bank") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = { referenceNumber = it },
                            label = { Text("SWIFT / Bank Reference Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (errorMessage != null) {
                        item {
                            Text(errorMessage!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            try {
                                val rate = BigDecimal(exchangeRateStr.trim())
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be greater than zero" }
                                require(rate > BigDecimal.ZERO) { "Rate must be positive" }

                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

                                onConfirm(
                                    selectedSupplierId,
                                    selectedOrderId,
                                    minor,
                                    selectedCurrency,
                                    rate,
                                    selectedMethod,
                                    paymentAccount,
                                    referenceNumber.ifBlank { null },
                                    notes.ifBlank { null }
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid payment parameters"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Record & Post Payment")
                    }
                }
            }
        }
    }
}
