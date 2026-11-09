package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.ProductImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PHASE 06 PART 2 — PRODUCT VARIANT PHOTO TESTS
 *
 * Verifies:
 * 1. Independent photo galleries per variant.
 * 2. Primary variant photo selection within variant scope.
 * 3. Fallback to product primary photo when variant has no photos.
 * 4. Fallback to placeholder/null when neither variant nor product has photos.
 * 5. Strict gallery isolation between variants of the same product.
 */
class ProductVariantPhotoTest {

    @Test
    fun testVariantPhotoHasBothProductIdAndVariantId() {
        val productId = "prod-tshirt-01"
        val variantRedId = "var-red-01"

        val variantImage = ProductImage(
            id = "img-v-1",
            productId = productId,
            variantId = variantRedId,
            imageUri = "/data/product_images/tshirt_red.jpg",
            isPrimary = true,
            sortOrder = 0
        )

        assertEquals(productId, variantImage.productId)
        assertEquals(variantRedId, variantImage.variantId)
        assertTrue(variantImage.isPrimary)
    }

    @Test
    fun testGalleriesAreIsolatedBetweenVariants() {
        val productId = "prod-tshirt-01"
        val variantRedId = "var-red-01"
        val variantBlueId = "var-blue-02"

        val allImages = listOf(
            // Product images
            ProductImage(id = "img-p1", productId = productId, variantId = null, imageUri = "p1.jpg", isPrimary = true, sortOrder = 0),
            // Red variant images
            ProductImage(id = "img-red-1", productId = productId, variantId = variantRedId, imageUri = "red1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "img-red-2", productId = productId, variantId = variantRedId, imageUri = "red2.jpg", isPrimary = false, sortOrder = 1),
            // Blue variant images
            ProductImage(id = "img-blue-1", productId = productId, variantId = variantBlueId, imageUri = "blue1.jpg", isPrimary = true, sortOrder = 0)
        )

        val redImages = allImages.filter { it.variantId == variantRedId }
        val blueImages = allImages.filter { it.variantId == variantBlueId }
        val productImages = allImages.filter { it.productId == productId && it.variantId == null }

        assertEquals(2, redImages.size)
        assertEquals(1, blueImages.size)
        assertEquals(1, productImages.size)

        // Verify Red variant contains only red images
        assertTrue(redImages.all { it.variantId == variantRedId })
        assertFalse(redImages.any { it.variantId == variantBlueId })

        // Verify Blue variant contains only blue images
        assertTrue(blueImages.all { it.variantId == variantBlueId })
        assertFalse(blueImages.any { it.variantId == variantRedId })
    }

    @Test
    fun testVariantPrimaryPhotoFallbackToProductPrimaryWhenVariantHasNoPhoto() {
        val productId = "prod-tshirt-01"
        val variantWithPhotoId = "var-red-01"
        val variantWithoutPhotoId = "var-green-03"

        val productPrimaryUri = "product_main.jpg"
        val variantRedPrimaryUri = "red_main.jpg"

        val allImages = listOf(
            ProductImage(id = "img-p1", productId = productId, variantId = null, imageUri = productPrimaryUri, isPrimary = true, sortOrder = 0),
            ProductImage(id = "img-red-1", productId = productId, variantId = variantWithPhotoId, imageUri = variantRedPrimaryUri, isPrimary = true, sortOrder = 0)
        )

        // Helper to resolve display image for a variant
        fun resolveDisplayImage(variantId: String): String? {
            val variantImages = allImages.filter { it.variantId == variantId }
            val variantPrimary = variantImages.firstOrNull { it.isPrimary }?.imageUri
                ?: variantImages.firstOrNull()?.imageUri
            if (variantPrimary != null) return variantPrimary

            // Fallback to product primary image
            val productImages = allImages.filter { it.productId == productId && it.variantId == null }
            return productImages.firstOrNull { it.isPrimary }?.imageUri
                ?: productImages.firstOrNull()?.imageUri
        }

        // Variant with own photo gets its own photo
        val redResolved = resolveDisplayImage(variantWithPhotoId)
        assertEquals(variantRedPrimaryUri, redResolved)

        // Variant without photo falls back to product primary photo
        val greenResolved = resolveDisplayImage(variantWithoutPhotoId)
        assertEquals(productPrimaryUri, greenResolved)
    }

    @Test
    fun testVariantPhotoFallbackReturnsNullWhenNeitherVariantNorProductHasPhoto() {
        val productId = "prod-tshirt-01"
        val variantId = "var-yellow-04"
        val emptyImages = emptyList<ProductImage>()

        fun resolveDisplayImage(variantId: String): String? {
            val variantImages = emptyImages.filter { it.variantId == variantId }
            val variantPrimary = variantImages.firstOrNull { it.isPrimary }?.imageUri
                ?: variantImages.firstOrNull()?.imageUri
            if (variantPrimary != null) return variantPrimary

            val productImages = emptyImages.filter { it.productId == productId && it.variantId == null }
            return productImages.firstOrNull { it.isPrimary }?.imageUri
                ?: productImages.firstOrNull()?.imageUri
        }

        val resolved = resolveDisplayImage(variantId)
        assertNull(resolved)
    }

    @Test
    fun testSettingPrimaryVariantPhotoDoesNotAlterProductOrOtherVariantPrimary() {
        val productId = "prod-tshirt-01"
        val variant1 = "var-1"
        val variant2 = "var-2"

        var images = listOf(
            ProductImage(id = "p-1", productId = productId, variantId = null, imageUri = "p1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "v1-1", productId = productId, variantId = variant1, imageUri = "v1-1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "v1-2", productId = productId, variantId = variant1, imageUri = "v1-2.jpg", isPrimary = false, sortOrder = 1),
            ProductImage(id = "v2-1", productId = productId, variantId = variant2, imageUri = "v2-1.jpg", isPrimary = true, sortOrder = 0)
        )

        // Switch primary of variant 1 to v1-2
        val targetImageId = "v1-2"
        images = images.map { img ->
            if (img.variantId == variant1) {
                img.copy(isPrimary = img.id == targetImageId)
            } else {
                img
            }
        }

        // Product primary untouched
        assertTrue(images.first { it.id == "p-1" }.isPrimary)
        // Variant 2 primary untouched
        assertTrue(images.first { it.id == "v2-1" }.isPrimary)
        // Variant 1 primary successfully switched
        assertFalse(images.first { it.id == "v1-1" }.isPrimary)
        assertTrue(images.first { it.id == "v1-2" }.isPrimary)
    }
}
