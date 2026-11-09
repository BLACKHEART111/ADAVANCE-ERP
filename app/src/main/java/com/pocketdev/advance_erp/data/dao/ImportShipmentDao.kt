package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportShipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ImportShipment)

    @Update
    suspend fun updateShipment(shipment: ImportShipment)

    @Query("DELETE FROM import_shipments WHERE id = :shipmentId")
    suspend fun deleteShipment(shipmentId: String)

    @Query("SELECT * FROM import_shipments ORDER BY createdAt DESC")
    fun getAllShipments(): Flow<List<ImportShipment>>

    @Query("SELECT * FROM import_shipments WHERE id = :shipmentId")
    suspend fun getShipmentById(shipmentId: String): ImportShipment?

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId ORDER BY shippingDate DESC")
    fun getShipmentsForOrder(orderId: String): Flow<List<ImportShipment>>

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId ORDER BY shippingDate DESC")
    suspend fun getShipmentsByOrderIdSync(orderId: String): List<ImportShipment>

    @Query("SELECT * FROM import_shipments WHERE shipmentNumber = :shipmentNumber")
    suspend fun getShipmentByNumber(shipmentNumber: String): ImportShipment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarton(carton: ShipmentCarton)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartons(cartons: List<ShipmentCarton>)

    @Query("DELETE FROM shipment_cartons WHERE shipmentId = :shipmentId")
    suspend fun deleteCartonsForShipment(shipmentId: String)

    @Query("DELETE FROM shipment_cartons WHERE id = :cartonId")
    suspend fun deleteCartonById(cartonId: String)

    @Query("DELETE FROM shipment_cartons WHERE purchaseOrderId = :orderId AND cartonNumber = :cartonNumber")
    suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String)

    @Query("SELECT * FROM shipment_cartons WHERE shipmentId = :shipmentId ORDER BY cartonNumber ASC")
    fun getCartonsForShipment(shipmentId: String): Flow<List<ShipmentCarton>>

    @Query("SELECT * FROM shipment_cartons WHERE shipmentId = :shipmentId ORDER BY cartonNumber ASC")
    suspend fun getCartonsForShipmentSync(shipmentId: String): List<ShipmentCarton>

    @Query("SELECT * FROM shipment_cartons WHERE purchaseOrderId = :orderId ORDER BY cartonNumber ASC")
    fun getCartonsForOrder(orderId: String): Flow<List<ShipmentCarton>>

    @Query("SELECT * FROM shipment_cartons WHERE purchaseOrderId = :orderId ORDER BY cartonNumber ASC")
    suspend fun getCartonsForOrderSync(orderId: String): List<ShipmentCarton>

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId LIMIT 1")
    suspend fun getBatchShipmentForOrder(orderId: String): ImportShipment?

    @Query("SELECT COUNT(*) FROM import_shipments")
    suspend fun getShipmentCount(): Int

    @Query("UPDATE import_shipments SET status = :status, actualArrivalDate = :arrivalDate, updatedAt = :updatedAt WHERE id = :shipmentId")
    suspend fun updateShipmentStatus(
        shipmentId: String,
        status: String,
        arrivalDate: Long? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Transaction
    suspend fun saveShipmentWithCartons(shipment: ImportShipment, cartons: List<ShipmentCarton>) {
        insertShipment(shipment)
        deleteCartonsForShipment(shipment.id)
        insertCartons(cartons)
    }
}
