package com.pocketdev.advance_erp.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpLogo
import com.pocketdev.advance_erp.core.designsystem.BrandConfig
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.BiometricHelper
import com.pocketdev.advance_erp.core.security.PinSecurityHelper

@Composable
fun AppLockScreen(
    storedPinHash: String?,
    storedPinSalt: String?,
    isBiometricEnabled: Boolean,
    language: AppLanguage = AppLanguage.BANGLA,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val isBn = language == AppLanguage.BANGLA

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun verifyAndUnlock() {
        if (enteredPin.length in 4..8) {
            val isMatch = PinSecurityHelper.verifyPin(enteredPin, storedPinHash, storedPinSalt)
            if (isMatch) {
                errorMessage = null
                onUnlocked()
            } else {
                errorMessage = if (isBn) "ভুল পিন কোড! পুনরায় চেষ্টা করুন।" else "Incorrect PIN code! Please try again."
                enteredPin = ""
            }
        } else {
            errorMessage = if (isBn) "পিন কোড ৪ থেকে ৮ ডিজিট হতে হবে।" else "PIN must be 4 to 8 digits."
        }
    }

    // Auto-prompt biometric authentication when enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && context is FragmentActivity) {
            BiometricHelper.showBiometricPrompt(
                activity = context,
                title = if (isBn) "ফিঙ্গারপ্রিন্ট আনলক" else "Biometric Unlock",
                subtitle = if (isBn) "Advance ERP আনলক করতে আঙুলের ছাপ দিন" else "Touch fingerprint sensor to unlock Advance ERP",
                negativeButtonText = if (isBn) "পিন ব্যবহার করুন" else "Use PIN",
                onAuthenticated = { onUnlocked() },
                onError = { err -> errorMessage = err }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Brand & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                AdvanceErpLogo(
                    variant = BrandConfig.LogoVariant.HORIZONTAL,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isBn) "অ্যাপ নিরাপত্তা লক" else "App Security Lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isBn) "আপনার ৪ থেকে ৮ ডিজিটের পিন কোড দিন" else "Enter your 4 to 8 digit security PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN dots indicators (up to 8 dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDots = maxOf(4, enteredPin.length.coerceAtMost(8))
                    for (i in 0 until maxDots) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) SecondaryTeal else Color.White.copy(alpha = 0.2f))
                                .border(1.5.dp, if (isFilled) SecondaryTeal else Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Middle & Bottom: Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rows 1-3, 4-6, 7-9
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (digit in row) {
                            KeypadButton(
                                text = digit,
                                onClick = {
                                    if (enteredPin.length < 8) {
                                        enteredPin += digit
                                        errorMessage = null
                                    }
                                }
                            )
                        }
                    }
                }

                // Row 4: Biometric / Fingerprint Button, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biometric button (if supported & enabled)
                    if (isBiometricEnabled && context is FragmentActivity) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SecondaryTeal.copy(alpha = 0.15f))
                                .clickable {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = context,
                                        title = if (isBn) "ফিঙ্গারপ্রিন্ট আনলক" else "Biometric Unlock",
                                        subtitle = if (isBn) "Advance ERP আনলক করতে আঙুলের ছাপ দিন" else "Touch fingerprint sensor to unlock Advance ERP",
                                        negativeButtonText = if (isBn) "পিন ব্যবহার করুন" else "Use PIN",
                                        onAuthenticated = { onUnlocked() },
                                        onError = { err -> errorMessage = err }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = SecondaryTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(64.dp))
                    }

                    // Digit 0
                    KeypadButton(
                        text = "0",
                        onClick = {
                            if (enteredPin.length < 8) {
                                enteredPin += "0"
                                errorMessage = null
                            }
                        }
                    )

                    // Backspace button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                    errorMessage = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unlock Action Button (active when pin length >= 4)
                Button(
                    onClick = { verifyAndUnlock() },
                    enabled = enteredPin.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = if (isBn) "আনলক করুন (Unlock)" else "Unlock App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (enteredPin.length >= 4) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
