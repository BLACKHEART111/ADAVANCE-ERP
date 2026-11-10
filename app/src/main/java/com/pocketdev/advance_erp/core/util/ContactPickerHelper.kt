package com.pocketdev.advance_erp.core.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactPickerHelper {

    /**
     * Extracts Contact Name and Phone Number from contact URI returned by
     * Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
     * or ContactsContract.Contacts.CONTENT_URI.
     */
    fun extractContactInfo(context: Context, contactUri: Uri): Pair<String?, String?> {
        var name: String? = null
        var phone: String? = null

        try {
            // First attempt: querying Phone.CONTENT_URI projection
            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(contactUri, phoneProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (phoneIdx >= 0) phone = cursor.getString(phoneIdx)
                }
            }

            // Fallback attempt: if phone wasn't retrieved (e.g. URI was Contacts.CONTENT_URI)
            if (phone.isNullOrBlank()) {
                val contactIdProjection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                )
                var contactId: String? = null
                context.contentResolver.query(contactUri, contactIdProjection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idIdx >= 0) contactId = cursor.getString(idIdx)
                        if (nameIdx >= 0 && name.isNullOrBlank()) name = cursor.getString(nameIdx)
                    }
                }

                if (!contactId.isNullOrBlank()) {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx >= 0) phone = phoneCursor.getString(numIdx)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Content provider query fallback safe handling
        }

        return Pair(name?.trim(), normalizePhoneNumber(phone))
    }

    /**
     * Normalizes phone numbers: removes spaces, dashes, parentheses.
     */
    fun normalizePhoneNumber(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        // Remove spaces, hyphens, parentheses
        val cleaned = rawPhone.replace("[\\s\\-\\(\\)]".toRegex(), "")
        return cleaned.ifBlank { null }
    }
}
