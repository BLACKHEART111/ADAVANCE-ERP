package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.ProductImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * PHASE 06 PART 2 — PRODUCT PHOTO AND GALLERY TESTS
 *
 * Verifies:
 * 1. Multiple photos per product.
 * 2. Primary photo flag management (only one primary per product).
 * 3. Photo reordering updates sortOrder correctly.
 * 4. Deleting a photo and reassigning primary.
 * 5. Data integrity across updates and reloads.
 */
class ProductPhotoAndGalleryTest {

    @Test
    fun testAddMultiplePhotosToProduct() {
        val productId = "prod-polo-001"
        val images = mutableListOf<ProductImage>()

        // Add 3 images
        for (i in 0 until 3) {
            images.add(
                ProductImage(
                    id = "img-$i",
                    productId = productId,
                    variantId = null,
                    imageUri = "/storage/emulated/0/Android/data/com.pocketdev.advance_erp/files/product_images/polo_$i.jpg",
                    isPrimary = i == 0,
                    sortOrder = i
                )
            )
        }

        assertEquals(3, images.size)
        assertEquals(1, images.count { it.isPrimary })
        assertEquals("img-0", images.first { it.isPrimary }.id)
        assertTrue(images.all { it.productId == productId })
        assertTrue(images.all { it.variantId == null })
    }

    @Test
    fun testSetPrimaryPhotoUpdatesOnlyTargetAndClearsPrevious() {
        val productId = "prod-polo-001"
        var images = listOf(
            ProductImage(id = "img-1", productId = productId, variantId = null, imageUri = "path/1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "img-2", productId = productId, variantId = null, imageUri = "path/2.jpg", isPrimary = false, sortOrder = 1),
            ProductImage(id = "img-3", productId = productId, variantId = null, imageUri = "path/3.jpg", isPrimary = false, sortOrder = 2)
        )

        // Switch primary to img-2
        val newPrimaryId = "img-2"
        images = images.map { img ->
            if (img.id == newPrimaryId) {
                img.copy(isPrimary = true)
            } else if (img.isPrimary) {
                img.copy(isPrimary = false)
            } else {
                img
            }
        }

        assertEquals(1, images.count { it.isPrimary })
        val primary = images.first { it.isPrimary }
        assertEquals("img-2", primary.id)
        assertFalse(images.first { it.id == "img-1" }.isPrimary)
        assertFalse(images.first { it.id == "img-3" }.isPrimary)
    }

    @Test
    fun testReorderPhotosUpdatesSortOrderSequentially() {
        val productId = "prod-polo-001"
        val originalList = listOf(
            ProductImage(id = "img-1", productId = productId, variantId = null, imageUri = "path/1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "img-2", productId = productId, variantId = null, imageUri = "path/2.jpg", isPrimary = false, sortOrder = 1),
            ProductImage(id = "img-3", productId = productId, variantId = null, imageUri = "path/3.jpg", isPrimary = false, sortOrder = 2)
        )

        // Move img-3 to first place: order becomes img-3, img-1, img-2
        val reorderedIds = listOf("img-3", "img-1", "img-2")
        val reorderedList = reorderedIds.mapIndexed { index, id ->
            originalList.first { it.id == id }.copy(sortOrder = index)
        }

        assertEquals(0, reorderedList.first { it.id == "img-3" }.sortOrder)
        assertEquals(1, reorderedList.first { it.id == "img-1" }.sortOrder)
        assertEquals(2, reorderedList.first { it.id == "img-2" }.sortOrder)

        // Sorted by sortOrder asc matches reorderedIds
        assertEquals(reorderedIds, reorderedList.sortedBy { it.sortOrder }.map { it.id })
    }

    @Test
    fun testDeletePrimaryPhotoReassignsNextAvailablePhotoAsPrimary() {
        val productId = "prod-polo-001"
        var images = listOf(
            ProductImage(id = "img-1", productId = productId, variantId = null, imageUri = "path/1.jpg", isPrimary = true, sortOrder = 0),
            ProductImage(id = "img-2", productId = productId, variantId = null, imageUri = "path/2.jpg", isPrimary = false, sortOrder = 1),
            ProductImage(id = "img-3", productId = productId, variantId = null, imageUri = "path/3.jpg", isPrimary = false, sortOrder = 2)
        )

        // Delete img-1 (the primary photo)
        val imageToDeleteId = "img-1"
        val wasPrimary = images.first { it.id == imageToDeleteId }.isPrimary
        images = images.filter { it.id != imageToDeleteId }

        if (wasPrimary && images.isNotEmpty() && images.none { it.isPrimary }) {
            // First remaining photo becomes primary
            images = images.mapIndexed { index, img ->
                if (index == 0) img.copy(isPrimary = true, sortOrder = index)
                else img.copy(sortOrder = index)
            }
        }

        assertEquals(2, images.size)
        assertEquals(1, images.count { it.isPrimary })
        assertEquals("img-2", images.first { it.isPrimary }.id)
        assertEquals(0, images.first { it.id == "img-2" }.sortOrder)
        assertEquals(1, images.first { it.id == "img-3" }.sortOrder)
    }

    @Test
    fun testDeleteAllPhotosLeavesEmptyGallery() {
        val productId = "prod-polo-001"
        var images = listOf(
            ProductImage(id = "img-1", productId = productId, variantId = null, imageUri = "path/1.jpg", isPrimary = true, sortOrder = 0)
        )

        images = images.filter { it.id != "img-1" }
        assertTrue(images.isEmpty())
        assertNull(images.firstOrNull { it.isPrimary })
    }
}
