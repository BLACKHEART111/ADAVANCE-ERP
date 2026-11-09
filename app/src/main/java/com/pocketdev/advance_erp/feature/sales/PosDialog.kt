package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.feature.inventory.BarcodeQrScannerDialog

data class PosCartItem(
    val product: Product,
    val variant: ProductVariant? = null,
    var quantity: Int = 1,
    var unitPriceMinor: Long,
    var discountMinor: Long = 0L
) {
    val lineGrossMinor: Long get() = quantity * unitPriceMinor
    val lineTotalMinor: Long get() = (lineGrossMinor - discountMinor).coerceAtLeast(0L)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosDialog(
    products: List<Product>,
    variants: List<ProductVariant>,
    warehouses: List<Warehouse>,
    customers: List<Customer>,
    salesViewModel: SalesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var isWholesale by remember { mutableStateOf(false) }

    val cartItems = remember { mutableStateListOf<PosCartItem>() }
    var barcodeInput by remember { mutableStateOf("") }
    var showScannerDialog by remember { mutableStateOf(false) }

    var overallDiscountPercent by remember { mutableStateOf(false) }
    var overallDiscountText by remember { mutableStateOf("0") }
    var cashTenderedText by remember { mutableStateOf("") }
    var autoPrintReceipt by remember { mutableStateOf(true) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var lastCreatedReceiptNote by remember { mutableStateOf<String?>(null) }

    // Computations
    val subtotalMinor = cartItems.sumOf { it.lineTotalMinor }
    val discountVal = overallDiscountText.toLongOrNull() ?: 0L
    val invoiceDiscountMinor = if (overallDiscountPercent) {
        val pct = discountVal.coerceIn(0L, 100L)
        (subtotalMinor * pct) / 100L
    } else {
        (discountVal * 100L).coerceAtMost(subtotalMinor)
    }
    val netTotalMinor = (subtotalMinor - invoiceDiscountMinor).coerceAtLeast(0L)
    val tenderedMinor = ((cashTenderedText.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceAtLeast(0L)
    val changeDueMinor = (tenderedMinor - netTotalMinor).coerceAtLeast(0L)

    fun addOrIncrementBarcode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) return

        // 1. Search variants
        val matchedVariant = variants.find {
            it.barcode.equals(code, ignoreCase = true) || it.sku.equals(code, ignoreCase = true)
        }

        if (matchedVariant != null) {
            val prod = products.find { it.id == matchedVariant.productId }
            if (prod != null) {
                val existing = cartItems.find { it.variant?.id == matchedVariant.id }
                if (existing != null) {
                    val idx = cartItems.indexOf(existing)
                    cartItems[idx] = existing.copy(quantity = existing.quantity + 1)
                } else {
                    val basePrice = matchedVariant.variantPriceMinor
                    val price = if (isWholesale) (basePrice * 90L) / 100L else basePrice
                    cartItems.add(PosCartItem(prod, matchedVariant, 1, price))
                }
                barcodeInput = ""
                return
            }
        }

        // 2. Search products
        val matchedProduct = products.find {
            it.barcode?.equals(code, ignoreCase = true) == true || it.baseSku.equals(code, ignoreCase = true)
        }

        if (matchedProduct != null) {
            val existing = cartItems.find { it.product.id == matchedProduct.id && it.variant == null }
            if (existing != null) {
                val idx = cartItems.indexOf(existing)
                cartItems[idx] = existing.copy(quantity = existing.quantity + 1)
            } else {
                val basePrice = matchedProduct.sellingPriceMinor
                val price = if (isWholesale) (basePrice * 90L) / 100L else basePrice
                cartItems.add(PosCartItem(matchedProduct, null, 1, price))
            }
            barcodeInput = ""
        }
    }

    val dueMinor = (netTotalMinor - tenderedMinor).coerceAtLeast(0L)
    val isWalkIn = selectedCustomer == null
    val canCheckout = cartItems.isNotEmpty() && !isCheckingOut && selectedWarehouse != null &&
        (!isWalkIn || dueMinor == 0L)
    var customerMenu by remember { mutableStateOf(false) }
    var warehouseMenu by remember { mutableStateOf(false) }

    val suggestions = remember(barcodeInput, products) {
        val q = barcodeInput.trim()
        if (q.length < 2) emptyList()
        else products.filter {
            it.nameEn.contains(q, ignoreCase = true) || it.baseSku.contains(q, ignoreCase = true)
        }.take(5)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Compact header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("POS / দ্রুত বিক্রয়", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    FilterChip(
                        selected = isWholesale,
                        onClick = {
                            isWholesale = !isWholesale
                            for (i in cartItems.indices) {
                                val item = cartItems[i]
                                val basePrice = item.variant?.variantPriceMinor ?: item.product.sellingPriceMinor
                                cartItems[i] = item.copy(unitPriceMinor = if (isWholesale) (basePrice * 90L) / 100L else basePrice)
                            }
                        },
                        label = { Text(if (isWholesale) "পাইকারি" else "খুচরা", maxLines = 1) }
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Everything scrolls vertically (phone-friendly single column)
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customer & warehouse
                    ExposedDropdownMenuBox(expanded = customerMenu, onExpandedChange = { customerMenu = it }) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Walk-in Cash Customer (নগদ ক্রেতা)",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Customer / ক্রেতা") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = customerMenu, onDismissRequest = { customerMenu = false }) {
                            DropdownMenuItem(text = { Text("Walk-in Cash Customer (নগদ ক্রেতা)") }, onClick = { selectedCustomer = null; customerMenu = false })
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.name} • ${c.phone}") }, onClick = { selectedCustomer = c; customerMenu = false })
                            }
                        }
                    }
                    if (warehouses.size > 1) {
                        ExposedDropdownMenuBox(expanded = warehouseMenu, onExpandedChange = { warehouseMenu = it }) {
                            OutlinedTextField(
                                value = selectedWarehouse?.name ?: "", onValueChange = {}, readOnly = true,
                                label = { Text("Warehouse / গুদাম") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = warehouseMenu, onDismissRequest = { warehouseMenu = false }) {
                                warehouses.forEach { w ->
                                    DropdownMenuItem(text = { Text(w.name) }, onClick = { selectedWarehouse = w; warehouseMenu = false })
                                }
                            }
                        }
                    }

                    // Scan / search
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = {
                                barcodeInput = it
                                if (it.endsWith("\n") || (it.length in 8..14 && it.all { ch -> ch.isDigit() })) {
                                    addOrIncrementBarcode(it.trim())
                                }
                            },
                            label = { Text("Barcode / SKU / পণ্যের নাম") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addOrIncrementBarcode(barcodeInput) })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showScannerDialog = true },
                            modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    suggestions.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { addOrIncrementBarcode(p.baseSku.ifBlank { p.barcode ?: "" }) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${p.nameEn} (${p.baseSku})", modifier = Modifier.weight(1f), maxLines = 1)
                            Text("৳${Formatters.formatMinorUnit(p.sellingPriceMinor)}", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Cart
                    if (cartItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Cart is empty / কার্ট খালি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Scan barcode or search a product", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        cartItems.toList().forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.product.nameEn, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            val sub = item.variant?.let { "${it.color ?: ""} ${it.size ?: ""} | SKU: ${it.sku}" } ?: "SKU: ${item.product.baseSku}"
                                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        IconButton(onClick = { cartItems.remove(item) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = {
                                            val idx = cartItems.indexOf(item)
                                            if (idx >= 0) { if (item.quantity > 1) cartItems[idx] = item.copy(quantity = item.quantity - 1) else cartItems.removeAt(idx) }
                                        }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                                        // Editable quantity
                                        OutlinedTextField(
                                            value = item.quantity.toString(),
                                            onValueChange = { t ->
                                                val q = t.filter { it.isDigit() }.toIntOrNull()
                                                val idx = cartItems.indexOf(item)
                                                if (idx >= 0 && q != null && q > 0) cartItems[idx] = item.copy(quantity = q)
                                            },
                                            label = { Text("Qty") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(80.dp)
                                        )
                                        IconButton(onClick = {
                                            val idx = cartItems.indexOf(item)
                                            if (idx >= 0) cartItems[idx] = item.copy(quantity = item.quantity + 1)
                                        }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, contentDescription = "Increase") }
                                        // Editable unit price
                                        var priceText by remember(item.product.id, item.variant?.id) { mutableStateOf(Formatters.formatMinorUnit(item.unitPriceMinor).replace(",", "")) }
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { t ->
                                                priceText = t
                                                val v = t.toDoubleOrNull()
                                                val idx = cartItems.indexOf(item)
                                                if (idx >= 0 && v != null && v >= 0) cartItems[idx] = item.copy(unitPriceMinor = Math.round(v * 100))
                                            },
                                            label = { Text("Price ৳") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text("Line total: ৳${Formatters.formatMinorUnit(item.lineTotalMinor)}", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Payment & Settlement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                        Text("৳${Formatters.formatMinorUnit(subtotalMinor)}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Discount:")
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = overallDiscountPercent,
                                onClick = { overallDiscountPercent = !overallDiscountPercent },
                                label = { Text(if (overallDiscountPercent) "%" else "৳") }
                            )
                        }
                        OutlinedTextField(
                            value = overallDiscountText,
                            onValueChange = { overallDiscountText = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.width(110.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NET PAYABLE:", fontWeight = FontWeight.Bold)
                        Text("৳${Formatters.formatMinorUnit(netTotalMinor)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }

                    Text("Quick Cash Tender:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Exact" to (netTotalMinor / 100.0), "৳500" to 500.0, "৳1000" to 1000.0, "৳2000" to 2000.0).forEach { (btnLabel, amt) ->
                            Button(
                                onClick = { cashTenderedText = "%.0f".format(amt) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) { Text(btnLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                        }
                    }
                    OutlinedTextField(
                        value = cashTenderedText,
                        onValueChange = { cashTenderedText = it },
                        label = { Text("Cash Received / গৃহীত টাকা (৳)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF81C784), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Change / ফেরত:", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                            Text("৳${Formatters.formatMinorUnit(changeDueMinor)}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                    }
                    if (dueMinor > 0L) {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE57373), RoundedCornerShape(8.dp)).padding(10.dp)) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Due / বকেয়া:", fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                                    Text("৳${Formatters.formatMinorUnit(dueMinor)}", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                                }
                                if (isWalkIn) {
                                    Text("বকেয়া বিক্রির জন্য একজন ক্রেতা নির্বাচন করুন / Select a customer to sell on credit", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoPrintReceipt, onCheckedChange = { autoPrintReceipt = it })
                        Text("Auto-print Thermal Receipt", style = MaterialTheme.typography.bodySmall)
                    }
                    lastCreatedReceiptNote?.let { Text(it, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val wh = selectedWarehouse ?: return@Button
                        if (!canCheckout) return@Button
                        isCheckingOut = true
                        val itemDrafts = cartItems.map {
                            InvoiceItemDraft(
                                productId = it.product.id, variantId = it.variant?.id,
                                quantity = it.quantity, unitPriceMinor = it.unitPriceMinor, discountMinor = it.discountMinor
                            )
                        }
                        val paidAmount = tenderedMinor.coerceAtMost(netTotalMinor)
                        val draft = SalesInvoiceDraft(
                            customerId = selectedCustomer?.id ?: "",
                            warehouseId = wh.id,
                            discountMinor = invoiceDiscountMinor,
                            paidAmountMinor = paidAmount,
                            paymentMethod = "CASH",
                            items = itemDrafts,
                            newCustomerDraft = if (selectedCustomer == null) {
                                NewCustomerDraft(name = "Walk-in Customer", phone = "0000000000", address = "Counter")
                            } else null
                        )
                        val changeNow = changeDueMinor
                        val dueNow = dueMinor
                        salesViewModel.createAndConfirmInvoice(draft) {
                            isCheckingOut = false
                            cartItems.clear()
                            cashTenderedText = ""
                            overallDiscountText = "0"
                            lastCreatedReceiptNote = if (dueNow > 0L) "Sale saved. Due: ৳${Formatters.formatMinorUnit(dueNow)}"
                                else "Sale completed! Change: ৳${Formatters.formatMinorUnit(changeNow)}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = canCheckout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(
                        if (isCheckingOut) "Processing..." else "COMPLETE SALE / বিক্রি সম্পন্ন (৳${Formatters.formatMinorUnit(netTotalMinor)})",
                        fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
            }
        }
    }

    if (showScannerDialog) {
        BarcodeQrScannerDialog(
            language = com.pocketdev.advance_erp.core.model.AppLanguage.BANGLA,
            onDismiss = { showScannerDialog = false },
            onCodeScanned = { code ->
                showScannerDialog = false
                addOrIncrementBarcode(code)
            }
        )
    }
}
