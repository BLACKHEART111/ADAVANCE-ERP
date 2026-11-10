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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.util.AppMediaStorage
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
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
import com.pocketdev.advance_erp.core.security.BiometricHelper
import com.pocketdev.advance_erp.feature.settings.components.BackupManagementSection
import com.pocketdev.advance_erp.feature.settings.components.ChangePinDialog
import com.pocketdev.advance_erp.feature.settings.components.DisablePinDialog
import com.pocketdev.advance_erp.feature.settings.components.SetPinDialog

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isBn = state.language == AppLanguage.BANGLA

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }

    var companyName by remember(state.profile) { mutableStateOf(state.profile?.companyName ?: "") }
    var taxNumber by remember(state.profile) { mutableStateOf(state.profile?.taxNumber ?: "") }
    var phone by remember(state.profile) { mutableStateOf(state.profile?.phone ?: "") }
    var address by remember(state.profile) { mutableStateOf(state.profile?.address ?: "") }
    val coroutineScope = rememberCoroutineScope()

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val savedPath = AppMediaStorage.saveCompanyLogo(context, it)
                    viewModel.updateCompanyLogo(savedPath)
                } catch (_: Exception) {}
            }
        }
    }

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
                            // Company Logo Selection Section
                            Text(
                                text = if (isBn) "কোম্পানি লোগো (ইনভয়েস ও স্টেটমেন্টে দৃশ্যমান)" else "Company Logo (Visible on invoices & statements)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val logoPath = state.companyLogoPath
                                val hasLogo = !logoPath.isNullOrBlank() && File(logoPath).exists()

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasLogo) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(logoPath!!))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Company Logo",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Business,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = if (isBn) "লোগো নেই" else "No Logo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                logoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = PrimaryPurple
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (hasLogo) {
                                                    if (isBn) "লোগো পরিবর্তন" else "Change Logo"
                                                } else {
                                                    if (isBn) "লোগো নির্বাচন" else "Select Logo"
                                                },
                                                fontSize = 12.sp,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (hasLogo) {
                                            OutlinedButton(
                                                onClick = { viewModel.updateCompanyLogo(null) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Red
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBn) "মুছুন" else "Remove",
                                                    fontSize = 12.sp,
                                                    color = Color.Red
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBn) "পিডিএফ রিপোর্ট এবং ইনভয়েসের শীর্ষে এই লোগোটি প্রিন্ট হবে।" else "This logo will be printed on top of all PDF invoices and statements.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = PrimaryPurple.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(14.dp))

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

            // Security & App Lock (PIN & Biometric Fingerprint)
            item {
                Column {
                    ERPSectionHeader(
                        title = if (isBn) "নিরাপত্তা ও অ্যাপ লক" else "Security & App Lock",
                        subtitle = if (isBn) "পিন কোড (৪–৮ ডিজিট) এবং ফিঙ্গারপ্রিন্ট বায়োমেট্রিক সুরক্ষা"
                        else "4-8 Digit PIN code & Fingerprint biometric protection"
                    )
                    ERPCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // PIN Lock Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (state.isAppLockEnabled) PrimaryPurple else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isBn) "পিন কোড লক (৪–৮ ডিজিট)" else "PIN Code Lock (4-8 Digits)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (state.isAppLockEnabled) {
                                                if (isBn) "সুরক্ষিত • ৪ থেকে ৮ ডিজিটের পিন সক্রিয়" else "Active • 4-8 Digit PIN enabled"
                                            } else {
                                                if (isBn) "নিষ্ক্রিয় • অ্যাপে প্রবেশের জন্য কোনো পিন লাগবে না" else "Disabled • No PIN required"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (state.isAppLockEnabled) StatusSuccess else TextMuted
                                        )
                                    }
                                }

                                Switch(
                                    checked = state.isAppLockEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            showSetPinDialog = true
                                        } else {
                                            showDisablePinDialog = true
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryPurple
                                    )
                                )
                            }

                            if (state.isAppLockEnabled) {
                                HorizontalDivider(color = ERPDivider)

                                // Change PIN Button
                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryPurple)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "পিন কোড পরিবর্তন করুন" else "Change PIN Code",
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple,
                                        fontSize = 12.sp
                                    )
                                }

                                HorizontalDivider(color = ERPDivider)

                                // Fingerprint / Biometric Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = if (state.isBiometricEnabled) SecondaryTeal else TextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isBn) "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক আনলক" else "Fingerprint / Biometric Unlock",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (!state.isBiometricSupported) {
                                                    if (isBn) "ডিভাইসে বায়োমেট্রিক সমর্থিত নয় বা সেটআপ নেই" else "Biometrics not supported or enrolled on device"
                                                } else if (state.isBiometricEnabled) {
                                                    if (isBn) "সক্রিয় • ফিঙ্গারপ্রিন্ট দিয়ে দ্রুত আনলক করুন" else "Enabled • Unlock instantly using fingerprint"
                                                } else {
                                                    if (isBn) "নিষ্ক্রিয়" else "Disabled"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (state.isBiometricEnabled) SecondaryTeal else TextMuted
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = state.isBiometricEnabled,
                                        enabled = state.isBiometricSupported,
                                        onCheckedChange = { enable ->
                                            if (enable && context is FragmentActivity) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = context,
                                                    title = if (isBn) "ফিঙ্গারপ্রিন্ট নিশ্চিতকরণ" else "Confirm Fingerprint",
                                                    subtitle = if (isBn) "ফিঙ্গারপ্রিন্ট সক্রিয় করতে আঙুলের ছাপ দিন" else "Touch fingerprint sensor to enable",
                                                    onAuthenticated = { viewModel.toggleBiometric(true) },
                                                    onError = {}
                                                )
                                            } else {
                                                viewModel.toggleBiometric(false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SecondaryTeal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Data Backup & Restore Section
            item {
                Column {
                    ERPSectionHeader(
                        title = if (isBn) "ডাটা ব্যাকআপ ও রিস্টোর" else "Data Backup & Restore",
                        subtitle = if (isBn) "সম্পূর্ণ অফলাইন ব্যাকআপ (.zip) • ছবি ও ডাটাবেজ সংরক্ষণ • প্রতি শুক্রবার স্বয়ংক্রিয় ব্যাকআপ"
                        else "Full offline ZIP backups • Photos & database bundle • Automatic Friday backup"
                    )
                    BackupManagementSection(
                        backupRecords = state.backupRecords,
                        isFridayAutoBackupEnabled = state.isFridayAutoBackupEnabled,
                        lastAutoBackupTime = state.lastAutoBackupTime,
                        language = state.language,
                        onCreateBackup = { onFinished -> viewModel.createInstantBackup(onFinished) },
                        onRestoreBackupFile = { file -> viewModel.restoreBackup(file) },
                        onRestoreFromUri = { uri -> viewModel.restoreFromUri(uri) },
                        onShareBackup = { file -> viewModel.shareBackup(file) },
                        onDeleteBackup = { recordId -> viewModel.deleteBackup(recordId) },
                        onToggleFridayAutoBackup = { enabled -> viewModel.toggleFridayAutoBackup(enabled) }
                    )
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

    // Set PIN Dialog
    if (showSetPinDialog) {
        SetPinDialog(
            language = state.language,
            onPinSet = { pin -> viewModel.setAppLock(pin) },
            onDismiss = { showSetPinDialog = false }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            storedHash = state.pinCodeHash,
            storedSalt = state.pinSalt,
            language = state.language,
            onPinChanged = { newPin -> viewModel.changePin(newPin) },
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Disable PIN Dialog
    if (showDisablePinDialog) {
        DisablePinDialog(
            storedHash = state.pinCodeHash,
            storedSalt = state.pinSalt,
            language = state.language,
            onPinVerifiedAndDisabled = { viewModel.disableAppLock() },
            onDismiss = { showDisablePinDialog = false }
        )
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
