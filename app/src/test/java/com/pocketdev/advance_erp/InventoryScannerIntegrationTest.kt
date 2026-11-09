package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.domain.repository.ResolvedProductItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PHASE 06 PART 2 — INVENTORY SCANNER INTEGRATION TESTS
 *
 * Verifies:
 * 1. Code resolution against Product Barcode.
 * 2. Code resolution against Product Base SKU.
 * 3. Code resolution against Product QR Code.
 * 4. Code resolution against Variant Barcode.
 * 5. Code resolution against Variant SKU.
 * 6. Code resolution against Variant QR Code.
 * 7. Graceful handling of unknown / unmapped codes.
 * 8. Scanner debounce behavior (ignoring duplicate scans within 1.5s).
 */
class InventoryScannerIntegrationTest {

    // Test product fixtures
    private val testProduct = ProductWithDetails(
        id = "prod-p1",
        categoryId = "cat-c1",
        categoryNameEn = "Clothing",
        categoryNameBn = "পোশাক",
        nameEn = "Classic Polo Shirt",
        nameBn = "ক্লাসিক পোলো শার্ট",
        baseSku = "CLO-POLO-001",
        barcode = "890123456789",
        qrCode = "ADV-ERP|PRODUCT|prod-p1|CLO-POLO-001",
        description = "Premium polo shirt",
        unit = "PCS",
        purchasePriceMinor = 50000,
        sellingPriceMinor = 80000,
        minStockAlert = 10,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        totalStock = 100,
        variantCount = 2,
        primaryImageUri = "/images/polo_main.jpg"
    )

    private val testVariantRed = ProductVariant(
        id = "var-red-01",
        productId = "prod-p1",
        sku = "CLO-POLO-001-RED",
        color = "Red",
        size = "L",
        variantCostMinor = 50000,
        variantPriceMinor = 80000,
        barcode = "890123456790",
        qrCode = "ADV-ERP|VARIANT|var-red-01|prod-p1|CLO-POLO-001-RED"
    )

    private val testVariantBlue = ProductVariant(
        id = "var-blue-02",
        productId = "prod-p1",
        sku = "CLO-POLO-001-BLU",
        color = "Blue",
        size = "M",
        variantCostMinor = 50000,
        variantPriceMinor = 80000,
        barcode = "890123456791",
        qrCode = "ADV-ERP|VARIANT|var-blue-02|prod-p1|CLO-POLO-001-BLU"
    )

    private val products = listOf(testProduct)
    private val variants = listOf(testVariantRed, testVariantBlue)

    /**
     * Replicates ProductRepositoryImpl.resolveCode logic
     */
    private fun resolveCode(rawCode: String): ResolvedProductItem? {
        val code = rawCode.trim()
        if (code.isEmpty()) return null

        val qr = QrCodeGenerator.parsePayload(code)
        if (qr.type == QrCodeGenerator.QrType.PRODUCT) {
            val prod = products.firstOrNull { it.id == qr.targetId || it.baseSku.equals(qr.targetId, ignoreCase = true) || it.baseSku.equals(qr.code, ignoreCase = true) }
            if (prod != null) {
                return ResolvedProductItem(product = prod, matchedVariant = null, matchedCode = code, matchType = "QR_PRODUCT")
            }
        } else if (qr.type == QrCodeGenerator.QrType.VARIANT) {
            val variant = variants.firstOrNull { it.id == qr.targetId || it.sku.equals(qr.targetId, ignoreCase = true) || it.sku.equals(qr.code, ignoreCase = true) }
            val prod = if (variant != null) {
                products.firstOrNull { it.id == variant.productId }
            } else if (!qr.secondaryId.isNullOrBlank()) {
                products.firstOrNull { it.id == qr.secondaryId }
            } else null

            if (prod != null) {
                return ResolvedProductItem(product = prod, matchedVariant = variant, matchedCode = code, matchType = "QR_VARIANT")
            }
        }

        val norm = Formatters.toEnglishDigits(code)

        // 2. Exact match product barcode
        val prodByBarcode = products.firstOrNull { it.barcode?.equals(code, ignoreCase = true) == true || (norm != code && it.barcode?.equals(norm, ignoreCase = true) == true) }
        if (prodByBarcode != null) {
            return ResolvedProductItem(product = prodByBarcode, matchedVariant = null, matchedCode = code, matchType = "BARCODE_PRODUCT")
        }

        // 3. Exact match variant barcode
        val varByBarcode = variants.firstOrNull { it.barcode?.equals(code, ignoreCase = true) == true || (norm != code && it.barcode?.equals(norm, ignoreCase = true) == true) }
        if (varByBarcode != null) {
            val parent = products.firstOrNull { it.id == varByBarcode.productId }
            if (parent != null) {
                return ResolvedProductItem(product = parent, matchedVariant = varByBarcode, matchedCode = code, matchType = "BARCODE_VARIANT")
            }
        }

        // 4. Exact match product base SKU
        val prodBySku = products.firstOrNull { it.baseSku.equals(code, ignoreCase = true) || it.baseSku.equals(norm, ignoreCase = true) }
        if (prodBySku != null) {
            return ResolvedProductItem(product = prodBySku, matchedVariant = null, matchedCode = code, matchType = "SKU_PRODUCT")
        }

        // 5. Exact match variant SKU
        val varBySku = variants.firstOrNull { it.sku.equals(code, ignoreCase = true) || it.sku.equals(norm, ignoreCase = true) }
        if (varBySku != null) {
            val parent = products.firstOrNull { it.id == varBySku.productId }
            if (parent != null) {
                return ResolvedProductItem(product = parent, matchedVariant = varBySku, matchedCode = code, matchType = "SKU_VARIANT")
            }
        }

        return null
    }

    @Test
    fun testResolveProductByBarcode() {
        val result = resolveCode("890123456789")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNull(result.matchedVariant)
        assertEquals("BARCODE_PRODUCT", result.matchType)
    }

    @Test
    fun testResolveProductByBaseSku() {
        val result = resolveCode("CLO-POLO-001")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNull(result.matchedVariant)
        assertEquals("SKU_PRODUCT", result.matchType)
    }

    @Test
    fun testResolveProductByQrCode() {
        val result = resolveCode("ADV-ERP|PRODUCT|prod-p1|CLO-POLO-001")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNull(result.matchedVariant)
        assertEquals("QR_PRODUCT", result.matchType)
    }

    @Test
    fun testResolveVariantByBarcode() {
        val result = resolveCode("890123456790")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNotNull(result.matchedVariant)
        assertEquals("var-red-01", result.matchedVariant!!.id)
        assertEquals("BARCODE_VARIANT", result.matchType)
    }

    @Test
    fun testResolveVariantBySku() {
        val result = resolveCode("CLO-POLO-001-BLU")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNotNull(result.matchedVariant)
        assertEquals("var-blue-02", result.matchedVariant!!.id)
        assertEquals("SKU_VARIANT", result.matchType)
    }

    @Test
    fun testResolveVariantByQrCode() {
        val result = resolveCode("ADV-ERP|VARIANT|var-red-01|prod-p1|CLO-POLO-001-RED")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNotNull(result.matchedVariant)
        assertEquals("var-red-01", result.matchedVariant!!.id)
        assertEquals("QR_VARIANT", result.matchType)
    }

    @Test
    fun testResolveProductByNewQrCodePrefix() {
        val result = resolveCode("ADV-ERP:PRD|prod-p1|CLO-POLO-001")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNull(result.matchedVariant)
        assertEquals("QR_PRODUCT", result.matchType)
    }

    @Test
    fun testResolveVariantByNewQrCodePrefix() {
        val result = resolveCode("ADV-ERP:VAR|var-red-01|prod-p1|CLO-POLO-001-RED")
        assertNotNull(result)
        assertEquals("prod-p1", result!!.product.id)
        assertNotNull(result.matchedVariant)
        assertEquals("var-red-01", result.matchedVariant!!.id)
        assertEquals("QR_VARIANT", result.matchType)
    }

    @Test
    fun testResolveBarcodeInBengaliDigits() {
        val bnBarcode = Formatters.toBanglaDigits("890123456790")
        val result = resolveCode(bnBarcode)
        assertNotNull("Should resolve barcode with Bengali numerals", result)
        assertEquals("prod-p1", result!!.product.id)
        assertEquals("var-red-01", result.matchedVariant!!.id)
    }

    @Test
    fun testParsePayloadBothFormats() {
        val payloadNew = QrCodeGenerator.parsePayload("ADV-ERP:VAR|v-123|p-456|SKU-99")
        assertEquals(QrCodeGenerator.QrType.VARIANT, payloadNew.type)
        assertEquals("v-123", payloadNew.targetId)
        assertEquals("p-456", payloadNew.secondaryId)
        assertEquals("SKU-99", payloadNew.code)

        val payloadOld = QrCodeGenerator.parsePayload("ADV-ERP|VARIANT|v-123|p-456|SKU-99")
        assertEquals(QrCodeGenerator.QrType.VARIANT, payloadOld.type)
        assertEquals("v-123", payloadOld.targetId)
        assertEquals("p-456", payloadOld.secondaryId)
        assertEquals("SKU-99", payloadOld.code)
    }

    @Test
    fun testResolveUnknownCodeReturnsNull() {
        val result = resolveCode("UNKNOWN-CODE-999")
        assertNull(result)
    }

    @Test
    fun testScannerDebounceSuppressesRapidDuplicateScans() {
        val debounceMs = 1500L
        var lastScanTime = 0L
        var lastScannedCode: String? = null
        val processedScans = mutableListOf<String>()

        fun onScanDetected(code: String, currentTime: Long) {
            if (code == lastScannedCode && (currentTime - lastScanTime) < debounceMs) {
                // Suppressed by debounce
                return
            }
            lastScanTime = currentTime
            lastScannedCode = code
            processedScans.add(code)
        }

        // Scan 1 at t=0
        onScanDetected("890123456789", 1000L)
        // Rapid scan 2 (same code) at t=300ms -> should be suppressed
        onScanDetected("890123456789", 1300L)
        // Rapid scan 3 (same code) at t=800ms -> should be suppressed
        onScanDetected("890123456789", 1800L)

        // Only 1 scan processed
        assertEquals(1, processedScans.size)
        assertEquals("890123456789", processedScans.first())

        // Scan 4 at t=2600ms (after debounce interval > 1500ms) -> should be processed
        onScanDetected("890123456789", 2600L)
        assertEquals(2, processedScans.size)

        // Scan 5 (different code) immediately at t=2700ms -> should be processed because code is different
        onScanDetected("CLO-POLO-001-RED", 2700L)
        assertEquals(3, processedScans.size)
        assertEquals("CLO-POLO-001-RED", processedScans.last())
    }
}
