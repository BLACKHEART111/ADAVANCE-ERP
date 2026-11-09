package com.pocketdev.advance_erp.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a monetary amount in minor units (e.g. poisha/cents).
 * Financial operations strictly avoid Float and Double.
 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode = CurrencyCode.BDT
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency vs ${other.currency}" }
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency vs ${other.currency}" }
        return Money(minorUnits - other.minorUnits, currency)
    }

    operator fun times(factor: Long): Money {
        return Money(minorUnits * factor, currency)
    }

    operator fun times(factor: BigDecimal): Money {
        val bdMinor = BigDecimal(minorUnits).multiply(factor).setScale(0, RoundingMode.HALF_UP)
        return Money(bdMinor.longValueExact(), currency)
    }

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies: $currency vs ${other.currency}" }
        return minorUnits.compareTo(other.minorUnits)
    }

    val isPositive: Boolean get() = minorUnits > 0
    val isZero: Boolean get() = minorUnits == 0L
    val isNegative: Boolean get() = minorUnits < 0

    companion object {
        val ZERO = Money(0L, CurrencyCode.BDT)

        fun ofMinor(minorUnits: Long, currency: CurrencyCode = CurrencyCode.BDT): Money {
            return Money(minorUnits, currency)
        }

        fun ofMajor(majorUnits: Long, currency: CurrencyCode = CurrencyCode.BDT): Money {
            return Money(majorUnits * 100L, currency)
        }

        fun parseFromString(amountStr: String, currency: CurrencyCode = CurrencyCode.BDT): Money {
            val cleaned = amountStr.trim().replace(",", "")
            if (cleaned.isBlank()) return ofMinor(0L, currency)
            val bd = BigDecimal(cleaned)
            val minor = bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            return Money(minor, currency)
        }
    }
}
