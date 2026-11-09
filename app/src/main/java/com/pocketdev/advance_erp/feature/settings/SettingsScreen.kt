package com.pocketdev.advance_erp.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpLogo
import com.pocketdev.advance_erp.core.designsystem.BrandConfig
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var companyName by remember(state.profile) { mutableStateOf(state.profile?.companyName ?: "") }
    var taxNumber by remember(state.profile) { mutableStateOf(state.profile?.taxNumber ?: "") }
    var phone by remember(state.profile) { mutableStateOf(state.profile?.phone ?: "") }
    var address by remember(state.profile) { mutableStateOf(state.profile?.address ?: "") }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ERPBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Official Brand Showcase Header
            item {
                ERPCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AdvanceErpLogo(
                            variant = BrandConfig.LogoVariant.HORIZONTAL,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = BrandConfig.TAGLINE_EN,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = BrandConfig.TAGLINE_BN,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            // Language Selection
            item {
                Column {
                    ERPSectionHeader(
                        title = stringResource(R.string.language_selection),
                        subtitle = "Select default application language"
                    )
                    ERPCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = state.language == lang
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryPurple.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { viewModel.onLanguageSelected(lang) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (isSelected) PrimaryPurple else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = lang.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryPurple else TextPrimary
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Currency Selection
            item {
                Column {
                    ERPSectionHeader(
                        title = stringResource(R.string.currency_selection),
                        subtitle = "Primary operational currency for trade calculations"
                    )
                    ERPCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyCode.entries.forEach { curr ->
                                val isSelected = state.currency == curr
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SecondaryTeal.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { viewModel.onCurrencySelected(curr) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) SecondaryTeal else ERPDivider,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = curr.symbol,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "${curr.code} (${curr.symbol})",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) SecondaryTeal else TextPrimary
                                            )
                                            Text(
                                                text = if (state.language == AppLanguage.BANGLA) curr.displayNameBn else curr.displayNameEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Business Profile
            item {
                Column {
                    ERPSectionHeader(
                        title = stringResource(R.string.business_profile),
                        subtitle = "Company details printed on invoices & reports"
                    )
                    ERPCard {
                        Column {
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text(stringResource(R.string.company_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = taxNumber,
                                onValueChange = { taxNumber = it },
                                label = { Text(stringResource(R.string.tax_id)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(stringResource(R.string.contact_phone)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text(stringResource(R.string.contact_address)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ERPActionButton(
                                text = stringResource(R.string.save_changes),
                                onClick = {
                                    viewModel.updateProfile(companyName, taxNumber, phone, null, address)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // System Info
            item {
                Column {
                    ERPSectionHeader(title = stringResource(R.string.app_info))
                    ERPCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemInfoRow("System Mode", "Single Device • Offline-First")
                            Divider(color = ERPDivider)
                            SystemInfoRow("Architecture", "Room SQLite + Hilt + Compose M3")
                            Divider(color = ERPDivider)
                            SystemInfoRow("Database Version", "v1.0 (Versioned Migrations Ready)")
                            Divider(color = ERPDivider)
                            SystemInfoRow("Foreign Keys", "ENFORCED (PRAGMA foreign_keys = ON)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = DeepNavy)
    }
}
