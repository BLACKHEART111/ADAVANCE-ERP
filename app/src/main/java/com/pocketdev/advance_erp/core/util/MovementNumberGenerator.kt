package com.pocketdev.advance_erp.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Concurrency-safe, collision-resistant unique movement and reference number generator.
 * Uses atomic sequence counter, millisecond timestamp, and random UUID component.
 * Guaranteed unique across concurrent threads and within the same millisecond.
 */
object MovementNumberGenerator {
    private val sequence = AtomicLong(1000L)
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    /**
     * Generates a unique collision-resistant identifier with a human-readable prefix.
     * e.g., SM-20261103-1730656789123-1001-A1B2C3
     */
    fun generate(prefix: String = "MOV"): String {
        val now = System.currentTimeMillis()
        val datePart = synchronized(dateFormat) { dateFormat.format(Date(now)) }
        val seq = sequence.incrementAndGet()
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        return "$prefix-$datePart-$now-$seq-$randomSuffix"
    }

    /**
     * Generates a UUID-backed random identifier string.
     */
    fun generateId(prefix: String = "mov"): String {
        return "$prefix-${UUID.randomUUID()}"
    }

    /**
     * Generates a unique transfer number.
     * e.g. TR-20261103-1001-A1B2C3
     */
    fun generateTransferNumber(): String {
        val now = System.currentTimeMillis()
        val datePart = synchronized(dateFormat) { dateFormat.format(Date(now)) }
        val seq = sequence.incrementAndGet() % 100000
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(4).uppercase()
        return "TR-$datePart-$seq-$randomSuffix"
    }
}
