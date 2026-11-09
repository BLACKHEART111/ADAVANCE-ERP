package com.pocketdev.advance_erp.core.model

import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val locale: Locale) {
    BANGLA("bn", "বাংলা", Locale("bn", "BD")),
    ENGLISH("en", "English", Locale("en", "US"));

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BANGLA
        }
    }
}
