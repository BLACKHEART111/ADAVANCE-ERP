package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfInvoiceDataTest {

    @Test
    fun testPdfInvoiceDataFormatting() {
        val invoiceNumber = "INV-20261102-1234"
        val customerName = "Rahim Enterprise"
        val timestamp = 1730550000000L
        val subtotalMinor = 15000000L // ৳150,000.00
        val discountMinor = 500000L   // ৳5,000.00
        val grandTotalMinor = subtotalMinor - discountMinor // ৳145,000.00
        val paidAmountMinor = 10000000L // ৳100,000.00
        val dueAmountMinor = grandTotalMinor - paidAmountMinor // ৳45,000.00

        val formattedSubtotal = Formatters.formatMinorUnit(subtotalMinor)
        val formattedDiscount = Formatters.formatMinorUnit(discountMinor)
        val formattedGrandTotal = Formatters.formatMinorUnit(grandTotalMinor)
        val formattedPaid = Formatters.formatMinorUnit(paidAmountMinor)
        val formattedDue = Formatters.formatMinorUnit(dueAmountMinor)

        assertEquals("150,000.00", formattedSubtotal)
        assertEquals("5,000.00", formattedDiscount)
        assertEquals("145,000.00", formattedGrandTotal)
        assertEquals("100,000.00", formattedPaid)
        assertEquals("45,000.00", formattedDue)

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val formattedDate = dateFormat.format(Date(timestamp))
        assertTrue(formattedDate.isNotEmpty())
    }

    @Test
    fun testProductNameTruncationForA4Table() {
        val longProductName = "Premium 4K Smart Ultra-HD Android Television Set 55 Inch OLED"
        val truncatedName = if (longProductName.length > 32) longProductName.take(30) + ".." else longProductName

        assertTrue(truncatedName.length <= 32)
        assertEquals(32, truncatedName.length)
        assertEquals("Premium 4K Smart Ultra-HD Andr..", truncatedName)
    }
}
