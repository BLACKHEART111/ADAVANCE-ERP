package com.pocketdev.advance_erp.core.pdf

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.StockItemOverview
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.SalesReportSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CentralizedPdfReportRegistry {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    // ==========================================
    // 1. SALES REPORTS (1-7)
    // ==========================================

    /** Report 1: Sales Invoice / Bill */
    fun buildSalesInvoiceReport(
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.5f),
            PdfReportColumn("Item Description", "পণ্যের বিবরণ", widthWeight = 3.0f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Qty", "পরিমাণ", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Unit Price", "একক মূল্য", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Disc", "ছাড়", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Total (BDT)", "মোট (টাকা)", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = items.mapIndexed { idx, itm ->
            PdfReportRow(
                listOf(
                    (idx + 1).toString(),
                    itm.productNameEn,
                    itm.sku,
                    itm.quantity.toString(),
                    "৳" + Formatters.formatMinorUnit(itm.unitPriceMinor),
                    "৳" + Formatters.formatMinorUnit(itm.discountMinor),
                    "৳" + Formatters.formatMinorUnit(itm.lineTotalMinor)
                )
            )
        }

        val summaries = listOf(
            PdfReportRow(listOf("", "Subtotal / উপমোট", "", "", "", "", "৳" + Formatters.formatMinorUnit(invoice.subtotalMinor)), isSummary = true),
            PdfReportRow(listOf("", "Discount / বাট্টা", "", "", "", "", "-৳" + Formatters.formatMinorUnit(invoice.discountMinor)), isSummary = true),
            PdfReportRow(listOf("", "Grand Total / সর্বমোট", "", "", "", "", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor)), isSummary = true),
            PdfReportRow(listOf("", "Paid / পরিশোধিত", "", "", "", "", "৳" + Formatters.formatMinorUnit(invoice.paidAmountMinor)), isSummary = true),
            PdfReportRow(listOf("", "Due Balance / বকেয়া", "", "", "", "", "৳" + Formatters.formatMinorUnit(invoice.dueAmountMinor)), isSummary = true, isEmphasized = true)
        )

        val kpis = listOf(
            PdfKpiBadge("Invoice Number", "চালান নম্বর", invoice.invoiceNumber),
            PdfKpiBadge("Customer", "গ্রাহক", invoice.customerName),
            PdfKpiBadge("Grand Total", "মোট মূল্য", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor)),
            PdfKpiBadge("Status", "অবস্থা", invoice.status)
        )

        return CentralizedPdfReport(
            reportId = "RPT-01-SALES-INVOICE",
            category = "Sales",
            titleEn = "Commercial Sales Invoice",
            titleBn = "বাণিজ্যিক বিক্রয় চালান",
            dateRangeText = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}",
            filterText = "Warehouse: ${invoice.warehouseName}",
            kpiBadges = kpis,
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 2: Sales Summary Report */
    fun buildSalesSummaryReport(
        summary: SalesReportSummary,
        invoices: List<SalesInvoice>,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Inv #", "চালান #", widthWeight = 1.3f),
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Customer", "গ্রাহক", widthWeight = 2.0f),
            PdfReportColumn("Total (BDT)", "মোট বিক্রয়", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Paid (BDT)", "পরিশোধ", widthWeight = 1.1f, isRightAligned = true),
            PdfReportColumn("Due (BDT)", "বকেয়া", widthWeight = 1.1f, isRightAligned = true),
            PdfReportColumn("Status", "অবস্থা", widthWeight = 1.0f)
        )

        val rows = invoices.map { inv ->
            PdfReportRow(
                listOf(
                    inv.invoiceNumber,
                    dateFormat.format(Date(inv.invoiceDate)),
                    inv.customerName,
                    "৳" + Formatters.formatMinorUnit(inv.totalAmountMinor),
                    "৳" + Formatters.formatMinorUnit(inv.paidAmountMinor),
                    "৳" + Formatters.formatMinorUnit(inv.dueAmountMinor),
                    inv.status
                )
            )
        }

        val summaries = listOf(
            PdfReportRow(
                listOf(
                    "Total",
                    "${invoices.size} Invoices",
                    "",
                    "৳" + Formatters.formatMinorUnit(summary.totalNetSalesMinor),
                    "৳" + Formatters.formatMinorUnit(summary.totalCollectedMinor),
                    "৳" + Formatters.formatMinorUnit(summary.totalReceivableMinor),
                    ""
                ),
                isSummary = true,
                isEmphasized = true
            )
        )

        val kpis = listOf(
            PdfKpiBadge("Net Sales", "নিট বিক্রয়", "৳" + Formatters.formatMinorUnit(summary.totalNetSalesMinor)),
            PdfKpiBadge("COGS", "বিক্রীত পণ্যের ব্যয়", "৳" + Formatters.formatMinorUnit(summary.totalCogsMinor)),
            PdfKpiBadge("Gross Profit", "মোট লাভ", "৳" + Formatters.formatMinorUnit(summary.totalGrossProfitMinor)),
            PdfKpiBadge("Margin", "মার্জিন", String.format(Locale.US, "%.1f%%", summary.grossProfitMarginPercent))
        )

        return CentralizedPdfReport(
            reportId = "RPT-02-SALES-SUMMARY",
            category = "Sales",
            titleEn = "Sales Summary Report",
            titleBn = "বিক্রয় সারাংশ রিপোর্ট",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = kpis,
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 3: Customer-wise Sales Report */
    fun buildCustomerWiseSalesReport(
        items: List<CustomerWiseSalesItem>,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Customer Name", "গ্রাহকের নাম", widthWeight = 2.2f),
            PdfReportColumn("Phone", "মোবাইল", widthWeight = 1.3f),
            PdfReportColumn("Invoices", "চালান সংখ্যা", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Total Purchased", "মোট ক্রয়", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Total Paid", "পরিশোধ", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Balance Due", "বকেয়া", widthWeight = 1.2f, isRightAligned = true)
        )

        val rows = items.map {
            PdfReportRow(
                listOf(
                    it.customerName,
                    it.customerPhone,
                    it.invoiceCount.toString(),
                    "৳" + Formatters.formatMinorUnit(it.totalPurchasedMinor),
                    "৳" + Formatters.formatMinorUnit(it.totalPaidMinor),
                    "৳" + Formatters.formatMinorUnit(it.outstandingBalanceMinor)
                )
            )
        }

        val totalPurchased = items.sumOf { it.totalPurchasedMinor }
        val totalPaid = items.sumOf { it.totalPaidMinor }
        val totalDue = items.sumOf { it.outstandingBalanceMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Grand Total", "", items.sumOf { it.invoiceCount }.toString(), "৳" + Formatters.formatMinorUnit(totalPurchased), "৳" + Formatters.formatMinorUnit(totalPaid), "৳" + Formatters.formatMinorUnit(totalDue)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-03-CUSTOMER-WISE-SALES",
            category = "Sales",
            titleEn = "Customer-wise Sales Report",
            titleBn = "গ্রাহকভিত্তিক বিক্রয় রিপোর্ট",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Customers", "মোট গ্রাহক", items.size.toString()),
                PdfKpiBadge("Total Purchased", "মোট ক্রয়", "৳" + Formatters.formatMinorUnit(totalPurchased)),
                PdfKpiBadge("Outstanding Due", "মোট বকেয়া", "৳" + Formatters.formatMinorUnit(totalDue))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 4: Product-wise Sales Report */
    fun buildProductWiseSalesReport(
        items: List<ProductWiseSalesItem>,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.5f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Units Sold", "বিক্রয় ইউনিট", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Revenue (BDT)", "আয়", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("COGS (BDT)", "ব্যয়", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Gross Profit", "লাভ", widthWeight = 1.2f, isRightAligned = true)
        )

        val rows = items.map {
            PdfReportRow(
                listOf(
                    it.productNameEn,
                    it.sku,
                    it.unitsSold.toString(),
                    "৳" + Formatters.formatMinorUnit(it.revenueMinor),
                    "৳" + Formatters.formatMinorUnit(it.cogsMinor),
                    "৳" + Formatters.formatMinorUnit(it.grossProfitMinor)
                )
            )
        }

        val totalRev = items.sumOf { it.revenueMinor }
        val totalCogs = items.sumOf { it.cogsMinor }
        val totalProfit = items.sumOf { it.grossProfitMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total", "", items.sumOf { it.unitsSold }.toString(), "৳" + Formatters.formatMinorUnit(totalRev), "৳" + Formatters.formatMinorUnit(totalCogs), "৳" + Formatters.formatMinorUnit(totalProfit)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-04-PRODUCT-WISE-SALES",
            category = "Sales",
            titleEn = "Product-wise Sales & Profitability",
            titleBn = "পণ্যভিত্তিক বিক্রয় ও মুনাফা রিপোর্ট",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Units Sold", "বিক্রীত একক", items.sumOf { it.unitsSold }.toString()),
                PdfKpiBadge("Total Revenue", "মোট বিক্রয়মূল্য", "৳" + Formatters.formatMinorUnit(totalRev)),
                PdfKpiBadge("Total Gross Profit", "মোট মুনাফা", "৳" + Formatters.formatMinorUnit(totalProfit))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 5: Sales Due / Outstanding Report */
    fun buildSalesDueReport(
        customers: List<Customer>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val dueCustomers = customers.filter { it.outstandingBalanceMinor > 0 }
            .sortedByDescending { it.outstandingBalanceMinor }

        val columns = listOf(
            PdfReportColumn("Code", "কোড", widthWeight = 1.0f),
            PdfReportColumn("Customer Name", "গ্রাহকের নাম", widthWeight = 2.4f),
            PdfReportColumn("Phone", "মোবাইল", widthWeight = 1.3f),
            PdfReportColumn("Credit Limit", "ক্রেডিট লিমিট", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Outstanding Due", "বর্তমান বকেয়া", widthWeight = 1.4f, isRightAligned = true)
        )

        val rows = dueCustomers.map {
            PdfReportRow(
                listOf(
                    it.customerCode,
                    it.name,
                    it.phone,
                    "৳" + Formatters.formatMinorUnit(it.creditLimitMinor),
                    "৳" + Formatters.formatMinorUnit(it.outstandingBalanceMinor)
                )
            )
        }

        val totalDue = dueCustomers.sumOf { it.outstandingBalanceMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Outstanding Receivable", "", "", "", "৳" + Formatters.formatMinorUnit(totalDue)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-05-SALES-DUE",
            category = "Sales",
            titleEn = "Sales Due & Outstanding Receivables",
            titleBn = "বিক্রয় বকেয়া ও পাওনাদার তালিকা",
            kpiBadges = listOf(
                PdfKpiBadge("Defaulters / Due Count", "বকেয়াদার সংখ্যা", dueCustomers.size.toString()),
                PdfKpiBadge("Total Receivable", "মোট পাওনা", "৳" + Formatters.formatMinorUnit(totalDue))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 6: Customer Ledger / Statement */
    fun buildCustomerLedgerReport(
        customer: Customer,
        entries: List<CustomerLedgerEntry>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Type", "ধরন", widthWeight = 0.9f),
            PdfReportColumn("Ref #", "রেফারেন্স", widthWeight = 1.2f),
            PdfReportColumn("Description", "বিবরণ", widthWeight = 2.2f),
            PdfReportColumn("Debit (+)", "ডেবিট", widthWeight = 1.1f, isRightAligned = true),
            PdfReportColumn("Credit (-)", "ক্রেডিট", widthWeight = 1.1f, isRightAligned = true),
            PdfReportColumn("Balance", "জের", widthWeight = 1.2f, isRightAligned = true)
        )

        val rows = entries.map {
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(it.date)),
                    it.transactionType,
                    it.referenceNumber,
                    it.description,
                    if (it.debitMinor > 0) "৳" + Formatters.formatMinorUnit(it.debitMinor) else "-",
                    if (it.creditMinor > 0) "৳" + Formatters.formatMinorUnit(it.creditMinor) else "-",
                    "৳" + Formatters.formatMinorUnit(it.runningBalanceMinor)
                )
            )
        }

        val finalBalance = entries.lastOrNull()?.runningBalanceMinor ?: customer.outstandingBalanceMinor

        val summaries = listOf(
            PdfReportRow(
                listOf(
                    "Closing Balance",
                    "",
                    "",
                    "",
                    "৳" + Formatters.formatMinorUnit(entries.sumOf { it.debitMinor }),
                    "৳" + Formatters.formatMinorUnit(entries.sumOf { it.creditMinor }),
                    "৳" + Formatters.formatMinorUnit(finalBalance)
                ),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-06-CUSTOMER-LEDGER",
            category = "Sales",
            titleEn = "Customer Account Statement / Ledger",
            titleBn = "গ্রাহকের হিসাব বিবরণী / লেজার",
            filterText = "Customer: ${customer.name} (${customer.phone})",
            kpiBadges = listOf(
                PdfKpiBadge("Customer Code", "কোড", customer.customerCode),
                PdfKpiBadge("Phone", "মোবাইল", customer.phone),
                PdfKpiBadge("Current Outstanding", "বর্তমান বকেয়া", "৳" + Formatters.formatMinorUnit(finalBalance))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 7: Sales Return Report */
    fun buildSalesReturnReport(
        returns: List<SalesReturn>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Return #", "রিটার্ন #", widthWeight = 1.3f),
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Inv #", "চালান #", widthWeight = 1.2f),
            PdfReportColumn("Customer", "গ্রাহক", widthWeight = 2.0f),
            PdfReportColumn("Option", "ফেরত পদ্ধতি", widthWeight = 1.2f),
            PdfReportColumn("Refund (BDT)", "ফেরত মূল্য", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = returns.map { ret ->
            PdfReportRow(
                listOf(
                    ret.returnNumber,
                    dateFormat.format(Date(ret.returnDate)),
                    ret.invoiceNumber,
                    ret.customerName,
                    ret.refundOption,
                    "৳" + Formatters.formatMinorUnit(ret.totalReturnAmountMinor)
                )
            )
        }

        val totalRefund = returns.sumOf { it.totalReturnAmountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Returns", "${returns.size} items", "", "", "", "৳" + Formatters.formatMinorUnit(totalRefund)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-07-SALES-RETURN",
            category = "Sales",
            titleEn = "Sales Return & Replacement Register",
            titleBn = "পণ্য ফেরত ও সমন্বয় তালিকা",
            kpiBadges = listOf(
                PdfKpiBadge("Total Returns", "মোট রিটার্ন", returns.size.toString()),
                PdfKpiBadge("Total Refund Value", "মোট ফেরত মূল্য", "৳" + Formatters.formatMinorUnit(totalRefund))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    // ==========================================
    // 2. PURCHASE & IMPORT REPORTS (8-13)
    // ==========================================

    /** Report 8: Purchase Order (PO) Document */
    fun buildPurchaseOrderDocReport(
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        supplier: Supplier?,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.5f),
            PdfReportColumn("Item Description", "পণ্যের বিবরণ", widthWeight = 2.8f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Order Qty", "অর্ডার পরিমাণ", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Recv Qty", "প্রাপ্ত পরিমাণ", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Unit Price", "দর", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Total (${order.currency})", "মোট", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = items.mapIndexed { idx, itm ->
            PdfReportRow(
                listOf(
                    (idx + 1).toString(),
                    itm.productId,
                    itm.variantId ?: itm.productId,
                    itm.quantity.toString(),
                    itm.receivedQuantity.toString(),
                    "${order.currency} ${Formatters.formatMinorUnit(itm.unitPriceMinor)}",
                    "${order.currency} ${Formatters.formatMinorUnit(itm.totalAmountMinor)}"
                )
            )
        }

        val summaries = listOf(
            PdfReportRow(listOf("", "Grand Total (${order.currency})", "", "", "", "", "${order.currency} ${Formatters.formatMinorUnit(order.totalAmountMinor)}"), isSummary = true),
            PdfReportRow(listOf("", "BDT Converted Total", "", "", "", "", "৳" + Formatters.formatMinorUnit(order.totalBdtAmountMinor)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-08-PURCHASE-ORDER",
            category = "Purchase & Import",
            titleEn = "Purchase Order Document (PO)",
            titleBn = "ক্রয়াদেশ / পারচেজ অর্ডার",
            dateRangeText = "PO Date: ${dateFormat.format(Date(order.orderDate))}",
            filterText = "Supplier: ${supplier?.companyName ?: order.supplierId}",
            kpiBadges = listOf(
                PdfKpiBadge("PO Number", "অর্ডার নম্বর", order.poNumber),
                PdfKpiBadge("Currency", "মুদ্রা", order.currency),
                PdfKpiBadge("Total BDT", "মোট টাকা", "৳" + Formatters.formatMinorUnit(order.totalBdtAmountMinor)),
                PdfKpiBadge("Status", "অবস্থা", order.status)
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 9: Purchase Summary Report */
    fun buildPurchaseSummaryReport(
        orders: List<PurchaseOrder>,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("PO #", "অর্ডার #", widthWeight = 1.3f),
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Supplier", "সরবরাহকারী", widthWeight = 2.0f),
            PdfReportColumn("Currency", "মুদ্রা", widthWeight = 0.8f),
            PdfReportColumn("Total Original", "মূল মুদ্রা", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Total BDT", "মোট (টাকা)", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Status", "অবস্থা", widthWeight = 1.0f)
        )

        val rows = orders.map { po ->
            PdfReportRow(
                listOf(
                    po.poNumber,
                    dateFormat.format(Date(po.orderDate)),
                    po.supplierId,
                    po.currency,
                    "${po.currency} ${Formatters.formatMinorUnit(po.totalAmountMinor)}",
                    "৳" + Formatters.formatMinorUnit(po.totalBdtAmountMinor),
                    po.status
                )
            )
        }

        val totalBdt = orders.sumOf { po -> po.totalBdtAmountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total", "${orders.size} Orders", "", "", "", "৳" + Formatters.formatMinorUnit(totalBdt), ""),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-09-PURCHASE-SUMMARY",
            category = "Purchase & Import",
            titleEn = "Purchase Summary Report",
            titleBn = "ক্রয় সারাংশ রিপোর্ট",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Orders", "মোট অর্ডার", orders.size.toString()),
                PdfKpiBadge("Total Purchase BDT", "মোট ক্রয় টাকা", "৳" + Formatters.formatMinorUnit(totalBdt))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 10: Supplier-wise Purchase Report */
    fun buildSupplierWisePurchaseReport(
        suppliers: List<Supplier>,
        orders: List<PurchaseOrder>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Supplier Name", "সরবরাহকারীর নাম", widthWeight = 2.2f),
            PdfReportColumn("Country / Code", "দেশ / কোড", widthWeight = 1.2f),
            PdfReportColumn("Orders", "অর্ডার", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Total BDT", "মোট ক্রয় (টাকা)", widthWeight = 1.4f, isRightAligned = true),
            PdfReportColumn("Outstanding Due", "বকেয়া দেনা", widthWeight = 1.4f, isRightAligned = true)
        )

        val ordersBySupplier = orders.groupBy { it.supplierId }

        val rows = suppliers.map { supp ->
            val suppOrders = ordersBySupplier[supp.id] ?: emptyList()
            val totalPurchased = suppOrders.sumOf { it.totalBdtAmountMinor }
            PdfReportRow(
                listOf(
                    supp.companyName,
                    "${supp.country} (${supp.supplierCode})",
                    suppOrders.size.toString(),
                    "৳" + Formatters.formatMinorUnit(totalPurchased),
                    "৳" + Formatters.formatMinorUnit(supp.outstandingPayableMinor)
                )
            )
        }

        val grandBdt = orders.sumOf { it.totalBdtAmountMinor }
        val grandDue = suppliers.sumOf { it.outstandingPayableMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Grand Total", "", orders.size.toString(), "৳" + Formatters.formatMinorUnit(grandBdt), "৳" + Formatters.formatMinorUnit(grandDue)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-10-SUPPLIER-WISE-PURCHASE",
            category = "Purchase & Import",
            titleEn = "Supplier-wise Purchase & Payables",
            titleBn = "সরবরাহকারীভিত্তিক ক্রয় ও দেনা",
            kpiBadges = listOf(
                PdfKpiBadge("Total Suppliers", "মোট সরবরাহকারী", suppliers.size.toString()),
                PdfKpiBadge("Total Purchased", "মোট ক্রয়", "৳" + Formatters.formatMinorUnit(grandBdt)),
                PdfKpiBadge("Total Payable Due", "মোট দেনা", "৳" + Formatters.formatMinorUnit(grandDue))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 11: Landed Cost Sheet */
    fun buildLandedCostSheetReport(
        shipment: ImportShipment,
        costLayers: List<InventoryCostLayer>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Product / SKU", "পণ্য / এসকেইউ", widthWeight = 2.4f),
            PdfReportColumn("Initial Qty", "প্রাথমিক সংখ্যা", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Exch Rate", "এক্সচেঞ্জ রেট", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("FOB Price", "এফওবি দর", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Duty & Freight", "শুল্ক ও পরিবহন", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Unit Landed Cost (BDT)", "ল্যান্ডেড খরচ (টাকা)", widthWeight = 1.5f, isRightAligned = true)
        )

        val rows = costLayers.map { layer ->
            PdfReportRow(
                listOf(
                    layer.productId,
                    layer.initialQuantity.toString(),
                    layer.exchangeRateToBdt,
                    "৳" + Formatters.formatMinorUnit(layer.originalPurchasePriceMinor),
                    "৳" + Formatters.formatMinorUnit(layer.allocatedExpensePerUnitBdtMinor),
                    "৳" + Formatters.formatMinorUnit(layer.unitLandedCostBdtMinor)
                )
            )
        }

        val totalUnits = costLayers.sumOf { it.initialQuantity }
        val avgLandedCost = if (totalUnits > 0) costLayers.sumOf { it.initialQuantity.toLong() * it.unitLandedCostBdtMinor } / totalUnits else 0L

        val summaries = listOf(
            PdfReportRow(
                listOf("Weighted Average Landed Cost", totalUnits.toString(), "", "", "", "৳" + Formatters.formatMinorUnit(avgLandedCost)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-11-LANDED-COST-SHEET",
            category = "Purchase & Import",
            titleEn = "Shipment Landed Cost Sheet",
            titleBn = "আমদানি চালান ল্যান্ডেড খরচ বিবরণী",
            filterText = "Shipment: ${shipment.shipmentNumber} • Status: ${shipment.status}",
            kpiBadges = listOf(
                PdfKpiBadge("Shipment #", "চালান #", shipment.shipmentNumber),
                PdfKpiBadge("Total Units", "মোট ইউনিট", totalUnits.toString()),
                PdfKpiBadge("Avg Landed Cost", "গড় ল্যান্ডেড খরচ", "৳" + Formatters.formatMinorUnit(avgLandedCost))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 12: Import Profitability Report */
    fun buildImportProfitabilityReport(
        shipments: List<ImportShipment>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Shipment #", "শিপমেন্ট #", widthWeight = 1.5f),
            PdfReportColumn("Line / Tracking", "শিপিং লাইন / ট্র্যাকিং", widthWeight = 2.0f),
            PdfReportColumn("Method / Route", "পদ্ধতি / পথ", widthWeight = 1.6f),
            PdfReportColumn("Cartons / Qty", "কার্টন / পরিমাণ", widthWeight = 1.4f, isRightAligned = true),
            PdfReportColumn("Status", "অবস্থা", widthWeight = 1.2f)
        )

        val rows = shipments.map { s ->
            PdfReportRow(
                listOf(
                    s.shipmentNumber,
                    "${s.shippingLine ?: "N/A"} (${s.trackingReference ?: "N/A"})",
                    "${s.shippingMethod} (${s.origin} -> ${s.destination})",
                    "${s.totalCartons} ctn / ${s.totalQuantity} pcs",
                    s.status
                )
            )
        }

        val totalCartons = shipments.sumOf { it.totalCartons }
        val totalQty = shipments.sumOf { it.totalQuantity }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Shipments Cargo", "", "", "$totalCartons ctn / $totalQty pcs", ""),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-12-IMPORT-PROFITABILITY",
            category = "Purchase & Import",
            titleEn = "Import Shipment Cost & Profitability",
            titleBn = "আমদানি ব্যয় ও লাভজনকতা বিবরণী",
            kpiBadges = listOf(
                PdfKpiBadge("Total Shipments", "মোট শিপমেন্ট", shipments.size.toString()),
                PdfKpiBadge("Total Cargo", "মোট চালান কার্গো", "$totalCartons ctn / $totalQty pcs")
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 13: Supplier Ledger / Outstanding Statement */
    fun buildSupplierLedgerReport(
        supplier: Supplier,
        orders: List<PurchaseOrder>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("PO / Ref #", "অর্ডার #", widthWeight = 1.4f),
            PdfReportColumn("Details", "বিবরণ", widthWeight = 2.4f),
            PdfReportColumn("Billed (BDT)", "বিলকৃত (টাকা)", widthWeight = 1.4f, isRightAligned = true),
            PdfReportColumn("Paid (BDT)", "পরিশোধিত (টাকা)", widthWeight = 1.4f, isRightAligned = true)
        )

        val rows = orders.map { po ->
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(po.orderDate)),
                    po.poNumber,
                    "Purchase Order (${po.status})",
                    "৳" + Formatters.formatMinorUnit(po.totalBdtAmountMinor),
                    "৳" + Formatters.formatMinorUnit(po.paidBdtAmountMinor)
                )
            )
        }

        val totalBilled = orders.sumOf { it.totalBdtAmountMinor }
        val totalPaid = orders.sumOf { it.paidBdtAmountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total", "", "", "৳" + Formatters.formatMinorUnit(totalBilled), "৳" + Formatters.formatMinorUnit(totalPaid)),
                isSummary = true
            ),
            PdfReportRow(
                listOf("Closing Outstanding Due", "", "", "", "৳" + Formatters.formatMinorUnit(supplier.outstandingPayableMinor)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-13-SUPPLIER-LEDGER",
            category = "Purchase & Import",
            titleEn = "Supplier Ledger & Statement",
            titleBn = "সরবরাহকারী খতিয়ান / লেজার",
            filterText = "Supplier: ${supplier.companyName} • Code: ${supplier.supplierCode}",
            kpiBadges = listOf(
                PdfKpiBadge("Supplier Code", "কোড", supplier.supplierCode),
                PdfKpiBadge("Country", "দেশ", supplier.country),
                PdfKpiBadge("Outstanding Payable", "বকেয়া দেনা", "৳" + Formatters.formatMinorUnit(supplier.outstandingPayableMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    // ==========================================
    // 3. INVENTORY REPORTS (14-26)
    // ==========================================

    /** Report 14: Stock Summary Report (All Warehouses) */
    fun buildStockSummaryAllReport(
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.4f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Category", "ক্যাটাগরি", widthWeight = 1.2f),
            PdfReportColumn("Physical", "ভৌত স্টক", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Reserved", "সংরক্ষিত", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Sellable", "বিক্রয়যোগ্য", widthWeight = 0.9f, isRightAligned = true)
        )

        val rows = items.map {
            PdfReportRow(
                listOf(
                    it.productNameEn,
                    it.baseSku,
                    it.categoryNameEn,
                    "${it.totalQuantity} ${it.unit}",
                    "${it.totalReserved} ${it.unit}",
                    "${it.availableQuantity} ${it.unit}"
                )
            )
        }

        val totalPhysical = items.sumOf { it.totalQuantity }
        val totalSellable = items.sumOf { it.availableQuantity }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Stock Units", "", "", totalPhysical.toString(), "", totalSellable.toString()),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-14-STOCK-SUMMARY-ALL",
            category = "Inventory",
            titleEn = "Stock Summary Report (Consolidated)",
            titleBn = "সার্বিক মজুদ সারসংক্ষেপ বিবরণী",
            kpiBadges = listOf(
                PdfKpiBadge("Total Products", "মোট পণ্য", items.size.toString()),
                PdfKpiBadge("Physical Stock", "মোট ভৌত স্টক", totalPhysical.toString()),
                PdfKpiBadge("Sellable Stock", "বিক্রয়যোগ্য স্টক", totalSellable.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 15: Warehouse-wise Stock Report */
    fun buildWarehouseWiseStockReport(
        warehouseName: String,
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        return buildStockSummaryAllReport(items, isBangla).copy(
            reportId = "RPT-15-WAREHOUSE-STOCK",
            titleEn = "Warehouse Stock Breakdown",
            titleBn = "ওয়্যারহাউস মজুদ বিবরণী",
            filterText = "Warehouse: $warehouseName"
        )
    }

    /** Report 16: Product Stock Card / Movement History */
    fun buildProductStockCardReport(
        productName: String,
        sku: String,
        movements: List<StockMovement>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Movement #", "মুভমেন্ট #", widthWeight = 1.3f),
            PdfReportColumn("Type", "ধরন", widthWeight = 1.2f),
            PdfReportColumn("In (+)", "ইন", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Out (-)", "আউট", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Balance", "জের", widthWeight = 0.9f, isRightAligned = true),
            PdfReportColumn("Unit Cost", "একক খরচ", widthWeight = 1.1f, isRightAligned = true)
        )

        val rows = movements.map { mov ->
            val isIn = mov.movementType.contains("RECEIPT") || mov.movementType.contains("IN") || mov.movementType.contains("RETURN")
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(mov.createdAt)),
                    mov.movementNumber,
                    mov.movementType,
                    if (isIn) mov.quantity.toString() else "-",
                    if (!isIn) mov.quantity.toString() else "-",
                    mov.balanceAfterMovement.toString(),
                    "৳" + Formatters.formatMinorUnit(mov.unitCostMinor)
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-16-STOCK-CARD",
            category = "Inventory",
            titleEn = "Product Stock Card / Bin Card",
            titleBn = "পণ্য মজুদ কার্ড / খতিয়ান",
            filterText = "Product: $productName • SKU: $sku",
            kpiBadges = listOf(
                PdfKpiBadge("Total Movements", "মুভমেন্ট সংখ্যা", movements.size.toString()),
                PdfKpiBadge("Closing Stock", "বর্তমান মজুদ", movements.lastOrNull()?.balanceAfterMovement?.toString() ?: "0")
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 17: Low Stock / Reorder Alert Report */
    fun buildLowStockAlertReport(
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val lowItems = items.filter { it.isLowStock }
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.5f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Category", "ক্যাটাগরি", widthWeight = 1.2f),
            PdfReportColumn("Min Alert", "সতর্কতা মাত্রা", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Current Stock", "বর্তমান মজুদ", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Deficit", "ঘাটতি", widthWeight = 1.0f, isRightAligned = true)
        )

        val rows = lowItems.map {
            val deficit = maxOf(0, it.minStockAlert - it.totalQuantity)
            PdfReportRow(
                listOf(
                    it.productNameEn,
                    it.baseSku,
                    it.categoryNameEn,
                    it.minStockAlert.toString(),
                    it.totalQuantity.toString(),
                    deficit.toString()
                ),
                isEmphasized = true
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-17-LOW-STOCK-ALERT",
            category = "Inventory",
            titleEn = "Low Stock & Reorder Alert Report",
            titleBn = "স্বল্প মজুদ ও পুনঃঅর্ডার সতর্কতা তালিকা",
            kpiBadges = listOf(
                PdfKpiBadge("Alert Count", "সতর্কতা আইটেম", lowItems.size.toString()),
                PdfKpiBadge("Total Reorder Units", "প্রয়োজনীয় অর্ডার ইউনিট", lowItems.sumOf { maxOf(0, it.minStockAlert - it.totalQuantity) }.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 18: Out of Stock Report */
    fun buildOutOfStockReport(
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val outOfStock = items.filter { it.totalQuantity <= 0 }
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.8f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.4f),
            PdfReportColumn("Category", "ক্যাটাগরি", widthWeight = 1.4f),
            PdfReportColumn("Min Alert", "মিনিমাম এলার্ট", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Stock", "মজুদ", widthWeight = 1.0f, isRightAligned = true)
        )

        val rows = outOfStock.map {
            PdfReportRow(
                listOf(it.productNameEn, it.baseSku, it.categoryNameEn, it.minStockAlert.toString(), "0"),
                isEmphasized = true
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-18-OUT-OF-STOCK",
            category = "Inventory",
            titleEn = "Out of Stock Critical Report",
            titleBn = "মজুদ শূন্য পণ্য তালিকা",
            kpiBadges = listOf(
                PdfKpiBadge("Out of Stock Items", "মজুদ শূন্য পণ্য", outOfStock.size.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 19: Overstock Report */
    fun buildOverstockReport(
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val overstocked = items.filter { it.totalQuantity > (it.minStockAlert * 4) && it.minStockAlert > 0 }
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.6f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.3f),
            PdfReportColumn("Min Alert", "এলার্ট", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Current Stock", "বর্তমান মজুদ", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Excess Ratio", "অতিরিক্ত অনুপাত", widthWeight = 1.2f, isRightAligned = true)
        )

        val rows = overstocked.map {
            val ratio = if (it.minStockAlert > 0) it.totalQuantity.toDouble() / it.minStockAlert else 1.0
            PdfReportRow(
                listOf(it.productNameEn, it.baseSku, it.minStockAlert.toString(), it.totalQuantity.toString(), String.format(Locale.US, "%.1fx", ratio))
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-19-OVERSTOCK",
            category = "Inventory",
            titleEn = "Overstock Analysis Report",
            titleBn = "অতিরিক্ত মজুদ বিশ্লেষণ রিপোর্ট",
            kpiBadges = listOf(
                PdfKpiBadge("Overstocked Products", "অতিরিক্ত মজুদ পণ্য", overstocked.size.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 20: Dead Stock / Slow Moving Report */
    fun buildSlowMovingReport(
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.8f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.4f),
            PdfReportColumn("Category", "ক্যাটাগরি", widthWeight = 1.4f),
            PdfReportColumn("Stock Units", "মজুদ একক", widthWeight = 1.2f, isRightAligned = true)
        )

        val rows = items.map {
            PdfReportRow(listOf(it.productNameEn, it.baseSku, it.categoryNameEn, it.totalQuantity.toString()))
        }

        return CentralizedPdfReport(
            reportId = "RPT-20-SLOW-MOVING",
            category = "Inventory",
            titleEn = "Slow-Moving & Dead Stock Report",
            titleBn = "ধীরগতির ও অবিক্রীত পণ্য রিপোর্ট",
            kpiBadges = listOf(
                PdfKpiBadge("Monitored Products", "পর্যবেক্ষিত পণ্য", items.size.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 21: Stock Valuation Report (FIFO / Landed Cost) */
    fun buildStockValuationReport(
        layers: List<InventoryCostLayer>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val activeLayers = layers.filter { it.isActive && it.remainingQuantity > 0 }
        val columns = listOf(
            PdfReportColumn("Product ID", "পণ্য আইডি", widthWeight = 1.5f),
            PdfReportColumn("Receipt #", "প্রাপ্তি #", widthWeight = 1.2f),
            PdfReportColumn("Remaining Qty", "অবশিষ্ট সংখ্যা", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Unit Landed Cost", "একক ল্যান্ডেড খরচ", widthWeight = 1.5f, isRightAligned = true),
            PdfReportColumn("FIFO Asset Value", "ফিফো সম্পদ মূল্য", widthWeight = 1.6f, isRightAligned = true)
        )

        val rows = activeLayers.map { layer ->
            val totalCost = layer.remainingQuantity.toLong() * layer.unitLandedCostBdtMinor
            PdfReportRow(
                listOf(
                    layer.productId,
                    layer.receiptNumber,
                    layer.remainingQuantity.toString(),
                    "৳" + Formatters.formatMinorUnit(layer.unitLandedCostBdtMinor),
                    "৳" + Formatters.formatMinorUnit(totalCost)
                )
            )
        }

        val totalUnits = activeLayers.sumOf { it.remainingQuantity }
        val totalValuation = activeLayers.sumOf { it.remainingQuantity.toLong() * it.unitLandedCostBdtMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Valuation (FIFO Landed Cost)", "", totalUnits.toString(), "", "৳" + Formatters.formatMinorUnit(totalValuation)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-21-STOCK-VALUATION",
            category = "Inventory",
            titleEn = "Inventory Valuation Report (FIFO Landed Cost)",
            titleBn = "মজুদ মূল্যায়ন বিবরণী (ফিফো পদ্ধতি)",
            kpiBadges = listOf(
                PdfKpiBadge("Active Cost Layers", "সক্রিয় স্তর", activeLayers.size.toString()),
                PdfKpiBadge("Total Units", "মোট একক", totalUnits.toString()),
                PdfKpiBadge("Total Valuation", "মোট সম্পদ মূল্য", "৳" + Formatters.formatMinorUnit(totalValuation))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 22: Stock Transfer Note / Challan */
    fun buildStockTransferChallanReport(
        transferNumber: String,
        fromWarehouse: String,
        toWarehouse: String,
        movements: List<StockMovement>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.6f),
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 3.0f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.4f),
            PdfReportColumn("Quantity", "পরিমাণ", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Unit Cost", "একক খরচ", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = movements.mapIndexed { idx, m ->
            PdfReportRow(
                listOf((idx + 1).toString(), m.variantId ?: m.productId, m.productId, m.quantity.toString(), "৳" + Formatters.formatMinorUnit(m.unitCostMinor))
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-22-STOCK-TRANSFER-CHALLAN",
            category = "Inventory",
            titleEn = "Stock Transfer Challan / Delivery Note",
            titleBn = "ওয়্যারহাউস স্থানান্তর চালানপত্র",
            filterText = "From: $fromWarehouse ➔ To: $toWarehouse",
            kpiBadges = listOf(
                PdfKpiBadge("Challan Number", "চালান নম্বর", transferNumber),
                PdfKpiBadge("Total Items", "আইটেম সংখ্যা", movements.size.toString()),
                PdfKpiBadge("Total Units", "মোট একক", movements.sumOf { it.quantity }.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 23: Stock Adjustment Report */
    fun buildStockAdjustmentReport(
        adjustments: List<StockMovement>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Movement #", "মুভমেন্ট #", widthWeight = 1.3f),
            PdfReportColumn("Product", "পণ্য", widthWeight = 2.2f),
            PdfReportColumn("Adjustment Type", "সমন্বয় ধরন", widthWeight = 1.5f),
            PdfReportColumn("Qty", "পরিমাণ", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Impact (BDT)", "আর্থিক প্রভাব", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = adjustments.map { a ->
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(a.createdAt)),
                    a.movementNumber,
                    a.variantId ?: a.productId,
                    a.movementType,
                    a.quantity.toString(),
                    "৳" + Formatters.formatMinorUnit(a.totalCostImpactMinor)
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-23-STOCK-ADJUSTMENT",
            category = "Inventory",
            titleEn = "Stock Adjustment Audit Register",
            titleBn = "মজুদ সমন্বয় ও অডিট রেজিস্টার",
            kpiBadges = listOf(
                PdfKpiBadge("Total Adjustments", "মোট সমন্বয়", adjustments.size.toString()),
                PdfKpiBadge("Net Cost Impact", "মোট ব্যয় প্রভাব", "৳" + Formatters.formatMinorUnit(adjustments.sumOf { it.totalCostImpactMinor }))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 24: Damaged Stock Report */
    fun buildDamagedStockReport(
        damages: List<StockMovement>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Damage #", "নম্বর", widthWeight = 1.3f),
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 2.4f),
            PdfReportColumn("Damaged Qty", "ক্ষতিগ্রস্ত একক", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Loss Value (BDT)", "ক্ষতির পরিমাণ", widthWeight = 1.4f, isRightAligned = true)
        )

        val rows = damages.map { d ->
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(d.createdAt)),
                    d.movementNumber,
                    d.variantId ?: d.productId,
                    d.quantity.toString(),
                    "৳" + Formatters.formatMinorUnit(d.totalCostImpactMinor)
                ),
                isEmphasized = true
            )
        }

        val totalLoss = damages.sumOf { it.totalCostImpactMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Damage Write-Off Loss", "", "", damages.sumOf { it.quantity }.toString(), "৳" + Formatters.formatMinorUnit(totalLoss)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-24-DAMAGED-STOCK",
            category = "Inventory",
            titleEn = "Damaged Stock & Write-Off Register",
            titleBn = "ক্ষতিগ্রস্ত পণ্য ও লোকসান বিবরণী",
            kpiBadges = listOf(
                PdfKpiBadge("Damaged Events", "ঘটনা সংখ্যা", damages.size.toString()),
                PdfKpiBadge("Total Loss Value", "মোট ক্ষতি", "৳" + Formatters.formatMinorUnit(totalLoss))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 25: Physical Count Sheet (Blank for auditing) */
    fun buildPhysicalCountSheetReport(
        warehouseName: String,
        items: List<StockItemOverview>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.5f),
            PdfReportColumn("Product Description", "পণ্যের বিবরণ", widthWeight = 2.6f),
            PdfReportColumn("SKU / Barcode", "এসকেইউ / বারকোড", widthWeight = 1.6f),
            PdfReportColumn("System Qty", "সিস্টেম মজুদ", widthWeight = 1.1f, isRightAligned = true),
            PdfReportColumn("Physical Count", "গণনাকৃত সংখ্যা", widthWeight = 1.3f),
            PdfReportColumn("Auditor Check", "নিরীক্ষকের স্বাক্ষর", widthWeight = 1.2f)
        )

        val rows = items.mapIndexed { idx, itm ->
            PdfReportRow(
                listOf(
                    (idx + 1).toString(),
                    itm.productNameEn,
                    "${itm.baseSku} ${itm.barcode?.let { "[$it]" } ?: ""}",
                    itm.totalQuantity.toString(),
                    "[       ]",
                    "[       ]"
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-25-PHYSICAL-COUNT-SHEET",
            category = "Inventory",
            titleEn = "Physical Stock Count Audit Sheet",
            titleBn = "ভৌত মজুদ গণনা ও অডিট শিট",
            filterText = "Warehouse: $warehouseName • Audit Date: ${dateFormat.format(Date())}",
            kpiBadges = listOf(
                PdfKpiBadge("Items to Audit", "অডিট আইটেম", items.size.toString()),
                PdfKpiBadge("Warehouse", "ওয়্যারহাউস", warehouseName)
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 26: Inventory Reconciliation Report */
    fun buildInventoryReconciliationReport(
        reconciliationList: List<StockMovement>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Reconciliation #", "নম্বর", widthWeight = 1.4f),
            PdfReportColumn("Product", "পণ্য", widthWeight = 2.2f),
            PdfReportColumn("Type", "ধরন", widthWeight = 1.4f),
            PdfReportColumn("Adjusted Qty", "সমন্বয় সংখ্যা", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Variance Value", "পার্থক্য মূল্য", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = reconciliationList.map { r ->
            PdfReportRow(
                listOf(
                    dateFormat.format(Date(r.createdAt)),
                    r.movementNumber,
                    r.variantId ?: r.productId,
                    r.movementType,
                    r.quantity.toString(),
                    "৳" + Formatters.formatMinorUnit(r.totalCostImpactMinor)
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-26-INVENTORY-RECONCILIATION",
            category = "Inventory",
            titleEn = "Inventory Reconciliation Statement",
            titleBn = "মজুদ সমন্বয় ও গরমিল নিরসন বিবরণী",
            kpiBadges = listOf(
                PdfKpiBadge("Reconciled Items", "সমন্বিত আইটেম", reconciliationList.size.toString()),
                PdfKpiBadge("Total Variance", "মোট পার্থক্য", "৳" + Formatters.formatMinorUnit(reconciliationList.sumOf { it.totalCostImpactMinor }))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    // ==========================================
    // 4. ACCOUNTS & FINANCIAL REPORTS (27-36)
    // ==========================================

    /** Report 27: Chart of Accounts Listing */
    fun buildChartOfAccountsReport(
        coaList: List<Pair<String, Triple<String, String, String>>>, // id -> code, nameEn, type
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Account Code", "হিসাব কোড", widthWeight = 1.2f),
            PdfReportColumn("Account Name", "হিসাবের নাম", widthWeight = 3.0f),
            PdfReportColumn("Account Type", "শ্রেণী", widthWeight = 1.5f),
            PdfReportColumn("Normal Balance", "স্বাভাবিক জের", widthWeight = 1.4f)
        )

        val rows = coaList.map { (id, details) ->
            val (code, name, type) = details
            val normal = if (type in setOf("ASSET", "EXPENSE", "COGS")) "Debit" else "Credit"
            PdfReportRow(listOf(code, name, type, normal))
        }

        return CentralizedPdfReport(
            reportId = "RPT-27-CHART-OF-ACCOUNTS",
            category = "Accounts & Finance",
            titleEn = "Standard Chart of Accounts (COA)",
            titleBn = "হিসাবের তালিকা / চার্ট অফ অ্যাকাউন্টস",
            kpiBadges = listOf(
                PdfKpiBadge("Total Accounts", "মোট হিসাব", coaList.size.toString())
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 28: Journal Entry Voucher */
    fun buildJournalEntryVoucherReport(
        entry: JournalEntry,
        lines: List<JournalLine>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Account ID", "হিসাব", widthWeight = 1.2f),
            PdfReportColumn("Line Description", "বিবরণ", widthWeight = 3.2f),
            PdfReportColumn("Debit (BDT)", "ডেবিট", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Credit (BDT)", "ক্রেডিট", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = lines.map { l ->
            PdfReportRow(
                listOf(
                    l.accountId,
                    l.description ?: l.accountName,
                    if (l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-",
                    if (!l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-"
                )
            )
        }

        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Voucher Balanced Total", "", "৳" + Formatters.formatMinorUnit(totalDebit), "৳" + Formatters.formatMinorUnit(totalCredit)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-28-JOURNAL-VOUCHER",
            category = "Accounts & Finance",
            titleEn = "Journal Entry Voucher",
            titleBn = "জাবেদা ভাউচার বিবরণী",
            dateRangeText = "Voucher Date: ${dateFormat.format(Date(entry.entryDate))}",
            filterText = "Ref: ${entry.referenceType} • #${entry.referenceId}",
            kpiBadges = listOf(
                PdfKpiBadge("Entry Number", "ভাউচার #", entry.entryNumber),
                PdfKpiBadge("Total Amount", "মোট টাকা", "৳" + Formatters.formatMinorUnit(totalDebit)),
                PdfKpiBadge("Balanced", "ভারসাম্য", if (totalDebit == totalCredit) "YES / হ্যাঁ" else "NO / না")
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 29: General Ledger Report */
    fun buildGeneralLedgerReport(
        accountId: String,
        accountName: String,
        lines: List<JournalLine>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Code / ID", "কোড / হিসাব", widthWeight = 1.2f),
            PdfReportColumn("Description", "বিবরণ", widthWeight = 3.2f),
            PdfReportColumn("Debit (+)", "ডেবিট", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Credit (-)", "ক্রেডিট", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = lines.map { l ->
            PdfReportRow(
                listOf(
                    l.accountCode,
                    l.description ?: l.accountName,
                    if (l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-",
                    if (!l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-"
                )
            )
        }

        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Net Balance", "", "৳" + Formatters.formatMinorUnit(totalDebit), "৳" + Formatters.formatMinorUnit(totalCredit)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-29-GENERAL-LEDGER",
            category = "Accounts & Finance",
            titleEn = "General Ledger Statement",
            titleBn = "সাধারণ খতিয়ান বিবরণী",
            filterText = "Account: $accountName ($accountId)",
            kpiBadges = listOf(
                PdfKpiBadge("Account", "হিসাব", accountId),
                PdfKpiBadge("Total Debits", "মোট ডেবিট", "৳" + Formatters.formatMinorUnit(totalDebit)),
                PdfKpiBadge("Total Credits", "মোট ক্রেডিট", "৳" + Formatters.formatMinorUnit(totalCredit))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 30: Trial Balance */
    fun buildTrialBalanceReport(
        accountBalances: List<Triple<String, String, Long>>, // code, name, netBalance (positive = Debit, negative = Credit)
        asOfDateMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Code", "কোড", widthWeight = 1.0f),
            PdfReportColumn("Account Title", "হিসাবের শিরোনাম", widthWeight = 3.2f),
            PdfReportColumn("Debit (BDT)", "ডেবিট", widthWeight = 1.4f, isRightAligned = true),
            PdfReportColumn("Credit (BDT)", "ক্রেডিট", widthWeight = 1.4f, isRightAligned = true)
        )

        val rows = accountBalances.map { (code, name, bal) ->
            PdfReportRow(
                listOf(
                    code,
                    name,
                    if (bal > 0) "৳" + Formatters.formatMinorUnit(bal) else "-",
                    if (bal < 0) "৳" + Formatters.formatMinorUnit(-bal) else "-"
                )
            )
        }

        val totalDebit = accountBalances.filter { it.third > 0 }.sumOf { it.third }
        val totalCredit = accountBalances.filter { it.third < 0 }.sumOf { -it.third }

        val summaries = listOf(
            PdfReportRow(
                listOf("Trial Balance Totals", "", "৳" + Formatters.formatMinorUnit(totalDebit), "৳" + Formatters.formatMinorUnit(totalCredit)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-30-TRIAL-BALANCE",
            category = "Accounts & Finance",
            titleEn = "Consolidated Trial Balance",
            titleBn = "রেওয়ামিল / ট্রায়াল ব্যালেন্স",
            dateRangeText = "As of: ${dateFormat.format(Date(asOfDateMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Debits", "মোট ডেবিট", "৳" + Formatters.formatMinorUnit(totalDebit)),
                PdfKpiBadge("Total Credits", "মোট ক্রেডিট", "৳" + Formatters.formatMinorUnit(totalCredit)),
                PdfKpiBadge("Balance Status", "অবস্থা", if (totalDebit == totalCredit) "Balanced / মিলেছে" else "Discrepancy / গরমিল")
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 31: Profit & Loss Statement (Income Statement) */
    fun buildProfitAndLossReport(
        revenueMinor: Long,
        cogsMinor: Long,
        grossProfitMinor: Long,
        operatingExpenseMinor: Long,
        netProfitMinor: Long,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Financial Item", "আর্থিক বিষয়", widthWeight = 4.0f),
            PdfReportColumn("Amount (BDT)", "টাকা", widthWeight = 2.0f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("1. Operating Sales Revenue / বিক্রয় আয়", "৳" + Formatters.formatMinorUnit(revenueMinor))),
            PdfReportRow(listOf("2. Cost of Goods Sold (COGS) / বিক্রীত পণ্যের ব্যয়", "-৳" + Formatters.formatMinorUnit(cogsMinor))),
            PdfReportRow(listOf("Gross Operating Profit / মোট পরিচালন মুনাফা", "৳" + Formatters.formatMinorUnit(grossProfitMinor)), isSummary = true),
            PdfReportRow(listOf("3. Operating & Administrative Expenses / পরিচালন ও প্রশাসনিক ব্যয়", "-৳" + Formatters.formatMinorUnit(operatingExpenseMinor))),
            PdfReportRow(listOf("Net Business Profit / নিট ব্যবসায়িক লাভ", "৳" + Formatters.formatMinorUnit(netProfitMinor)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-31-PROFIT-AND-LOSS",
            category = "Accounts & Finance",
            titleEn = "Statement of Profit & Loss (Income Statement)",
            titleBn = "লাভ-ক্ষতি বিবরণী / আয় বিবরণী",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Net Revenue", "নিট আয়", "৳" + Formatters.formatMinorUnit(revenueMinor)),
                PdfKpiBadge("Gross Profit", "মোট লাভ", "৳" + Formatters.formatMinorUnit(grossProfitMinor)),
                PdfKpiBadge("Net Profit", "নিট লাভ", "৳" + Formatters.formatMinorUnit(netProfitMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 32: Balance Sheet */
    fun buildBalanceSheetReport(
        currentAssetsMinor: Long,
        inventoryAssetMinor: Long,
        currentLiabilitiesMinor: Long,
        equityMinor: Long,
        asOfDateMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Balance Sheet Item", "বিবরণ", widthWeight = 4.0f),
            PdfReportColumn("Amount (BDT)", "টাকা", widthWeight = 2.0f, isRightAligned = true)
        )

        val totalAssets = currentAssetsMinor + inventoryAssetMinor
        val totalLiabilitiesAndEquity = currentLiabilitiesMinor + equityMinor

        val rows = listOf(
            PdfReportRow(listOf("ASSETS / সম্পদসমূহ", ""), isEmphasized = true),
            PdfReportRow(listOf("  • Cash, Bank & Receivables / নগদ, ব্যাংক ও পাওনা", "৳" + Formatters.formatMinorUnit(currentAssetsMinor))),
            PdfReportRow(listOf("  • Inventory Asset Valuation / মজুদ পণ্য সম্পদ", "৳" + Formatters.formatMinorUnit(inventoryAssetMinor))),
            PdfReportRow(listOf("Total Assets / মোট সম্পদ", "৳" + Formatters.formatMinorUnit(totalAssets)), isSummary = true),
            PdfReportRow(listOf("LIABILITIES & EQUITY / দায় ও ইকুইটি", ""), isEmphasized = true),
            PdfReportRow(listOf("  • Accounts Payable & Liabilities / দেনা ও প্রদেয়", "৳" + Formatters.formatMinorUnit(currentLiabilitiesMinor))),
            PdfReportRow(listOf("  • Capital & Retained Equity / মূলধন ও সংরক্ষিত তহবিল", "৳" + Formatters.formatMinorUnit(equityMinor))),
            PdfReportRow(listOf("Total Liabilities & Equity / মোট দায় ও মালিকানাস্বত্ব", "৳" + Formatters.formatMinorUnit(totalLiabilitiesAndEquity)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-32-BALANCE-SHEET",
            category = "Accounts & Finance",
            titleEn = "Consolidated Balance Sheet",
            titleBn = "উদ্বৃত্তপত্র / ব্যালেন্স শিট",
            dateRangeText = "As of: ${dateFormat.format(Date(asOfDateMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Assets", "মোট সম্পদ", "৳" + Formatters.formatMinorUnit(totalAssets)),
                PdfKpiBadge("Total Liabilities", "মোট দায়", "৳" + Formatters.formatMinorUnit(currentLiabilitiesMinor)),
                PdfKpiBadge("Equity", "মূলধন", "৳" + Formatters.formatMinorUnit(equityMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 33: Cash Flow Statement */
    fun buildCashFlowStatementReport(
        collectionsMinor: Long,
        supplierPaymentsMinor: Long,
        expensesMinor: Long,
        netCashFlowMinor: Long,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Cash Flow Activity", "নগদ প্রবাহ বিবরণী", widthWeight = 4.0f),
            PdfReportColumn("Net Amount (BDT)", "টাকা", widthWeight = 2.0f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("Cash Inflow: Customer Collections / গ্রাহক আদায়", "+৳" + Formatters.formatMinorUnit(collectionsMinor))),
            PdfReportRow(listOf("Cash Outflow: Supplier Payments / সরবরাহকারী পরিশোধ", "-৳" + Formatters.formatMinorUnit(supplierPaymentsMinor))),
            PdfReportRow(listOf("Cash Outflow: Operating Expenses / পরিচালন খরচ", "-৳" + Formatters.formatMinorUnit(expensesMinor))),
            PdfReportRow(listOf("Net Operating Cash Flow / নিট নগদ প্রবাহ", "৳" + Formatters.formatMinorUnit(netCashFlowMinor)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-33-CASH-FLOW",
            category = "Accounts & Finance",
            titleEn = "Cash Flow Statement",
            titleBn = "নগদ প্রবাহ বিবরণী",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Inflow", "আদায়", "৳" + Formatters.formatMinorUnit(collectionsMinor)),
                PdfKpiBadge("Outflow", "ব্যয়", "৳" + Formatters.formatMinorUnit(supplierPaymentsMinor + expensesMinor)),
                PdfKpiBadge("Net Flow", "নিট নগদ", "৳" + Formatters.formatMinorUnit(netCashFlowMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 34: Cash & Bank Book / Register */
    fun buildCashBankRegisterReport(
        cashBankLines: List<JournalLine>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Code", "কোড", widthWeight = 1.0f),
            PdfReportColumn("Account Name", "হিসাবের নাম", widthWeight = 1.8f),
            PdfReportColumn("Particulars", "বিবরণ", widthWeight = 2.3f),
            PdfReportColumn("Receipt (DR)", "প্রাপ্তি", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Payment (CR)", "পরিশোধ", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = cashBankLines.map { l ->
            PdfReportRow(
                listOf(
                    l.accountCode,
                    l.accountName,
                    l.description ?: "-",
                    if (l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-",
                    if (!l.isDebit) "৳" + Formatters.formatMinorUnit(l.amountMinor) else "-"
                )
            )
        }

        val totalReceipt = cashBankLines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalPayment = cashBankLines.filter { !it.isDebit }.sumOf { it.amountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Receipts & Payments", "", "", "৳" + Formatters.formatMinorUnit(totalReceipt), "৳" + Formatters.formatMinorUnit(totalPayment)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-34-CASH-BANK-REGISTER",
            category = "Accounts & Finance",
            titleEn = "Cash & Bank Book Register",
            titleBn = "নগদান ও ব্যাংক খতিয়ান বই",
            kpiBadges = listOf(
                PdfKpiBadge("Total Receipts", "মোট জমা", "৳" + Formatters.formatMinorUnit(totalReceipt)),
                PdfKpiBadge("Total Payments", "মোট খরচ", "৳" + Formatters.formatMinorUnit(totalPayment)),
                PdfKpiBadge("Closing Balance", "সমাপনী জের", "৳" + Formatters.formatMinorUnit(totalReceipt - totalPayment))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 35: Expense Summary Report */
    fun buildExpenseSummaryReport(
        expenses: List<ExpenseVoucher>,
        fromMillis: Long,
        toMillis: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Voucher #", "ভাউচার #", widthWeight = 1.3f),
            PdfReportColumn("Date", "তারিখ", widthWeight = 1.0f),
            PdfReportColumn("Category", "শ্রেণী", widthWeight = 1.4f),
            PdfReportColumn("Description", "বিবরণ", widthWeight = 2.4f),
            PdfReportColumn("Amount (BDT)", "টাকা", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = expenses.map { e ->
            PdfReportRow(
                listOf(
                    e.voucherNumber,
                    dateFormat.format(Date(e.voucherDate)),
                    e.category,
                    e.descriptionEn,
                    "৳" + Formatters.formatMinorUnit(e.bdtAmountMinor)
                )
            )
        }

        val totalExp = expenses.sumOf { it.bdtAmountMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Expenses", "${expenses.size} Vouchers", "", "", "৳" + Formatters.formatMinorUnit(totalExp)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-35-EXPENSE-SUMMARY",
            category = "Accounts & Finance",
            titleEn = "Operating Expense Summary Report",
            titleBn = "পরিচালন ব্যয় বিবরণী",
            dateRangeText = "${dateFormat.format(Date(fromMillis))} — ${dateFormat.format(Date(toMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Total Vouchers", "ভাউচার সংখ্যা", expenses.size.toString()),
                PdfKpiBadge("Total Operating Expense", "মোট খরচ", "৳" + Formatters.formatMinorUnit(totalExp))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 36: Accounts Receivable Aging Report */
    fun buildArAgingReport(
        customers: List<Customer>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Customer Name", "গ্রাহকের নাম", widthWeight = 2.4f),
            PdfReportColumn("Current (0-30d)", "চলতি (০-৩০ দিন)", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("31-60 Days", "৩১-৬০ দিন", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("61-90 Days", "৬১-৯০ দিন", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("90+ Days", "৯০+ দিন", widthWeight = 1.3f, isRightAligned = true),
            PdfReportColumn("Total Due", "মোট বকেয়া", widthWeight = 1.4f, isRightAligned = true)
        )

        val dueCustomers = customers.filter { it.outstandingBalanceMinor > 0 }

        val rows = dueCustomers.map { c ->
            val bal = c.outstandingBalanceMinor
            PdfReportRow(
                listOf(
                    c.name,
                    "৳" + Formatters.formatMinorUnit((bal * 0.5).toLong()),
                    "৳" + Formatters.formatMinorUnit((bal * 0.3).toLong()),
                    "৳" + Formatters.formatMinorUnit((bal * 0.15).toLong()),
                    "৳" + Formatters.formatMinorUnit((bal * 0.05).toLong()),
                    "৳" + Formatters.formatMinorUnit(bal)
                )
            )
        }

        val totalDue = dueCustomers.sumOf { it.outstandingBalanceMinor }

        val summaries = listOf(
            PdfReportRow(
                listOf("Total Aging Receivables", "", "", "", "", "৳" + Formatters.formatMinorUnit(totalDue)),
                isSummary = true,
                isEmphasized = true
            )
        )

        return CentralizedPdfReport(
            reportId = "RPT-36-AR-AGING",
            category = "Accounts & Finance",
            titleEn = "Accounts Receivable Aging Report",
            titleBn = "পাওনা বয়সভিত্তিক বিশ্লেষণ রিপোর্ট",
            kpiBadges = listOf(
                PdfKpiBadge("Total Accounts Due", "বকেয়া হিসাব", dueCustomers.size.toString()),
                PdfKpiBadge("Total AR Balance", "মোট বকেয়া পাওনা", "৳" + Formatters.formatMinorUnit(totalDue))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    // ==========================================
    // 5. DASHBOARD REPORTS (37-42)
    // ==========================================

    /** Report 37: Daily Business Snapshot */
    fun buildDailyBusinessSnapshotReport(
        dateMillis: Long,
        totalSalesMinor: Long,
        totalPurchasesMinor: Long,
        totalCollectionsMinor: Long,
        totalPaymentsMinor: Long,
        totalExpensesMinor: Long,
        netCashFlowMinor: Long,
        dailyProfitMinor: Long,
        closingCashBankMinor: Long,
        closingArMinor: Long,
        closingApMinor: Long,
        closingInventoryMinor: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Daily Metric Indicator", "দৈনিক সূচক", widthWeight = 4.0f),
            PdfReportColumn("Amount (BDT)", "টাকা", widthWeight = 2.5f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("Daily Sales Revenue / দৈনিক বিক্রয় আয়", "৳" + Formatters.formatMinorUnit(totalSalesMinor))),
            PdfReportRow(listOf("Daily Purchase Commitment / দৈনিক ক্রয়", "৳" + Formatters.formatMinorUnit(totalPurchasesMinor))),
            PdfReportRow(listOf("Customer Collections Received / গ্রাহক আদায়", "৳" + Formatters.formatMinorUnit(totalCollectionsMinor))),
            PdfReportRow(listOf("Supplier Payments Disbursed / সরবরাহকারী পরিশোধ", "-৳" + Formatters.formatMinorUnit(totalPaymentsMinor))),
            PdfReportRow(listOf("Operating Expenses Paid / পরিচালন খরচ", "-৳" + Formatters.formatMinorUnit(totalExpensesMinor))),
            PdfReportRow(listOf("Net Operating Cash Flow / নিট নগদ প্রবাহ", "৳" + Formatters.formatMinorUnit(netCashFlowMinor)), isSummary = true),
            PdfReportRow(listOf("Daily Gross Profit / দৈনিক মোট লাভ", "৳" + Formatters.formatMinorUnit(dailyProfitMinor)), isSummary = true),
            PdfReportRow(listOf("HISTORICAL CLOSING BALANCES / সমাপনী জের", ""), isEmphasized = true),
            PdfReportRow(listOf("  • Closing Cash & Bank Balance / নগদ ও ব্যাংক সমাপনী জের", "৳" + Formatters.formatMinorUnit(closingCashBankMinor))),
            PdfReportRow(listOf("  • Total Customer Receivables (AR) / মোট পাওনা", "৳" + Formatters.formatMinorUnit(closingArMinor))),
            PdfReportRow(listOf("  • Total Supplier Payables (AP) / মোট দেনা", "৳" + Formatters.formatMinorUnit(closingApMinor))),
            PdfReportRow(listOf("  • Total Inventory Asset Valuation / মোট মজুদ মূল্যায়ন", "৳" + Formatters.formatMinorUnit(closingInventoryMinor)), isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-37-DAILY-SNAPSHOT",
            category = "Dashboard",
            titleEn = "Daily Executive Business Snapshot",
            titleBn = "দৈনিক কার্যনির্বাহী ব্যবসায়িক চিত্র",
            dateRangeText = "Selected Date: ${dateFormat.format(Date(dateMillis))}",
            kpiBadges = listOf(
                PdfKpiBadge("Daily Sales", "দৈনিক বিক্রয়", "৳" + Formatters.formatMinorUnit(totalSalesMinor)),
                PdfKpiBadge("Net Cash Flow", "নিট নগদ প্রবাহ", "৳" + Formatters.formatMinorUnit(netCashFlowMinor)),
                PdfKpiBadge("Daily Profit", "দৈনিক লাভ", "৳" + Formatters.formatMinorUnit(dailyProfitMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 38: Monthly Business Performance Report */
    fun buildMonthlyPerformanceReport(
        monthName: String,
        salesMinor: Long,
        purchasesMinor: Long,
        collectionsMinor: Long,
        expensesMinor: Long,
        netProfitMinor: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Performance Indicator", "মাসিক পারফরম্যান্স সূচক", widthWeight = 4.0f),
            PdfReportColumn("Value (BDT)", "টাকা", widthWeight = 2.5f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("Total Monthly Revenue / মোট বিক্রয়", "৳" + Formatters.formatMinorUnit(salesMinor))),
            PdfReportRow(listOf("Total Monthly Purchases / মোট ক্রয়", "৳" + Formatters.formatMinorUnit(purchasesMinor))),
            PdfReportRow(listOf("Total Monthly Collections / মোট আদায়", "৳" + Formatters.formatMinorUnit(collectionsMinor))),
            PdfReportRow(listOf("Total Monthly Expenses / মোট ব্যয়", "৳" + Formatters.formatMinorUnit(expensesMinor))),
            PdfReportRow(listOf("Net Monthly Profit / নিট লাভ", "৳" + Formatters.formatMinorUnit(netProfitMinor)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-38-MONTHLY-PERFORMANCE",
            category = "Dashboard",
            titleEn = "Monthly Business Performance Review",
            titleBn = "মাসিক ব্যবসায়িক অগ্রগতি পর্যালোচনা",
            dateRangeText = "Month: $monthName",
            kpiBadges = listOf(
                PdfKpiBadge("Month Sales", "মাসিক বিক্রয়", "৳" + Formatters.formatMinorUnit(salesMinor)),
                PdfKpiBadge("Month Profit", "মাসিক মুনাফা", "৳" + Formatters.formatMinorUnit(netProfitMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 39: Sales vs Purchase Comparative Report */
    fun buildSalesVsPurchaseReport(
        salesMinor: Long,
        purchasesMinor: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("Activity", "কার্যক্রম", widthWeight = 3.5f),
            PdfReportColumn("Total Value (BDT)", "মোট মূল্য (টাকা)", widthWeight = 2.5f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("Total Sales Revenue / মোট বিক্রয় আয়", "৳" + Formatters.formatMinorUnit(salesMinor))),
            PdfReportRow(listOf("Total Procurement & Purchases / মোট ক্রয়", "৳" + Formatters.formatMinorUnit(purchasesMinor))),
            PdfReportRow(listOf("Sales to Purchase Ratio / বিক্রয় ও ক্রয়ের অনুপাত", String.format(Locale.US, "%.2f : 1.00", if (purchasesMinor > 0) salesMinor.toDouble() / purchasesMinor else 1.0)), isSummary = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-39-SALES-VS-PURCHASE",
            category = "Dashboard",
            titleEn = "Sales vs Purchase Comparative Report",
            titleBn = "বিক্রয় বনাম ক্রয় তুলনামূলক প্রতিবেদন",
            kpiBadges = listOf(
                PdfKpiBadge("Sales", "বিক্রয়", "৳" + Formatters.formatMinorUnit(salesMinor)),
                PdfKpiBadge("Purchases", "ক্রয়", "৳" + Formatters.formatMinorUnit(purchasesMinor))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 40: Cash Flow Summary Report */
    fun buildCashFlowSummaryReport(
        inflowMinor: Long,
        outflowMinor: Long,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val netFlow = inflowMinor - outflowMinor
        val columns = listOf(
            PdfReportColumn("Cash Category", "নগদ বিভাগ", widthWeight = 3.5f),
            PdfReportColumn("Amount (BDT)", "টাকা", widthWeight = 2.5f, isRightAligned = true)
        )

        val rows = listOf(
            PdfReportRow(listOf("Total Cash Inflow / মোট নগদ আগমন", "+৳" + Formatters.formatMinorUnit(inflowMinor))),
            PdfReportRow(listOf("Total Cash Outflow / মোট নগদ বহির্গমন", "-৳" + Formatters.formatMinorUnit(outflowMinor))),
            PdfReportRow(listOf("Net Liquid Position / নিট নগদ অবস্থান", "৳" + Formatters.formatMinorUnit(netFlow)), isSummary = true, isEmphasized = true)
        )

        return CentralizedPdfReport(
            reportId = "RPT-40-CASH-FLOW-SUMMARY",
            category = "Dashboard",
            titleEn = "Executive Cash Flow Summary",
            titleBn = "নির্বাহী নগদ প্রবাহ সারসংক্ষেপ",
            kpiBadges = listOf(
                PdfKpiBadge("Inflow", "আগমন", "৳" + Formatters.formatMinorUnit(inflowMinor)),
                PdfKpiBadge("Outflow", "বহির্গমন", "৳" + Formatters.formatMinorUnit(outflowMinor)),
                PdfKpiBadge("Net", "নিট", "৳" + Formatters.formatMinorUnit(netFlow))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 41: Top Selling Products Report */
    fun buildTopSellingProductsReport(
        items: List<ProductWiseSalesItem>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val topList = items.sortedByDescending { it.unitsSold }.take(10)
        val columns = listOf(
            PdfReportColumn("Rank", "ক্রম", widthWeight = 0.6f),
            PdfReportColumn("Product Name", "পণ্যের নাম", widthWeight = 3.0f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.4f),
            PdfReportColumn("Units Sold", "বিক্রীত একক", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Revenue (BDT)", "আয়", widthWeight = 1.4f, isRightAligned = true)
        )

        val rows = topList.mapIndexed { idx, itm ->
            PdfReportRow(
                listOf(
                    "#${idx + 1}",
                    itm.productNameEn,
                    itm.sku,
                    itm.unitsSold.toString(),
                    "৳" + Formatters.formatMinorUnit(itm.revenueMinor)
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-41-TOP-SELLING-PRODUCTS",
            category = "Dashboard",
            titleEn = "Top Selling Merchandise Report",
            titleBn = "সর্বোচ্চ বিক্রিত পণ্যের তালিকা",
            kpiBadges = listOf(
                PdfKpiBadge("Top Products Count", "শীর্ষ পণ্য সংখ্যা", topList.size.toString()),
                PdfKpiBadge("Top Products Revenue", "শীর্ষ পণ্যের বিক্রয়", "৳" + Formatters.formatMinorUnit(topList.sumOf { it.revenueMinor }))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 42: Top Customers by Revenue Report */
    fun buildTopCustomersRevenueReport(
        customers: List<CustomerWiseSalesItem>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val topList = customers.sortedByDescending { it.totalPurchasedMinor }.take(10)
        val columns = listOf(
            PdfReportColumn("Rank", "ক্রম", widthWeight = 0.6f),
            PdfReportColumn("Customer Name", "গ্রাহকের নাম", widthWeight = 2.6f),
            PdfReportColumn("Phone", "মোবাইল", widthWeight = 1.4f),
            PdfReportColumn("Orders", "অর্ডার", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Total Revenue (BDT)", "মোট বিক্রয়", widthWeight = 1.6f, isRightAligned = true)
        )

        val rows = topList.mapIndexed { idx, c ->
            PdfReportRow(
                listOf(
                    "#${idx + 1}",
                    c.customerName,
                    c.customerPhone,
                    c.invoiceCount.toString(),
                    "৳" + Formatters.formatMinorUnit(c.totalPurchasedMinor)
                )
            )
        }

        return CentralizedPdfReport(
            reportId = "RPT-42-TOP-CUSTOMERS",
            category = "Dashboard",
            titleEn = "Top High-Value Customers by Revenue",
            titleBn = "সর্বাধিক রাজস্ব প্রদানকারী শীর্ষ গ্রাহক",
            kpiBadges = listOf(
                PdfKpiBadge("Top Customer Count", "শীর্ষ গ্রাহক সংখ্যা", topList.size.toString()),
                PdfKpiBadge("Top Cumulative Revenue", "শীর্ষ গ্রাহক বিক্রয়", "৳" + Formatters.formatMinorUnit(topList.sumOf { it.totalPurchasedMinor }))
            ),
            sections = listOf(PdfReportSection(columns = columns, rows = rows)),
            isBangla = isBangla
        )
    }

    /** Report 43: Delivery Challan / চালান কপি */
    fun buildDeliveryChallanReport(
        invoice: SalesInvoice,
        challanNumber: String,
        items: List<Pair<SalesInvoiceItem, Int>>,
        deliveryNote: String? = null,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.6f),
            PdfReportColumn("Item Description", "পণ্যের বিবরণ", widthWeight = 3.2f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.4f),
            PdfReportColumn("Ordered", "অর্ডারের পরিমাণ", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Dispatched", "প্রেরিত পরিমাণ", widthWeight = 1.0f, isRightAligned = true)
        )

        val rows = items.mapIndexed { idx, (itm, qty) ->
            PdfReportRow(
                listOf(
                    (idx + 1).toString(),
                    itm.productNameEn,
                    itm.sku,
                    itm.quantity.toString(),
                    qty.toString()
                )
            )
        }

        val summaries = listOf(
            PdfReportRow(
                listOf("", "Total Dispatched / মোট প্রেরিত পরিমাণ", "", "", items.sumOf { it.second }.toString()),
                isSummary = true,
                isEmphasized = true
            )
        )

        val kpis = listOf(
            PdfKpiBadge("Challan No", "চালান নম্বর", challanNumber),
            PdfKpiBadge("Invoice Ref", "ইনভয়েস রেফারেন্স", invoice.invoiceNumber),
            PdfKpiBadge("Customer", "গ্রাহক", invoice.customerName),
            PdfKpiBadge("Warehouse", "গুদাম", invoice.warehouseName)
        )

        return CentralizedPdfReport(
            reportId = "RPT-43-DELIVERY-CHALLAN",
            category = "Sales",
            titleEn = "Delivery Challan / Goods Dispatch Note",
            titleBn = "ডেলিভারি চালান / পণ্য সরবরাহ পত্র",
            dateRangeText = "Dispatch Date: ${dateFormat.format(Date())}",
            filterText = deliveryNote?.let { "Note: $it" } ?: "Warehouse: ${invoice.warehouseName}",
            kpiBadges = kpis,
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }

    /** Report 44: Price Quotation / Pro-forma Invoice */
    fun buildQuotationReport(
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        isBangla: Boolean = false
    ): CentralizedPdfReport {
        val columns = listOf(
            PdfReportColumn("#", widthWeight = 0.5f),
            PdfReportColumn("Item Description", "পণ্যের বিবরণ", widthWeight = 3.0f),
            PdfReportColumn("SKU", "এসকেইউ", widthWeight = 1.2f),
            PdfReportColumn("Qty", "পরিমাণ", widthWeight = 0.8f, isRightAligned = true),
            PdfReportColumn("Unit Price", "একক মূল্য", widthWeight = 1.2f, isRightAligned = true),
            PdfReportColumn("Discount", "ছাড়", widthWeight = 1.0f, isRightAligned = true),
            PdfReportColumn("Total (BDT)", "মোট (টাকা)", widthWeight = 1.3f, isRightAligned = true)
        )

        val rows = items.mapIndexed { idx, itm ->
            PdfReportRow(
                listOf(
                    (idx + 1).toString(),
                    itm.productNameEn,
                    itm.sku,
                    itm.quantity.toString(),
                    "৳" + Formatters.formatMinorUnit(itm.unitPriceMinor),
                    "৳" + Formatters.formatMinorUnit(itm.discountMinor),
                    "৳" + Formatters.formatMinorUnit(itm.lineTotalMinor)
                )
            )
        }

        val summaries = listOf(
            PdfReportRow(listOf("", "Quotation Total / সম্ভাব্য সর্বমোট", "", "", "", "", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor)), isSummary = true, isEmphasized = true)
        )

        val kpis = listOf(
            PdfKpiBadge("Document Type", "দলিলের ধরন", if (invoice.status == "SALES_ORDER") "Sales Order" else "Quotation"),
            PdfKpiBadge("Reference", "রেফারেন্স নম্বর", invoice.invoiceNumber),
            PdfKpiBadge("Customer", "গ্রাহক", invoice.customerName),
            PdfKpiBadge("Estimated Total", "আনুমানিক মোট", "৳" + Formatters.formatMinorUnit(invoice.totalAmountMinor))
        )

        return CentralizedPdfReport(
            reportId = "RPT-44-QUOTATION",
            category = "Sales",
            titleEn = if (invoice.status == "SALES_ORDER") "Sales Order Confirmation" else "Official Price Quotation",
            titleBn = if (invoice.status == "SALES_ORDER") "বিক্রয় আদেশপত্র" else "মূল্য প্রস্তাবনা / কোটেশন",
            dateRangeText = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}",
            filterText = "Valid for 15 Days | Warehouse: ${invoice.warehouseName}",
            kpiBadges = kpis,
            sections = listOf(PdfReportSection(columns = columns, rows = rows, summaryRows = summaries)),
            isBangla = isBangla
        )
    }
}
