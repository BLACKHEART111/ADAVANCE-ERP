package com.pocketdev.advance_erp.feature.contacts

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.util.AppMediaStorage
import com.pocketdev.advance_erp.core.util.ContactPickerHelper
import com.pocketdev.advance_erp.feature.sales.DueReminderDialog
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    initialTab: ContactsTab = ContactsTab.CUSTOMERS,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var customerForPhotoUpdate by remember { mutableStateOf<Customer?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val customerPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pickedUri ->
            customerForPhotoUpdate?.let { targetCust ->
                coroutineScope.launch {
                    try {
                        val savedPath = AppMediaStorage.saveCustomerPhoto(context, pickedUri, targetCust.id)
                        viewModel.updateCustomerPhoto(targetCust, savedPath)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(initialTab) {
        viewModel.onTabSelect(initialTab)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setAddDialogVisible(true) },
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                val isCustomers = state.selectedTab == ContactsTab.CUSTOMERS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCustomers) PrimaryPurple else Color.Transparent)
                        .clickable { viewModel.onTabSelect(ContactsTab.CUSTOMERS) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.customers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCustomers) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCustomers) Color.White else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isCustomers) PrimaryPurple else Color.Transparent)
                        .clickable { viewModel.onTabSelect(ContactsTab.SUPPLIERS) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.suppliers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!isCustomers) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isCustomers) Color.White else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact List
            if (state.selectedTab == ContactsTab.CUSTOMERS) {
                if (state.customers.isEmpty()) {
                    ERPEmptyState(
                        icon = Icons.Default.Group,
                        title = stringResource(R.string.no_contacts),
                        message = "No customers registered yet.",
                        actionText = stringResource(R.string.add_customer),
                        onActionClick = { viewModel.setAddDialogVisible(true) }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.customers, key = { it.id }) { c ->
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Customer Profile Photo Avatar
                                            val photoPath = c.profileImagePath
                                            val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()

                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryPurple.copy(alpha = 0.12f))
                                                    .clickable {
                                                        customerForPhotoUpdate = c
                                                        customerPhotoLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (hasPhoto) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(File(photoPath!!))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = c.name,
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Text(
                                                        text = c.name.take(1).uppercase(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryPurple
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = c.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Code: ${c.customerCode}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.formatMoney(c.outstandingBalanceMinor, state.currency, state.language),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (c.outstandingBalanceMinor > 0) StatusError else StatusSuccess
                                            )
                                            Text(
                                                text = stringResource(R.string.outstanding_balance),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = c.phone, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = c.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (c.outstandingBalanceMinor > 0) {
                                                Button(
                                                    onClick = { viewModel.openDueReminder(c) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Send,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (state.language == AppLanguage.BANGLA) "বকেয়া তাগাদা" else "Due Reminder",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { customerToDelete = c },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Customer",
                                                    tint = StatusError,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (state.suppliers.isEmpty()) {
                    ERPEmptyState(
                        icon = Icons.Default.Storefront,
                        title = stringResource(R.string.no_contacts),
                        message = "No overseas suppliers recorded.",
                        actionText = stringResource(R.string.add_supplier),
                        onActionClick = { viewModel.setAddDialogVisible(true) }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.suppliers, key = { it.id }) { s ->
                            ERPCard(contentPadding = 14.dp) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = s.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = "${s.supplierCode} • ${s.country} (${s.currency})", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.formatMoney(s.outstandingPayableMinor, state.currency, state.language),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepNavy
                                            )
                                            Text(text = "Payable Due", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = ERPDivider)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Contact: ${s.contactPerson ?: "-"} • Phone: ${s.phone} • ${s.address}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddContactDialog(
            isCustomer = state.selectedTab == ContactsTab.CUSTOMERS,
            isBangla = state.language == AppLanguage.BANGLA,
            onDismiss = { viewModel.setAddDialogVisible(false) },
            onAddCustomer = { name, phone, email, address, limit, photoPath ->
                viewModel.addCustomer(name, phone, email, address, limit, photoPath)
            },
            onAddSupplier = { name, contact, phone, email, address, country, curr ->
                viewModel.addSupplier(name, contact, phone, email, address, country, curr)
            }
        )
    }

    if (state.activeDueReminderData != null) {
        DueReminderDialog(
            data = state.activeDueReminderData!!,
            initialIsBangla = state.language == AppLanguage.BANGLA,
            onDismiss = { viewModel.dismissDueReminderDialog() }
        )
    }

    if (customerToDelete != null) {
        val cust = customerToDelete!!
        val isBlocked = cust.outstandingBalanceMinor != 0L
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Text(
                    text = if (state.language == AppLanguage.BANGLA) "গ্রাহক মুছুন / আর্কাইভ" else "Delete / Archive Customer",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                if (isBlocked) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA)
                                "⚠️ বকেয়া রয়েছে: '${cust.name}' এর নিকট ${Formatters.formatMoney(cust.outstandingBalanceMinor, state.currency, state.language)} বকেয়া রয়েছে।"
                            else
                                "⚠️ Outstanding Balance: '${cust.name}' has an outstanding balance of ${Formatters.formatMoney(cust.outstandingBalanceMinor, state.currency, state.language)}.",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.language == AppLanguage.BANGLA)
                                "বকেয়া সম্পূর্ণরূপে পরিশোধ বা সমন্বয় না হওয়া পর্যন্ত গ্রাহক মুছে ফেলা বা আর্কাইভ করা যাবে না।"
                            else
                                "Customers with non-zero outstanding balances cannot be deleted or archived. Settle all dues first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = if (state.language == AppLanguage.BANGLA)
                            "আপনি কি '${cust.name}' কে মুছে ফেলতে চান? যদি গ্রাহকের কোনো বিগত সেলস চালান বা পেমেন্ট রেকর্ড থাকে, তবে অডিট হিস্ট্রি সুরক্ষায় গ্রাহকটি নিরাপদে আর্কাইভ করা হবে।"
                        else
                            "Are you sure you want to delete '${cust.name}'? If the customer has past sales invoices or payments, they will be safely archived to preserve financial audit history.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                if (!isBlocked) {
                    TextButton(
                        onClick = {
                            val cId = cust.id
                            customerToDelete = null
                            viewModel.deleteOrArchiveCustomer(cId)
                        }
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA) "হ্যাঁ, মুছুন" else "Delete / Archive",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text(if (state.language == AppLanguage.BANGLA) "বুঝেছি" else "Understood", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isBlocked) {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text(if (state.language == AppLanguage.BANGLA) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun AddContactDialog(
    isCustomer: Boolean,
    isBangla: Boolean = false,
    onDismiss: () -> Unit,
    onAddCustomer: (name: String, phone: String, email: String?, address: String, creditLimitMinor: Long, profileImagePath: String?) -> Unit,
    onAddSupplier: (companyName: String, contactPerson: String?, phone: String, email: String?, address: String, country: String, currency: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimitStr by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("China") }
    var currency by remember { mutableStateOf("CNY") }
    var profileImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val (pickedName, pickedPhone) = ContactPickerHelper.extractContactInfo(context, uri)
                if (!pickedName.isNullOrBlank()) name = pickedName
                if (!pickedPhone.isNullOrBlank()) phone = pickedPhone
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        profileImageUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCustomer) {
                            if (isBangla) "নতুন গ্রাহক নিবন্ধন" else stringResource(R.string.add_customer)
                        } else {
                            if (isBangla) "নতুন সরবরাহকারী যোগ" else stringResource(R.string.add_supplier)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = ERPDivider, modifier = Modifier.padding(vertical = 12.dp))

                if (isCustomer) {
                    // Profile Photo & Phone Contact Import Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple.copy(alpha = 0.12f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(profileImageUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Customer Photo",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Add Photo",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (profileImageUri != null) {
                                        if (isBangla) "ছবি নির্বাচিত" else "Photo Selected"
                                    } else {
                                        if (isBangla) "প্রোফাইল ছবি যোগ করুন" else "Add Profile Photo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isBangla) "ট্যাপ করে ছবি বাছুন" else "Tap avatar to select",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Phone Contacts Picker Button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "কন্টাক্ট থেকে আনুন" else "Pick from Contacts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isCustomer) (if (isBangla) "গ্রাহকের নাম *" else stringResource(R.string.contact_name)) else stringResource(R.string.company_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("Contact Person") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isBangla) "মোবাইল নম্বর *" else stringResource(R.string.contact_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.contact_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditLimitStr,
                        onValueChange = { creditLimitStr = it },
                        label = { Text(stringResource(R.string.credit_limit)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ERPActionButton(text = stringResource(R.string.cancel), onClick = onDismiss, variant = ERPButtonVariant.GHOST)
                    Spacer(modifier = Modifier.width(8.dp))
                    ERPActionButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            if (name.isBlank() || phone.isBlank()) return@ERPActionButton
                            if (isCustomer) {
                                val limit = Money.parseFromString(creditLimitStr).minorUnits
                                coroutineScope.launch {
                                    var savedPhotoPath: String? = null
                                    if (profileImageUri != null) {
                                        try {
                                            savedPhotoPath = AppMediaStorage.saveCustomerPhoto(context, profileImageUri!!)
                                        } catch (_: Exception) {}
                                    }
                                    onAddCustomer(name, phone, email, address, limit, savedPhotoPath)
                                }
                            } else {
                                onAddSupplier(name, contactPerson, phone, email, address, country, currency)
                            }
                        },
                        variant = ERPButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
