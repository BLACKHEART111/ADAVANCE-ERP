package com.pocketdev.advance_erp.feature.controlcenter

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.ControlCenterExcelGenerator
import com.pocketdev.advance_erp.core.util.ControlCenterPdfGenerator
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SavedFilterPreset
import com.pocketdev.advance_erp.domain.model.TransactionDetailRecord
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusinessControlCenterViewModel @Inject constructor(
    private val repository: BusinessControlCenterRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ControlCenterFilterState())
    val filterState: StateFlow<ControlCenterFilterState> = _filterState.asStateFlow()

    val transactions: StateFlow<List<UnifiedTransactionItem>> = _filterState
        .flatMapLatest { filter -> repository.getTransactions(filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryDashboard: StateFlow<BusinessSummaryDashboardData> = _filterState
        .flatMapLatest { filter -> repository.getSummaryDashboard(filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSummaryDashboardData())

    val customers: StateFlow<List<Customer>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouses: StateFlow<List<Warehouse>> = repository.getAllWarehouses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<ProductCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPresets: StateFlow<List<SavedFilterPreset>> = repository.getSavedPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRole: StateFlow<UserRole> = preferencesRepository.selectedUserRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.OWNER_ADMIN)

    val appLanguage: StateFlow<AppLanguage> = preferencesRepository.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.BANGLA)

    val currencyCode: StateFlow<CurrencyCode> = preferencesRepository.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyCode.BDT)

    val businessProfile: StateFlow<BusinessProfile?> = repository.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTransactionDetail = MutableStateFlow<TransactionDetailRecord?>(null)
    val selectedTransactionDetail: StateFlow<TransactionDetailRecord?> = _selectedTransactionDetail.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun updateFilter(filter: ControlCenterFilterState) {
        _filterState.value = filter
    }

    fun selectCategory(category: ReportCategory) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun selectDateRange(option: DateRangeOption, customStart: Long? = null, customEnd: Long? = null) {
        _filterState.value = _filterState.value.copy(
            dateRangeOption = option,
            customStartDate = customStart,
            customEndDate = customEnd
        )
    }

    fun setCustomStartDate(startMillis: Long) {
        val currentEnd = _filterState.value.customEndDate ?: System.currentTimeMillis()
        val adjustedEnd = if (startMillis > currentEnd) startMillis else currentEnd
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = startMillis,
            customEndDate = adjustedEnd
        )
    }

    fun setCustomEndDate(endMillis: Long) {
        val currentStart = _filterState.value.customStartDate ?: (endMillis - (7L * 24 * 60 * 60 * 1000L))
        val adjustedStart = if (endMillis < currentStart) endMillis else currentStart
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = adjustedStart,
            customEndDate = endMillis
        )
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val s = minOf(startMillis, endMillis)
        val e = maxOf(startMillis, endMillis)
        _filterState.value = _filterState.value.copy(
            dateRangeOption = DateRangeOption.CUSTOM,
            customStartDate = s,
            customEndDate = e
        )
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun resetFilters() {
        _filterState.value = ControlCenterFilterState()
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            repository.savePreset(name, _filterState.value)
        }
    }

    fun applyPreset(preset: SavedFilterPreset) {
        _filterState.value = preset.filterState
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            repository.deletePreset(presetId)
        }
    }

    fun openTransactionDetail(rawType: String, id: String) {
        viewModelScope.launch {
            val record = repository.getTransactionDetails(rawType, id)
            _selectedTransactionDetail.value = record
        }
    }

    fun dismissTransactionDetail() {
        _selectedTransactionDetail.value = null
    }

    private fun getReportTitle(): String {
        val cat = _filterState.value.category
        val isBn = appLanguage.value == AppLanguage.BANGLA
        return if (cat == ReportCategory.ALL_TRANSACTIONS) {
            if (isBn) "ব্যবসা নিয়ন্ত্রণ কেন্দ্র - সম্পূর্ণ বিবরণী" else "Business Control Center - Master Ledger"
        } else {
            if (isBn) "${cat.titleBn} প্রতিবেদন" else "${cat.titleEn} Report"
        }
    }

    fun exportAndSharePdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val canView = SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(),
                    filterState = _filterState.value,
                    summary = summaryDashboard.value,
                    transactions = transactions.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(context, file, "application/pdf", "Share PDF Report via")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "PDF Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAndShareExcel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val canView = SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterExcelGenerator.exportToXlsx(
                    context = context,
                    reportTitle = getReportTitle(),
                    filterState = _filterState.value,
                    summary = summaryDashboard.value,
                    transactions = transactions.value,
                    customers = customers.value,
                    suppliers = suppliers.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(
                        context,
                        file,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "Share Excel (XLSX) Workbook via"
                    )
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Excel Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAndShareCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val file = ControlCenterExcelGenerator.exportToCsv(
                    context = context,
                    reportTitle = getReportTitle(),
                    transactions = transactions.value
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.shareFile(context, file, "text/csv", "Share CSV Report via")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "CSV Generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun printReport(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val canView = SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(),
                    filterState = _filterState.value,
                    summary = summaryDashboard.value,
                    transactions = transactions.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.printPdf(context, file, "Advance_ERP_${System.currentTimeMillis()}")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun savePdfToStorage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val canView = SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterPdfGenerator.generateReportPdf(
                    context = context,
                    title = getReportTitle(),
                    filterState = _filterState.value,
                    summary = summaryDashboard.value,
                    transactions = transactions.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView,
                    isBangla = appLanguage.value == AppLanguage.BANGLA
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.saveToDownloads(context, file)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveExcelToStorage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val canView = SecurityManager.canViewProfit(userRole.value)
                val file = ControlCenterExcelGenerator.exportToXlsx(
                    context = context,
                    reportTitle = getReportTitle(),
                    filterState = _filterState.value,
                    summary = summaryDashboard.value,
                    transactions = transactions.value,
                    customers = customers.value,
                    suppliers = suppliers.value,
                    businessProfile = businessProfile.value,
                    canViewProfit = canView
                )
                launch(Dispatchers.Main) {
                    ControlCenterShareService.saveToDownloads(context, file)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }
}
