package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

data class ProductLabelData(
    val productName: String,
    val sku: String,
    val barcode: String,
    val qrPayload: String,
    val variantDescription: String? = null,
    val priceMinor: Long,
    val companyName: String = "ADVANCE ERP"
)

enum class LabelSheetFormat(val displayName: String, val cols: Int, val rows: Int) {
    THERMAL_50X30("Thermal Roll 50x30mm", 1, 1),
    A4_24_LABELS("A4 Sticker Sheet (24 Labels - 3x8)", 3, 8),
    A4_30_LABELS("A4 Sticker Sheet (30 Labels - 3x10)", 3, 10)
}

object LabelPrintingEngine {

    // 50mm x 30mm at 72dpi: 142 x 85 points
    private const val THERMAL_WIDTH = 142
    private const val THERMAL_HEIGHT = 85

    // Standard A4 at 72dpi: 595 x 842 points
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    /**
     * Generates a multi-page PDF for thermal roll printers (50x30mm) with [count] labels.
     */
    fun generateThermalRollPdf(
        context: Context,
        labelData: ProductLabelData,
        count: Int = 1
    ): File {
        val document = PdfDocument()

        val barcodeBitmap = try {
            BarcodeGenerator.generateBarcodeBitmap(
                content = labelData.barcode,
                width = 560,
                height = 220,
                includeText = true
            )
        } catch (e: Exception) {
            null
        }

        val qrBitmap = try {
            QrCodeGenerator.generateQrBitmap(
                content = labelData.qrPayload,
                targetSize = 220
            )
        } catch (e: Exception) {
            null
        }

        for (pageIndex in 1..count) {
            val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_WIDTH, THERMAL_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Draw single label content inside thermal bounds
            drawSingleLabel(
                canvas = canvas,
                x = 4f,
                y = 4f,
                w = (THERMAL_WIDTH - 8).toFloat(),
                h = (THERMAL_HEIGHT - 8).toFloat(),
                data = labelData,
                barcodeBitmap = barcodeBitmap,
                qrBitmap = qrBitmap,
                isThermal = true
            )

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "labels").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Label_${labelData.sku}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()

        return file
    }

    /**
     * Generates an A4 sticker sheet PDF (grid of 24 or 30 labels) repeated [count] times.
     */
    fun generateA4StickerSheetPdf(
        context: Context,
        labelData: ProductLabelData,
        format: LabelSheetFormat = LabelSheetFormat.A4_24_LABELS,
        totalLabelsToPrint: Int = 24
    ): File {
        val document = PdfDocument()
        val labelsPerPage = format.cols * format.rows
        val totalPages = (totalLabelsToPrint + labelsPerPage - 1) / labelsPerPage

        val barcodeBitmap = try {
            BarcodeGenerator.generateBarcodeBitmap(
                content = labelData.barcode,
                width = 560,
                height = 220,
                includeText = true
            )
        } catch (e: Exception) {
            null
        }

        val qrBitmap = try {
            QrCodeGenerator.generateQrBitmap(
                content = labelData.qrPayload,
                targetSize = 220
            )
        } catch (e: Exception) {
            null
        }

        val marginX = 20f
        val marginY = 24f
        val availableW = A4_WIDTH - (marginX * 2)
        val availableH = A4_HEIGHT - (marginY * 2)

        val cellW = availableW / format.cols
        val cellH = availableH / format.rows

        var labelsDrawn = 0

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            for (r in 0 until format.rows) {
                for (c in 0 until format.cols) {
                    if (labelsDrawn >= totalLabelsToPrint) break

                    val left = marginX + (c * cellW) + 2f
                    val top = marginY + (r * cellH) + 2f
                    val w = cellW - 4f
                    val h = cellH - 4f

                    // Draw outer sticker border guide
                    val borderPaint = Paint().apply {
                        color = Color.parseColor("#E2E8F0")
                        style = Paint.Style.STROKE
                        strokeWidth = 0.5f
                    }
                    canvas.drawRoundRect(RectF(left, top, left + w, top + h), 4f, 4f, borderPaint)

                    drawSingleLabel(
                        canvas = canvas,
                        x = left + 4f,
                        y = top + 4f,
                        w = w - 8f,
                        h = h - 8f,
                        data = labelData,
                        barcodeBitmap = barcodeBitmap,
                        qrBitmap = qrBitmap,
                        isThermal = false
                    )

                    labelsDrawn++
                }
            }

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "labels").apply { if (!exists()) mkdirs() }
        val file = File(dir, "A4_Labels_${labelData.sku}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()

        return file
    }

    private fun drawSingleLabel(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        data: ProductLabelData,
        barcodeBitmap: Bitmap?,
        qrBitmap: Bitmap?,
        isThermal: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Company Banner
        paint.color = Color.parseColor("#263849")
        paint.textSize = if (isThermal) 6.5f else 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.companyName.take(22), x, y + 8f, paint)

        // Price Badge on right
        paint.color = Color.parseColor("#792B87") // Royal Purple
        paint.textSize = if (isThermal) 7.5f else 8.5f
        paint.textAlign = Paint.Align.RIGHT
        val priceText = "MRP: ৳" + Formatters.formatMinorUnit(data.priceMinor)
        canvas.drawText(priceText, x + w, y + 8f, paint)

        // Product Name
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.textSize = if (isThermal) 7.5f else 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var nameToDisplay = data.productName
        while (paint.measureText(nameToDisplay) > w && nameToDisplay.length > 6) {
            nameToDisplay = nameToDisplay.dropLast(2) + ".."
        }
        canvas.drawText(nameToDisplay, x, y + 18f, paint)

        // Variant info or SKU (auto-fit font size so SKU is not cut off)
        paint.typeface = Typeface.DEFAULT
        var subTextSize = if (isThermal) 6f else 6.5f
        paint.textSize = subTextSize
        paint.color = Color.DKGRAY
        val subLine = data.variantDescription?.let { "$it | SKU: ${data.sku}" } ?: "SKU: ${data.sku}"
        val subWidth = paint.measureText(subLine)
        if (subWidth > w && subWidth > 0f) {
            subTextSize = (subTextSize * (w / subWidth)).coerceAtLeast(4.5f)
            paint.textSize = subTextSize
        }
        canvas.drawText(subLine, x, y + 26.5f, paint)

        // Visuals Area: Barcode on Left, QR Code on Right
        val visualsTop = y + 29.5f
        val availableVisualH = (h - 29.5f).coerceAtLeast(32f)
        val qrSize = availableVisualH.coerceAtMost(if (isThermal) 42f else 46f)
        paint.isFilterBitmap = true

        if (qrBitmap != null) {
            val qrTop = visualsTop + (availableVisualH - qrSize) / 2f
            val qrDest = RectF(x + w - qrSize, qrTop, x + w, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, null, qrDest, paint)
        }

        val barcodeW = if (qrBitmap != null) w - qrSize - 5f else w
        val barcodeH = availableVisualH.coerceAtMost(if (isThermal) 44f else 48f)
        if (barcodeBitmap != null) {
            val barcodeTop = visualsTop + (availableVisualH - barcodeH) / 2f
            val barcodeDest = RectF(x, barcodeTop, x + barcodeW, barcodeTop + barcodeH)
            canvas.drawBitmap(barcodeBitmap, null, barcodeDest, paint)
        } else {
            // Text fallback
            paint.textSize = 7f
            paint.color = Color.BLACK
            canvas.drawText("*${data.barcode}*", x + 4f, visualsTop + 18f, paint)
        }
    }
}
