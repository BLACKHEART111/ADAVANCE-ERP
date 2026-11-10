package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DueReminderStatementData(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerCode: String,
    val companyName: String,
    val companyPhone: String,
    val totalDueMinor: Long,
    val invoiceCount: Int = 0,
    val asOfDate: Long = System.currentTimeMillis(),
    val paymentInstructions: String? = null,
    val pdfFile: File? = null
)

object DueReminderHelper {

    fun generatePoliteReminderCaption(data: DueReminderStatementData, isBangla: Boolean): String {
        val dueFormatted = Formatters.formatMoney(data.totalDueMinor, CurrencyCode.BDT, if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH)
        val datePattern = if (isBangla) "dd MMMM, yyyy" else "dd MMM yyyy"
        val dateLocale = if (isBangla) Locale("bn", "BD") else Locale.ENGLISH
        val dateStr = SimpleDateFormat(datePattern, dateLocale).format(Date(data.asOfDate))

        return if (isBangla) {
            buildString {
                append("সম্মানিত ${data.customerName} সাহেব,\n")
                append("আসসালামু আলাইকুম। ${data.companyName}-এর পক্ষ থেকে আন্তরিক সালাম ও শুভেচ্ছা গ্রহণ করবেন। আশা করি মহান আল্লাহর অশেষ রহমতে আপনি সপরিবারে কুশলে আছেন।\n\n")
                append("📌 *বিনীত বকেয়া তথ্য ও হিসাব বিবরণী*\n")
                append("হিসাবের তারিখ: $dateStr\n")
                append("--------------------------------\n")
                append("• গ্রাহকের নাম: ${data.customerName}\n")
                append("• গ্রাহক কোড: ${data.customerCode}\n")
                append("• মোট চলতি বকেয়ার পরিমাণ: *$dueFormatted*\n")
                if (data.invoiceCount > 0) {
                    append("• অমীমাংসিত চালানের সংখ্যা: ${data.invoiceCount} টি\n")
                }
                append("--------------------------------\n\n")
                append("আপনার ব্যবসায়িক সুবিধার জন্য এবং হিসাব মিলকরণের সুবিধার্থে বিস্তারিত হালনাগাদ হিসাব বিবরণী (PDF Statement) এই বার্তার সাথে সংযুক্ত করা হয়েছে।\n\n")
                append("উক্ত বকেয়া টাকাটি অনুগ্রহপূর্বক আপনার সুবিধাজনক সময়ে পরিশোধের সদয় ব্যবস্থা গ্রহণ করার জন্য অত্যন্ত বিনীতভাবে অনুরোধ জানাচ্ছি।\n\n")
                if (!data.paymentInstructions.isNullOrBlank()) {
                    append("💳 পেমেন্ট মাধ্যম:\n${data.paymentInstructions}\n\n")
                }
                append("যদি ইতিমধ্যে আপনি এই বকেয়া পরিশোধ করে থাকেন, তবে অনুগ্রহপূর্বক এই বার্তাটি ক্ষমাসুন্দর দৃষ্টিতে দেখবেন এবং আমাদের অবহিত করবেন।\n\n")
                append("আমাদের সাথে আপনার মূল্যবান ব্যবসায়িক সম্পর্কের জন্য আমরা আন্তরিকভাবে কৃতজ্ঞ ও আনন্দিত। আপনার উত্তরোত্তর সাফল্য ও সুস্বাস্থ্য কামনা করছি।\n\n")
                append("ধন্যবাদান্তে ও সশ্রদ্ধ শুভেচ্ছান্তে,\n")
                append("*${data.companyName}*\n")
                if (data.companyPhone.isNotBlank()) {
                    append("📞 যোগাযোগ ও সহায়তা: ${data.companyPhone}")
                }
            }
        } else {
            buildString {
                append("Dear ${data.customerName},\n")
                append("Warm greetings and respectful regards from ${data.companyName}. We hope this message finds you well.\n\n")
                append("📌 *Courteous Account Statement & Due Reminder*\n")
                append("Statement Date: $dateStr\n")
                append("--------------------------------\n")
                append("• Customer Name: ${data.customerName}\n")
                append("• Customer ID: ${data.customerCode}\n")
                append("• Total Outstanding Due: *$dueFormatted*\n")
                if (data.invoiceCount > 0) {
                    append("• Pending Invoices: ${data.invoiceCount}\n")
                }
                append("--------------------------------\n\n")
                append("For your kind reference and ease of reconciliation, your detailed updated account statement (PDF) is attached herewith.\n\n")
                append("We kindly and respectfully request you to settle the outstanding dues at your earliest convenience.\n\n")
                if (!data.paymentInstructions.isNullOrBlank()) {
                    append("💳 Payment Details:\n${data.paymentInstructions}\n\n")
                }
                append("If payment has already been remitted recently, please accept our sincere apologies for this reminder and kindly let us know.\n\n")
                append("Thank you very much for your valued partnership and continued trust in ${data.companyName}.\n\n")
                append("Warmest regards,\n")
                append("*${data.companyName}*\n")
                if (data.companyPhone.isNotBlank()) {
                    append("📞 Support & Assistance: ${data.companyPhone}")
                }
            }
        }
    }
}
