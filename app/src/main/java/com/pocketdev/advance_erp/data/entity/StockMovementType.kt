package com.pocketdev.advance_erp.data.entity

/**
 * Standardized stock movement types for ADVANCE ERP inventory tracking.
 * Ensures consistent movement classification, proper directional styling,
 * audit history integrity, and seamless backward compatibility with legacy raw strings.
 */
enum class StockMovementType(
    val code: String,
    val titleEn: String,
    val titleBn: String,
    val isIncoming: Boolean
) {
    PURCHASE_RECEIPT("PURCHASE_RECEIPT", "Purchase Receipt", "ক্রয় গ্রহণ", true),
    SALE_DISPATCH("SALE_DISPATCH", "Sales Dispatch", "বিক্রয় বিতরণ", false),
    SALE_RETURN("SALE_RETURN", "Sales Return", "বিক্রয় ফেরত গ্রহণ", true),
    PURCHASE_RETURN("PURCHASE_RETURN", "Purchase Return", "ক্রয় ফেরত প্রদান", false),
    STOCK_ADJUSTMENT_IN("STOCK_ADJUSTMENT_IN", "Stock Adjustment (+)", "স্টক সমন্বয় বৃদ্ধি (+)", true),
    STOCK_ADJUSTMENT_OUT("STOCK_ADJUSTMENT_OUT", "Stock Adjustment (-)", "স্টক সমন্বয় হ্রাস (-)", false),
    TRANSFER_OUT("TRANSFER_OUT", "Transfer Out", "স্থানান্তর প্রেরণ", false),
    TRANSFER_IN("TRANSFER_IN", "Transfer In", "স্থানান্তর গ্রহণ", true),
    DAMAGE_WRITE_OFF("DAMAGE_WRITE_OFF", "Damage Write-Off", "নষ্ট পণ্য অবলোপন", false),
    SHORTAGE("SHORTAGE", "Shortage Discrepancy", "ঘাটতি অসঙ্গতি", false),
    OPENING_STOCK("OPENING_STOCK", "Opening Stock", "প্রারম্ভিক মজুদ", true),
    INVENTORY_COST_ADJUSTMENT("INVENTORY_COST_ADJUSTMENT", "Inventory Cost Adjustment", "মজুদ ব্যয় সমন্বয়", true);

    companion object {
        fun fromCode(code: String): StockMovementType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: when (code.uppercase()) {
                    "SALE" -> SALE_DISPATCH
                    "ADJUSTMENT_INCREASE" -> STOCK_ADJUSTMENT_IN
                    "ADJUSTMENT_DECREASE" -> STOCK_ADJUSTMENT_OUT
                    "IMPORT_RECEIPT", "PURCHASE_RECEIVE" -> PURCHASE_RECEIPT
                    "SHORTAGE" -> SHORTAGE
                    "OPENING_STOCK" -> OPENING_STOCK
                    "INVENTORY_COST_ADJUSTMENT" -> INVENTORY_COST_ADJUSTMENT
                    "DAMAGE_EXCLUDED", "MISSING_IN_TRANSIT", "SHORTAGE_DISCREPANCY" -> DAMAGE_WRITE_OFF
                    else -> STOCK_ADJUSTMENT_IN
                }
        }
    }
}
