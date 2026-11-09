package com.pocketdev.advance_erp.core.util

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
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.model.CashFlowStatement
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width
    private const val PAGE_HEIGHT = 842 // A4 height

    fun generateProfitAndLossPdf(
        context: Context,
        pnl: ProfitAndLossStatement,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        drawHeader(context, canvas, paint, businessProfile, if (isBangla) "লাভ-ক্ষতি বিবরণী" else "PROFIT & LOSS STATEMENT")

        val fromStr = dateFormat.format(Date(pnl.fromDate))
        val toStr = dateFormat.format(Date(pnl.toDate))
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Period: $fromStr - $toStr", 36f, 130f, paint)

        var y = 160f
        fun drawSection(title: String, amountMinor: Long, isBold: Boolean = false, isHeader: Boolean = false) {
            paint.typeface = if (isBold || isHeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textSize = if (isHeader) 12f else 10f
            paint.color = if (isHeader) Color.parseColor("#1E293B") else Color.parseColor("#334155")
            canvas.drawText(title, 36f, y, paint)

            val amtText = Formatters.formatMinorUnit(amountMinor)
            val amtWidth = paint.measureText(amtText)
            canvas.drawText(amtText, PAGE_WIDTH - 36f - amtWidth, y, paint)
            y += 20f
        }

        fun drawDivider() {
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(36f, y - 5f, PAGE_WIDTH - 36f, y - 5f, paint)
            paint.style = Paint.Style.FILL
            y += 10f
        }

        drawSection("1. Operating Revenue (Sales)", pnl.grossSalesMinor, isHeader = true)
        drawSection("   Less: Sales Returns", -pnl.salesReturnsMinor)
        drawSection("   Less: Sales Discounts", -pnl.salesDiscountsMinor)
        drawDivider()
        drawSection("Net Sales Revenue", pnl.netSalesRevenueMinor, isBold = true)
        y += 10f

        drawSection("2. Cost of Goods Sold (COGS)", -pnl.costOfGoodsSoldMinor, isHeader = true)
        drawDivider()
        drawSection("Gross Profit", pnl.grossProfitMinor, isBold = true)
        y += 10f

        drawSection("3. Operating Expenses", pnl.totalOperatingExpensesMinor, isHeader = true)
        for (item in pnl.operatingExpenses) {
            val name = if (isBangla) item.nameBn else item.nameEn
            drawSection("   • $name (${item.accountCode})", item.amountMinor)
        }
        drawDivider()
        drawSection("Total Operating Expenses", pnl.totalOperatingExpensesMinor, isBold = true)
        drawSection("Operating Profit", pnl.operatingProfitMinor, isBold = true)
        y += 10f

        drawSection("4. Other Financial Items", pnl.foreignExchangeGainLossMinor, isHeader = true)
        drawSection("   Realized Foreign Exchange Gain / (Loss)", pnl.foreignExchangeGainLossMinor)
        drawDivider()
        drawSection("NET PROFIT / (LOSS)", pnl.netProfitMinor, isBold = true, isHeader = true)

        document.finishPage(page)

        val outDir = File(context.cacheDir, "financial_reports").apply { if (!exists()) mkdirs() }
        val outFile = File(outDir, "pnl_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos -> document.writeTo(fos) }
        document.close()
        return outFile
    }

    fun generateBalanceSheetPdf(
        context: Context,
        bs: BalanceSheet,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        drawHeader(context, canvas, paint, businessProfile, if (isBangla) "উদ্বৃত্তপত্র / ব্যালেন্স শিট" else "BALANCE SHEET")

        val asOfStr = dateFormat.format(Date(bs.asOfDate))
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        canvas.drawText("As of Date: $asOfStr", 36f, 130f, paint)

        var y = 160f
        fun drawRow(title: String, amountMinor: Long, isBold: Boolean = false, isHeader: Boolean = false) {
            paint.typeface = if (isBold || isHeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textSize = if (isHeader) 11.5f else 9.5f
            paint.color = if (isHeader) Color.parseColor("#0F172A") else Color.parseColor("#334155")
            canvas.drawText(title, 36f, y, paint)

            val amtText = Formatters.formatMinorUnit(amountMinor)
            val amtWidth = paint.measureText(amtText)
            canvas.drawText(amtText, PAGE_WIDTH - 36f - amtWidth, y, paint)
            y += 18f
        }

        fun drawDivider() {
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(36f, y - 4f, PAGE_WIDTH - 36f, y - 4f, paint)
            paint.style = Paint.Style.FILL
            y += 8f
        }

        // Assets
        drawRow("ASSETS", bs.totalAssetsMinor, isBold = true, isHeader = true)
        for (item in bs.currentAssets) {
            val name = if (isBangla) item.nameBn else item.nameEn
            drawRow("  ${item.accountCode} - $name", item.amountMinor)
        }
        drawDivider()
        drawRow("TOTAL ASSETS", bs.totalAssetsMinor, isBold = true)
        y += 12f

        // Liabilities
        drawRow("LIABILITIES", bs.totalLiabilitiesMinor, isBold = true, isHeader = true)
        for (item in bs.currentLiabilities) {
            val name = if (isBangla) item.nameBn else item.nameEn
            drawRow("  ${item.accountCode} - $name", item.amountMinor)
        }
        drawDivider()
        drawRow("TOTAL LIABILITIES", bs.totalLiabilitiesMinor, isBold = true)
        y += 12f

        // Equity
        drawRow("EQUITY", bs.totalEquityMinor, isBold = true, isHeader = true)
        for (item in bs.equityItems) {
            val name = if (isBangla) item.nameBn else item.nameEn
            drawRow("  ${item.accountCode} - $name", item.amountMinor)
        }
        drawRow("  Retained Earnings (Accumulated Net Profit)", bs.retainedEarningsMinor)
        drawDivider()
        drawRow("TOTAL EQUITY", bs.totalEquityMinor, isBold = true)
        y += 8f

        drawDivider()
        drawRow("TOTAL LIABILITIES & EQUITY", bs.totalLiabilitiesAndEquityMinor, isBold = true, isHeader = true)

        document.finishPage(page)

        val outDir = File(context.cacheDir, "financial_reports").apply { if (!exists()) mkdirs() }
        val outFile = File(outDir, "balance_sheet_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos -> document.writeTo(fos) }
        document.close()
        return outFile
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        businessProfile: BusinessProfile?,
        reportTitle: String
    ) {
        // 1. Top Decorative Brand Banner in Deep Navy
        paint.color = Color.parseColor("#263849") // Official Deep Navy
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        // Accent Stripes: Royal Purple & Secondary Teal
        paint.color = Color.parseColor("#792B87") // Royal Purple
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat() * 0.5f, 115f, paint)
        paint.color = Color.parseColor("#299DB2") // Secondary Teal
        canvas.drawRect(PAGE_WIDTH.toFloat() * 0.5f, 110f, PAGE_WIDTH.toFloat(), 115f, paint)

        // Draw Official Logo on Banner
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_compact_white)
        if (logoBitmap != null) {
            val logoH = 32f
            val logoW = logoH * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
            val logoDest = RectF(36f, 16f, 36f + logoW, 16f + logoH)
            canvas.drawBitmap(logoBitmap, null, logoDest, paint)
        }

        // Company Details
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val company = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(company, 36f, 68f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#E2E8F0")
        val addr = businessProfile?.address ?: "Motijheel C/A, Dhaka-1000, Bangladesh"
        val phone = businessProfile?.phone ?: "+880 1711-000000"
        canvas.drawText("$addr  |  $phone", 36f, 84f, paint)

        paint.color = Color.parseColor("#299DB2") // Official Secondary Teal
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleWidth = paint.measureText(reportTitle)
        canvas.drawText(reportTitle, PAGE_WIDTH - 36f - titleWidth, 42f, paint)
    }

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
