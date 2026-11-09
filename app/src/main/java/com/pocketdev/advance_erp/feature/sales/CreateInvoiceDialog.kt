package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import java.math.BigDecimal

class ItemDraftState(
    productId: String,
    variantId: String? = null,
    quantityStr: String = "1",
    unitPriceStr: String = "0.00",
    discountStr: String = "0.00",
    isPercentageDiscount: Boolean = false
) {
    // Backed by Compose state so edits (qty, price, discount) recompose immediately.
    var productId by mutableStateOf(productId)
    var variantId by mutableStateOf(variantId)
    var quantityStr by mutableStateOf(quantityStr)
    var unitPriceStr by mutableStateOf(unitPriceStr)
    var discountStr by mutableStateOf(discountStr)
    var isPercentageDiscount by mutableStateOf(isPercentageDiscount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    customers: List<Customer>,
    warehouses: List<Warehouse>,
    products: List<Product>,
    variants: List<ProductVariant> = emptyList(),
    stockBalances: List<StockBalance> = emptyList(),
    customerSummary: CustomerSummary? = null,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onCustomerSelected: (String) -> Unit = {},
    onSubmit: (SalesInvoiceDraft) -> Unit = {},
    onSubmitDraft: (SalesInvoiceDraft) -> Unit = onSubmit,
    onConfirmSale: (SalesInvoiceDraft) -> Unit = onSubmit
) {
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var pendingNewCustomerDraft by remember { mutableStateOf<NewCustomerDraft?>(null) }
    var showCustomerLookup by remember { mutableStateOf(false) }

    var selectedWarehouseId by remember { mutableStateOf(warehouses.firstOrNull()?.id ?: "") }
    var warehouseExpanded by remember { mutableStateOf(false) }

    var invoiceDiscountStr by remember { mutableStateOf("0.00") }
    var isInvoiceDiscountPercentage by remember { mutableStateOf(false) }
    var additionalChargesStr by remember { mutableStateOf("0.00") }
    var paidAmountStr by remember { mutableStateOf("0.00") }
    var isFullPaymentSelected by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, BANK, MFS, CREDIT
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Option C Credit Limit Control State
    var showCreditLimitWarningDialog by remember { mutableStateOf(false) }
    var creditLimitApproved by remember { mutableStateOf(false) }
    var creditApproverName by remember { mutableStateOf("Manager") }
    var creditApprovalReason by remember { mutableStateOf("Authorized credit override") }

    LaunchedEffect(selectedCustomerId) {
        if (selectedCustomerId.isNotBlank()) {
            onCustomerSelected(selectedCustomerId)
        }
    }

    val variantsByProduct = remember(variants) {
        variants.groupBy { it.productId }
    }
    val productsMap = remember(products) {
        products.associateBy { it.id }
    }

    val itemsList = remember {
        mutableStateListOf<ItemDraftState>().apply {
            if (products.isNotEmpty()) {
                val p = products.first()
                val prodVariants = variantsByProduct[p.id] ?: emptyList()
                val firstVariant = prodVariants.firstOrNull()
                val initialPriceMinor = firstVariant?.variantPriceMinor ?: p.sellingPriceMinor
                add(
                    ItemDraftState(
                        productId = p.id,
                        variantId = firstVariant?.id,
                        quantityStr = "1",
                        unitPriceStr = BigDecimal(initialPriceMinor).divide(BigDecimal(100)).toPlainString()
                    )
                )
            }
        }
    }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedWarehouse = warehouses.find { it.id == selectedWarehouseId }

    // Real-time Calculations
    var subtotalMinor = 0L
    itemsList.forEach { item ->
        val qty = item.quantityStr.toIntOrNull() ?: 0
        val price = runCatching { Money.parseFromString(item.unitPriceStr).minorUnits }.getOrDefault(0L)
        val lineGross = qty * price
        val discAmount = if (item.isPercentageDiscount) {
            val pct = item.discountStr.toDoubleOrNull() ?: 0.0
            val validPct = pct.coerceIn(0.0, 100.0)
            ((lineGross * validPct) / 100.0).toLong()
        } else {
            runCatching { Money.parseFromString(item.discountStr).minorUnits }.getOrDefault(0L)
        }
        val validDisc = minOf(discAmount, lineGross)
        val line = maxOf(0L, lineGross - validDisc)
        subtotalMinor += line
    }

    val rawInvDiscountMinor = if (isInvoiceDiscountPercentage) {
        val pct = invoiceDiscountStr.toDoubleOrNull() ?: 0.0
        val validPct = pct.coerceIn(0.0, 100.0)
        ((subtotalMinor * validPct) / 100.0).toLong()
    } else {
        runCatching { Money.parseFromString(invoiceDiscountStr).minorUnits }.getOrDefault(0L)
    }
    val invDiscountMinor = minOf(rawInvDiscountMinor, subtotalMinor)
    val addChargesMinor = runCatching { Money.parseFromString(additionalChargesStr).minorUnits }.getOrDefault(0L)
    val grandTotalMinor = maxOf(0L, subtotalMinor - invDiscountMinor + addChargesMinor)

    val currentPaidMinor = if (isFullPaymentSelected) {
        grandTotalMinor
    } else {
        runCatching { Money.parseFromString(paidAmountStr).minorUnits }.getOrDefault(0L)
    }
    val dueAmountMinor = maxOf(0L, grandTotalMinor - currentPaidMinor)

    // Helper: Compute sellable stock for (warehouse, product, variant)
    fun getSellableStock(productId: String, variantId: String?): Int {
        if (selectedWarehouseId.isBlank()) return 0
        val balance = if (variantId != null) {
            stockBalances.find { it.warehouseId == selectedWarehouseId && it.productId == productId && it.variantId == variantId }
                ?: stockBalances.find { it.warehouseId == selectedWarehouseId && it.productId == productId }
        } else {
            stockBalances.find { it.warehouseId == selectedWarehouseId && it.productId == productId }
        }
        return maxOf(0, (balance?.quantity ?: 0) - (balance?.reservedQuantity ?: 0))
    }

    fun buildInvoiceDraft(): SalesInvoiceDraft? {
        if (selectedCustomerId.isBlank() && pendingNewCustomerDraft == null) {
            errorMessage = if (isBangla) "অনুগ্রহ করে একজন গ্রাহক নির্বাচন করুন অথবা নতুন গ্রাহক যোগ করুন" else "Please select or create a customer"
            return null
        }
        if (selectedWarehouseId.isBlank()) {
            errorMessage = if (isBangla) "অনুগ্রহ করে ওয়্যারহাউস নির্বাচন করুন" else "Please select a warehouse"
            return null
        }
        if (itemsList.isEmpty()) {
            errorMessage = if (isBangla) "কমপক্ষে একটি পণ্য যোগ করুন" else "Please add at least one item"
            return null
        }

        val draftItems = itemsList.mapNotNull { item ->
            val qty = item.quantityStr.toIntOrNull() ?: 0
            val price = runCatching { Money.parseFromString(item.unitPriceStr).minorUnits }.getOrDefault(0L)
            val lineGross = qty * price
            val discAmount = if (item.isPercentageDiscount) {
                val pct = item.discountStr.toDoubleOrNull() ?: 0.0
                val validPct = pct.coerceIn(0.0, 100.0)
                ((lineGross * validPct) / 100.0).toLong()
            } else {
                runCatching { Money.parseFromString(item.discountStr).minorUnits }.getOrDefault(0L)
            }
            val disc = minOf(discAmount, lineGross)
            if (qty > 0 && price >= 0) {
                InvoiceItemDraft(
                    productId = item.productId,
                    variantId = item.variantId,
                    quantity = qty,
                    unitPriceMinor = price,
                    discountMinor = disc
                )
            } else null
        }

        if (draftItems.isEmpty()) {
            errorMessage = if (isBangla) "সব পণ্যের সঠিক পরিমাণ ও মূল্য দিন" else "All items must have valid quantity > 0 and price >= 0"
            return null
        }

        val paymentAccountId = when (paymentMethod) {
            "BANK" -> "coa-1020"
            "MFS" -> "coa-1030"
            else -> "coa-1010"
        }

        return SalesInvoiceDraft(
            customerId = selectedCustomerId,
            warehouseId = selectedWarehouseId,
            invoiceDate = System.currentTimeMillis(),
            discountMinor = invDiscountMinor,
            additionalChargesMinor = addChargesMinor,
            paidAmountMinor = currentPaidMinor,
            paymentMethod = paymentMethod,
            paymentAccountId = paymentAccountId,
            notes = notes.ifBlank { null },
            items = draftItems,
            newCustomerDraft = pendingNewCustomerDraft
        )
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
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "নতুন বিক্রয় চালান / NEW SALE" else "New Sales Invoice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isBangla) "গ্রাহক তথ্য, ভেরিয়েন্ট পণ্য ও পেমেন্ট" else "Customer details, variant items & payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DeepNavy.copy(alpha = 0.08f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Customer Summary Card
                    item {
                        if (pendingNewCustomerDraft != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(SecondaryTeal.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${pendingNewCustomerDraft!!.name} (New Customer)",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${pendingNewCustomerDraft!!.phone} • ${pendingNewCustomerDraft!!.address}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showCustomerLookup = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isBangla) "পরিবর্তন" else "Change", fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            CustomerSummaryCard(
                                customer = selectedCustomer,
                                summary = customerSummary,
                                isBangla = isBangla,
                                onChangeCustomerClick = { showCustomerLookup = true }
                            )
                        }
                    }

                    // 2. Dispatch Warehouse Selector
                    item {
                        Text(
                            text = if (isBangla) "বিতরণ ওয়্যারহাউস / DISPATCH WAREHOUSE *" else "DISPATCH WAREHOUSE *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = warehouseExpanded,
                            onExpandedChange = { warehouseExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedWarehouse?.let { "${it.name} (${it.code})" } ?: "Select Warehouse",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = warehouseExpanded,
                                onDismissRequest = { warehouseExpanded = false }
                            ) {
                                warehouses.forEach { wh ->
                                    DropdownMenuItem(
                                        text = { Text("${wh.name} (${wh.code})") },
                                        onClick = {
                                            selectedWarehouseId = wh.id
                                            warehouseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Invoice Items Header & Add Button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "পণ্য ও ভেরিয়েন্ট সমূহ (${itemsList.size})" else "INVOICE ITEMS (${itemsList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Button(
                                onClick = {
                                    if (products.isNotEmpty()) {
                                        val p = products.first()
                                        val pVars = variantsByProduct[p.id] ?: emptyList()
                                        val firstVar = pVars.firstOrNull()
                                        val initPrice = firstVar?.variantPriceMinor ?: p.sellingPriceMinor
                                        itemsList.add(
                                            ItemDraftState(
                                                productId = p.id,
                                                variantId = firstVar?.id,
                                                quantityStr = "1",
                                                unitPriceStr = BigDecimal(initPrice).divide(BigDecimal(100)).toPlainString()
                                            )
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBangla) "পণ্য যোগ করুন" else "Add Item", fontSize = 12.sp)
                            }
                        }
                    }

                    // 4. Product Rows with Variant Selection & Sellable Stock
                    itemsIndexed(itemsList) { index, itemDraft ->
                        var productDropdown by remember { mutableStateOf(false) }
                        var variantDropdown by remember { mutableStateOf(false) }

                        val curProduct = productsMap[itemDraft.productId]
                        val prodVariants: List<ProductVariant> = variantsByProduct[itemDraft.productId] ?: emptyList()
                        val curVariant = prodVariants.find { it.id == itemDraft.variantId }

                        val sellableStock = getSellableStock(itemDraft.productId, itemDraft.variantId)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
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
                                        Text(
                                            text = "#${index + 1} ${curProduct?.nameEn ?: "Select Item"}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (curVariant != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = SecondaryTeal.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                val varLabel = buildString {
                                                    curVariant.color?.let { append(it) }
                                                    if (curVariant.color != null && curVariant.size != null) append("-")
                                                    curVariant.size?.let { append(it) }
                                                    if (isEmpty()) append(curVariant.sku)
                                                }
                                                Text(
                                                    text = varLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SecondaryTeal,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (itemsList.size > 1) {
                                        IconButton(onClick = { itemsList.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Product selector dropdown
                                ExposedDropdownMenuBox(
                                    expanded = productDropdown,
                                    onExpandedChange = { productDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = curProduct?.let { "${it.nameEn} [${it.baseSku}]" } ?: "Select Product",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdown) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f)
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = productDropdown,
                                        onDismissRequest = { productDropdown = false }
                                    ) {
                                        products.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { Text("${prod.nameEn} (${prod.baseSku})") },
                                                onClick = {
                                                    itemDraft.productId = prod.id
                                                    val pVars = variantsByProduct[prod.id] ?: emptyList()
                                                    val firstV = pVars.firstOrNull()
                                                    itemDraft.variantId = firstV?.id
                                                    val priceMinor = firstV?.variantPriceMinor ?: prod.sellingPriceMinor
                                                    itemDraft.unitPriceStr = BigDecimal(priceMinor).divide(BigDecimal(100)).toPlainString()
                                                    productDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Variant selector dropdown if product has variants
                                if (prodVariants.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = variantDropdown,
                                        onExpandedChange = { variantDropdown = it }
                                    ) {
                                        val variantTitle = curVariant?.let { v ->
                                            listOfNotNull(v.color, v.size, v.sku).joinToString(" - ")
                                        } ?: "Select Variant"
                                        OutlinedTextField(
                                            value = variantTitle,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(if (isBangla) "ভেরিয়েন্ট" else "Variant") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = variantDropdown) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SecondaryTeal,
                                                unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f)
                                            )
                                        )
                                        ExposedDropdownMenu(
                                            expanded = variantDropdown,
                                            onDismissRequest = { variantDropdown = false }
                                        ) {
                                            for (v in prodVariants) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = listOfNotNull(v.color, v.size, v.sku).joinToString(" • "),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Price: ৳" + Formatters.formatMinorUnit(v.variantPriceMinor),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = TextSecondary
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        itemDraft.variantId = v.id
                                                        itemDraft.unitPriceStr = BigDecimal(v.variantPriceMinor).divide(BigDecimal(100)).toPlainString()
                                                        variantDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Sellable Stock Indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = if (sellableStock > 0) StatusSuccess else StatusError,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) {
                                            "বিক্রয়যোগ্য স্টক: $sellableStock টি (${selectedWarehouse?.name ?: "Warehouse"})"
                                        } else {
                                            "Sellable Stock: $sellableStock units (${selectedWarehouse?.name ?: "Warehouse"})"
                                        },
                                        fontSize = 11.sp,
                                        color = if (sellableStock > 0) TextSecondary else StatusError,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Qty, Unit Price (Editable!), Line Discount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = itemDraft.quantityStr,
                                        onValueChange = { itemDraft.quantityStr = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = itemDraft.unitPriceStr,
                                        onValueChange = { itemDraft.unitPriceStr = it },
                                        label = { Text("Price (৳)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = itemDraft.discountStr,
                                        onValueChange = { itemDraft.discountStr = it },
                                        label = { Text("Disc (৳)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Discount, Additional Charges & Notes
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = invoiceDiscountStr,
                                    onValueChange = { invoiceDiscountStr = it },
                                    label = { Text(if (isBangla) "চালান বাট্টা (৳)" else "Invoice Discount (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = additionalChargesStr,
                                    onValueChange = { additionalChargesStr = it },
                                    label = { Text(if (isBangla) "অতিরিক্ত চার্জ (ডেলিভারি/ট্যাক্স ৳)" else "Additional Charges (৳)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text(if (isBangla) "নোট / শর্তাবলী" else "Notes / Terms") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // 6. Payment Method & Paid Amount
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBangla) "পেমেন্ট মাধ্যম ও পরিশোধ" else "Payment Method & Settlement",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val paymentOptions = listOf(
                                    "CASH" to if (isBangla) "নগদ (Cash)" else "Cash (নগদ)",
                                    "BANK" to if (isBangla) "ব্যাংক (Bank)" else "Bank (ব্যাংক)",
                                    "MFS" to if (isBangla) "বিকাশ / নগদ (MFS)" else "MFS (bKash/Nagad)",
                                    "CREDIT" to if (isBangla) "বকেয়া (Credit)" else "Credit (বকেয়া)"
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    paymentOptions.chunked(2).forEach { rowOptions ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowOptions.forEach { (code, label) ->
                                                val isSelected = paymentMethod == code
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            paymentMethod = code
                                                            if (code == "CREDIT") {
                                                                isFullPaymentSelected = false
                                                                paidAmountStr = "0.00"
                                                            }
                                                        },
                                                    color = if (isSelected) PrimaryPurple else ERPBackground,
                                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, DeepNavy.copy(alpha = 0.12f))
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) Color.White else TextPrimary,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (isFullPaymentSelected) BigDecimal(grandTotalMinor).divide(BigDecimal(100)).toPlainString() else paidAmountStr,
                                        onValueChange = {
                                            isFullPaymentSelected = false
                                            paidAmountStr = it
                                        },
                                        label = { Text(if (isBangla) "পরিশোধিত অর্থ (৳)" else "Paid Amount (৳)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            isFullPaymentSelected = true
                                            paidAmountStr = BigDecimal(grandTotalMinor).divide(BigDecimal(100)).toPlainString()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                                    ) {
                                        Text(if (isBangla) "সম্পূর্ণ পরিশোধ" else "Full Pay", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 7. Dynamic Financial Summary Box
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
                                    Text("Subtotal / উপমোট:", color = Color.White.copy(alpha = 0.8f))
                                    Text("৳" + Formatters.formatMinorUnit(subtotalMinor), color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                if (invDiscountMinor > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Invoice Discount / বাট্টা:", color = Color(0xFFFF8A80))
                                        Text("-৳" + Formatters.formatMinorUnit(invDiscountMinor), color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (addChargesMinor > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Additional Charges / অন্যান্য:", color = Color.White.copy(alpha = 0.8f))
                                        Text("+৳" + Formatters.formatMinorUnit(addChargesMinor), color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Grand Total / মোট বিল:", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("৳" + Formatters.formatMinorUnit(grandTotalMinor), color = StatusSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paid Amount / পরিশোধ:", color = Color.White.copy(alpha = 0.8f))
                                    Text("৳" + Formatters.formatMinorUnit(currentPaidMinor), color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Due Amount / বকেয়া:", color = if (dueAmountMinor > 0) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.8f))
                                    Text("৳" + Formatters.formatMinorUnit(dueAmountMinor), color = if (dueAmountMinor > 0) Color(0xFFFF8A80) else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage!!,
                                color = StatusError,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DeepNavy.copy(alpha = 0.08f))

                // Bottom Action Buttons: Save Draft vs Confirm Sale
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল" else "Cancel",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // 1. Save Draft (No stock reduction, No accounting post)
                    OutlinedButton(
                        onClick = {
                            val draft = buildInvoiceDraft() ?: return@OutlinedButton
                            onSubmitDraft(draft)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepNavy),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "খসড়া সংরক্ষণ" else "Save Draft",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Confirm Sale (Atomic Stock Deduction + FIFO Depletion + Journal Posting)
                    Button(
                        onClick = {
                            val draft = buildInvoiceDraft() ?: return@Button
                            val cust = customers.find { it.id == selectedCustomerId }
                            val existingDue = customerSummary?.outstandingBalanceMinor ?: cust?.outstandingBalanceMinor ?: 0L
                            val expectedDue = existingDue + maxOf(0L, grandTotalMinor - currentPaidMinor)
                            val limit = cust?.creditLimitMinor ?: 0L

                            if (limit > 0 && expectedDue > limit && !creditLimitApproved) {
                                showCreditLimitWarningDialog = true
                            } else {
                                val finalDraft = if (creditLimitApproved) {
                                    draft.copy(notes = "${draft.notes ?: ""}\n[CREDIT OVERRIDE APPROVED by $creditApproverName: $creditApprovalReason]".trim())
                                } else draft
                                onConfirmSale(finalDraft)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.15f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "নিশ্চিত বিক্রয়" else "Confirm Sale",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showCustomerLookup) {
        CustomerLookupDialog(
            customers = customers,
            isBangla = isBangla,
            onDismiss = { showCustomerLookup = false },
            onSelectCustomer = { cust ->
                selectedCustomerId = cust.id
                pendingNewCustomerDraft = null
            },
            onCreateNewCustomer = { newCustDraft ->
                pendingNewCustomerDraft = newCustDraft
                selectedCustomerId = ""
            }
        )
    }

    if (showCreditLimitWarningDialog) {
        val cust = customers.find { it.id == selectedCustomerId }
        val existingDue = customerSummary?.outstandingBalanceMinor ?: cust?.outstandingBalanceMinor ?: 0L
        val expectedDue = existingDue + maxOf(0L, grandTotalMinor - currentPaidMinor)
        val limit = cust?.creditLimitMinor ?: 0L

        Dialog(onDismissRequest = { showCreditLimitWarningDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isBangla) "ক্রেডিট লিমিট সতর্কবার্তা (Option C)" else "Credit Limit Warning (Option C)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC53030)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBangla)
                            "গ্রাহক ${cust?.name ?: ""}-এর অনুমোদিত ক্রেডিট সীমা ৳${Formatters.formatMinorUnit(limit)} কিন্তু এই বিক্রয়ের পর সম্ভাব্য মোট বকেয়া হবে ৳${Formatters.formatMinorUnit(expectedDue)}। এটি নিশ্চিত করতে অনুমোদিত অনুমোদন প্রয়োজন।"
                        else
                            "Customer credit limit is ৳${Formatters.formatMinorUnit(limit)}, but expected due will reach ৳${Formatters.formatMinorUnit(expectedDue)}. Confirmation requires authorized approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = creditApproverName,
                        onValueChange = { creditApproverName = it },
                        label = { Text(if (isBangla) "অনুমোদনকারীর নাম / পদবী" else "Approver Name / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditApprovalReason,
                        onValueChange = { creditApprovalReason = it },
                        label = { Text(if (isBangla) "অনুমোদনের কারণ / নোট" else "Approval Reason / Justification") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreditLimitWarningDialog = false }) {
                            Text(if (isBangla) "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (creditApproverName.isNotBlank() && creditApprovalReason.isNotBlank()) {
                                    creditLimitApproved = true
                                    showCreditLimitWarningDialog = false
                                    val draft = buildInvoiceDraft() ?: return@Button
                                    val finalDraft = draft.copy(
                                        notes = "${draft.notes ?: ""}\n[CREDIT OVERRIDE APPROVED by $creditApproverName: $creditApprovalReason]".trim()
                                    )
                                    onConfirmSale(finalDraft)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC53030))
                        ) {
                            Text(if (isBangla) "অনুমোদন ও নিশ্চিত করুন" else "Approve & Confirm")
                        }
                    }
                }
            }
        }
    }
}
