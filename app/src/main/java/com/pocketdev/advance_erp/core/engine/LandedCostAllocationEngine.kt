package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode

enum class AllocationMethod(val titleEn: String, val titleBn: String) {
    QUANTITY("By Quantity", "পরিমাণ অনুযায়ী"),
    GROSS_WEIGHT("By Gross Weight", "মোট ওজন অনুযায়ী"),
    CBM("By CBM (Volume)", "আয়তন (সিবিএম) অনুযায়ী"),
    PURCHASE_VALUE("By Purchase Value", "ক্রয়মূল্য অনুযায়ী"),
    MANUAL("Manual Allocation", "ম্যানুয়াল বরাদ্দ")
}

data class AllocationItemInput(
    val id: String,
    val name: String,
    val quantity: Int,
    val grossWeightKg: BigDecimal = BigDecimal.ZERO,
    val cbm: BigDecimal = BigDecimal.ZERO,
    val purchaseValueBdtMinor: Long = 0L,
    val manualRatio: BigDecimal = BigDecimal.ZERO
)

data class AllocationResultItem(
    val itemId: String,
    val name: String,
    val basisValue: BigDecimal,
    val basisPercentage: BigDecimal,
    val allocatedBdtMinor: Long,
    val unitAllocatedCostBdtMinor: Long
)

object LandedCostAllocationEngine {

    /**
     * Allocates a given cost in BDT minor units among items according to the selected AllocationMethod.
     * Guarantees that:
     * sum of all allocatedBdtMinor == totalCostBdtMinor exactly.
     */
    fun allocate(
        totalCostBdtMinor: Long,
        items: List<AllocationItemInput>,
        method: AllocationMethod
    ): List<AllocationResultItem> {
        if (items.isEmpty() || totalCostBdtMinor == 0L) {
            return items.map { item ->
                AllocationResultItem(
                    itemId = item.id,
                    name = item.name,
                    basisValue = BigDecimal.ZERO,
                    basisPercentage = BigDecimal.ZERO,
                    allocatedBdtMinor = 0L,
                    unitAllocatedCostBdtMinor = 0L
                )
            }
        }

        // 1. Extract basis value per item
        val bases: List<Pair<AllocationItemInput, BigDecimal>> = items.map { item ->
            val basis = when (method) {
                AllocationMethod.QUANTITY -> BigDecimal(item.quantity)
                AllocationMethod.GROSS_WEIGHT -> item.grossWeightKg
                AllocationMethod.CBM -> item.cbm
                AllocationMethod.PURCHASE_VALUE -> BigDecimal(item.purchaseValueBdtMinor)
                AllocationMethod.MANUAL -> item.manualRatio
            }
            require(basis >= BigDecimal.ZERO) { "Allocation basis cannot be negative for ${item.name}: $basis" }
            item to basis
        }

        val totalBasis = bases.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.second) }

        // If total basis is 0 (e.g. all 0 weight), fallback to equal distribution
        val effectiveBases = if (totalBasis.compareTo(BigDecimal.ZERO) == 0) {
            val equalBasis = BigDecimal.ONE
            bases.map { it.first to equalBasis }
        } else {
            bases
        }

        val effectiveTotalBasis = effectiveBases.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.second) }
        val totalCostBd = BigDecimal(totalCostBdtMinor)

        // 2. Initial allocation (floor/half_down) to avoid over-allocation
        var allocatedSum = 0L
        val provisionalResults = mutableListOf<Triple<AllocationItemInput, BigDecimal, Long>>()

        effectiveBases.forEach { (item, basis) ->
            // Fraction = basis / effectiveTotalBasis
            // Amount = (totalCost * basis) / effectiveTotalBasis
            val itemShare = totalCostBd.multiply(basis).divide(effectiveTotalBasis, 0, RoundingMode.FLOOR).longValueExact()
            allocatedSum += itemShare
            provisionalResults.add(Triple(item, basis, itemShare))
        }

        // 3. Exact Reconciliation: distribute rounding remainder
        var remainder = totalCostBdtMinor - allocatedSum
        val reconciledAmounts = provisionalResults.map { it.third }.toMutableList()

        // Distribute remainder unit-by-unit (poisha-by-poisha) to items with highest basis
        val sortedIndices = provisionalResults.indices.sortedByDescending { effectiveBases[it].second }
        var idx = 0
        while (remainder > 0) {
            val targetIndex = sortedIndices[idx % sortedIndices.size]
            reconciledAmounts[targetIndex] = reconciledAmounts[targetIndex] + 1L
            remainder -= 1L
            idx++
        }

        // 4. Construct final results with percentage and per-unit cost
        val hundred = BigDecimal("100")
        val finalResults = effectiveBases.mapIndexed { i, (item, basis) ->
            val allocatedMinor = reconciledAmounts[i]
            val percentage = if (effectiveTotalBasis > BigDecimal.ZERO) {
                basis.multiply(hundred).divide(effectiveTotalBasis, 4, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val unitAllocatedMinor = if (item.quantity > 0) {
                BigDecimal(allocatedMinor).divide(BigDecimal(item.quantity), 0, RoundingMode.HALF_UP).longValueExact()
            } else {
                0L
            }

            AllocationResultItem(
                itemId = item.id,
                name = item.name,
                basisValue = basis,
                basisPercentage = percentage,
                allocatedBdtMinor = allocatedMinor,
                unitAllocatedCostBdtMinor = unitAllocatedMinor
            )
        }

        // Sanity assertion: Exact reconciliation check
        val actualTotal = finalResults.sumOf { it.allocatedBdtMinor }
        check(actualTotal == totalCostBdtMinor) {
            "Internal reconciliation error: sum=$actualTotal vs total=$totalCostBdtMinor"
        }

        return finalResults
    }
}
