package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectExchangeDialog(
    invoice: SalesInvoice,
    invoiceItems: List<SalesInvoiceItem>,
    allProducts: List<Product>,
    allVariants: List<ProductVariant>,
    onDismiss: () -> Unit,
    onExchangeConfirmed: (DirectExchangeDraft) -> Unit
) {
    var selectedReturnItem by remember { mutableStateOf(invoiceItems.firstOrNull()) }
    var returnQtyText by remember { mutableStateOf("1") }
    var isDamagedReturn by remember { mutableStateOf(false) }

    var selectedReplacementProduct by remember { mutableStateOf(allProducts.firstOrNull()) }
    var selectedReplacementVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var replacementQtyText by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    val returnQty = returnQtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val replacementQty = replacementQtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val returnedCreditMinor = (selectedReturnItem?.unitPriceMinor ?: 0L) * returnQty
    val replacementUnitPriceMinor = selectedReplacementVariant?.variantPriceMinor ?: selectedReplacementProduct?.sellingPriceMinor ?: 0L
    val replacementDebitMinor = replacementUnitPriceMinor * replacementQty
    val priceDiffMinor = replacementDebitMinor - returnedCreditMinor // > 0 means customer pays, < 0 means refund

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Direct Product Exchange / পণ্য বিনিময়",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Original Invoice: ${invoice.invoiceNumber} • Customer: ${invoice.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left Column: Returned Item
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. Returned Item / ফেরত পণ্য", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

                            Text("Select item from invoice:", style = MaterialTheme.typography.bodySmall)
                            invoiceItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedReturnItem?.id == item.id,
                                        onClick = { selectedReturnItem = item }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(item.productNameEn, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        Text("Qty: ${item.quantity} | ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = returnQtyText,
                                onValueChange = { input ->
                                    val q = input.filter { it.isDigit() }
                                    val maxQ = selectedReturnItem?.quantity ?: 1
                                    if ((q.toIntOrNull() ?: 0) <= maxQ) returnQtyText = q
                                },
                                label = { Text("Return Quantity / ফেরতের সংখ্যা") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Condition toggle
                            Text("Product Condition / পণ্যের অবস্থা:", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = !isDamagedReturn,
                                    onClick = { isDamagedReturn = false },
                                    label = { Text("Good / Sellable (পুনর্বিক্রয়যোগ্য)") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = isDamagedReturn,
                                    onClick = { isDamagedReturn = true },
                                    label = { Text("Damaged (কোয়ারেন্টাইন)") }
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                "Return Credit: ৳${Formatters.formatMinorUnit(returnedCreditMinor)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Right Column: Replacement Item
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("2. Replacement Item / প্রতিস্থাপন পণ্য", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            // Product selector (take top 6 or matching)
                            Text("Select Replacement Product:", style = MaterialTheme.typography.bodySmall)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(allProducts.take(8)) { prod ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedReplacementProduct?.id == prod.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        onClick = {
                                            selectedReplacementProduct = prod
                                            selectedReplacementVariant = allVariants.firstOrNull { it.productId == prod.id }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(prod.nameEn, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("৳${Formatters.formatMinorUnit(prod.sellingPriceMinor)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = replacementQtyText,
                                onValueChange = { replacementQtyText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Replacement Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Replacement Debit: ৳${Formatters.formatMinorUnit(replacementDebitMinor)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Settlement Summary Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (priceDiffMinor >= 0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net Price Settlement / সমন্বয়:", fontWeight = FontWeight.Bold)
                            Text(
                                if (priceDiffMinor > 0) "Customer to pay difference / গ্রাহক অতিরিক্ত পরিশোধ করবেন"
                                else if (priceDiffMinor < 0) "Store refund to customer / দোকান থেকে ফেরতযোগ্য"
                                else "Equal exchange / সমমূল্যে পরিবর্তন",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "৳${Formatters.formatMinorUnit(kotlin.math.abs(priceDiffMinor))}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (priceDiffMinor >= 0) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val retItem = selectedReturnItem ?: return@Button
                            val repProduct = selectedReplacementProduct ?: return@Button

                            val draft = DirectExchangeDraft(
                                customerId = invoice.customerId,
                                warehouseId = invoice.warehouseId,
                                originalInvoiceId = invoice.id,
                                returnedItems = listOf(
                                    ReturnItemDraft(
                                        invoiceItemId = retItem.id,
                                        productId = retItem.productId,
                                        variantId = retItem.variantId,
                                        returnQuantity = returnQty,
                                        isDamaged = isDamagedReturn,
                                        damagedQuantity = if (isDamagedReturn) returnQty else 0
                                    )
                                ),
                                replacementItems = listOf(
                                    InvoiceItemDraft(
                                        productId = repProduct.id,
                                        variantId = selectedReplacementVariant?.id,
                                        quantity = replacementQty,
                                        unitPriceMinor = replacementUnitPriceMinor
                                    )
                                ),
                                netDifferenceMinor = priceDiffMinor,
                                paymentMethod = "CASH",
                                notes = "Direct Exchange for ${invoice.invoiceNumber}. $notes"
                            )
                            onExchangeConfirmed(draft)
                        }
                    ) {
                        Text("Confirm Exchange / বিনিময় নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}
