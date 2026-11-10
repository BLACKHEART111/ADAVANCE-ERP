package com.pocketdev.advance_erp.core.ai

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

object StockForecastEngine {

    fun generateForecast(
        products: List<ProductWithDetails>,
        invoices: List<SalesInvoice>,
        invoiceItems: List<SalesInvoiceItem>,
        analysisWindowDays: Int = 30
    ): StockForecastSummary {
        val now = System.currentTimeMillis()
        val windowMillis = analysisWindowDays.toLong() * 24L * 3600L * 1000L
        val cutoffTime = now - windowMillis

        // 1. Filter valid posted invoices within the analysis window
        val validInvoices = invoices.filter {
            it.isPosted && it.status != "CANCELLED" && it.invoiceDate >= cutoffTime
        }
        val validInvoiceIds = validInvoices.map { it.id }.toSet()

        // 2. Aggregate sales per product
        val salesByProduct = mutableMapOf<String, Int>()
        for (item in invoiceItems) {
            if (validInvoiceIds.contains(item.invoiceId)) {
                val netQty = max(0, item.quantity - item.returnedQuantity)
                salesByProduct[item.productId] = (salesByProduct[item.productId] ?: 0) + netQty
            }
        }

        // 3. Analyze each product
        val forecastItems = mutableListOf<StockForecastItem>()

        var urgentCount = 0
        var deadCount = 0
        var fastCount = 0
        var totalDeadCapitalMinor = 0L
        var totalInventoryCapitalMinor = 0L

        for (product in products) {
            val stock = max(0, product.totalStock)
            val unitsSold = salesByProduct[product.id] ?: 0
            val dailyVelocity = unitsSold.toDouble() / analysisWindowDays.toDouble()

            val daysUntilStockout = if (dailyVelocity > 0.0) {
                (stock.toDouble() / dailyVelocity).toInt()
            } else null

            val predictedRunoutDate = if (daysUntilStockout != null) {
                now + (daysUntilStockout.toLong() * 24L * 3600L * 1000L)
            } else null

            val isUrgentReorder = (stock <= product.minStockAlert) ||
                (daysUntilStockout != null && daysUntilStockout <= 7)

            val recommendedQty = if (isUrgentReorder) {
                val targetBuffer = max(product.minStockAlert * 2, ceil(dailyVelocity * 14).toInt() + product.minStockAlert)
                max(10, targetBuffer - stock)
            } else 0

            val isDeadStock = (stock > 0 && unitsSold == 0)
            val capitalLocked = stock.toLong() * product.purchasePriceMinor

            totalInventoryCapitalMinor += capitalLocked
            if (isDeadStock) {
                deadCount++
                totalDeadCapitalMinor += capitalLocked
            }
            if (isUrgentReorder) urgentCount++

            val velocityLevel = when {
                isDeadStock -> StockVelocityLevel.DEAD_STOCK
                dailyVelocity >= 1.0 -> {
                    fastCount++
                    StockVelocityLevel.HIGH_VELOCITY
                }
                dailyVelocity >= 0.2 -> StockVelocityLevel.MEDIUM_VELOCITY
                else -> StockVelocityLevel.LOW_VELOCITY
            }

            // AI Recommendations in Bengali & English
            val (recBn, recEn) = generateRecommendations(
                productName = product.nameEn,
                stock = stock,
                minAlert = product.minStockAlert,
                unitsSold = unitsSold,
                dailyVelocity = dailyVelocity,
                daysUntilStockout = daysUntilStockout,
                recommendedQty = recommendedQty,
                isDeadStock = isDeadStock,
                capitalLocked = capitalLocked,
                analysisDays = analysisWindowDays
            )

            forecastItems.add(
                StockForecastItem(
                    productId = product.id,
                    productName = product.nameEn,
                    sku = product.baseSku,
                    categoryName = product.categoryNameEn,
                    currentStock = stock,
                    minStockAlert = product.minStockAlert,
                    purchasePriceMinor = product.purchasePriceMinor,
                    sellingPriceMinor = product.sellingPriceMinor,
                    unitsSoldLast30Days = unitsSold,
                    dailyVelocity = dailyVelocity,
                    daysUntilStockout = daysUntilStockout,
                    predictedRunoutDate = predictedRunoutDate,
                    recommendedReorderQty = recommendedQty,
                    capitalLockedMinor = capitalLocked,
                    velocityLevel = velocityLevel,
                    isUrgentReorder = isUrgentReorder,
                    isDeadStock = isDeadStock,
                    recommendationBn = recBn,
                    recommendationEn = recEn
                )
            )
        }

        // Sort items: urgent reorders first, then dead stock, then descending by velocity
        forecastItems.sortWith(
            compareByDescending<StockForecastItem> { it.isUrgentReorder }
                .thenByDescending { it.isDeadStock }
                .thenByDescending { it.dailyVelocity }
        )

        return StockForecastSummary(
            totalProductsAnalyzed = products.size,
            urgentReorderCount = urgentCount,
            deadStockCount = deadCount,
            fastMovingCount = fastCount,
            totalCapitalInDeadStockMinor = totalDeadCapitalMinor,
            totalInventoryCapitalMinor = totalInventoryCapitalMinor,
            items = forecastItems
        )
    }

    private fun generateRecommendations(
        productName: String,
        stock: Int,
        minAlert: Int,
        unitsSold: Int,
        dailyVelocity: Double,
        daysUntilStockout: Int?,
        recommendedQty: Int,
        isDeadStock: Boolean,
        capitalLocked: Long,
        analysisDays: Int
    ): Pair<String, String> {
        val formattedCapital = "৳" + Formatters.formatMinorUnit(capitalLocked)

        return when {
            stock == 0 -> {
                Pair(
                    "🚨 স্টক খালি (Out of Stock)! দ্রুত $recommendedQty টি রিরর্ডার করে ইনভেন্টরি সচল করুন।",
                    "🚨 Out of stock! Reorder $recommendedQty units immediately to avoid lost sales."
                )
            }
            daysUntilStockout != null && daysUntilStockout <= 3 -> {
                Pair(
                    "⚠️ জরুরি সতর্কতা: বর্তমান গতিতে আগামী $daysUntilStockout দিনের মধ্যে স্টক শেষ হয়ে যাবে! অবিলম্বে $recommendedQty টি পণ্য রিরর্ডার করুন।",
                    "⚠️ Urgent Alert: Inventory will run out in $daysUntilStockout days! Reorder $recommendedQty units immediately."
                )
            }
            daysUntilStockout != null && daysUntilStockout <= 7 -> {
                Pair(
                    "🔔 স্টক অ্যালার্ট: প্রায় $daysUntilStockout দিনের স্টক অবশিষ্ট আছে। সাপ্লায়ারের সাথে যোগাযোগ করে $recommendedQty টি অর্ডার প্রস্তুত রাখুন।",
                    "🔔 Low Stock Alert: ~ $daysUntilStockout days of inventory remaining. Prepare purchase order for $recommendedQty units."
                )
            }
            isDeadStock -> {
                Pair(
                    "🧊 অচল স্টক (Dead Stock): গত $analysisDays দিনে কোনো বিক্রি নেই! মোট $formattedCapital মূলধন আটকে আছে। বিশেষ ডিসকাউন্ট, বান্ডেল অফার বা প্রোমোশন দিন।",
                    "🧊 Dead Stock: 0 sales in $analysisDays days! $formattedCapital tied up in capital. Consider clearance discount or promotional bundling."
                )
            }
            dailyVelocity >= 1.0 -> {
                val formattedVelocity = String.format(Locale.US, "%.1f", dailyVelocity)
                Pair(
                    "⚡ হট সেলিং পণ্য: দৈনিক গড়ে $formattedVelocity টি বিক্রি হচ্ছে। স্টক শেষ হওয়া রোধে বাফার স্টক পর্যাপ্ত রাখুন।",
                    "⚡ Fast Moving: Avg $formattedVelocity units sold daily. Ensure adequate safety buffer stock to capture all demand."
                )
            }
            else -> {
                Pair(
                    "✅ স্টক পরিস্থিতি সন্তোষজনক। নিয়মিত বিক্রয় পর্যবেক্ষণ বজায় রাখুন।",
                    "✅ Inventory level is balanced. Maintain normal stock monitoring."
                )
            }
        }
    }
}
