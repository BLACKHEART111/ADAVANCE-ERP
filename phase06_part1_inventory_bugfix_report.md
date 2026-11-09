# ADVANCE ERP — Phase 06 Part 1: Inventory Critical Bug Fixes & Core Functionality Report

**Date:** November 3, 2026  
**Project:** ADVANCE ERP (`/workspace/advance-erp`)  
**Package:** `com.pocketdev.advance_erp`  
**Reference Document:** `inventory_module_audit_report.md`  
**Database Version:** Upgraded from Version 5 to 6 (`MIGRATION_5_6`)  
**Build Status:** `BUILD SUCCESSFUL` (123/123 tests passing, debug APK assembled)  

---

## 1. Executive Summary

Phase 06 Part 1 resolves all high-priority bugs and architectural defects identified in the Inventory module audit:
1. **Movement Number Collision Elimination:** Replaced deterministic `System.currentTimeMillis() % 10000` with collision-resistant `MovementNumberGenerator` and enforced database uniqueness (`OnConflictStrategy.ABORT`).
2. **Stock Transfer Between Warehouses:** Built full lifecycle stock transfer workflows (`DRAFT` → `IN_TRANSIT` → `RECEIVED` / `CANCELLED`) with in-transit quarantine, FIFO unit landed cost layer preservation, and zero valuation distortion.
3. **Variant-Aware Stock Adjustments:** Fixed hardcoded `null` variants in `StockAdjustmentDialog.kt`, wiring variant selection end-to-end to isolate variant balances and cost layers.
4. **Standardized Stock Movement Types:** Replaced fragmented raw strings with centralized domain model `StockMovementType` and upgraded dashboard metrics to live database queries.
5. **Correct Sales Return Accounting & Display:** Corrected UI direction styling for `SALE_RETURN` (`+` incoming green) and segregated damaged returns into damage write-off accounting without inflating sellable inventory.
6. **Inventory Screen UX Enhancements:** Added multi-field search (English/Bangla name, SKU, barcode, variant SKU), category filter chips, and a real-time 4-metric Inventory Valuation Card backed by FIFO remaining cost layers.

---

## 2. Root Cause Analysis of Audited Bugs

### Bug 1: Movement Number Collision
* **Location:** `ImportRepositoryImpl.kt` (`receiveShipment`, `recordDamagedGoods`), `InventoryRepositoryImpl.kt` (`adjustStock`), `ProductRepositoryImpl.kt` (`addProduct`), `SalesRepositoryImpl.kt` (`completeSale`, `processReturn`).
* **Root Cause:** Movement numbers were generated using `System.currentTimeMillis() % 10000`. In multi-item purchase receipts or rapid transactions within the same millisecond, this modulo arithmetic generated identical numbers (`0000`–`9999`). Furthermore, `StockMovementDao` used `@Insert(onConflict = OnConflictStrategy.REPLACE)`, causing silent overwrites of prior stock movement audit entries.
* **Fix:** Introduced `MovementNumberGenerator` generating `MOV-YYYYMMDD-HHMMSS-SSSSSS-RRRR` combining high-resolution wall timestamps, an `AtomicLong` sequence counter, and a cryptographically pseudorandom UUID suffix. Enforced database uniqueness on `StockMovement.movementNumber` and switched DAO conflict strategy to `OnConflictStrategy.ABORT`.

### Bug 2: Missing Inter-Warehouse Stock Transfer Workflow
* **Location:** Architecture gap across Data, Domain, and UI layers.
* **Root Cause:** Warehouse stock could only be adjusted independently via manual adjustments. There was no mechanism to transfer items between warehouses while tracking in-transit stock, ensuring source availability, or migrating specific FIFO landed cost layers.
* **Fix:** Designed and implemented `StockTransfer` and `StockTransferCostLayer` entities, `StockTransferDao`, repository workflow methods (`createTransfer`, `dispatchTransfer`, `receiveTransfer`, `cancelTransfer`), Room migration `MIGRATION_5_6`, UI dialog `StockTransferDialog.kt`, and transfer management tabs in `InventoryScreen.kt`.

### Bug 3: Variant-Aware Stock Adjustment
* **Location:** `StockAdjustmentDialog.kt:144` (`variantId = null`).
* **Root Cause:** The UI dialog completely omitted variant selection controls and hardcoded `variantId = null` when calling `onConfirm`. Products with variants had their parent product stock adjusted, corrupting variant balance breakdown and preventing variant-specific FIFO layer deductions.
* **Fix:** Added dynamic variant dropdown selector in `StockAdjustmentDialog.kt`. Enforced variant selection for products having variants. Propagated `selectedVariantId` through ViewModel → Repository → DAO to target variant-specific balances and `InventoryCostLayer` records.

### Bug 4: Inconsistent Stock Movement Types & Dashboard Count Failure
* **Location:** `DashboardRepositoryImpl.kt`, `SalesRepositoryImpl.kt`, `ImportRepositoryImpl.kt`.
* **Root Cause:** Movement types were raw, un-validated strings (`"SALE"`, `"SALE_DISPATCH"`, `"PURCHASE"`, `"IMPORT_RECEIPT"`). `DashboardRepositoryImpl` queried `stockMovementDao.getAllMovements()` limited to 10 rows and filtered by `it.movementType == "SALE_DISPATCH"`, whereas `SalesRepositoryImpl` recorded `"SALE"`. This resulted in 0 sales dispatches reported on the dashboard.
* **Fix:** Standardized types in `StockMovementType` enum. Added direct SQL aggregation queries `getSalesDispatchCountSince` and `getSalesDispatchAmountSince` in `StockMovementDao` matching both standard (`SALE_DISPATCH`) and legacy (`SALE`) records over the full time window.

### Bug 5: Sales Return Display and Damaged Return Accounting
* **Location:** `InventoryScreen.kt`, `SalesRepositoryImpl.kt`.
* **Root Cause:** `InventoryScreen.kt` classified incoming movements with `contains("RECEIPT") || contains("INCREASE")`. `SALE_RETURN` contained neither, displaying customer returns as outbound negative stock with red styling. Moreover, damaged returns were credited to general stock balances, inappropriately increasing sellable stock.
* **Fix:** Added `StockMovementType.isIncoming` and updated `InventoryScreen.kt` to render `SALE_RETURN` as `+` with `StatusSuccess` green styling. In `SalesRepositoryImpl.kt`, customer returns now accept `isDamaged` and `damagedQuantity`; sound returns increase stock balance and create FIFO cost layers, while damaged goods are excluded from sellable inventory and recorded as `DAMAGE_WRITE_OFF` with accounting debits to `coa-5050` (Damaged Goods Expense).

### Bug 6: Inventory Screen UX Gaps
* **Location:** `InventoryScreen.kt`, `StockBalanceDao.kt`.
* **Root Cause:** No real-time search or category filters existed. Valuation was either absent or computed using retail selling price instead of actual inventory landed costs.
* **Fix:** Extended `StockItemOverview` with `categoryId`, `barcode`, and variant SKUs. Implemented multi-field search and category chip filtering. Built an Inventory Valuation Card displaying Physical Qty, Sellable Stock, Damaged Stock, and Total FIFO Valuation minor currency summed directly from unexhausted `InventoryCostLayer` records.

---

## 3. Files Changed and Added

| File Path | Action | Description |
|:---|:---:|:---|
| `core/util/MovementNumberGenerator.kt` | **Added** | Collision-resistant generator with timestamp, atomic sequence, and random suffix. |
| `data/entity/StockMovementType.kt` | **Added** | Centralized domain enum with bilingual titles, directions, and legacy mapping. |
| `data/entity/StockTransfer.kt` | **Added** | `StockTransfer` and `StockTransferCostLayer` Room entities. |
| `data/dao/StockTransferDao.kt` | **Added** | DAO for transfers, transfer details, in-transit aggregation, and cost layers. |
| `ui/inventory/StockTransferDialog.kt` | **Added** | Material 3 dialog for initiating inter-warehouse stock transfers. |
| `core/database/DatabaseMigrations.kt` | **Modified** | Implemented `MIGRATION_5_6` for transfer tables, indexes, and data cleanup. |
| `core/database/AdvanceErpDatabase.kt` | **Modified** | Incremented database version to 6; registered new entities and DAO. |
| `core/di/DatabaseModule.kt` | **Modified** | Injected `StockTransferDao`. |
| `data/dao/StockMovementDao.kt` | **Modified** | Enforced `ABORT` conflict strategy; added sales dispatch & damaged count queries. |
| `data/dao/StockBalanceDao.kt` | **Modified** | Added `categoryId`, `barcode`, variant SKUs, physical, and sellable stock queries. |
| `data/dao/InventoryCostLayerDao.kt` | **Modified** | Added FIFO valuation queries and layer transfer migration queries. |
| `data/repository/InventoryRepository.kt` | **Modified** | Added transfer workflow signatures and valuation metrics. |
| `data/repository/InventoryRepositoryImpl.kt` | **Modified** | Implemented transfer state machine, FIFO layer migration, and variant adjustment. |
| `data/repository/ImportRepositoryImpl.kt` | **Modified** | Adopted `MovementNumberGenerator` and standardized movement types. |
| `data/repository/ProductRepositoryImpl.kt` | **Modified** | Adopted `MovementNumberGenerator` and added initial stock FIFO cost layer. |
| `data/repository/SalesRepository.kt` | **Modified** | Extended `ReturnItemDraft` with `isDamaged` and `damagedQuantity`. |
| `data/repository/SalesRepositoryImpl.kt` | **Modified** | Segregated damaged returns (`coa-5050`); adopted generator & standard types. |
| `data/repository/DashboardRepositoryImpl.kt` | **Modified** | Switched dispatch count/amounts to live SQL queries matching standard types. |
| `ui/inventory/StockAdjustmentDialog.kt` | **Modified** | Added variant dropdown selector and variant ID propagation. |
| `ui/inventory/InventoryViewModel.kt` | **Modified** | Added search state, category filters, transfer workflow actions, and valuation state. |
| `ui/inventory/InventoryScreen.kt` | **Modified** | Added Valuation Card, search bar, category chips, Transfers tab, and `SALE_RETURN` fix. |
| `res/values/strings.xml` | **Modified** | English localization strings for valuation, transfers, and movement types. |
| `res/values-bn/strings.xml` | **Modified** | Bangla localization strings for valuation, transfers, and movement types. |
| `src/test/java/com/pocketdev/advance_erp/MovementCollisionAndUniquenessTest.kt` | **Added** | Tests 1–4: Concurrency, same-ms creation, uniqueness, and rollback. |
| `src/test/java/com/pocketdev/advance_erp/WarehouseStockTransferWorkflowTest.kt` | **Added** | Tests 5–9: Draft → Dispatch → Receive, cancellation, FIFO layer preservation. |
| `src/test/java/com/pocketdev/advance_erp/VariantAdjustmentAndInventoryScreenTest.kt` | **Added** | Tests 10–15: Variant adjustment, returns, search, category chips, FIFO valuation. |
| `src/test/java/com/pocketdev/advance_erp/DatabaseMigrationVerificationTest.kt` | **Modified** | Verified `MIGRATION_5_6` start/end versions and schema execution. |

---

## 4. Database Migrations (MIGRATION_5_6)

Database version was bumped from **5 to 6**. The migration script performs:
1. **Creation of `stock_transfers` table:**
   - Columns: `id`, `transferNumber` (UNIQUE), `sourceWarehouseId`, `destinationWarehouseId`, `productId`, `variantId`, `quantity`, `transferDate`, `notes`, `status`, `dispatchedAt`, `receivedAt`, `cancelledAt`, `createdAt`, `updatedAt`.
   - Foreign Keys: `sourceWarehouseId` → `warehouses.id`, `destinationWarehouseId` → `warehouses.id`, `productId` → `products.id`.
   - Indexes: `index_stock_transfers_transferNumber` (UNIQUE), `index_stock_transfers_sourceWarehouseId`, `index_stock_transfers_destinationWarehouseId`, `index_stock_transfers_productId`, `index_stock_transfers_status`.
2. **Creation of `stock_transfer_cost_layers` table:**
   - Columns: `id`, `transferId`, `originalLayerId`, `quantity`, `unitLandedCostMinor`, `batchNumber`, `expiryDate`, `createdAt`.
   - Foreign Keys: `transferId` → `stock_transfers.id` (CASCADE).
   - Indexes: `index_stock_transfer_cost_layers_transferId`.
3. **Historical Data Sanitization:**
   - Normalizes legacy movement types without data loss:
     * `'IMPORT_RECEIPT'` → `'PURCHASE_RECEIPT'`
     * `'SALE'` → `'SALE_DISPATCH'`
     * `'PURCHASE'` → `'PURCHASE_RECEIPT'`

---

## 5. Business Rules Implemented

### 1. Collision-Resistant Movement Numbers
- Format: `MOV-YYYYMMDD-HHMMSS-SSSSSS-RRRR` (e.g., `MOV-20261103-181500-000123-A7F9`).
- Guaranteed unique even across simultaneous threads running within the exact same microsecond.
- Database uniqueness backed by unique index on `StockMovement.movementNumber`.
- Any collision attempt immediately triggers `SQLiteConstraintException` and rolls back the atomic transaction.

### 2. End-to-End Stock Transfer Lifecycle
- **DRAFT:** Creates transfer record without modifying inventory or cost layers.
- **DISPATCH:**
  * Validates source warehouse stock availability (`quantity <= availableStock`).
  * Decrements `StockBalance.quantity` at source warehouse.
  * Consumes source `InventoryCostLayer` records in FIFO order.
  * Persists exact unit landed costs in `StockTransferCostLayer`.
  * Increments `StockBalance.inTransitQuantity` and overall product `inTransitQuantity`.
  * In-transit stock is quarantined and strictly excluded from sellable stock.
  * Creates `TRANSFER_OUT` stock movement.
- **RECEIVE:**
  * Increments `StockBalance.quantity` at destination warehouse.
  * Creates new `InventoryCostLayer` records at destination with original unit landed costs.
  * Decrements source `StockBalance.inTransitQuantity` and product `inTransitQuantity`.
  * Creates `TRANSFER_IN` stock movement.
- **CANCEL:**
  * Permitted only in `DRAFT` or `IN_TRANSIT` status.
  * Reverses dispatched stock back into source warehouse `StockBalance.quantity`.
  * Reinstates source `InventoryCostLayer`s with original landed cost.
  * Clears `inTransitQuantity`.
- **Valuation Invariant:**
  $$\text{Valuation}_{\text{Source}} + \text{Valuation}_{\text{Dest}} + \text{Valuation}_{\text{InTransit}} = \text{Constant}$$
  Transfer operations never duplicate, inflate, or destroy inventory valuation.

### 3. Variant-Aware Stock Adjustment
- Detects if product has defined variants (`hasVariants == true`).
- Requires explicit variant selection when variants exist.
- Adjusts only the selected variant's `StockBalance` and variant-tagged `InventoryCostLayer`.
- Retains full audit log containing both `productId` and `variantId`.

### 4. Sales Return & Damaged Goods Accounting
- `SALE_RETURN` displays as incoming `+` with green styling.
- Sound goods: increases sellable stock and adds active FIFO cost layer.
- Damaged goods: strictly excluded from sellable stock; written off to Account `coa-5050` (Damaged Goods Expense) and logged under `StockMovementType.DAMAGE_WRITE_OFF`.

### 5. Inventory Valuation Metric
- Computed strictly as:
  $$\text{Total Valuation} = \sum_{\text{active layers}} (\text{remainingQuantity} \times \text{unitLandedCostMinor})$$
  Does not use retail selling price or unweighted average costs.

---

## 6. Actual Test Results

All test suites were executed via `testDebugUnitTest` (Gradle 8.14.3, Android AAPT2 override).

### Test Suite Execution Summary
- **Total Tests Executed:** 123
- **Failures:** 0
- **Ignored:** 0
- **Success Rate:** 100%
- **Execution Time:** ~0.864s (Unit test phase) / 2m 20s (Total test task)

### Verification of Required Audit Test Scenarios

| # | Scenario | Test Class & Method | Result |
|:---|:---|:---|:---:|
| 1 | Same-millisecond movement creation | `MovementCollisionAndUniquenessTest.testSameMillisecondMovementsAreUnique` | **PASSED** |
| 2 | Multiple movement creation within one receipt | `MovementCollisionAndUniquenessTest.testMultipleMovementsWithinSingleReceiptHaveUniqueNumbers` | **PASSED** |
| 3 | Movement number uniqueness enforcement | `MovementCollisionAndUniquenessTest.testMovementNumberFormatAndUniqueness` | **PASSED** |
| 4 | Concurrent movement creation & transaction rollback | `MovementCollisionAndUniquenessTest.testConcurrentMovementCreation` & `testDatabaseUniquenessConstraintRollsBackTransaction` | **PASSED** |
| 5 | Warehouse transfer: Draft → Dispatch → Receive | `WarehouseStockTransferWorkflowTest.testDraftDispatchReceiveWorkflow` | **PASSED** |
| 6 | Insufficient source stock validation | `WarehouseStockTransferWorkflowTest.testInsufficientSourceStockThrowsException` | **PASSED** |
| 7 | Duplicate dispatch & receipt prevention | `WarehouseStockTransferWorkflowTest.testDuplicateDispatchPrevented` & `testDuplicateReceivePrevented` | **PASSED** |
| 8 | Transfer cancellation & stock/layer reversal | `WarehouseStockTransferWorkflowTest.testCancelInTransitTransferReversesStockAndLayers` | **PASSED** |
| 9 | FIFO cost preservation without valuation duplication | `WarehouseStockTransferWorkflowTest.testFifoCostPreservationAcrossWarehouses` & `testValuationNeverDuplicatedDuringTransfer` | **PASSED** |
| 10 | Variant-specific stock adjustment | `VariantAdjustmentAndInventoryScreenTest.testVariantSpecificAdjustmentIsolatesVariantBalance` | **PASSED** |
| 11 | `SALE_DISPATCH` dashboard count & sales sum | `VariantAdjustmentAndInventoryScreenTest.testSalesDispatchDashboardCountAndAmount` | **PASSED** |
| 12 | `SALE_RETURN` stock increase & incoming styling | `VariantAdjustmentAndInventoryScreenTest.testSaleReturnStockIncreaseAndStyling` | **PASSED** |
| 13 | Damaged return exclusion & damage accounting | `VariantAdjustmentAndInventoryScreenTest.testDamagedReturnExclusionAndAccounting` | **PASSED** |
| 14 | Search and category filtering without duplicates | `VariantAdjustmentAndInventoryScreenTest.testSearchAndCategoryFiltering` | **PASSED** |
| 15 | FIFO inventory valuation calculation | `VariantAdjustmentAndInventoryScreenTest.testFifoInventoryValuationCalculation` | **PASSED** |
| — | Database migration v5 → v6 validation | `DatabaseMigrationVerificationTest.testMigration5To6_StockTransfersAndLegacyMovementTypes` | **PASSED** |

---

## 7. APK Build Verification

The debug APK was built using the cached offline build path in accordance with workspace guidelines:

- **Command:** `timeout 5m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 assembleDebug --console=plain --stacktrace`
- **Result:** `BUILD SUCCESSFUL in 1m 43s` (39 actionable tasks: 5 executed, 34 up-to-date)
- **Output Artifact:** `/workspace/advance-erp/app/build/outputs/apk/debug/app-debug.apk`
- **Artifact Size:** 66 MB (68,709,004 bytes)
- **Timestamp:** November 3, 2026, 18:19 UTC
- **Note:** Verification was performed via unit and database migration tests in headless JVM environment; physical hardware deployment was not conducted.

---

## 8. Remaining Issues & Next Phase Recommendations

- **No blocking issues remaining for Phase 06 Part 1.** All critical inventory bugs, collision vulnerabilities, missing transfer lifecycles, variant adjustment bugs, and UX display inaccuracies have been resolved.
- **Recommendations for Phase 06 Part 2:**
  - Introduce multi-item transfer manifests (bundling multiple products into a single transfer shipment).
  - Add optional QR code scanning for warehouse dispatch and receiving verification.
  - Implement scheduled automated inventory reorder alerts based on minimum warehouse safety thresholds.
