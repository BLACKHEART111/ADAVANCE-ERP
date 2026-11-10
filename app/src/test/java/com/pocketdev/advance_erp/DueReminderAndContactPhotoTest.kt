package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.ContactPickerHelper
import com.pocketdev.advance_erp.core.util.DueReminderHelper
import com.pocketdev.advance_erp.core.util.DueReminderStatementData
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DueReminderAndContactPhotoTest {

    @Test
    fun testPoliteDueReminderCaptionBanglaToneAndDetails() {
        val data = DueReminderStatementData(
            customerId = "cust-101",
            customerName = "রহিম আহমেদ",
            customerPhone = "+8801711223344",
            customerCode = "CUST-00101",
            companyName = "এডভান্স ট্রেডার্স লিমিটেড",
            companyPhone = "+8801999888777",
            totalDueMinor = 7500000L, // ৳75,000.00
            invoiceCount = 2,
            asOfDate = 1731175200000L,
            paymentInstructions = "বিকাশ মার্চেন্ট: 01999888777 অথবা ব্র্যাক ব্যাংক একাউন্ট: 150120..."
        )

        val captionBn = DueReminderHelper.generatePoliteReminderCaption(data, isBangla = true)

        // Verify extremely polite, courteous Bengali tone
        assertTrue("Must address politely as সম্মানিত", captionBn.contains("সম্মানিত রহিম আহমেদ সাহেব"))
        assertTrue("Must include warm greetings", captionBn.contains("আসসালামু আলাইকুম"))
        assertTrue("Must include polite phrasing", captionBn.contains("বিনীত বকেয়া তথ্য"))
        assertTrue("Must include PDF attachment notice", captionBn.contains("হালনাগাদ হিসাব বিবরণী (PDF Statement) এই বার্তার সাথে সংযুক্ত করা হয়েছে"))
        assertTrue("Must make courteous request", captionBn.contains("অত্যন্ত বিনীতভাবে অনুরোধ"))
        assertTrue("Must include apology if already paid", captionBn.contains("ক্ষমাসুন্দর দৃষ্টিতে দেখবেন"))
        assertTrue("Must include respectful signoff", captionBn.contains("ধন্যবাদান্তে ও সশ্রদ্ধ শুভেচ্ছান্তে"))

        // Verify accurate financial data
        assertTrue("Must show BDT formatted due", captionBn.contains("৳") && captionBn.contains("৭৫,০০০"))
        assertTrue("Must show customer code", captionBn.contains("CUST-00101"))
        assertTrue("Must show company phone", captionBn.contains("+8801999888777"))
        assertTrue("Must show payment instructions", captionBn.contains("বিকাশ মার্চেন্ট: 01999888777"))
    }

    @Test
    fun testPoliteDueReminderCaptionEnglishToneAndDetails() {
        val data = DueReminderStatementData(
            customerId = "cust-102",
            customerName = "Tanvir Hasan",
            customerPhone = "01812345678",
            customerCode = "CUST-00102",
            companyName = "Advance Global Import",
            companyPhone = "+8801800000000",
            totalDueMinor = 1250000L, // ৳12,500.00
            invoiceCount = 1,
            paymentInstructions = "Bank Transfer to City Bank A/C: 1102938481"
        )

        val captionEn = DueReminderHelper.generatePoliteReminderCaption(data, isBangla = false)

        // Verify polite and respectful English tone
        assertTrue("Must start with Dear", captionEn.contains("Dear Tanvir Hasan"))
        assertTrue("Must include warm greetings", captionEn.contains("Warm greetings and respectful regards"))
        assertTrue("Must state courteous reminder", captionEn.contains("Courteous Account Statement & Due Reminder"))
        assertTrue("Must include PDF notice", captionEn.contains("updated account statement (PDF) is attached herewith"))
        assertTrue("Must request politely", captionEn.contains("kindly and respectfully request you to settle the outstanding dues"))
        assertTrue("Must include apology if already remitted", captionEn.contains("please accept our sincere apologies"))
        assertTrue("Must include warm regards", captionEn.contains("Warmest regards"))

        // Verify financial figures
        assertTrue("Must show formatted currency", captionEn.contains("12,500"))
        assertTrue("Must show customer ID", captionEn.contains("CUST-00102"))
        assertTrue("Must show support phone", captionEn.contains("+8801800000000"))
    }

    @Test
    fun testContactPickerPhoneNormalization() {
        // Test various contact phone formats
        val phoneWithSpaces = "+880 1711 223 344"
        val phoneWithHyphens = "+880-1811-223344"
        val phoneWithParentheses = "(01711) 223344"
        val mixedPhone = "+880 (171) 122-3344"

        assertEquals("+8801711223344", ContactPickerHelper.normalizePhoneNumber(phoneWithSpaces))
        assertEquals("+8801811223344", ContactPickerHelper.normalizePhoneNumber(phoneWithHyphens))
        assertEquals("01711223344", ContactPickerHelper.normalizePhoneNumber(phoneWithParentheses))
        assertEquals("+8801711223344", ContactPickerHelper.normalizePhoneNumber(mixedPhone))
        assertEquals(null, ContactPickerHelper.normalizePhoneNumber(""))
        assertEquals(null, ContactPickerHelper.normalizePhoneNumber(null))
    }

    @Test
    fun testCustomerProfilePhotoFieldSupport() {
        val customerWithPhoto = Customer(
            id = "cust-photo-01",
            customerCode = "CUST-P01",
            name = "হুমায়ুন কবির",
            phone = "01700112233",
            address = "Dhanmondi, Dhaka",
            profileImagePath = "/data/user/0/app/files/customer_photos/cust_01.jpg"
        )

        assertNotNull(customerWithPhoto.profileImagePath)
        assertEquals("/data/user/0/app/files/customer_photos/cust_01.jpg", customerWithPhoto.profileImagePath)

        val customerWithoutPhoto = Customer(
            id = "cust-photo-02",
            customerCode = "CUST-P02",
            name = "কামাল হোসেন",
            phone = "01800112233",
            address = "Uttara, Dhaka"
        )
        assertEquals(null, customerWithoutPhoto.profileImagePath)
    }

    @Test
    fun testBusinessProfileCompanyLogoSupport() {
        val profile = BusinessProfile(
            id = "default_profile",
            companyName = "Advance Enterprise",
            phone = "+8801700000000",
            address = "Gulshan-1, Dhaka",
            logoPath = "/data/user/0/app/files/company_logo/logo.png"
        )

        assertNotNull(profile.logoPath)
        assertEquals("/data/user/0/app/files/company_logo/logo.png", profile.logoPath)
    }
}
