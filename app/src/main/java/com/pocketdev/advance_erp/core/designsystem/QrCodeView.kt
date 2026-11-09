package com.pocketdev.advance_erp.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.util.QrCodeGenerator

@Composable
fun QrCodeView(
    payload: String,
    modifier: Modifier = Modifier,
    qrSizeDp: Int = 180,
    showActions: Boolean = true
) {
    val context = LocalContext.current
    val sanitized = payload.trim().ifBlank { "ADV-ERP" }

    val matrix = remember(sanitized) {
        QrCodeGenerator.generateQrMatrix(sanitized)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR Code Canvas with 4 module quiet zone
        val matrixSize = matrix.size
        val quietZone = 3
        val totalModules = matrixSize + quietZone * 2

        Box(
            modifier = Modifier
                .size(qrSizeDp.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(qrSizeDp.dp)) {
                val moduleSize = size.width / totalModules.toFloat()

                for (r in 0 until matrixSize) {
                    for (c in 0 until matrixSize) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset((c + quietZone) * moduleSize, (r + quietZone) * moduleSize),
                                size = Size(moduleSize + 0.3f, moduleSize + 0.3f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Truncated payload text
        Text(
            text = sanitized,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = TextMuted,
            maxLines = 1
        )

        // Actions: Copy & Share
        if (showActions) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QR Code", sanitized)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "QR Code data copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy QR Data",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, sanitized)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share QR Code")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share QR Code",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
