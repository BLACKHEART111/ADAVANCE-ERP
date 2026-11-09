package com.pocketdev.advance_erp.core.model

enum class CurrencyCode(val code: String, val symbol: String, val displayNameEn: String, val displayNameBn: String) {
    BDT("BDT", "৳", "Bangladeshi Taka", "বাংলাদেশি টাকা"),
    CNY("CNY", "¥", "Chinese Yuan", "চাইনিজ ইউয়ান"),
    USD("USD", "$", "US Dollar", "ইউএস ডলার");

    companion object {
        fun fromCode(code: String?): CurrencyCode {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BDT
        }
    }
}
