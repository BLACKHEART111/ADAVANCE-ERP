package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.LandedCostAllocation
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportCostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostItem(item: ImportCostItem)

    @Query("DELETE FROM import_cost_items WHERE id = :costId")
    suspend fun deleteCostItem(costId: String)

    @Query("SELECT * FROM import_cost_items WHERE shipmentId = :shipmentId ORDER BY costDate ASC")
    fun getCostItemsForShipment(shipmentId: String): Flow<List<ImportCostItem>>

    @Query("SELECT * FROM import_cost_items WHERE shipmentId = :shipmentId ORDER BY costDate ASC")
    suspend fun getCostItemsForShipmentSync(shipmentId: String): List<ImportCostItem>

    @Query("SELECT * FROM import_cost_items WHERE purchaseOrderId = :orderId ORDER BY costDate ASC")
    fun getCostItemsForOrder(orderId: String): Flow<List<ImportCostItem>>

    @Query("SELECT * FROM import_cost_items WHERE purchaseOrderId = :orderId ORDER BY costDate ASC")
    suspend fun getCostItemsForOrderSync(orderId: String): List<ImportCostItem>

    @Query("SELECT * FROM import_cost_items WHERE purchaseOrderId = :orderId AND isEstimated = 0 ORDER BY costDate ASC")
    suspend fun getActualCostItemsForOrderSync(orderId: String): List<ImportCostItem>

    @Query("SELECT * FROM import_cost_items WHERE purchaseOrderId = :orderId AND isEstimated = 1 ORDER BY costDate ASC")
    suspend fun getEstimatedCostItemsForOrderSync(orderId: String): List<ImportCostItem>

    @Query("SELECT SUM(bdtAmountMinor) FROM import_cost_items WHERE shipmentId = :shipmentId")
    suspend fun getTotalCostBdtMinorForShipment(shipmentId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<LandedCostAllocation>)

    @Query("DELETE FROM landed_cost_allocations WHERE shipmentId = :shipmentId")
    suspend fun deleteAllocationsForShipment(shipmentId: String)

    @Query("SELECT * FROM landed_cost_allocations WHERE shipmentId = :shipmentId")
    fun getAllocationsForShipment(shipmentId: String): Flow<List<LandedCostAllocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<LandedCostSnapshot>)

    @Query("DELETE FROM landed_cost_snapshots WHERE shipmentId = :shipmentId")
    suspend fun deleteSnapshotsForShipment(shipmentId: String)

    @Query("SELECT * FROM landed_cost_snapshots WHERE shipmentId = :shipmentId")
    fun getSnapshotsForShipment(shipmentId: String): Flow<List<LandedCostSnapshot>>

    @Query("SELECT * FROM landed_cost_snapshots WHERE shipmentId = :shipmentId")
    suspend fun getSnapshotsForShipmentSync(shipmentId: String): List<LandedCostSnapshot>

    @Transaction
    suspend fun replaceAllocationsAndSnapshots(
        shipmentId: String,
        allocations: List<LandedCostAllocation>,
        snapshots: List<LandedCostSnapshot>
    ) {
        deleteAllocationsForShipment(shipmentId)
        deleteSnapshotsForShipment(shipmentId)
        insertAllocations(allocations)
        insertSnapshots(snapshots)
    }
}
