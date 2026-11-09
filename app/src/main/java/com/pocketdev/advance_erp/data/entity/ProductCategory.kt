package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_categories",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class ProductCategory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nameEn: String,
    val nameBn: String,
    val code: String,
    val description: String? = null
)
