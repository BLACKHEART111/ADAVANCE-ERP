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
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.DueCollectionCaptionHelper
import com.pocketdev.advance_erp.core.util.DueCollectionStatementData
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.StatementShareHelper

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)

@Composable
fun DueCollectionStatementShareDialog(
    data: DueCollectionStatementData,
    initialIsBangla: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isBangla by remember { mutableStateOf(initialIsBangla) }
    var captionText by remember(isBangla) {
        mutableStateOf(DueCollectionCaptionHelper.generateCaption(data, isBangla))
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
                    .padding(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "WhatsApp",
                                tint = WhatsAppDarkGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "পেমেন্ট স্টেটমেন্ট ও WhatsApp ক্যাপশন" else "Payment Statement & WhatsApp Caption",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${data.customerName} • #${data.paymentNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = DeepNavy.copy(alpha = 0.1f)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Financial Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previous Due
                        SummaryMiniCard(
                            label = if (isBangla) "পূর্বের বকেয়া" else "Previous Due",
                            value = "৳" + Formatters.formatMinorUnit(data.previousDueMinor),
                            color = Color(0xFFC53030),
                            modifier = Modifier.weight(1f)
                        )
                        // Paid Today
                        SummaryMiniCard(
                            label = if (isBangla) "আজকের জমা" else "Paid Today",
                            value = "৳" + Formatters.formatMinorUnit(data.amountReceivedMinor),
                            color = StatusSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        // Current Due / Advance
                        val currentDueVal = if (data.isAdvancePayment && data.advanceAmountMinor > 0L) {
                            "৳" + Formatters.formatMinorUnit(data.advanceAmountMinor) + " (Adv)"
                        } else {
                            "৳" + Formatters.formatMinorUnit(maxOf(0L, data.currentDueMinor))
                        }
                        SummaryMiniCard(
                            label = if (data.isAdvancePayment) (if (isBangla) "অ্যাডভান্স ব্যালেন্স" else "Advance Balance")
                                    else (if (isBangla) "বর্তমান বকেয়া" else "Current Due"),
                            value = currentDueVal,
                            color = if (data.isAdvancePayment) SecondaryTeal else if (data.currentDueMinor == 0L) StatusSuccess else Color(0xFFC53030),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language and Reset Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isBangla,
                                onClick = { isBangla = true },
                                label = { Text("বাংলা (Bangla)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryPurple
                                )
                            )
                            FilterChip(
                                selected = !isBangla,
                                onClick = { isBangla = false },
                                label = { Text("English") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryPurple
                                )
                            )
                        }

                        TextButtonRow(
                            onClick = {
                                captionText = DueCollectionCaptionHelper.generateCaption(data, isBangla)
                            },
                            text = if (isBangla) "ডিফল্ট ক্যাপশন রিসেট" else "Reset Caption"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Editable Caption Field
                    Text(
                        text = if (isBangla) "WhatsApp বার্তা এডিট করুন:" else "Edit WhatsApp Caption Message:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppDarkGreen,
                            unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Bar Note
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = WhatsAppGreen.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = WhatsAppDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla)
                                    "গ্রাহকের সম্পূর্ণ হিসাব লেজার PDF তৈরি হয়েছে। WhatsApp-এ পাঠালে PDF ফাইল ও টেক্সট ক্যাপশন একসাথে সংযুক্ত হবে।"
                                else
                                    "Customer's statement PDF is ready. Sharing will send both the PDF document and this text caption.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavy
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DeepNavy.copy(alpha = 0.1f)
                )

                // Actions Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary WhatsApp Share Button
                    Button(
                        onClick = {
                            StatementShareHelper.shareToWhatsApp(
                                context = context,
                                pdfFile = data.statementPdfFile,
                                caption = captionText,
                                phone = data.customerPhone
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "WhatsApp-এ পাঠান (PDF + ক্যাপশন)" else "Share to WhatsApp (PDF + Caption)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Secondary Share Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF Preview
                        OutlinedButton(
                            onClick = { showPdfPreviewDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "PDF প্রিভিউ" else "PDF Preview", fontSize = 12.sp)
                        }

                        // Share PDF Only
                        OutlinedButton(
                            onClick = {
                                if (data.statementPdfFile != null) {
                                    StatementShareHelper.sharePdfOnly(context, data.statementPdfFile)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "শুধু PDF" else "PDF Only", fontSize = 12.sp)
                        }

                        // Share Text Only
                        OutlinedButton(
                            onClick = {
                                StatementShareHelper.shareCaptionOnly(context, captionText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "শুধু টেক্সট" else "Text Only", fontSize = 12.sp)
                        }
                    }

                    // Copy & Other Apps Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                StatementShareHelper.copyToClipboard(context, captionText)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBangla) "ক্যাপশন কপি" else "Copy Text", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                StatementShareHelper.shareToAnyApp(context, data.statementPdfFile, captionText)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBangla) "অন্যান্য অ্যাপ" else "Other Apps", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // PDF Preview Modal
    if (showPdfPreviewDialog && data.statementPdfFile != null) {
        PdfViewerDialog(
            pdfFile = data.statementPdfFile,
            reportTitle = "${data.customerName} Statement",
            isBangla = isBangla,
            onDismiss = { showPdfPreviewDialog = false }
        )
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextButtonRow(onClick: () -> Unit, text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = PrimaryPurple,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}
