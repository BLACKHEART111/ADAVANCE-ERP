package com.pocketdev.advance_erp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.BarcodeView
import com.pocketdev.advance_erp.core.designsystem.QrCodeView
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.core.util.LabelPrintingEngine
import com.pocketdev.advance_erp.core.util.LabelSheetFormat
import com.pocketdev.advance_erp.core.util.ProductLabelData
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant

@Composable
fun ProductLabelPrintDialog(
    product: Product,
    variants: List<ProductVariant> = emptyList(),
    companyName: String = "ADVANCE ERP",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedVariant by remember { mutableStateOf<ProductVariant?>(variants.firstOrNull()) }
    var selectedFormat by remember { mutableStateOf(LabelSheetFormat.THERMAL_50X30) }
    var labelCountText by remember { mutableStateOf("1") }
    var isGenerating by remember { mutableStateOf(false) }

    val currentSku = selectedVariant?.sku ?: product.baseSku
    val currentBarcode = selectedVariant?.barcode?.takeIf { it.isNotBlank() } ?: product.barcode?.takeIf { it.isNotBlank() } ?: currentSku
    val currentPrice = selectedVariant?.variantPriceMinor?.takeIf { it > 0L } ?: product.sellingPriceMinor
    val qrPayload = remember(product.id, selectedVariant?.id) {
        if (selectedVariant != null) {
            QrCodeGenerator.createVariantQrPayload(selectedVariant!!.id, product.id, currentSku)
        } else {
            QrCodeGenerator.createProductQrPayload(product.id, currentSku)
        }
    }

    val labelData = remember(product, selectedVariant, currentSku, currentBarcode, currentPrice, qrPayload) {
        ProductLabelData(
            productName = product.nameEn,
            sku = currentSku,
            barcode = currentBarcode,
            qrPayload = qrPayload,
            variantDescription = selectedVariant?.let { "${it.color ?: ""} ${it.size ?: ""}".trim().ifEmpty { null } },
            priceMinor = currentPrice,
            companyName = companyName
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Barcode & Label Printing",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "লেবেল ও বারকোড প্রিন্ট — ${product.nameEn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Variant selector if available
                    if (variants.isNotEmpty()) {
                        Text("Select Variant / ভেরিয়েন্ট:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedVariant == null,
                                onClick = { selectedVariant = null },
                                label = { Text("Base Product") }
                            )
                            variants.forEach { v ->
                                val label = "${v.color ?: ""} ${v.size ?: ""}".trim().ifEmpty { v.sku }
                                FilterChip(
                                    selected = selectedVariant?.id == v.id,
                                    onClick = { selectedVariant = v },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Format selector
                    Text("Label Sheet Format / লেবেল ফরম্যাট:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LabelSheetFormat.values().forEach { fmt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(fmt.displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Print count input
                    OutlinedTextField(
                        value = labelCountText,
                        onValueChange = { labelCountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quantity to Print / লেবেল সংখ্যা") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Interactive Preview Box
                    Text("Live Label Preview / প্রিভিউ:", style = MaterialTheme.typography.labelLarge)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    companyName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF263849)
                                )
                                Text(
                                    "MRP: ৳${Formatters.formatMinorUnit(currentPrice)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF792B87)
                                )
                            }
                            Text(
                                product.nameEn,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            val subInfo = labelData.variantDescription?.let { "$it | SKU: $currentSku" } ?: "SKU: $currentSku"
                            Text(
                                subInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(44.dp)) {
                                    BarcodeView(
                                        code = currentBarcode,
                                        modifier = Modifier.fillMaxSize(),
                                        showActions = false
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.size(44.dp)) {
                                    QrCodeView(
                                        payload = qrPayload,
                                        modifier = Modifier.fillMaxSize(),
                                        showActions = false
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

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
                            isGenerating = true
                            val count = labelCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val pdfFile = if (selectedFormat == LabelSheetFormat.THERMAL_50X30) {
                                LabelPrintingEngine.generateThermalRollPdf(context, labelData, count)
                            } else {
                                LabelPrintingEngine.generateA4StickerSheetPdf(context, labelData, selectedFormat, count)
                            }
                            val shareIntent = InvoicePdfGenerator.createSharePdfIntent(context, pdfFile)
                            context.startActivity(shareIntent)
                            isGenerating = false
                            onDismiss()
                        },
                        enabled = !isGenerating
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGenerating) "Generating..." else "Print / Export PDF")
                    }
                }
            }
        }
    }
}
