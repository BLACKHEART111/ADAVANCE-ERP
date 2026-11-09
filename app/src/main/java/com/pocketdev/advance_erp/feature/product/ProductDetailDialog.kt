package com.pocketdev.advance_erp.feature.product

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.BarcodeView
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.FullScreenImageDialog
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.QrCodeView
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance

@Composable
fun ProductDetailDialog(
    product: ProductWithDetails,
    variants: List<ProductVariant>,
    productImages: List<ProductImage> = emptyList(),
    variantStockMap: Map<String, Int> = emptyMap(),
    language: AppLanguage,
    currency: CurrencyCode,
    preSelectedVariantId: String? = null,
    onLoadVariantImages: (suspend (variantId: String) -> List<ProductImage>)? = null,
    onEditProduct: (() -> Unit)? = null,
    onDeleteProduct: ((productId: String) -> Unit)? = null,
    onViewCostHistory: ((productId: String) -> Unit)? = null,
    onPrintLabels: ((product: ProductWithDetails, variants: List<ProductVariant>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Map of variantId to their primary image
    val variantImagesMap = remember { mutableStateMapOf<String, String>() }

    // Selected hero image uri (defaults to primary product photo or first photo)
    val defaultPrimaryUri = productImages.firstOrNull { it.isPrimary }?.imageUri
        ?: productImages.firstOrNull()?.imageUri
        ?: product.primaryImageUri
    var selectedHeroUri by remember(defaultPrimaryUri) { mutableStateOf(defaultPrimaryUri) }

    // Load variant images on launch
    LaunchedEffect(variants) {
        if (onLoadVariantImages != null) {
            variants.forEach { v ->
                val vImages = onLoadVariantImages(v.id)
                val primary = vImages.firstOrNull { it.isPrimary }?.imageUri ?: vImages.firstOrNull()?.imageUri
                if (primary != null) {
                    variantImagesMap[v.id] = primary
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Header: Title & Close & Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) product.nameBn else product.nameEn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.BANGLA) product.nameEn else product.nameBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEditProduct != null) {
                            IconButton(onClick = onEditProduct) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Product",
                                    tint = PrimaryPurple
                                )
                            }
                        }
                        if (onDeleteProduct != null) {
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Product",
                                    tint = StatusError
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Divider(color = ERPDivider)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {

                // Hero Image & Photo Gallery
                if (selectedHeroUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { fullScreenImageUri = selectedHeroUri }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(rememberImageModel(selectedHeroUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Primary Product Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Thumbnail Gallery Strip
                    if (productImages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(productImages, key = { it.id }) { img ->
                                val isSelected = img.imageUri == selectedHeroUri
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PrimaryPurple else ERPDivider,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedHeroUri = img.imageUri }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(rememberImageModel(img.imageUri))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Gallery Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Placeholder when product has no images
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, ERPDivider, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No photos added",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Badges: Category & Base SKU
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) product.categoryNameBn else product.categoryNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(SecondaryTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SKU: ${product.baseSku}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Financial Overview: Purchase Price & Selling Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailCard(
                        title = stringResource(R.string.purchase_price),
                        value = Formatters.formatMoney(product.purchasePriceMinor, currency, language),
                        color = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                    DetailCard(
                        title = stringResource(R.string.selling_price),
                        value = Formatters.formatMoney(product.sellingPriceMinor, currency, language),
                        color = PrimaryPurple,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stock Status
                val isLowStock = product.totalStock <= product.minStockAlert
                val stockColor = when {
                    product.totalStock == 0 -> StatusError
                    isLowStock -> Color(0xFFF59E0B)
                    else -> StatusSuccess
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(stockColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = stockColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.in_stock),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${Formatters.formatNumber(product.totalStock.toLong(), language)} ${product.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = stockColor
                            )
                        }
                    }

                    if (isLowStock) {
                        Text(
                            text = stringResource(R.string.low_stock_warning),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusError
                        )
                    }
                }

                if (onViewCostHistory != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryPurple.copy(alpha = 0.08f))
                            .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { onViewCostHistory(product.id) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "মজুদ ব্যয় ইতিহাস ও ফিফো লেয়ার" else "Cost History & FIFO Layers",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "View batch landed costs, remaining layers & adjustments",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode Section
                val barcodeValue = product.barcode?.ifBlank { null } ?: product.baseSku
                Text(
                    text = stringResource(R.string.barcode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                BarcodeView(code = barcodeValue)

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code Section
                val qrPayload = product.qrCode?.ifBlank { null }
                    ?: QrCodeGenerator.createProductQrPayload(product.id, product.baseSku)
                Text(
                    text = "QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                QrCodeView(payload = qrPayload, qrSizeDp = 150)

                Spacer(modifier = Modifier.height(16.dp))

                // Variants Section
                Text(
                    text = "${stringResource(R.string.variants)} (${variants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (variants.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_variants),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    variants.forEach { v ->
                        val isPreselected = v.id == preSelectedVariantId
                        val vImageUri = variantImagesMap[v.id] ?: selectedHeroUri

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPreselected) SecondaryTeal.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (isPreselected) 2.dp else 1.dp,
                                    color = if (isPreselected) SecondaryTeal else ERPDivider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Variant Thumbnail (or product primary photo fallback)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .clickable {
                                            if (vImageUri != null) {
                                                fullScreenImageUri = vImageUri
                                            }
                                        }
                                ) {
                                    if (vImageUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(rememberImageModel(vImageUri))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Variant Photo",
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

                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
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
                                        if (variantStockMap[v.id] != null) {
                                            val stock = variantStockMap[v.id]!!
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (stock <= 0) StatusError.copy(alpha = 0.15f)
                                                        else if (stock <= 5) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                        else StatusSuccess.copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$stock ${product.unit}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (stock <= 0) StatusError
                                                    else if (stock <= 5) Color(0xFFF59E0B)
                                                    else SecondaryTeal,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Color: ${v.color ?: "-"} • Size: ${v.size ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!v.barcode.isNullOrBlank()) {
                                        Text(
                                            text = "Barcode: ${v.barcode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatMoney(v.variantPriceMinor, currency, language),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = ERPDivider)

            // Fixed Bottom Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                if (onPrintLabels != null) {
                    ERPActionButton(
                        text = if (language == AppLanguage.BANGLA) "বারকোড ও স্টিকার লেবেল প্রিন্ট করুন" else "Print Barcode & Sticker Labels",
                        onClick = { onPrintLabels(product, variants) },
                        variant = ERPButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                ERPActionButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    variant = ERPButtonVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        }
    }

    // Delete confirmation dialog
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
                                    "⚠️ সতর্কতা: এই পণ্যের বর্তমানে ${product.totalStock} ${product.unit} মজুদ রয়েছে। পণ্যটি মুছে ফেললে তা সক্রিয় ক্যাটালগ থেকে বাদ দেওয়া হবে।"
                                else
                                    "⚠️ Warning: This product currently has ${product.totalStock} ${product.unit} in stock. Deleting it will remove it from active catalog.",
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
                        onDeleteProduct(product.id)
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

    // Full screen image preview dialog
    if (fullScreenImageUri != null) {
        FullScreenImageDialog(
            imageUriOrPath = fullScreenImageUri!!,
            onDismiss = { fullScreenImageUri = null }
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
