package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.Warehouse
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {

    @Query("SELECT * FROM warehouses WHERE isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE isActive = 1 ORDER BY isDefault DESC, name ASC")
    suspend fun getAllWarehousesSync(): List<Warehouse>

    @Query("SELECT * FROM warehouses WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWarehouse(): Warehouse?

    @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1")
    suspend fun getWarehouseById(id: String): Warehouse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<Warehouse>)

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse)
}
