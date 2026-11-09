package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ControlCenterPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 32f

    fun generateReportPdf(
        context: Context,
        title: String,
        filterState: ControlCenterFilterState,
        summary: BusinessSummaryDashboardData,
        transactions: List<UnifiedTransactionItem>,
        businessProfile: BusinessProfile?,
        canViewProfit: Boolean,
        isBangla: Boolean = false
    ): File {
        val pdfDir = File(context.cacheDir, "reports_pdf").apply { if (!exists()) mkdirs() }
        val sanitizedTitle = title.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(30)
        val file = File(pdfDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val generatedAt = timeFormat.format(Date())

        val (fromMillis, toMillis) = filterState.dateRangeOption.getBoundsMillis(filterState.customStartDate, filterState.customEndDate)
        val periodText = if (filterState.dateRangeOption.name == "ALL_TIME" || fromMillis == 0L) {
            if (isBangla) "সময়কাল: ব্যবসা শুরুর তারিখ থেকে বর্তমান দিন পর্যন্ত (সকল সময়)" else "Period: All Time (Inception to Present)"
        } else {
            val fromStr = dateFormat.format(Date(fromMillis))
            val toStr = dateFormat.format(Date(kotlin.math.min(toMillis, System.currentTimeMillis())))
            if (isBangla) "সময়কাল: $fromStr হতে $toStr" else "Period: $fromStr - $toStr"
        }

        // Layout constants
        val rowHeight = 22f
        val headerHeight = 110f
        val summaryCardHeight = if (canViewProfit) 70f else 50f
        val tableHeaderY_page1 = headerHeight + summaryCardHeight + 20f

        val rowsPerPageSubsequent = ((PAGE_HEIGHT - 90f - 40f) / rowHeight).toInt()
        val rowsPage1 = ((PAGE_HEIGHT - tableHeaderY_page1 - 60f) / rowHeight).toInt().coerceAtLeast(1)

        val totalTransactions = transactions.size
        val subsequentRows = (totalTransactions - rowsPage1).coerceAtLeast(0)
        val totalPages = 1 + if (subsequentRows > 0) Math.ceil(subsequentRows.toDouble() / rowsPerPageSubsequent).toInt() else 0

        var currentItemIdx = 0

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = 0f

            if (pageIndex == 1) {
                // Header Banner
                drawHeaderBanner(context, canvas, paint, businessProfile, title, periodText)
                currentY = headerHeight + 10f

                // Summary KPI Box
                drawSummaryCards(canvas, paint, summary, canViewProfit, currentY, isBangla)
                currentY += summaryCardHeight + 10f

                // Table Header
                drawTableHeader(canvas, paint, currentY, isBangla)
                currentY += 20f

                // Draw rows for Page 1
                val itemsToDraw = minOf(rowsPage1, totalTransactions - currentItemIdx)
                for (i in 0 until itemsToDraw) {
                    val item = transactions[currentItemIdx]
                    drawTransactionRow(canvas, paint, item, currentY, currentItemIdx % 2 == 1, dateFormat)
                    currentY += rowHeight
                    currentItemIdx++
                }
            } else {
                // Subsequent page mini header
                drawSubsequentHeader(canvas, paint, title, periodText, pageIndex, totalPages)
                currentY = 50f

                // Table Header
                drawTableHeader(canvas, paint, currentY, isBangla)
                currentY += 20f

                // Draw rows
                val itemsToDraw = minOf(rowsPerPageSubsequent, totalTransactions - currentItemIdx)
                for (i in 0 until itemsToDraw) {
                    val item = transactions[currentItemIdx]
                    drawTransactionRow(canvas, paint, item, currentY, currentItemIdx % 2 == 1, dateFormat)
                    currentY += rowHeight
                    currentItemIdx++
                }
            }

            // If last page, draw totals footer
            if (pageIndex == totalPages) {
                drawTotalsFooter(canvas, paint, summary, currentY + 10f, isBangla)
            }

            // Page Footer
            drawPageFooter(canvas, paint, generatedAt, pageIndex, totalPages)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawHeaderBanner(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        businessProfile: BusinessProfile?,
        reportTitle: String,
        periodText: String
    ) {
        // Dark Navy Header Background
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Accent strip (Purple & Teal)
        paint.color = Color.parseColor("#7C3AED")
        canvas.drawRect(0f, 95f, PAGE_WIDTH * 0.5f, 98f, paint)
        paint.color = Color.parseColor("#0EA5E9")
        canvas.drawRect(PAGE_WIDTH * 0.5f, 95f, PAGE_WIDTH.toFloat(), 98f, paint)

        // Company Logo
        try {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_compact_white)
            if (logo != null) {
                val h = 24f
                val w = h * (logo.width.toFloat() / logo.height.toFloat())
                canvas.drawBitmap(logo, null, RectF(MARGIN, 16f, MARGIN + w, 16f + h), paint)
            }
        } catch (_: Exception) {}

        // Company Name & Info
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val compName = businessProfile?.companyName ?: "ADVANCE ERP ENTERPRISE"
        canvas.drawText(compName, MARGIN, 54f, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val address = businessProfile?.address ?: "Motijheel C/A, Dhaka-1000, Bangladesh"
        val phone = businessProfile?.phone ?: "+880 1700-000000"
        val bin = businessProfile?.taxNumber ?: "BIN: 001928374-0101"
        canvas.drawText("$address  |  Phone: $phone  |  $bin", MARGIN, 68f, paint)

        // Report Title on Right
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleWidth = paint.measureText(reportTitle)
        canvas.drawText(reportTitle, PAGE_WIDTH - MARGIN - titleWidth, 36f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        val periodWidth = paint.measureText(periodText)
        canvas.drawText(periodText, PAGE_WIDTH - MARGIN - periodWidth, 54f, paint)

        paint.color = Color.parseColor("#94A3B8")
        val sysBadge = "ADVANCE BUSINESS CONTROL CENTER"
        val sysWidth = paint.measureText(sysBadge)
        canvas.drawText(sysBadge, PAGE_WIDTH - MARGIN - sysWidth, 68f, paint)
    }

    private fun drawSubsequentHeader(
        canvas: Canvas,
        paint: Paint,
        title: String,
        periodText: String,
        currentPage: Int,
        totalPages: Int
    ) {
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 35f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, MARGIN, 22f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        val txt = "$periodText  |  Page $currentPage of $totalPages"
        val tw = paint.measureText(txt)
        canvas.drawText(txt, PAGE_WIDTH - MARGIN - tw, 22f, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, 35f, PAGE_WIDTH - MARGIN, 35f, paint)
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        paint: Paint,
        summary: BusinessSummaryDashboardData,
        canViewProfit: Boolean,
        startY: Float,
        isBangla: Boolean
    ) {
        val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 15f) / 4f
        val boxHeight = 26f

        fun drawMetric(label: String, amountMinor: Long, col: Int, row: Int, colorHex: String) {
            val left = MARGIN + col * (boxWidth + 5f)
            val top = startY + row * (boxHeight + 5f)
            val right = left + boxWidth
            val bottom = top + boxHeight

            // Background card
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(left, top, right, bottom), 4f, 4f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(RectF(left, top, right, bottom), 4f, 4f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 6.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(label, left + 6f, top + 10f, paint)

            paint.color = Color.parseColor(colorHex)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtText = Formatters.formatMinorUnit(amountMinor)
            canvas.drawText(amtText, left + 6f, top + 21f, paint)
        }

        drawMetric(if (isBangla) "মোট বিক্রয়" else "Total Sales", summary.totalSalesMinor, 0, 0, "#0F172A")
        drawMetric(if (isBangla) "মোট ক্রয়" else "Total Purchase", summary.totalPurchaseMinor, 1, 0, "#7C3AED")
        drawMetric(if (isBangla) "মোট গ্রহণ" else "Total Received", summary.totalReceivedMinor, 2, 0, "#16A34A")
        drawMetric(if (isBangla) "মোট পরিশোধ" else "Total Paid", summary.totalPaidMinor, 3, 0, "#0284C7")

        drawMetric(if (isBangla) "মোট ব্যয়" else "Total Expenses", summary.totalExpensesMinor, 0, 1, "#D97706")
        drawMetric(if (isBangla) "পাওনা (Receivable)" else "Customer Due", summary.totalOutstandingReceivableMinor, 1, 1, "#EA580C")
        drawMetric(if (isBangla) "দেনা (Payable)" else "Supplier Due", summary.totalOutstandingPayableMinor, 2, 1, "#DC2626")

        if (canViewProfit) {
            drawMetric(if (isBangla) "নিট মুনাফা" else "Net Profit", summary.netProfitMinor, 3, 1, if (summary.netProfitMinor >= 0) "#16A34A" else "#DC2626")
        } else {
            drawMetric(if (isBangla) "মুনাফা (গোপনীয়)" else "Profit (Admin)", 0L, 3, 1, "#94A3B8")
        }
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float, isBangla: Boolean) {
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f, paint)

        paint.color = Color.WHITE
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val headerY = y + 12f
        canvas.drawText(if (isBangla) "তারিখ" else "Date", MARGIN + 4f, headerY, paint)
        canvas.drawText(if (isBangla) "রেফারেন্স" else "Ref No.", MARGIN + 58f, headerY, paint)
        canvas.drawText(if (isBangla) "বিবরণ / পার্টি" else "Party / Particulars", MARGIN + 130f, headerY, paint)
        canvas.drawText(if (isBangla) "ক্যাটাগরি" else "Category", MARGIN + 270f, headerY, paint)
        canvas.drawText(if (isBangla) "পেমেন্ট" else "Method", MARGIN + 335f, headerY, paint)
        canvas.drawText(if (isBangla) "স্ট্যাটাস" else "Status", MARGIN + 380f, headerY, paint)

        // Numeric right-aligned
        val debitText = if (isBangla) "ইনফ্লো / ডেবিট" else "Inflow / Debit"
        val creditText = if (isBangla) "আউটফ্লো / ক্রেডিট" else "Outflow / Credit"
        val balanceText = if (isBangla) "ব্যালেন্স" else "Balance"

        canvas.drawText(debitText, MARGIN + 425f, headerY, paint)
        canvas.drawText(creditText, MARGIN + 475f, headerY, paint)
        canvas.drawText(balanceText, PAGE_WIDTH - MARGIN - 4f - paint.measureText(balanceText), headerY, paint)
    }

    private fun drawTransactionRow(
        canvas: Canvas,
        paint: Paint,
        item: UnifiedTransactionItem,
        y: Float,
        isZebra: Boolean,
        dateFormat: SimpleDateFormat
    ) {
        if (isZebra) {
            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, paint)
        }

        // Bottom border
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN, y + 20f, PAGE_WIDTH - MARGIN, y + 20f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 6.5f
        paint.typeface = Typeface.DEFAULT
        val textY = y + 13f

        // Date
        paint.color = Color.parseColor("#475569")
        canvas.drawText(dateFormat.format(Date(item.date)), MARGIN + 4f, textY, paint)

        // Ref No
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(item.referenceNumber.take(12), MARGIN + 58f, textY, paint)

        // Party / Particulars
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#1E293B")
        val partyAndDesc = "${item.primaryEntityName} - ${item.particulars}".take(32)
        canvas.drawText(partyAndDesc, MARGIN + 130f, textY, paint)

        // Category
        paint.color = Color.parseColor("#64748B")
        canvas.drawText(item.category.titleEn.take(11), MARGIN + 270f, textY, paint)

        // Method
        canvas.drawText((item.paymentMethod ?: "-").take(8), MARGIN + 335f, textY, paint)

        // Status Badge
        paint.color = item.statusColorHex.toInt()
        canvas.drawText(item.status.take(8), MARGIN + 380f, textY, paint)

        // Debit / Credit Amounts
        val amtStr = Formatters.formatMinorUnit(item.amountMinor)
        if (item.isInflow) {
            paint.color = Color.parseColor("#16A34A")
            canvas.drawText(amtStr, MARGIN + 425f, textY, paint)
        } else {
            paint.color = Color.parseColor("#DC2626")
            canvas.drawText(amtStr, MARGIN + 475f, textY, paint)
        }

        // Running Balance
        val balMinor = item.runningBalanceMinor ?: item.amountMinor
        val balStr = Formatters.formatMinorUnit(balMinor)
        paint.color = Color.parseColor("#0F172A")
        val balW = paint.measureText(balStr)
        canvas.drawText(balStr, PAGE_WIDTH - MARGIN - 4f - balW, textY, paint)
    }

    private fun drawTotalsFooter(
        canvas: Canvas,
        paint: Paint,
        summary: BusinessSummaryDashboardData,
        y: Float,
        isBangla: Boolean
    ) {
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f), 4f, 4f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val textY = y + 17f
        val summaryLabel = if (isBangla) "সর্বমোট হিসাব ও সমাপনী ব্যালেন্স" else "SUMMARY TOTALS & CLOSING BALANCE"
        canvas.drawText(summaryLabel, MARGIN + 8f, textY, paint)

        val closingText = "${if (isBangla) "সমাপনী: " else "Closing: "} ${Formatters.formatMinorUnit(summary.closingBalanceMinor)}"
        val cw = paint.measureText(closingText)
        canvas.drawText(closingText, PAGE_WIDTH - MARGIN - 8f - cw, textY, paint)
    }

    private fun drawPageFooter(canvas: Canvas, paint: Paint, generatedAt: String, page: Int, totalPages: Int) {
        val y = PAGE_HEIGHT - 20f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN, y - 8f, PAGE_WIDTH - MARGIN, y - 8f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 6.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated by ADVANCE ERP Business Control Center on $generatedAt  •  Strictly Confidential", MARGIN, y, paint)

        val pageStr = "Page $page of $totalPages"
        val pw = paint.measureText(pageStr)
        canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - pw, y, paint)
    }
}
