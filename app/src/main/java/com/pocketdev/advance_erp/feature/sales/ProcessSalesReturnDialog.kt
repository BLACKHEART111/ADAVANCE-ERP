package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSalesReturnDialog(
    invoice: SalesInvoice,
    items: List<SalesInvoiceItem>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (SalesReturnDraft) -> Unit
) {
    var selectedRefundOption by remember { mutableStateOf(ReturnRefundOption.CUSTOMER_CREDIT) }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Map of invoiceItemId -> returnQuantityStr
    val returnQuantities = remember {
        mutableStateMapOf<String, String>().apply {
            items.forEach { put(it.id, "0") }
        }
    }

    var totalReturnMinor = 0L
    items.forEach { item ->
        val returnQty = returnQuantities[item.id]?.toIntOrNull() ?: 0
        totalReturnMinor += (returnQty.toLong() * item.unitPriceMinor)
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
                .clip(RoundedCornerShape(16.dp)),
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
                    Column {
                        Text(
                            text = if (isBangla) "বিক্রয় ফেরত প্রক্রিয়া / SALES RETURN" else "Process Sales Return",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Invoice #${invoice.invoiceNumber} • ${invoice.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isBangla) "ফেরতযোগ্য পণ্য ও পরিমাণ" else "Select Items & Return Quantity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    itemsIndexed(items) { _, item ->
                        val maxReturnable = item.quantity - item.returnedQuantity
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = item.productNameEn,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Sold: ${item.quantity} • Prev Return: ${item.returnedQuantity} • Price: ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                OutlinedTextField(
                                    value = returnQuantities[item.id] ?: "0",
                                    onValueChange = { returnQuantities[item.id] = it },
                                    label = { Text("Ret Qty (max $maxReturnable)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(130.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Refund Option Selector
                    item {
                        Text(
                            text = if (isBangla) "রিফান্ড / সমন্বয় পদ্ধতি *" else "Refund / Adjustment Option *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ReturnRefundOption.values().forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRefundOption == opt,
                                    onClick = { selectedRefundOption = opt }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) opt.titleBn else opt.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Reason
                    item {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text(if (isBangla) "ফেরতের কারণ (ড্যামেজ / অতিরিক্ত / কোয়ালিটি)" else "Reason (Damaged, Defective, Excess)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Summary Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = DeepNavy
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Return Credit:", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("৳" + Formatters.formatMinorUnit(totalReturnMinor), color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                            val drafts = mutableListOf<ReturnItemDraft>()
                            for (item in items) {
                                val qty = returnQuantities[item.id]?.toIntOrNull() ?: 0
                                val maxReturnable = item.quantity - item.returnedQuantity
                                if (qty > maxReturnable) {
                                    errorMessage = "Cannot return more than $maxReturnable for ${item.productNameEn}"
                                    return@Button
                                }
                                if (qty > 0) {
                                    drafts.add(
                                        ReturnItemDraft(
                                            invoiceItemId = item.id,
                                            productId = item.productId,
                                            variantId = item.variantId,
                                            returnQuantity = qty
                                        )
                                    )
                                }
                            }

                            if (drafts.isEmpty()) {
                                errorMessage = "Please enter return quantity > 0 for at least one item"
                                return@Button
                            }

                            onSubmit(
                                SalesReturnDraft(
                                    invoiceId = invoice.id,
                                    warehouseId = invoice.warehouseId,
                                    refundOption = selectedRefundOption,
                                    reason = reason.ifBlank { null },
                                    returnDate = System.currentTimeMillis(),
                                    items = drafts
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC53030)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "ফেরত নিশ্চিত করুন" else "Confirm Return & Stock Restore")
                    }
                }
            }
        }
    }
}
