package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AdditionalCostCategory(val titleEn: String, val titleBn: String, val isCapitalizableDefault: Boolean) {
    WAREHOUSE_RENT("Warehouse Rent", "ওয়্যারহাউস ভাড়া", false),
    WAREHOUSE_TRANSFER_RENT("Warehouse Transfer Transport", "ওয়্যারহাউস স্থানান্তর পরিবহন", true),
    LOADING_UNLOADING("Loading / Unloading", "লোডিং / আনলোডিং", true),
    REPACKING("Repacking", "রিপ্যাকিং", true),
    PACKAGING("Packaging", "প্যাকেজিং সামগ্রী", true),
    HANDLING("Handling", "হ্যান্ডলিং চার্জ", true),
    OTHER_OPERATIONAL("Other Operational Cost", "অন্যান্য পরিচালন ব্যয়", false)
}

@Entity(
    tableName = "additional_product_costs",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("purchaseOrderId"),
        Index("warehouseId"),
        Index(value = ["costNumber"], unique = true)
    ]
)
data class AdditionalProductCost(
    @PrimaryKey
    val id: String,
    val costNumber: String,
    val purchaseOrderId: String,
    val warehouseId: String? = null,
    val category: String = AdditionalCostCategory.HANDLING.name,
    val description: String,
    val amountMinor: Long,
    val currency: String = "BDT",
    val exchangeRateToBdt: String = "1.0000",
    val bdtAmountMinor: Long,
    val isCapitalizable: Boolean = true,
    val allocationMethod: String = "QUANTITY",
    val allocatedToInventoryMinor: Long = 0L,
    val allocatedToCogsMinor: Long = 0L,
    val expenseDate: Long = System.currentTimeMillis(),
    val journalEntryId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
