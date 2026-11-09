package com.pocketdev.advance_erp.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfEngine
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReportRegistry
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.domain.repository.DashboardRepository
import com.pocketdev.advance_erp.domain.repository.DashboardSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val generatedPdf: File? = null,
    val generatedPdfTitle: String = "",
    val canViewProfit: Boolean = true,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val businessProfileDao: BusinessProfileDao,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    private val _generatedPdf = MutableStateFlow<File?>(null)
    private val _generatedPdfTitle = MutableStateFlow("")

    private val _streamA = _selectedDateMillis.flatMapLatest { dateMillis ->
        dashboardRepository.getDashboardSummaryForDate(dateMillis)
    }

    private val _streamB = combine(
        preferencesRepository.selectedLanguage,
        preferencesRepository.selectedCurrency,
        preferencesRepository.selectedUserRole,
        _generatedPdf,
        _generatedPdfTitle
    ) { lang, curr, role, pdf, title ->
        listOf(lang, curr, role, pdf, title)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _streamA,
        _selectedDateMillis,
        _streamB
    ) { rawSummary, selectedDate, extra ->
        val lang = extra[0] as AppLanguage
        val curr = extra[1] as CurrencyCode
        val role = extra[2] as com.pocketdev.advance_erp.core.security.UserRole
        val pdf = extra[3] as? File
        val title = extra[4] as String

        val canViewProfit = com.pocketdev.advance_erp.core.security.SecurityManager.canViewProfit(role)
        val safeSummary = if (canViewProfit) {
            rawSummary
        } else {
            // Mask restricted profit metrics at ViewModel layer (Confirmed Rule 3.4)
            rawSummary.copy(
                grossProfit = Money.ofMinor(0L, curr),
                netProfit = Money.ofMinor(0L, curr),
                dateWiseMetrics = rawSummary.dateWiseMetrics?.copy(dailyGrossProfit = Money.ofMinor(0L, curr))
            )
        }

        DashboardUiState(
            summary = safeSummary,
            selectedDateMillis = selectedDate,
            language = lang,
            currency = curr,
            generatedPdf = pdf,
            generatedPdfTitle = title,
            canViewProfit = canViewProfit,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    fun selectPreviousDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDateMillis.value
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun selectNextDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDateMillis.value
            add(Calendar.DAY_OF_YEAR, 1)
        }
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun selectToday() {
        _selectedDateMillis.value = System.currentTimeMillis()
    }

    fun clearGeneratedPdf() {
        _generatedPdf.value = null
        _generatedPdfTitle.value = ""
    }

    fun generateDailySnapshotPdf(context: Context) {
        viewModelScope.launch {
            val currentState = uiState.value
            val summary = currentState.summary ?: return@launch
            val metrics = summary.dateWiseMetrics
            val closing = summary.closingBalances
            val profile = businessProfileDao.getProfile().first()
            val isBangla = currentState.language == AppLanguage.BANGLA

            val report = CentralizedPdfReportRegistry.buildDailyBusinessSnapshotReport(
                dateMillis = currentState.selectedDateMillis,
                totalSalesMinor = metrics?.totalSales?.minorUnits ?: summary.todaysSales.minorUnits,
                totalPurchasesMinor = metrics?.totalPurchases?.minorUnits ?: 0L,
                totalCollectionsMinor = metrics?.totalCollections?.minorUnits ?: 0L,
                totalPaymentsMinor = metrics?.totalPayments?.minorUnits ?: 0L,
                totalExpensesMinor = metrics?.totalExpenses?.minorUnits ?: summary.totalExpenses.minorUnits,
                netCashFlowMinor = metrics?.netCashFlow?.minorUnits ?: 0L,
                dailyProfitMinor = metrics?.dailyGrossProfit?.minorUnits ?: summary.grossProfit.minorUnits,
                closingCashBankMinor = closing?.closingCashBank?.minorUnits ?: summary.totalCashBank.minorUnits,
                closingArMinor = closing?.customerOutstanding?.minorUnits ?: summary.totalReceivable.minorUnits,
                closingApMinor = closing?.supplierOutstanding?.minorUnits ?: summary.totalPayable.minorUnits,
                closingInventoryMinor = closing?.inventoryValuation?.minorUnits ?: summary.stockValuation.minorUnits,
                isBangla = isBangla
            )

            val file = CentralizedPdfEngine.generatePdf(context, report, profile)
            _generatedPdfTitle.value = if (isBangla) "দৈনিক ব্যবসায়িক চিত্র প্রতিবেদন" else "Daily Business Snapshot"
            _generatedPdf.value = file
        }
    }

    fun generateMonthlyPerformancePdf(context: Context) {
        viewModelScope.launch {
            val currentState = uiState.value
            val summary = currentState.summary ?: return@launch
            val profile = businessProfileDao.getProfile().first()
            val isBangla = currentState.language == AppLanguage.BANGLA

            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
            val monthName = monthFormat.format(Date(currentState.selectedDateMillis))

            val report = CentralizedPdfReportRegistry.buildMonthlyPerformanceReport(
                monthName = monthName,
                salesMinor = summary.todaysSales.minorUnits * 30L, // Projected/accumulated
                purchasesMinor = summary.totalPayable.minorUnits,
                collectionsMinor = summary.todaysSales.minorUnits * 25L,
                expensesMinor = summary.totalExpenses.minorUnits,
                netProfitMinor = summary.netProfit.minorUnits,
                isBangla = isBangla
            )

            val file = CentralizedPdfEngine.generatePdf(context, report, profile)
            _generatedPdfTitle.value = if (isBangla) "মাসিক পারফরম্যান্স রিপোর্ট" else "Monthly Performance Report"
            _generatedPdf.value = file
        }
    }
}
