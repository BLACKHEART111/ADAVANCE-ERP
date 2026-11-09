package com.pocketdev.advance_erp.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Self-contained Pure Kotlin QR Code Model 2 Generator.
 * Implements ISO/IEC 18004 Byte Mode with Reed-Solomon Error Correction.
 * Completely offline with zero third-party dependencies.
 */
object QrCodeGenerator {

    /**
     * QR Code Structured Payload Prefix
     */
    const val QR_PREFIX_PRODUCT = "ADV-ERP|PRODUCT"
    const val QR_PREFIX_VARIANT = "ADV-ERP|VARIANT"

    data class QrPayload(
        val type: QrType,
        val targetId: String,
        val secondaryId: String? = null,
        val code: String? = null
    )

    enum class QrType {
        PRODUCT,
        VARIANT,
        PLAIN_TEXT
    }

    /**
     * Creates a structured QR code payload for a product.
     * Contains only internal reference IDs, zero customer/cost data.
     */
    fun createProductQrPayload(productId: String, sku: String): String {
        return "$QR_PREFIX_PRODUCT|$productId|$sku"
    }

    /**
     * Creates a structured QR code payload for a product variant.
     */
    fun createVariantQrPayload(variantId: String, productId: String, sku: String): String {
        return "$QR_PREFIX_VARIANT|$variantId|$productId|$sku"
    }

    /**
     * Parses a scanned QR payload or plain string into structured target.
     */
    fun parsePayload(scanned: String): QrPayload {
        val trimmed = scanned.trim()

        // 1. Colon-delimited simple format: ADV-ERP:PRD:<id>
        if (trimmed.startsWith("ADV-ERP:PRD:")) {
            val id = trimmed.removePrefix("ADV-ERP:PRD:").trim()
            return QrPayload(QrType.PRODUCT, targetId = id)
        }
        if (trimmed.startsWith("ADV-ERP:VAR:")) {
            val id = trimmed.removePrefix("ADV-ERP:VAR:").trim()
            return QrPayload(QrType.VARIANT, targetId = id)
        }

        // 2. Short prefix pipe format: ADV-ERP:PRD|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP:PRD|")) {
            val parts = trimmed.split("|")
            val id = parts.getOrNull(1) ?: ""
            val sku = parts.getOrNull(2)
            return QrPayload(QrType.PRODUCT, targetId = id, code = sku)
        }

        // 3. Short prefix variant pipe format: ADV-ERP:VAR|<varId>|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP:VAR|")) {
            val parts = trimmed.split("|")
            val varId = parts.getOrNull(1) ?: ""
            val prodId = parts.getOrNull(2)
            val sku = parts.getOrNull(3)
            return QrPayload(QrType.VARIANT, targetId = varId, secondaryId = prodId, code = sku)
        }

        // 4. Full pipe format: ADV-ERP|PRODUCT|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP|PRODUCT|")) {
            val parts = trimmed.split("|")
            val id = parts.getOrNull(2) ?: ""
            val sku = parts.getOrNull(3)
            return QrPayload(QrType.PRODUCT, targetId = id, code = sku)
        }

        // 5. Full pipe format variant: ADV-ERP|VARIANT|<varId>|<prodId>|<sku>
        if (trimmed.startsWith("ADV-ERP|VARIANT|")) {
            val parts = trimmed.split("|")
            val varId = parts.getOrNull(2) ?: ""
            val prodId = parts.getOrNull(3)
            val sku = parts.getOrNull(4)
            return QrPayload(QrType.VARIANT, targetId = varId, secondaryId = prodId, code = sku)
        }

        return QrPayload(QrType.PLAIN_TEXT, targetId = trimmed, code = trimmed)
    }

    // --- QR Code Matrix Generation Engine ---

    // GF(256) log and antilog tables (primitive polynomial 0x11D: x^8 + x^4 + x^3 + x^2 + 1)
    private val EXP_TABLE = IntArray(512)
    private val LOG_TABLE = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP_TABLE[i] = x
            EXP_TABLE[i + 255] = x
            LOG_TABLE[x] = i
            x = x shl 1
            if (x and 0x100 != 0) {
                x = x xor 0x11D
            }
        }
        LOG_TABLE[0] = 0
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]]
    }

    // Reed-Solomon Generator Polynomial for Error Correction
    private fun getRsGeneratorPolynomial(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, EXP_TABLE[i])
            val product = IntArray(poly.size + 1)
            for (j in poly.indices) {
                for (k in factor.indices) {
                    product[j + k] = product[j + k] xor gfMultiply(poly[j], factor[k])
                }
            }
            poly = product
        }
        return poly
    }

    private fun computeRsErrorCorrection(data: IntArray, ecCount: Int): IntArray {
        val gen = getRsGeneratorPolynomial(ecCount)
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = b xor remainder[0]
            for (i in 0 until ecCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMultiply(gen[i + 1], factor)
            }
            remainder[ecCount - 1] = gfMultiply(gen[ecCount], factor)
        }
        return remainder
    }

    // Standard QR Version 3 (29x29) and Version 4 (33x33) specs (Error Correction Level M)
    // Version 3: 70 total codewords (44 data codewords, 26 EC codewords) - holds up to 42 ASCII chars
    // Version 4: 100 total codewords (64 data codewords, 36 EC codewords) - holds up to 62 ASCII chars
    // Version 5: 134 total codewords (86 data codewords, 48 EC codewords) - holds up to 84 ASCII chars
    private data class QrSpec(val version: Int, val size: Int, val dataCapacityBytes: Int, val ecCodewords: Int, val alignPos: Int)

    private fun selectSpec(contentLength: Int): QrSpec {
        return when {
            contentLength <= 24 -> QrSpec(version = 2, size = 25, dataCapacityBytes = 28, ecCodewords = 16, alignPos = 18)
            contentLength <= 42 -> QrSpec(version = 3, size = 29, dataCapacityBytes = 44, ecCodewords = 26, alignPos = 22)
            contentLength <= 62 -> QrSpec(version = 4, size = 33, dataCapacityBytes = 64, ecCodewords = 36, alignPos = 26)
            else -> QrSpec(version = 5, size = 37, dataCapacityBytes = 86, ecCodewords = 48, alignPos = 30)
        }
    }

    /**
     * Generates a 2D boolean matrix for the QR Code (true = dark module, false = light module).
     */
    fun generateQrMatrix(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val spec = selectSpec(bytes.size)
        val size = spec.size

        // 1. Encode Data in Byte Mode (0100)
        val bitBuffer = mutableListOf<Boolean>()
        // Mode: Byte (0100)
        bitBuffer.addAll(listOf(false, true, false, false))
        // Character count indicator (8 bits for versions 1..9)
        val len = bytes.size
        for (i in 7 downTo 0) {
            bitBuffer.add((len shr i and 1) == 1)
        }
        // Data bits
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitBuffer.add((byteVal shr i and 1) == 1)
            }
        }
        // Terminator (up to 4 zeroes)
        val maxDataBits = spec.dataCapacityBytes * 8
        val termLen = minOf(4, maxDataBits - bitBuffer.size)
        repeat(termLen) { bitBuffer.add(false) }
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(false)
        }
        // Pad bytes (0xEC, 0x11)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < maxDataBits) {
            val pad = padBytes[padIdx % 2]
            for (i in 7 downTo 0) {
                bitBuffer.add((pad shr i and 1) == 1)
            }
            padIdx++
        }

        // Convert data bits to data codewords
        val dataCodewords = IntArray(spec.dataCapacityBytes)
        for (i in 0 until spec.dataCapacityBytes) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bitBuffer[i * 8 + j]) {
                    byteVal = byteVal or (1 shl (7 - j))
                }
            }
            dataCodewords[i] = byteVal
        }

        // Compute Error Correction Codewords
        val ecCodewords = computeRsErrorCorrection(dataCodewords, spec.ecCodewords)
        val allCodewords = dataCodewords + ecCodewords

        // Convert all codewords to bit stream
        val finalBits = mutableListOf<Boolean>()
        for (cw in allCodewords) {
            for (j in 7 downTo 0) {
                finalBits.add((cw shr j and 1) == 1)
            }
        }

        // Initialize Matrix and Reserved Functions Mask
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // Function: Draw Finder Pattern (7x7)
        fun drawFinder(r0: Int, c0: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[r0 + r][c0 + c] = isBorder || isInner
                    reserved[r0 + r][c0 + c] = true
                }
            }
            // Separator quiet zone around finder
            for (r in -1..7) {
                for (c in -1..7) {
                    val row = r0 + r
                    val col = c0 + c
                    if (row in 0 until size && col in 0 until size) {
                        reserved[row][col] = true
                    }
                }
            }
        }

        // Draw 3 Finders: Top-Left, Top-Right, Bottom-Left
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Draw Alignment Pattern (5x5) if Version >= 2
        if (spec.alignPos > 0) {
            val ar = spec.alignPos
            val ac = spec.alignPos
            for (r in -2..2) {
                for (c in -2..2) {
                    val isOuter = r == -2 || r == 2 || c == -2 || c == 2
                    val isCenter = r == 0 && c == 0
                    matrix[ar + r][ac + c] = isOuter || isCenter
                    reserved[ar + r][ac + c] = true
                }
            }
        }

        // Draw Timing Patterns (alternating black/white at row 6 and col 6)
        for (i in 0 until size) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // Dark Module at (4 * V + 9, 8)
        val darkR = 4 * spec.version + 9
        matrix[darkR][8] = true
        reserved[darkR][8] = true

        // Reserve Format Info Area (around finders)
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // Place Data Bits Zig-Zag from right to left
        var bitIdx = 0
        var goingUp = true
        var col = size - 1

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing pattern column

            val rows = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in 0..1) {
                    val currCol = col - c
                    if (!reserved[row][currCol]) {
                        val bit = if (bitIdx < finalBits.size) finalBits[bitIdx++] else false
                        // Apply Mask Pattern 0: (row + col) % 2 == 0
                        val mask = (row + currCol) % 2 == 0
                        matrix[row][currCol] = bit xor mask
                    }
                }
            }
            goingUp = !goingUp
            col -= 2
        }

        // Format Info for Error Correction Level M (00) and Mask 0 (000):
        // Standard format data: 0b00000 -> with 10-bit BCH and 0x5412 mask = 0b101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Draw format bits around Top-Left
        val tlCoords = listOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), Pair(8, 8), Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8),
            Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0 until 15) {
            val bit = formatBits[i] == 1
            matrix[tlCoords[i].first][tlCoords[i].second] = bit
        }

        // Draw format bits around Top-Right and Bottom-Left
        for (i in 0..7) {
            matrix[size - 1 - i][8] = (formatBits[i] == 1)
        }
        for (i in 0..6) {
            matrix[8][size - 7 + i] = (formatBits[8 + i] == 1)
        }

        return matrix
    }

    /**
     * Renders a QR code matrix into an Android Bitmap with quiet zones.
     */
    fun generateQrBitmap(content: String, targetSize: Int = 300): Bitmap {
        val sanitized = content.trim().ifBlank { "ADV-ERP" }
        val matrix = generateQrMatrix(sanitized)
        val matrixSize = matrix.size
        val quietZone = 4
        val totalModules = matrixSize + quietZone * 2

        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val modulePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }

        val scale = targetSize.toFloat() / totalModules.toFloat()

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    val left = (c + quietZone) * scale
                    val top = (r + quietZone) * scale
                    val right = left + scale
                    val bottom = top + scale
                    canvas.drawRect(left, top, right, bottom, modulePaint)
                }
            }
        }

        return bitmap
    }
}
