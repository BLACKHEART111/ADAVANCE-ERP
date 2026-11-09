package com.pocketdev.advance_erp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
    private val keyUserName = stringPreferencesKey("user_profile_name")
    private val keyUserRole = stringPreferencesKey("user_role")

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
}
