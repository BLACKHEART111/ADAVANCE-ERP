package com.pocketdev.advance_erp.core.ai

enum class StockVelocityLevel {
    HIGH_VELOCITY,   // Fast moving (হট আইটেম) - 1+ units/day
    MEDIUM_VELOCITY, // Steady sales
    LOW_VELOCITY,    // Slow moving
    DEAD_STOCK       // Zero sales in analysis window (অচল স্টক)
}

data class StockForecastItem(
    val productId: String,
    val productName: String,
    val sku: String,
    val categoryName: String,
    val currentStock: Int,
    val minStockAlert: Int,
    val purchasePriceMinor: Long,
    val sellingPriceMinor: Long,
    val unitsSoldLast30Days: Int,
    val dailyVelocity: Double,
    val daysUntilStockout: Int?,
    val predictedRunoutDate: Long?,
    val recommendedReorderQty: Int,
    val capitalLockedMinor: Long,
    val velocityLevel: StockVelocityLevel,
    val isUrgentReorder: Boolean,
    val isDeadStock: Boolean,
    val recommendationBn: String,
    val recommendationEn: String
)

data class StockForecastSummary(
    val totalProductsAnalyzed: Int,
    val urgentReorderCount: Int,
    val deadStockCount: Int,
    val fastMovingCount: Int,
    val totalCapitalInDeadStockMinor: Long,
    val totalInventoryCapitalMinor: Long,
    val items: List<StockForecastItem>
)
