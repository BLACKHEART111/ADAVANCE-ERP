package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ERPButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    GHOST
}

@Composable
fun ERPActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ERPButtonVariant = ERPButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val shape = RoundedCornerShape(12.dp)

    when (variant) {
        ERPButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryPurple.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                contentPadding = contentPadding
            ) {
                ButtonInnerContent(text, icon, isLoading, Color.White)
            }
        }
        ERPButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryTeal,
                    contentColor = Color.White,
                    disabledContainerColor = SecondaryTeal.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                contentPadding = contentPadding
            ) {
                ButtonInnerContent(text, icon, isLoading, Color.White)
            }
        }
        ERPButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled && !isLoading,
                shape = shape,
                border = BorderStroke(1.5.dp, if (enabled) PrimaryPurple else ERPDivider),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryPurple,
                    disabledContentColor = TextMuted
                ),
                contentPadding = contentPadding
            ) {
                ButtonInnerContent(text, icon, isLoading, PrimaryPurple)
            }
        }
        ERPButtonVariant.GHOST -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary
                ),
                contentPadding = contentPadding,
                elevation = null
            ) {
                ButtonInnerContent(text, icon, isLoading, TextPrimary)
            }
        }
    }
}

@Composable
private fun ButtonInnerContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = tint,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false
        )
    }
}
