package com.pocketdev.advance_erp.core.util

import android.content.Context
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ControlCenterExcelGenerator {

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getExcelDir(context: Context): File {
        return File(context.cacheDir, "reports_excel").apply { if (!exists()) mkdirs() }
    }

    fun exportToXlsx(
        context: Context,
        reportTitle: String,
        filterState: ControlCenterFilterState,
        summary: BusinessSummaryDashboardData,
        transactions: List<UnifiedTransactionItem>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        businessProfile: BusinessProfile?,
        canViewProfit: Boolean
    ): File {
        val sanitizedTitle = reportTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(30)
        val file = File(getExcelDir(context), "${sanitizedTitle}_${System.currentTimeMillis()}.xlsx")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val (fromMillis, toMillis) = filterState.dateRangeOption.getBoundsMillis(filterState.customStartDate, filterState.customEndDate)
        val periodStr = if (filterState.dateRangeOption.name == "ALL_TIME" || fromMillis == 0L) {
            "All Time (Inception to Present)"
        } else {
            "${dateOnlyFormat.format(Date(fromMillis))} to ${dateOnlyFormat.format(Date(kotlin.math.min(toMillis, System.currentTimeMillis())))}"
        }

        // Build XML content for 4 Sheets:
        // 1. Summary
        // 2. Transactions
        // 3. Customers
        // 4. Suppliers

        val sheet1Xml = buildSummarySheetXml(businessProfile, reportTitle, periodStr, summary, canViewProfit, dateFormat)
        val sheet2Xml = buildTransactionsSheetXml(transactions, dateFormat)
        val sheet3Xml = buildCustomersSheetXml(customers, transactions)
        val sheet4Xml = buildSuppliersSheetXml(suppliers, transactions)

        // Write OpenXML Zip Archive
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            // _rels/.rels
            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            // xl/_rels/workbook.xml.rels
            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)

            // xl/workbook.xml
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Executive Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Transactions &amp; Ledger" sheetId="2" r:id="rId2"/>
    <sheet name="Customer Ledger" sheetId="3" r:id="rId3"/>
    <sheet name="Supplier Ledger" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            // xl/styles.xml
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
            writeZipEntry(zos, "xl/styles.xml", stylesXml)

            // Worksheets
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", sheet2Xml)
            writeZipEntry(zos, "xl/worksheets/sheet3.xml", sheet3Xml)
            writeZipEntry(zos, "xl/worksheets/sheet4.xml", sheet4Xml)
        }

        return file
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun buildSummarySheetXml(
        businessProfile: BusinessProfile?,
        reportTitle: String,
        periodStr: String,
        summary: BusinessSummaryDashboardData,
        canViewProfit: Boolean,
        dateFormat: SimpleDateFormat
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")
        var row = 1
        fun addRow(c1: String, c2: String, isBold: Boolean = false) {
            val s = if (isBold) " s=\"1\"" else ""
            sb.append("    <row r=\"$row\">")
            sb.append("<c r=\"A$row\" t=\"inlineStr\"$s><is><t>${escapeXml(c1)}</t></is></c>")
            sb.append("<c r=\"B$row\" t=\"inlineStr\"$s><is><t>${escapeXml(c2)}</t></is></c>")
            sb.append("</row>\n")
            row++
        }

        fun addNumRow(c1: String, amountMinor: Long, isBold: Boolean = false) {
            val s = if (isBold) " s=\"1\"" else ""
            sb.append("    <row r=\"$row\">")
            sb.append("<c r=\"A$row\" t=\"inlineStr\"$s><is><t>${escapeXml(c1)}</t></is></c>")
            sb.append("<c r=\"B$row\" t=\"n\"$s><v>${amountMinor / 100.0}</v></c>")
            sb.append("</row>\n")
            row++
        }

        addRow("ADVANCE ERP BUSINESS CONTROL CENTER", "EXECUTIVE SUMMARY", isBold = true)
        addRow("Company Name", businessProfile?.companyName ?: "ADVANCE ERP ENTERPRISE")
        addRow("Address", businessProfile?.address ?: "Dhaka, Bangladesh")
        addRow("Contact", businessProfile?.phone ?: "+880 1700-000000")
        addRow("Report Title", reportTitle)
        addRow("Selected Period", periodStr)
        addRow("Generated On", dateFormat.format(Date()))
        addRow("", "")

        addRow("FINANCIAL & OPERATIONAL SUMMARY", "AMOUNT (BDT)", isBold = true)
        addNumRow("Total Sales Revenue (Gross)", summary.totalSalesMinor)
        addNumRow("Less: Total Sales Returns", -summary.totalSalesReturnMinor)
        addNumRow("Total Purchase Orders (BDT)", summary.totalPurchaseMinor)
        addNumRow("Total Customer Collections (Received)", summary.totalReceivedMinor)
        addNumRow("Total Supplier Payments (Paid)", summary.totalPaidMinor)
        addNumRow("Total Operating Expenses", summary.totalExpensesMinor)
        addNumRow("Total Outstanding Receivable (Customer Due)", summary.totalOutstandingReceivableMinor)
        addNumRow("Total Outstanding Payable (Supplier Due)", summary.totalOutstandingPayableMinor)

        if (canViewProfit) {
            addNumRow("Gross Profit", summary.grossProfitMinor, isBold = true)
            addNumRow("Net Profit", summary.netProfitMinor, isBold = true)
        } else {
            addRow("Gross / Net Profit", "Hidden (Owner / Admin Only)")
        }

        addNumRow("Net Cash Flow (Received - Paid - Expenses)", summary.totalReceivedMinor - summary.totalPaidMinor - summary.totalExpensesMinor, isBold = true)
        addRow("Total Completed Deliveries", "${summary.totalDeliveryCount} Orders")
        addRow("Total System Transactions", "${summary.totalTransactionsCount} Records")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildTransactionsSheetXml(
        transactions: List<UnifiedTransactionItem>,
        dateFormat: SimpleDateFormat
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="inlineStr" s="1"><is><t>Date</t></is></c>
      <c r="B1" t="inlineStr" s="1"><is><t>Reference ID</t></is></c>
      <c r="C1" t="inlineStr" s="1"><is><t>Category</t></is></c>
      <c r="D1" t="inlineStr" s="1"><is><t>Party / Entity</t></is></c>
      <c r="E1" t="inlineStr" s="1"><is><t>Particulars / Details</t></is></c>
      <c r="F1" t="inlineStr" s="1"><is><t>Payment Method</t></is></c>
      <c r="G1" t="inlineStr" s="1"><is><t>Status</t></is></c>
      <c r="H1" t="inlineStr" s="1"><is><t>Warehouse</t></is></c>
      <c r="I1" t="inlineStr" s="1"><is><t>Inflow / Debit (BDT)</t></is></c>
      <c r="J1" t="inlineStr" s="1"><is><t>Outflow / Credit (BDT)</t></is></c>
      <c r="K1" t="inlineStr" s="1"><is><t>Running Balance (BDT)</t></is></c>
    </row>
""")
        var r = 2
        for (item in transactions) {
            val dateStr = dateFormat.format(Date(item.date))
            val debitVal = if (item.isInflow) item.amountMinor / 100.0 else 0.0
            val creditVal = if (!item.isInflow) item.amountMinor / 100.0 else 0.0
            val balVal = (item.runningBalanceMinor ?: item.amountMinor) / 100.0

            sb.append("    <row r=\"$r\">")
            sb.append("<c r=\"A$r\" t=\"inlineStr\"><is><t>${escapeXml(dateStr)}</t></is></c>")
            sb.append("<c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(item.referenceNumber)}</t></is></c>")
            sb.append("<c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(item.category.titleEn)}</t></is></c>")
            sb.append("<c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(item.primaryEntityName)}</t></is></c>")
            sb.append("<c r=\"E$r\" t=\"inlineStr\"><is><t>${escapeXml(item.particulars)}</t></is></c>")
            sb.append("<c r=\"F$r\" t=\"inlineStr\"><is><t>${escapeXml(item.paymentMethod ?: "-")}</t></is></c>")
            sb.append("<c r=\"G$r\" t=\"inlineStr\"><is><t>${escapeXml(item.status)}</t></is></c>")
            sb.append("<c r=\"H$r\" t=\"inlineStr\"><is><t>${escapeXml(item.warehouseName ?: "-")}</t></is></c>")
            sb.append("<c r=\"I$r\" t=\"n\"><v>$debitVal</v></c>")
            sb.append("<c r=\"J$r\" t=\"n\"><v>$creditVal</v></c>")
            sb.append("<c r=\"K$r\" t=\"n\"><v>$balVal</v></c>")
            sb.append("</row>\n")
            r++
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildCustomersSheetXml(
        customers: List<Customer>,
        transactions: List<UnifiedTransactionItem>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="inlineStr" s="1"><is><t>Customer Code</t></is></c>
      <c r="B1" t="inlineStr" s="1"><is><t>Customer Name</t></is></c>
      <c r="C1" t="inlineStr" s="1"><is><t>Phone</t></is></c>
      <c r="D1" t="inlineStr" s="1"><is><t>Address</t></is></c>
      <c r="E1" t="inlineStr" s="1"><is><t>Total Billed (BDT)</t></is></c>
      <c r="F1" t="inlineStr" s="1"><is><t>Total Collected (BDT)</t></is></c>
      <c r="G1" t="inlineStr" s="1"><is><t>Outstanding Balance (BDT)</t></is></c>
    </row>
""")
        var r = 2
        for (c in customers) {
            val billed = transactions.filter { it.primaryEntityId == c.id && it.rawType == "INVOICE" }.sumOf { it.amountMinor } / 100.0
            val collected = transactions.filter { it.primaryEntityId == c.id && it.rawType == "PAYMENT" }.sumOf { it.amountMinor } / 100.0
            val balance = c.outstandingBalanceMinor / 100.0

            sb.append("    <row r=\"$r\">")
            sb.append("<c r=\"A$r\" t=\"inlineStr\"><is><t>${escapeXml(c.customerCode)}</t></is></c>")
            sb.append("<c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(c.name)}</t></is></c>")
            sb.append("<c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(c.phone)}</t></is></c>")
            sb.append("<c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(c.address)}</t></is></c>")
            sb.append("<c r=\"E$r\" t=\"n\"><v>$billed</v></c>")
            sb.append("<c r=\"F$r\" t=\"n\"><v>$collected</v></c>")
            sb.append("<c r=\"G$r\" t=\"n\"><v>$balance</v></c>")
            sb.append("</row>\n")
            r++
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildSuppliersSheetXml(
        suppliers: List<Supplier>,
        transactions: List<UnifiedTransactionItem>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="inlineStr" s="1"><is><t>Supplier Code</t></is></c>
      <c r="B1" t="inlineStr" s="1"><is><t>Company Name</t></is></c>
      <c r="C1" t="inlineStr" s="1"><is><t>Country</t></is></c>
      <c r="D1" t="inlineStr" s="1"><is><t>Phone</t></is></c>
      <c r="E1" t="inlineStr" s="1"><is><t>Total Orders (BDT)</t></is></c>
      <c r="F1" t="inlineStr" s="1"><is><t>Total Paid (BDT)</t></is></c>
      <c r="G1" t="inlineStr" s="1"><is><t>Outstanding Payable (BDT)</t></is></c>
    </row>
""")
        var r = 2
        for (s in suppliers) {
            val ordered = transactions.filter { it.primaryEntityId == s.id && it.rawType == "PURCHASE" }.sumOf { it.amountMinor } / 100.0
            val paid = transactions.filter { it.primaryEntityId == s.id && it.rawType == "SUPPLIER_PAYMENT" }.sumOf { it.amountMinor } / 100.0
            val payable = s.outstandingPayableMinor / 100.0

            sb.append("    <row r=\"$r\">")
            sb.append("<c r=\"A$r\" t=\"inlineStr\"><is><t>${escapeXml(s.supplierCode)}</t></is></c>")
            sb.append("<c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(s.companyName)}</t></is></c>")
            sb.append("<c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(s.country)}</t></is></c>")
            sb.append("<c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(s.phone)}</t></is></c>")
            sb.append("<c r=\"E$r\" t=\"n\"><v>$ordered</v></c>")
            sb.append("<c r=\"F$r\" t=\"n\"><v>$paid</v></c>")
            sb.append("<c r=\"G$r\" t=\"n\"><v>$payable</v></c>")
            sb.append("</row>\n")
            r++
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    fun exportToCsv(
        context: Context,
        reportTitle: String,
        transactions: List<UnifiedTransactionItem>
    ): File {
        val sanitizedTitle = reportTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(30)
        val file = File(getExcelDir(context), "${sanitizedTitle}_${System.currentTimeMillis()}.csv")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        FileWriter(file).use { writer ->
            writer.append("Date,Reference ID,Category,Party / Entity,Particulars,Method,Status,Warehouse,Debit BDT,Credit BDT,Running Balance BDT\n")
            for (t in transactions) {
                val d = dateFormat.format(Date(t.date))
                val debit = if (t.isInflow) t.amountMinor / 100.0 else 0.0
                val credit = if (!t.isInflow) t.amountMinor / 100.0 else 0.0
                val bal = (t.runningBalanceMinor ?: t.amountMinor) / 100.0
                writer.append("\"$d\",\"${t.referenceNumber}\",\"${t.category.titleEn}\",\"${t.primaryEntityName.replace("\"", "\"\"")}\",\"${t.particulars.replace("\"", "\"\"")}\",\"${t.paymentMethod ?: ""}\",\"${t.status}\",\"${t.warehouseName ?: ""}\",$debit,$credit,$bal\n")
            }
        }
        return file
    }
}
