package com.pocketdev.advance_erp

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpTheme
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import com.pocketdev.advance_erp.navigation.MainAppScaffold
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var backupRepository: BackupRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val language by preferencesRepository.selectedLanguage.collectAsState(initial = AppLanguage.BANGLA)

            val currentConfig = LocalConfiguration.current
            val localizedConfig = remember(language, currentConfig) {
                Configuration(currentConfig).apply {
                    setLocale(language.locale)
                }
            }

            LaunchedEffect(language) {
                Locale.setDefault(language.locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(localizedConfig, resources.displayMetrics)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig
            ) {
                AdvanceErpTheme {
                    MainAppScaffold(
                        preferencesRepository = preferencesRepository,
                        settingsRepository = settingsRepository,
                        backupRepository = backupRepository
                    )
                }
            }
        }
    }
}
