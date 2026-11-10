package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.model.Money
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DueCollectionStatementData(
    val customerId: String,
    val customerName: String,
    val customerPhone: String?,
    val customerCode: String,
    val companyName: String,
    val companyPhone: String,
    val paymentNumber: String,
    val paymentDate: Long,
    val paymentMethod: String,
    val amountReceivedMinor: Long,
    val previousDueMinor: Long,
    val currentDueMinor: Long,
    val isFullPayment: Boolean,
    val isPartialPayment: Boolean,
    val isAdvancePayment: Boolean,
    val advanceAmountMinor: Long = 0L,
    val referenceNote: String? = null,
    val statementPdfFile: File? = null
)

object DueCollectionCaptionHelper {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    /**
     * Generates a clean, professional, and friendly WhatsApp caption.
     * Supports both Bengali (বাংলা) and English.
     */
    fun generateCaption(data: DueCollectionStatementData, isBangla: Boolean): String {
        val dateStr = dateFormat.format(Date(data.paymentDate))
        val timeStr = timeFormat.format(Date(data.paymentDate))
        val amountReceivedStr = "৳" + Formatters.formatMinorUnit(data.amountReceivedMinor)
        val previousDueStr = "৳" + Formatters.formatMinorUnit(data.previousDueMinor)
        val currentDueStr = "৳" + Formatters.formatMinorUnit(maxOf(0L, data.currentDueMinor))
        val advanceStr = "৳" + Formatters.formatMinorUnit(data.advanceAmountMinor)

        val company = data.companyName.ifBlank { "ADVANCE ERP" }
        val carePhone = data.companyPhone.ifBlank { "01700-000000" }

        return if (isBangla) {
            buildString {
                append("🧾 *পেমেন্ট প্রাপ্তি স্বীকার ও হিসাব বিবরণী*\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("👤 *সম্মানিত গ্রাহক:* ${data.customerName}\n")
                if (!data.customerCode.isNullOrBlank()) {
                    append("🆔 *গ্রাহক আইডি:* ${data.customerCode}\n")
                }
                append("🏢 *প্রতিষ্ঠান:* $company\n")
                append("📞 *কাস্টমার কেয়ার:* $carePhone\n\n")

                append("📋 *লেনদেন বিবরণ:*\n")
                append("• রসিদ নম্বর: #${data.paymentNumber}\n")
                append("• তারিখ ও সময়: $dateStr, $timeStr\n")
                append("• পেমেন্ট মেথড: ${translatePaymentMethodBn(data.paymentMethod)}\n")
                if (!data.referenceNote.isNullOrBlank()) {
                    append("• রেফারেন্স: ${data.referenceNote}\n")
                }
                append("────────────────────────\n")
                append("💰 *পূর্বের মোট বকেয়া:* $previousDueStr\n")
                append("📥 *আজকের জমার পরিমাণ:* $amountReceivedStr\n")
                append("⚖️ *বর্তমান অবশিষ্ট বকেয়া:* $currentDueStr\n")

                when {
                    data.isAdvancePayment && data.advanceAmountMinor > 0L -> {
                        append("⭐ *অ্যাডভান্স ব্যালেন্স:* $advanceStr\n")
                        append("────────────────────────\n")
                        append("✨ *ধন্যবাদ! আপনার অতিরিক্ত জমা $advanceStr আপনার একাউন্ট ব্যালেন্সে অগ্রিম (Customer Advance) হিসেবে জমা রয়েছে।*\n")
                    }
                    data.isFullPayment -> {
                        append("────────────────────────\n")
                        append("✅ *অভিনন্দন! আপনার পূর্বের সকল বকেয়া সম্পূর্ণ পরিশোধ হয়েছে। বর্তমান বকেয়া ৳০.০০।*\n")
                    }
                    data.currentDueMinor == 0L -> {
                        append("────────────────────────\n")
                        append("✅ *আপনার কোনো বকেয়া অবশিষ্ট নেই (জিরো ডিউ)।*\n")
                    }
                    else -> {
                        append("────────────────────────\n")
                        append("⚠️ *অবশিষ্ট বকেয়া:* $currentDueStr\n")
                        append("পরবর্তী নির্ধারিত তারিখের মধ্যে অবশিষ্ট বকেয়া পরিশোধ করার জন্য বিনীত অনুরোধ করা হলো।\n")
                    }
                }

                append("\n📎 *হালনাগাদ সম্পূর্ণ লেজার স্টেটমেন্ট (PDF) সাথে সংযুক্ত করা হয়েছে।*\n\n")
                append("আমাদের ওপর আস্থা রাখার জন্য আপনাকে আন্তরিক ধন্যবাদ।\n")
                append("— *$company*")
            }
        } else {
            buildString {
                append("🧾 *Payment Receipt & Account Statement*\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("👤 *Dear Customer:* ${data.customerName}\n")
                if (!data.customerCode.isNullOrBlank()) {
                    append("🆔 *Customer ID:* ${data.customerCode}\n")
                }
                append("🏢 *Company:* $company\n")
                append("📞 *Customer Care:* $carePhone\n\n")

                append("📋 *Transaction Details:*\n")
                append("• Receipt No: #${data.paymentNumber}\n")
                append("• Date & Time: $dateStr, $timeStr\n")
                append("• Payment Method: ${data.paymentMethod}\n")
                if (!data.referenceNote.isNullOrBlank()) {
                    append("• Reference: ${data.referenceNote}\n")
                }
                append("────────────────────────\n")
                append("💰 *Previous Total Due:* $previousDueStr\n")
                append("📥 *Amount Received Today:* $amountReceivedStr\n")
                append("⚖️ *Current Outstanding Due:* $currentDueStr\n")

                when {
                    data.isAdvancePayment && data.advanceAmountMinor > 0L -> {
                        append("⭐ *Advance Credit Balance:* $advanceStr\n")
                        append("────────────────────────\n")
                        append("✨ *Thank you! Excess payment of $advanceStr is held securely as an advance credit on your account.*\n")
                    }
                    data.isFullPayment -> {
                        append("────────────────────────\n")
                        append("✅ *Congratulations! All outstanding dues have been fully settled. Current Due: ৳0.00.*\n")
                    }
                    data.currentDueMinor == 0L -> {
                        append("────────────────────────\n")
                        append("✅ *You currently have Zero Due (৳0.00).*\n")
                    }
                    else -> {
                        append("────────────────────────\n")
                        append("⚠️ *Remaining Outstanding Balance:* $currentDueStr\n")
                        append("Kindly arrange payment for the remaining balance at your earliest convenience.\n")
                    }
                }

                append("\n📎 *Your updated complete ledger statement (PDF) is attached.* \n\n")
                append("Thank you for choosing to do business with us!\n")
                append("— *$company*")
            }
        }
    }

    private fun translatePaymentMethodBn(method: String): String {
        return when (method.uppercase()) {
            "CASH" -> "নগদ (Cash)"
            "BANK" -> "ব্যাংক ট্রান্সফার (Bank)"
            "BKASH" -> "বিকাশ (bKash)"
            "NAGAD" -> "নগদ (Nagad)"
            "ROCKET" -> "রকেট (Rocket)"
            "CHEQUE" -> "চেক (Cheque)"
            else -> method
        }
    }
}
