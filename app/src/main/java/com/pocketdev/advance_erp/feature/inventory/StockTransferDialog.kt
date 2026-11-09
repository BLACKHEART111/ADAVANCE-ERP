package com.pocketdev.advance_erp.feature.inventory

import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.StockTransferDraft

@Composable
fun StockTransferDialog(
    warehouses: List<Warehouse>,
    products: List<ProductWithDetails>,
    language: AppLanguage,
    onLoadVariants: suspend (productId: String) -> List<ProductVariant>,
    onDismiss: () -> Unit,
    onConfirm: (StockTransferDraft) -> Unit
) {
    var sourceWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    var destWarehouse by remember {
        mutableStateOf(warehouses.getOrNull(1) ?: warehouses.firstOrNull())
    }
    var destDropdownExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var availableVariants by remember { mutableStateOf<List<ProductVariant>>(emptyList()) }
    var selectedVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var variantDropdownExpanded by remember { mutableStateOf(false) }

    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isBangla = language == AppLanguage.BANGLA

    LaunchedEffect(selectedProduct?.id) {
        val prodId = selectedProduct?.id
        if (prodId != null && (selectedProduct?.variantCount ?: 0) > 0) {
            val variants = onLoadVariants(prodId)
            availableVariants = variants
            selectedVariant = variants.firstOrNull()
        } else {
            availableVariants = emptyList()
            selectedVariant = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "নতুন ওয়্যারহাউস স্থানান্তর" else "New Stock Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 12.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Source Warehouse
                Text(
                    text = if (isBangla) "উৎস ওয়্যারহাউস *" else "Source Warehouse *",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { sourceDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = sourceWarehouse?.name ?: "Select Source Warehouse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sourceWarehouse != null) TextPrimary else TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text(wh.name) },
                                onClick = {
                                    sourceWarehouse = wh
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Warehouse
                Text(
                    text = if (isBangla) "গন্তব্য ওয়্যারহাউস *" else "Destination Warehouse *",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { destDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = destWarehouse?.name ?: "Select Destination Warehouse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (destWarehouse != null) TextPrimary else TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = destDropdownExpanded,
                        onDismissRequest = { destDropdownExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text(wh.name) },
                                onClick = {
                                    destWarehouse = wh
                                    destDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Selector
                Text(
                    text = if (isBangla) "স্থানান্তরযোগ্য পণ্য *" else "Product to Transfer *",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { productDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val prodTitle = if (selectedProduct != null) {
                            "${selectedProduct?.baseSku} - ${if (isBangla) selectedProduct?.nameBn else selectedProduct?.nameEn}"
                        } else {
                            "Select Product"
                        }
                        Text(
                            text = prodTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedProduct != null) TextPrimary else TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = productDropdownExpanded,
                        onDismissRequest = { productDropdownExpanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = {
                                    Text("${prod.baseSku} - ${if (isBangla) prod.nameBn else prod.nameEn}")
                                },
                                onClick = {
                                    selectedProduct = prod
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Variant Selector
                if (availableVariants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isBangla) "পণ্যের ভেরিয়েন্ট *" else "Product Variant *",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                                .clickable { variantDropdownExpanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val variantText = if (selectedVariant != null) {
                                listOfNotNull(
                                    selectedVariant?.sku,
                                    selectedVariant?.color,
                                    selectedVariant?.size
                                ).filter { it.isNotBlank() }.joinToString(" - ")
                            } else {
                                if (isBangla) "ভেরিয়েন্ট নির্বাচন করুন" else "Select Variant"
                            }
                            Text(
                                text = variantText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedVariant != null) TextPrimary else TextMuted
                            )
                        }

                        DropdownMenu(
                            expanded = variantDropdownExpanded,
                            onDismissRequest = { variantDropdownExpanded = false }
                        ) {
                            availableVariants.forEach { variant ->
                                val vTitle = listOfNotNull(
                                    variant.sku,
                                    variant.color,
                                    variant.size
                                ).filter { it.isNotBlank() }.joinToString(" - ")
                                DropdownMenuItem(
                                    text = { Text(vTitle) },
                                    onClick = {
                                        selectedVariant = variant
                                        variantDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (isBangla) "স্থানান্তরের পরিমাণ *" else "Transfer Quantity *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "নোট / রেফারেন্স" else "Notes / Reference") },
                    placeholder = { Text("e.g. Stock replenishment for Mirpur branch") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ERPActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        variant = ERPButtonVariant.GHOST
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ERPActionButton(
                        text = if (isBangla) "খসড়া তৈরি করুন" else "Create Draft",
                        onClick = {
                            val qty = quantityStr.toIntOrNull()
                            if (qty == null || qty <= 0) {
                                errorMessage = "Please enter a valid positive quantity"
                                return@ERPActionButton
                            }
                            if (sourceWarehouse == null) {
                                errorMessage = "Please select a source warehouse"
                                return@ERPActionButton
                            }
                            if (destWarehouse == null) {
                                errorMessage = "Please select a destination warehouse"
                                return@ERPActionButton
                            }
                            if (sourceWarehouse!!.id == destWarehouse!!.id) {
                                errorMessage = "Source and destination warehouses cannot be the same"
                                return@ERPActionButton
                            }
                            if (selectedProduct == null) {
                                errorMessage = "Please select a product"
                                return@ERPActionButton
                            }
                            if (availableVariants.isNotEmpty() && selectedVariant == null) {
                                errorMessage = "Please select a variant for this product"
                                return@ERPActionButton
                            }

                            onConfirm(
                                StockTransferDraft(
                                    sourceWarehouseId = sourceWarehouse!!.id,
                                    destinationWarehouseId = destWarehouse!!.id,
                                    productId = selectedProduct!!.id,
                                    variantId = selectedVariant?.id,
                                    quantity = qty,
                                    transferDate = System.currentTimeMillis(),
                                    notes = notes.trim().ifEmpty { null }
                                )
                            )
                        },
                        variant = ERPButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
