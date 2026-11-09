package com.pocketdev.advance_erp.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.designsystem.BottomNavDestination
import com.pocketdev.advance_erp.core.designsystem.ERPBottomNavigation
import com.pocketdev.advance_erp.core.designsystem.ERPDrawer
import com.pocketdev.advance_erp.core.designsystem.ERPTopBar
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold(
    preferencesRepository: UserPreferencesRepository,
    settingsRepository: SettingsRepository? = null,
    backupRepository: BackupRepository? = null
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destinations.DASHBOARD

    val currentLanguage by preferencesRepository.selectedLanguage.collectAsState(initial = AppLanguage.BANGLA)
    val currentPhotoUri by preferencesRepository.profilePhotoUri.collectAsState(initial = null)
    val customUserName by preferencesRepository.userName.collectAsState(initial = null)
    val businessProfile by (settingsRepository?.getBusinessProfile() ?: flowOf(null)).collectAsState(initial = null)

    val currentUserName = customUserName ?: businessProfile?.companyName?.ifBlank { null } ?: "Santanu Ganguly"

    var showSyncLogoutDialog by remember { mutableStateOf(false) }

    val topBarTitle = when (currentRoute) {
        Destinations.DASHBOARD -> stringResource(R.string.nav_dashboard)
        Destinations.PRODUCTS -> stringResource(R.string.nav_products)
        Destinations.INVENTORY -> stringResource(R.string.nav_inventory)
        Destinations.SALES -> stringResource(R.string.nav_sales)
        Destinations.ACCOUNTS -> stringResource(R.string.nav_accounts)
        Destinations.MORE -> stringResource(R.string.nav_more)
        Destinations.PURCHASES -> stringResource(R.string.nav_purchases)
        Destinations.IMPORT_MANAGEMENT -> stringResource(R.string.nav_import_management)
        Destinations.SHIPMENTS -> stringResource(R.string.nav_shipments)
        Destinations.CUSTOMERS -> stringResource(R.string.nav_customers)
        Destinations.SUPPLIERS -> stringResource(R.string.nav_suppliers)
        Destinations.LEDGER -> stringResource(R.string.nav_ledger)
        Destinations.REPORTS -> stringResource(R.string.nav_reports)
        Destinations.SETTINGS -> stringResource(R.string.nav_settings)
        else -> stringResource(R.string.app_name)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ERPDrawer(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    coroutineScope.launch {
                        drawerState.close()
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                userName = currentUserName,
                profilePhotoUri = currentPhotoUri,
                onPhotoSelected = { newUri ->
                    coroutineScope.launch {
                        preferencesRepository.setProfilePhotoUri(newUri)
                    }
                },
                onUserNameChanged = { newName ->
                    coroutineScope.launch {
                        preferencesRepository.setUserName(newName)
                        settingsRepository?.let { repo ->
                            val current = repo.getBusinessProfile().firstOrNull() ?: BusinessProfile(companyName = newName, phone = "", address = "")
                            repo.updateBusinessProfile(current.copy(companyName = newName))
                        }
                    }
                },
                onSyncLogout = {
                    showSyncLogoutDialog = true
                },
                onEditProfile = {
                    coroutineScope.launch {
                        drawerState.close()
                        navController.navigate(Destinations.SETTINGS)
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ERPTopBar(
                    title = topBarTitle,
                    subtitle = businessProfile?.companyName?.ifBlank { null } ?: "Advance Import Ltd.",
                    currentLanguage = currentLanguage,
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onToggleLanguage = {
                        coroutineScope.launch {
                            val newLang = if (currentLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                            preferencesRepository.setLanguage(newLang)
                        }
                    }
                )
            },
            bottomBar = {
                ERPBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AdvanceErpNavGraph(navController = navController)
            }
        }
    }

    if (showSyncLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showSyncLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.drawer_sync_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.drawer_sync_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                backupRepository?.createBackup(isAutomatic = false, notes = "Drawer manual sync")
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.drawer_sync_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            showSyncLogoutDialog = false
                            drawerState.close()
                            navController.navigate(Destinations.SETTINGS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(stringResource(R.string.drawer_sync_now))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showSyncLogoutDialog = false
                            coroutineScope.launch {
                                drawerState.close()
                                navController.navigate(Destinations.SETTINGS)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.drawer_logout_now))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showSyncLogoutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}
