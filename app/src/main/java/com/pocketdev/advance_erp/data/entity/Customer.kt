package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index(value = ["phone"])
    ]
)
data class Customer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerCode: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String,
    val creditLimitMinor: Long = 0L,
    val outstandingBalanceMinor: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
