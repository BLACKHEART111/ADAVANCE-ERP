package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import kotlinx.coroutines.flow.Flow

data class InvoiceItemDraft(
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val unitPriceMinor: Long,
    val discountMinor: Long = 0L
)

data class NewCustomerDraft(
    val name: String,
    val phone: String,
    val address: String,
    val email: String? = null,
    val openingBalanceMinor: Long = 0L
)

data class CustomerSummary(
    val customer: Customer,
    val totalSalesMinor: Long,
    val lifetimePaidMinor: Long,
    val outstandingBalanceMinor: Long,
    val totalReturnsMinor: Long
)

data class SalesInvoiceDraft(
    val customerId: String = "",
    val warehouseId: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String? = null,
    val discountMinor: Long = 0L,
    val additionalChargesMinor: Long = 0L,
    val paidAmountMinor: Long = 0L,
    val paymentMethod: String = "CASH",
    val paymentAccountId: String = "coa-1010",
    val items: List<InvoiceItemDraft>,
    val newCustomerDraft: NewCustomerDraft? = null
)

data class DeliveryDispatchDraft(
    val invoiceId: String,
    val warehouseId: String,
    val deliveryDate: Long = System.currentTimeMillis(),
    val deliveryPerson: String? = null,
    val transportInfo: String? = null,
    val notes: String? = null,
    val items: List<DeliveryDispatchItemDraft>
)

data class DeliveryDispatchItemDraft(
    val invoiceItemId: String,
    val productId: String,
    val variantId: String? = null,
    val quantityToDispatch: Int
)

data class DeliveryChallanInfo(
    val deliveryNumber: String,
    val invoiceId: String,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String?,
    val deliveryDate: Long,
    val deliveryPerson: String?,
    val transportInfo: String?,
    val notes: String?,
    val items: List<DeliveryChallanItemInfo>
)

data class DeliveryChallanItemInfo(
    val productId: String,
    val variantId: String?,
    val productNameEn: String,
    val productNameBn: String,
    val sku: String,
    val dispatchedQuantity: Int
)

data class UndeliveredItemInfo(
    val invoiceItemId: String,
    val productId: String,
    val variantId: String?,
    val productNameEn: String,
    val productNameBn: String,
    val sku: String,
    val orderedQuantity: Int,
    val dispatchedQuantity: Int,
    val remainingQuantity: Int
)

data class DirectExchangeDraft(
    val customerId: String,
    val warehouseId: String,
    val originalInvoiceId: String?,
    val returnedItems: List<ReturnItemDraft>,
    val replacementItems: List<InvoiceItemDraft>,
    val netDifferenceMinor: Long,
    val paymentMethod: String = "CASH",
    val paymentAccountId: String = "coa-1010",
    val notes: String? = null
)

data class InvoiceAllocationDraft(
    val invoiceId: String,
    val amountMinor: Long
)

data class CustomerPaymentDraft(
    val customerId: String,
    val amountMinor: Long,
    val paymentMethod: String = "CASH",
    val accountId: String = "coa-1010",
    val paymentDate: Long = System.currentTimeMillis(),
    val referenceNote: String? = null,
    val allocations: List<InvoiceAllocationDraft> = emptyList()
)

data class ReturnItemDraft(
    val invoiceItemId: String,
    val productId: String,
    val variantId: String? = null,
    val returnQuantity: Int,
    val isDamaged: Boolean = false,
    val damagedQuantity: Int = 0
)

data class SalesReturnDraft(
    val invoiceId: String,
    val warehouseId: String,
    val refundOption: ReturnRefundOption = ReturnRefundOption.CUSTOMER_CREDIT,
    val reason: String? = null,
    val returnDate: Long = System.currentTimeMillis(),
    val items: List<ReturnItemDraft>
)

data class CustomerLedgerEntry(
    val id: String,
    val date: Long,
    val transactionType: String, // "INVOICE", "PAYMENT", "RETURN"
    val referenceNumber: String,
    val description: String,
    val debitMinor: Long,   // Increases receivable
    val creditMinor: Long,  // Decreases receivable
    val runningBalanceMinor: Long
)

data class SalesReportSummary(
    val totalGrossSalesMinor: Long,
    val totalDiscountMinor: Long,
    val totalNetSalesMinor: Long,
    val totalCogsMinor: Long,
    val totalGrossProfitMinor: Long,
    val grossProfitMarginPercent: Double,
    val totalCollectedMinor: Long,
    val totalReceivableMinor: Long,
    val totalReturnsMinor: Long
)

data class ProductWiseSalesItem(
    val productId: String,
    val productNameEn: String,
    val productNameBn: String,
    val sku: String,
    val unitsSold: Int,
    val revenueMinor: Long,
    val cogsMinor: Long,
    val grossProfitMinor: Long
)

data class CustomerWiseSalesItem(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val invoiceCount: Int,
    val totalPurchasedMinor: Long,
    val totalPaidMinor: Long,
    val outstandingBalanceMinor: Long
)

interface SalesRepository {

    // Invoices
    suspend fun createInvoice(draft: SalesInvoiceDraft): String
    suspend fun updateInvoice(invoiceId: String, draft: SalesInvoiceDraft)
    fun getAllInvoices(): Flow<List<SalesInvoice>>
    suspend fun getInvoiceById(id: String): SalesInvoice?
    suspend fun getInvoiceItems(invoiceId: String): List<SalesInvoiceItem>
    fun getInvoiceItemsFlow(invoiceId: String): Flow<List<SalesInvoiceItem>>
    suspend fun postInvoice(invoiceId: String): Result<Unit>
    suspend fun createAndPostInvoice(draft: SalesInvoiceDraft): Result<String>

    // Customer
    suspend fun getCustomerSummary(customerId: String): CustomerSummary?
    suspend fun searchCustomers(query: String): List<Customer>

    // Payments
    suspend fun recordCustomerPayment(draft: CustomerPaymentDraft): String
    fun getAllPayments(): Flow<List<CustomerPayment>>
    fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>>
    fun getAllocationsForInvoice(invoiceId: String): Flow<List<PaymentAllocation>>
    suspend fun getAllocationsForInvoiceSync(invoiceId: String): List<PaymentAllocation>

    // Returns
    suspend fun processSalesReturn(draft: SalesReturnDraft): String
    fun getAllReturns(): Flow<List<SalesReturn>>
    fun getReturnsForInvoice(invoiceId: String): Flow<List<SalesReturn>>
    suspend fun getItemsForReturn(returnId: String): List<SalesReturnItem>

    // Customer Ledger & Balances
    suspend fun getCustomerLedger(customerId: String): List<CustomerLedgerEntry>
    suspend fun recalculateCustomerBalance(customerId: String): Long
    fun getCustomerAdvanceBalance(customerId: String): Flow<Long>

    // Reports
    suspend fun getSalesReportSummary(fromMillis: Long, toMillis: Long): SalesReportSummary
    suspend fun getProductWiseSalesReport(fromMillis: Long, toMillis: Long): List<ProductWiseSalesItem>
    suspend fun getCustomerWiseSalesReport(fromMillis: Long, toMillis: Long): List<CustomerWiseSalesItem>

    // Quotations & Sales Orders
    suspend fun createQuotation(draft: SalesInvoiceDraft): String
    suspend fun convertQuotationToSalesOrder(quotationId: String): Result<String>
    suspend fun convertSalesOrderToInvoice(salesOrderId: String): Result<String>

    // Deliveries & Partial Dispatch
    suspend fun dispatchDelivery(draft: DeliveryDispatchDraft): Result<String>
    suspend fun getDeliveryChallansForInvoice(invoiceId: String): List<DeliveryChallanInfo>
    suspend fun getRemainingUndeliveredItems(invoiceId: String): List<UndeliveredItemInfo>

    // Direct Exchange (Method A)
    suspend fun processDirectExchange(draft: DirectExchangeDraft): Result<String>

    // Safe Deletion, Cancellation & Void
    suspend fun voidCustomerPayment(paymentId: String, reason: String, operator: String = "SYSTEM_ADMIN"): Result<Unit>
    suspend fun cancelOrDeleteInvoice(invoiceId: String, reason: String, operator: String = "SYSTEM_ADMIN"): Result<Unit>
}
