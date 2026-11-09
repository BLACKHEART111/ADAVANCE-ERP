package com.pocketdev.advance_erp.core.security

enum class UserRole(val code: String, val titleEn: String, val titleBn: String) {
    OWNER_ADMIN("OWNER_ADMIN", "Owner / Admin", "মালিক / প্রশাসক"),
    MANAGER("MANAGER", "Manager", "ব্যবস্থাপক"),
    SALES_OPERATOR("SALES_OPERATOR", "Sales Operator", "বিক্রয় কর্মী"),
    PURCHASE_OPERATOR("PURCHASE_OPERATOR", "Purchase Operator", "ক্রয় কর্মী"),
    ACCOUNTANT("ACCOUNTANT", "Accountant", "হিসাবরক্ষক"),
    INVENTORY_OPERATOR("INVENTORY_OPERATOR", "Inventory Operator", "ইনভেন্টরি কর্মী");

    companion object {
        fun fromCode(code: String): UserRole {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OWNER_ADMIN
        }
    }
}

object SecurityManager {
    /**
     * Confirmed Rule 3.4: Only Owner/Admin may access Gross Profit, Net Profit,
     * COGS, profit margins, product-wise profitability, and profit analytics.
     */
    fun canViewProfit(role: UserRole): Boolean {
        return role == UserRole.OWNER_ADMIN
    }

    fun canViewCost(role: UserRole): Boolean {
        return role == UserRole.OWNER_ADMIN || role == UserRole.MANAGER || role == UserRole.ACCOUNTANT || role == UserRole.PURCHASE_OPERATOR
    }

    fun canApproveCreditLimitOverride(role: UserRole): Boolean {
        return role == UserRole.OWNER_ADMIN || role == UserRole.MANAGER
    }

    fun canPerformSensitiveOperation(role: UserRole): Boolean {
        return role == UserRole.OWNER_ADMIN || role == UserRole.MANAGER
    }
}
