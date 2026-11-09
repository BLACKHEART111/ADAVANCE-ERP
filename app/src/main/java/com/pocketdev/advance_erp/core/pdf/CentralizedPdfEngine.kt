package com.pocketdev.advance_erp.core.pdf

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfReportColumn(
    val titleEn: String,
    val titleBn: String? = null,
    val widthWeight: Float = 1.0f,
    val isRightAligned: Boolean = false
)

data class PdfReportRow(
    val cells: List<String>,
    val isSummary: Boolean = false,
    val isEmphasized: Boolean = false
)

data class PdfReportSection(
    val titleEn: String? = null,
    val titleBn: String? = null,
    val columns: List<PdfReportColumn>,
    val rows: List<PdfReportRow>,
    val summaryRows: List<PdfReportRow> = emptyList()
)

data class PdfKpiBadge(
    val labelEn: String,
    val labelBn: String? = null,
    val value: String
)

data class CentralizedPdfReport(
    val reportId: String,
    val category: String, // e.g. "Sales", "Purchase", "Inventory", "Accounts", "Dashboard"
    val titleEn: String,
    val titleBn: String,
    val dateRangeText: String? = null,
    val filterText: String? = null,
    val kpiBadges: List<PdfKpiBadge> = emptyList(),
    val sections: List<PdfReportSection>,
    val additionalNotes: String? = null,
    val isBangla: Boolean = false
)

object CentralizedPdfEngine {

    const val PAGE_WIDTH = 595 // Standard A4 in points at 72 dpi
    const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val FOOTER_HEIGHT = 45f

    private const val COLOR_NAVY = "#263849"
    private const val COLOR_PURPLE = "#792B87"
    private const val COLOR_TEAL = "#299DB2"
    private const val COLOR_BG_ALT = "#F8FAFC"
    private const val COLOR_TEXT_MAIN = "#1E293B"
    private const val COLOR_TEXT_MUTED = "#64748B"
    private const val COLOR_DIVIDER = "#E2E8F0"

    private const val ROW_HEIGHT = 20f
    private const val HEADER_ROW_HEIGHT = 24f

    fun generatePdf(
        context: Context,
        report: CentralizedPdfReport,
        businessProfile: BusinessProfile?
    ): File {
        val totalPages = calculateTotalPages(report)
        val document = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val generatedAtText = "Generated: ${dateFormat.format(Date())}"

        var currentPageIndex = 1
        var currentSectionIdx = 0
        var currentRowIdx = 0
        var currentSummaryIdx = 0

        while (currentPageIndex <= totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val isFirstPage = (currentPageIndex == 1)
            var currentY = drawHeader(context, canvas, paint, report, businessProfile, isFirstPage)

            // Content Area limit
            val maxY = PAGE_HEIGHT - FOOTER_HEIGHT - 10f

            // Draw sections and rows
            while (currentSectionIdx < report.sections.size) {
                val section = report.sections[currentSectionIdx]

                // If starting section, draw section title if present
                if (currentRowIdx == 0 && currentSummaryIdx == 0 && (!section.titleEn.isNullOrBlank() || !section.titleBn.isNullOrBlank())) {
                    if (currentY + 24f > maxY) break
                    val secTitle = if (report.isBangla && !section.titleBn.isNullOrBlank()) section.titleBn else (section.titleEn ?: "")
                    paint.color = Color.parseColor(COLOR_NAVY)
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(secTitle, MARGIN_X, currentY + 14f, paint)
                    currentY += 22f
                }

                // Table Column Header (Repeats on each new page)
                if (currentY + HEADER_ROW_HEIGHT > maxY) break
                currentY = drawTableHeader(canvas, paint, section.columns, currentY, report.isBangla)

                // Draw Data Rows
                var canFitMoreRows = true
                while (currentRowIdx < section.rows.size) {
                    if (currentY + ROW_HEIGHT > maxY) {
                        canFitMoreRows = false
                        break
                    }
                    val row = section.rows[currentRowIdx]
                    val isAlt = (currentRowIdx % 2 == 1)
                    drawTableRow(canvas, paint, section.columns, row, currentY, isAlt)
                    currentY += ROW_HEIGHT
                    currentRowIdx++
                }

                if (!canFitMoreRows) break

                // Draw Summary Rows for this section
                var canFitMoreSummaries = true
                while (currentSummaryIdx < section.summaryRows.size) {
                    if (currentY + ROW_HEIGHT > maxY) {
                        canFitMoreSummaries = false
                        break
                    }
                    val summaryRow = section.summaryRows[currentSummaryIdx]
                    drawSummaryRow(canvas, paint, section.columns, summaryRow, currentY)
                    currentY += ROW_HEIGHT
                    currentSummaryIdx++
                }

                if (!canFitMoreSummaries) break

                // Section finished, move to next
                currentSectionIdx++
                currentRowIdx = 0
                currentSummaryIdx = 0
                currentY += 12f
            }

            // Draw Footer on every page
            drawFooter(canvas, paint, currentPageIndex, totalPages, generatedAtText)

            document.finishPage(page)
            currentPageIndex++
        }

        // Save PDF to cache/reports
        val dir = File(context.cacheDir, "erp_reports")
        if (!dir.exists()) dir.mkdirs()
        val fileName = "${report.reportId}_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.flush()
        fos.close()
        document.close()

        return file
    }

    private fun calculateTotalPages(report: CentralizedPdfReport): Int {
        val firstPageUsableHeight = PAGE_HEIGHT - 120f - (if (report.kpiBadges.isNotEmpty()) 42f else 0f) - FOOTER_HEIGHT
        val subsequentPageUsableHeight = PAGE_HEIGHT - 60f - FOOTER_HEIGHT

        var totalRowsNeeded = 0
        report.sections.forEach { section ->
            if (!section.titleEn.isNullOrBlank() || !section.titleBn.isNullOrBlank()) totalRowsNeeded++
            totalRowsNeeded++ // column header
            totalRowsNeeded += section.rows.size
            totalRowsNeeded += section.summaryRows.size
            totalRowsNeeded++ // spacing
        }

        val rowsOnFirst = (firstPageUsableHeight / ROW_HEIGHT).toInt()
        val rowsOnSubsequent = (subsequentPageUsableHeight / ROW_HEIGHT).toInt()

        if (totalRowsNeeded <= rowsOnFirst) return 1
        val remaining = totalRowsNeeded - rowsOnFirst
        val extraPages = (remaining + rowsOnSubsequent - 1) / rowsOnSubsequent
        return maxOf(1, 1 + extraPages)
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        report: CentralizedPdfReport,
        businessProfile: BusinessProfile?,
        isFirstPage: Boolean
    ): Float {
        val usableWidth = PAGE_WIDTH.toFloat()

        if (isFirstPage) {
            // Full Brand Header Banner
            val bannerHeight = 88f
            paint.color = Color.parseColor(COLOR_NAVY)
            canvas.drawRect(0f, 0f, usableWidth, bannerHeight, paint)

            // Accent Stripes
            paint.color = Color.parseColor(COLOR_PURPLE)
            canvas.drawRect(0f, bannerHeight, usableWidth * 0.5f, bannerHeight + 4f, paint)
            paint.color = Color.parseColor(COLOR_TEAL)
            canvas.drawRect(usableWidth * 0.5f, bannerHeight, usableWidth, bannerHeight + 4f, paint)

            // Company Logo
            try {
                val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_compact_white)
                if (logo != null) {
                    val logoH = 26f
                    val logoW = logoH * (logo.width.toFloat() / logo.height.toFloat())
                    canvas.drawBitmap(logo, null, RectF(MARGIN_X, 12f, MARGIN_X + logoW, 12f + logoH), paint)
                }
            } catch (_: Exception) {}

            // Company Details
            val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(companyName, MARGIN_X, 48f, paint)

            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#E2E8F0")
            val contactLine = "${businessProfile?.address ?: "Tejgaon CA, Dhaka-1208, Bangladesh"}  |  Tel: ${businessProfile?.phone ?: "+880 1700-000000"}  |  BIN: ${businessProfile?.taxNumber ?: "002891928-0101"}"
            canvas.drawText(contactLine, MARGIN_X, 62f, paint)

            // Report Title Badge (Right-aligned)
            paint.color = Color.parseColor(COLOR_TEAL)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = if (report.isBangla) report.titleBn else report.titleEn
            val titleW = paint.measureText(title)
            canvas.drawText(title, usableWidth - MARGIN_X - titleW, 36f, paint)

            paint.color = Color.LTGRAY
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            val catText = "Category: ${report.category}"
            canvas.drawText(catText, usableWidth - MARGIN_X - paint.measureText(catText), 50f, paint)

            var nextY = bannerHeight + 14f

            // Date Range & Filter Criteria sub-banner
            if (!report.dateRangeText.isNullOrBlank() || !report.filterText.isNullOrBlank()) {
                val metaText = listOfNotNull(report.dateRangeText, report.filterText).joinToString("  •  ")
                paint.color = Color.parseColor(COLOR_TEXT_MUTED)
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText(metaText, MARGIN_X, nextY, paint)
                nextY += 14f
            }

            // KPI Badges (if provided)
            if (report.kpiBadges.isNotEmpty()) {
                val badgeCount = report.kpiBadges.size
                val badgeWidth = (usableWidth - (2 * MARGIN_X) - ((badgeCount - 1) * 8f)) / badgeCount
                val badgeHeight = 28f

                report.kpiBadges.forEachIndexed { i, badge ->
                    val bx = MARGIN_X + i * (badgeWidth + 8f)
                    val rect = RectF(bx, nextY, bx + badgeWidth, nextY + badgeHeight)
                    paint.color = Color.parseColor(COLOR_BG_ALT)
                    canvas.drawRoundRect(rect, 4f, 4f, paint)

                    paint.style = Paint.Style.STROKE
                    paint.color = Color.parseColor(COLOR_DIVIDER)
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(rect, 4f, 4f, paint)
                    paint.style = Paint.Style.FILL

                    paint.textSize = 7.5f
                    paint.color = Color.parseColor(COLOR_TEXT_MUTED)
                    paint.typeface = Typeface.DEFAULT
                    val bLabel = if (report.isBangla && badge.labelBn != null) badge.labelBn else badge.labelEn
                    canvas.drawText(bLabel, bx + 6f, nextY + 11f, paint)

                    paint.textSize = 9.5f
                    paint.color = Color.parseColor(COLOR_NAVY)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(badge.value, bx + 6f, nextY + 23f, paint)
                }
                nextY += badgeHeight + 10f
            }

            return nextY
        } else {
            // Subsequent Page Mini-Banner
            paint.color = Color.parseColor(COLOR_NAVY)
            canvas.drawRect(0f, 0f, usableWidth, 28f, paint)

            paint.color = Color.WHITE
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = if (report.isBangla) report.titleBn else report.titleEn
            canvas.drawText(title, MARGIN_X, 18f, paint)

            val companyName = businessProfile?.companyName ?: "ADVANCE ERP"
            paint.color = Color.LTGRAY
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            val rightText = "$companyName  (Continued)"
            canvas.drawText(rightText, usableWidth - MARGIN_X - paint.measureText(rightText), 18f, paint)

            return 40f
        }
    }

    private fun drawTableHeader(
        canvas: Canvas,
        paint: Paint,
        columns: List<PdfReportColumn>,
        y: Float,
        isBangla: Boolean
    ): Float {
        val totalWeight = columns.sumOf { it.widthWeight.toDouble() }.toFloat()
        val tableWidth = PAGE_WIDTH - (2 * MARGIN_X)

        paint.color = Color.parseColor(COLOR_NAVY)
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + HEADER_ROW_HEIGHT, paint)

        var currentX = MARGIN_X
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        columns.forEach { col ->
            val colWidth = (col.widthWeight / totalWeight) * tableWidth
            val text = if (isBangla && col.titleBn != null) col.titleBn else col.titleEn
            val textY = y + 15f

            if (col.isRightAligned) {
                val textW = paint.measureText(text)
                canvas.drawText(text, currentX + colWidth - 6f - textW, textY, paint)
            } else {
                canvas.drawText(text, currentX + 6f, textY, paint)
            }
            currentX += colWidth
        }

        return y + HEADER_ROW_HEIGHT
    }

    private fun drawTableRow(
        canvas: Canvas,
        paint: Paint,
        columns: List<PdfReportColumn>,
        row: PdfReportRow,
        y: Float,
        isAlt: Boolean
    ) {
        val totalWeight = columns.sumOf { it.widthWeight.toDouble() }.toFloat()
        val tableWidth = PAGE_WIDTH - (2 * MARGIN_X)

        if (isAlt) {
            paint.color = Color.parseColor(COLOR_BG_ALT)
            canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + ROW_HEIGHT, paint)
        }

        // Bottom light divider
        paint.color = Color.parseColor(COLOR_DIVIDER)
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_X, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN_X, y + ROW_HEIGHT, paint)

        var currentX = MARGIN_X
        paint.color = Color.parseColor(COLOR_TEXT_MAIN)
        paint.textSize = 8f
        paint.typeface = if (row.isEmphasized) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT

        columns.forEachIndexed { i, col ->
            val colWidth = (col.widthWeight / totalWeight) * tableWidth
            val rawText = row.cells.getOrNull(i) ?: ""
            // Word trim if too long
            val text = trimTextToWidth(paint, rawText, colWidth - 12f)
            val textY = y + 14f

            if (col.isRightAligned) {
                val textW = paint.measureText(text)
                canvas.drawText(text, currentX + colWidth - 6f - textW, textY, paint)
            } else {
                canvas.drawText(text, currentX + 6f, textY, paint)
            }
            currentX += colWidth
        }
    }

    private fun drawSummaryRow(
        canvas: Canvas,
        paint: Paint,
        columns: List<PdfReportColumn>,
        summaryRow: PdfReportRow,
        y: Float
    ) {
        val totalWeight = columns.sumOf { it.widthWeight.toDouble() }.toFloat()
        val tableWidth = PAGE_WIDTH - (2 * MARGIN_X)

        // Summary background
        paint.color = Color.parseColor("#EBF3F8")
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + ROW_HEIGHT, paint)

        // Top double / thick border
        paint.color = Color.parseColor(COLOR_TEAL)
        paint.strokeWidth = 1.2f
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, paint)
        canvas.drawLine(MARGIN_X, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN_X, y + ROW_HEIGHT, paint)

        var currentX = MARGIN_X
        paint.color = Color.parseColor(COLOR_NAVY)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        columns.forEachIndexed { i, col ->
            val colWidth = (col.widthWeight / totalWeight) * tableWidth
            val rawText = summaryRow.cells.getOrNull(i) ?: ""
            val text = trimTextToWidth(paint, rawText, colWidth - 12f)
            val textY = y + 14f

            if (col.isRightAligned) {
                val textW = paint.measureText(text)
                canvas.drawText(text, currentX + colWidth - 6f - textW, textY, paint)
            } else {
                canvas.drawText(text, currentX + 6f, textY, paint)
            }
            currentX += colWidth
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        currentPage: Int,
        totalPages: Int,
        generatedAt: String
    ) {
        val footerY = PAGE_HEIGHT - FOOTER_HEIGHT + 14f

        paint.color = Color.parseColor(COLOR_DIVIDER)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN_X, footerY - 8f, PAGE_WIDTH - MARGIN_X, footerY - 8f, paint)

        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor(COLOR_TEXT_MUTED)

        // Left: System branding
        canvas.drawText("ADVANCE ERP Enterprise Reporting System", MARGIN_X, footerY + 8f, paint)

        // Center: Timestamp
        val timeW = paint.measureText(generatedAt)
        canvas.drawText(generatedAt, (PAGE_WIDTH - timeW) / 2f, footerY + 8f, paint)

        // Right: Page X of Y
        val pageText = "Page $currentPage of $totalPages"
        val pageW = paint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_X - pageW, footerY + 8f, paint)
    }

    private fun trimTextToWidth(paint: Paint, text: String, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "…" else text
    }

    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createViewIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
