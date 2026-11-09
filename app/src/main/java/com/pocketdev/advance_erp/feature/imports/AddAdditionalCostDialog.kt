package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriceCheck
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AddAdditionalCostDialog(
    orderId: String,
    warehouseId: String? = "wh-dhk-01",
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        category: AdditionalCostCategory,
        description: String,
        amountMinor: Long,
        isCapitalizable: Boolean,
        allocationMethod: String,
        notes: String?
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<AdditionalCostCategory>(AdditionalCostCategory.HANDLING) }
    var description by remember { mutableStateOf("Local handling and warehousing carriage") }
    var amountStr by remember { mutableStateOf("5000.00") }
    var isCapitalizable by remember { mutableStateOf(true) }
    var allocationMethod by remember { mutableStateOf("QUANTITY") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ERPBackground)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(PrimaryPurple.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = PrimaryPurple)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "অতিরিক্ত পরিচালন ব্যয়" else "Post-Arrival Additional Cost",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Text(
                                text = if (isBangla) "মূলধনীকরণ বনাম সাময়িক খরচ ব্যবস্থাপনা" else "Capitalizable vs Period Expense Tracking",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = ERPDivider, modifier = Modifier.padding(vertical = 12.dp))

                // Category Selector
                Text(
                    text = if (isBangla) "ব্যয়ের ধরণ (Category)" else "Cost Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val availableCats = listOf(
                        AdditionalCostCategory.HANDLING,
                        AdditionalCostCategory.WAREHOUSE_RENT,
                        AdditionalCostCategory.REPACKING,
                        AdditionalCostCategory.LOADING_UNLOADING
                    )
                    availableCats.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryPurple else Color.White)
                                .clickable {
                                    selectedCategory = cat
                                    description = when (cat) {
                                        AdditionalCostCategory.HANDLING -> "Warehouse merchandise handling charge"
                                        AdditionalCostCategory.WAREHOUSE_RENT -> "Warehouse rent and storage fee"
                                        AdditionalCostCategory.REPACKING -> "Merchandise repacking and sorting fee"
                                        AdditionalCostCategory.LOADING_UNLOADING -> "Truck loading and unloading charge"
                                        else -> "Destination operational expense"
                                    }
                                    isCapitalizable = cat.isCapitalizableDefault
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBangla) cat.titleBn else cat.titleEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                maxLines = 1,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isBangla) "বিবরণ" else "Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isBangla) "পরিমাণ (BDT)" else "Amount in BDT") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Capitalizable Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCapitalizable) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCapitalizable)
                                    (if (isBangla) "ইনভেন্টরি অ্যাসেটে মূলধনীকরণ (Capitalizable)" else "Capitalize to Inventory Asset")
                                else
                                    (if (isBangla) "লাভ-ক্ষতি হিসাবে প্রত্যক্ষ ব্যয় (Period Expense)" else "Charge as Period Operating Expense"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCapitalizable) StatusSuccess else StatusError
                            )
                            Text(
                                text = if (isCapitalizable)
                                    (if (isBangla) "ডেবিট: ১২১০ ইনভেন্টরি অ্যাসেট (বা ৫০১০ COGS যদি বিক্রি হয়ে থাকে)" else "Dr. 1210 Inventory Asset (or 5010 COGS if already sold) / Cr. Cash/Bank")
                                else
                                    (if (isBangla) "ডেবিট: ৫১১০ পরিচালন ব্যয় / ক্রেডিট: ১০২০ ক্যাশ বা ব্যাংক" else "Dr. 5110 Operating Expense / Cr. Cash or Bank"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isCapitalizable,
                            onCheckedChange = { isCapitalizable = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusSuccess,
                                checkedTrackColor = StatusSuccess.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isBangla) "মন্তব্য (ঐচ্ছিক)" else "Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            try {
                                val amtBd = BigDecimal(amountStr.trim())
                                require(amtBd > BigDecimal.ZERO) { "Amount must be greater than zero." }
                                val minor = amtBd.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
                                onConfirm(
                                    selectedCategory,
                                    description.trim(),
                                    minor,
                                    isCapitalizable,
                                    allocationMethod,
                                    notes.ifBlank { null }
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Invalid amount entered."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isBangla) "সংরক্ষণ করুন" else "Save Cost")
                    }
                }
            }
        }
    }
}
