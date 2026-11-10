package com.pocketdev.advance_erp.feature.sales

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfEngine
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.core.util.DueCollectionStatementData
import com.pocketdev.advance_erp.core.util.DueReminderStatementData
import com.pocketdev.advance_erp.core.util.InvoicePdfGenerator
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.ProductRepository
import com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.domain.repository.SalesReportSummary
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SalesUiState(
    val invoices: List<SalesInvoice> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val products: List<Product> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val stockBalances: List<StockBalance> = emptyList(),
    val payments: List<CustomerPayment> = emptyList(),
    val returns: List<SalesReturn> = emptyList(),
    val selectedCustomerSummary: CustomerSummary? = null,
    val selectedInvoice: SalesInvoice? = null,
    val selectedInvoiceItems: List<SalesInvoiceItem> = emptyList(),
    val selectedInvoiceAllocations: List<PaymentAllocation> = emptyList(),
    val selectedCustomerLedger: List<CustomerLedgerEntry> = emptyList(),
    val selectedCustomerIdForLedger: String? = null,
    val salesReport: SalesReportSummary? = null,
    val productWiseReport: List<ProductWiseSalesItem> = emptyList(),
    val customerWiseReport: List<CustomerWiseSalesItem> = emptyList(),
    val businessProfile: BusinessProfile? = null,
    val nextPaymentNumber: String? = null,
    val language: AppLanguage = AppLanguage.BANGLA,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val activeDueCollectionData: DueCollectionStatementData? = null,
    val activeDueReminderData: DueReminderStatementData? = null
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val productVariantDao: ProductVariantDao,
    private val customerDao: CustomerDao,
    private val warehouseDao: WarehouseDao,
    private val stockBalanceDao: StockBalanceDao,
    private val businessProfileDao: BusinessProfileDao,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState(isLoading = true))
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        loadDefaultReport()
        refreshNextPaymentNumber()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Combine flows in groups to respect Kotlin's combine parameter limits
            val streamA = combine(
                salesRepository.getAllInvoices(),
                customerDao.getAllCustomers(),
                warehouseDao.getAllWarehouses()
            ) { invoices, customers, warehouses ->
                Triple(invoices, customers, warehouses)
            }

            val streamB = combine(
                productRepository.getAllProducts(),
                salesRepository.getAllPayments(),
                salesRepository.getAllReturns()
            ) { productsWithDetails, payments, returns ->
                val products = productsWithDetails.map { pwd ->
                    Product(
                        id = pwd.id,
                        categoryId = pwd.categoryId,
                        nameEn = pwd.nameEn,
                        nameBn = pwd.nameBn,
                        baseSku = pwd.baseSku,
                        barcode = pwd.barcode,
                        description = pwd.description,
                        unit = pwd.unit,
                        purchasePriceMinor = pwd.purchasePriceMinor,
                        sellingPriceMinor = pwd.sellingPriceMinor,
                        minStockAlert = pwd.minStockAlert,
                        isActive = pwd.isActive,
                        createdAt = pwd.createdAt,
                        updatedAt = pwd.createdAt
                    )
                }
                Triple(products, payments, returns)
            }

            combine(
                streamA,
                streamB,
                preferencesRepository.selectedLanguage
            ) { (invoices, customers, warehouses), (products, payments, returns), lang ->
                val currentSelectedId = _uiState.value.selectedInvoice?.id
                val updatedSelectedInvoice = if (currentSelectedId != null) {
                    invoices.find { it.id == currentSelectedId }
                } else null

                _uiState.value.copy(
                    invoices = invoices,
                    customers = customers,
                    warehouses = warehouses,
                    products = products,
                    payments = payments,
                    returns = returns,
                    selectedInvoice = updatedSelectedInvoice ?: _uiState.value.selectedInvoice,
                    language = lang,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        viewModelScope.launch {
            productVariantDao.getAllVariants().collect { vars ->
                _uiState.update { it.copy(variants = vars) }
            }
        }

        viewModelScope.launch {
            stockBalanceDao.getAllBalances().collect { balances ->
                _uiState.update { it.copy(stockBalances = balances) }
            }
        }

        viewModelScope.launch {
            businessProfileDao.getProfile().collect { profile ->
                _uiState.update { it.copy(businessProfile = profile) }
            }
        }
    }

    fun loadDefaultReport() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            val summary = salesRepository.getSalesReportSummary(thirtyDaysAgo, now)
            val productReport = salesRepository.getProductWiseSalesReport(thirtyDaysAgo, now)
            val customerReport = salesRepository.getCustomerWiseSalesReport(thirtyDaysAgo, now)

            _uiState.update {
                it.copy(
                    salesReport = summary,
                    productWiseReport = productReport,
                    customerWiseReport = customerReport
                )
            }
        }
    }

    fun selectInvoice(invoice: SalesInvoice) {
        viewModelScope.launch {
            val items = salesRepository.getInvoiceItems(invoice.id)
            val allocations = salesRepository.getAllocationsForInvoiceSync(invoice.id)
            _uiState.update {
                it.copy(
                    selectedInvoice = invoice,
                    selectedInvoiceItems = items,
                    selectedInvoiceAllocations = allocations
                )
            }
        }
    }

    fun clearSelectedInvoice() {
        _uiState.update {
            it.copy(
                selectedInvoice = null,
                selectedInvoiceItems = emptyList(),
                selectedInvoiceAllocations = emptyList()
            )
        }
    }

    fun loadCustomerSummary(customerId: String) {
        viewModelScope.launch {
            val summary = salesRepository.getCustomerSummary(customerId)
            _uiState.update { it.copy(selectedCustomerSummary = summary) }
        }
    }

    fun createInvoice(draft: SalesInvoiceDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val newInvoiceId = salesRepository.createInvoice(draft)
                _uiState.update { it.copy(userMessage = "Invoice created successfully: $newInvoiceId") }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to create invoice: ${e.message}") }
            }
        }
    }

    fun createAndConfirmInvoice(draft: SalesInvoiceDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.createAndPostInvoice(draft)
            result.onSuccess { newInvoiceId ->
                _uiState.update { it.copy(userMessage = "Sale confirmed and posted: $newInvoiceId") }
                loadDefaultReport()
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Failed to confirm sale: ${err.message}") }
            }
        }
    }

    fun postInvoice(invoiceId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.postInvoice(invoiceId)
            result.onSuccess {
                _uiState.update { it.copy(userMessage = "Invoice posted! Stock and Accounting journals updated.") }
                val updated = salesRepository.getInvoiceById(invoiceId)
                if (updated != null) {
                    selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Post failed: ${err.message}") }
            }
        }
    }

    fun refreshNextPaymentNumber() {
        viewModelScope.launch {
            runCatching {
                val nextNum = salesRepository.previewNextCustomerPaymentNumber()
                _uiState.update { it.copy(nextPaymentNumber = nextNum) }
            }
        }
    }

    fun recordPayment(draft: CustomerPaymentDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val customer = _uiState.value.customers.find { it.id == draft.customerId }
                    ?: customerDao.getCustomerById(draft.customerId)
                val prevDue = customer?.outstandingBalanceMinor ?: 0L

                val paymentNumber = salesRepository.recordCustomerPayment(draft)
                val isBn = _uiState.value.language == AppLanguage.BANGLA
                val nextNum = runCatching { salesRepository.previewNextCustomerPaymentNumber() }.getOrNull()

                // Fetch updated ledger to guarantee exact source synchronization
                val ledger = salesRepository.getCustomerLedger(draft.customerId)
                val updatedCustomer = customerDao.getCustomerById(draft.customerId) ?: customer
                val currentDue = ledger.lastOrNull()?.runningBalanceMinor ?: (updatedCustomer?.outstandingBalanceMinor ?: (prevDue - draft.amountMinor))
                val isAdv = currentDue < 0L

                // Generate detailed customer ledger PDF statement
                val pdfFile = runCatching {
                    if (updatedCustomer != null) {
                        val pdfReport = CentralizedPdfReportRegistry.buildCustomerLedgerReport(
                            customer = updatedCustomer,
                            entries = ledger,
                            isBangla = isBn
                        )
                        CentralizedPdfEngine.generatePdf(context, pdfReport, _uiState.value.businessProfile)
                    } else null
                }.getOrNull()

                val profile = _uiState.value.businessProfile
                val stmtData = DueCollectionStatementData(
                    customerId = draft.customerId,
                    customerName = updatedCustomer?.name ?: "Customer",
                    customerPhone = updatedCustomer?.phone,
                    customerCode = updatedCustomer?.customerCode ?: "",
                    companyName = profile?.companyName ?: "ADVANCE ERP",
                    companyPhone = profile?.phone ?: "",
                    paymentNumber = paymentNumber,
                    paymentDate = draft.paymentDate,
                    paymentMethod = draft.paymentMethod,
                    amountReceivedMinor = draft.amountMinor,
                    previousDueMinor = prevDue,
                    currentDueMinor = currentDue,
                    isFullPayment = prevDue > 0L && currentDue == 0L,
                    isPartialPayment = currentDue > 0L && prevDue > currentDue,
                    isAdvancePayment = isAdv,
                    advanceAmountMinor = if (isAdv) kotlin.math.abs(currentDue) else 0L,
                    referenceNote = draft.referenceNote,
                    statementPdfFile = pdfFile
                )

                _uiState.update {
                    it.copy(
                        userMessage = if (isBn) "পেমেন্ট রশিদ সফলভাবে সংরক্ষিত হয়েছে: #$paymentNumber"
                                      else "Payment receipt recorded successfully: #$paymentNumber",
                        nextPaymentNumber = nextNum,
                        activeDueCollectionData = stmtData
                    )
                }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Payment failed: ${e.message}") }
            }
        }
    }

    fun openDueCollectionStatementShare(customer: Customer, recentPayment: CustomerPayment? = null) {
        viewModelScope.launch {
            try {
                val ledger = salesRepository.getCustomerLedger(customer.id)
                val isBn = _uiState.value.language == AppLanguage.BANGLA
                val pdfReport = CentralizedPdfReportRegistry.buildCustomerLedgerReport(
                    customer = customer,
                    entries = ledger,
                    isBangla = isBn
                )
                val pdfFile = CentralizedPdfEngine.generatePdf(context, pdfReport, _uiState.value.businessProfile)
                val profile = _uiState.value.businessProfile
                val currentDue = ledger.lastOrNull()?.runningBalanceMinor ?: customer.outstandingBalanceMinor
                val amountReceived = recentPayment?.amountMinor ?: 0L
                val prevDue = if (amountReceived > 0L) currentDue + amountReceived else currentDue
                val isAdv = currentDue < 0L

                val stmtData = DueCollectionStatementData(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    customerCode = customer.customerCode,
                    companyName = profile?.companyName ?: "ADVANCE ERP",
                    companyPhone = profile?.phone ?: "",
                    paymentNumber = recentPayment?.paymentNumber ?: "STMT-${System.currentTimeMillis() % 100000}",
                    paymentDate = recentPayment?.paymentDate ?: System.currentTimeMillis(),
                    paymentMethod = recentPayment?.paymentMethod ?: "CASH",
                    amountReceivedMinor = amountReceived,
                    previousDueMinor = prevDue,
                    currentDueMinor = currentDue,
                    isFullPayment = prevDue > 0L && currentDue == 0L,
                    isPartialPayment = currentDue > 0L && prevDue > currentDue,
                    isAdvancePayment = isAdv,
                    advanceAmountMinor = if (isAdv) kotlin.math.abs(currentDue) else 0L,
                    referenceNote = recentPayment?.referenceNote,
                    statementPdfFile = pdfFile
                )
                _uiState.update { it.copy(activeDueCollectionData = stmtData) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissDueCollectionDialog() {
        _uiState.update { it.copy(activeDueCollectionData = null) }
    }

    fun openDueReminder(customer: Customer) {
        viewModelScope.launch {
            try {
                val ledger = salesRepository.getCustomerLedger(customer.id)
                val isBn = _uiState.value.language == AppLanguage.BANGLA
                val pdfReport = CentralizedPdfReportRegistry.buildCustomerLedgerReport(
                    customer = customer,
                    entries = ledger,
                    isBangla = isBn
                )
                val pdfFile = CentralizedPdfEngine.generatePdf(context, pdfReport, _uiState.value.businessProfile)
                val profile = _uiState.value.businessProfile
                val currentDue = customer.outstandingBalanceMinor
                val invoiceCount = ledger.count { it.transactionType == "INVOICE" }

                val reminderData = DueReminderStatementData(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    customerCode = customer.customerCode,
                    companyName = profile?.companyName ?: "ADVANCE ERP",
                    companyPhone = profile?.phone ?: "",
                    totalDueMinor = currentDue,
                    invoiceCount = invoiceCount,
                    asOfDate = System.currentTimeMillis(),
                    paymentInstructions = if (isBn) "বিকাশ / নগদ / ব্যাংক অ্যাকাউন্টের মাধ্যমে পরিশোধ করা যাবে। বিস্তারিত জানতে যোগাযোগ করুন।" else "Payments accepted via Bank Transfer, bKash or Cash. Contact us for details.",
                    pdfFile = pdfFile
                )
                _uiState.update { it.copy(activeDueReminderData = reminderData) }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Due reminder preparation failed: ${e.message}") }
            }
        }
    }

    fun dismissDueReminderDialog() {
        _uiState.update { it.copy(activeDueReminderData = null) }
    }

    fun processReturn(draft: SalesReturnDraft, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val returnId = salesRepository.processSalesReturn(draft)
                _uiState.update { it.copy(userMessage = "Sales return processed and stock restored: $returnId") }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                loadDefaultReport()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Return failed: ${e.message}") }
            }
        }
    }

    fun loadCustomerLedger(customerId: String) {
        viewModelScope.launch {
            val ledger = salesRepository.getCustomerLedger(customerId)
            _uiState.update {
                it.copy(
                    selectedCustomerIdForLedger = customerId,
                    selectedCustomerLedger = ledger
                )
            }
        }
    }

    fun generateInvoicePdf(context: Context, invoice: SalesInvoice): File {
        val items = _uiState.value.selectedInvoiceItems.ifEmpty {
            kotlinx.coroutines.runBlocking { salesRepository.getInvoiceItems(invoice.id) }
        }
        return InvoicePdfGenerator.generateInvoicePdf(
            context = context,
            invoice = invoice,
            items = items,
            businessProfile = _uiState.value.businessProfile,
            isBangla = _uiState.value.language == AppLanguage.BANGLA
        )
    }

    fun generatePaymentReceiptPdf(context: Context, payment: CustomerPayment): File {
        val allocations = kotlinx.coroutines.runBlocking {
            salesRepository.getAllocationsForInvoiceSync(payment.id)
        }
        return InvoicePdfGenerator.generatePaymentReceiptPdf(
            context = context,
            payment = payment,
            allocations = allocations,
            businessProfile = _uiState.value.businessProfile,
            isBangla = _uiState.value.language == AppLanguage.BANGLA
        )
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun dispatchDelivery(draft: com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.dispatchDelivery(draft)
            result.onSuccess { challanNo ->
                _uiState.update { it.copy(userMessage = "Delivery challan dispatched: $challanNo") }
                if (_uiState.value.selectedInvoice != null) {
                    val updated = salesRepository.getInvoiceById(_uiState.value.selectedInvoice!!.id)
                    if (updated != null) selectInvoice(updated)
                }
                onSuccess(challanNo)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Delivery failed: ${err.message}") }
            }
        }
    }

    fun processDirectExchange(draft: com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.processDirectExchange(draft)
            result.onSuccess { exchNo ->
                _uiState.update { it.copy(userMessage = "Direct exchange completed successfully: $exchNo") }
                loadDefaultReport()
                onSuccess(exchNo)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Direct exchange failed: ${err.message}") }
            }
        }
    }

    fun convertQuotationToSalesOrder(quotationId: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.convertQuotationToSalesOrder(quotationId)
            result.onSuccess { orderId ->
                _uiState.update { it.copy(userMessage = "Quotation converted to Sales Order: $orderId") }
                val updated = salesRepository.getInvoiceById(quotationId)
                if (updated != null) selectInvoice(updated)
                onSuccess(orderId)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    fun convertSalesOrderToInvoice(salesOrderId: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.convertSalesOrderToInvoice(salesOrderId)
            result.onSuccess { invId ->
                _uiState.update { it.copy(userMessage = "Sales Order converted to Commercial Invoice: $invId") }
                val updated = salesRepository.getInvoiceById(salesOrderId)
                if (updated != null) selectInvoice(updated)
                onSuccess(invId)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    suspend fun getDeliveryChallansForInvoice(invoiceId: String) = salesRepository.getDeliveryChallansForInvoice(invoiceId)

    suspend fun getRemainingUndeliveredItems(invoiceId: String) = salesRepository.getRemainingUndeliveredItems(invoiceId)

    fun generateThermalReceiptPdf(
        context: Context,
        invoice: SalesInvoice,
        cashTenderedMinor: Long = invoice.paidAmountMinor,
        changeReturnedMinor: Long = 0L,
        is80mm: Boolean = true
    ): File {
        val items = _uiState.value.selectedInvoiceItems.ifEmpty {
            kotlinx.coroutines.runBlocking { salesRepository.getInvoiceItems(invoice.id) }
        }
        return InvoicePdfGenerator.generateThermalReceiptPdf(
            context = context,
            invoice = invoice,
            items = items,
            businessProfile = _uiState.value.businessProfile,
            cashTenderedMinor = cashTenderedMinor,
            changeReturnedMinor = changeReturnedMinor,
            isBangla = _uiState.value.language == AppLanguage.BANGLA,
            is80mm = is80mm
        )
    }

    fun voidCustomerPayment(paymentId: String, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.voidCustomerPayment(paymentId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Payment receipt voided successfully") }
                loadDefaultReport()
                onSuccess()
            } else {
                _uiState.update { it.copy(userMessage = "Failed to void payment: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun cancelOrDeleteInvoice(invoiceId: String, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = salesRepository.cancelOrDeleteInvoice(invoiceId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Invoice cancelled/deleted successfully", selectedInvoice = null) }
                loadDefaultReport()
                onSuccess()
            } else {
                _uiState.update { it.copy(userMessage = "Failed to cancel invoice: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
}
