package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_balances",
    foreignKeys = [
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["warehouseId"]),
        Index(value = ["productId"]),
        Index(value = ["variantId"]),
        Index(value = ["warehouseId", "productId", "variantId"], unique = true)
    ]
)
data class StockBalance(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val warehouseId: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int = 0,
    val reservedQuantity: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
