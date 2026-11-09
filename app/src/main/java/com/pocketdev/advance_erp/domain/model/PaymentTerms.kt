package com.pocketdev.advance_erp.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

enum class PaymentTermType(val labelEn: String, val labelBn: String) {
    HUNDRED_PERCENT_ADVANCE("100% Advance", "১০০% অগ্রিম পরিশোধ"),
    THIRTY_SEVENTY("30% Advance + 70% Balance", "৩০% অগ্রিম + ৭০% অবশিষ্ট"),
    FIFTY_FIFTY("50% Advance + 50% Balance", "৫০% অগ্রিম + ৫০% অবশিষ্ট"),
    CUSTOM_PARTIAL("Custom Partial Payment", "কাস্টম আংশিক কিস্তি"),
    FULL_DUE("Full Due (100% Later)", "সম্পূর্ণ বকেয়া (পরবর্তীতে পরিশোধ)")
}

data class PaymentScheduleInstallment(
    val installmentNumber: Int,
    val titleEn: String,
    val titleBn: String,
    val percentage: BigDecimal,
    val amountMinor: Long,
    val bdtAmountMinor: Long
)

object PaymentTermsCalculator {

    private val HUNDRED = BigDecimal("100")

    /**
     * Calculates the scheduled payment tranches based on the selected PaymentTermType.
     * Guarantees that:
     * 1. sum of amountMinor == totalAmountMinor exactly.
     * 2. sum of bdtAmountMinor == totalBdtAmountMinor exactly.
     */
    fun calculateSchedule(
        termType: PaymentTermType,
        totalAmountMinor: Long,
        exchangeRateToBdt: BigDecimal,
        customInstallmentPercentages: List<BigDecimal> = emptyList()
    ): List<PaymentScheduleInstallment> {
        if (totalAmountMinor <= 0L) return emptyList()

        val percentages: List<Pair<String, Pair<String, BigDecimal>>> = when (termType) {
            PaymentTermType.HUNDRED_PERCENT_ADVANCE -> listOf(
                "Advance Payment (100%)" to ("১০০% অগ্রিম পরিশোধ" to HUNDRED)
            )
            PaymentTermType.THIRTY_SEVENTY -> listOf(
                "Advance Deposit (30%)" to ("৩০% অগ্রিম ডিপোজিট" to BigDecimal("30")),
                "Final Balance (70%)" to ("৭০% চূড়ান্ত অবশিষ্ট" to BigDecimal("70"))
            )
            PaymentTermType.FIFTY_FIFTY -> listOf(
                "First Advance (50%)" to ("৫০% প্রথম অগ্রিম" to BigDecimal("50")),
                "Final Balance (50%)" to ("৫০% চূড়ান্ত অবশিষ্ট" to BigDecimal("50"))
            )
            PaymentTermType.FULL_DUE -> listOf(
                "Full Due on Arrival (100%)" to ("পৌঁছানোর পর সম্পূর্ণ পরিশোধ (১০০%)" to HUNDRED)
            )
            PaymentTermType.CUSTOM_PARTIAL -> {
                val list = if (customInstallmentPercentages.isNotEmpty()) {
                    customInstallmentPercentages
                } else {
                    listOf(BigDecimal("40"), BigDecimal("30"), BigDecimal("30"))
                }
                list.mapIndexed { i, pct ->
                    "Installment #${i + 1} (${pct}%)" to ("কিস্তি #${i + 1} (${pct}%)" to pct)
                }
            }
        }

        val totalPercentage = percentages.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.second.second) }
        require(totalPercentage.compareTo(HUNDRED) == 0) {
            "Total installment percentages must equal 100%, got $totalPercentage%"
        }

        val totalBd = BigDecimal(totalAmountMinor)
        var sumMinor = 0L
        val provisional = percentages.map { (en, bnAndPct) ->
            val (bn, pct) = bnAndPct
            val minor = totalBd.multiply(pct).divide(HUNDRED, 0, RoundingMode.FLOOR).longValueExact()
            sumMinor += minor
            Triple(en, bn, Pair(pct, minor))
        }.toMutableList()

        // Distribute remainder poisha/fen so sum equals totalAmountMinor exactly
        var remainder = totalAmountMinor - sumMinor
        var i = 0
        while (remainder > 0) {
            val (en, bn, pair) = provisional[i % provisional.size]
            provisional[i % provisional.size] = Triple(en, bn, Pair(pair.first, pair.second + 1L))
            remainder--
            i++
        }

        val totalBdtAmountMinor = BigDecimal(totalAmountMinor)
            .multiply(exchangeRateToBdt)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        var sumBdtMinor = 0L
        val provisionalBdt = provisional.map { (_, _, pair) ->
            val (_, minor) = pair
            val bdt = BigDecimal(minor).multiply(exchangeRateToBdt).setScale(0, RoundingMode.FLOOR).longValueExact()
            sumBdtMinor += bdt
            bdt
        }.toMutableList()

        var bdtRemainder = totalBdtAmountMinor - sumBdtMinor
        var j = 0
        while (bdtRemainder > 0) {
            provisionalBdt[j % provisionalBdt.size] = provisionalBdt[j % provisionalBdt.size] + 1L
            bdtRemainder--
            j++
        }

        return provisional.mapIndexed { idx, (en, bn, pair) ->
            PaymentScheduleInstallment(
                installmentNumber = idx + 1,
                titleEn = en,
                titleBn = bn,
                percentage = pair.first,
                amountMinor = pair.second,
                bdtAmountMinor = provisionalBdt[idx]
            )
        }
    }
}
