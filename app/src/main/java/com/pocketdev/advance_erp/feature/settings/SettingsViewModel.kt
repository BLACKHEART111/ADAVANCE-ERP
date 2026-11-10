package com.pocketdev.advance_erp.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.BiometricHelper
import com.pocketdev.advance_erp.core.security.PinSecurityHelper
import com.pocketdev.advance_erp.core.worker.FridayBackupScheduler
import com.pocketdev.advance_erp.data.entity.BackupRecord
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.BANGLA,
    val currency: CurrencyCode = CurrencyCode.BDT,
    val profile: BusinessProfile? = null,
    val companyLogoPath: String? = null,
    val isAppLockEnabled: Boolean = false,
    val pinCodeHash: String? = null,
    val pinSalt: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isBiometricSupported: Boolean = false,
    val isFridayAutoBackupEnabled: Boolean = true,
    val lastAutoBackupTime: Long? = null,
    val backupRecords: List<BackupRecord> = emptyList(),
    val userMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.selectedLanguage,
        preferencesRepository.selectedCurrency,
        settingsRepository.getBusinessProfile(),
        preferencesRepository.companyLogoPath,
        preferencesRepository.isAppLockEnabled,
        preferencesRepository.pinCodeHash,
        preferencesRepository.pinSalt,
        preferencesRepository.isBiometricEnabled,
        preferencesRepository.isFridayAutoBackupEnabled,
        preferencesRepository.lastAutoBackupTime,
        backupRepository.getAllBackupRecords(),
        _userMessage
    ) { args: Array<Any?> ->
        val profile = args[2] as? BusinessProfile
        val prefLogoPath = args[3] as? String
        val effectiveLogoPath = prefLogoPath ?: profile?.logoPath
        SettingsUiState(
            language = args[0] as AppLanguage,
            currency = args[1] as CurrencyCode,
            profile = profile?.copy(logoPath = effectiveLogoPath) ?: profile,
            companyLogoPath = effectiveLogoPath,
            isAppLockEnabled = args[4] as Boolean,
            pinCodeHash = args[5] as? String,
            pinSalt = args[6] as? String,
            isBiometricEnabled = args[7] as Boolean,
            isBiometricSupported = BiometricHelper.isBiometricAvailable(context),
            isFridayAutoBackupEnabled = args[8] as Boolean,
            lastAutoBackupTime = args[9] as? Long,
            backupRecords = (args[10] as? List<*>)?.filterIsInstance<BackupRecord>() ?: emptyList(),
            userMessage = args[11] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isBiometricSupported = BiometricHelper.isBiometricAvailable(context))
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
            val currentLogo = uiState.value.companyLogoPath ?: uiState.value.profile?.logoPath
            val updated = BusinessProfile(
                id = "default_profile",
                companyName = name.trim(),
                taxNumber = taxNumber?.trim()?.ifBlank { null },
                phone = phone.trim(),
                email = email?.trim()?.ifBlank { null },
                address = address.trim(),
                defaultCurrency = uiState.value.currency.code,
                logoPath = currentLogo,
                updatedAt = System.currentTimeMillis()
            )
            settingsRepository.updateBusinessProfile(updated)
            _userMessage.value = "Business profile saved"
        }
    }

    fun updateCompanyLogo(logoPath: String?) {
        viewModelScope.launch {
            preferencesRepository.setCompanyLogoPath(logoPath)
            val profile = uiState.value.profile
            if (profile != null) {
                settingsRepository.updateBusinessProfile(profile.copy(logoPath = logoPath))
            }
            _userMessage.value = if (logoPath != null) "কোম্পানি লোগো সংরক্ষিত হয়েছে।" else "কোম্পানি লোগো সরানো হয়েছে।"
        }
    }

    fun setAppLock(pin: String) {
        viewModelScope.launch {
            val salt = PinSecurityHelper.generateSalt()
            val hash = PinSecurityHelper.hashPin(pin, salt)
            preferencesRepository.setAppLock(enabled = true, pinHash = hash, salt = salt)
            _userMessage.value = "নিরাপত্তা পিন কোড সফলভাবে সেট করা হয়েছে।"
        }
    }

    fun changePin(newPin: String) {
        viewModelScope.launch {
            val salt = PinSecurityHelper.generateSalt()
            val hash = PinSecurityHelper.hashPin(newPin, salt)
            preferencesRepository.setAppLock(enabled = true, pinHash = hash, salt = salt)
            _userMessage.value = "পিন কোড সফলভাবে পরিবর্তন করা হয়েছে।"
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            preferencesRepository.setAppLock(enabled = false)
            _userMessage.value = "অ্যাপ নিরাপত্তা লক নিষ্ক্রিয় করা হয়েছে।"
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
            _userMessage.value = if (enabled) "ফিঙ্গারপ্রিন্ট আনলক সক্রিয় করা হয়েছে।" else "ফিঙ্গারপ্রিন্ট আনলক নিষ্ক্রিয় করা হয়েছে।"
        }
    }

    fun toggleFridayAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFridayAutoBackupEnabled(enabled)
            if (enabled) {
                FridayBackupScheduler.schedule(context)
                _userMessage.value = "প্রতি শুক্রবারের স্বয়ংক্রিয় ব্যাকআপ সক্রিয় করা হয়েছে।"
            } else {
                FridayBackupScheduler.cancel(context)
                _userMessage.value = "স্বয়ংক্রিয় ব্যাকআপ বন্ধ করা হয়েছে।"
            }
        }
    }

    fun createInstantBackup(onFinished: () -> Unit) {
        viewModelScope.launch {
            try {
                val record = backupRepository.createBackup(
                    isAutomatic = false,
                    notes = "Settings Manual Backup"
                )
                _userMessage.value = "ব্যাকআপ সফল: ${record.fileName}"
            } catch (e: Exception) {
                _userMessage.value = "ব্যাকআপ তৈরি ব্যর্থ হয়েছে: ${e.localizedMessage}"
            } finally {
                onFinished()
            }
        }
    }

    fun restoreBackup(file: File) {
        viewModelScope.launch {
            try {
                val res = backupRepository.restoreBackup(file)
                if (res.isSuccess) {
                    _userMessage.value = "ডাটাবেজ এবং ছবি সফলভাবে রিস্টোর করা হয়েছে!"
                } else {
                    _userMessage.value = "রিস্টোর ব্যর্থ: ${res.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _userMessage.value = "রিস্টোর ব্যর্থ: ${e.localizedMessage}"
            }
        }
    }

    fun restoreFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val res = backupRepository.restoreFromUri(context, uri)
                if (res.isSuccess) {
                    _userMessage.value = "নির্বাচিত জিপ ফাইল থেকে সফলভাবে রিস্টোর সম্পন্ন হয়েছে!"
                } else {
                    _userMessage.value = "রিস্টোর ব্যর্থ: ${res.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _userMessage.value = "রিস্টোর ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

    fun shareBackup(file: File) {
        try {
            backupRepository.shareBackup(context, file)
        } catch (e: Exception) {
            _userMessage.value = "ফাইল শেয়ার ত্রুটি: ${e.localizedMessage}"
        }
    }

    fun deleteBackup(recordId: String) {
        viewModelScope.launch {
            val deleted = backupRepository.deleteBackup(recordId)
            if (deleted) {
                _userMessage.value = "ব্যাকআপ ফাইল মুছে ফেলা হয়েছে।"
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }
}
