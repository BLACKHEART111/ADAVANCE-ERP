package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "warehouses",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class Warehouse(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String,
    val location: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
