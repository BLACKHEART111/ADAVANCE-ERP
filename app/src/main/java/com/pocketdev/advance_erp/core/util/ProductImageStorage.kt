package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val imagesDir: File by lazy {
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * Copies and optimizes an image from [sourceUri] into app-managed storage.
     * Normalizes orientation, scales to max 1280px, and returns the persistent local file path.
     */
    suspend fun saveImageFromUri(sourceUri: Uri, isThumbnail: Boolean = false): String = withContext(Dispatchers.IO) {
        val maxDimension = if (isThumbnail) 256 else 1280
        val quality = if (isThumbnail) 80 else 85
        val prefix = if (isThumbnail) "thumb_" else "img_"
        val fileName = "$prefix${UUID.randomUUID()}.jpg"
        val destFile = File(imagesDir, fileName)

        // Read orientation from ExifInterface
        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val exif = ExifInterface(input)
                rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            // Fallback to 0 degrees if orientation reading fails
        }

        // Decode bounds to calculate sample size
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        var sampleSize = 1
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        if (origWidth > maxDimension || origHeight > maxDimension) {
            val halfWidth = origWidth / 2
            val halfHeight = origHeight / 2
            while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        // Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap: Bitmap? = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }

        if (bitmap == null) {
            throw IllegalArgumentException("Unable to decode image from uri: $sourceUri")
        }

        // Rotate if needed
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        // Scale to exact max dimension if still larger
        val curW = bitmap.width
        val curH = bitmap.height
        if (curW > maxDimension || curH > maxDimension) {
            val ratio = minOf(maxDimension.toFloat() / curW, maxDimension.toFloat() / curH)
            val targetW = (curW * ratio).toInt()
            val targetH = (curH * ratio).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        // Write to destination JPEG file
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()

        destFile.absolutePath
    }

    /**
     * Checks if a stored image file exists on disk.
     */
    fun doesImageExist(pathOrUri: String?): Boolean {
        if (pathOrUri.isNullOrBlank()) return false
        val cleanPath = pathOrUri.removePrefix("file://")
        return File(cleanPath).exists()
    }

    /**
     * Safely deletes an image file from storage.
     */
    suspend fun deleteImageFile(pathOrUri: String?) = withContext(Dispatchers.IO) {
        if (!pathOrUri.isNullOrBlank()) {
            val cleanPath = pathOrUri.removePrefix("file://")
            val file = File(cleanPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
