package com.pocketdev.advance_erp.feature.settings.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.BackupRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupManagementSection(
    backupRecords: List<BackupRecord>,
    isFridayAutoBackupEnabled: Boolean,
    lastAutoBackupTime: Long?,
    language: AppLanguage,
    onCreateBackup: (onFinished: () -> Unit) -> Unit,
    onRestoreBackupFile: (file: File) -> Unit,
    onRestoreFromUri: (uri: Uri) -> Unit,
    onShareBackup: (file: File) -> Unit,
    onDeleteBackup: (recordId: String) -> Unit,
    onToggleFridayAutoBackup: (enabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isBn = language == AppLanguage.BANGLA
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    var isCreatingBackup by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupRecord?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupRecord?>(null) }
    var pendingUriToRestore by remember { mutableStateOf<Uri?>(null) }

    // File Picker Launcher for Restoring .zip
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUriToRestore = uri
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Actions Card: Instant Backup + File Restore + Friday Auto Backup
        ERPCard {
            Column(modifier = Modifier.padding(4.dp)) {
                // Instant Backup Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "তাত্ক্ষণিক সম্পূর্ণ ব্যাকআপ (.zip)" else "Instant Full Backup (.zip)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBn) "সকল পণ্যের ছবি, প্রোফাইল ও ডাটাবেজ date/mm/yy ফরম্যাটে সেভ হবে"
                            else "Bundles all photos, SQLite database & audit logs in date/mm/yy format",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            isCreatingBackup = true
                            onCreateBackup {
                                isCreatingBackup = false
                                Toast.makeText(
                                    context,
                                    if (isBn) "সম্পূর্ণ ব্যাকআপ ফাইল তৈরি হয়েছে!" else "Full ZIP backup created!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isCreatingBackup,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isCreatingBackup) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBn) "ব্যাকআপ নিন" else "Create Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                // Pick from Storage Button
                OutlinedButton(
                    onClick = {
                        try {
                            zipPickerLauncher.launch("application/zip")
                        } catch (e: Exception) {
                            zipPickerLauncher.launch("*/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "ডিভাইস থেকে ব্যাকআপ ফাইল রিস্টোর করুন (.zip)" else "Choose Backup ZIP from Storage to Restore",
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryTeal,
                        fontSize = 12.5.sp
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ERPDivider)

                // Friday Auto-Backup Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = null,
                            tint = if (isFridayAutoBackupEnabled) StatusSuccess else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "প্রতি শুক্রবার স্বয়ংক্রিয় ব্যাকআপ" else "Every Friday Auto-Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isFridayAutoBackupEnabled) {
                                    if (isBn) "সক্রিয় • প্রতি শুক্রবার রাত ১০:০০ টায় ছবিসহ ব্যাকআপ তৈরি হবে"
                                    else "Active • Auto creates full ZIP backup every Friday at 10 PM"
                                } else {
                                    if (isBn) "নিষ্ক্রিয়" else "Disabled"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFridayAutoBackupEnabled) StatusSuccess else TextMuted
                            )
                            if (lastAutoBackupTime != null && lastAutoBackupTime > 0L) {
                                val timeStr = dateFormat.format(Date(lastAutoBackupTime))
                                Text(
                                    text = "${if (isBn) "সর্বশেষ ব্যাকআপ: " else "Last backup: "}${if (isBn) Formatters.toBanglaDigits(timeStr) else timeStr}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isFridayAutoBackupEnabled,
                        onCheckedChange = { onToggleFridayAutoBackup(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusSuccess
                        )
                    )
                }
            }
        }

        // Previous Backups History List
        if (backupRecords.isNotEmpty()) {
            ERPCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isBn) "পূর্ববর্তী ব্যাকআপের তালিকা (${backupRecords.size})" else "Backup History (${backupRecords.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    HorizontalDivider(color = ERPDivider)

                    backupRecords.take(8).forEach { bak ->
                        val file = File(bak.filePath)
                        val sizeKb = bak.fileSizeBytes / 1024
                        val sizeStr = if (sizeKb > 1024) "%.1f MB".format(sizeKb / 1024.0) else "$sizeKb KB"
                        val dateFormatted = dateFormat.format(Date(bak.createdAt))
                        val displayDate = if (isBn) Formatters.toBanglaDigits(dateFormatted) else dateFormatted

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bak.fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = displayDate,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextMuted
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextMuted
                                            )
                                            Text(
                                                text = sizeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryPurple
                                            )
                                            if (bak.isAutomatic) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (isBn) "স্বয়ংক্রিয়" else "Auto",
                                                        fontSize = 9.sp,
                                                        color = StatusSuccess,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Share button
                                    IconButton(
                                        onClick = {
                                            if (file.exists()) {
                                                onShareBackup(file)
                                            } else {
                                                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Restore button
                                    IconButton(
                                        onClick = { backupToRestore = bak },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { backupToDelete = bak },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Restoring from History
    backupToRestore?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ ফাইল রিস্টোর নিশ্চিতকরণ" else "Confirm Database Restore",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn) "আপনি কি নিশ্চিত যে '${bak.fileName}' ফাইল থেকে ডাটা রিস্টোর করতে চান?\n\nরিস্টোরের পূর্বে বর্তমান ডাটার একটি সেফটি কপি তৈরি করা হবে এবং ব্যাকআপে থাকা সকল ছবি ও ডাটা পুনরুদ্ধার হবে।"
                    else "Are you sure you want to restore from '${bak.fileName}'?\n\nA safety backup of your active data will be created before restoring."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(bak.filePath)
                        onRestoreBackupFile(file)
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBn) "হ্যাঁ, রিস্টোর করুন" else "Yes, Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Restoring from picked Uri
    pendingUriToRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUriToRestore = null },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SecondaryTeal) },
            title = {
                Text(
                    text = if (isBn) "নির্বাচিত জিপ ফাইল রিস্টোর করুন" else "Restore Selected ZIP File",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn) "ডিভাইস থেকে নির্বাচিত ফাইলটি যাচাই করে ছবি ও ডাটা রিস্টোর করা হবে। আপনি কি এগিয়ে যেতে চান?"
                    else "The selected ZIP archive will be verified and restored. Proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreFromUri(uri)
                        pendingUriToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBn) "রিস্টোর শুরু করুন" else "Start Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUriToRestore = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Deleting
    backupToDelete?.let { bak ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ ফাইল মুছবেন?" else "Delete Backup File?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn) "আপনি কি '${bak.fileName}' ব্যাকআপ ফাইলটি স্থায়ীভাবে মুছে ফেলতে চান?"
                    else "Are you sure you want to permanently delete '${bak.fileName}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBackup(bak.id)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
