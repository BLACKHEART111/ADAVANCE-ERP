package com.pocketdev.advance_erp.feature.sales

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceDetailDialog(
    invoice: SalesInvoice,
    items: List<SalesInvoiceItem>,
    allocations: List<PaymentAllocation>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onPostInvoice: () -> Unit,
    onRecordPayment: () -> Unit,
    onProcessReturn: () -> Unit,
    onSharePdf: (Context) -> Unit,
    onDispatchDelivery: () -> Unit = {},
    onDirectExchange: () -> Unit = {},
    onConvertToSalesOrder: () -> Unit = {},
    onConvertToInvoice: () -> Unit = {},
    onPrintThermalReceipt: (Context) -> Unit = {},
    onCancelOrDeleteInvoice: (reason: String) -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

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
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = invoice.invoiceNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val badgeColor = when (invoice.status) {
                                "PAID" -> StatusSuccess
                                "POSTED" -> SecondaryTeal
                                "PARTIALLY_PAID" -> Color(0xFFF59E0B)
                                else -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = invoice.status,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Text(
                            text = dateFormat.format(Date(invoice.invoiceDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (invoice.status != "CANCELLED") {
                            IconButton(onClick = { showCancelConfirmDialog = true }) {
                                Icon(
                                    imageVector = if (!invoice.isPosted) Icons.Default.DeleteOutline else Icons.Default.Cancel,
                                    contentDescription = if (!invoice.isPosted) "Delete Draft" else "Cancel Invoice",
                                    tint = StatusError
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                if (invoice.status == "CANCELLED") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        color = StatusError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⛔ " + if (isBangla) "চালানটি বাতিল করা হয়েছে (CANCELLED)" else "INVOICE CANCELLED",
                                color = StatusError,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!invoice.cancellationReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (isBangla) "কারণ: " else "Reason: "}${invoice.cancellationReason}",
                                    color = StatusError,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer & Warehouse details card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isBangla) "গ্রাহকের নাম:" else "Customer:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = invoice.customerName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (!invoice.customerPhone.isNullOrBlank()) {
                                        Text(invoice.customerPhone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isBangla) "ওয়্যারহাউস:" else "Warehouse:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = invoice.warehouseName,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }

                    // Items Table
                    item {
                        Text(
                            text = if (isBangla) "চালানের পণ্যসমূহ (${items.size})" else "Line Items (${items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    itemsIndexed(items) { idx, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 0.5.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${if (isBangla && item.productNameBn.isNotBlank()) item.productNameBn else item.productNameEn}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text("SKU: ${item.sku}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "৳" + Formatters.formatMinorUnit(item.lineTotalMinor),
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (item.discountMinor > 0) {
                                            Text(
                                                text = "Disc: -৳${Formatters.formatMinorUnit(item.discountMinor)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFE53E3E)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Qty: ${item.quantity} × ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (item.returnedQuantity > 0) {
                                        Text(
                                            text = "Returned: ${item.returnedQuantity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Red,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (invoice.isPosted && item.unitCostMinor > 0) {
                                        Text(
                                            text = "Unit COGS: ৳${Formatters.formatMinorUnit(item.unitCostMinor)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryTeal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Financial Summary Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DeepNavy
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal:", color = Color.White.copy(alpha = 0.8f))
                                    Text("৳" + Formatters.formatMinorUnit(invoice.subtotalMinor), color = Color.White)
                                }
                                if (invoice.discountMinor > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Invoice Discount:", color = Color(0xFFFF8A80))
                                        Text("-৳" + Formatters.formatMinorUnit(invoice.discountMinor), color = Color(0xFFFF8A80))
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Grand Total:", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paid Amount:", color = StatusSuccess)
                                    Text("৳" + Formatters.formatMinorUnit(invoice.paidAmountMinor), color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Balance Due:", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                                    Text("৳" + Formatters.formatMinorUnit(invoice.dueAmountMinor), color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                                }
                                if (invoice.isPosted && invoice.totalCogsMinor > 0) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.15f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total COGS Recognized:", color = SecondaryTeal, style = MaterialTheme.typography.bodySmall)
                                        Text("৳" + Formatters.formatMinorUnit(invoice.totalCogsMinor), color = SecondaryTeal, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Payment Allocations
                    if (allocations.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isBangla) "পেমেন্ট ইতিহাস (${allocations.size})" else "Payment Allocations (${allocations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        itemsIndexed(allocations) { _, alloc ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Payment #${alloc.paymentId.take(8)}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    Text("৳" + Formatters.formatMinorUnit(alloc.amountMinor), fontWeight = FontWeight.Bold, color = StatusSuccess)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DeepNavy.copy(alpha = 0.1f))

                // Quotation / Sales Order Conversion Banner
                if (invoice.status == "QUOTATION") {
                    Button(
                        onClick = onConvertToSalesOrder,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "বিক্রয় আদেশে রূপান্তর করুন (Sales Order)" else "Convert to Sales Order")
                    }
                } else if (invoice.status == "SALES_ORDER") {
                    Button(
                        onClick = onConvertToInvoice,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBangla) "বাণিজ্যিক চালানে রূপান্তর করুন (Sales Invoice)" else "Convert to Commercial Invoice")
                    }
                }

                // Delivery Challan & Exchange Actions for Posted Invoices
                if (invoice.isPosted && invoice.status != "CANCELLED") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDispatchDelivery,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBangla) "ডেলিভারি চালান" else "Dispatch Challan", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = onDirectExchange,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBangla) "পণ্য বিনিময়" else "Direct Exchange", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share PDF
                    OutlinedButton(
                        onClick = { onSharePdf(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "PDF" else "PDF")
                    }

                    // Thermal Receipt
                    OutlinedButton(
                        onClick = { onPrintThermalReceipt(context) },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "রশিদ (Thermal)" else "Receipt")
                    }

                    // Post or Pay or Return
                    if (!invoice.isPosted && invoice.status != "QUOTATION" && invoice.status != "SALES_ORDER" && invoice.status != "CANCELLED") {
                        Button(
                            onClick = onPostInvoice,
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "পোস্ট করুন" else "Post Invoice")
                        }
                    } else if (invoice.isPosted && invoice.status != "CANCELLED") {
                        if (invoice.dueAmountMinor > 0) {
                            Button(
                                onClick = onRecordPayment,
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBangla) "পেমেন্ট" else "Pay")
                            }
                        }
                        OutlinedButton(
                            onClick = onProcessReturn,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "ফেরত" else "Return")
                        }
                    }
                }
            }
        }

        if (showCancelConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmDialog = false },
                title = {
                    Text(
                        text = if (!invoice.isPosted) {
                            if (isBangla) "ড্রাফট চালান মুছুন" else "Delete Draft Invoice"
                        } else {
                            if (isBangla) "বিক্রয় চালান বাতিল করুন" else "Cancel Sales Invoice"
                        },
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!invoice.isPosted) {
                            Text(
                                text = if (isBangla)
                                    "আপনি কি নিশ্চিত যে #${invoice.invoiceNumber} ড্রাফট চালানটি স্থায়ীভাবে মুছে ফেলতে চান? এটি স্টক বা অ্যাকাউন্টিংয়ে কোনো প্রভাব ফেলবে না।"
                                else
                                    "Are you sure you want to permanently delete draft invoice #${invoice.invoiceNumber}?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = if (isBangla)
                                    "⚠️ সতর্কবার্তা: চালানটি বাতিল করলে:\n• ওয়্যারহাউজের স্টক এবং FIFO কস্ট লেয়ার পুনরুদ্ধার হবে।\n• বিক্রয় আয় ও দেনাদার হিসাব স্বয়ংক্রিয়ভাবে রিভার্স হবে।\n• সংগৃহীত পেমেন্ট থাকলে তা কাস্টমার অগ্রিম (Advance) হিসেবে জমা হবে।\n\nচালান বাতিলের কারণ লিখুন:"
                                else
                                    "⚠️ Warning: Cancelling this invoice will:\n• Restore warehouse stock & FIFO cost layers.\n• Reverse sales revenue & accounts receivable.\n• Convert any allocated payments into customer advance credits.\n\nPlease provide a cancellation reason:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = cancelReason,
                                onValueChange = { cancelReason = it },
                                label = { Text(if (isBangla) "বাতিলের কারণ" else "Cancellation Reason") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val reasonToSend = if (!invoice.isPosted) "Draft deleted by user" else cancelReason.trim()
                            if (invoice.isPosted && reasonToSend.isBlank()) return@TextButton
                            showCancelConfirmDialog = false
                            onCancelOrDeleteInvoice(reasonToSend)
                            onDismiss()
                        },
                        enabled = !invoice.isPosted || cancelReason.isNotBlank()
                    ) {
                        Text(
                            text = if (!invoice.isPosted) (if (isBangla) "হ্যাঁ, মুছুন" else "Delete") else (if (isBangla) "চালান বাতিল করুন" else "Confirm Cancel"),
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmDialog = false }) {
                        Text(if (isBangla) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
