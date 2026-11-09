package com.pocketdev.advance_erp.feature.imports

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.PaymentTermType
import com.pocketdev.advance_erp.domain.model.PaymentTermsCalculator
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import java.math.BigDecimal
import java.math.RoundingMode

private class ItemDraftState(
    productId: String = "",
    quantityStr: String = "100",
    unitPriceStr: String = "45.00",
    cartonQuantityStr: String = "5",
    pcsPerCartonStr: String = "20",
    grossWeightKgStr: String = "35.0",
    netWeightKgStr: String = "33.0"
) {
    var productId by mutableStateOf(productId)
    var quantityStr by mutableStateOf(quantityStr)
    var unitPriceStr by mutableStateOf(unitPriceStr)
    var cartonQuantityStr by mutableStateOf(cartonQuantityStr)
    var pcsPerCartonStr by mutableStateOf(pcsPerCartonStr)
    var grossWeightKgStr by mutableStateOf(grossWeightKgStr)
    var netWeightKgStr by mutableStateOf(netWeightKgStr)
}

@Composable
fun CreatePurchaseOrderDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    categories: List<ProductCategory> = emptyList(),
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onAddNewProduct: ((nameEn: String, nameBn: String, categoryId: String, sku: String, unit: String, purchasePrice: Long, sellingPrice: Long, onCreated: (Product) -> Unit) -> Unit)? = null,
    onAddNewCategory: ((nameEn: String, nameBn: String, onCreated: (ProductCategory) -> Unit) -> Unit)? = null,
    onSavePo: (
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus
    ) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: "") }
    var selectedCurrency by remember { mutableStateOf("CNY") }
    var exchangeRateStr by remember { mutableStateOf("17.00") }
    var selectedPaymentTerm by remember { mutableStateOf(PaymentTermType.THIRTY_SEVENTY) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State for inline Add Product dialog
    var showInlineAddProductDialog by remember { mutableStateOf(false) }
    var newProdNameEn by remember { mutableStateOf("") }
    var newProdNameBn by remember { mutableStateOf("") }
    var newProdSku by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("Pair") }
    var newProdPurchasePriceStr by remember { mutableStateOf("45.00") }
    var newProdSellingPriceStr by remember { mutableStateOf("95.00") }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: "cat-footwear") }
    var showInlineAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatNameEn by remember { mutableStateOf("") }
    var newCatNameBn by remember { mutableStateOf("") }

    val itemsList = remember {
        mutableStateListOf(
            ItemDraftState(
                productId = products.firstOrNull()?.id ?: "",
                quantityStr = "100",
                unitPriceStr = "45.00",
                cartonQuantityStr = "5",
                pcsPerCartonStr = "20",
                grossWeightKgStr = "35.0",
                netWeightKgStr = "33.0"
            )
        )
    }

    fun submitPo(targetStatus: PurchaseOrderStatus) {
        try {
            require(selectedSupplierId.isNotBlank()) {
                if (isBangla) "দয়া করে একজন সরবরাহকারী নির্বাচন করুন।" else "Please select a supplier."
            }
            val rate = BigDecimal(exchangeRateStr.trim())
            require(rate > BigDecimal.ZERO) {
                if (isBangla) "বিনিময় হার শূন্যের বেশি হতে হবে।" else "Exchange rate must be positive."
            }
            require(itemsList.isNotEmpty()) {
                if (isBangla) "অন্তত একটি পণ্য তালিকায় যোগ করুন।" else "Please add at least one item."
            }

            val drafts = itemsList.mapIndexed { idx, state ->
                require(state.productId.isNotBlank()) {
                    if (isBangla) "আইটেম #${idx + 1}: পণ্য নির্বাচন করুন।" else "Item #${idx + 1}: Please select a product."
                }
                val qty = state.quantityStr.trim().toIntOrNull() ?: 0
                require(qty > 0) {
                    if (isBangla) "আইটেম #${idx + 1}: পরিমাণ ১ বা তার বেশি হতে হবে।" else "Item #${idx + 1}: Quantity must be at least 1."
                }
                val priceBd = BigDecimal(state.unitPriceStr.trim().ifEmpty { "0" })
                require(priceBd > BigDecimal.ZERO) {
                    if (isBangla) "আইটেম #${idx + 1}: একক মূল্য শূন্যের বেশি হতে হবে।" else "Item #${idx + 1}: Unit price must be positive."
                }
                val priceMinor = priceBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                val ctnQty = state.cartonQuantityStr.trim().toIntOrNull() ?: 0
                val pcsPerCtn = state.pcsPerCartonStr.trim().toIntOrNull() ?: 0
                val grossKg = state.grossWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val netKg = state.netWeightKgStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

                PurchaseOrderItemDraft(
                    productId = state.productId,
                    quantity = qty,
                    unitPriceMinor = priceMinor,
                    cartonQuantity = ctnQty,
                    pcsPerCarton = pcsPerCtn,
                    grossWeightKg = grossKg,
                    netWeightKg = netKg
                )
            }

            val termsNote = "[Terms: ${selectedPaymentTerm.labelEn}]"
            val combinedNotes = if (notes.isNotBlank()) "$termsNote • ${notes.trim()}" else termsNote
            onSavePo(selectedSupplierId, selectedCurrency, rate, drafts, combinedNotes, targetStatus)
            onDismiss()
        } catch (e: Exception) {
            errorMessage = e.message ?: if (isBangla) "তথ্য যাচাই ব্যর্থ হয়েছে" else "Validation failed"
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
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "নতুন ক্রয়াদেশ (চীন)" else "New Purchase Order (China)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = if (isBangla) "FOB ফ্যাক্টরি অর্ডার • রিয়েল-টাইম রেট ও মুদ্রা ট্র্যাকিং" else "FOB Factory Order • Preserves Currency & Rates",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Supplier Selection
                    item {
                        Column {
                            Text(
                                text = if (isBangla) "সরবরাহকারী নির্বাচন (Select Supplier):" else "Select Supplier:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (suppliers.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBangla) "কোনো সরবরাহকারী পাওয়া যায়নি। অনুগ্রহ করে আগে Contacts এ সরবরাহকারী যোগ করুন।" else "No suppliers found. Please add a China factory/supplier in Contacts first.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    suppliers.forEach { sup ->
                                        val isSelected = sup.id == selectedSupplierId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) PrimaryPurple else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedSupplierId = sup.id
                                                    if (sup.currency.isNotBlank()) {
                                                        selectedCurrency = sup.currency
                                                        exchangeRateStr = when (sup.currency) {
                                                            "CNY" -> "17.00"
                                                            "USD" -> "120.00"
                                                            else -> "1.00"
                                                        }
                                                    }
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(if (isSelected) PrimaryPurple else Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = sup.companyName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) PrimaryPurple else TextPrimary
                                                )
                                                Text(
                                                    text = "${sup.country} • Currency: ${sup.currency}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Currency & Exchange Rate
                    item {
                        Column {
                            Text(
                                text = if (isBangla) "মুদ্রা ও বিনিময় হার (Currency & Rate):" else "Currency & Exchange Rate:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Currency Toggle (CNY / USD)
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .padding(3.dp)
                                ) {
                                    listOf("CNY", "USD").forEach { cur ->
                                        val isSel = selectedCurrency == cur
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) PrimaryPurple else Color.Transparent)
                                                .clickable {
                                                    selectedCurrency = cur
                                                    exchangeRateStr = if (cur == "CNY") "17.00" else "120.00"
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = cur,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else TextPrimary
                                            )
                                        }
                                    }
                                }

                                // Rate Input
                                OutlinedTextField(
                                    value = exchangeRateStr,
                                    onValueChange = { exchangeRateStr = it },
                                    label = { Text("1 $selectedCurrency = ? BDT") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 3. Order Items Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "পণ্য ও পরিমাণ (Items):" else "Products & Quantities:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (onAddNewProduct != null) {
                                    Button(
                                        onClick = { showInlineAddProductDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "New Product", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "+ নতুন পণ্য" else "+ New Product",
                                            color = SecondaryTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (products.isNotEmpty()) {
                                            itemsList.add(ItemDraftState(productId = products.first().id))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "আইটেম যোগ" else "+ Add Item",
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    if (products.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "কোনো পণ্য পাওয়া যায়নি। Inventory বা ওপরের '+ নতুন পণ্য' বোতামে ক্লিক করুন।" else "No products found. Click '+ New Product' above to create one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(itemsList) { index, itemState ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Item #${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                    if (itemsList.size > 1) {
                                        IconButton(
                                            onClick = { itemsList.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Product Selector Chips
                                Text(
                                    text = if (isBangla) "পণ্য নির্বাচন করুন:" else "Select Product:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(products) { prod ->
                                        val isProdSel = itemState.productId == prod.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isProdSel) PrimaryPurple.copy(alpha = 0.15f) else Color.White)
                                                .border(
                                                    width = if (isProdSel) 1.dp else 0.5.dp,
                                                    color = if (isProdSel) PrimaryPurple else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { itemState.productId = prod.id }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla && prod.nameBn.isNotBlank()) prod.nameBn else prod.nameEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isProdSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isProdSel) PrimaryPurple else TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = itemState.quantityStr,
                                        onValueChange = { itemState.quantityStr = it },
                                        label = { Text(if (isBangla) "পরিমাণ (Qty)" else "Quantity") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = itemState.unitPriceStr,
                                        onValueChange = { itemState.unitPriceStr = it },
                                        label = { Text("Price ($selectedCurrency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Packing fields: Cartons & Gross Weight
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = itemState.cartonQuantityStr,
                                        onValueChange = { itemState.cartonQuantityStr = it },
                                        label = { Text(if (isBangla) "কার্টন সংখ্যা" else "Cartons") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = itemState.grossWeightKgStr,
                                        onValueChange = { itemState.grossWeightKgStr = it },
                                        label = { Text(if (isBangla) "মোট ওজন (KG)" else "Gross Wt (KG)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // 4. Order Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isBangla) "অর্ডার নোট / বিবরণ (মন্তব্য)" else "Order Notes / Remarks") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    // 5. Payment Terms & Schedule Calculation
                    item {
                        Column {
                            Text(
                                text = if (isBangla) "পেমেন্ট শর্ত ও সময়সূচি (Payment Terms):" else "Payment Terms & Schedule:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(PaymentTermType.values()) { term ->
                                    val isSel = selectedPaymentTerm == term
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple else Color(0xFFF1F5F9))
                                            .clickable { selectedPaymentTerm = term }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) term.labelBn else term.labelEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }

                            // Calculate preview schedule
                            val totalMinor = itemsList.sumOf { item ->
                                val q = item.quantityStr.toIntOrNull() ?: 0
                                val p = BigDecimal(item.unitPriceStr.ifEmpty { "0" })
                                    .multiply(BigDecimal("100"))
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .longValueExact()
                                q * p
                            }
                            val rateBd = BigDecimal(exchangeRateStr.trim().ifEmpty { "1" })
                            val schedule = remember(selectedPaymentTerm, totalMinor, rateBd) {
                                PaymentTermsCalculator.calculateSchedule(selectedPaymentTerm, totalMinor, rateBd)
                            }

                            if (schedule.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isBangla) "নির্ধারিত কিস্তি সময়সূচি:" else "Calculated Payment Schedule:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryTeal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        schedule.forEach { inst ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (isBangla) inst.titleBn else inst.titleEn,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "$selectedCurrency ${BigDecimal(inst.amountMinor).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)} (৳ ${BigDecimal(inst.bdtAmountMinor).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error Message Banner
                    if (errorMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusError)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = StatusError,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল" else "Cancel",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Save Draft Button
                        OutlinedButton(
                            onClick = { submitPo(PurchaseOrderStatus.DRAFT) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "ড্রাফট রাখুন" else "Save Draft",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        // Confirm PO Button
                        Button(
                            onClick = { submitPo(PurchaseOrderStatus.CONFIRMED) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "PO নিশ্চিত করুন" else "Confirm PO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Inline Add Product Dialog
    if (showInlineAddProductDialog && onAddNewProduct != null) {
        AlertDialog(
            onDismissRequest = { showInlineAddProductDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ক্যাটালগে নতুন পণ্য যোগ করুন" else "Add New Product to Catalog",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProdNameEn,
                        onValueChange = { newProdNameEn = it },
                        label = { Text("Product Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newProdSku,
                        onValueChange = { newProdSku = it },
                        label = { Text("Base SKU") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newProdUnit,
                            onValueChange = { newProdUnit = it },
                            label = { Text("Unit (PCS/Pair)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newProdPurchasePriceStr,
                            onValueChange = { newProdPurchasePriceStr = it },
                            label = { Text("Cost ($selectedCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "ক্যাটাগরি নির্বাচন করুন:" else "Select Category:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        if (onAddNewCategory != null) {
                            TextButton(
                                onClick = { showInlineAddCategoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(if (isBangla) "+ নতুন ক্যাটাগরি" else "+ New Category", fontSize = 11.sp)
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                val isSel = cat.id == selectedCategoryId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else Color.White)
                                        .border(
                                            width = if (isSel) 1.dp else 0.5.dp,
                                            color = if (isSel) PrimaryPurple else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedCategoryId = cat.id }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isBangla && cat.nameBn.isNotBlank()) cat.nameBn else cat.nameEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) PrimaryPurple else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProdNameEn.isNotBlank() && newProdSku.isNotBlank()) {
                            val priceMinor = BigDecimal(newProdPurchasePriceStr.ifEmpty { "0" })
                                .multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                            val sellMinor = BigDecimal(newProdSellingPriceStr.ifEmpty { "0" })
                                .multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                            onAddNewProduct(
                                newProdNameEn.trim(),
                                newProdNameBn.ifBlank { newProdNameEn.trim() },
                                selectedCategoryId.ifBlank { "cat-footwear" },
                                newProdSku.trim(),
                                newProdUnit.trim(),
                                priceMinor,
                                sellMinor
                            ) { created ->
                                showInlineAddProductDialog = false
                                itemsList.add(
                                    ItemDraftState(
                                        productId = created.id,
                                        quantityStr = "100",
                                        unitPriceStr = newProdPurchasePriceStr
                                    )
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Create Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineAddProductDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Inline Add Category Dialog
    if (showInlineAddCategoryDialog && onAddNewCategory != null) {
        AlertDialog(
            onDismissRequest = { showInlineAddCategoryDialog = false },
            title = {
                Text(
                    text = if (isBangla) "নতুন ক্যাটাগরি তৈরি করুন" else "Create New Category",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCatNameEn,
                        onValueChange = { newCatNameEn = it },
                        label = { Text("Category Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatNameBn,
                        onValueChange = { newCatNameBn = it },
                        label = { Text("Category Name (Bangla - Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatNameEn.isNotBlank()) {
                            onAddNewCategory(newCatNameEn.trim(), newCatNameBn.trim()) { created ->
                                selectedCategoryId = created.id
                                showInlineAddCategoryDialog = false
                                newCatNameEn = ""
                                newCatNameBn = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(if (isBangla) "তৈরি করুন" else "Save Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineAddCategoryDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
