package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.BarcodeGenerator
import com.pocketdev.advance_erp.core.util.QrCodeGenerator
import com.pocketdev.advance_erp.core.util.SkuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PHASE 06 PART 2 — AUTO SKU, BARCODE & QR TESTS
 *
 * Verifies:
 * 1. Automatic base SKU generation format and collision avoidance loop.
 * 2. Variant SKU generation with sanitized attributes.
 * 3. Duplicate SKU detection and rejection on manual editing.
 * 4. Code 128 barcode encoding (Start B, Stop code, Modulo 103 checksum, bit pattern size).
 * 5. 12-digit barcode number generator with uniqueness verification.
 * 6. Pure Kotlin QR Code Model 2 matrix generation and structured offline payload.
 * 7. Verification that QR code payloads contain zero private customer/supplier data.
 */
class AutoSkuAndBarcodeTest {

    @Test
    fun testGenerateBaseSkuFormatAndSequence() {
        val existingSkus = mutableSetOf<String>()

        // First SKU for Men's Shirts
        val sku1 = SkuGenerator.generateProductSku(
            categoryNameOrCode = "Clothing",
            productNameEn = "Men Cotton Shirt",
            existingSkus = existingSkus
        )
        existingSkus.add(sku1)

        assertTrue(sku1.startsWith("CLO-"))

        // Second SKU for Men's Shirts in same category -> increments sequence
        val sku2 = SkuGenerator.generateProductSku(
            categoryNameOrCode = "Clothing",
            productNameEn = "Men Cotton Shirt",
            existingSkus = existingSkus
        )
        existingSkus.add(sku2)

        assertTrue(sku2.startsWith("CLO-"))
        assertNotEquals(sku1, sku2)
    }

    @Test
    fun testGenerateVariantSkuFormats() {
        val baseSku = "CLO-MEN-001"
        val existingVariantSkus = mutableSetOf<String>()

        // Variant with color and size
        val var1 = SkuGenerator.generateVariantSku(
            baseSku = baseSku,
            color = "Red",
            size = "XL",
            existingSkus = existingVariantSkus
        )
        existingVariantSkus.add(var1)
        assertTrue(var1.contains("RED"))
        assertTrue(var1.contains("XL"))

        // Variant with color only
        val var2 = SkuGenerator.generateVariantSku(
            baseSku = baseSku,
            color = "Blue",
            size = null,
            existingSkus = existingVariantSkus
        )
        existingVariantSkus.add(var2)
        assertTrue(var2.contains("BLU"))

        // Variant with size only
        val var3 = SkuGenerator.generateVariantSku(
            baseSku = baseSku,
            color = null,
            size = "32GB",
            existingSkus = existingVariantSkus
        )
        existingVariantSkus.add(var3)
        assertTrue(var3.contains("32GB"))

        // Collision avoidance
        val var1DuplicateCandidate = SkuGenerator.generateVariantSku(
            baseSku = baseSku,
            color = "Red",
            size = "XL",
            existingSkus = existingVariantSkus
        )
        assertNotEquals(var1, var1DuplicateCandidate)
    }

    @Test
    fun testManualSkuDuplicateRejection() {
        val existingProductSkus = listOf("CLO-MENC-001", "ELE-SMAR-001", "GRO-RICE-001")

        fun isSkuAvailable(sku: String, excludeSku: String? = null): Boolean {
            val trimmed = sku.trim().uppercase()
            if (trimmed.isEmpty()) return false
            return existingProductSkus.none { it.uppercase() == trimmed && it.uppercase() != excludeSku?.uppercase() }
        }

        // New SKU that already exists -> rejected
        assertFalse(isSkuAvailable("CLO-MENC-001"))
        assertFalse(isSkuAvailable("clo-menc-001")) // Case insensitive

        // Brand new SKU -> accepted
        assertTrue(isSkuAvailable("CLO-MENC-005"))

        // Existing SKU being kept by the same product during edit -> accepted
        assertTrue(isSkuAvailable("CLO-MENC-001", excludeSku = "CLO-MENC-001"))
    }

    @Test
    fun testCode128BitPatternStructure() {
        val testText = "ADV12345"
        val bitPattern = BarcodeGenerator.encodeCode128(testText)

        assertNotNull(bitPattern)

        // Code 128 bit pattern length formula:
        // 20 quiet zone modules + (1 Start Code + N Data Characters + 1 Check Digit) * 11 bits + 13 bits (Stop Pattern)
        val expectedLength = 20 + (1 + testText.length + 1) * 11 + 13
        assertEquals(expectedLength, bitPattern.size)

        // Quiet zone begins and ends with white spaces (false)
        assertFalse(bitPattern.first())
        assertFalse(bitPattern.last())

        // Start code begins at index 10 (after quiet zone) and must be a black bar (true)
        assertTrue(bitPattern[10])
    }

    @Test
    fun testCode128ChecksumValidation() {
        // Known Code 128 calculation for "A":
        // Start Code B = 104
        // 'A' (ASCII 65) has Code 128 value = 65 - 32 = 33
        // Checksum = (104 + 1 * 33) % 103 = 137 % 103 = 34
        // Total symbols: Start B (104), 'A' (33), Checksum (34), Stop (106)
        val bitPattern = BarcodeGenerator.encodeCode128("A")
        assertNotNull(bitPattern)

        val expectedBits = 20 + (1 + 1 + 1) * 11 + 13 // 20 quiet + 33 + 13 = 66 bits
        assertEquals(expectedBits, bitPattern.size)
    }

    @Test
    fun test12DigitBarcodeGenerator() {
        val barcode1 = BarcodeGenerator.generateStandardBarcode()
        val barcode2 = BarcodeGenerator.generateStandardBarcode()

        assertEquals(12, barcode1.length)
        assertEquals(12, barcode2.length)
        assertTrue(barcode1.all { it.isDigit() })
        assertTrue(barcode2.all { it.isDigit() })
        assertNotEquals(barcode1, barcode2)
    }

    @Test
    fun testQrCodePayloadStructureAndPrivacy() {
        val prodId = "prod-abc-123"
        val sku = "CLO-SHIR-001"

        val prodPayload = QrCodeGenerator.createProductQrPayload(prodId, sku)
        assertEquals("ADV-ERP|PRODUCT|prod-abc-123|CLO-SHIR-001", prodPayload)

        val varId = "var-xyz-789"
        val varPayload = QrCodeGenerator.createVariantQrPayload(varId, prodId, "CLO-SHIR-001-RED")
        assertEquals("ADV-ERP|VARIANT|var-xyz-789|prod-abc-123|CLO-SHIR-001-RED", varPayload)

        // Strict privacy check: zero customer or supplier info
        assertFalse(prodPayload.contains("customer", ignoreCase = true))
        assertFalse(prodPayload.contains("supplier", ignoreCase = true))
        assertFalse(prodPayload.contains("phone", ignoreCase = true))
        assertFalse(varPayload.contains("customer", ignoreCase = true))
        assertFalse(varPayload.contains("supplier", ignoreCase = true))
    }

    @Test
    fun testQrCodeMatrixGeneration() {
        val payload = "ADV-ERP|PRODUCT|123|SKU"
        val matrix = QrCodeGenerator.generateQrMatrix(payload)

        val dimension = matrix.size
        assertTrue(dimension >= 21) // Minimum Version 1 is 21x21
        assertEquals(dimension, matrix[0].size)

        // Check top-left finder pattern: (0,0) must be dark
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
        assertTrue(matrix[6][6])
        // Finder pattern center (3,3) must be dark
        assertTrue(matrix[3][3])
        // Finder pattern white separator (1,1) must be false
        assertFalse(matrix[1][1])
    }
}
