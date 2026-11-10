package com.pocketdev.advance_erp.feature.sales

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.ui.platform.LocalContext
import com.pocketdev.advance_erp.core.util.ContactPickerHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft

@Composable
fun CustomerLookupDialog(
    customers: List<Customer>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onCreateNewCustomer: (NewCustomerDraft) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    // New Customer Form State
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newOpeningBalanceStr by remember { mutableStateOf("0.00") }
    var formError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val (pickedName, pickedPhone) = ContactPickerHelper.extractContactInfo(context, uri)
                if (!pickedName.isNullOrBlank()) {
                    newName = pickedName
                    formError = null
                }
                if (!pickedPhone.isNullOrBlank()) {
                    newPhone = pickedPhone
                    formError = null
                }
            }
        }
    }

    val filteredCustomers = remember(searchQuery, customers) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.lowercase().contains(q) ||
                        it.phone.contains(q) ||
                        it.customerCode.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = ERPBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCreatingNew) {
                                if (isBangla) "নতুন গ্রাহক নিবন্ধন" else "Create New Customer"
                            } else {
                                if (isBangla) "গ্রাহক অনুসন্ধান ও নির্বাচন" else "Customer Lookup & Selection"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isCreatingNew) {
                                if (isBangla) "গ্রাহকের তথ্য প্রবেশ করান এবং চালিয়ে যান" else "Enter customer details and continue"
                            } else {
                                if (isBangla) "মোবাইল নম্বর বা নাম দিয়ে খুঁজুন" else "Search by mobile number or name"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DeepNavy.copy(alpha = 0.08f))

                if (!isCreatingNew) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (isBangla) "মোবাইল বা গ্রাহকের নাম লিখুন..." else "Enter mobile number or customer name...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryPurple)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DeepNavy.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Create New Customer" button banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (searchQuery.matches("^[0-9+ ]+$".toRegex())) {
                                    newPhone = searchQuery.trim()
                                } else if (searchQuery.isNotBlank()) {
                                    newName = searchQuery.trim()
                                }
                                isCreatingNew = true
                            },
                        color = SecondaryTeal.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "নতুন গ্রাহক — তৈরি ও চালিয়ে যান" else "New Customer — Create & Continue",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                            }
                            Text(
                                text = if (isBangla) "নতুন যোগ করুন +" else "Add New +",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer list
                    Text(
                        text = if (isBangla) "গ্রাহক তালিকা (${filteredCustomers.size})" else "Customers (${filteredCustomers.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBangla) "কোনো গ্রাহক খুঁজে পাওয়া যায়নি" else "No matching customer found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (searchQuery.matches("^[0-9+ ]+$".toRegex())) {
                                            newPhone = searchQuery.trim()
                                        } else {
                                            newName = searchQuery.trim()
                                        }
                                        isCreatingNew = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "'$searchQuery' দিয়ে নতুন গ্রাহক তৈরি করুন" else "Create '$searchQuery' as New Customer",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { customer ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectCustomer(customer)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = customer.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${customer.phone} • ${customer.customerCode}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Select",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // New Customer Creation Form
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Contact Picker Auto-fill Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "গ্রাহকের তথ্য পূরণ করুন" else "Fill Customer Information",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "ফোন কন্টাক্ট থেকে আনুন" else "Pick from Contacts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "গ্রাহকের নাম *" else "Customer Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = {
                                newPhone = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "মোবাইল নম্বর *" else "Mobile Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = {
                                newAddress = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "ঠিকানা *" else "Address *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text(if (isBangla) "ইমেইল (ঐচ্ছিক)" else "Email (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = newOpeningBalanceStr,
                            onValueChange = { newOpeningBalanceStr = it },
                            label = { Text(if (isBangla) "প্রারম্ভিক বকেয়া (BDT, ঐচ্ছিক)" else "Opening Balance (BDT, Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (formError != null) {
                            Text(
                                text = formError!!,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { isCreatingNew = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isBangla) "ফিরে যান" else "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmedName = newName.trim()
                                    val trimmedPhone = newPhone.trim().replace("[^0-9+]".toRegex(), "")
                                    val trimmedAddress = newAddress.trim()

                                    if (trimmedName.isBlank()) {
                                        formError = if (isBangla) "গ্রাহকের নাম আবশ্যক" else "Customer name is required"
                                        return@Button
                                    }
                                    if (trimmedPhone.isBlank()) {
                                        formError = if (isBangla) "মোবাইল নম্বর আবশ্যক" else "Mobile number is required"
                                        return@Button
                                    }
                                    if (trimmedAddress.isBlank()) {
                                        formError = if (isBangla) "ঠিকানা আবশ্যক" else "Address is required"
                                        return@Button
                                    }

                                    // Duplicate mobile verification
                                    val duplicate = customers.find {
                                        it.phone.replace("[^0-9+]".toRegex(), "") == trimmedPhone
                                    }
                                    if (duplicate != null) {
                                        formError = if (isBangla) {
                                            "এই মোবাইল নম্বরে ইতিমধ্যে একজন গ্রাহক নিবন্ধিত আছে: ${duplicate.name}"
                                        } else {
                                            "Customer already exists with this phone: ${duplicate.name}"
                                        }
                                        return@Button
                                    }

                                    val openBal = runCatching {
                                        Money.parseFromString(newOpeningBalanceStr).minorUnits
                                    }.getOrDefault(0L)

                                    val draft = NewCustomerDraft(
                                        name = trimmedName,
                                        phone = trimmedPhone,
                                        address = trimmedAddress,
                                        email = newEmail.trim().takeIf { it.isNotBlank() },
                                        openingBalanceMinor = openBal
                                    )
                                    onCreateNewCustomer(draft)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isBangla) "সংরক্ষণ ও চালিয়ে যান" else "Save & Continue")
                            }
                        }
                    }
                }
            }
        }
    }
}
