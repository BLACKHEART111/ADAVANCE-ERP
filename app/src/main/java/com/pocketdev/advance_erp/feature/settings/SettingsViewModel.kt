package com.pocketdev.advance_erp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val profile: BusinessProfile? = null,
    val userMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.selectedLanguage,
        preferencesRepository.selectedCurrency,
        settingsRepository.getBusinessProfile(),
        _userMessage
    ) { language, currency, profile, message ->
        SettingsUiState(
            language = language,
            currency = currency,
            profile = profile,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
            _userMessage.value = "Language changed to ${language.displayName}"
        }
    }

    fun onCurrencySelected(currency: CurrencyCode) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(currency)
            _userMessage.value = "Currency updated to ${currency.code}"
        }
    }

    fun updateProfile(name: String, taxNumber: String?, phone: String, email: String?, address: String) {
        viewModelScope.launch {
            val updated = BusinessProfile(
                id = "default_profile",
                companyName = name.trim(),
                taxNumber = taxNumber?.trim()?.ifBlank { null },
                phone = phone.trim(),
                email = email?.trim()?.ifBlank { null },
                address = address.trim(),
                defaultCurrency = uiState.value.currency.code,
                updatedAt = System.currentTimeMillis()
            )
            settingsRepository.updateBusinessProfile(updated)
            _userMessage.value = "Business profile saved"
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }
}
