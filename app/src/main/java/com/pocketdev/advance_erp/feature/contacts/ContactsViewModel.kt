package com.pocketdev.advance_erp.feature.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfEngine
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.core.util.DueReminderStatementData
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ContactsTab {
    CUSTOMERS,
    SUPPLIERS
}

data class ContactsUiState(
    val selectedTab: ContactsTab = ContactsTab.CUSTOMERS,
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val showAddDialog: Boolean = false,
    val userMessage: String? = null,
    val activeDueReminderData: DueReminderStatementData? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val salesRepository: SalesRepository,
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ContactsTab.CUSTOMERS)
    private val _showAddDialog = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _activeDueReminderData = MutableStateFlow<DueReminderStatementData?>(null)

    val uiState: StateFlow<ContactsUiState> = combine(
        _selectedTab,
        combine(contactsRepository.getAllCustomers(), contactsRepository.getAllSuppliers()) { c, s -> Pair(c, s) },
        combine(preferencesRepository.selectedLanguage, preferencesRepository.selectedCurrency) { l, c -> Pair(l, c) },
        _showAddDialog,
        _userMessage,
        _activeDueReminderData
    ) { args: Array<Any?> ->
        val tab = args[0] as ContactsTab
        val (customers, suppliers) = args[1] as Pair<List<Customer>, List<Supplier>>
        val (language, currency) = args[2] as Pair<AppLanguage, CurrencyCode>
        val showAdd = args[3] as Boolean
        val msg = args[4] as? String
        val dueReminder = args[5] as? DueReminderStatementData
        ContactsUiState(
            selectedTab = tab,
            customers = customers,
            suppliers = suppliers,
            language = language,
            currency = currency,
            showAddDialog = showAdd,
            userMessage = msg,
            activeDueReminderData = dueReminder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactsUiState()
    )

    fun onTabSelect(tab: ContactsTab) {
        _selectedTab.value = tab
    }

    fun setAddDialogVisible(visible: Boolean) {
        _showAddDialog.value = visible
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun addCustomer(
        name: String,
        phone: String,
        email: String?,
        address: String,
        creditLimitMinor: Long,
        profileImagePath: String? = null
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                customerCode = "CUST-${System.currentTimeMillis() % 10000}",
                name = name.trim(),
                phone = phone.trim(),
                email = email?.trim()?.ifBlank { null },
                address = address.trim(),
                creditLimitMinor = creditLimitMinor,
                outstandingBalanceMinor = 0L,
                profileImagePath = profileImagePath,
                isActive = true
            )
            contactsRepository.addCustomer(customer)
            _showAddDialog.value = false
            _userMessage.value = "Customer added successfully"
        }
    }

    fun updateCustomerPhoto(customer: Customer, photoPath: String?) {
        viewModelScope.launch {
            contactsRepository.updateCustomer(customer.copy(profileImagePath = photoPath))
            _userMessage.value = if (photoPath != null) "গ্রাহকের প্রোফাইল ছবি সংরক্ষিত হয়েছে" else "ছবি সরানো হয়েছে"
        }
    }

    fun openDueReminder(customer: Customer) {
        viewModelScope.launch {
            try {
                val ledger = salesRepository.getCustomerLedger(customer.id)
                val isBn = uiState.value.language == AppLanguage.BANGLA
                val profile = settingsRepository.getBusinessProfile().firstOrNull()
                val pdfReport = CentralizedPdfReportRegistry.buildCustomerLedgerReport(
                    customer = customer,
                    entries = ledger,
                    isBangla = isBn
                )
                val pdfFile = CentralizedPdfEngine.generatePdf(context, pdfReport, profile)
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
                    paymentInstructions = if (isBn) "বিকাশ / নগদ / ব্যাংক অ্যাকাউন্টের মাধ্যমে পরিশোধ করা যাবে।" else "Payments accepted via Bank Transfer, bKash or Cash.",
                    pdfFile = pdfFile
                )
                _activeDueReminderData.value = reminderData
            } catch (e: Exception) {
                _userMessage.value = "বকেয়া তাগাদা প্রস্তুত করতে ব্যর্থ: ${e.message}"
            }
        }
    }

    fun dismissDueReminderDialog() {
        _activeDueReminderData.value = null
    }

    fun deleteOrArchiveCustomer(customerId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.deleteOrArchiveCustomer(customerId)) {
                is CustomerDeleteResult.Blocked -> {
                    _userMessage.value = result.reason
                }
                is CustomerDeleteResult.Archived -> {
                    _userMessage.value = result.message
                }
                is CustomerDeleteResult.HardDeleted -> {
                    _userMessage.value = "Customer deleted permanently (no financial records found)"
                }
            }
        }
    }

    fun addSupplier(companyName: String, contactPerson: String?, phone: String, email: String?, address: String, country: String, currency: String) {
        viewModelScope.launch {
            val supplier = Supplier(
                id = UUID.randomUUID().toString(),
                supplierCode = "SUP-${System.currentTimeMillis() % 10000}",
                companyName = companyName.trim(),
                contactPerson = contactPerson?.trim()?.ifBlank { null },
                phone = phone.trim(),
                email = email?.trim()?.ifBlank { null },
                address = address.trim(),
                country = country.trim(),
                currency = currency.trim(),
                outstandingPayableMinor = 0L,
                isActive = true
            )
            contactsRepository.addSupplier(supplier)
            _showAddDialog.value = false
            _userMessage.value = "Supplier added successfully"
        }
    }
}
