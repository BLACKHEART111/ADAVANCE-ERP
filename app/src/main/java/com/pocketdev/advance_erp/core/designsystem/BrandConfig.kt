package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pocketdev.advance_erp.R

/**
 * ADVANCE ERP — Official Brand Configuration & Design Identity
 * Centralized theme, assets, typography, and logo components.
 */
object BrandConfig {

    // Official Brand Palette
    val RoyalPurple = Color(0xFF792B87)
    val Teal = Color(0xFF299DB2)
    val DeepNavy = Color(0xFF263849)
    val Background = Color(0xFFF2F5F7)
    val White = Color(0xFFFFFFFF)

    // Secondary Accent & Surface Gradients
    val RoyalPurpleDark = Color(0xFF5B1F67)
    val RoyalPurpleLight = Color(0xFF9944A9)
    val TealDark = Color(0xFF1B6E7D)
    val TealLight = Color(0xFF4DBFD4)
    val NavyDark = Color(0xFF1B2835)
    val NavySurface = Color(0xFF1E2D3D)

    // Brand Metadata
    const val BRAND_NAME = "ADVANCE ERP"
    const val BRAND_VERSION = "1.0.0"
    const val TAGLINE_EN = "Import Business Management System"
    const val TAGLINE_BN = "আমদানি ব্যবসা ব্যবস্থাপনা পদ্ধতি"
    const val DEFAULT_CURRENCY = "BDT"
    const val COPYRIGHT = "© 2026 ADVANCE ERP. All rights reserved."

    // Brand Drawables
    val DrawableLogoHorizontal = R.drawable.logo_horizontal
    val DrawableLogoHorizontalWhite = R.drawable.logo_horizontal_white
    val DrawableLogoCompact = R.drawable.logo_compact
    val DrawableLogoCompactWhite = R.drawable.logo_compact_white
    val DrawableLogoSymbol = R.drawable.logo_symbol
    val DrawableLogoMonochromeDark = R.drawable.logo_monochrome_dark
    val DrawableLogoMonochromeWhite = R.drawable.logo_monochrome_white
    val DrawableLogoSymbolMonochromeDark = R.drawable.logo_symbol_monochrome_dark
    val DrawableLogoSymbolMonochromeWhite = R.drawable.logo_symbol_monochrome_white
    val DrawableAppIconBadge = R.drawable.app_icon_badge
    val DrawableAppIconSymbol = R.drawable.app_icon_symbol

    enum class LogoVariant {
        HORIZONTAL,
        COMPACT,
        SYMBOL,
        MONOCHROME
    }
}

/**
 * Reusable Compose Component for rendering the official ADVANCE ERP logo.
 * Automatically adapts between light and dark backgrounds.
 */
@Composable
fun AdvanceErpLogo(
    modifier: Modifier = Modifier,
    variant: BrandConfig.LogoVariant = BrandConfig.LogoVariant.COMPACT,
    isDarkBackground: Boolean = isSystemInDarkTheme(),
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = "ADVANCE ERP Logo"
) {
    val resId = when (variant) {
        BrandConfig.LogoVariant.HORIZONTAL -> {
            if (isDarkBackground) BrandConfig.DrawableLogoHorizontalWhite else BrandConfig.DrawableLogoHorizontal
        }
        BrandConfig.LogoVariant.COMPACT -> {
            if (isDarkBackground) BrandConfig.DrawableLogoCompactWhite else BrandConfig.DrawableLogoCompact
        }
        BrandConfig.LogoVariant.SYMBOL -> {
            BrandConfig.DrawableLogoSymbol
        }
        BrandConfig.LogoVariant.MONOCHROME -> {
            if (isDarkBackground) BrandConfig.DrawableLogoMonochromeWhite else BrandConfig.DrawableLogoMonochromeDark
        }
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
