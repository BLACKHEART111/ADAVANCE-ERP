package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.feature.sales.PaymentDateFilter
import com.pocketdev.advance_erp.feature.sales.isSameCalendarDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PaymentSearchAndUniqueTrackingTest {

    private val now = System.currentTimeMillis()
    private val yesterdayMillis = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    private val threeDaysAgoMillis = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -3)
    }.timeInMillis
    private val lastMonthMillis = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -40)
    }.timeInMillis

    private val customers = listOf(
        Customer(
            id = "cust-1",
            customerCode = "CUST-000001",
            name = "Md. Rahim Uddin",
            phone = "01711223344",
            address = "Dhaka",
            creditLimitMinor = 5000000L,
            outstandingBalanceMinor = 1500000L
        ),
        Customer(
            id = "cust-2",
            customerCode = "CUST-000002",
            name = "Karim Traders",
            phone = "01899887766",
            address = "Chittagong",
            creditLimitMinor = 10000000L,
            outstandingBalanceMinor = 0L
        ),
        Customer(
            id = "cust-3",
            customerCode = "CUST-000003",
            name = "Hasan Enterprise",
            phone = "01955443322",
            address = "Sylhet",
            creditLimitMinor = 2000000L,
            outstandingBalanceMinor = 500000L
        )
    )

    private val payments = listOf(
        CustomerPayment(
            id = "pay-1",
            paymentNumber = "CP-2026-0001",
            customerId = "cust-1",
            customerName = "Md. Rahim Uddin",
            amountMinor = 500000L, // 5,000.00
            paymentMethod = "CASH",
            referenceNote = "MR-1001",
            paymentDate = now, // Today
            isPosted = true,
            isVoided = false
        ),
        CustomerPayment(
            id = "pay-2",
            paymentNumber = "CP-2026-0002",
            customerId = "cust-2",
            customerName = "Karim Traders",
            amountMinor = 1200000L, // 12,000.00
            paymentMethod = "BANK",
            referenceNote = "CHQ-778899",
            paymentDate = yesterdayMillis, // Yesterday
            isPosted = true,
            isVoided = false
        ),
        CustomerPayment(
            id = "pay-3",
            paymentNumber = "CP-2026-0003",
            customerId = "cust-1",
            customerName = "Md. Rahim Uddin",
            amountMinor = 300000L, // 3,000.00
            paymentMethod = "BKASH",
            referenceNote = "TrxID-987654",
            paymentDate = threeDaysAgoMillis, // 3 days ago (this week)
            isPosted = true,
            isVoided = false
        ),
        CustomerPayment(
            id = "pay-4",
            paymentNumber = "CP-2026-0004",
            customerId = "cust-3",
            customerName = "Hasan Enterprise",
            amountMinor = 800000L,
            paymentMethod = "CASH",
            referenceNote = null,
            paymentDate = lastMonthMillis, // 40 days ago
            isPosted = true,
            isVoided = true,
            voidReason = "Bounced Cheque"
        )
    )

    private val customersMap = customers.associateBy { it.id }

    private fun filterPayments(
        searchQuery: String = "",
        selectedMethodFilter: String = "ALL",
        selectedDateFilter: PaymentDateFilter = PaymentDateFilter.ALL,
        selectedCustomDateMillis: Long = now
    ): List<CustomerPayment> {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        return payments.filter { pay ->
            val cust = customersMap[pay.customerId]
            val matchesSearch = if (q.isBlank()) {
                true
            } else {
                val matchesNumber = pay.paymentNumber.lowercase(Locale.ROOT).contains(q)
                val matchesCustName = pay.customerName.lowercase(Locale.ROOT).contains(q)
                val matchesPhone = cust?.phone?.lowercase(Locale.ROOT)?.contains(q) == true
                val matchesRef = pay.referenceNote?.lowercase(Locale.ROOT)?.contains(q) == true

                val payDate = Date(pay.paymentDate)
                val d1 = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(payDate)
                val d2 = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(payDate)
                val d3 = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(payDate)
                val d4 = SimpleDateFormat("dd MMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val d5 = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val d6 = SimpleDateFormat("MMM yyyy", Locale.US).format(payDate).lowercase(Locale.ROOT)
                val dBnDigits1 = Formatters.toBanglaDigits(d1)
                val dBnDigits2 = Formatters.toBanglaDigits(d2)
                val dBnDigits3 = Formatters.toBanglaDigits(d3)
                val matchesDateText = d1.contains(q) || d2.contains(q) || d3.contains(q) ||
                                      d4.contains(q) || d5.contains(q) || d6.contains(q) ||
                                      dBnDigits1.contains(q) || dBnDigits2.contains(q) || dBnDigits3.contains(q)

                matchesNumber || matchesCustName || matchesPhone || matchesRef || matchesDateText
            }

            val matchesMethod = when (selectedMethodFilter) {
                "ALL" -> true
                "VOIDED" -> pay.isVoided
                "MFS" -> !pay.isVoided && (pay.paymentMethod.equals("BKASH", ignoreCase = true) || pay.paymentMethod.equals("NAGAD", ignoreCase = true))
                else -> !pay.isVoided && pay.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
            }

            val matchesDate = when (selectedDateFilter) {
                PaymentDateFilter.ALL -> true
                PaymentDateFilter.TODAY -> isSameCalendarDay(pay.paymentDate, now)
                PaymentDateFilter.YESTERDAY -> isSameCalendarDay(pay.paymentDate, yesterdayMillis)
                PaymentDateFilter.THIS_WEEK -> {
                    val weekStart = Calendar.getInstance().apply {
                        timeInMillis = now
                        add(Calendar.DAY_OF_YEAR, -6)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val nowEnd = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    pay.paymentDate in weekStart..nowEnd
                }
                PaymentDateFilter.THIS_MONTH -> {
                    val monthStart = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val monthEnd = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    pay.paymentDate in monthStart..monthEnd
                }
                PaymentDateFilter.CUSTOM_DATE -> isSameCalendarDay(pay.paymentDate, selectedCustomDateMillis)
            }

            matchesSearch && matchesMethod && matchesDate
        }
    }

    @Test
    fun testSearchByPaymentUniqueNumber() {
        val results = filterPayments("CP-2026-0002")
        assertEquals(1, results.size)
        assertEquals("CP-2026-0002", results[0].paymentNumber)
        assertEquals("Karim Traders", results[0].customerName)

        val partialNumberResults = filterPayments("0003")
        assertEquals(1, partialNumberResults.size)
        assertEquals("CP-2026-0003", partialNumberResults[0].paymentNumber)
    }

    @Test
    fun testSearchByCustomerName() {
        val results = filterPayments("Rahim")
        assertEquals(2, results.size)
        assertTrue(results.all { it.customerId == "cust-1" })

        val karimResults = filterPayments("karim")
        assertEquals(1, karimResults.size)
        assertEquals("Karim Traders", karimResults[0].customerName)
    }

    @Test
    fun testSearchByCustomerPhone() {
        val results = filterPayments("01711223344")
        assertEquals(2, results.size)
        assertTrue(results.all { it.customerName == "Md. Rahim Uddin" })

        val karimPhoneResults = filterPayments("8877")
        assertEquals(1, karimPhoneResults.size)
        assertEquals("Karim Traders", karimPhoneResults[0].customerName)
    }

    @Test
    fun testSearchByReferenceNote() {
        val results = filterPayments("CHQ-778899")
        assertEquals(1, results.size)
        assertEquals("CP-2026-0002", results[0].paymentNumber)

        val trxResults = filterPayments("TrxID")
        assertEquals(1, trxResults.size)
        assertEquals("CP-2026-0003", trxResults[0].paymentNumber)
    }

    @Test
    fun testFilterByPaymentMethod() {
        val cashPayments = filterPayments(selectedMethodFilter = "CASH")
        assertEquals(1, cashPayments.size)
        assertEquals("CP-2026-0001", cashPayments[0].paymentNumber)

        val bankPayments = filterPayments(selectedMethodFilter = "BANK")
        assertEquals(1, bankPayments.size)
        assertEquals("CP-2026-0002", bankPayments[0].paymentNumber)

        val mfsPayments = filterPayments(selectedMethodFilter = "MFS")
        assertEquals(1, mfsPayments.size)
        assertEquals("CP-2026-0003", mfsPayments[0].paymentNumber)

        val voidedPayments = filterPayments(selectedMethodFilter = "VOIDED")
        assertEquals(1, voidedPayments.size)
        assertEquals("CP-2026-0004", voidedPayments[0].paymentNumber)
    }

    @Test
    fun testFilterByDatePresets() {
        // Today
        val todayResults = filterPayments(selectedDateFilter = PaymentDateFilter.TODAY)
        assertEquals(1, todayResults.size)
        assertEquals("CP-2026-0001", todayResults[0].paymentNumber)

        // Yesterday
        val yesterdayResults = filterPayments(selectedDateFilter = PaymentDateFilter.YESTERDAY)
        assertEquals(1, yesterdayResults.size)
        assertEquals("CP-2026-0002", yesterdayResults[0].paymentNumber)

        // This Week (last 7 days: pay-1, pay-2, pay-3)
        val weekResults = filterPayments(selectedDateFilter = PaymentDateFilter.THIS_WEEK)
        assertEquals(3, weekResults.size)
        assertTrue(weekResults.any { it.paymentNumber == "CP-2026-0001" })
        assertTrue(weekResults.any { it.paymentNumber == "CP-2026-0002" })
        assertTrue(weekResults.any { it.paymentNumber == "CP-2026-0003" })

        // Custom specific date
        val customDateResults = filterPayments(
            selectedDateFilter = PaymentDateFilter.CUSTOM_DATE,
            selectedCustomDateMillis = yesterdayMillis
        )
        assertEquals(1, customDateResults.size)
        assertEquals("CP-2026-0002", customDateResults[0].paymentNumber)
    }

    @Test
    fun testSearchByDateTypedInSearchBar() {
        // Format of pay-2's date in dd/MM/yyyy
        val dateStringPay2 = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(yesterdayMillis))
        val resultsByDateString = filterPayments(searchQuery = dateStringPay2)
        assertTrue("Should find pay-2 by exact date string", resultsByDateString.any { it.paymentNumber == "CP-2026-0002" })

        // Bengali numerals of pay-2's date
        val bnDateString = Formatters.toBanglaDigits(dateStringPay2)
        val resultsByBnDate = filterPayments(searchQuery = bnDateString)
        assertTrue("Should find pay-2 by Bengali date digits", resultsByBnDate.any { it.paymentNumber == "CP-2026-0002" })

        // Search by Year "2026"
        val yearResults = filterPayments(searchQuery = "2026")
        assertTrue(yearResults.size >= 3)
    }

    @Test
    fun testCombinedDateAndMethodAndCustomerSearch() {
        // Search customer Rahim + Date Today
        val rahimToday = filterPayments(
            searchQuery = "Rahim",
            selectedDateFilter = PaymentDateFilter.TODAY
        )
        assertEquals(1, rahimToday.size)
        assertEquals("CP-2026-0001", rahimToday[0].paymentNumber)

        // Search customer Rahim + Date Yesterday (should be empty because Rahim's payments were today and 3 days ago)
        val rahimYesterday = filterPayments(
            searchQuery = "Rahim",
            selectedDateFilter = PaymentDateFilter.YESTERDAY
        )
        assertEquals(0, rahimYesterday.size)

        // Search Bank method + Date Yesterday
        val bankYesterday = filterPayments(
            selectedMethodFilter = "BANK",
            selectedDateFilter = PaymentDateFilter.YESTERDAY
        )
        assertEquals(1, bankYesterday.size)
        assertEquals("CP-2026-0002", bankYesterday[0].paymentNumber)
    }

    @Test
    fun testTotalCollectedCalculation() {
        val allValid = filterPayments("")
        val total = allValid.filter { !it.isVoided }.sumOf { it.amountMinor }
        // 500000 + 1200000 + 300000 = 2000000 (20,000.00 BDT)
        assertEquals(2000000L, total)
    }
}
