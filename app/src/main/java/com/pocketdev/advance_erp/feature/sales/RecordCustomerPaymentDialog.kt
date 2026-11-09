package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceAllocationDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCustomerPaymentDialog(
    customers: List<Customer>,
    invoices: List<SalesInvoice>,
    preselectedInvoice: SalesInvoice? = null,
    preselectedCustomerId: String? = null,
    nextPaymentNumber: String? = null,
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (CustomerPaymentDraft) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(preselectedInvoice?.customerId ?: preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var customerExpanded by remember { mutableStateOf(false) }

    var amountStr by remember {
        mutableStateOf(
            if (preselectedInvoice != null) {
                (preselectedInvoice.dueAmountMinor / 100.0).toString()
            } else {
                val due = customers.find { it.id == preselectedCustomerId }?.outstandingBalanceMinor ?: 0L
                if (due > 0L) (due / 100.0).toString() else "0.00"
            }
        )
    }

    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var methodExpanded by remember { mutableStateOf(false) }

    val accounts = listOf(
        Pair("coa-1010", "Cash in Hand (নগদ তহবিল)"),
        Pair("coa-1020", "City Bank Corporate (সিটি ব্যাংক)"),
        Pair("coa-1030", "Islami Bank Bangladesh Ltd. (ইসলামী ব্যাংক)")
    )
    var selectedAccountId by remember { mutableStateOf(accounts.first().first) }
    var accountExpanded by remember { mutableStateOf(false) }

    var referenceNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val unpaidInvoicesForCustomer = remember(selectedCustomerId, invoices) {
        invoices.filter {
            it.customerId == selectedCustomerId &&
                    it.isPosted &&
                    it.dueAmountMinor > 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(16.dp))
                .navigationBarsPadding()
                .imePadding(),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "গ্রাহকের পেমেন্ট গ্রহণ" else "Record Customer Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla) "বকেয়া বিল আদায় অথবা অগ্রিম জমা" else "Collect receivables or advance deposit",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Tracking / Receipt Number Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryPurple.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "পেমেন্ট ট্র্যাকিং / রশিদ নং:" else "Payment Tracking / Receipt #:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurple
                        ) {
                            Text(
                                text = nextPaymentNumber ?: "AUTO-GEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Selector
                    item {
                        Text(
                            text = if (isBangla) "গ্রাহক / CUSTOMER *" else "CUSTOMER *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Select Customer",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} (Due: ৳${Formatters.formatMinorUnit(cust.outstandingBalanceMinor)})") },
                                        onClick = {
                                            selectedCustomerId = cust.id
                                            customerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Amount
                    item {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text(if (isBangla) "আদায়কৃত টাকার পরিমাণ (৳) *" else "Amount Received (৳) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Payment Method & Account
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Method
                            ExposedDropdownMenuBox(
                                expanded = methodExpanded,
                                onExpandedChange = { methodExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedPaymentMethod,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Method") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = methodExpanded,
                                    onDismissRequest = { methodExpanded = false }
                                ) {
                                    listOf("CASH", "BANK", "CHEQUE", "BKASH", "NAGAD").forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m) },
                                            onClick = {
                                                selectedPaymentMethod = m
                                                methodExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Account
                            val curAccount = accounts.find { it.first == selectedAccountId }
                            ExposedDropdownMenuBox(
                                expanded = accountExpanded,
                                onExpandedChange = { accountExpanded = it },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                OutlinedTextField(
                                    value = curAccount?.second?.take(16) + "...",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Deposit Account") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = accountExpanded,
                                    onDismissRequest = { accountExpanded = false }
                                ) {
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text(acc.second) },
                                            onClick = {
                                                selectedAccountId = acc.first
                                                accountExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reference Note
                    item {
                        OutlinedTextField(
                            value = referenceNote,
                            onValueChange = { referenceNote = it },
                            label = { Text(if (isBangla) "মানি রিসিট / চেক নং / রেফারেন্স" else "Cheque # / Slip # / Reference") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Open Invoices preview
                    if (unpaidInvoicesForCustomer.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = DeepNavy.copy(alpha = 0.05f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isBangla) "বকেয়া ইনভয়েসসমূহ (${unpaidInvoicesForCustomer.size}):" else "Open Invoices (${unpaidInvoicesForCustomer.size}):",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    unpaidInvoicesForCustomer.take(3).forEach { inv ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(inv.invoiceNumber, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("Due: ৳" + Formatters.formatMinorUnit(inv.dueAmountMinor), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFC53030))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBangla) "পেমেন্ট স্বয়ংক্রিয়ভাবে ক্রমানুসারে ইনভয়েসে সমন্বয় হবে।" else "Payments will be automatically applied FIFO against open invoices.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = runCatching { Money.parseFromString(amountStr).minorUnits }.getOrDefault(0L)
                            if (amount <= 0) {
                                errorMessage = "Please enter a valid amount > 0"
                                return@Button
                            }
                            if (selectedCustomerId.isBlank()) {
                                errorMessage = "Please select a customer"
                                return@Button
                            }

                            val allocations = if (preselectedInvoice != null) {
                                listOf(InvoiceAllocationDraft(preselectedInvoice.id, minOf(amount, preselectedInvoice.dueAmountMinor)))
                            } else emptyList()

                            onSubmit(
                                CustomerPaymentDraft(
                                    customerId = selectedCustomerId,
                                    amountMinor = amount,
                                    paymentMethod = selectedPaymentMethod,
                                    accountId = selectedAccountId,
                                    paymentDate = System.currentTimeMillis(),
                                    referenceNote = referenceNote.ifBlank { null },
                                    allocations = allocations
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "পেমেন্ট সংরক্ষণ করুন" else "Record & Post Payment")
                    }
                }
            }
        }
    }
}
