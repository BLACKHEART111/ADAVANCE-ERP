package com.pocketdev.advance_erp.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = ERPSurface,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = OnPrimaryPurpleContainer,
    secondary = SecondaryTeal,
    onSecondary = ERPSurface,
    secondaryContainer = SecondaryTealContainer,
    onSecondaryContainer = OnSecondaryTealContainer,
    tertiary = DeepNavy,
    onTertiary = ERPSurface,
    tertiaryContainer = ERPSurfaceVariant,
    onTertiaryContainer = DeepNavyDark,
    background = ERPBackground,
    onBackground = TextPrimary,
    surface = ERPSurface,
    onSurface = TextPrimary,
    surfaceVariant = ERPSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ERPDivider,
    error = StatusError,
    onError = ERPSurface,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurpleLight,
    onPrimary = DeepNavyDark,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = PrimaryPurpleContainer,
    secondary = SecondaryTealLight,
    onSecondary = DeepNavyDark,
    secondaryContainer = SecondaryTealDark,
    onSecondaryContainer = SecondaryTealContainer,
    tertiary = ERPSurfaceVariant,
    onTertiary = DeepNavyDark,
    background = DeepNavyDark,
    onBackground = TextOnDark,
    surface = DeepNavy,
    onSurface = TextOnDark,
    surfaceVariant = DeepNavySurface,
    onSurfaceVariant = TextMuted,
    outline = DeepNavyLight,
    error = StatusError,
    onError = ERPSurface
)

@Composable
fun AdvanceErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = (if (darkTheme) DeepNavyDark else DeepNavy).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ERPTypography,
        shapes = ERPShapes,
        content = content
    )
}
