package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentNumberService @Inject constructor(
    private val database: AdvanceErpDatabase
) {
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val yearFormat = SimpleDateFormat("yyyy", Locale.US)

    suspend fun getNextInvoiceNumber(): String = getNextDocumentNumber("INV", "sales_invoices", "invoiceNumber")
    suspend fun getNextPurchaseNumber(): String = getNextDocumentNumber("PUR", "purchase_orders", "poNumber")
    suspend fun getNextCustomerPaymentNumber(): String = getNextDocumentNumber("CP", "customer_payments", "paymentNumber")
    suspend fun getNextSupplierPaymentNumber(): String = getNextDocumentNumber("SP", "supplier_payments", "paymentNumber")
    suspend fun getNextSalesReturnNumber(): String = getNextDocumentNumber("SR", "sales_returns", "returnNumber")
    suspend fun getNextStockTransferNumber(): String = getNextDocumentNumber("ST", "stock_transfers", "transferNumber")
    suspend fun getNextExpenseNumber(): String = getNextDocumentNumber("EXP", "expense_vouchers", "voucherNumber")
    suspend fun getNextStockAdjustmentNumber(): String = getNextDocumentNumber("ADJ", "stock_movements", "movementNumber")
    suspend fun getNextDeliveryNumber(): String = getNextDocumentNumber("DEL", "stock_movements", "movementNumber")
    suspend fun getNextQuotationNumber(): String = getNextDocumentNumber("QUO", "sales_invoices", "invoiceNumber")
    suspend fun getNextSalesOrderNumber(): String = getNextDocumentNumber("SO", "sales_invoices", "invoiceNumber")

    private suspend fun getNextDocumentNumber(
        prefix: String,
        tableName: String,
        columnName: String
    ): String = withContext(Dispatchers.IO) {
        val currentYear = yearFormat.format(Date())
        val key = "$prefix-$currentYear"

        val counter = counters.computeIfAbsent(key) {
            val maxSeq = queryMaxSequenceFromDb(prefix, currentYear, tableName, columnName)
            AtomicLong(maxSeq)
        }

        val nextSeq = counter.incrementAndGet()
        "$prefix-$currentYear-${nextSeq.toString().padStart(4, '0')}"
    }

    private fun queryMaxSequenceFromDb(
        prefix: String,
        year: String,
        tableName: String,
        columnName: String
    ): Long {
        return try {
            val query = "SELECT $columnName FROM $tableName WHERE $columnName LIKE '$prefix-$year-%'"
            val cursor = database.openHelper.readableDatabase.query(query)
            var maxSeq = 0L
            while (cursor.moveToNext()) {
                val str = cursor.getString(0) ?: continue
                val parts = str.split("-")
                if (parts.size >= 3) {
                    val seq = parts.last().toLongOrNull() ?: 0L
                    if (seq > maxSeq) maxSeq = seq
                }
            }
            cursor.close()
            maxSeq
        } catch (e: Exception) {
            0L
        }
    }
}
