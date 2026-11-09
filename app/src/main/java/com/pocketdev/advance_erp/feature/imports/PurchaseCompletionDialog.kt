package com.pocketdev.advance_erp.feature.imports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.PurchaseOrder

@Composable
fun PurchaseCompletionDialog(
    order: PurchaseOrder,
    totalOrdered: Int,
    totalReceived: Int,
    goodQuantity: Int,
    damagedQuantity: Int,
    missingQuantity: Int,
    paymentDueMinor: Long,
    isCostFinalized: Boolean,
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmComplete: (disposition: String?, reason: String?, notes: String?) -> Unit
) {
    val remainingQuantity = (totalOrdered - totalReceived).coerceAtLeast(0)
    var selectedDisposition by remember { mutableStateOf("SHORTAGE") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dispositions = listOf(
        "SHORTAGE" to if (isBangla) "ঘাটতি অমীমাংসিত (Shortage Discrepancy)" else "Shortage Discrepancy",
        "CANCELLED_BALANCE" to if (isBangla) "অবশিষ্ট অর্ডার বাতিল (Cancelled Balance)" else "Cancelled Unfulfilled Balance",
        "SUPPLIER_CLAIM" to if (isBangla) "সরবরাহকারী ক্ষতিপূরণ দাবি (Supplier Claim)" else "Supplier Claim"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ERPBackground)
            ) {
                // Header
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ক্রয়াদেশ সমাপ্তিকরণ (Manual Completion)" else "Manual Purchase Completion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Purchase: ${order.poNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary Audit Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isBangla) "চূড়ান্ত নিরীক্ষা ও স্থিতি সারসংক্ষেপ:" else "Final Order Reconciliation Summary:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ordered Quantity:", color = TextSecondary, fontSize = 13.sp)
                                Text("$totalOrdered units", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Received:", color = TextSecondary, fontSize = 13.sp)
                                Text("$totalReceived units", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Good (In Stock):", color = TextSecondary, fontSize = 13.sp)
                                Text("$goodQuantity units", fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Damaged (Written Off):", color = TextSecondary, fontSize = 13.sp)
                                Text("$damagedQuantity units", fontWeight = FontWeight.Bold, color = StatusError)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Missing (Shortage Claim):", color = TextSecondary, fontSize = 13.sp)
                                Text("$missingQuantity units", fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Unreceived:", color = TextSecondary, fontSize = 13.sp)
                                Text("$remainingQuantity units", fontWeight = FontWeight.Bold, color = if (remainingQuantity > 0) StatusError else StatusSuccess)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Due Balance:", color = TextSecondary, fontSize = 13.sp)
                                val dueMajor = paymentDueMinor / 100
                                Text("${order.currency} $dueMajor", fontWeight = FontWeight.Bold, color = if (paymentDueMinor > 0) StatusWarning else StatusSuccess)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Actual Cost Status:", color = TextSecondary, fontSize = 13.sp)
                                Text(if (isCostFinalized) "FINALIZED" else "PENDING", fontWeight = FontWeight.Bold, color = if (isCostFinalized) StatusSuccess else Color(0xFFD97706))
                            }
                        }
                    }

                    // If remaining unreceived units exist: require disposition and reason!
                    if (remainingQuantity > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla)
                                            "সতর্কতা: $remainingQuantity টি পণ্য অপূর্ণ রয়েছে! নিষ্পত্তি কারণ বাধ্যতামূলক।"
                                        else
                                            "Notice: $remainingQuantity units unreceived! Explicit disposition required.",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }

                                Text(
                                    text = if (isBangla) "অপূর্ণ অংশের নিষ্পত্তি নির্বাচন করুন:" else "Select Unreceived Disposition:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )

                                dispositions.forEach { (dispKey, dispLabel) ->
                                    val isSel = dispKey == selectedDisposition
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) PrimaryPurple.copy(alpha = 0.15f) else Color.White)
                                            .border(1.dp, if (isSel) PrimaryPurple else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .clickable { selectedDisposition = dispKey }
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = dispLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) PrimaryPurple else TextPrimary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text(if (isBangla) "নিষ্পত্তির কারণ (বাধ্যতামূলক)" else "Disposition Reason (Required)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = StatusError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isBangla) "সমাপ্তি মন্তব্য (বাধ্যতামূলক নয়)" else "Completion Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isBangla) "বাতিল" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (remainingQuantity > 0 && reason.isBlank()) {
                                    errorMessage = if (isBangla)
                                        "অবশিষ্ট $remainingQuantity পণ্যের নিষ্পত্তির কারণ উল্লেখ করা বাধ্যতামূলক।"
                                    else
                                        "Reason is required for $remainingQuantity unreceived units."
                                    return@Button
                                }
                                onConfirmComplete(
                                    if (remainingQuantity > 0) selectedDisposition else null,
                                    reason.ifBlank { null },
                                    notes.ifBlank { null }
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text(text = if (isBangla) "সম্পন্ন নিশ্চিত করুন" else "Confirm Complete")
                        }
                    }
                }
            }
        }
    }
}
