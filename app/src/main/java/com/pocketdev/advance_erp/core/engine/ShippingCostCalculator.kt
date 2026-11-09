package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode

data class ProductWeightInput(
    val productId: String,
    val productName: String,
    val grossWeightKg: BigDecimal
)

data class ProductShippingAllocation(
    val productId: String,
    val productName: String,
    val grossWeightKg: BigDecimal,
    val shippingCostBdtMinor: Long
)

object ShippingCostCalculator {

    /**
     * Calculates total shipping cost based on Gross Weight:
     * Shipping Cost = Gross Weight (KG) × Rate per KG
     */
    fun calculateShippingCost(totalGrossWeightKg: BigDecimal, ratePerKgBdt: BigDecimal): BigDecimal {
        require(ratePerKgBdt >= BigDecimal.ZERO) { "Shipping rate per KG cannot be negative: $ratePerKgBdt" }
        require(totalGrossWeightKg >= BigDecimal.ZERO) { "Gross weight cannot be negative: $totalGrossWeightKg" }
        return totalGrossWeightKg.multiply(ratePerKgBdt).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates total shipping cost and allocates it to products based on their Gross Weight.
     * Shipping Cost = Total Gross Weight (KG) × Shipping Rate per KG
     * Product Shipping = Product Gross Weight (KG) × Shipping Rate per KG
     *
     * Guarantee: sum of product allocations == total shipping cost exactly.
     */
    fun calculateAndAllocate(
        products: List<ProductWeightInput>,
        ratePerKgBdt: BigDecimal
    ): Pair<Long, List<ProductShippingAllocation>> {
        require(ratePerKgBdt >= BigDecimal.ZERO) { "Shipping rate per KG cannot be negative: $ratePerKgBdt" }
        if (products.isEmpty() || ratePerKgBdt.compareTo(BigDecimal.ZERO) == 0) {
            return 0L to products.map {
                ProductShippingAllocation(
                    productId = it.productId,
                    productName = it.productName,
                    grossWeightKg = it.grossWeightKg,
                    shippingCostBdtMinor = 0L
                )
            }
        }

        val totalGrossWeight = products.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.grossWeightKg) }
        val totalShippingCostBdt = totalGrossWeight.multiply(ratePerKgBdt)
        // Convert to minor units (poisha, multiply by 100)
        val totalShippingMinor = totalShippingCostBdt.multiply(BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()

        if (totalGrossWeight.compareTo(BigDecimal.ZERO) == 0 || totalShippingMinor == 0L) {
            return totalShippingMinor to products.map {
                ProductShippingAllocation(it.productId, it.productName, it.grossWeightKg, 0L)
            }
        }

        // Allocate using LandedCostAllocationEngine by Gross Weight
        val allocationInputs = products.map {
            AllocationItemInput(
                id = it.productId,
                name = it.productName,
                quantity = 1,
                grossWeightKg = it.grossWeightKg
            )
        }

        val results = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = totalShippingMinor,
            items = allocationInputs,
            method = AllocationMethod.GROSS_WEIGHT
        )

        val resultMap = results.associateBy { it.itemId }

        val allocations = products.map { p ->
            val alloc = resultMap[p.productId]
            ProductShippingAllocation(
                productId = p.productId,
                productName = p.productName,
                grossWeightKg = p.grossWeightKg,
                shippingCostBdtMinor = alloc?.allocatedBdtMinor ?: 0L
            )
        }

        return totalShippingMinor to allocations
    }
}
