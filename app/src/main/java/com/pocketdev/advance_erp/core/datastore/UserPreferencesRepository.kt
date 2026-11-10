package com.pocketdev.advance_erp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyLanguage = stringPreferencesKey("app_language")
    private val keyCurrency = stringPreferencesKey("app_currency")
    private val keyProfilePhotoUri = stringPreferencesKey("profile_photo_uri")
    private val keyCompanyLogoPath = stringPreferencesKey("company_logo_path")
    private val keyUserName = stringPreferencesKey("user_profile_name")
    private val keyUserRole = stringPreferencesKey("user_role")

    // Security & App Lock Keys
    private val keyAppLockEnabled = booleanPreferencesKey("app_lock_enabled")
    private val keyPinCodeHash = stringPreferencesKey("pin_code_hash")
    private val keyPinSalt = stringPreferencesKey("pin_salt")
    private val keyBiometricEnabled = booleanPreferencesKey("biometric_enabled")

    // Automatic Friday Backup Keys
    private val keyFridayAutoBackup = booleanPreferencesKey("friday_auto_backup_enabled")
    private val keyLastAutoBackupTime = longPreferencesKey("last_auto_backup_time")

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyAppLockEnabled] ?: false }

    val pinCodeHash: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyPinCodeHash] }

    val pinSalt: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyPinSalt] }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyBiometricEnabled] ?: false }

    val isFridayAutoBackupEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyFridayAutoBackup] ?: true }

    val lastAutoBackupTime: Flow<Long?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[keyLastAutoBackupTime] }

    val selectedUserRole: Flow<com.pocketdev.advance_erp.core.security.UserRole> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val code = preferences[keyUserRole] ?: com.pocketdev.advance_erp.core.security.UserRole.OWNER_ADMIN.code
            com.pocketdev.advance_erp.core.security.UserRole.fromCode(code)
        }

    val selectedLanguage: Flow<AppLanguage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val code = preferences[keyLanguage] ?: "bn"
            AppLanguage.fromCode(code)
        }

    val selectedCurrency: Flow<CurrencyCode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val code = preferences[keyCurrency] ?: "BDT"
            CurrencyCode.fromCode(code)
        }

    val profilePhotoUri: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[keyProfilePhotoUri]
        }

    val companyLogoPath: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[keyCompanyLogoPath]
        }

    val userName: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[keyUserName]
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[keyLanguage] = language.code
        }
    }

    suspend fun setCurrency(currency: CurrencyCode) {
        context.dataStore.edit { preferences ->
            preferences[keyCurrency] = currency.code
        }
    }

    suspend fun setProfilePhotoUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[keyProfilePhotoUri] = uri
            } else {
                preferences.remove(keyProfilePhotoUri)
            }
        }
    }

    suspend fun setCompanyLogoPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[keyCompanyLogoPath] = path
            } else {
                preferences.remove(keyCompanyLogoPath)
            }
        }
    }

    suspend fun setUserName(name: String?) {
        context.dataStore.edit { preferences ->
            if (!name.isNullOrBlank()) {
                preferences[keyUserName] = name
            } else {
                preferences.remove(keyUserName)
            }
        }
    }

    suspend fun setUserRole(role: com.pocketdev.advance_erp.core.security.UserRole) {
        context.dataStore.edit { preferences ->
            preferences[keyUserRole] = role.code
        }
    }

    suspend fun setAppLock(enabled: Boolean, pinHash: String? = null, salt: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[keyAppLockEnabled] = enabled
            if (pinHash != null && salt != null) {
                preferences[keyPinCodeHash] = pinHash
                preferences[keyPinSalt] = salt
            } else if (!enabled) {
                preferences.remove(keyPinCodeHash)
                preferences.remove(keyPinSalt)
                preferences[keyBiometricEnabled] = false
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyBiometricEnabled] = enabled
        }
    }

    suspend fun setFridayAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyFridayAutoBackup] = enabled
        }
    }

    suspend fun setLastAutoBackupTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[keyLastAutoBackupTime] = time
        }
    }
}
