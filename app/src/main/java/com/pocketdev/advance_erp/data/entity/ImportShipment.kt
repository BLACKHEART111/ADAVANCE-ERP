package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ShipmentStatus(val labelEn: String, val labelBn: String) {
    DRAFT("Draft", "খসড়া"),
    BOOKED("Booked / Dispatched", "বুকিংকৃত / কারখানা ত্যাগ"),
    IN_TRANSIT("In Transit (Air/Sea)", "পথিমধ্যে রয়েছে"),
    CUSTOMS("Customs Clearance", "কাস্টমস ছাড়পত্র"),
    ARRIVED("Arrived at Port", "পোর্টে পৌঁছেছে"),
    RECEIVED("Received at Warehouse", "ওয়্যারহাউসে গৃহীত"),
    COMPLETED("Completed & Costed", "সম্পন্ন ও ব্যয় বণ্টনকৃত"),
    CANCELLED("Cancelled", "বাতিল")
}

enum class ShippingMethod(val labelEn: String, val labelBn: String) {
    AIR("Air Cargo", "এয়ার কার্গো (বিমান)"),
    SEA("Sea Freight (FCL/LCL)", "সমুদ্রপথ (জাহাজ)"),
    OTHER("Land / Express Courier", "ল্যান্ড / এক্সপ্রেস কুরিয়ার")
}

@Entity(
    tableName = "import_shipments",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PurchaseOrder::class,
            parentColumns = ["id"],
            childColumns = ["purchaseOrderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("supplierId"),
        Index("purchaseOrderId"),
        Index(value = ["shipmentNumber"], unique = true)
    ]
)
data class ImportShipment(
    @PrimaryKey
    val id: String,
    val shipmentNumber: String,
    val supplierId: String,
    val purchaseOrderId: String? = null,
    val shippingMethod: String = ShippingMethod.SEA.name,
    val origin: String = "Shenzhen, China",
    val destination: String = "Chattogram Port, Bangladesh",
    val trackingReference: String? = null,
    val shippingLine: String? = null,
    val shippingDate: Long = System.currentTimeMillis(),
    val estimatedArrivalDate: Long = System.currentTimeMillis() + (14L * 24 * 3600 * 1000),
    val actualArrivalDate: Long? = null,
    val status: String = ShipmentStatus.DRAFT.name,
    val totalCartons: Int = 0,
    val totalQuantity: Int = 0,
    val totalGrossWeightKg: String = "0.000",
    val totalNetWeightKg: String = "0.000",
    val totalCbm: String = "0.000000",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shipment_cartons",
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
        ),
        ForeignKey(
            entity = ProductVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("shipmentId"),
        Index("productId"),
        Index("variantId"),
        Index("purchaseOrderId")
    ]
)
data class ShipmentCarton(
    @PrimaryKey
    val id: String,
    val shipmentId: String,
    val cartonNumber: String,
    val productId: String,
    val variantId: String? = null,
    val quantity: Int,
    val lengthCm: String = "50.0",
    val widthCm: String = "40.0",
    val heightCm: String = "30.0",
    val grossWeightKg: String = "15.0",
    val netWeightKg: String = "14.0",
    val cbm: String = "0.060000",
    val purchaseOrderId: String? = null,
    val isMixedCarton: Boolean = false,
    val totalCartonQuantity: Int = 0,
    val cartonGrossWeightKg: String = "0.000"
)
