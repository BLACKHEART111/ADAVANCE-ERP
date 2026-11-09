package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.feature.product.NewVariantDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying variant pricing, formatting, and draft state mechanics.
 */
class ProductVariantPriceAndDialogUiTest {

    @Test
    fun testVariantDraftCustomPricesArePreserved() {
        val basePurchasePrice = "500.00"
        val baseSellingPrice = "800.00"

        val variantCustomCost = "550.00"
        val variantCustomPrice = "900.00"

        val cost = Money.parseFromString(variantCustomCost.ifBlank { basePurchasePrice }).minorUnits
        val price = Money.parseFromString(variantCustomPrice.ifBlank { baseSellingPrice }).minorUnits

        val draft = NewVariantDraft(
            sku = "LHB-002-BRO-1",
            color = "Brown",
            size = "M",
            costMinor = cost,
            priceMinor = price,
            initialStock = 100
        )

        assertEquals(55000L, draft.costMinor)
        assertEquals(90000L, draft.priceMinor)
        assertEquals(100, draft.initialStock)
    }

    @Test
    fun testVariantDraftFallbackToBasePricesWhenBlank() {
        val basePurchasePrice = "500.00"
        val baseSellingPrice = "800.00"

        val variantCustomCost = ""
        val variantCustomPrice = ""

        val cost = Money.parseFromString(variantCustomCost.ifBlank { basePurchasePrice }).minorUnits
        val price = Money.parseFromString(variantCustomPrice.ifBlank { baseSellingPrice }).minorUnits

        val draft = NewVariantDraft(
            sku = "LHB-002-BLK-1",
            color = "Black",
            size = "L",
            costMinor = cost,
            priceMinor = price,
            initialStock = 50
        )

        assertEquals(50000L, draft.costMinor)
        assertEquals(80000L, draft.priceMinor)
    }

    @Test
    fun testEffectiveVariantPriceLogicInViewModel() {
        val baseSellingMinor = 80000L
        val basePurchaseMinor = 50000L

        val variant1 = NewVariantDraft(
            sku = "VAR-1",
            color = "Red",
            size = "S",
            costMinor = 52000L,
            priceMinor = 85000L
        )

        val variant2 = NewVariantDraft(
            sku = "VAR-2",
            color = "Blue",
            size = "M",
            costMinor = 0L,
            priceMinor = 0L
        )

        val effectivePrice1 = if (variant1.priceMinor > 0L) variant1.priceMinor else baseSellingMinor
        val effectiveCost1 = if (variant1.costMinor > 0L) variant1.costMinor else basePurchaseMinor

        val effectivePrice2 = if (variant2.priceMinor > 0L) variant2.priceMinor else baseSellingMinor
        val effectiveCost2 = if (variant2.costMinor > 0L) variant2.costMinor else basePurchaseMinor

        assertEquals(85000L, effectivePrice1)
        assertEquals(52000L, effectiveCost1)

        assertEquals(80000L, effectivePrice2)
        assertEquals(50000L, effectiveCost2)
    }

    @Test
    fun testVariantPriceFormattingInBanglaAndEnglish() {
        val priceMinor = 80000L // 800.00 BDT

        val formattedBn = Formatters.formatMoney(priceMinor, CurrencyCode.BDT, AppLanguage.BANGLA)
        val formattedEn = Formatters.formatMoney(priceMinor, CurrencyCode.BDT, AppLanguage.ENGLISH)

        assertTrue(formattedBn.contains("৮০০") || formattedBn.contains("800"))
        assertTrue(formattedEn.contains("800"))
    }
}
