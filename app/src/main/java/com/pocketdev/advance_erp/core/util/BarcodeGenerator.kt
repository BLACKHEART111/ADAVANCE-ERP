package com.pocketdev.advance_erp.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Standard Code 128 (Subset B) Barcode Generator & Validator.
 * Pure Kotlin implementation with zero external dependencies.
 */
object BarcodeGenerator {

    private val sequenceCounter = AtomicLong(System.currentTimeMillis() % 1000000L)

    // Code 128 patterns: 107 symbols (index 0 to 106).
    // Each pattern has 6 elements (widths of alternating bar-space-bar-space-bar-space summing to 11).
    // The stop character (index 106) has 7 elements summing to 13.
    private val CODE_128_PATTERNS = arrayOf(
        intArrayOf(2, 1, 2, 2, 2, 2), // 0: ' ' (space)
        intArrayOf(2, 2, 2, 1, 2, 2), // 1: '!'
        intArrayOf(2, 2, 2, 2, 2, 1), // 2: '"'
        intArrayOf(1, 2, 1, 2, 2, 3), // 3: '#'
        intArrayOf(1, 2, 1, 3, 2, 2), // 4: '$'
        intArrayOf(1, 3, 1, 2, 2, 2), // 5: '%'
        intArrayOf(1, 2, 2, 2, 1, 3), // 6: '&'
        intArrayOf(1, 2, 2, 3, 1, 2), // 7: '\''
        intArrayOf(1, 3, 2, 2, 1, 2), // 8: '('
        intArrayOf(2, 2, 1, 2, 1, 3), // 9: ')'
        intArrayOf(2, 2, 1, 3, 1, 2), // 10: '*'
        intArrayOf(2, 3, 1, 2, 1, 2), // 11: '+'
        intArrayOf(1, 1, 2, 2, 3, 2), // 12: ','
        intArrayOf(1, 2, 2, 1, 3, 2), // 13: '-'
        intArrayOf(1, 2, 2, 2, 3, 1), // 14: '.'
        intArrayOf(1, 1, 3, 2, 2, 2), // 15: '/'
        intArrayOf(1, 2, 3, 1, 2, 2), // 16: '0'
        intArrayOf(1, 2, 3, 2, 2, 1), // 17: '1'
        intArrayOf(2, 2, 3, 2, 1, 1), // 18: '2'
        intArrayOf(2, 2, 1, 1, 3, 2), // 19: '3'
        intArrayOf(2, 2, 1, 2, 3, 1), // 20: '4'
        intArrayOf(2, 1, 3, 2, 1, 2), // 21: '5'
        intArrayOf(2, 2, 3, 1, 1, 2), // 22: '6'
        intArrayOf(3, 1, 2, 1, 3, 1), // 23: '7'
        intArrayOf(3, 1, 1, 2, 2, 2), // 24: '8'
        intArrayOf(3, 2, 1, 1, 2, 2), // 25: '9'
        intArrayOf(3, 2, 1, 2, 2, 1), // 26: ':'
        intArrayOf(3, 1, 2, 2, 1, 2), // 27: ';'
        intArrayOf(3, 2, 2, 1, 1, 2), // 28: '<'
        intArrayOf(3, 2, 2, 2, 1, 1), // 29: '='
        intArrayOf(2, 1, 2, 1, 2, 3), // 30: '>'
        intArrayOf(2, 1, 2, 3, 2, 1), // 31: '?'
        intArrayOf(2, 3, 2, 1, 2, 1), // 32: '@'
        intArrayOf(1, 1, 1, 3, 2, 3), // 33: 'A'
        intArrayOf(1, 3, 1, 1, 2, 3), // 34: 'B'
        intArrayOf(1, 3, 1, 3, 2, 1), // 35: 'C'
        intArrayOf(1, 1, 2, 3, 1, 3), // 36: 'D'
        intArrayOf(1, 3, 2, 1, 1, 3), // 37: 'E'
        intArrayOf(1, 3, 2, 3, 1, 1), // 38: 'F'
        intArrayOf(2, 1, 1, 3, 1, 3), // 39: 'G'
        intArrayOf(2, 3, 1, 1, 1, 3), // 40: 'H'
        intArrayOf(2, 3, 1, 3, 1, 1), // 41: 'I'
        intArrayOf(1, 1, 2, 1, 3, 3), // 42: 'J'
        intArrayOf(1, 1, 2, 3, 3, 1), // 43: 'K'
        intArrayOf(1, 3, 2, 1, 3, 1), // 44: 'L'
        intArrayOf(1, 1, 3, 1, 2, 3), // 45: 'M'
        intArrayOf(1, 1, 3, 3, 2, 1), // 46: 'N'
        intArrayOf(1, 3, 3, 1, 2, 1), // 47: 'O'
        intArrayOf(3, 1, 3, 1, 2, 1), // 48: 'P'
        intArrayOf(2, 1, 1, 3, 3, 1), // 49: 'Q'
        intArrayOf(2, 3, 1, 1, 3, 1), // 50: 'R'
        intArrayOf(2, 1, 3, 1, 1, 3), // 51: 'S'
        intArrayOf(2, 1, 3, 3, 1, 1), // 52: 'T'
        intArrayOf(2, 1, 3, 1, 3, 1), // 53: 'U'
        intArrayOf(3, 1, 1, 1, 2, 3), // 54: 'V'
        intArrayOf(3, 1, 1, 3, 2, 1), // 55: 'W'
        intArrayOf(3, 3, 1, 1, 2, 1), // 56: 'X'
        intArrayOf(3, 1, 2, 1, 1, 3), // 57: 'Y'
        intArrayOf(3, 1, 2, 3, 1, 1), // 58: 'Z'
        intArrayOf(3, 3, 2, 1, 1, 1), // 59: '['
        intArrayOf(3, 1, 4, 1, 1, 1), // 60: '\\'
        intArrayOf(2, 2, 1, 4, 1, 1), // 61: ']'
        intArrayOf(4, 3, 1, 1, 1, 1), // 62: '^'
        intArrayOf(1, 1, 1, 2, 2, 4), // 63: '_'
        intArrayOf(1, 1, 1, 4, 2, 2), // 64: '`'
        intArrayOf(1, 2, 1, 1, 2, 4), // 65: 'a'
        intArrayOf(1, 2, 1, 4, 2, 1), // 66: 'b'
        intArrayOf(1, 4, 1, 1, 2, 2), // 67: 'c'
        intArrayOf(1, 4, 1, 2, 2, 1), // 68: 'd'
        intArrayOf(1, 1, 2, 2, 1, 4), // 69: 'e'
        intArrayOf(1, 1, 2, 4, 1, 2), // 70: 'f'
        intArrayOf(1, 2, 2, 1, 1, 4), // 71: 'g'
        intArrayOf(1, 2, 2, 4, 1, 1), // 72: 'h'
        intArrayOf(1, 4, 2, 1, 1, 2), // 73: 'i'
        intArrayOf(1, 4, 2, 2, 1, 1), // 74: 'j'
        intArrayOf(2, 4, 1, 2, 1, 1), // 75: 'k'
        intArrayOf(2, 2, 1, 1, 1, 4), // 76: 'l'
        intArrayOf(4, 1, 3, 1, 1, 1), // 77: 'm'
        intArrayOf(2, 4, 1, 1, 1, 2), // 78: 'n'
        intArrayOf(1, 3, 4, 1, 1, 1), // 79: 'o'
        intArrayOf(1, 1, 1, 2, 4, 2), // 80: 'p'
        intArrayOf(1, 2, 1, 1, 4, 2), // 81: 'q'
        intArrayOf(1, 2, 1, 2, 4, 1), // 82: 'r'
        intArrayOf(1, 1, 4, 2, 1, 2), // 83: 's'
        intArrayOf(1, 2, 4, 1, 1, 2), // 84: 't'
        intArrayOf(1, 2, 4, 2, 1, 1), // 85: 'u'
        intArrayOf(4, 1, 1, 2, 1, 2), // 86: 'v'
        intArrayOf(4, 2, 1, 1, 1, 2), // 87: 'w'
        intArrayOf(4, 2, 1, 2, 1, 1), // 88: 'x'
        intArrayOf(2, 1, 2, 1, 4, 1), // 89: 'y'
        intArrayOf(2, 1, 4, 1, 2, 1), // 90: 'z'
        intArrayOf(4, 1, 2, 1, 2, 1), // 91: '{'
        intArrayOf(1, 1, 1, 1, 4, 3), // 92: '|'
        intArrayOf(1, 1, 1, 3, 4, 1), // 93: '}'
        intArrayOf(1, 3, 1, 1, 4, 1), // 94: '~'
        intArrayOf(1, 1, 4, 1, 1, 3), // 95: DEL
        intArrayOf(1, 1, 4, 3, 1, 1), // 96: FNC3
        intArrayOf(4, 1, 1, 1, 1, 3), // 97: FNC2
        intArrayOf(4, 1, 1, 3, 1, 1), // 98: SHIFT
        intArrayOf(1, 1, 3, 1, 4, 1), // 99: CODE C
        intArrayOf(1, 1, 4, 1, 3, 1), // 100: CODE B
        intArrayOf(3, 1, 1, 1, 4, 1), // 101: CODE A
        intArrayOf(4, 1, 1, 1, 3, 1), // 102: FNC1
        intArrayOf(2, 1, 1, 4, 1, 2), // 103: START A
        intArrayOf(2, 1, 1, 2, 1, 4), // 104: START B (Subset B)
        intArrayOf(2, 1, 1, 2, 3, 2), // 105: START C
        intArrayOf(2, 3, 3, 1, 1, 1, 2) // 106: STOP pattern (7 elements: bar 2, space 3, bar 3, space 1, bar 1, space 1, bar 2)
    )

    private const val START_B_INDEX = 104
    private const val STOP_INDEX = 106
    private const val QUIET_ZONE_MODULES = 10

    /**
     * Validates if a string can be encoded into Code 128 (Subset B: ASCII 32 to 126).
     */
    fun isValidCode128(text: String): Boolean {
        if (text.isBlank() || text.length > 64) return false
        return text.all { it.code in 32..126 }
    }

    /**
     * Encodes a string into a boolean array of modules (true = black bar, false = white space).
     */
    fun encodeCode128(text: String): BooleanArray {
        require(isValidCode128(text)) { "Invalid characters for Code 128: $text" }

        val codeIndices = mutableListOf<Int>()
        codeIndices.add(START_B_INDEX)

        var checksum = START_B_INDEX
        for (i in text.indices) {
            val charCode = text[i].code
            val index = charCode - 32 // ASCII 32 maps to index 0
            codeIndices.add(index)
            checksum += (i + 1) * index
        }

        val checkCharIndex = checksum % 103
        codeIndices.add(checkCharIndex)
        codeIndices.add(STOP_INDEX)

        // Calculate total module count: 2 * quiet zones + elements
        val bits = mutableListOf<Boolean>()
        // Quiet zone at start
        repeat(QUIET_ZONE_MODULES) { bits.add(false) }

        for (patternIndex in codeIndices) {
            val pattern = CODE_128_PATTERNS[patternIndex]
            var isBar = true
            for (width in pattern) {
                repeat(width) { bits.add(isBar) }
                isBar = !isBar
            }
        }

        // Quiet zone at end
        repeat(QUIET_ZONE_MODULES) { bits.add(false) }

        return bits.toBooleanArray()
    }

    /**
     * Renders a Code 128 barcode into an Android Bitmap with optional human-readable text.
     */
    fun generateBarcodeBitmap(
        content: String,
        width: Int = 500,
        height: Int = 160,
        includeText: Boolean = true
    ): Bitmap {
        val sanitized = content.trim().ifBlank { "0000000000" }
        val modules = try {
            encodeCode128(sanitized)
        } catch (e: Exception) {
            encodeCode128(sanitized.filter { it.code in 32..126 }.ifEmpty { "BARCODE" })
        }

        val safeWidth = width.coerceAtLeast(120)
        val safeHeight = height.coerceAtLeast(36)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }

        val topPadding = (safeHeight * 0.06f).coerceIn(2f, 10f)
        val bottomPadding = (safeHeight * 0.05f).coerceIn(2f, 8f)
        var computedTextSize = if (includeText) (safeHeight * 0.17f).coerceIn(9f, 28f) else 0f

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = computedTextSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        if (includeText) {
            val maxTextWidth = safeWidth * 0.92f
            val measured = textPaint.measureText(sanitized)
            if (measured > maxTextWidth && measured > 0f) {
                computedTextSize = (computedTextSize * (maxTextWidth / measured)).coerceAtLeast(7f)
                textPaint.textSize = computedTextSize
            }
        }

        val textGap = if (includeText) (safeHeight * 0.04f).coerceIn(2f, 6f) else 0f
        val textBlockHeight = if (includeText) (computedTextSize + textGap) else 0f
        val barcodeHeight = (safeHeight - topPadding - bottomPadding - textBlockHeight)
            .coerceAtLeast(safeHeight * 0.62f)
        val moduleWidth = safeWidth.toFloat() / modules.size.toFloat()

        for (i in modules.indices) {
            if (modules[i]) {
                val left = i * moduleWidth
                val right = (i + 1) * moduleWidth
                canvas.drawRect(left, topPadding, right, topPadding + barcodeHeight, barPaint)
            }
        }

        if (includeText) {
            canvas.drawText(sanitized, safeWidth / 2f, safeHeight - bottomPadding, textPaint)
        }

        return bitmap
    }

    /**
     * Generates a collision-resistant standard 12-digit barcode with check digit.
     * Starts with '20' (GS1 internal store merchandise prefix) followed by sequence & random suffix.
     */
    fun generateStandardBarcode(prefix: String = "20"): String {
        val seq = (sequenceCounter.incrementAndGet() % 1000000L).toString().padStart(6, '0')
        val rnd = Random.nextInt(100, 999).toString()
        val base11 = (prefix + seq + rnd).take(11)

        // Calculate Modulo 10 check digit (EAN/UPC standard)
        var sum = 0
        for (i in base11.indices) {
            val digit = base11[i].digitToInt()
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checkDigit = (10 - (sum % 10)) % 10

        return base11 + checkDigit
    }
}
