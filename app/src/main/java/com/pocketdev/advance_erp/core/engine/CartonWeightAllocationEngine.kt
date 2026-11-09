package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode

data class MixedCartonLineInput(
    val productId: String,
    val variantId: String? = null,
    val productName: String = "",
    val quantity: Int
)

data class MixedCartonLineAllocated(
    val productId: String,
    val variantId: String? = null,
    val productName: String = "",
    val quantity: Int,
    val allocatedGrossWeightKg: BigDecimal,
    val allocatedNetWeightKg: BigDecimal
)

object CartonWeightAllocationEngine {

    private const val WEIGHT_SCALE = 3 // 3 decimal places for KG precision (e.g. 10.000 KG)

    /**
     * Mandatory Business Rule:
     * Product Allocated Weight = Carton Gross Weight × (Product Quantity / Total Carton Quantity)
     *
     * Guarantees:
     * 1. Exact carton gross weight is preserved.
     * 2. No duplicate weight allocation.
     * 3. Decimal precision is maintained.
     * 4. Rounding residuals are handled so allocated weights always sum exactly to carton gross weight.
     * 5. Product quantities must sum to the total carton quantity.
     * 6. Rejects negative or zero quantities.
     */
    fun allocateWeights(
        cartonGrossWeightKg: BigDecimal,
        cartonNetWeightKg: BigDecimal = BigDecimal.ZERO,
        lines: List<MixedCartonLineInput>
    ): List<MixedCartonLineAllocated> {
        require(cartonGrossWeightKg > BigDecimal.ZERO) {
            "Carton gross weight must be greater than zero: $cartonGrossWeightKg"
        }
        require(lines.isNotEmpty()) {
            "Carton must contain at least one product line"
        }
        lines.forEach { line ->
            require(line.quantity > 0) {
                "Product line quantity must be strictly positive for ${line.productName.ifBlank { line.productId }}: ${line.quantity}"
            }
        }

        val totalCartonQuantity = lines.sumOf { it.quantity }
        require(totalCartonQuantity > 0) {
            "Total carton quantity must be greater than zero: $totalCartonQuantity"
        }

        val totalQtyBd = BigDecimal(totalCartonQuantity)

        // 1. Allocate gross weight using FLOOR to guarantee allocatedGrossSum <= cartonGrossWeightKg
        var allocatedGrossSum = BigDecimal.ZERO
        var allocatedNetSum = BigDecimal.ZERO

        val preliminary = lines.map { line ->
            val qtyBd = BigDecimal(line.quantity)
            // allocated = cartonGrossWeight * (qty / totalQty)
            val grossShare = cartonGrossWeightKg.multiply(qtyBd)
                .divide(totalQtyBd, WEIGHT_SCALE, RoundingMode.FLOOR)

            val netShare = if (cartonNetWeightKg > BigDecimal.ZERO) {
                cartonNetWeightKg.multiply(qtyBd)
                    .divide(totalQtyBd, WEIGHT_SCALE, RoundingMode.FLOOR)
            } else {
                BigDecimal.ZERO
            }

            allocatedGrossSum = allocatedGrossSum.add(grossShare)
            allocatedNetSum = allocatedNetSum.add(netShare)

            Triple(line, grossShare, netShare)
        }.toMutableList()

        // 2. Rounding Residual Reconciliation for Gross Weight
        val grossResidual = cartonGrossWeightKg.subtract(allocatedGrossSum)
        val netResidual = if (cartonNetWeightKg > BigDecimal.ZERO) {
            cartonNetWeightKg.subtract(allocatedNetSum)
        } else {
            BigDecimal.ZERO
        }

        // Distribute 0.001 residual poisha-by-poisha to the largest quantity lines
        // Sort indices by quantity descending to distribute rounding residuals to the most significant lines
        val sortedIndices = lines.indices.sortedByDescending { lines[it].quantity }

        var remainingGrossSteps = (grossResidual.multiply(BigDecimal.valueOf(Math.pow(10.0, WEIGHT_SCALE.toDouble())))).toInt()
        var remainingNetSteps = (netResidual.multiply(BigDecimal.valueOf(Math.pow(10.0, WEIGHT_SCALE.toDouble())))).toInt()
        val stepUnit = BigDecimal.ONE.movePointLeft(WEIGHT_SCALE)

        val finalLines = preliminary.toMutableList()

        var idx = 0
        while (remainingGrossSteps > 0 && idx < sortedIndices.size) {
            val targetIdx = sortedIndices[idx % sortedIndices.size]
            val current = finalLines[targetIdx]
            finalLines[targetIdx] = Triple(current.first, current.second.add(stepUnit), current.third)
            remainingGrossSteps--
            idx++
        }

        idx = 0
        while (remainingNetSteps > 0 && idx < sortedIndices.size) {
            val targetIdx = sortedIndices[idx % sortedIndices.size]
            val current = finalLines[targetIdx]
            finalLines[targetIdx] = Triple(current.first, current.second, current.third.add(stepUnit))
            remainingNetSteps--
            idx++
        }

        // 3. Return final lines and sanity check exact sum
        val result = finalLines.map {
            MixedCartonLineAllocated(
                productId = it.first.productId,
                variantId = it.first.variantId,
                productName = it.first.productName,
                quantity = it.first.quantity,
                allocatedGrossWeightKg = it.second,
                allocatedNetWeightKg = it.third
            )
        }

        val finalGrossSum = result.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.allocatedGrossWeightKg) }
        check(finalGrossSum.compareTo(cartonGrossWeightKg) == 0) {
            "Internal Error: Allocated gross weight sum ($finalGrossSum) does not match carton gross weight ($cartonGrossWeightKg)"
        }

        return result
    }
}
