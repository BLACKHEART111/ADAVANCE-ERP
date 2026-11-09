package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = ProductCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["baseSku"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["qrCode"])
    ]
)
data class Product(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val nameEn: String,
    val nameBn: String,
    val baseSku: String,
    val barcode: String? = null,
    val qrCode: String? = null,
    val description: String? = null,
    val unit: String = "PCS",
    val purchasePriceMinor: Long,
    val sellingPriceMinor: Long,
    val minStockAlert: Int = 10,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
