package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AppMediaStorage {

    /**
     * Copies and optimizes a company logo from [sourceUri] into app storage.
     * Returns the persistent absolute path of the saved logo image.
     */
    suspend fun saveCompanyLogo(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "company_logo")
        if (!dir.exists()) dir.mkdirs()

        val destFile = File(dir, "company_logo_${System.currentTimeMillis()}.png")
        copyAndOptimizeImage(context, sourceUri, destFile, maxDimension = 800, isPng = true)
        destFile.absolutePath
    }

    /**
     * Copies and optimizes a customer profile photo from [sourceUri] into app storage.
     * Returns the persistent absolute path of the saved customer photo.
     */
    suspend fun saveCustomerPhoto(context: Context, sourceUri: Uri, customerId: String = UUID.randomUUID().toString()): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "customer_photos")
        if (!dir.exists()) dir.mkdirs()

        val destFile = File(dir, "cust_${customerId}_${System.currentTimeMillis()}.jpg")
        copyAndOptimizeImage(context, sourceUri, destFile, maxDimension = 600, isPng = false)
        destFile.absolutePath
    }

    private fun copyAndOptimizeImage(
        context: Context,
        sourceUri: Uri,
        destFile: File,
        maxDimension: Int,
        isPng: Boolean
    ) {
        // Read EXIF rotation
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
        } catch (_: Exception) {}

        // Decode bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
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

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalArgumentException("Cannot decode image from uri: $sourceUri")

        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        FileOutputStream(destFile).use { out ->
            if (isPng) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }
        bitmap.recycle()
    }
}
