package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PaymentSearchAndUniqueTrackingTest {

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
            isPosted = true,
            isVoided = true,
            voidReason = "Bounced Cheque"
        )
    )

    private val customersMap = customers.associateBy { it.id }

    private fun filterPayments(
        searchQuery: String,
        selectedMethodFilter: String = "ALL"
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
                matchesNumber || matchesCustName || matchesPhone || matchesRef
            }

            val matchesMethod = when (selectedMethodFilter) {
                "ALL" -> true
                "VOIDED" -> pay.isVoided
                "MFS" -> !pay.isVoided && (pay.paymentMethod.equals("BKASH", ignoreCase = true) || pay.paymentMethod.equals("NAGAD", ignoreCase = true))
                else -> !pay.isVoided && pay.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
            }

            matchesSearch && matchesMethod
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
        // Rahim's phone: 01711223344
        val results = filterPayments("01711223344")
        assertEquals(2, results.size)
        assertTrue(results.all { it.customerName == "Md. Rahim Uddin" })

        // Partial phone search: 8877
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
        val cashPayments = filterPayments(searchQuery = "", selectedMethodFilter = "CASH")
        assertEquals(1, cashPayments.size) // pay-1 is non-voided CASH, pay-4 is VOIDED
        assertEquals("CP-2026-0001", cashPayments[0].paymentNumber)

        val bankPayments = filterPayments(searchQuery = "", selectedMethodFilter = "BANK")
        assertEquals(1, bankPayments.size)
        assertEquals("CP-2026-0002", bankPayments[0].paymentNumber)

        val mfsPayments = filterPayments(searchQuery = "", selectedMethodFilter = "MFS")
        assertEquals(1, mfsPayments.size)
        assertEquals("CP-2026-0003", mfsPayments[0].paymentNumber)

        val voidedPayments = filterPayments(searchQuery = "", selectedMethodFilter = "VOIDED")
        assertEquals(1, voidedPayments.size)
        assertEquals("CP-2026-0004", voidedPayments[0].paymentNumber)
    }

    @Test
    fun testTotalCollectedCalculation() {
        val allValid = filterPayments("")
        val total = allValid.filter { !it.isVoided }.sumOf { it.amountMinor }
        // 500000 + 1200000 + 300000 = 2000000 (20,000.00 BDT)
        assertEquals(2000000L, total)
    }
}
