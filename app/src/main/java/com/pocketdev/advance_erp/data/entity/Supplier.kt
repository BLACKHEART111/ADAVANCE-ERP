package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["supplierCode"], unique = true),
        Index(value = ["country"])
    ]
)
data class Supplier(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val supplierCode: String,
    val companyName: String,
    val contactPerson: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val country: String = "China",
    val currency: String = "CNY",
    val outstandingPayableMinor: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
