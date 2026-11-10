package com.pocketdev.advance_erp.feature.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.DueReminderHelper
import com.pocketdev.advance_erp.core.util.DueReminderStatementData
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.StatementShareHelper

@Composable
fun DueReminderDialog(
    data: DueReminderStatementData,
    initialIsBangla: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isBangla by remember { mutableStateOf(initialIsBangla) }
    var captionText by remember(isBangla) {
        mutableStateOf(DueReminderHelper.generatePoliteReminderCaption(data, isBangla))
    }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhatsAppDarkGreen)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "হোয়াটসঅ্যাপ ভদ্র বকেয়া তাগাদা" else "WhatsApp Polite Due Reminder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBangla) "বিনয়ী ভাষা ও হালনাগাদ স্টেটমেন্ট PDF সহ শেয়ার করুন" else "Courteous language with attached statement PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Customer & Due Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = data.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${data.customerCode} • ${data.customerPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusError.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isBangla) "চলতি মোট বকেয়া" else "Outstanding Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusError,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = Formatters.formatMoney(
                                                data.totalDueMinor,
                                                CurrencyCode.BDT,
                                                if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError
                                        )
                                    }
                                }
                            }

                            if (data.invoiceCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isBangla) "অমীমাংসিত চালানের সংখ্যা: ${data.invoiceCount} টি" else "Pending unpaid invoices: ${data.invoiceCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // PDF Attachment Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (data.pdfFile != null && data.pdfFile.exists()) {
                                SecondaryTeal.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "হিসাব বিবরণী PDF স্টেটমেন্ট প্রস্তুত" else "Account Statement PDF Ready",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (data.pdfFile != null) {
                                            "${data.pdfFile.name} (${data.pdfFile.length() / 1024} KB)"
                                        } else {
                                            if (isBangla) "পিডিএফ লোড হচ্ছে..." else "Loading PDF..."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (data.pdfFile != null && data.pdfFile.exists()) {
                                OutlinedButton(
                                    onClick = { showPdfPreviewDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Preview",
                                        modifier = Modifier.size(16.dp),
                                        tint = PrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBangla) "প্রিভিউ" else "Preview",
                                        fontSize = 11.sp,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }

                    // Language Selection Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "বার্তার ভাষা নির্বাচন:" else "Message Language:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isBangla,
                                onClick = {
                                    isBangla = true
                                    captionText = DueReminderHelper.generatePoliteReminderCaption(data, true)
                                },
                                label = { Text("বাংলা", fontWeight = if (isBangla) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WhatsAppGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = !isBangla,
                                onClick = {
                                    isBangla = false
                                    captionText = DueReminderHelper.generatePoliteReminderCaption(data, false)
                                },
                                label = { Text("English", fontWeight = if (!isBangla) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WhatsAppGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Editable Caption Field
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "হোয়াটসঅ্যাপ বার্তার খসড়া (প্রয়োজনে এডিট করুন):" else "WhatsApp Message Draft (Editable):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isBangla) "খুব ভদ্র ও বিনয়ী ফরম্যাট" else "Courteous & Respectful",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppGreen,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Primary WhatsApp Action Button
                    Button(
                        onClick = {
                            StatementShareHelper.shareToWhatsApp(
                                context = context,
                                pdfFile = data.pdfFile,
                                caption = captionText,
                                phone = data.customerPhone
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBangla) "WhatsApp এ ভদ্র তাগাদা পাঠান (PDF সহ)" else "Send Polite Reminder via WhatsApp (with PDF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // Secondary Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (data.pdfFile != null) {
                                    StatementShareHelper.sharePdfOnly(context, data.pdfFile)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = data.pdfFile != null
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "শুধুমাত্র PDF" else "PDF Only", fontSize = 11.sp, color = PrimaryPurple)
                        }

                        OutlinedButton(
                            onClick = {
                                StatementShareHelper.shareCaptionOnly(context, captionText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(14.dp), tint = SecondaryTeal)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "শুধুমাত্র টেক্সট" else "Text Only", fontSize = 11.sp, color = SecondaryTeal)
                        }

                        OutlinedButton(
                            onClick = {
                                StatementShareHelper.copyToClipboard(context, captionText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "কপি টেক্সট" else "Copy Text", fontSize = 11.sp, color = DeepNavy)
                        }
                    }

                    // System Share Chooser
                    OutlinedButton(
                        onClick = {
                            StatementShareHelper.shareToAnyApp(context, data.pdfFile, captionText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "অন্যান্য অ্যাপে শেয়ার করুন" else "Share via Other Apps", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showPdfPreviewDialog && data.pdfFile != null) {
        PdfViewerDialog(
            pdfFile = data.pdfFile,
            reportTitle = if (isBangla) "বকেয়া হিসাব বিবরণী" else "Due Statement",
            isBangla = isBangla,
            onDismiss = { showPdfPreviewDialog = false }
        )
    }
}
