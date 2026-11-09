package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profiles")
data class BusinessProfile(
    @PrimaryKey
    val id: String = "default_profile",
    val companyName: String,
    val taxNumber: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val defaultCurrency: String = "BDT",
    val updatedAt: Long = System.currentTimeMillis()
)
