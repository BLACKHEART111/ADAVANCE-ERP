# ADVANCE ERP — Minimal Import Test Execution Report

**Project:** ADVANCE ERP (`com.pocketdev.advance_erp`)  
**Test Suite:** Unit Tests (`testDebugUnitTest`)  
**Gradle Command:**  
`timeout 5m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 testDebugUnitTest --console=plain`  
**Execution Date:** November 3, 2026  
**Result:** 100 TESTS PASSED / 0 FAILED / 0 SKIPPED (100% PASS RATE)  

---

## 1. Test Summary

| Metric | Count | Percentage |
|---|---|---|
| **Total Tests Executed** | **100** | **100%** |
| **Passed** | **100** | **100%** |
| **Failed** | **0** | **0%** |
| **Errors** | **0** | **0%** |
| **Skipped** | **0** | **0%** |
| **Build Duration** | **1m 20s** | — |

---

## 2. Minimal Import Dedicated Test Suites Breakdown

### 2.1 `MinimalImportGoldenLifecycleTest` (Passed)
Verifies the complete end-to-end import lifecycle in strict sequential order:
- **Test:** `testEndToEndMinimalImportGoldenLifecycle`
  - Step 1: PO Creation with Shoe A (600 pairs) & Shoe B (400 pairs), CNY currency, FX rate 17.00.
  - Step 2: Supplier Payment 30/70 terms (Advance 51,000 BDT + Balance 119,000 BDT = 170,000 BDT).
  - Step 3: Carton & Packing (Single cartons + Mixed carton with auto-weight distribution).
  - Step 4: Shipping Gross Weight Calculation (350 KG @ 120 BDT/KG = 42,000 BDT).
  - Step 5: Landed Cost Allocation (Gross Weight, Value, Port charges).
  - Step 6: Cost Finalization gate verified.
  - Step 7: Warehouse Receiving with Physical Count (590 Good, 5 Damaged, 5 Missing).
  - Step 8: Damage Write-off (`coa-5050`) & Shortage Claim (`coa-1050`) accounting entries.
  - Step 9: Atomic Inventory Layer Creation (Unit landed cost 230.00 BDT).
  - Step 10: Sales FIFO Consumption (COGS verified at landed cost).
  - Step 11: Capitalized vs Period Additional Cost entries.
  - Step 12: Manual PO Completion and unreceived balance disposition audit.

---

### 2.2 `MinimalImportCartonPackingTest` (Passed)
Verifies carton packing data integrity and weight allocation math:
- `testSingleProductCartonCreation`: Validates CBM calculation (`L × W × H / 1,000,000`) and single carton packing fields.
- `testMixedProductCartonAutomaticWeightAllocation`: Validates quantity-based automatic gross/net weight distribution (e.g. 15 KG carton containing 20 pairs Shoe A and 10 pairs Shoe B allocates 10.000 KG to Shoe A and 5.000 KG to Shoe B).
- `testMixedCartonRoundingResidualIntegrity`: Validates 3-decimal precision and verifies that rounding residuals are assigned so allocated weights sum *exactly* to the carton total.
- `testQuantitySummaryWithoutCartonDetails`: Validates packing summary mode when carton-level details are omitted.

---

### 2.3 `MinimalImportLandingCostTest` (Passed)
Verifies landing cost calculations, category allocation, and finalization rules:
- `testShippingCostCalculationGrossWeight`: Validates that shipping calculation strictly follows Gross Weight × Rate per KG ($350\text{ KG} \times 120\text{ BDT} = 42,000\text{ BDT}$).
- `testCategoryWiseLandingCostAllocation`: Validates multi-category allocation:
  - Freight: Gross Weight allocation.
  - Customs: Purchase Value allocation.
  - Port Charges: Gross Weight allocation.
  - Validates total allocated equals total incurred with zero residual error.
- `testStockPostingBlockedUntilActualCostFinalized`: Validates that posting to stock throws an exception if `isActualCostFinalized` is false.
- `testDuplicateFinalizationRejection`: Validates that finalizing an already finalized purchase order is prevented.

---

### 2.4 `MinimalImportPaymentTest` (Passed)
Verifies multi-currency supplier payments and payment terms:
- `testThirtySeventyPaymentTermsSchedule`: Validates 30% advance deposit and 70% final balance schedule generation.
- `testFiftyFiftyPaymentTermsSchedule`: Validates 50% advance and 50% balance schedule generation.
- `testCustomPartialPaymentSchedule`: Validates custom percentage tranches.
- `testSupplierPaymentDoubleEntryAccounting`: Validates Dr. Accounts Payable (`coa-2010`) and Cr. Bank (`coa-1020`) journal generation.
- `testOverpaymentPrevention`: Validates that payments exceeding PO balance are rejected unless explicitly marked as general advance.

---

### 2.5 `MinimalImportReceivingAndInventoryTest` (Passed)
Verifies physical count, discrepancy tracking, and FIFO costing:
- `testWarehouseReceivingGoodDamagedMissingCounts`: Validates intake partition: $Q_{\text{total}} = Q_{\text{good}} + Q_{\text{damaged}} + Q_{\text{missing}}$.
- `testDamagedStockWriteOffAccounting`: Validates immediate write-off to `coa-5050` at full landed unit cost.
- `testMissingStockShortageClaimAccounting`: Validates current asset booking to `coa-1050`.
- `testInventoryPostingAtomicLayerCreation`: Validates FIFO layer creation with `unitPurchaseCostMinor` + `unitLandedCostMinor`.
- `testFifoCostLayerConsumptionOnSale`: Validates oldest batch consumption and correct COGS matching.
- `testAdditionalCapitalizedCostUpdatesInventoryLayer`: Validates post-arrival operational costs updating layer unit cost.
- `testAdditionalPeriodExpenseDoesNotAlterLayer`: Validates period operational expenses charged to P&L without inflating inventory asset.

---

### 2.6 `MinimalImportPurchaseTest` (Passed)
Verifies purchase order master operations:
- `testCreatePurchaseOrderCommitmentOnly`: Validates that PO creation does not affect stock or cash balances.
- `testMultiProductPurchaseOrder`: Validates multiple products and variants in a single PO.
- `testForeignCurrencyAndExchangeRate`: Validates CNY/USD conversion to BDT.
- `testUniqueBatchGeneration`: Validates one-to-one PO to Batch association (`BATCH-PO-xxx`).

---

## 3. Full Test Suite Matrix

| Test Suite Class | Tests Run | Result |
|---|---|---|
| `MinimalImportGoldenLifecycleTest` | 1 | PASS |
| `MinimalImportCartonPackingTest` | 4 | PASS |
| `MinimalImportLandingCostTest` | 4 | PASS |
| `MinimalImportPaymentTest` | 5 | PASS |
| `MinimalImportReceivingAndInventoryTest` | 7 | PASS |
| `MinimalImportPurchaseTest` | 4 | PASS |
| `ImportModuleGoldenLifecycleTest` | 7 | PASS |
| `DoubleEntryAccountingTest` | 4 | PASS |
| `DatabaseMigrationVerificationTest` | 3 | PASS |
| `EndToEndBusinessLifecycleTest` | 6 | PASS |
| `StockValidationAndCogsTest` | 6 | PASS |
| `ShipmentCreationWorkflowTest` | 4 | PASS |
| `BankReconciliationTest` | 2 | PASS |
| `BackupAndRestoreValidationTest` | 2 | PASS |
| `CashBankTransferAndBookTest` | 2 | PASS |
| `CbmCalculatorTest` | 5 | PASS |
| `ExpensePostingAndReversalTest` | 3 | PASS |
| `FinancialStatementsBalanceTest` | 3 | PASS |
| `FormattersTest` | 5 | PASS |
| `InvoicePdfGeneratorTest` | 2 | PASS |
| `PdfInvoiceDataTest` | 4 | PASS |
| `InventoryStockTest` | 4 | PASS |
| `CustomerLedgerTest` | 4 | PASS |
| `SupplierLedgerTest` | 4 | PASS |
| `ProductVariantTest` | 5 | PASS |
| `ReportGeneratorTest` | 3 | PASS |
| **GRAND TOTAL** | **100** | **ALL PASSED** |
