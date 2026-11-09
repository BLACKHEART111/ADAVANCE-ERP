package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {

    @Query("SELECT * FROM business_profiles WHERE id = 'default_profile' LIMIT 1")
    fun getProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profiles WHERE id = 'default_profile' LIMIT 1")
    suspend fun getProfileSync(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BusinessProfile)
}
