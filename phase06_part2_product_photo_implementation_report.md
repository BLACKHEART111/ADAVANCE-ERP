# ADVANCE ERP — Phase 06 Part 2: Product Photo & Variant Media Implementation Report

**Date:** November 3, 2026  
**Module:** Product & Inventory Media Management  
**Project Workspace:** `/workspace/advance-erp`  
**Package:** `com.pocketdev.advance_erp`  
**Database Schema Version:** `7`  

---

## 1. Executive Summary

Phase 06 Part 2 introduces enterprise-grade media and physical item identification capabilities to ADVANCE ERP. A complete, offline-first Product Photo and Variant Gallery subsystem was designed and implemented without adding unverified third-party libraries. 

Key achievements delivered:
- **Product Photo Gallery**: Full multi-photo lifecycle support per product with primary photo designation, sequential reordering, deletion with confirmation, and fullscreen preview.
- **Product Variant Photos**: Dedicated gallery per variant with independent primary photo selection and intelligent fallback to the product's primary image or a vector placeholder.
- **Internal Storage Architecture**: All images are safely managed in app-private internal storage (`context.filesDir/product_images/`), completely isolated from external file tampering.
- **Image Normalization Engine**: Automatic EXIF orientation normalization, thumbnail generation (256px), and downsampling (max dimension 1280px at 85% JPEG quality) to prevent Out-Of-Memory (OOM) errors during rapid inventory browsing.
- **UI Integration**: Smooth thumbnails across Product List, Stock Overview, Low Stock alerts, Add Product dialog, Edit Product dialog, and the comprehensive Product Detail dialog.

---

## 2. Architecture & Storage Design

### 2.1 File Storage Location
Images are stored locally within the application sandbox:
```text
/data/user/0/com.pocketdev.advance_erp/files/product_images/
├── prod_<productId>_<uuid>.jpg      (Optimized main image, max 1280px)
└── prod_<productId>_<uuid>_thumb.jpg (Thumbnail, max 256px)
```
- **Privacy & Security**: Files reside in app-internal storage, inaccessible to other applications.
- **Automatic Lifecycle Cleanup**: Deleting an image in the UI removes both the Room database record and the underlying storage files from disk via `ProductImageStorage.deleteImageFile()`.

### 2.2 Database Schema: `product_images`
The `product_images` table in Room schema version 7 manages photo metadata:

| Column | SQLite Type | Constraints / Description |
|:---|:---|:---|
| `id` | `TEXT` | Primary Key (`UUID`) |
| `productId` | `TEXT` | Foreign Key → `products(id)` ON DELETE CASCADE |
| `variantId` | `TEXT` | Nullable Foreign Key → `product_variants(id)` ON DELETE CASCADE |
| `imageUri` | `TEXT` | Absolute file path or content URI |
| `isPrimary` | `INTEGER` | Boolean flag (1 = Primary, 0 = Secondary) |
| `sortOrder` | `INTEGER` | Ascending sequence number for UI ordering |
| `createdAt` | `INTEGER` | Millisecond epoch timestamp |

**Indices:**
- `index_product_images_productId` ON `product_images(productId)`
- `index_product_images_variantId` ON `product_images(variantId)`

---

## 3. Image Optimization & EXIF Normalization

### 3.1 EXIF Orientation Correction
Images captured by mobile cameras typically store physical sensor orientation in EXIF tags (e.g. `ORIENTATION_ROTATE_90`, `ORIENTATION_ROTATE_180`, `ORIENTATION_ROTATE_270`). `ProductImageStorage.kt` uses the standard Android platform `android.media.ExifInterface` (available since API 24+, minSdk 28):
```kotlin
val exif = ExifInterface(inputStream)
val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
val matrix = Matrix()
when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
}
```

### 3.2 Downsampling & Compression
- **Full Image**: Scaled down so that the maximum dimension does not exceed 1280 pixels, compressed at 85% JPEG quality.
- **Thumbnail Image**: Scaled to max 256 pixels for instant lazy-list loading in Compose cards.
- **Garbage Collection Safety**: Bitmaps are recycled immediately after disk serialization.

---

## 4. Multi-Photo Gallery & UI Controls

### 4.1 Reusable Component: `PhotoGalleryPicker`
Implemented in `feature/product/PhotoGalleryPicker.kt`:
1. **Multi-Selection**: Leverages Android `ActivityResultContracts.GetMultipleContents` or single camera captures.
2. **Primary Badge**: Visual "PRIMARY" chip on the designated hero photo with a star icon.
3. **Set as Primary Action**: Long-press or click badge to designate any photo as primary; automatically demotes previous primary photo.
4. **Drag & Reorder**: Reorder photos with immediate visual feedback, updating `sortOrder` sequentially.
5. **Delete Confirmation Dialog**: Deletion requires explicit user confirmation via an AlertDialog, preventing accidental media loss.
6. **Fullscreen Zoom Preview**: Tapping a photo opens `FullScreenImageDialog` with pan, pinch-to-zoom, and high-resolution rendering.

### 4.2 Variant Photo Isolation & Fallback
Implemented in `ProductDetailDialog.kt` and `StockBalanceDao.kt`:
- **Independent Variant Galleries**: Product variants (e.g., Red / Size XL vs Blue / Size M) have distinct galleries (`variantId == v.id`).
- **3-Tier Fallback Hierarchy**:
  1. **Variant Primary Photo**: If the variant has photos, its primary photo is displayed.
  2. **Product Primary Photo**: If the variant has no photos, the product's primary photo is displayed.
  3. **Default Vector Placeholder**: If neither the variant nor the parent product has photos, a stylized `Inventory2` placeholder icon is displayed.

---

## 5. Automated Verification Results

All photo gallery and variant photo features are thoroughly tested in:
1. [`ProductPhotoAndGalleryTest.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/ProductPhotoAndGalleryTest.kt):
   - `testAddMultiplePhotosToProduct`: PASSED
   - `testSetPrimaryPhotoUpdatesOnlyTargetAndClearsPrevious`: PASSED
   - `testReorderPhotosUpdatesSortOrderSequentially`: PASSED
   - `testDeletePrimaryPhotoReassignsNextAvailablePhotoAsPrimary`: PASSED
   - `testDeleteAllPhotosLeavesEmptyGallery`: PASSED
2. [`ProductVariantPhotoTest.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/ProductVariantPhotoTest.kt):
   - `testVariantPhotoHasBothProductIdAndVariantId`: PASSED
   - `testGalleriesAreIsolatedBetweenVariants`: PASSED
   - `testVariantPrimaryPhotoFallbackToProductPrimaryWhenVariantHasNoPhoto`: PASSED
   - `testVariantPhotoFallbackReturnsNullWhenNeitherVariantNorProductHasPhoto`: PASSED
   - `testSettingPrimaryVariantPhotoDoesNotAlterProductOrOtherVariantPrimary`: PASSED

---

## 6. Deliverable Sign-Off

- **Source Code Locations**:
  - `core/util/ProductImageStorage.kt`
  - `data/entity/ProductImage.kt`
  - `data/dao/ProductImageDao.kt`
  - `feature/product/PhotoGalleryPicker.kt`
  - `core/designsystem/FullScreenImageDialog.kt`
  - `feature/product/ProductDetailDialog.kt`
- **Status**: Production-Ready, 100% Passing Tests, Debug APK Assembled.
