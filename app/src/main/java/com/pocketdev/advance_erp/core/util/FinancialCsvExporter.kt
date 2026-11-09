package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.domain.model.AgingReport
import com.pocketdev.advance_erp.domain.model.BalanceSheet
import com.pocketdev.advance_erp.domain.model.CashBankBookEntry
import com.pocketdev.advance_erp.domain.model.ProfitAndLossStatement
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialCsvExporter {

    private fun getCsvDir(context: Context): File {
        return File(context.cacheDir, "financial_csv").apply { if (!exists()) mkdirs() }
    }

    fun exportProfitAndLossCsv(context: Context, pnl: ProfitAndLossStatement): File {
        val file = File(getCsvDir(context), "pnl_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("Section,Account Code,Account Name,Amount BDT\n")
            writer.append("Operating Revenue,4010,Gross Sales Revenue,${pnl.grossSalesMinor / 100.0}\n")
            writer.append("Operating Revenue,4020,Sales Returns & Allowances,-${pnl.salesReturnsMinor / 100.0}\n")
            writer.append("Operating Revenue,4030,Sales Discounts Allowed,-${pnl.salesDiscountsMinor / 100.0}\n")
            writer.append("Net Revenue,,,${pnl.netSalesRevenueMinor / 100.0}\n")
            writer.append("Cost of Goods Sold,5010,COGS,-${pnl.costOfGoodsSoldMinor / 100.0}\n")
            writer.append("Gross Profit,,,${pnl.grossProfitMinor / 100.0}\n")
            for (exp in pnl.operatingExpenses) {
                writer.append("Operating Expense,${exp.accountCode},\"${exp.nameEn}\",${exp.amountMinor / 100.0}\n")
            }
            writer.append("Total Operating Expenses,,,${pnl.totalOperatingExpensesMinor / 100.0}\n")
            writer.append("Operating Profit,,,${pnl.operatingProfitMinor / 100.0}\n")
            writer.append("Other Items,5090,Realized FX Gain/Loss,${pnl.foreignExchangeGainLossMinor / 100.0}\n")
            writer.append("NET PROFIT / (LOSS),,,${pnl.netProfitMinor / 100.0}\n")
        }
        return file
    }

    fun exportBalanceSheetCsv(context: Context, bs: BalanceSheet): File {
        val file = File(getCsvDir(context), "balance_sheet_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("Category,Account Code,Account Name,Amount BDT\n")
            for (item in bs.currentAssets) {
                writer.append("ASSET,${item.accountCode},\"${item.nameEn}\",${item.amountMinor / 100.0}\n")
            }
            writer.append("TOTAL ASSETS,,,${bs.totalAssetsMinor / 100.0}\n")

            for (item in bs.currentLiabilities) {
                writer.append("LIABILITY,${item.accountCode},\"${item.nameEn}\",${item.amountMinor / 100.0}\n")
            }
            writer.append("TOTAL LIABILITIES,,,${bs.totalLiabilitiesMinor / 100.0}\n")

            for (item in bs.equityItems) {
                writer.append("EQUITY,${item.accountCode},\"${item.nameEn}\",${item.amountMinor / 100.0}\n")
            }
            writer.append("EQUITY,3020,Retained Earnings (Accumulated Profit),${bs.retainedEarningsMinor / 100.0}\n")
            writer.append("TOTAL EQUITY,,,${bs.totalEquityMinor / 100.0}\n")
            writer.append("TOTAL LIABILITIES & EQUITY,,,${bs.totalLiabilitiesAndEquityMinor / 100.0}\n")
        }
        return file
    }

    fun exportCashBankBookCsv(context: Context, title: String, entries: List<CashBankBookEntry>): File {
        val file = File(getCsvDir(context), "${title.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}.csv")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        FileWriter(file).use { writer ->
            writer.append("Date,Reference,Description,Receipt Debit BDT,Payment Credit BDT,Running Balance BDT\n")
            for (entry in entries) {
                val d = dateFormat.format(Date(entry.date))
                writer.append("\"$d\",\"${entry.referenceNumber}\",\"${entry.description}\",${entry.receiptMinor / 100.0},${entry.paymentMinor / 100.0},${entry.runningBalanceMinor / 100.0}\n")
            }
        }
        return file
    }

    fun exportTrialBalanceCsv(context: Context, items: List<TrialBalanceItem>): File {
        val file = File(getCsvDir(context), "trial_balance_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("Account Code,Account Name,Type,Normal Balance,Total Debit BDT,Total Credit BDT,Net Debit BDT,Net Credit BDT\n")
            for (it in items) {
                writer.append("${it.accountCode},\"${it.accountNameEn}\",${it.accountType},${it.normalBalance},${it.totalDebitMinor / 100.0},${it.totalCreditMinor / 100.0},${it.netDebitMinor / 100.0},${it.netCreditMinor / 100.0}\n")
            }
        }
        return file
    }

    fun exportAgingReportCsv(context: Context, aging: AgingReport): File {
        val file = File(getCsvDir(context), "${aging.reportType.lowercase()}_aging_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("Aging Bracket,Amount BDT,Percentage %\n")
            for (b in aging.buckets) {
                writer.append("\"${b.labelEn}\",${b.amountMinor / 100.0},${"%.2f".format(b.percentage)}\n")
            }
            writer.append("Total Outstanding,${aging.totalOutstandingMinor / 100.0},100.00\n")
        }
        return file
    }

    fun shareCsv(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
