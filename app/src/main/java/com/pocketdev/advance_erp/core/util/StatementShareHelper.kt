package com.pocketdev.advance_erp.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object StatementShareHelper {

    fun shareToWhatsApp(
        context: Context,
        pdfFile: File?,
        caption: String,
        phone: String? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (pdfFile != null && pdfFile.exists()) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, caption)
                // Prefer standard WhatsApp or WhatsApp Business
                setPackage("com.whatsapp")
            }

            // Verify if WhatsApp is installed
            val packageManager = context.packageManager
            val resolved = intent.resolveActivity(packageManager)
            if (resolved != null) {
                context.startActivity(intent)
            } else {
                // Try WhatsApp Business package
                intent.setPackage("com.whatsapp.w4b")
                if (intent.resolveActivity(packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback to system chooser
                    intent.setPackage(null)
                    val chooser = Intent.createChooser(intent, "Share via (WhatsApp not found)")
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Direct system share fallback
            shareToAnyApp(context, pdfFile, caption)
        }
    }

    fun sharePdfOnly(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Statement PDF"))
    }

    fun shareCaptionOnly(context: Context, caption: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "Customer Payment Receipt & Statement")
        }
        context.startActivity(Intent.createChooser(intent, "Share Caption via"))
    }

    fun shareToAnyApp(context: Context, pdfFile: File?, caption: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (pdfFile != null && pdfFile.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, caption)
        }
        context.startActivity(Intent.createChooser(intent, "Share Statement via"))
    }

    fun copyToClipboard(context: Context, text: String, label: String = "WhatsApp Caption") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Caption copied to clipboard! (ক্যাপশন কপি করা হয়েছে)", Toast.LENGTH_SHORT).show()
    }
}
