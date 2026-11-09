package com.pocketdev.advance_erp.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val userMessage: String? = null
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ContactsTab.CUSTOMERS)
    private val _showAddDialog = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactsUiState> = combine(
        _selectedTab,
        combine(contactsRepository.getAllCustomers(), contactsRepository.getAllSuppliers()) { c, s -> Pair(c, s) },
        combine(preferencesRepository.selectedLanguage, preferencesRepository.selectedCurrency) { l, c -> Pair(l, c) },
        _showAddDialog,
        _userMessage
    ) { tab, contactsPair, prefsPair, showAdd, msg ->
        val (customers, suppliers) = contactsPair
        val (language, currency) = prefsPair
        ContactsUiState(
            selectedTab = tab,
            customers = customers,
            suppliers = suppliers,
            language = language,
            currency = currency,
            showAddDialog = showAdd,
            userMessage = msg
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

    fun addCustomer(name: String, phone: String, email: String?, address: String, creditLimitMinor: Long) {
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
                isActive = true
            )
            contactsRepository.addCustomer(customer)
            _showAddDialog.value = false
            _userMessage.value = "Customer added successfully"
        }
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
