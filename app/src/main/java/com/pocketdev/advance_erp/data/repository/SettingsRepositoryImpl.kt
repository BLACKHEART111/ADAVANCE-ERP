package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.dao.AppSettingsDao
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.entity.AppSettingsEntity
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val profileDao: BusinessProfileDao,
    private val settingsDao: AppSettingsDao,
    private val preferencesRepository: UserPreferencesRepository
) : SettingsRepository {

    override fun getBusinessProfile(): Flow<BusinessProfile?> = profileDao.getProfile()

    override suspend fun updateBusinessProfile(profile: BusinessProfile) {
        profileDao.insertOrUpdate(profile)
    }

    override fun getSettings(): Flow<AppSettingsEntity?> = settingsDao.getSettings()

    override suspend fun updateLanguage(language: AppLanguage) {
        settingsDao.updateLanguage(language.code)
        preferencesRepository.setLanguage(language)
    }

    override suspend fun updateCurrency(currency: CurrencyCode) {
        settingsDao.updateCurrency(currency.code)
        preferencesRepository.setCurrency(currency)
    }
}
