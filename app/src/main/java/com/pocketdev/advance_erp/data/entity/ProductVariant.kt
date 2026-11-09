package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["qrCode"])
    ]
)
data class ProductVariant(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val sku: String,
    val color: String? = null,
    val size: String? = null,
    val variantPriceMinor: Long,
    val variantCostMinor: Long,
    val barcode: String? = null,
    val qrCode: String? = null,
    val isActive: Boolean = true
)
