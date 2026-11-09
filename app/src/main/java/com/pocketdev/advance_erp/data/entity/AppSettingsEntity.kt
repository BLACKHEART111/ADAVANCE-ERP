package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val languageCode: String = "bn",
    val currencyCode: String = "BDT",
    val isDarkMode: Boolean = false,
    val lastSyncTime: Long = 0L,
    val dbVersion: Int = 1
)
