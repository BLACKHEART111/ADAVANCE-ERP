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
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImportPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 points
    private const val PAGE_HEIGHT = 842

    private fun drawBrandHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        businessProfile: BusinessProfile?,
        documentTitle: String,
        documentNumber: String,
        dateFormatted: String
    ) {
        // Deep Navy Header Banner
        paint.color = Color.parseColor("#263849")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        // Accent Stripes: Royal Purple & Secondary Teal
        paint.color = Color.parseColor("#792B87")
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat() * 0.5f, 115f, paint)
        paint.color = Color.parseColor("#299DB2")
        canvas.drawRect(PAGE_WIDTH.toFloat() * 0.5f, 110f, PAGE_WIDTH.toFloat(), 115f, paint)

        // Official White Logo
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_compact_white)
        if (logoBitmap != null) {
            val logoH = 32f
            val logoW = logoH * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
            val logoDest = RectF(36f, 16f, 36f + logoW, 16f + logoH)
            canvas.drawBitmap(logoBitmap, null, logoDest, paint)
        }

        // Company Information
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val companyName = businessProfile?.companyName ?: "ADVANCE IMPORT & EXPORT ERP"
        canvas.drawText(companyName, 36f, 68f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#E2E8F0")
        val address = businessProfile?.address ?: "Motijheel C/A, Dhaka-1000, Bangladesh"
        val contact = "Phone: ${businessProfile?.phone ?: "+880 1711-000000"}  |  BIN/Tax: ${businessProfile?.taxNumber ?: "BIN-99281726-01"}"
        canvas.drawText("$address  |  $contact", 36f, 84f, paint)

        // Document Title & Number Badge
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#299DB2") // Teal
        val titleWidth = paint.measureText(documentTitle)
        canvas.drawText(documentTitle, PAGE_WIDTH - 36f - titleWidth, 42f, paint)

        paint.textSize = 10f
        paint.color = Color.LTGRAY
        paint.typeface = Typeface.DEFAULT
        val docNoText = "Ref #: $documentNumber"
        val dateText = "Date: $dateFormatted"
        canvas.drawText(docNoText, PAGE_WIDTH - 36f - paint.measureText(docNoText), 64f, paint)
        canvas.drawText(dateText, PAGE_WIDTH - 36f - paint.measureText(dateText), 80f, paint)
    }

    private fun drawSignatures(canvas: Canvas, paint: Paint, leftLabel: String, rightLabel: String) {
        val footerY = PAGE_HEIGHT - 65f
        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        canvas.drawLine(48f, footerY, 200f, footerY, paint)
        canvas.drawLine(PAGE_WIDTH - 200f, footerY, PAGE_WIDTH - 48f, footerY, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 9f
        paint.color = Color.parseColor("#718096")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(leftLabel, 50f, footerY + 16f, paint)
        canvas.drawText(rightLabel, PAGE_WIDTH - 195f, footerY + 16f, paint)
    }

    // 1. PURCHASE ORDER PDF
    fun generatePurchaseOrderPdf(
        context: Context,
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val docTitle = if (isBangla) "বৈদেশিক ক্রয়াদেশ / PURCHASE ORDER" else "COMMERCIAL PURCHASE ORDER"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, order.poNumber, dateFormat.format(Date(order.orderDate))
        )

        // Supplier & Order Meta Cards
        val boxY = 135f
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(36f, boxY, PAGE_WIDTH - 36f, boxY + 74f), 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(RectF(36f, boxY, PAGE_WIDTH - 36f, boxY + 74f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#263849")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER / OVERSEAS FACTORY:", 48f, boxY + 18f, paint)

        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 12f
        canvas.drawText(supplier?.companyName ?: "China Manufacturing Factory", 48f, boxY + 36f, paint)

        paint.color = Color.parseColor("#718096")
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        val supContact = "Attn: ${supplier?.contactPerson ?: "Export Dept"}  |  ${supplier?.phone ?: "+86-000-0000"}"
        val supAddress = "${supplier?.address ?: "Guangzhou, Guangdong"} • ${supplier?.country ?: "China"}"
        canvas.drawText(supContact, 48f, boxY + 52f, paint)
        canvas.drawText(supAddress, 48f, boxY + 66f, paint)

        // Meta on Right
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#792B87")
        canvas.drawText("STATUS: ${order.status}", 350f, boxY + 18f, paint)
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Currency: ${order.currency}", 350f, boxY + 36f, paint)
        canvas.drawText("Actual Exchange Rate: 1 ${order.currency} = ${order.exchangeRateToBdt} BDT", 350f, boxY + 52f, paint)

        // Table Header
        val tableY = 225f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, tableY, PAGE_WIDTH - 36f, tableY + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("#", 44f, tableY + 16f, paint)
        canvas.drawText("PRODUCT / DESCRIPTION", 66f, tableY + 16f, paint)
        canvas.drawText("SKU", 260f, tableY + 16f, paint)
        canvas.drawText("QTY", 335f, tableY + 16f, paint)
        canvas.drawText("UNIT (${order.currency})", 380f, tableY + 16f, paint)
        canvas.drawText("TOTAL (${order.currency})", 450f, tableY + 16f, paint)
        canvas.drawText("TOTAL (৳ BDT)", 515f, tableY + 16f, paint)

        // Table Rows
        var currentY = tableY + 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, currentY, PAGE_WIDTH - 36f, currentY + 22f, paint)
            }

            paint.color = Color.parseColor("#4A5568")
            canvas.drawText("${index + 1}", 44f, currentY + 15f, paint)

            val product = productsMap[item.productId]
            val pName = if (isBangla && product?.nameBn?.isNotBlank() == true) product.nameBn else (product?.nameEn ?: item.productId)
            val truncated = if (pName.length > 30) pName.take(28) + ".." else pName

            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(truncated, 66f, currentY + 15f, paint)

            paint.color = Color.parseColor("#718096")
            canvas.drawText(product?.baseSku ?: "SKU", 260f, currentY + 15f, paint)

            paint.color = Color.parseColor("#1A202C")
            canvas.drawText("${item.quantity}", 338f, currentY + 15f, paint)

            val unitStr = Formatters.formatMinorUnit(item.unitPriceMinor)
            canvas.drawText(unitStr, 380f, currentY + 15f, paint)

            val totalStr = Formatters.formatMinorUnit(item.totalAmountMinor)
            canvas.drawText(totalStr, 450f, currentY + 15f, paint)

            paint.color = Color.parseColor("#792B87")
            val bdtStr = "৳" + Formatters.formatMinorUnit(item.totalBdtAmountMinor)
            canvas.drawText(bdtStr, 515f, currentY + 15f, paint)

            currentY += 22f
        }

        // Totals Card
        val totalBoxY = (currentY + 15f).coerceAtLeast(360f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(300f, totalBoxY, PAGE_WIDTH - 36f, totalBoxY + 84f), 8f, 8f, paint)

        val totalQty = items.sumOf { it.quantity }
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#4A5568")
        canvas.drawText("Total Items / Quantity:", 316f, totalBoxY + 20f, paint)
        canvas.drawText("$totalQty pcs", PAGE_WIDTH - 48f - paint.measureText("$totalQty pcs"), totalBoxY + 20f, paint)

        canvas.drawText("Total (${order.currency}):", 316f, totalBoxY + 38f, paint)
        val origTotalStr = "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"
        canvas.drawText(origTotalStr, PAGE_WIDTH - 48f - paint.measureText(origTotalStr), totalBoxY + 38f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#263849")
        canvas.drawText("Total Equivalent (BDT):", 316f, totalBoxY + 56f, paint)
        val bdtTotalStr = "৳" + Formatters.formatMinorUnit(order.totalBdtAmountMinor)
        canvas.drawText(bdtTotalStr, PAGE_WIDTH - 48f - paint.measureText(bdtTotalStr), totalBoxY + 56f, paint)

        paint.color = if (order.paidAmountMinor >= order.totalAmountMinor) Color.parseColor("#276749") else Color.parseColor("#C53030")
        val outstanding = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
        val outStr = "Outstanding Balance: ${order.currency} ${Formatters.formatMinorUnit(outstanding)}"
        canvas.drawText(outStr, 316f, totalBoxY + 74f, paint)

        // Notes on Left
        if (!order.notes.isNullOrBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#4A5568")
            paint.textSize = 9.5f
            canvas.drawText("Consignment / Order Remarks:", 36f, totalBoxY + 20f, paint)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(order.notes, 36f, totalBoxY + 36f, paint)
        }

        drawSignatures(canvas, paint, "Prepared by Import Officer", "Managing Director / Authorized Signatory")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${order.poNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // 2. SUPPLIER PAYMENT RECEIPT VOUCHER PDF
    fun generateSupplierPaymentReceiptPdf(
        context: Context,
        payment: SupplierPayment,
        order: PurchaseOrder?,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        val docTitle = if (isBangla) "বৈদেশিক পেমেন্ট ভাউচার / PAYMENT RECEIPT" else "SUPPLIER PAYMENT VOUCHER"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, payment.paymentNumber, dateFormat.format(Date(payment.paymentDate))
        )

        var y = 145f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Remittance Paid To (Beneficiary): ${supplier?.companyName ?: "China Supplier"}", 36f, y, paint)

        y += 24f
        paint.textSize = 11f
        paint.color = Color.parseColor("#4A5568")
        paint.typeface = Typeface.DEFAULT
        val poLinked = if (order != null) "Linked PO: ${order.poNumber}" else "General Supplier Advance"
        canvas.drawText("$poLinked  |  Method: ${payment.paymentMethod}  |  Ref: ${payment.referenceNumber ?: "N/A"}", 36f, y, paint)

        y += 20f
        canvas.drawText("Disbursed Account: ${payment.paymentAccount}", 36f, y, paint)

        y += 32f
        paint.color = Color.parseColor("#276749")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val amtStr = "Amount Remitted: ${payment.currency} ${Formatters.formatMinorUnit(payment.amountMinor)}  (৳ ${Formatters.formatMinorUnit(payment.bdtAmountMinor)} BDT)"
        canvas.drawText(amtStr, 36f, y, paint)

        y += 20f
        paint.textSize = 10f
        paint.color = Color.parseColor("#718096")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Applied Exchange Rate: 1 ${payment.currency} = ${payment.exchangeRateToBdt} BDT", 36f, y, paint)

        // Summary Breakdown Box
        y += 35f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FINANCIAL RECONCILIATION SUMMARY", 48f, y + 16f, paint)

        y += 24f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#1A202C")
        if (order != null) {
            canvas.drawText("Total Purchase Order Value (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f

            canvas.drawText("Total Cumulative Paid (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(order.paidAmountMinor)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f

            val rem = (order.totalAmountMinor - order.paidAmountMinor).coerceAtLeast(0L)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (rem == 0L) Color.parseColor("#276749") else Color.parseColor("#C53030")
            canvas.drawText("Remaining Outstanding (${order.currency}):", 48f, y + 18f, paint)
            canvas.drawText("${order.currency} ${Formatters.formatMinorUnit(rem)}", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f
        } else {
            canvas.drawText("Supplier Current Outstanding Payable:", 48f, y + 18f, paint)
            canvas.drawText("৳ ${Formatters.formatMinorUnit(supplier?.outstandingPayableMinor ?: 0L)} BDT", PAGE_WIDTH - 180f, y + 18f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Remittance Officer / Cashier", "Chief Financial Officer / Authorized Bank Signatory")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${payment.paymentNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // 3. SHIPMENT PACKING LIST & CARTON REPORT PDF
    fun generatePackingListPdf(
        context: Context,
        shipment: ImportShipment,
        cartons: List<ShipmentCarton>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val docTitle = if (isBangla) "প্যাকিং লিস্ট ও কার্টন রিপোর্ট / PACKING LIST" else "CARGO PACKING LIST & CBM"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, shipment.shipmentNumber, dateFormat.format(Date(shipment.shippingDate))
        )

        val boxY = 135f
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(36f, boxY, PAGE_WIDTH - 36f, boxY + 70f), 8f, 8f, paint)

        paint.color = Color.parseColor("#263849")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CONSIGNMENT DETAILS:", 48f, boxY + 18f, paint)

        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Origin: ${shipment.origin}  →  Destination: ${shipment.destination}", 48f, boxY + 34f, paint)
        canvas.drawText("Carrier / Line: ${shipment.shippingLine ?: "Air/Sea Carrier"}  |  Tracking/AWB: ${shipment.trackingReference ?: "N/A"}", 48f, boxY + 48f, paint)
        canvas.drawText("Supplier: ${supplier?.companyName ?: "China Supplier"}  |  Method: ${shipment.shippingMethod}", 48f, boxY + 62f, paint)

        // Summary Bar on Right
        paint.color = Color.parseColor("#792B87")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Cartons: ${shipment.totalCartons} | Total Qty: ${shipment.totalQuantity} pcs", 330f, boxY + 34f, paint)
        paint.color = Color.parseColor("#299DB2")
        canvas.drawText("Volume: ${shipment.totalCbm} m³ | Weight: ${shipment.totalGrossWeightKg} kg", 330f, boxY + 48f, paint)

        // Table Header
        val tableY = 220f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, tableY, PAGE_WIDTH - 36f, tableY + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("CARTON #", 44f, tableY + 16f, paint)
        canvas.drawText("PRODUCT / DESCRIPTION", 140f, tableY + 16f, paint)
        canvas.drawText("QTY", 335f, tableY + 16f, paint)
        canvas.drawText("DIMS (L×W×H cm)", 380f, tableY + 16f, paint)
        canvas.drawText("WEIGHT (kg)", 465f, tableY + 16f, paint)
        canvas.drawText("CBM (m³)", 525f, tableY + 16f, paint)

        var currentY = tableY + 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        cartons.forEachIndexed { index, carton ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, currentY, PAGE_WIDTH - 36f, currentY + 22f, paint)
            }

            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(carton.cartonNumber, 44f, currentY + 15f, paint)

            val product = productsMap[carton.productId]
            val pName = product?.nameEn ?: carton.productId
            val truncated = if (pName.length > 28) pName.take(26) + ".." else pName
            canvas.drawText(truncated, 140f, currentY + 15f, paint)

            canvas.drawText("${carton.quantity}", 338f, currentY + 15f, paint)
            canvas.drawText("${carton.lengthCm}×${carton.widthCm}×${carton.heightCm}", 380f, currentY + 15f, paint)
            canvas.drawText("${carton.grossWeightKg} kg", 465f, currentY + 15f, paint)
            paint.color = Color.parseColor("#299DB2")
            canvas.drawText("${carton.cbm} m³", 525f, currentY + 15f, paint)

            currentY += 22f
        }

        drawSignatures(canvas, paint, "Forwarding Agent / Inspector", "Receiving Warehouse Officer")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "packing_list_${shipment.shipmentNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // 4. LANDED COST ALLOCATION REPORT PDF
    fun generateLandedCostReportPdf(
        context: Context,
        shipment: ImportShipment,
        snapshots: List<LandedCostSnapshot>,
        costItems: List<ImportCostItem>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val docTitle = if (isBangla) "ল্যান্ডেড কস্ট রিপোর্ট / LANDED COST ENGINE" else "TRUE LANDED COST REPORT"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, shipment.shipmentNumber, dateFormat.format(Date(System.currentTimeMillis()))
        )

        var y = 135f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("IMPORT EXPENSES BREAKDOWN (ACTUAL DISBURSED):", 36f, y, paint)

        y += 18f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 20f, paint)
        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9f
        canvas.drawText("CATEGORY", 44f, y + 14f, paint)
        canvas.drawText("DESCRIPTION", 180f, y + 14f, paint)
        canvas.drawText("BASIS METHOD", 360f, y + 14f, paint)
        canvas.drawText("AMOUNT (৳ BDT)", PAGE_WIDTH - 120f, y + 14f, paint)

        y += 20f
        paint.typeface = Typeface.DEFAULT
        var totalExpensesBdt = 0L

        costItems.forEach { cost ->
            totalExpensesBdt += cost.bdtAmountMinor
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(cost.costCategory, 44f, y + 14f, paint)
            canvas.drawText(cost.description.take(24), 180f, y + 14f, paint)
            canvas.drawText(cost.allocationMethod, 360f, y + 14f, paint)
            canvas.drawText("৳" + Formatters.formatMinorUnit(cost.bdtAmountMinor), PAGE_WIDTH - 120f, y + 14f, paint)
            y += 18f
        }

        y += 10f
        paint.color = Color.parseColor("#792B87")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val expSummary = "Total Import Expenses: ৳${Formatters.formatMinorUnit(totalExpensesBdt)} BDT"
        canvas.drawText(expSummary, PAGE_WIDTH - 36f - paint.measureText(expSummary), y, paint)

        y += 24f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        canvas.drawText("FINAL PRODUCT LANDED COST ALLOCATION SNAPSHOT:", 36f, y, paint)

        y += 16f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 8.5f
        canvas.drawText("PRODUCT / SKU", 44f, y + 15f, paint)
        canvas.drawText("QTY", 210f, y + 15f, paint)
        canvas.drawText("FOB PURCHASE (৳)", 260f, y + 15f, paint)
        canvas.drawText("ALLOCATED EXP (৳)", 360f, y + 15f, paint)
        canvas.drawText("TOTAL BATCH (৳)", 460f, y + 15f, paint)
        canvas.drawText("UNIT COST (৳)", 530f, y + 15f, paint)

        y += 22f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f

        snapshots.forEachIndexed { idx, s ->
            if (idx % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 20f, paint)
            }
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(s.productNameEn.take(24), 44f, y + 14f, paint)
            canvas.drawText("${s.totalReceivedQuantity}", 210f, y + 14f, paint)
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalPurchaseCostBdtMinor), 260f, y + 14f, paint)
            paint.color = Color.parseColor("#299DB2")
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalAllocatedCostsBdtMinor), 360f, y + 14f, paint)
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.totalLandedCostBdtMinor), 460f, y + 14f, paint)
            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("৳" + Formatters.formatMinorUnit(s.unitLandedCostBdtMinor), 530f, y + 14f, paint)
            paint.typeface = Typeface.DEFAULT
            y += 20f
        }

        drawSignatures(canvas, paint, "Costing & Allocation Accountant", "Chief Financial Officer")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "landed_cost_${shipment.shipmentNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // 5. WAREHOUSE RECEIVING REPORT PDF
    fun generateWarehouseReceivingReportPdf(
        context: Context,
        receipt: WarehouseReceipt,
        items: List<WarehouseReceiptItem>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?,
        isBangla: Boolean = false
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        val docTitle = if (isBangla) "ওয়্যারহাউস গ্রহণ রিপোর্ট / RECEIVING REPORT" else "WAREHOUSE RECEIVING REPORT"
        drawBrandHeader(
            context, canvas, paint, businessProfile,
            docTitle, receipt.receiptNumber, dateFormat.format(Date(receipt.receiptDate))
        )

        var y = 145f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Facility: Central Warehouse (${receipt.warehouseId})  |  Inspector: ${receipt.receivedBy}", 36f, y, paint)

        y += 18f
        paint.textSize = 10f
        paint.color = Color.parseColor("#4A5568")
        paint.typeface = Typeface.DEFAULT
        val disStr = if (receipt.hasDiscrepancies) "Status: Discrepancies Noted (Inspection Pending Approval)" else "Status: Clean Inspection • Posted to Stock"
        canvas.drawText("$disStr  |  Remarks: ${receipt.notes ?: "Check-in complete"}", 36f, y, paint)

        // Table
        y += 30f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT / DESCRIPTION", 44f, y + 16f, paint)
        canvas.drawText("EXPECTED", 220f, y + 16f, paint)
        canvas.drawText("RECEIVED GOOD", 290f, y + 16f, paint)
        canvas.drawText("DAMAGED", 380f, y + 16f, paint)
        canvas.drawText("MISSING", 440f, y + 16f, paint)
        canvas.drawText("UNIT LANDED COST", 500f, y + 16f, paint)

        y += 24f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[item.productId]
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText(product?.nameEn ?: item.productId, 44f, y + 15f, paint)
            canvas.drawText("${item.expectedQuantity}", 220f, y + 15f, paint)
            paint.color = Color.parseColor("#276749")
            canvas.drawText("${item.receivedQuantity}", 290f, y + 15f, paint)
            paint.color = if (item.damagedQuantity > 0) Color.parseColor("#C53030") else Color.parseColor("#718096")
            canvas.drawText("${item.damagedQuantity}", 380f, y + 15f, paint)
            paint.color = if (item.missingQuantity > 0) Color.parseColor("#C53030") else Color.parseColor("#718096")
            canvas.drawText("${item.missingQuantity}", 440f, y + 15f, paint)
            paint.color = Color.parseColor("#792B87")
            canvas.drawText("৳" + Formatters.formatMinorUnit(item.unitLandedCostMinor), 500f, y + 15f, paint)

            y += 22f
        }

        drawSignatures(canvas, paint, "Warehouse Officer / Storekeeper", "Inventory Manager / Auditor")

        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${receipt.receiptNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun generateCostVarianceReportPdf(
        context: Context,
        shipment: ImportShipment,
        costVarianceBdtMinor: Long,
        oldTotalExpenseBdtMinor: Long,
        newTotalExpenseBdtMinor: Long,
        reason: String,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "COST VARIANCE REPORT",
            documentNumber = "VAR-${shipment.shipmentNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIPMENT REFERENCE: ${shipment.shipmentNumber}", 36f, y, paint)
        canvas.drawText("ORIGIN / DEST: ${shipment.origin} -> ${shipment.destination}", 36f, y + 16f, paint)
        canvas.drawText("ADJUSTMENT REASON: $reason", 36f, y + 32f, paint)

        y += 60f
        // Summary Table
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 26f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COST METRIC", 44f, y + 17f, paint)
        canvas.drawText("AMOUNT (BDT)", PAGE_WIDTH - 150f, y + 17f, paint)

        y += 26f
        val metrics = listOf(
            "Original Estimated Import Cost" to oldTotalExpenseBdtMinor,
            "Actual Incurred Import Cost" to newTotalExpenseBdtMinor,
            "Landed Cost Variance" to costVarianceBdtMinor
        )

        metrics.forEachIndexed { index, (label, amount) ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)
            }
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#2D3748")
            canvas.drawText(label, 44f, y + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (label.contains("Variance")) Color.parseColor("#792B87") else Color.parseColor("#1A202C")
            val amountStr = "৳ " + Formatters.formatMinorUnit(amount)
            canvas.drawText(amountStr, PAGE_WIDTH - 150f, y + 16f, paint)
            y += 24f
        }

        drawSignatures(canvas, paint, "Cost Accountant / Analyst", "Chief Financial Officer (CFO)")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "Variance-${shipment.shipmentNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun generateDamageLossReportPdf(
        context: Context,
        receipt: WarehouseReceipt,
        shipment: ImportShipment,
        items: List<WarehouseReceiptItem>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "DAMAGE & LOSS REPORT",
            documentNumber = "DMR-${receipt.receiptNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(receipt.receiptDate))
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECEIPT REF: ${receipt.receiptNumber}  |  SHIPMENT: ${shipment.shipmentNumber}", 36f, y, paint)
        canvas.drawText("INSPECTED BY: ${receipt.receivedBy}  |  WAREHOUSE: Tejgaon Central WH", 36f, y + 16f, paint)

        y += 45f
        paint.color = Color.parseColor("#FEE2E2")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#991B1B")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT", 44f, y + 16f, paint)
        canvas.drawText("EXPECTED", 240f, y + 16f, paint)
        canvas.drawText("DAMAGED", 320f, y + 16f, paint)
        canvas.drawText("MISSING", 400f, y + 16f, paint)
        canvas.drawText("STATUS / CLAIM", 470f, y + 16f, paint)

        y += 24f
        items.filter { it.damagedQuantity > 0 || it.missingQuantity > 0 }.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[item.productId]
            paint.color = Color.parseColor("#1A202C")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(product?.nameEn ?: item.productId, 44f, y + 15f, paint)
            canvas.drawText("${item.expectedQuantity} pcs", 240f, y + 15f, paint)
            paint.color = Color.parseColor("#C53030")
            canvas.drawText("${item.damagedQuantity} pcs", 320f, y + 15f, paint)
            canvas.drawText("${item.missingQuantity} pcs", 400f, y + 15f, paint)
            paint.color = Color.parseColor("#792B87")
            canvas.drawText("Supplier Claim Pending", 470f, y + 15f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Warehouse Inspector", "Insurance / Claim Officer")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "DamageReport-${receipt.receiptNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun generateInventoryCostLayerReportPdf(
        context: Context,
        layers: List<InventoryCostLayer>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "FIFO COST LAYER VALUATION",
            documentNumber = "ICL-${System.currentTimeMillis() % 10000}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        )

        var y = 140f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT", 44f, y + 16f, paint)
        canvas.drawText("RECEIPT #", 180f, y + 16f, paint)
        canvas.drawText("INITIAL", 270f, y + 16f, paint)
        canvas.drawText("REMAINING", 330f, y + 16f, paint)
        canvas.drawText("UNIT COST", 400f, y + 16f, paint)
        canvas.drawText("LAYER VALUE", 480f, y + 16f, paint)

        y += 24f
        var totalValuation = 0L

        layers.forEachIndexed { index, layer: InventoryCostLayer ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[layer.productId]
            val layerValue: Long = layer.remainingQuantity.toLong() * layer.unitLandedCostBdtMinor
            totalValuation += layerValue

            paint.color = Color.parseColor("#1A202C")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(product?.nameEn ?: layer.productId, 44f, y + 15f, paint)
            canvas.drawText(layer.receiptNumber, 180f, y + 15f, paint)
            canvas.drawText("${layer.initialQuantity}", 270f, y + 15f, paint)
            paint.color = if (layer.remainingQuantity > 0) Color.parseColor("#276749") else Color.parseColor("#718096")
            canvas.drawText("${layer.remainingQuantity}", 330f, y + 15f, paint)
            paint.color = Color.parseColor("#1A202C")
            canvas.drawText("৳ " + Formatters.formatMinorUnit(layer.unitLandedCostBdtMinor), 400f, y + 15f, paint)
            paint.color = Color.parseColor("#792B87")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("৳ " + Formatters.formatMinorUnit(layerValue), 480f, y + 15f, paint)
            y += 22f
        }

        y += 10f
        paint.color = Color.parseColor("#263849")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL INVENTORY VALUATION: ৳ ${Formatters.formatMinorUnit(totalValuation)}", 44f, y + 16f, paint)

        drawSignatures(canvas, paint, "Warehouse Inventory Head", "Internal Audit / Controller")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "CostLayers-Valuation.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun generateCbmReportPdf(
        context: Context,
        shipment: ImportShipment,
        cartons: List<ShipmentCarton>,
        productsMap: Map<String, Product>,
        businessProfile: BusinessProfile?
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawBrandHeader(
            context = context,
            canvas = canvas,
            paint = paint,
            businessProfile = businessProfile,
            documentTitle = "CBM & PACKING REPORT",
            documentNumber = "CBM-${shipment.shipmentNumber}",
            dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(shipment.shippingDate))
        )

        var y = 140f
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIPMENT: ${shipment.shipmentNumber}  |  METHOD: ${shipment.shippingMethod}", 36f, y, paint)
        canvas.drawText("TOTAL CARTONS: ${shipment.totalCartons}  |  TOTAL CBM: ${shipment.totalCbm} m³  |  GROSS WEIGHT: ${shipment.totalGrossWeightKg} kg", 36f, y + 16f, paint)

        y += 45f
        paint.color = Color.parseColor("#EDF2F7")
        canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 24f, paint)

        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CARTON #", 44f, y + 16f, paint)
        canvas.drawText("PRODUCT", 120f, y + 16f, paint)
        canvas.drawText("QTY", 250f, y + 16f, paint)
        canvas.drawText("DIMS (L×W×H cm)", 300f, y + 16f, paint)
        canvas.drawText("WEIGHT (kg)", 420f, y + 16f, paint)
        canvas.drawText("CBM (m³)", 500f, y + 16f, paint)

        y += 24f
        cartons.forEachIndexed { index, ctn ->
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 22f, paint)
            }
            val product = productsMap[ctn.productId]
            paint.color = Color.parseColor("#1A202C")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(ctn.cartonNumber, 44f, y + 15f, paint)
            canvas.drawText(product?.nameEn ?: ctn.productId, 120f, y + 15f, paint)
            canvas.drawText("${ctn.quantity} pcs", 250f, y + 15f, paint)
            canvas.drawText("${ctn.lengthCm}×${ctn.widthCm}×${ctn.heightCm}", 300f, y + 15f, paint)
            canvas.drawText("${ctn.grossWeightKg} / ${ctn.netWeightKg}", 420f, y + 15f, paint)
            paint.color = Color.parseColor("#299DB2")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(ctn.cbm, 500f, y + 15f, paint)
            y += 22f
        }

        drawSignatures(canvas, paint, "Shipping Forwarder / Agent", "Receiving Port Logistics")
        document.finishPage(page)

        val dir = File(context.cacheDir, "import_documents").apply { if (!exists()) mkdirs() }
        val file = File(dir, "CBM-${shipment.shipmentNumber}.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
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
