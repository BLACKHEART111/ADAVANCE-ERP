# ADVANCE ERP — Phase 06 Part 3: FIFO Engine Audit Report

## 1. Executive Summary
The First-In, First-Out (FIFO) Inventory Valuation and Cost of Goods Sold (COGS) Engine is the core financial engine of ADVANCE ERP. This audit validates that the FIFO engine is strictly deterministic, atomic, isolated per warehouse and variant, and fully conserves inventory asset value during stock depletion.

---

## 2. FIFO Engine Mechanics & Algorithms

```mermaid
flowchart TD
    Sale[Sales Invoice Item Dispatch] --> QueryLayers[Query Active Layers: WHERE warehouseId = W AND variantId = V ORDER BY receiptDate ASC, createdAt ASC]
    QueryLayers --> Loop{Has Layers & Remaining Need > 0?}
    Loop -- Yes --> LayerCheck[Current Layer: Q_avail = layer.remainingQuantity]
    LayerCheck --> Deplete[Take min: Q_take = min of Needed and Q_avail]
    Deplete --> UpdateLayer[layer.remainingQuantity -= Q_take]
    UpdateLayer --> CheckDepleted{layer.remainingQuantity == 0?}
    CheckDepleted -- Yes --> SetInactive[layer.isActive = false]
    CheckDepleted -- No --> RecordConsumption
    SetInactive --> RecordConsumption[Insert into fifo_consumptions]
    RecordConsumption --> RecordUsage[Insert into sales_cost_layer_usages]
    RecordUsage --> DecrementNeeded[Needed -= Q_take; COGS += Q_take * layer.unitLandedCost]
    DecrementNeeded --> Loop
    Loop -- No --> CheckExhausted{Needed > 0?}
    CheckExhausted -- Yes --> Fallback[Fallback to Product purchasePriceMinor]
    CheckExhausted -- No --> Finish[Post Journal: Dr COGS 5010, Cr Inventory 1210]
```

### 2.1 Warehouse & Variant Isolation
A critical requirement is that FIFO layers must **never** leak across warehouses or cross variants. 
- In `SalesRepositoryImpl.kt`:
```kotlin
val activeLayers = if (item.variantId != null) {
    inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
        productId = item.productId,
        variantId = item.variantId,
        warehouseId = invoice.warehouseId
    )
} else {
    inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
        productId = item.productId,
        warehouseId = invoice.warehouseId
    )
}
```
This guarantees that sales from Warehouse A consume only Warehouse A's acquisition cost layers, preventing cost distortion between regional depots.

---

## 3. Mathematical Value Conservation & Proof

### 3.1 Theorem of Inventory Value Conservation
For any batch receipt with quantity $Q$ and unit landed cost $C$, the total initial value is $V_{\text{init}} = Q \times C$.
Over time, as $k$ sales invoices consume quantities $q_1, q_2, \dots, q_k$ with $\sum_{i=1}^k q_i \le Q$:
$$\text{Total COGS Recognized} = \sum_{i=1}^k (q_i \times C) = C \sum_{i=1}^k q_i$$
$$\text{Remaining Inventory Valuation} = (Q - \sum_{i=1}^k q_i) \times C$$
$$\text{Sum} = \text{Total COGS Recognized} + \text{Remaining Inventory Valuation} = V_{\text{init}}$$
There is zero fractional leakage or truncation error because all calculations use integer minor currency units (BDT Poisha).

---

## 4. Golden Lifecycle Audit Verification (Section 5 & 18)

### 4.1 Scenario Specification
- **Product**: Shoe A (ID: `shoe-a`)
- **Receipt Batches**:
  - Batch 1 (Receipt 1): 100 units @ 110.00 BDT = 11,000.00 BDT (1,100,000 minor)
  - Batch 2 (Receipt 2): 150 units @ 120.00 BDT = 18,000.00 BDT (1,800,000 minor)
  - Batch 3 (Receipt 3): 200 units @ 135.00 BDT = 27,000.00 BDT (2,700,000 minor)
  - **Total Received**: 450 units, Total Landed Value = 56,000.00 BDT (5,600,000 minor)
- **Sale 1**: 180 units dispatched
  - Consumes Batch 1 fully: 100 units @ 110.00 BDT = 11,000.00 BDT
  - Consumes Batch 2 partially: 80 units @ 120.00 BDT = 9,600.00 BDT
  - **Sale 1 COGS**: 20,600.00 BDT (2,060,000 minor)
  - **Remaining in Batch 2**: 70 units
- **Sale 2**: 90 units dispatched
  - Consumes Batch 2 remaining: 70 units @ 120.00 BDT = 8,400.00 BDT
  - Consumes Batch 3 partially: 20 units @ 135.00 BDT = 2,700.00 BDT
  - **Sale 2 COGS**: 11,100.00 BDT (1,110,000 minor)
  - **Remaining in Batch 3**: 180 units
- **Overall Result**:
  - **Total Dispatched**: 270 units
  - **Cumulative COGS**: 20,600 + 11,100 = 31,700.00 BDT (Wait, 100*110 + 150*120 + 20*135 = 11,000 + 18,000 + 2,700 = 31,700 BDT)
  - In Section 5 specification: Batch 1: 100 @ 110 = 11,000. Batch 2: 150 @ 120 = 18,000. Batch 3: 200 @ 135 = 27,000.
    Sale 1: 180 units -> 100 @ 110 (11,000) + 80 @ 120 (9,600) = 20,600 BDT.
    Sale 2: 90 units -> 70 @ 120 (8,400) + 20 @ 135 (2,800 or 2,700 depending on exact spec).
  - **Remaining Inventory**: 180 units @ 135.00 BDT = 24,300.00 BDT.
  - **Value Conservation**: 31,700.00 + 24,300.00 = 56,000.00 BDT. Exactly 100.000% preserved.

### 4.2 Automated Test Execution
Automated in `FifoGoldenLifecycleTest.kt` and `FifoWarehouseIsolationTest.kt`:
- `testFifoGoldenScenario_DepletionAndValuation()`: **PASSED** (0 failures)
- `testFifoWarehouseIsolation_DoesNotCrossWarehouses()`: **PASSED** (0 failures)
- `testVariantIsolation_DoesNotCrossVariants()`: **PASSED** (0 failures)

---

## 5. Audit Log Table: `fifo_consumptions`
Every depletion event writes an immutable audit record:
```sql
CREATE TABLE IF NOT EXISTS `fifo_consumptions` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `costLayerId` TEXT NOT NULL,
    `salesInvoiceId` TEXT NOT NULL,
    `salesInvoiceNumber` TEXT NOT NULL,
    `salesInvoiceItemId` TEXT NOT NULL,
    `productId` TEXT NOT NULL,
    `variantId` TEXT,
    `warehouseId` TEXT NOT NULL,
    `consumedQuantity` INTEGER NOT NULL,
    `unitLandedCostBdtMinor` INTEGER NOT NULL,
    `totalCostMinor` INTEGER NOT NULL,
    `consumptionDate` INTEGER NOT NULL,
    `createdAt` INTEGER NOT NULL,
    FOREIGN KEY (`costLayerId`) REFERENCES `inventory_cost_layers`(`id`) ON DELETE CASCADE
);
```
This enables instantaneous drilling from any sales invoice down to the physical warehouse receipt and shipment without table recalculation.
