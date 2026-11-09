package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.AppSettingsEntity
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getBusinessProfile(): Flow<BusinessProfile?>
    suspend fun updateBusinessProfile(profile: BusinessProfile)
    fun getSettings(): Flow<AppSettingsEntity?>
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateCurrency(currency: CurrencyCode)
}
