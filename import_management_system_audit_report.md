# ADVANCE ERP — Import Management Module System Audit Report

**Audit Type:** Architectural, Monetary & Relational Integrity Audit  
**Auditor:** ADVANCE ERP Technical Architecture Team  
**Date:** November 3, 2026  
**Scope:** China Procurement, Foreign Currency Handling, Multi-Consignment Tracking, Landed Cost Allocation Engine, Physical Warehouse Receiving, FIFO Inventory Cost Layers, and General Ledger Reconciliation  
**Overall System Health Rating:** **Grade A (Audit-Ready & Structurally Sound)**

---

## 1. Executive Summary

This system audit reviews the complete Import Management subsystem within the `com.pocketdev.advance_erp` application codebase. The audit verifies compliance with strict ERP standards: offline-first operation, transactional SQLite/Room relational integrity, zero float drift on monetary amounts, strict segregation between procurement commitments and warehouse stock movements, and double-entry accounting reconciliation.

---

## 2. Issues & Architectural Risk Classifications

### 2.1 Critical Issues (Addressed & Remediated)

1. **Procurement Commitment Stock Bleed (Previously Identified $\rightarrow$ Remediated)**
   - *Risk:* In naive implementations, confirming a purchase order prematurely updates stock balances or inflates warehouse inventory, creating phantom assets.
   - *Audit Verification:* Audit of [`ImportRepositoryImpl.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt) verifies that `createPurchaseOrder` and `updatePurchaseOrderStatus` modify solely the `purchase_orders` commitment table and supplier accounts payable records. Live stock balances are strictly updated in `postWarehouseReceipt`. Zero stock movements occur prior to physical warehouse receiving.

2. **Damaged & Missing Stock Exclusion from Sellable Inventory (Remediated)**
   - *Risk:* If physical receiving check-in counts damaged or missing cartons as sellable warehouse stock, customer orders will be accepted against defective or nonexistent inventory, causing dispatch failures.
   - *Audit Verification:* [`ImportRepositoryImpl.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt) explicitly computes sound good quantity:
     $$\text{Good Quantity} = \text{Received Quantity} - \text{Damaged Quantity}$$
     Sellable `StockBalance` and `InventoryCostLayer` records are created strictly for good quantity. Damaged units are isolated in `StockMovement` logs with type `DAMAGE_EXCLUDED`, and shortages are logged as `MISSING_IN_TRANSIT`.

3. **Floating-Point Rounding & Monetary Drift (Remediated)**
   - *Risk:* Using `Double` or `Float` for foreign currency conversion, fractional CBM allocation, or multi-tranche payments results in cumulative fractional cent/poisha discrepancies that violate balance sheet equilibrium.
   - *Audit Verification:* All persistent financial columns use integer `Long` minor units (paisa for BDT, fen for CNY). Volumetric math in [`CbmCalculator.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/engine/CbmCalculator.kt) and Landed Cost allocation in [`LandedCostAllocationEngine.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/engine/LandedCostAllocationEngine.kt) use `BigDecimal` with explicit rounding modes and deterministic remainder redistribution.

---

### 2.2 High-Priority Issues (Addressed & Remediated)

1. **Duplicate Warehouse Receipt Posting (Remediated)**
   - *Risk:* Repeatedly tapping "Post to Warehouse" or network/process retries could inject duplicated stock balances and duplicate journal entries.
   - *Audit Verification:* [`ImportRepositoryImpl.kt:postWarehouseReceipt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt) enforces:
     ```kotlin
     check(receipt.status != ReceiptStatus.POSTED.name) {
         "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
     }
     ```
     Execution is wrapped within an atomic `database.withTransaction` block.

2. **Over-Shipment and Overpayment Validation (Remediated)**
   - *Risk:* Consignments exceeding confirmed order quantities or supplier payments exceeding remaining balances lead to financial leakage.
   - *Audit Verification:* Both boundaries are checked at repository level:
     - `require(amountMinor <= remainingBalance)` in `recordSupplierPayment`
     - `require(alreadyShipped + newQty <= orderedQty)` in `createShipment`

3. **Post-Receiving Landed Cost Variance Adjustment (Remediated)**
   - *Risk:* In real-world shipping, final customs duties and demurrage bills arrive days or weeks after containers are physically unloaded and partial stock has already been sold.
   - *Audit Verification:* [`ImportRepositoryImpl.kt:adjustLandedCost`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt) calculates per-unit variance:
     - Portion attributable to sold inventory $\rightarrow$ adjusted to COGS (Account 5010).
     - Portion attributable to remaining warehouse stock $\rightarrow$ adjusted to Inventory Asset (Account 1210).
     - Historical sales invoices remain unaltered while financial statements maintain exact double-entry balance.

---

### 2.3 Medium-Priority Issues (Addressed & Verified)

1. **Consolidated Consignments from Multiple POs**
   - *Status:* Supported. `ImportShipment` maintains a nullable `purchaseOrderId`, and `ShipmentCarton` maps carton-level product references.
2. **Deterministic Rounding Remainder Distribution**
   - *Status:* Verified. `LandedCostAllocationEngine` allocates remainders unit-by-unit (poisha-by-poisha) to items with the largest basis value, guaranteeing $\Sigma \text{Allocated} == \text{Total Cost}$ exactly.
3. **Cancellation Safeguards**
   - *Status:* Verified. `cancelPurchaseOrder` prohibits cancellation if payments or shipments exist, and safely rolls back supplier accounts payable commitments if confirmed.

---

### 2.4 Low-Priority Issues & Minor UI Polish

1. **Localization Label Fallbacks:**
   - *Status:* Verified. All 8 tabs, dialog fields, status badges, and PDF documents provide complete bilingual English and Bengali labels.
2. **Centralized Search Responsiveness:**
   - *Status:* Verified. Search queries reactively filter PO numbers, supplier names, shipment tracking codes, and notes without main-thread blocking.

---

## 3. Remaining Risks & Ongoing Recommendations

1. **Foreign Exchange Fluctuations on Prepayments:**
   - When large supplier prepayments are made months before container dispatch, fluctuations in USD/CNY vs BDT between payment date and customs assessment date should be audited periodically for unrealized vs realized FX gain/loss entries.
2. **Database Backup Strategy:**
   - Because the system operates offline-first on SQLite/Room, users should regularly invoke the built-in encrypted database export feature before major firmware updates.
