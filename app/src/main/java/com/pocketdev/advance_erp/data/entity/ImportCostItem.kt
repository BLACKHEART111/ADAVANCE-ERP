package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pocketdev.advance_erp.core.engine.AllocationMethod

enum class CostCategory(
    val code: String,
    val titleEn: String,
    val titleBn: String,
    val defaultAllocationMethod: AllocationMethod,
    val isCapitalizable: Boolean = true
) {
    PRODUCT_PURCHASE("PRD_PURCHASE", "Product Purchase Cost", "পণ্য ক্রয় খরচ", AllocationMethod.PURCHASE_VALUE, true),
    CHINA_DOMESTIC_TRANSPORT("CN_DOMESTIC", "China Domestic Transport", "চীন অভ্যন্তরীণ পরিবহন", AllocationMethod.GROSS_WEIGHT, true),
    INTERNATIONAL_FREIGHT("INT_FREIGHT", "International Freight", "আন্তর্জাতিক ফ্রেইট (মালবাহী ভাড়া)", AllocationMethod.GROSS_WEIGHT, true),
    SEA_FREIGHT("SEA_FREIGHT", "Sea Freight", "সমুদ্রপথে মালবাহী ভাড়া", AllocationMethod.GROSS_WEIGHT, true),
    AIR_FREIGHT("AIR_FREIGHT", "Air Freight", "বিমানপথে মালবাহী ভাড়া", AllocationMethod.GROSS_WEIGHT, true),
    CUSTOMS_DUTY("CUSTOMS_DUTY", "Customs Duty & Tariff", "কাস্টমস শুল্ক ও ট্যারিফ", AllocationMethod.PURCHASE_VALUE, true),
    CUSTOMS_TAX_VAT("CUSTOMS_TAX_VAT", "Customs Tax / VAT", "কাস্টমস ট্যাক্স ও ভ্যাট", AllocationMethod.PURCHASE_VALUE, true),
    VAT_SUPPLEMENTARY_DUTY("VAT_SD", "VAT & Supplementary Duty", "ভ্যাট ও সম্পূরক শুল্ক", AllocationMethod.PURCHASE_VALUE, true),
    CF_CHARGES("CF_AGENCY", "C&F Clearing Agency Fees", "সিঅ্যান্ডএফ এজেন্ট ফি", AllocationMethod.QUANTITY, true),
    CF_CHARGE("CF_CHARGE", "C&F Charge", "সিঅ্যান্ডএফ চার্জ", AllocationMethod.QUANTITY, true),
    PORT_CHARGES("PORT_DEMURRAGE", "Port & Terminal Charges", "পোর্ট ও ডেমারেজ চার্জ", AllocationMethod.GROSS_WEIGHT, true),
    PORT_CHARGE("PORT_CHARGE", "Port Charge", "পোর্ট চার্জ", AllocationMethod.GROSS_WEIGHT, true),
    SHIPPING_LINE_CHARGES("SHIP_LINE", "Shipping Line Charges", "শিপিং লাইন চার্জ", AllocationMethod.CBM, true),
    SHIPPING_AGENT_CHARGE("SHIP_AGENT", "Shipping Agent Charge", "শিপিং এজেন্ট চার্জ", AllocationMethod.CBM, true),
    LOCAL_TRANSPORTATION("LOCAL_TRANS", "Local Transportation", "স্থানীয় পরিবহন", AllocationMethod.GROSS_WEIGHT, true),
    LOADING_UNLOADING("LOAD_UNLOAD", "Loading / Unloading", "লোডিং / আনলোডিং", AllocationMethod.QUANTITY, true),
    WAREHOUSE_CHARGES("WH_CHARGES", "Warehouse & Handling", "ওয়্যারহাউস ও হ্যান্ডলিং", AllocationMethod.QUANTITY, true),
    INSURANCE("INSURANCE", "Marine Cargo Insurance", "কার্গো বীমা প্রিমিয়াম", AllocationMethod.PURCHASE_VALUE, true),
    BANK_CHARGE("BANK_CHARGE", "Bank Charge", "ব্যাংক চার্জ", AllocationMethod.PURCHASE_VALUE, false),
    DOCUMENTATION_CHARGE("DOC_CHARGE", "Documentation Charge", "ডকুমেন্টেশন চার্জ", AllocationMethod.QUANTITY, true),
    OTHER_CHARGES("OTHER_CHARGES", "Other Charges", "অন্যান্য চার্জ", AllocationMethod.MANUAL, false),
    OTHER_EXPENSES("OTHER_EXP", "Other Import Expenses", "অন্যান্য আমদানি ব্যয়", AllocationMethod.MANUAL, false)
}

@Entity(
    tableName = "import_cost_items",
    foreignKeys = [
        ForeignKey(
            entity = ImportShipment::class,
            parentColumns = ["id"],
            childColumns = ["shipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("shipmentId"),
        Index("purchaseOrderId")
    ]
)
data class ImportCostItem(
    @PrimaryKey
    val id: String,
    val shipmentId: String,
    val costCategory: String = CostCategory.INTERNATIONAL_FREIGHT.name,
    val description: String,
    val amountMinor: Long,
    val currency: String = "BDT",
    val exchangeRateToBdt: String = "1.0000",
    val bdtAmountMinor: Long,
    val costDate: Long = System.currentTimeMillis(),
    val paymentAccount: String = "City Bank Corporate (Import LC)",
    val documentReference: String? = null,
    val allocationMethod: String = AllocationMethod.GROSS_WEIGHT.name,
    val isPosted: Boolean = false,
    val purchaseOrderId: String? = null,
    val isEstimated: Boolean = false,
    val isPaid: Boolean = false,
    val notes: String? = null,
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCapitalizableCost: Boolean
        get() = try {
            CostCategory.valueOf(costCategory).isCapitalizable
        } catch (e: Exception) {
            true
        }
}
