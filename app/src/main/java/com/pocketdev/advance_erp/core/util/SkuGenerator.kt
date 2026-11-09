package com.pocketdev.advance_erp.core.util

import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Enterprise Automatic SKU Generator.
 * Creates clean, collision-resistant, human-readable SKUs for products and variants.
 */
object SkuGenerator {

    private val sequenceCounter = AtomicLong(1L)

    /**
     * Generates a unique product Base SKU based on category and English name.
     * Format: [CAT3]-[NAME3]-[001..999]
     * If the candidate already exists in [existingSkus], increments sequence until unique.
     */
    fun generateProductSku(
        categoryNameOrCode: String,
        productNameEn: String,
        existingSkus: Set<String> = emptySet()
    ): String {
        val catToken = extractToken(categoryNameOrCode, 3, "GEN")
        val nameToken = extractToken(productNameEn, 3, "PRD")

        var attempt = 1
        val basePrefix = "$catToken-$nameToken"

        while (true) {
            val seq = (sequenceCounter.getAndIncrement() % 1000).toString().padStart(3, '0')
            val candidate = "$basePrefix-$seq"
            if (!existingSkus.contains(candidate.uppercase())) {
                return candidate.uppercase()
            }
            attempt++
            if (attempt > 1000) {
                // Suffix fallback with random characters if namespace is crowded
                val randomSuffix = Random.nextInt(1000, 9999)
                return "$basePrefix-$randomSuffix".uppercase()
            }
        }
    }

    /**
     * Generates a unique SKU for a variant based on the parent product's base SKU and variant attributes.
     * Format: [BASE_SKU]-[COLOR3]-[SIZE3]-[SEQ]
     */
    fun generateVariantSku(
        baseSku: String,
        color: String?,
        size: String?,
        existingSkus: Set<String> = emptySet()
    ): String {
        val cleanBase = baseSku.trim().uppercase().ifBlank { "PRD-001" }
        val colorToken = if (!color.isNullOrBlank()) extractToken(color, 3, "VAR") else null
        val sizeToken = if (!size.isNullOrBlank()) sanitizeAttribute(size) else null

        val parts = mutableListOf(cleanBase)
        if (colorToken != null) parts.add(colorToken)
        if (sizeToken != null) parts.add(sizeToken)

        val prefix = parts.joinToString("-")
        var seq = 1

        while (true) {
            val candidate = if (colorToken != null || sizeToken != null) {
                // If attributes exist, candidate can be prefix or prefix-01
                if (seq == 1 && !existingSkus.contains(prefix)) {
                    prefix
                } else {
                    "$prefix-${seq.toString().padStart(2, '0')}"
                }
            } else {
                "$prefix-VAR-${seq.toString().padStart(2, '0')}"
            }

            if (!existingSkus.contains(candidate.uppercase())) {
                return candidate.uppercase()
            }
            seq++
        }
    }

    /**
     * Validates SKU string: Must be alphanumeric with hyphens, 3 to 40 characters.
     */
    fun isValidSku(sku: String): Boolean {
        if (sku.isBlank() || sku.length < 3 || sku.length > 40) return false
        return sku.matches(Regex("^[A-Za-z0-9_.-]+$"))
    }

    private fun extractToken(input: String, length: Int, defaultToken: String): String {
        val cleaned = input.trim()
            .replace(Regex("[^A-Za-z0-9]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }

        if (cleaned.isEmpty()) return defaultToken

        // If multiple words (e.g. "Men Casual Shoe" -> "MCS")
        if (cleaned.size >= length) {
            val initials = cleaned.map { it.first().uppercaseChar() }.joinToString("").take(length)
            if (initials.length >= length) return initials
        }

        // Single word or short words: take first letters of first word
        val lettersOnly = input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        return lettersOnly.take(length).padEnd(length, 'X').ifBlank { defaultToken }
    }

    private fun sanitizeAttribute(value: String): String {
        return value.trim()
            .replace(Regex("[^A-Za-z0-9]"), "")
            .uppercase()
            .take(6)
            .ifBlank { "00" }
    }
}
