package com.pocketdev.advance_erp.feature.product

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import kotlinx.coroutines.launch

@Composable
fun EditProductDialog(
    product: ProductWithDetails,
    initialVariants: List<ProductVariant>,
    initialImages: List<ProductImage>,
    categories: List<ProductCategory>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onGenerateSku: suspend (categoryId: String, nameEn: String) -> String,
    onGenerateBarcode: suspend () -> String,
    onValidateSku: suspend (sku: String) -> Boolean,
    onValidateBarcode: suspend (barcode: String) -> Boolean,
    onDeleteProduct: ((productId: String) -> Unit)? = null,
    onConfirm: (
        nameEn: String,
        nameBn: String,
        baseSku: String,
        barcode: String?,
        categoryId: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>,
        variants: List<NewVariantDraft>,
        deletedImageIds: List<String>
    ) -> Unit
) {
    val scope = rememberCoroutineScope()

    var nameEn by remember { mutableStateOf(product.nameEn) }
    var nameBn by remember { mutableStateOf(product.nameBn) }
    var baseSku by remember { mutableStateOf(product.baseSku) }
    var barcode by remember { mutableStateOf(product.barcode ?: "") }
    var unit by remember { mutableStateOf(product.unit) }
    var purchasePriceStr by remember { mutableStateOf((product.purchasePriceMinor / 100.0).toString()) }
    var sellingPriceStr by remember { mutableStateOf((product.sellingPriceMinor / 100.0).toString()) }
    var minStockAlertStr by remember { mutableStateOf(product.minStockAlert.toString()) }

    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull { it.id == product.categoryId } ?: categories.firstOrNull())
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val productPhotos = remember {
        mutableStateListOf<GalleryItemDraft>().apply {
            addAll(initialImages.map {
                GalleryItemDraft(it.id, it.imageUri, it.isPrimary, it.sortOrder)
            })
        }
    }
    val deletedImageIds = remember { mutableStateListOf<String>() }

    val variants = remember {
        mutableStateListOf<NewVariantDraft>().apply {
            addAll(initialVariants.map {
                NewVariantDraft(
                    id = it.id,
                    sku = it.sku,
                    color = it.color ?: "",
                    size = it.size ?: "",
                    costMinor = it.variantCostMinor,
                    priceMinor = it.variantPriceMinor,
                    barcode = it.barcode,
                    qrCode = it.qrCode
                )
            })
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingDuplicates by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
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
                        text = if (language == AppLanguage.BANGLA) "পণ্য সম্পাদনা করুন" else "Edit Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
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

                // 1. Photo Gallery Section
                PhotoGalleryPicker(
                    title = if (language == AppLanguage.BANGLA) "পণ্যের ছবি" else "Product Photos",
                    photos = productPhotos,
                    onPhotosChanged = { updated ->
                        // Detect deleted initial images
                        val updatedIds = updated.map { it.id }.toSet()
                        for (initImg in initialImages) {
                            if (!updatedIds.contains(initImg.id) && !deletedImageIds.contains(initImg.id)) {
                                deletedImageIds.add(initImg.id)
                            }
                        }
                        productPhotos.clear()
                        productPhotos.addAll(updated)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Category Dropdown
                Text(
                    text = stringResource(R.string.category),
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
                            .clickable { categoryDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) selectedCategory?.nameBn ?: "" else selectedCategory?.nameEn ?: "Select Category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Names
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(stringResource(R.string.product_name_en)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text(stringResource(R.string.product_name_bn)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. SKU with Auto-generate & Barcode with Auto-generate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = baseSku,
                        onValueChange = { baseSku = it.uppercase() },
                        label = { Text(stringResource(R.string.sku)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val cat = selectedCategory
                            if (cat != null) {
                                scope.launch {
                                    val name = if (nameEn.isBlank()) "PRD" else nameEn
                                    val auto = onGenerateSku(cat.id, name)
                                    baseSku = auto
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(SecondaryTeal.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto SKU", tint = SecondaryTeal)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(stringResource(R.string.barcode)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                val auto = onGenerateBarcode()
                                barcode = auto
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Barcode", tint = PrimaryPurple)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text(stringResource(R.string.purchase_price)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text(stringResource(R.string.selling_price)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Min Stock Alert & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minStockAlertStr,
                        onValueChange = { minStockAlertStr = it },
                        label = { Text(stringResource(R.string.min_stock_alert)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(R.string.unit)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDeleteProduct != null) {
                        ERPActionButton(
                            text = if (language == AppLanguage.BANGLA) "পণ্য মুছুন" else "Delete",
                            icon = Icons.Default.DeleteOutline,
                            onClick = { showDeleteConfirmation = true },
                            variant = ERPButtonVariant.GHOST
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ERPActionButton(
                            text = stringResource(R.string.cancel),
                            onClick = onDismiss,
                            variant = ERPButtonVariant.GHOST
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ERPActionButton(
                            text = if (language == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save Changes",
                            onClick = {
                            if (nameEn.isBlank()) {
                                errorMessage = "Product English name cannot be empty"
                            } else if (baseSku.isBlank()) {
                                errorMessage = "Base SKU cannot be empty"
                            } else {
                                val purchaseMinor = (purchasePriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                val sellingMinor = (sellingPriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                val minStock = minStockAlertStr.toIntOrNull() ?: 10

                                scope.launch {
                                    isCheckingDuplicates = true
                                    val skuOk = onValidateSku(baseSku.trim())
                                    if (!skuOk) {
                                        errorMessage = "Base SKU '$baseSku' is already taken by another product"
                                        isCheckingDuplicates = false
                                    } else {
                                        var barOk = true
                                        if (barcode.isNotBlank()) {
                                            barOk = onValidateBarcode(barcode.trim())
                                            if (!barOk) {
                                                errorMessage = "Barcode '$barcode' is already in use"
                                            }
                                        }
                                        if (barOk) {
                                            val cat = selectedCategory
                                            if (cat != null) {
                                                val nameBnFinal = if (nameBn.isBlank()) nameEn.trim() else nameBn.trim()
                                                val barcodeFinal = if (barcode.isBlank()) null else barcode.trim()
                                                val unitFinal = if (unit.isBlank()) "PCS" else unit.trim()
                                                onConfirm(
                                                    nameEn.trim(),
                                                    nameBnFinal,
                                                    baseSku.trim().uppercase(),
                                                    barcodeFinal,
                                                    cat.id,
                                                    unitFinal,
                                                    purchaseMinor,
                                                    sellingMinor,
                                                    minStock,
                                                    productPhotos.toList(),
                                                    variants.toList(),
                                                    deletedImageIds.toList()
                                                )
                                            }
                                        }
                                        isCheckingDuplicates = false
                                    }
                                }
                            }
                        },
                            variant = ERPButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && onDeleteProduct != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = if (language == AppLanguage.BANGLA) "পণ্য মুছে ফেলতে চান?" else "Delete Product?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${product.nameEn} (SKU: ${product.baseSku})",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (product.totalStock > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, StatusError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "⚠️ সতর্কতা: এই পণ্যের বর্তমানে ${product.totalStock} ${product.unit} মজুদ রয়েছে। পণ্যটি মুছে ফেললে ক্যাটালগ থেকে বাদ দেওয়া হবে।"
                                else
                                    "⚠️ Warning: This product has ${product.totalStock} ${product.unit} in stock. Deleting will remove it from active catalog.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError
                            )
                        }
                    } else {
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "পণ্যটি সক্রিয় ক্যাটালগ ও বিক্রয় তালিকা থেকে মুছে ফেলা হবে। আপনি কি নিশ্চিত?"
                            else
                                "This product will be removed from your active catalog. Are you sure?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteProduct?.invoke(product.id)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "হ্যাঁ, মুছে ফেলুন" else "Yes, Delete",
                        color = StatusError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
