package com.pocketdev.advance_erp.feature.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Warehouse
import kotlinx.coroutines.launch

@Composable
fun AddProductDialog(
    categories: List<ProductCategory>,
    warehouses: List<Warehouse>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAddNewCategory: ((nameEn: String, nameBn: String, onCreated: (ProductCategory) -> Unit) -> Unit)? = null,
    onGenerateSku: (suspend (categoryId: String, nameEn: String) -> String)? = null,
    onGenerateVariantSku: (suspend (baseSku: String, color: String?, size: String?) -> String)? = null,
    onGenerateBarcode: (suspend () -> String)? = null,
    onValidateSku: (suspend (sku: String, excludeId: String?) -> Boolean)? = null,
    onValidateBarcode: (suspend (barcode: String, excludeId: String?) -> Boolean)? = null,
    onLoadCategoryProducts: (suspend (categoryId: String) -> List<ProductWithDetails>)? = null,
    onLoadProductRestockBundle: (suspend (productId: String) -> ExistingProductRestockBundle?)? = null,
    onUpdateExistingProduct: ((
        productId: String,
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
        deletedImageIds: List<String>,
        additionalStock: Int,
        warehouseId: String
    ) -> Unit)? = null,
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
        initialStock: Int,
        warehouseId: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameEn by remember { mutableStateOf("") }
    var nameBn by remember { mutableStateOf("") }
    var baseSku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("PCS") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var minStockAlertStr by remember { mutableStateOf("10") }
    var initialStockStr by remember { mutableStateOf("0") }

    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedWarehouse by remember { mutableStateOf(warehouses.firstOrNull()) }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    // Existing products in selected category
    var existingCategoryProducts by remember { mutableStateOf<List<ProductWithDetails>>(emptyList()) }
    var selectedExistingProduct by remember { mutableStateOf<ProductWithDetails?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var existingBaseStock by remember { mutableStateOf(0) }
    val deletedImageIds = remember { mutableStateListOf<String>() }

    // Product Photos
    val productPhotos = remember { mutableStateListOf<GalleryItemDraft>() }

    // Variants
    val variants = remember { mutableStateListOf<NewVariantDraft>() }
    var showVariantInputs by remember { mutableStateOf(false) }
    var variantColor by remember { mutableStateOf("") }
    var variantSize by remember { mutableStateOf("") }
    var variantSku by remember { mutableStateOf("") }
    var variantBarcode by remember { mutableStateOf("") }
    var variantQtyStr by remember { mutableStateOf("") }
    var variantCostStr by remember { mutableStateOf("") }
    var variantPriceStr by remember { mutableStateOf("") }
    val variantPhotos = remember { mutableStateListOf<GalleryItemDraft>() }

    // Variant inline editing index
    var editingVariantIdx by remember { mutableStateOf<Int?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Load products of selected category
    LaunchedEffect(selectedCategory?.id) {
        val catId = selectedCategory?.id
        if (catId != null && onLoadCategoryProducts != null) {
            existingCategoryProducts = onLoadCategoryProducts(catId)
        } else {
            existingCategoryProducts = emptyList()
        }
    }

    val clearToNewProduct: () -> Unit = {
        selectedExistingProduct = null
        nameEn = ""
        nameBn = ""
        baseSku = ""
        barcode = ""
        unit = "PCS"
        purchasePriceStr = ""
        sellingPriceStr = ""
        minStockAlertStr = "10"
        initialStockStr = "0"
        existingBaseStock = 0
        productPhotos.clear()
        deletedImageIds.clear()
        variants.clear()
        showVariantInputs = false
        variantColor = ""
        variantSize = ""
        variantSku = ""
        variantBarcode = ""
        variantQtyStr = ""
        variantCostStr = ""
        variantPriceStr = ""
        variantPhotos.clear()
        editingVariantIdx = null
        errorMessage = null
    }

    val selectProduct: (ProductWithDetails) -> Unit = { p ->
        selectedExistingProduct = p
        editingVariantIdx = null
        errorMessage = null
        scope.launch {
            val bundle = onLoadProductRestockBundle?.invoke(p.id)
            if (bundle != null) {
                nameEn = bundle.product.nameEn
                nameBn = bundle.product.nameBn
                baseSku = bundle.product.baseSku
                barcode = bundle.product.barcode ?: ""
                unit = bundle.product.unit
                purchasePriceStr = (bundle.product.purchasePriceMinor / 100.0).toString()
                sellingPriceStr = (bundle.product.sellingPriceMinor / 100.0).toString()
                minStockAlertStr = bundle.product.minStockAlert.toString()
                existingBaseStock = bundle.baseStock
                initialStockStr = "0"

                productPhotos.clear()
                productPhotos.addAll(bundle.productPhotos)
                deletedImageIds.clear()

                variants.clear()
                variants.addAll(bundle.variants)
            }
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
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedExistingProduct != null) {
                                if (language == AppLanguage.BANGLA) "পণ্য স্টক বৃদ্ধি ও সম্পাদনা" else "Restock & Edit Product"
                            } else {
                                stringResource(R.string.add_product)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedExistingProduct != null) {
                            Text(
                                text = "${selectedExistingProduct?.nameEn} (SKU: ${selectedExistingProduct?.baseSku})",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = ERPDivider)

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Existing Product Status Banner
                if (selectedExistingProduct != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryPurple.copy(alpha = 0.08f))
                            .border(1.dp, PrimaryPurple.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA)
                                        "বিদ্যমান পণ্যের স্টক বৃদ্ধি মোড সক্রিয়"
                                    else
                                        "Existing Product Restock Mode Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                                Text(
                                    text = if (language == AppLanguage.BANGLA)
                                        "পূর্বের মোট মজুদ: $existingBaseStock $unit • নিচে ভ্যারিয়েন্টে অতিরিক্ত পরিমাণ যোগ বা এডিট করুন"
                                    else
                                        "Current Total Stock: $existingBaseStock $unit • Add extra quantity or edit variants below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            TextButton(onClick = clearToNewProduct) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "+ নতুন তৈরি" else "+ New",
                                    color = SecondaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1. Product Photos Gallery Picker
                PhotoGalleryPicker(
                    title = if (language == AppLanguage.BANGLA) "পণ্যের ছবি" else "Product Photos",
                    photos = productPhotos,
                    onPhotosChanged = { updated ->
                        val updatedIds = updated.map { it.id }.toSet()
                        for (img in productPhotos) {
                            if (!updatedIds.contains(img.id) && !img.id.startsWith("draft-") && !deletedImageIds.contains(img.id)) {
                                deletedImageIds.add(img.id)
                            }
                        }
                        productPhotos.clear()
                        productPhotos.addAll(updated)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Category Selection
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
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { categoryDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) selectedCategory?.nameBn ?: "" else selectedCategory?.nameEn ?: "Select Category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedCategory != null) TextPrimary else TextMuted
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
                                    clearToNewProduct()
                                }
                            )
                        }
                        if (onAddNewCategory != null) {
                            Divider(color = ERPDivider)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "+ " + if (language == AppLanguage.BANGLA) "নতুন ক্যাটাগরি তৈরি করুন" else "Create New Category",
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    categoryDropdownExpanded = false
                                    showAddCategoryDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2.1 Existing Products in Selected Category (Auto-suggest / Restock selection)
                if (selectedCategory != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "এই ক্যাটাগরির পণ্যসমূহ (${existingCategoryProducts.size})"
                                else
                                    "Products in this Category (${existingCategoryProducts.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedExistingProduct != null) PrimaryPurple else TextPrimary
                            )
                            if (selectedExistingProduct != null) {
                                TextButton(
                                    onClick = clearToNewProduct,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "+ নতুন পণ্য মোড" else "+ New Product Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (existingCategoryProducts.isNotEmpty()) {
                            // Dropdown Box for full list
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(
                                            1.dp,
                                            if (selectedExistingProduct != null) PrimaryPurple else ERPDivider,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { productDropdownExpanded = true }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (selectedExistingProduct != null) {
                                                "📦 ${selectedExistingProduct?.nameEn} (${selectedExistingProduct?.baseSku}) • মজুদ: ${selectedExistingProduct?.totalStock} $unit"
                                            } else {
                                                if (language == AppLanguage.BANGLA)
                                                    "বিদ্যমান পণ্য সিলেক্ট করুন (স্টক যোগ / এডিট) অথবা নতুন তৈরি করুন"
                                                else
                                                    "Select existing product to restock/edit or create new"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (selectedExistingProduct != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedExistingProduct != null) PrimaryPurple else TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = if (selectedExistingProduct != null) PrimaryPurple else TextSecondary
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = productDropdownExpanded,
                                    onDismissRequest = { productDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (language == AppLanguage.BANGLA)
                                                    "✨ নতুন পণ্য তৈরি করুন (Create Brand New Product)"
                                                else
                                                    "✨ Create Brand New Product",
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal
                                            )
                                        },
                                        onClick = {
                                            productDropdownExpanded = false
                                            clearToNewProduct()
                                        }
                                    )
                                    Divider(color = ERPDivider)
                                    existingCategoryProducts.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "${p.nameEn} (${p.baseSku})",
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "${if (language == AppLanguage.BANGLA) "বর্তমান স্টক:" else "Current Stock:"} ${p.totalStock} ${p.unit} • ${p.variantCount} ${if (language == AppLanguage.BANGLA) "ভেরিয়েন্ট" else "variants"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                productDropdownExpanded = false
                                                selectProduct(p)
                                            }
                                        )
                                    }
                                }
                            }

                            // Horizontal quick tap chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedExistingProduct == null,
                                    onClick = clearToNewProduct,
                                    label = {
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "+ নতুন পণ্য" else "+ New Product",
                                            fontWeight = if (selectedExistingProduct == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                existingCategoryProducts.forEach { p ->
                                    val isSelected = selectedExistingProduct?.id == p.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                clearToNewProduct()
                                            } else {
                                                selectProduct(p)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "${p.nameEn} (${p.totalStock})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "এই ক্যাটাগরিতে এখনও কোনো পণ্য নেই। নিচে নতুন পণ্যের তথ্য দিন।"
                                else
                                    "No existing products in this category yet. Enter new product details below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. Product Names (En & Bn)
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

                // 4. Base SKU with Auto-generate & Barcode with Auto-generate
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

                    if (onGenerateSku != null) {
                        IconButton(
                            onClick = {
                                val cat = selectedCategory
                                if (cat != null) {
                                    scope.launch {
                                        val name = if (nameEn.isBlank()) "PRD" else nameEn
                                        val generated = onGenerateSku(cat.id, name)
                                        baseSku = generated
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

                    if (onGenerateBarcode != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val generated = onGenerateBarcode()
                                    barcode = generated
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Barcode", tint = PrimaryPurple)
                        }
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

                // 6. Min Stock Alert & Opening / Additional Stock
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

                    val variantStockTotal = variants.sumOf { it.initialStock }
                    val hasVariantStock = variantStockTotal > 0
                    val isRestock = selectedExistingProduct != null

                    OutlinedTextField(
                        value = if (hasVariantStock) variantStockTotal.toString() else initialStockStr,
                        onValueChange = { initialStockStr = it.filter { ch -> ch.isDigit() } },
                        label = {
                            Text(
                                if (isRestock) {
                                    if (hasVariantStock) {
                                        if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্টে নতুন যোগ" else "Variant Added Stock"
                                    } else {
                                        if (language == AppLanguage.BANGLA) "নতুন স্টক যোগ ($unit)" else "Add Stock Qty ($unit)"
                                    }
                                } else {
                                    if (language == AppLanguage.BANGLA) "প্রারম্ভিক মজুদ ($unit)" else "Initial Stock ($unit)"
                                }
                            )
                        },
                        supportingText = {
                            if (isRestock) {
                                val addedNow = if (hasVariantStock) variantStockTotal else (initialStockStr.toIntOrNull() ?: 0)
                                Text(
                                    text = if (language == AppLanguage.BANGLA)
                                        "বর্তমান: $existingBaseStock | যোগ: +$addedNow | মোট হবে: ${existingBaseStock + addedNow} $unit"
                                    else
                                        "Current: $existingBaseStock | Added: +$addedNow | Total: ${existingBaseStock + addedNow} $unit"
                                )
                            } else if (hasVariantStock) {
                                Text(if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্টের মোট" else "Sum of variants")
                            } else null
                        },
                        enabled = !hasVariantStock,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Warehouse for Stock
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
                            .height(52.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // 8. Variants Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "${stringResource(R.string.variants)} (${variants.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedExistingProduct != null && variants.isNotEmpty()) {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "নিচের প্রতিটি ভ্যারিয়েন্টে নতুন পরিমাণ যোগ করতে পারেন"
                                else
                                    "Enter additional quantity to add for each variant below",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    ERPActionButton(
                        text = if (showVariantInputs) {
                            if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel"
                        } else {
                            if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট যোগ" else "Add Variant"
                        },
                        onClick = { showVariantInputs = !showVariantInputs },
                        variant = if (showVariantInputs) ERPButtonVariant.GHOST else ERPButtonVariant.OUTLINED,
                        icon = if (showVariantInputs) Icons.Default.Close else Icons.Default.Add,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                if (showVariantInputs) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "নতুন ভ্যারিয়েন্ট তথ্য যোগ করুন" else "Add New Variant Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = variantColor,
                                onValueChange = { variantColor = it },
                                label = { Text(stringResource(R.string.color)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = variantSize,
                                onValueChange = { variantSize = it },
                                label = { Text(stringResource(R.string.size)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Variant SKU
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = variantSku,
                                onValueChange = { variantSku = it.uppercase() },
                                label = { Text(stringResource(R.string.variant_sku)) },
                                placeholder = { Text(if (baseSku.isNotBlank()) "$baseSku-V1" else "SKU-V1") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            if (onGenerateVariantSku != null) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val auto = onGenerateVariantSku(
                                                baseSku.ifBlank { "PRD" },
                                                variantColor.ifBlank { null },
                                                variantSize.ifBlank { null }
                                            )
                                            variantSku = auto
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(SecondaryTeal.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Variant SKU", tint = SecondaryTeal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Variant Barcode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = variantBarcode,
                                onValueChange = { variantBarcode = it },
                                label = { Text("Variant Barcode (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            if (onGenerateBarcode != null) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val auto = onGenerateBarcode()
                                            variantBarcode = auto
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Variant Barcode", tint = PrimaryPurple)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Variant Cost & Selling Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = variantCostStr,
                                onValueChange = { variantCostStr = it },
                                label = {
                                    Text(if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট ক্রয় খরচ" else "Variant Cost Price")
                                },
                                placeholder = { Text(purchasePriceStr.ifBlank { "0" }) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = variantPriceStr,
                                onValueChange = { variantPriceStr = it },
                                label = {
                                    Text(if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট বিক্রয় মূল্য" else "Variant Selling Price")
                                },
                                placeholder = { Text(sellingPriceStr.ifBlank { "0" }) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Variant Opening Quantity
                        OutlinedTextField(
                            value = variantQtyStr,
                            onValueChange = { variantQtyStr = it.filter { ch -> ch.isDigit() } },
                            label = {
                                Text(
                                    if (language == AppLanguage.BANGLA) "এই ভ্যারিয়েন্টের পরিমাণ ($unit)"
                                    else "Opening Quantity ($unit)"
                                )
                            },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Variant Photos Picker
                        PhotoGalleryPicker(
                            title = "Variant Photos",
                            photos = variantPhotos,
                            onPhotosChanged = { updated ->
                                variantPhotos.clear()
                                variantPhotos.addAll(updated)
                            },
                            maxPhotos = 4
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ERPActionButton(
                            text = if (language == AppLanguage.BANGLA) "তালিকায় ভ্যারিয়েন্ট যোগ করুন" else "Add Variant to List",
                            onClick = {
                                val skuToUse = if (variantSku.isNotBlank()) {
                                    variantSku.trim().uppercase()
                                } else {
                                    "${baseSku.ifBlank { "PRD" }}-${variants.size + 1}"
                                }
                                val cost = Money.parseFromString(variantCostStr.ifBlank { purchasePriceStr }).minorUnits
                                val price = Money.parseFromString(variantPriceStr.ifBlank { sellingPriceStr }).minorUnits

                                variants.add(
                                    NewVariantDraft(
                                        sku = skuToUse,
                                        color = variantColor.trim(),
                                        size = variantSize.trim(),
                                        costMinor = cost,
                                        priceMinor = price,
                                        barcode = variantBarcode.trim().ifBlank { null },
                                        photos = variantPhotos.toList(),
                                        initialStock = variantQtyStr.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                                        existingStock = 0,
                                        isExistingVariant = false
                                    )
                                )

                                variantColor = ""
                                variantSize = ""
                                variantSku = ""
                                variantBarcode = ""
                                variantCostStr = ""
                                variantPriceStr = ""
                                variantQtyStr = ""
                                variantPhotos.clear()
                                showVariantInputs = false
                            },
                            variant = ERPButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (variants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    variants.forEachIndexed { idx, v ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(
                                    1.dp,
                                    if (v.isExistingVariant) SecondaryTeal.copy(alpha = 0.4f) else ERPDivider,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Variant photo thumbnail preview
                                    val firstPhoto = v.photos.firstOrNull { it.isPrimary } ?: v.photos.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                    ) {
                                        if (firstPhoto != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(rememberImageModel(firstPhoto.uriString))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.align(Alignment.Center).size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = v.sku,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (v.isExistingVariant) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(SecondaryTeal.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (language == AppLanguage.BANGLA) "বিদ্যমান" else "Existing",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SecondaryTeal,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Color: ${v.color.ifBlank { "-" }} • Size: ${v.size.ifBlank { "-" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            if (v.priceMinor > 0L) {
                                                Text(
                                                    text = "• ৳ ${Formatters.formatMoney(v.priceMinor, CurrencyCode.BDT, language)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryPurple
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons: Edit details & Delete
                                    IconButton(
                                        onClick = {
                                            editingVariantIdx = if (editingVariantIdx == idx) null else idx
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (editingVariantIdx == idx) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (!v.isExistingVariant) {
                                        IconButton(
                                            onClick = { variants.removeAt(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                // Stock Overview Badges (handles the exact scenario: e.g. 50 pcs previous + 100 pcs new = 150 pcs total)
                                if (v.isExistingVariant) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Previous stock badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE2E8F0))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${if (language == AppLanguage.BANGLA) "আগের মজুদ:" else "Previous:"} ${v.existingStock} $unit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // Added stock badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (v.initialStock > 0) SecondaryTeal.copy(alpha = 0.18f) else Color(0xFFE2E8F0).copy(alpha = 0.6f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${if (language == AppLanguage.BANGLA) "নতুন যোগ:" else "Added:"} +${v.initialStock} $unit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (v.initialStock > 0) SecondaryTeal else TextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Total stock badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryPurple.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${if (language == AppLanguage.BANGLA) "মোট হবে:" else "Total:"} ${v.existingStock + v.initialStock} $unit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Inline Input for Stock Quantity + Quick Add Chips (+10, +50, +100)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (v.initialStock > 0) v.initialStock.toString() else "",
                                        onValueChange = { newVal ->
                                            val qty = newVal.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                                            variants[idx] = v.copy(initialStock = qty)
                                        },
                                        label = {
                                            Text(
                                                if (v.isExistingVariant) {
                                                    if (language == AppLanguage.BANGLA) "নতুন যোগ ($unit)" else "Add Qty ($unit)"
                                                } else {
                                                    if (language == AppLanguage.BANGLA) "পরিমাণ ($unit)" else "Qty ($unit)"
                                                }
                                            )
                                        },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Quick bump buttons
                                    listOf(10, 50, 100).forEach { bump ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryPurple.copy(alpha = 0.08f))
                                                .clickable {
                                                    variants[idx] = v.copy(initialStock = v.initialStock + bump)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "+$bump",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Expanded variant detail editing (color, size, barcode, price)
                                if (editingVariantIdx == idx) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = v.color,
                                            onValueChange = { variants[idx] = v.copy(color = it) },
                                            label = { Text(stringResource(R.string.color)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = v.size,
                                            onValueChange = { variants[idx] = v.copy(size = it) },
                                            label = { Text(stringResource(R.string.size)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = v.sku,
                                            onValueChange = { variants[idx] = v.copy(sku = it.uppercase()) },
                                            label = { Text("Variant SKU") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = v.barcode ?: "",
                                            onValueChange = { variants[idx] = v.copy(barcode = it.ifBlank { null }) },
                                            label = { Text("Barcode") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = if (v.costMinor > 0) (v.costMinor / 100.0).toString() else "",
                                            onValueChange = { input ->
                                                val minor = Money.parseFromString(input).minorUnits
                                                variants[idx] = v.copy(costMinor = minor)
                                            },
                                            label = { Text(if (language == AppLanguage.BANGLA) "ক্রয় খরচ" else "Cost Price") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = if (v.priceMinor > 0) (v.priceMinor / 100.0).toString() else "",
                                            onValueChange = { input ->
                                                val minor = Money.parseFromString(input).minorUnits
                                                variants[idx] = v.copy(priceMinor = minor)
                                            },
                                            label = { Text(if (language == AppLanguage.BANGLA) "বিক্রয় মূল্য" else "Selling Price") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { editingVariantIdx = null },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.BANGLA) "সম্পন্ন" else "Done",
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                }

                Divider(color = ERPDivider)

                // Fixed Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ERPActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        variant = ERPButtonVariant.GHOST
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    val isRestock = selectedExistingProduct != null
                    ERPActionButton(
                        text = if (isRestock) {
                            if (language == AppLanguage.BANGLA) "মজুদ যোগ ও সংরক্ষণ করুন" else "Restock & Save Product"
                        } else {
                            stringResource(R.string.add_product)
                        },
                        onClick = {
                            if (nameEn.isBlank()) {
                                errorMessage = "Product English name cannot be empty"
                            } else if (baseSku.isBlank()) {
                                errorMessage = "Base SKU cannot be empty"
                            } else if (selectedCategory == null) {
                                errorMessage = "Please select a category"
                            } else {
                                val purchaseMinor = Money.parseFromString(purchasePriceStr).minorUnits
                                val sellingMinor = Money.parseFromString(sellingPriceStr).minorUnits
                                val minStock = minStockAlertStr.toIntOrNull() ?: 10
                                val variantStockSum = variants.sumOf { it.initialStock }
                                val additionalStock = if (variantStockSum > 0) variantStockSum else (initialStockStr.toIntOrNull() ?: 0)
                                val whId = selectedWarehouse?.id ?: ""
                                val excludeId = selectedExistingProduct?.id

                                scope.launch {
                                    var skuOk = true
                                    if (onValidateSku != null) {
                                        skuOk = onValidateSku(baseSku.trim().uppercase(), excludeId)
                                        if (!skuOk) {
                                            errorMessage = "Base SKU '$baseSku' is already in use"
                                        }
                                    }
                                    if (skuOk) {
                                        var barcodeOk = true
                                        if (onValidateBarcode != null && barcode.isNotBlank()) {
                                            barcodeOk = onValidateBarcode(barcode.trim(), excludeId)
                                            if (!barcodeOk) {
                                                errorMessage = "Barcode '$barcode' is already in use"
                                            }
                                        }
                                        if (barcodeOk) {
                                            val cat = selectedCategory
                                            if (cat != null) {
                                                val nameBnFinal = if (nameBn.isBlank()) nameEn.trim() else nameBn.trim()
                                                val barcodeFinal = if (barcode.isBlank()) null else barcode.trim()
                                                val unitFinal = if (unit.isBlank()) "PCS" else unit.trim()

                                                if (isRestock && onUpdateExistingProduct != null && selectedExistingProduct != null) {
                                                    onUpdateExistingProduct(
                                                        selectedExistingProduct!!.id,
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
                                                        deletedImageIds.toList(),
                                                        additionalStock,
                                                        whId
                                                    )
                                                } else {
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
                                                        additionalStock,
                                                        whId
                                                    )
                                                }
                                            }
                                        }
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

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var catEn by remember { mutableStateOf("") }
        var catBn by remember { mutableStateOf("") }
        var catErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.BANGLA) "নতুন ক্যাটাগরি যোগ করুন" else "Add New Category",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (catErr != null) {
                        Text(text = catErr!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = catEn,
                        onValueChange = { catEn = it; catErr = null },
                        label = { Text("Category Name (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catBn,
                        onValueChange = { catBn = it; catErr = null },
                        label = { Text("ক্যাটাগরির নাম (বাংলা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (catEn.isBlank()) {
                            catErr = "English name required"
                            return@TextButton
                        }
                        onAddNewCategory?.invoke(catEn.trim(), catBn.trim()) { newCat ->
                            selectedCategory = newCat
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}
