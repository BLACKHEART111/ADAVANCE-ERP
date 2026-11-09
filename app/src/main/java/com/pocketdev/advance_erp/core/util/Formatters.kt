package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val BANGLA_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(BANGLA_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            val idx = BANGLA_DIGITS.indexOf(ch)
            if (idx >= 0) {
                sb.append(('0' + idx))
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatMoney(money: Money, language: AppLanguage): String {
        return formatMoney(money.minorUnits, money.currency, language)
    }

    fun formatMoney(minorUnits: Long, currency: CurrencyCode, language: AppLanguage): String {
        val isNegative = minorUnits < 0
        val absMinor = kotlin.math.abs(minorUnits)
        val majorPart = absMinor / 100
        val minorPart = absMinor % 100

        val symbols = DecimalFormatSymbols(Locale.US)
        val majorFormatted = DecimalFormat("#,##0", symbols).format(majorPart)
        val fullFormatted = String.format(Locale.US, "%s.%02d", majorFormatted, minorPart)

        val sign = if (isNegative) "-" else ""
        return if (language == AppLanguage.BANGLA) {
            val banglaFormatted = toBanglaDigits(fullFormatted)
            "$sign${currency.symbol} $banglaFormatted"
        } else {
            "$sign${currency.symbol} $fullFormatted"
        }
    }

    fun formatCurrency(minorUnits: Long, currency: CurrencyCode, language: AppLanguage): String {
        return formatMoney(minorUnits, currency, language)
    }

    fun formatNumber(number: Long, language: AppLanguage): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatted = DecimalFormat("#,##0", symbols).format(number)
        return if (language == AppLanguage.BANGLA) {
            toBanglaDigits(formatted)
        } else {
            formatted
        }
    }

    fun formatMinorUnit(minorUnits: Long): String {
        val isNegative = minorUnits < 0
        val absMinor = kotlin.math.abs(minorUnits)
        val majorPart = absMinor / 100
        val minorPart = absMinor % 100
        val symbols = DecimalFormatSymbols(Locale.US)
        val majorFormatted = DecimalFormat("#,##0", symbols).format(majorPart)
        val sign = if (isNegative) "-" else ""
        return String.format(Locale.US, "%s%s.%02d", sign, majorFormatted, minorPart)
    }

    fun formatDate(timestampMs: Long, language: AppLanguage): String {
        val pattern = if (language == AppLanguage.BANGLA) "dd MMM, yyyy" else "MMM dd, yyyy"
        val sdf = SimpleDateFormat(pattern, language.locale)
        val dateStr = sdf.format(Date(timestampMs))
        return if (language == AppLanguage.BANGLA) {
            toBanglaDigits(dateStr)
        } else {
            dateStr
        }
    }

    fun formatTime(timestampMs: Long, language: AppLanguage): String {
        val sdf = SimpleDateFormat("hh:mm a", language.locale)
        val timeStr = sdf.format(Date(timestampMs))
        return if (language == AppLanguage.BANGLA) {
            toBanglaDigits(timeStr)
        } else {
            timeStr
        }
    }
}
