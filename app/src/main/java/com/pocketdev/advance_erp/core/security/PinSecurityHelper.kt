package com.pocketdev.advance_erp.core.security

import java.security.MessageDigest
import java.util.UUID

object PinSecurityHelper {

    fun generateSalt(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String?, storedSalt: String?): Boolean {
        if (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) return false
        val calculated = hashPin(enteredPin, storedSalt)
        return calculated.equals(storedHash, ignoreCase = true)
    }

    fun isValidPin(pin: String): Boolean {
        return pin.length in 4..8 && pin.all { it.isDigit() }
    }
}
