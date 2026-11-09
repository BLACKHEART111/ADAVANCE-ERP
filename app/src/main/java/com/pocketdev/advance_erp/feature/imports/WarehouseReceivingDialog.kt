package com.pocketdev.advance_erp.feature.imports

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
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
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft

private data class ReceivingLineState(
    val cartonId: String?,
    val cartonNumber: String,
    val productId: String,
    val variantId: String? = null,
    val productName: String,
    val expectedQty: Int,
    var receivedQtyStr: String,
    var goodQtyStr: String,
    var damagedQtyStr: String = "0",
    var missingQtyStr: String = "0",
    var conditionNotes: String = "Good Condition"
)

@Composable
fun WarehouseReceivingDialog(
    shipment: ImportShipment? = null,
    order: PurchaseOrder? = null,
    poItems: List<PurchaseOrderItem> = emptyList(),
    cartons: List<ShipmentCarton> = emptyList(),
    products: List<Product> = emptyList(),
    isCostFinalized: Boolean = order?.isActualCostFinalized ?: false,
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onSaveDraft: ((warehouseId: String, receivingMethod: String, items: List<ReceiptItemDraft>, notes: String?) -> Unit)? = null,
    onConfirmPost: ((warehouseId: String, items: List<ReceiptItemDraft>, notes: String?, allowDiscrepancy: Boolean) -> Unit)? = null
) {
    var selectedMethod by remember { mutableStateOf(if (cartons.isNotEmpty()) "CARTON_WISE" else "PHYSICAL_COUNT") }
    var destinationWarehouseId by remember { mutableStateOf("wh-dhk-01") }
    var notes by remember { mutableStateOf("Warehouse receiving inspection completed.") }
    var showBlockedCostDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isPostingAction by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val productsMap = remember(products) { products.associateBy { it.id } }

    // Initialize lines based on selected method
    val lines = remember(selectedMethod, cartons, poItems) {
        val list = mutableStateListOf<ReceivingLineState>()
        if (selectedMethod == "CARTON_WISE" && cartons.isNotEmpty()) {
            cartons.forEach { c ->
                val prod = productsMap[c.productId]
                val name = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod?.nameBn!! else prod?.nameEn ?: c.productId
                list.add(
                    ReceivingLineState(
                        cartonId = c.id,
                        cartonNumber = c.cartonNumber,
                        productId = c.productId,
                        variantId = c.variantId,
                        productName = name,
                        expectedQty = c.quantity,
                        receivedQtyStr = c.quantity.toString(),
                        goodQtyStr = c.quantity.toString(),
                        damagedQtyStr = "0",
                        missingQtyStr = "0"
                    )
                )
            }
        } else {
            // Physical Count method: lines per purchase order item
            poItems.forEach { item ->
                val prod = productsMap[item.productId]
                val name = if (isBangla && !prod?.nameBn.isNullOrBlank()) prod?.nameBn!! else prod?.nameEn ?: item.productId
                val remaining = (item.quantity - item.receivedQuantity).coerceAtLeast(0)
                list.add(
                    ReceivingLineState(
                        cartonId = null,
                        cartonNumber = "SUMMARY",
                        productId = item.productId,
                        variantId = item.variantId,
                        productName = name,
                        expectedQty = remaining,
                        receivedQtyStr = remaining.toString(),
                        goodQtyStr = remaining.toString(),
                        damagedQtyStr = "0",
                        missingQtyStr = "0"
                    )
                )
            }
        }
        list
    }

    fun validateAndBuildDrafts(): List<ReceiptItemDraft>? {
        errorMessage = null
        val drafts = mutableListOf<ReceiptItemDraft>()
        for ((idx, line) in lines.withIndex()) {
            val received = line.receivedQtyStr.trim().toIntOrNull() ?: -1
            val good = line.goodQtyStr.trim().toIntOrNull() ?: -1
            val damaged = line.damagedQtyStr.trim().toIntOrNull() ?: 0
            val missing = line.missingQtyStr.trim().toIntOrNull() ?: 0

            if (received < 0 || good < 0 || damaged < 0 || missing < 0) {
                errorMessage = "Row #${idx + 1} (${line.productName}): Quantities cannot be negative."
                return null
            }
            if (good + damaged + missing != received) {
                errorMessage = "Row #${idx + 1} (${line.productName}): Received ($received) must equal Good ($good) + Damaged ($damaged) + Missing ($missing)."
                return null
            }

            drafts.add(
                ReceiptItemDraft(
                    cartonId = line.cartonId,
                    productId = line.productId,
                    variantId = line.variantId,
                    expectedQuantity = line.expectedQty,
                    receivedQuantity = received,
                    damagedQuantity = damaged,
                    missingQuantity = missing,
                    excessQuantity = (received - line.expectedQty).coerceAtLeast(0),
                    conditionNotes = line.conditionNotes
                )
            )
        }
        return drafts
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
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ERPBackground)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavy)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ওয়্যারহাউস পণ্য গ্রহণ ও পরিদর্শন" else "Warehouse Receiving Inspection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (order != null) "Purchase Order: ${order.poNumber}" else "Shipment: ${shipment?.shipmentNumber ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Cost Finalized Status Alert Banner
                if (!isCostFinalized) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "প্রকৃত খরচ চূড়ান্তকরণ বাকি — স্টক পোস্টিং সাময়িক লক" else "Actual Landing Cost Pending — Stock Posting Locked",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = if (isBangla)
                                        "আপনি খসড়া পরিদর্শন সংরক্ষণ করতে পারবেন, তবে প্রকৃত খরচ চূড়ান্ত না করা পর্যন্ত মজুদ ইনভেন্টরি পোস্টিং হবে না।"
                                    else
                                        "Draft inspections can be recorded, but stock posting to inventory is blocked until Actual Landing Cost is finalized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "প্রকৃত ল্যান্ডেড কস্ট চূড়ান্তকৃত! স্টক পোস্টিং অনুমোদিত।" else "Actual Landed Cost Finalized! Stock posting unlocked.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }

                // Receiving Method Switcher: Method A (Physical Count) vs Method B (Carton-wise)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Method A: Physical Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMethod == "PHYSICAL_COUNT") PrimaryPurple else Color.White)
                            .border(1.dp, if (selectedMethod == "PHYSICAL_COUNT") PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { selectedMethod = "PHYSICAL_COUNT" }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "পদ্ধতি ক: সরাসরি গণনা (Physical Count)" else "Method A: Physical Count",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMethod == "PHYSICAL_COUNT") Color.White else TextPrimary
                        )
                    }

                    // Method B: Carton-wise
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMethod == "CARTON_WISE") PrimaryPurple else Color.White)
                            .border(1.dp, if (selectedMethod == "CARTON_WISE") PrimaryPurple else Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { selectedMethod = "CARTON_WISE" }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBangla) "পদ্ধতি খ: কার্টন-ভিত্তিক (Carton-wise)" else "Method B: Carton-wise",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMethod == "CARTON_WISE") Color.White else TextPrimary
                        )
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Items Inspection Table
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = line.productName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepNavy
                                        )
                                        if (line.cartonNumber.isNotBlank() && line.cartonNumber != "SUMMARY") {
                                            Text(
                                                text = "Carton: ${line.cartonNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Expected: ${line.expectedQty}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Inputs: Received, Good, Damaged, Missing
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = line.receivedQtyStr,
                                        onValueChange = { newVal ->
                                            line.receivedQtyStr = newVal
                                            line.goodQtyStr = newVal
                                            line.damagedQtyStr = "0"
                                            line.missingQtyStr = "0"
                                        },
                                        label = { Text("Received", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = line.goodQtyStr,
                                        onValueChange = { line.goodQtyStr = it },
                                        label = { Text("Good", fontSize = 11.sp, color = StatusSuccess) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = line.damagedQtyStr,
                                        onValueChange = { line.damagedQtyStr = it },
                                        label = { Text("Damaged", fontSize = 11.sp, color = StatusError) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = line.missingQtyStr,
                                        onValueChange = { line.missingQtyStr = it },
                                        label = { Text("Missing", fontSize = 11.sp, color = Color(0xFFD97706)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = line.conditionNotes,
                                    onValueChange = { line.conditionNotes = it },
                                    label = { Text(if (isBangla) "পরিদর্শন মন্তব্য" else "Inspection notes", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Bottom Action Footer
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 1: Save Draft (always available)
                        OutlinedButton(
                            onClick = {
                                val drafts = validateAndBuildDrafts() ?: return@OutlinedButton
                                onSaveDraft?.invoke(destinationWarehouseId, selectedMethod, drafts, notes)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isBangla) "খসড়া সংরক্ষণ" else "Save Draft")
                        }

                        // Button 2: Post to Stock (Inventory Capitalization)
                        Button(
                            onClick = {
                                if (!isCostFinalized) {
                                    showBlockedCostDialog = true
                                    return@Button
                                }
                                val drafts = validateAndBuildDrafts() ?: return@Button
                                isPostingAction = true
                                showConfirmationDialog = true
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCostFinalized) StatusSuccess else Color.Gray
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (isCostFinalized) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isBangla) "স্টকে পোস্টিং করুন" else "Post to Stock")
                        }
                    }
                }
            }
        }
    }

    // Cost Blocked Warning Dialog
    if (showBlockedCostDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedCostDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706)) },
            title = {
                Text(
                    text = if (isBangla) "প্রকৃত খরচ চূড়ান্তকরণ বাধ্যতামূলক" else "Actual Landing Cost Finalization Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "মজুদ পণ্যে পোস্টিং করার আগে প্রকৃত ল্যান্ডেড কস্ট (Actual Landing Cost) চূড়ান্ত করতে হবে। অনুগ্রহ করে ক্রয়াদেশের 'Landing Cost' সেকশনে গিয়ে প্রকৃত খরচ এন্ট্রি করে 'Finalize Actual Landing Cost' সম্পন্ন করুন।"
                    else
                        "Stock posting into inventory is strictly blocked until Actual Landing Cost is finalized. Please enter actual expenses and click 'Finalize Actual Landing Cost' first."
                )
            },
            confirmButton = {
                Button(onClick = { showBlockedCostDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Confirm Post Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBangla) "মজুদ পোস্টিং নিশ্চিতকরণ" else "Confirm Stock Posting",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি নিশ্চিত যে এই রিসিভিং মজুদ ইনভেন্টরিতে পোস্টিং করতে চান? এটি শুধুমাত্র ভালো পণ্য (Good Quantity) স্টকে যোগ করবে, নষ্ট পণ্য অবলোপন করবে এবং ঘাটতির জন্য সরবরাহকারীর কাছে দাবি রেকর্ড করবে।"
                    else
                        "Are you sure you want to post this receiving to inventory? This will debit Inventory for good units, write off damaged stock, and log shortage claims."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        val drafts = validateAndBuildDrafts() ?: return@Button
                        if (onConfirmPost != null) {
                            onConfirmPost(destinationWarehouseId, drafts, notes, true)
                        } else {
                            onSaveDraft?.invoke(destinationWarehouseId, selectedMethod, drafts, notes)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text(text = if (isBangla) "হ্যাঁ, পোস্টিং করুন" else "Yes, Post to Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
