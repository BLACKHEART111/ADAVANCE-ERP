package com.pocketdev.advance_erp.core.designsystem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DrawerDestination(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    DASHBOARD("dashboard", Icons.Default.Dashboard, R.string.nav_dashboard),
    PRODUCTS("products", Icons.Default.Category, R.string.nav_products),
    INVENTORY("inventory", Icons.Default.Inventory, R.string.nav_inventory),
    PURCHASES("purchases", Icons.Default.ShoppingCart, R.string.nav_purchases),
    IMPORT_MANAGEMENT("import_management", Icons.Default.Public, R.string.nav_import_management),
    SHIPMENTS("shipments", Icons.Default.FlightTakeoff, R.string.nav_shipments),
    CUSTOMERS("customers", Icons.Default.Group, R.string.nav_customers),
    SUPPLIERS("suppliers", Icons.Default.Storefront, R.string.nav_suppliers),
    SALES("sales", Icons.Default.PointOfSale, R.string.nav_sales),
    ACCOUNTS("accounts", Icons.Default.AccountBalance, R.string.nav_accounts),
    LEDGER("ledger", Icons.Default.MenuBook, R.string.nav_ledger),
    REPORTS("reports", Icons.Default.Assessment, R.string.nav_reports),
    SETTINGS("settings", Icons.Default.Settings, R.string.nav_settings)
}

@Composable
fun ERPDrawer(
    currentRoute: String,
    onNavigate: (DrawerDestination) -> Unit,
    userName: String = "Advance ERP Admin",
    profilePhotoUri: String? = null,
    onPhotoSelected: (String?) -> Unit = {},
    onUserNameChanged: (String) -> Unit = {},
    onSyncLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = DrawerTeal
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(DrawerTeal)
        ) {
            // Header with seamless curved wave & real-time clock & profile photo picker
            DrawerWaveHeader(
                userName = userName,
                profilePhotoUri = profilePhotoUri,
                onPhotoSelected = onPhotoSelected,
                onUserNameChanged = onUserNameChanged,
                onSyncLogout = onSyncLogout,
                onEditProfile = onEditProfile
            )

            // Scrollable Menu Items on vibrant teal canvas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                DrawerDestination.entries.forEach { destination ->
                    val isSelected = currentRoute == destination.route

                    DrawerMenuItemRow(
                        destination = destination,
                        isSelected = isSelected,
                        onClick = { onNavigate(destination) }
                    )
                }
            }

            // Official Brand Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdvanceErpLogo(
                    variant = BrandConfig.LogoVariant.COMPACT,
                    isDarkBackground = true,
                    modifier = Modifier.height(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v${BrandConfig.BRAND_VERSION} • ${BrandConfig.TAGLINE_EN}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DrawerWaveHeader(
    userName: String,
    profilePhotoUri: String?,
    onPhotoSelected: (String?) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onSyncLogout: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    // Real-time clock updating every second from system time
    var liveTime by remember { mutableStateOf(getLiveClockTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveTime = getLiveClockTime()
            val now = System.currentTimeMillis()
            val delayMillis = 1000L - (now % 1000L)
            delay(if (delayMillis > 50L) delayMillis else 1000L)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val destFile = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    onPhotoSelected(destFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Top section has DrawerNavyHeader with an organic curved wave boundary
                val wavePath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.72f)
                    // Cubic bezier wave swooping smoothly from right to left
                    // Passing seamlessly across the circular profile avatar
                    cubicTo(
                        x1 = size.width * 0.65f, y1 = size.height * 0.76f,
                        x2 = size.width * 0.28f, y2 = size.height * 0.58f,
                        x3 = 0f, y3 = size.height * 0.48f
                    )
                    close()
                }
                drawPath(wavePath, color = DrawerNavyHeader)
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: "CURRENT TIME" real-time clock and "SYNC & LOG OUT ⏻"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Real-time clock block with brand symbol
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdvanceErpLogo(
                        variant = BrandConfig.LogoVariant.SYMBOL,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.drawer_clock_time),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = liveTime.first,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = liveTime.second,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // Right: Sync & Log Out button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSyncLogout)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SYNC &",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "LOG OUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = "Sync & Log Out",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Profile photo area with white border and edit badge
            Box(
                modifier = Modifier.size(72.dp)
            ) {
                // Circular avatar with 3.dp pure white border
                Surface(
                    modifier = Modifier
                        .size(68.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable { showPhotoOptionsDialog = true },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    val customFile = profilePhotoUri?.let { File(it) }
                    if (customFile != null && customFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(customFile)
                                .crossfade(true)
                                .error(R.drawable.avatar_santanu)
                                .fallback(R.drawable.avatar_santanu)
                                .build(),
                            contentDescription = stringResource(R.string.drawer_profile_options),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_santanu),
                            contentDescription = stringResource(R.string.drawer_profile_options),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Edit badge at top-right of avatar circle (tapping opens photo picker directly)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(DrawerEditBadge, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            photoPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.drawer_select_photo),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Name below avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showPhotoOptionsDialog = true }
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    if (showPhotoOptionsDialog) {
        var nameInput by remember { mutableStateOf(userName) }

        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.drawer_profile_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .border(2.dp, SecondaryTeal, CircleShape),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        val customFile = profilePhotoUri?.let { File(it) }
                        if (customFile != null && customFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customFile)
                                    .crossfade(true)
                                    .error(R.drawable.avatar_santanu)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_santanu),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.drawer_select_photo))
                    }

                    if (!profilePhotoUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onPhotoSelected(null)
                                showPhotoOptionsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.drawer_remove_photo))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.drawer_edit_user_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onUserNameChanged(nameInput.trim())
                        }
                        showPhotoOptionsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun getLiveClockTime(): Pair<String, String> {
    val cal = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("hh:mm", Locale.ENGLISH)
    val amPmFormat = SimpleDateFormat("a", Locale.ENGLISH)
    return Pair(
        timeFormat.format(cal.time),
        amPmFormat.format(cal.time).uppercase(Locale.ENGLISH)
    )
}

@Composable
private fun DrawerMenuItemRow(
    destination: DrawerDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    if (isSelected) DrawerTealDark.copy(alpha = 0.5f) else Color.Transparent
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = stringResource(destination.labelResId),
                tint = if (isSelected) Color.White else DrawerIconTint,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(destination.labelResId),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.95f)
            )
        }

        // Elegant thin divider line aligned with text
        HorizontalDivider(
            color = DrawerDivider,
            thickness = 0.8.dp,
            modifier = Modifier.padding(start = 56.dp, end = 20.dp)
        )
    }
}
