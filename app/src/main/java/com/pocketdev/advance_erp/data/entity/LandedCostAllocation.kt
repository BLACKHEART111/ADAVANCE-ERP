package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "landed_cost_allocations",
    foreignKeys = [
        ForeignKey(
            entity = ImportShipment::class,
            parentColumns = ["id"],
            childColumns = ["shipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ImportCostItem::class,
            parentColumns = ["id"],
            childColumns = ["costItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("shipmentId"),
        Index("costItemId"),
        Index("productId")
    ]
)
data class LandedCostAllocation(
    @PrimaryKey
    val id: String,
    val shipmentId: String,
    val costItemId: String,
    val productId: String,
    val variantId: String? = null,
    val allocationBasis: String,
    val basisValue: String,
    val basisPercentage: String,
    val allocatedBdtMinor: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "landed_cost_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ImportShipment::class,
            parentColumns = ["id"],
            childColumns = ["shipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("shipmentId"),
        Index("productId")
    ]
)
data class LandedCostSnapshot(
    @PrimaryKey
    val id: String,
    val shipmentId: String,
    val productId: String,
    val variantId: String? = null,
    val productNameEn: String,
    val productNameBn: String,
    val sku: String,
    val totalReceivedQuantity: Int,
    val totalPurchaseCostBdtMinor: Long,
    val totalAllocatedCostsBdtMinor: Long,
    val totalLandedCostBdtMinor: Long,
    val unitLandedCostBdtMinor: Long,
    val snapshotDate: Long = System.currentTimeMillis(),
    val isFinal: Boolean = true
)
