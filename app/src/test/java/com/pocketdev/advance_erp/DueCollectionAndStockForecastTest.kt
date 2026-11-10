package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.ai.StockForecastEngine
import com.pocketdev.advance_erp.core.ai.StockVelocityLevel
import com.pocketdev.advance_erp.core.util.DueCollectionCaptionHelper
import com.pocketdev.advance_erp.core.util.DueCollectionStatementData
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DueCollectionAndStockForecastTest {

    // ==========================================
    // 1. FEATURE 1: DUE COLLECTION CAPTION TESTS
    // ==========================================

    @Test
    fun testPartialPaymentCaptionBanglaAndEnglish() {
        val sampleData = DueCollectionStatementData(
            customerId = "cust-1",
            customerName = "Rahim Enterprise (রহিম এন্টারপ্রাইজ)",
            customerPhone = "01711223344",
            customerCode = "CUST-001",
            companyName = "ADVANCE ERP TRADERS",
            companyPhone = "01800112233",
            paymentNumber = "PAY-20261109-001",
            paymentDate = 1794250000000L,
            paymentMethod = "CASH",
            amountReceivedMinor = 500000L, // ৳5,000
            previousDueMinor = 2500000L,   // ৳25,000
            currentDueMinor = 2000000L,    // ৳20,000
            isFullPayment = false,
            isPartialPayment = true,
            isAdvancePayment = false,
            advanceAmountMinor = 0L,
            referenceNote = "Half payment for October"
        )

        // Bengali caption test
        val bnCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = true)
        assertTrue("Must contain customer name", bnCaption.contains("রহিম এন্টারপ্রাইজ"))
        assertTrue("Must contain company name", bnCaption.contains("ADVANCE ERP TRADERS"))
        assertTrue("Must contain customer care phone", bnCaption.contains("01800112233"))
        assertTrue("Must contain receipt number", bnCaption.contains("PAY-20261109-001"))
        assertTrue("Must show received amount ৳5,000.00", bnCaption.contains("5,000.00"))
        assertTrue("Must show previous due ৳25,000.00", bnCaption.contains("25,000.00"))
        assertTrue("Must show remaining due ৳20,000.00", bnCaption.contains("20,000.00"))
        assertTrue("Must mention PDF statement attachment", bnCaption.contains("PDF"))

        // English caption test
        val enCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = false)
        assertTrue("Must contain company in English", enCaption.contains("ADVANCE ERP TRADERS"))
        assertTrue("Must show received amount in English", enCaption.contains("5,000.00"))
        assertTrue("Must show remaining due in English", enCaption.contains("20,000.00"))
        assertTrue("Must indicate PDF attachment in English", enCaption.contains("ledger statement (PDF) is attached"))
    }

    @Test
    fun testFullPaymentClearsAllDues() {
        val sampleData = DueCollectionStatementData(
            customerId = "cust-2",
            customerName = "Karim Store",
            customerPhone = "01911223344",
            customerCode = "CUST-002",
            companyName = "ADVANCE ERP TRADERS",
            companyPhone = "01800112233",
            paymentNumber = "PAY-20261109-002",
            paymentDate = 1794250000000L,
            paymentMethod = "BANK",
            amountReceivedMinor = 1500000L, // ৳15,000
            previousDueMinor = 1500000L,   // ৳15,000
            currentDueMinor = 0L,          // ৳0.00
            isFullPayment = true,
            isPartialPayment = false,
            isAdvancePayment = false,
            advanceAmountMinor = 0L
        )

        val bnCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = true)
        assertTrue("Must acknowledge full payment in Bengali", bnCaption.contains("সম্পূর্ণ পরিশোধ হয়েছে"))
        assertTrue("Must show 0.00 current due", bnCaption.contains("0.00"))

        val enCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = false)
        assertTrue("Must acknowledge full settlement in English", enCaption.contains("fully settled"))
    }

    @Test
    fun testAdvancePaymentRecognition() {
        val sampleData = DueCollectionStatementData(
            customerId = "cust-3",
            customerName = "Hasan Brothers",
            customerPhone = "01611223344",
            customerCode = "CUST-003",
            companyName = "ADVANCE ERP TRADERS",
            companyPhone = "01800112233",
            paymentNumber = "PAY-20261109-003",
            paymentDate = 1794250000000L,
            paymentMethod = "BKASH",
            amountReceivedMinor = 1000000L, // ৳10,000 paid
            previousDueMinor = 300000L,     // ৳3,000 previous due
            currentDueMinor = -700000L,     // -৳7,000 credit advance
            isFullPayment = false,
            isPartialPayment = false,
            isAdvancePayment = true,
            advanceAmountMinor = 700000L    // ৳7,000 advance
        )

        val bnCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = true)
        assertTrue("Must state advance credit in Bengali", bnCaption.contains("অ্যাডভান্স হিসেবে জমা রয়েছে") || bnCaption.contains("অগ্রিম"))
        assertTrue("Must show advance amount ৳7,000.00", bnCaption.contains("7,000.00"))

        val enCaption = DueCollectionCaptionHelper.generateCaption(sampleData, isBangla = false)
        assertTrue("Must state advance credit balance in English", enCaption.contains("Advance Credit Balance"))
    }

    // ==========================================
    // 2. FEATURE 2: AI STOCK FORECAST TESTS
    // ==========================================

    @Test
    fun testAiStockForecastVelocityAndStockoutDays() {
        val now = System.currentTimeMillis()

        // Create sample product with 30 units in stock, alert threshold 10
        val fastProduct = ProductWithDetails(
            id = "prod-fast",
            categoryId = "cat-1",
            categoryNameEn = "Electronics",
            categoryNameBn = "ইলেকট্রনিক্স",
            nameEn = "Wireless Mouse Pro",
            nameBn = "ওয়্যারলেস মাউস",
            baseSku = "W-MOUSE-01",
            barcode = "890123456001",
            description = null,
            unit = "PCS",
            purchasePriceMinor = 40000L, // ৳400
            sellingPriceMinor = 65000L,
            minStockAlert = 10,
            isActive = true,
            createdAt = now - 1000000L,
            totalStock = 30, // 30 in stock
            variantCount = 0
        )

        // Create invoices with 60 units sold in the last 30 days (2 units/day velocity)
        val invoices = listOf(
            SalesInvoice(
                id = "inv-1",
                invoiceNumber = "INV-001",
                customerId = "cust-1",
                customerName = "Customer 1",
                warehouseId = "wh-1",
                warehouseName = "Main WH",
                invoiceDate = now - (5L * 24 * 3600 * 1000), // 5 days ago
                subtotalMinor = 6500000L,
                discountMinor = 0L,
                taxMinor = 0L,
                totalAmountMinor = 6500000L,
                paidAmountMinor = 6500000L,
                dueAmountMinor = 0L,
                status = "POSTED",
                isPosted = true
            )
        )

        val invoiceItems = listOf(
            SalesInvoiceItem(
                id = "item-1",
                invoiceId = "inv-1",
                productId = "prod-fast",
                productNameEn = "Wireless Mouse Pro",
                productNameBn = "ওয়্যারলেস মাউস প্রো",
                sku = "W-MOUSE-01",
                quantity = 60, // 60 sold in 30 days = 2.0 units/day
                unitPriceMinor = 65000L,
                unitCostMinor = 40000L,
                discountMinor = 0L,
                lineTotalMinor = 3900000L,
                returnedQuantity = 0
            )
        )

        val summary = StockForecastEngine.generateForecast(
            products = listOf(fastProduct),
            invoices = invoices,
            invoiceItems = invoiceItems,
            analysisWindowDays = 30
        )

        assertEquals(1, summary.totalProductsAnalyzed)
        val item = summary.items.first()

        assertEquals(60, item.unitsSoldLast30Days)
        assertEquals(2.0, item.dailyVelocity, 0.01) // 60 / 30 = 2.0
        // Stockout in: 30 stock / 2.0 velocity = 15 days
        assertEquals(15, item.daysUntilStockout)
        assertEquals(StockVelocityLevel.HIGH_VELOCITY, item.velocityLevel)
        assertFalse("Should not be dead stock", item.isDeadStock)
    }

    @Test
    fun testDeadStockDetectionAndCapitalLocked() {
        val now = System.currentTimeMillis()

        // Create sample product with 50 units in stock, but zero sales
        val deadProduct = ProductWithDetails(
            id = "prod-dead",
            categoryId = "cat-2",
            categoryNameEn = "Accessories",
            categoryNameBn = "এক্সেসরিজ",
            nameEn = "Old USB Cable 1.0",
            nameBn = "ইউএসবি ক্যাবল",
            baseSku = "USB-OLD-01",
            barcode = "890123456002",
            description = null,
            unit = "PCS",
            purchasePriceMinor = 15000L, // ৳150 cost
            sellingPriceMinor = 25000L,
            minStockAlert = 5,
            isActive = true,
            createdAt = now - (60L * 24 * 3600 * 1000),
            totalStock = 50, // 50 in stock
            variantCount = 0
        )

        val summary = StockForecastEngine.generateForecast(
            products = listOf(deadProduct),
            invoices = emptyList(), // Zero sales
            invoiceItems = emptyList(),
            analysisWindowDays = 30
        )

        assertEquals(1, summary.deadStockCount)
        val item = summary.items.first()
        assertTrue("Item must be flagged as Dead Stock", item.isDeadStock)
        assertEquals(StockVelocityLevel.DEAD_STOCK, item.velocityLevel)
        // Capital locked: 50 * ৳150 = ৳7,500 = 750,000 minor units
        assertEquals(750000L, item.capitalLockedMinor)
        assertEquals(750000L, summary.totalCapitalInDeadStockMinor)
        assertTrue("Bengali recommendation must mention dead stock", item.recommendationBn.contains("অচল স্টক"))
    }

    @Test
    fun testUrgentReorderRecommendationWhenNearRunout() {
        val now = System.currentTimeMillis()

        // Product with 5 units remaining, selling 2/day -> runs out in 2 days!
        val criticalProduct = ProductWithDetails(
            id = "prod-critical",
            categoryId = "cat-1",
            categoryNameEn = "Snacks",
            categoryNameBn = "স্ন্যাক্স",
            nameEn = "Premium Chocolate Bar",
            nameBn = "চকলেট বার",
            baseSku = "CHOC-01",
            barcode = null,
            description = null,
            unit = "PCS",
            purchasePriceMinor = 5000L,
            sellingPriceMinor = 8000L,
            minStockAlert = 10,
            isActive = true,
            createdAt = now,
            totalStock = 5,
            variantCount = 0
        )

        val invoices = listOf(
            SalesInvoice(
                id = "inv-crit",
                invoiceNumber = "INV-CRIT",
                customerId = "cust-1",
                customerName = "Customer",
                warehouseId = "wh-1",
                warehouseName = "WH",
                invoiceDate = now - 100000L,
                subtotalMinor = 480000L,
                discountMinor = 0L,
                taxMinor = 0L,
                totalAmountMinor = 480000L,
                paidAmountMinor = 480000L,
                dueAmountMinor = 0L,
                status = "POSTED",
                isPosted = true
            )
        )
        val invoiceItems = listOf(
            SalesInvoiceItem(
                id = "crit-item",
                invoiceId = "inv-crit",
                productId = "prod-critical",
                productNameEn = "Premium Chocolate Bar",
                productNameBn = "প্রিমিয়াম চকলেট বার",
                sku = "CHOC-01",
                quantity = 60, // 2/day
                unitPriceMinor = 8000L,
                unitCostMinor = 5000L,
                discountMinor = 0L,
                lineTotalMinor = 480000L,
                returnedQuantity = 0
            )
        )

        val summary = StockForecastEngine.generateForecast(
            products = listOf(criticalProduct),
            invoices = invoices,
            invoiceItems = invoiceItems,
            analysisWindowDays = 30
        )

        val item = summary.items.first()
        assertTrue("Item must be flagged as Urgent Reorder", item.isUrgentReorder)
        assertEquals(1, summary.urgentReorderCount)
        assertEquals(2, item.daysUntilStockout)
        assertTrue("Must recommend reorder quantity > 0", item.recommendedReorderQty > 0)
        assertTrue("Recommendation must alert impending stockout", item.recommendationBn.contains("জরুরি সতর্কতা") || item.recommendationBn.contains("অ্যালার্ট"))
    }
}
