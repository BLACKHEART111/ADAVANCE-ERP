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
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 points at 72dpi
    private const val PAGE_HEIGHT = 842

    fun generateInvoicePdf(
        context: Context,
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        // 1. Top Decorative Brand Banner
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

        // Company Name / Business Identity
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(companyName, 36f, 68f, paint)

        // Company Details
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#E2E8F0")
        val address = businessProfile?.address ?: "Tejgaon Commercial Area, Dhaka-1208, Bangladesh"
        val contact = "Phone: ${businessProfile?.phone ?: "+880 1700-000000"}  |  BIN/Tax: ${businessProfile?.taxNumber ?: "002891928-0101"}"
        canvas.drawText("$address  |  $contact", 36f, 84f, paint)

        // Invoice Badge
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#299DB2") // Official Secondary Teal
        val headerTitle = if (isBangla) "বিক্রয় চালান / INVOICE" else "COMMERCIAL INVOICE"
        val titleWidth = paint.measureText(headerTitle)
        canvas.drawText(headerTitle, PAGE_WIDTH - 36f - titleWidth, 44f, paint)

        // Invoice Number & Date
        paint.textSize = 10f
        paint.color = Color.LTGRAY
        paint.typeface = Typeface.DEFAULT
        val invNoText = "# ${invoice.invoiceNumber}"
        val invDateText = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}"
        canvas.drawText(invNoText, PAGE_WIDTH - 36f - paint.measureText(invNoText), 66f, paint)
        canvas.drawText(invDateText, PAGE_WIDTH - 36f - paint.measureText(invDateText), 82f, paint)

        // 2. Customer & Billing Info Box
        val boxY = 135f
        paint.color = Color.parseColor("#F7FAFC")
        val billBox = RectF(36f, boxY, PAGE_WIDTH - 36f, boxY + 68f)
        canvas.drawRoundRect(billBox, 6f, 6f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(billBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Bill To Header
        paint.color = Color.parseColor("#792B87") // Official Royal Purple
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val billToLabel = if (isBangla) "গ্রাহকের তথ্য / INVOICE TO:" else "CUSTOMER & BILL TO:"
        canvas.drawText(billToLabel, 48f, boxY + 20f, paint)

        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.customerName, 48f, boxY + 38f, paint)

        paint.color = Color.parseColor("#718096")
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        val phoneText = "Mobile: ${invoice.customerPhone ?: "N/A"}"
        val whText = "Dispatched From: ${invoice.warehouseName}"
        canvas.drawText(phoneText, 48f, boxY + 54f, paint)
        canvas.drawText(whText, 320f, boxY + 38f, paint)

        // Status Tag inside Box
        val statusText = "STATUS: ${invoice.status}"
        paint.color = if (invoice.status == "PAID") Color.parseColor("#276749") else Color.parseColor("#C53030")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(statusText, 320f, boxY + 54f, paint)

        // 3. Items Table Header
        val tableY = 224f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, tableY, PAGE_WIDTH - 36f, tableY + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("#", 44f, tableY + 16f, paint)
        canvas.drawText("PRODUCT / DESCRIPTION", 70f, tableY + 16f, paint)
        canvas.drawText("SKU", 260f, tableY + 16f, paint)
        canvas.drawText("QTY", 335f, tableY + 16f, paint)
        canvas.drawText("UNIT PRICE (৳)", 390f, tableY + 16f, paint)
        canvas.drawText("DISCOUNT", 465f, tableY + 16f, paint)
        canvas.drawText("TOTAL (৳)", 515f, tableY + 16f, paint)

        // Table Rows
        var currentY = tableY + 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        items.forEachIndexed { index, item ->
            val isEven = index % 2 == 0
            if (isEven) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, currentY, PAGE_WIDTH - 36f, currentY + 22f, paint)
            }

            paint.color = Color.parseColor("#4A5568")
            canvas.drawText("${index + 1}", 44f, currentY + 15f, paint)

            paint.color = Color.parseColor("#1A202C")
            val pName = if (isBangla && item.productNameBn.isNotBlank()) item.productNameBn else item.productNameEn
            val truncatedName = if (pName.length > 32) pName.take(30) + ".." else pName
            canvas.drawText(truncatedName, 70f, currentY + 15f, paint)

            paint.color = Color.parseColor("#718096")
            canvas.drawText(item.sku, 260f, currentY + 15f, paint)

            paint.color = Color.parseColor("#1A202C")
            canvas.drawText("${item.quantity}", 338f, currentY + 15f, paint)
            canvas.drawText("৳" + Formatters.formatMinorUnit(item.unitPriceMinor), 390f, currentY + 15f, paint)

            paint.color = Color.parseColor("#E53E3E")
            canvas.drawText(if (item.discountMinor > 0) "-৳${Formatters.formatMinorUnit(item.discountMinor)}" else "৳0.00", 465f, currentY + 15f, paint)

            paint.color = Color.parseColor("#2D3748")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val lineTotalStr = "৳" + Formatters.formatMinorUnit(item.lineTotalMinor)
            canvas.drawText(lineTotalStr, 515f, currentY + 15f, paint)
            paint.typeface = Typeface.DEFAULT

            currentY += 22f
        }

        // Table bottom border
        paint.color = Color.parseColor("#CBD5E0")
        canvas.drawLine(36f, currentY + 4f, PAGE_WIDTH - 36f, currentY + 4f, paint)

        // 4. Financial Summary Card (Bottom-Right)
        val sumY = currentY + 20f
        val sumBoxLeft = 320f
        val sumBoxRight = PAGE_WIDTH - 36f

        paint.color = Color.parseColor("#F7FAFC")
        val summaryBox = RectF(sumBoxLeft, sumY, sumBoxRight, sumY + 110f)
        canvas.drawRoundRect(summaryBox, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(summaryBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        var lineY = sumY + 22f
        paint.textSize = 9.5f

        // Subtotal
        paint.color = Color.parseColor("#4A5568")
        canvas.drawText("Subtotal / উপমোট:", sumBoxLeft + 14f, lineY, paint)
        val subtotalStr = "৳" + Formatters.formatMinorUnit(invoice.subtotalMinor)
        canvas.drawText(subtotalStr, sumBoxRight - 14f - paint.measureText(subtotalStr), lineY, paint)

        // Discount
        lineY += 18f
        canvas.drawText("Invoice Discount / বাট্টা:", sumBoxLeft + 14f, lineY, paint)
        paint.color = Color.parseColor("#E53E3E")
        val discStr = "-৳" + Formatters.formatMinorUnit(invoice.discountMinor)
        canvas.drawText(discStr, sumBoxRight - 14f - paint.measureText(discStr), lineY, paint)

        // Total
        lineY += 20f
        paint.color = Color.parseColor("#1A202C")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("Grand Total / মোট বিল:", sumBoxLeft + 14f, lineY, paint)
        val totalStr = "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor)
        canvas.drawText(totalStr, sumBoxRight - 14f - paint.measureText(totalStr), lineY, paint)

        // Paid & Due
        lineY += 20f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#276749")
        canvas.drawText("Amount Paid / পরিশোধিত:", sumBoxLeft + 14f, lineY, paint)
        val paidStr = "৳" + Formatters.formatMinorUnit(invoice.paidAmountMinor)
        canvas.drawText(paidStr, sumBoxRight - 14f - paint.measureText(paidStr), lineY, paint)

        lineY += 18f
        paint.color = Color.parseColor("#C53030")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Balance Due / বকেয়া:", sumBoxLeft + 14f, lineY, paint)
        val dueStr = "৳" + Formatters.formatMinorUnit(invoice.dueAmountMinor)
        canvas.drawText(dueStr, sumBoxRight - 14f - paint.measureText(dueStr), lineY, paint)

        // 5. Signature Footer
        val footerY = PAGE_HEIGHT - 65f
        paint.color = Color.parseColor("#A0AEC0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(48f, footerY, 190f, footerY, paint)
        canvas.drawLine(PAGE_WIDTH - 190f, footerY, PAGE_WIDTH - 48f, footerY, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#718096")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Customer's Signature / গ্রাহকের স্বাক্ষর", 50f, footerY + 14f, paint)
        canvas.drawText("Authorized Signature & Seal", PAGE_WIDTH - 180f, footerY + 14f, paint)

        // Watermark Note
        val noteText = "Generated by Advance Import ERP • Real-time Offline Persistence • Powered by Clean Architecture"
        val noteWidth = paint.measureText(noteText)
        canvas.drawText(noteText, (PAGE_WIDTH - noteWidth) / 2f, PAGE_HEIGHT - 22f, paint)

        document.finishPage(page)

        // Write to Cache/File directory
        val dir = File(context.cacheDir, "invoices")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return file
    }

    fun generatePaymentReceiptPdf(
        context: Context,
        payment: CustomerPayment,
        allocations: List<PaymentAllocation>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        // 1. Top Decorative Brand Banner
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

        // Title
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(companyName, 36f, 68f, paint)

        // Company Details
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#E2E8F0")
        val address = businessProfile?.address ?: "Tejgaon Commercial Area, Dhaka-1208, Bangladesh"
        val contact = "Phone: ${businessProfile?.phone ?: "+880 1700-000000"}"
        canvas.drawText("$address  |  $contact", 36f, 84f, paint)

        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#299DB2") // Official Teal
        val receiptHeader = if (isBangla) "টাকা প্রাপ্তির রশিদ / MONEY RECEIPT" else "OFFICIAL PAYMENT RECEIPT"
        val rWidth = paint.measureText(receiptHeader)
        canvas.drawText(receiptHeader, PAGE_WIDTH - 36f - rWidth, 42f, paint)

        paint.textSize = 10f
        paint.color = Color.LTGRAY
        paint.typeface = Typeface.DEFAULT
        val rNoText = "Receipt #: ${payment.paymentNumber}"
        val rDateText = "Date: ${dateFormat.format(Date(payment.paymentDate))}"
        canvas.drawText(rNoText, PAGE_WIDTH - 36f - paint.measureText(rNoText), 64f, paint)
        canvas.drawText(rDateText, PAGE_WIDTH - 36f - paint.measureText(rDateText), 80f, paint)

        // Receipt Details
        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Received With Thanks From: ${payment.customerName}", 36f, y, paint)

        y += 24f
        paint.textSize = 11f
        paint.color = Color.parseColor("#4A5568")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Payment Method: ${payment.paymentMethod}  |  Ref / Note: ${payment.referenceNote ?: "N/A"}", 36f, y, paint)

        y += 30f
        paint.color = Color.parseColor("#276749")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val amountStr = "Amount Received: ৳" + Formatters.formatMinorUnit(payment.amountMinor)
        canvas.drawText(amountStr, 36f, y, paint)

        // Allocations breakdown
        y += 36f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("APPLIED TO INVOICE NUMBER", 48f, y + 16f, paint)
        canvas.drawText("ALLOCATED AMOUNT (৳)", PAGE_WIDTH - 180f, y + 16f, paint)

        y += 24f
        paint.typeface = Typeface.DEFAULT
        allocations.forEach { alloc ->
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(alloc.invoiceNumber, 48f, y + 18f, paint)
            val allocStr = "৳" + Formatters.formatMinorUnit(alloc.amountMinor)
            canvas.drawText(allocStr, PAGE_WIDTH - 180f, y + 18f, paint)
            y += 24f
        }

        if (payment.unallocatedCreditMinor > 0) {
            paint.color = Color.parseColor("#2B6CB0")
            canvas.drawText("Unallocated Advance Credit / আমানত:", 48f, y + 18f, paint)
            val advStr = "৳" + Formatters.formatMinorUnit(payment.unallocatedCreditMinor)
            canvas.drawText(advStr, PAGE_WIDTH - 180f, y + 18f, paint)
            y += 24f
        }

        // Signatures
        val footerY = PAGE_HEIGHT - 70f
        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        canvas.drawLine(48f, footerY, 190f, footerY, paint)
        canvas.drawLine(PAGE_WIDTH - 190f, footerY, PAGE_WIDTH - 48f, footerY, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#718096")
        canvas.drawText("Depositor's Signature", 50f, footerY + 14f, paint)
        canvas.drawText("Authorized Cashier / Accountant", PAGE_WIDTH - 180f, footerY + 14f, paint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "invoices")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${payment.paymentNumber}.pdf")
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return file
    }

    /**
     * Generates a thermal receipt PDF formatted for standard 80mm (226pt) or 58mm (164pt) ESC/POS thermal printers.
     */
    fun generateThermalReceiptPdf(
        context: Context,
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        businessProfile: BusinessProfile?,
        cashTenderedMinor: Long = invoice.paidAmountMinor,
        changeReturnedMinor: Long = 0L,
        isBangla: Boolean = false,
        is80mm: Boolean = true
    ): File {
        val width = if (is80mm) 226 else 164
        val baseHeight = 380
        val itemsHeight = items.size * 24
        val height = baseHeight + itemsHeight

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

        val leftMargin = 8f
        val rightMargin = width - 8f
        val centerX = width / 2f
        var currentY = 18f

        // Company Header
        paint.color = Color.BLACK
        paint.textSize = if (is80mm) 11f else 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(companyName, centerX, currentY, paint)
        currentY += 12f

        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val address = businessProfile?.address ?: "Tejgaon CA, Dhaka-1208"
        canvas.drawText(address, centerX, currentY, paint)
        currentY += 10f

        val phone = "Tel: ${businessProfile?.phone ?: "+880 1700-000000"}"
        canvas.drawText(phone, centerX, currentY, paint)
        currentY += 10f

        val bin = "BIN: ${businessProfile?.taxNumber ?: "002891928-0101"}"
        canvas.drawText(bin, centerX, currentY, paint)
        currentY += 12f

        // Divider
        fun drawDashedLine(y: Float) {
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7f
            val dash = if (is80mm) "------------------------------------------------------------" else "------------------------------------------"
            canvas.drawText(dash, leftMargin, y, paint)
        }

        drawDashedLine(currentY)
        currentY += 10f

        // Meta info
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.LEFT
        val titleText = if (isBangla) "নগদ বিক্রয় রশিদ / CASH RECEIPT" else "RETAIL SALES RECEIPT"
        canvas.drawText(titleText, leftMargin, currentY, paint)
        currentY += 10f

        canvas.drawText("Inv: ${invoice.invoiceNumber}", leftMargin, currentY, paint)
        currentY += 10f
        canvas.drawText("Date: ${dateFormat.format(Date(invoice.invoiceDate))}", leftMargin, currentY, paint)
        currentY += 10f
        canvas.drawText("Cust: ${invoice.customerName}", leftMargin, currentY, paint)
        currentY += 12f

        drawDashedLine(currentY)
        currentY += 10f

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item", leftMargin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total", rightMargin, currentY, paint)
        currentY += 10f

        // Items
        paint.typeface = Typeface.DEFAULT
        items.forEach { item ->
            paint.textAlign = Paint.Align.LEFT
            val itemName = if (item.productNameEn.length > 22) item.productNameEn.take(20) + ".." else item.productNameEn
            canvas.drawText(itemName, leftMargin, currentY, paint)
            currentY += 9f

            val subDesc = "${item.quantity} x ৳${Formatters.formatMinorUnit(item.unitPriceMinor)}"
            canvas.drawText(subDesc, leftMargin + 6f, currentY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("৳" + Formatters.formatMinorUnit(item.lineTotalMinor), rightMargin, currentY, paint)
            currentY += 13f
        }

        drawDashedLine(currentY)
        currentY += 10f

        // Totals
        fun drawTotalRow(label: String, amountStr: String, isBold: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, leftMargin + 20f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountStr, rightMargin, currentY, paint)
            currentY += 11f
        }

        drawTotalRow("Subtotal:", "৳" + Formatters.formatMinorUnit(invoice.subtotalMinor))
        if (invoice.discountMinor > 0) {
            drawTotalRow("Discount:", "-৳" + Formatters.formatMinorUnit(invoice.discountMinor))
        }
        drawTotalRow("NET TOTAL:", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor), isBold = true)
        currentY += 2f
        drawTotalRow("Cash Paid:", "৳" + Formatters.formatMinorUnit(cashTenderedMinor))
        if (changeReturnedMinor > 0) {
            drawTotalRow("Change Due:", "৳" + Formatters.formatMinorUnit(changeReturnedMinor))
        }

        drawDashedLine(currentY)
        currentY += 14f

        // Barcode
        try {
            val barcodeBitmap = BarcodeGenerator.generateBarcodeBitmap(
                content = invoice.invoiceNumber,
                width = (width - 24),
                height = 36,
                includeText = true
            )
            canvas.drawBitmap(barcodeBitmap, 12f, currentY, paint)
            currentY += 46f
        } catch (e: Exception) {
            currentY += 10f
        }

        // Footer Message
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val footerMsg = if (isBangla) "ধন্যবাদ, আবার আসবেন!" else "Thank you for shopping with us!"
        canvas.drawText(footerMsg, centerX, currentY, paint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Receipt_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return file
    }

    fun createSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
