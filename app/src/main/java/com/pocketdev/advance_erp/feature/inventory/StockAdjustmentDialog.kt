package com.pocketdev.advance_erp.feature.inventory

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.Warehouse

@Composable
fun StockAdjustmentDialog(
    warehouses: List<Warehouse>,
    products: List<ProductWithDetails>,
    language: AppLanguage,
    onLoadVariants: suspend (productId: String) -> List<ProductVariant>,
    onDismiss: () -> Unit,
    onConfirm: (
        warehouseId: String,
        productId: String,
        variantId: String?,
        quantity: Int,
        isIncrease: Boolean,
        reason: String
    ) -> Unit
) {
    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var availableVariants by remember { mutableStateOf<List<ProductVariant>>(emptyList()) }
    var selectedVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var variantDropdownExpanded by remember { mutableStateOf(false) }

    var isIncrease by remember { mutableStateOf(true) }
    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load variants whenever the selected product changes
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
                        text = stringResource(R.string.stock_adjustment),
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

                // Adjustment Type Selector: Increase (+) vs Decrease (-)
                Text(
                    text = stringResource(R.string.adjustment_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncrease) StatusSuccess else Color.Transparent)
                            .border(1.5.dp, if (isIncrease) StatusSuccess else ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { isIncrease = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.adjustment_increase),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncrease) Color.White else TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isIncrease) StatusError else Color.Transparent)
                            .border(1.5.dp, if (!isIncrease) StatusError else ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { isIncrease = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.adjustment_decrease),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isIncrease) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Warehouse Selector
                Text(
                    text = stringResource(R.string.warehouse),
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
                            .clickable { warehouseDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = selectedWarehouse?.name ?: "Select Warehouse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedWarehouse != null) TextPrimary else TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = warehouseDropdownExpanded,
                        onDismissRequest = { warehouseDropdownExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text(wh.name) },
                                onClick = {
                                    selectedWarehouse = wh
                                    warehouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Selector
                Text(
                    text = stringResource(R.string.nav_products),
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
                            "${selectedProduct?.baseSku} - ${if (language == AppLanguage.BANGLA) selectedProduct?.nameBn else selectedProduct?.nameEn}"
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
                                    Text("${prod.baseSku} - ${if (language == AppLanguage.BANGLA) prod.nameBn else prod.nameEn}")
                                },
                                onClick = {
                                    selectedProduct = prod
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Variant Selector (Shown when product has variants)
                if (availableVariants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "পণ্যের ভেরিয়েন্ট *" else "Product Variant *",
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
                                if (language == AppLanguage.BANGLA) "ভেরিয়েন্ট নির্বাচন করুন" else "Select Variant"
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
                    label = { Text(stringResource(R.string.adjustment_quantity)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason / Reference
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.adjustment_reason)) },
                    placeholder = { Text("e.g. Physical audit recount, Damaged in transit") },
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
                        text = stringResource(R.string.confirm),
                        onClick = {
                            val qty = quantityStr.toIntOrNull()
                            if (qty == null || qty <= 0) {
                                errorMessage = "Please enter a valid positive quantity"
                                return@ERPActionButton
                            }
                            if (selectedWarehouse == null) {
                                errorMessage = "Please select a warehouse"
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
                            if (reason.isBlank()) {
                                errorMessage = "Please enter an adjustment reason"
                                return@ERPActionButton
                            }

                            onConfirm(
                                selectedWarehouse!!.id,
                                selectedProduct!!.id,
                                selectedVariant?.id,
                                qty,
                                isIncrease,
                                reason.trim()
                            )
                        },
                        variant = if (isIncrease) ERPButtonVariant.SECONDARY else ERPButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
