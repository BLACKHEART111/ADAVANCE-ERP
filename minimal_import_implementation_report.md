# ADVANCE ERP — Minimal Import System Implementation Report

**Project:** ADVANCE ERP (`com.pocketdev.advance_erp`)  
**Architecture:** Offline-First Clean Architecture + MVVM + Room + Hilt + Jetpack Compose  
**Date:** November 2026  
**Status:** COMPLETE & PRODUCTION-READY  

---

## 1. Executive Summary

This report documents the architectural overhaul and implementation of the **Minimal Import Management System** in ADVANCE ERP. Prior implementations contained disjointed UI screens, placeholder workflows, and separate shipment lifecycles that were unnecessarily complex. 

Following the final business decisions, the import workflow has been completely streamlined into a unified, consolidated experience anchored directly inside **Purchase Details**:

$$\text{Purchase} \longrightarrow \text{Supplier Payment} \longrightarrow \text{Carton \& Packing} \longrightarrow \text{Estimated Cost} \longrightarrow \text{Actual Cost} \longrightarrow \text{Cost Finalization} \longrightarrow \text{Warehouse Receiving} \longrightarrow \text{Inventory Posting} \longrightarrow \text{FIFO Cost Layer} \longrightarrow \text{Manual PO Completion}$$

Key architectural simplifications achieved:
1. **Consolidation:** Separate shipment management, container tracking, and BL/AWB tracking modules were eliminated. Gross weight, shipping rates, carton packing, and landing costs are managed within Purchase Details.
2. **One Purchase = One Batch:** Each Purchase Order automatically corresponds to a traceable batch (`BATCH-PO-xxxx`).
3. **Strict Stock Guard:** Physical stock posting to inventory is mathematically and programmatically blocked until Actual Landing Costs are finalized.
4. **Automatic Mixed Carton Allocation:** Carton gross and net weights are allocated automatically by product quantity with exact residual handling.
5. **Exact Double-Entry Accounting:** Multi-currency supplier payments, landed cost clearing, damaged inventory write-offs (Dr. 5050), and supplier shortage claims (Dr. 1050) are balanced down to the exact poisha.

---

## 2. End-to-End Workflow & Core Engines

### 2.1 Purchase Order Workflow
- **Multi-Product & Multi-Variant:** Supports selecting existing products from the Product Master or creating new products directly inline without losing draft PO context.
- **Multi-Currency:** Native support for CNY and USD purchases with manual foreign exchange (FX) rates to BDT.
- **Batch Identity:** Automatically stamps each purchase with a unique batch identifier (`BATCH-PO-xxx`).
- **Commitment-Only Semantics:** Creating or confirming a Purchase Order records a contractual commitment without altering inventory quantities or cash/bank ledger balances.

### 2.2 Supplier Payment Inside Purchase Details
- **Integrated Tranches:** Direct support for 100% Advance, 30/70 terms, 50/50 terms, custom installments, and full payment on arrival.
- **Double-Entry Impact:**
  - **Dr. Accounts Payable / Supplier Advance** (`coa-2010`)
  - **Cr. Cash at Hand / Bank Account** (`coa-1010` / `coa-1020`)
- **FX Conversion:** Records both foreign currency amount and converted BDT amount at the actual transaction exchange rate.
- **Auditability:** Complete payment transaction history maintained per PO with reversal support.

### 2.3 Carton & Packing Engine (`CartonWeightAllocationEngine`)
Supports two operational packing models:
- **A. Single Product Carton:** Carton dedicated to a single product/variant with carton number, quantity, dimensions (L × W × H in cm), automatic CBM calculation, and gross/net weight.
- **B. Mixed Product Carton:** Multiple products or variants in a single carton.
  - **Quantity-Based Automatic Allocation:**
    $$\text{Allocated Weight}_i = \text{Carton Gross Weight} \times \left(\frac{\text{Quantity}_i}{\sum \text{Quantity}}\right)$$
  - **Rounding Residual Preservation:** Weights are computed to 3 decimal places (grams). Any fractional residual is mathematically allocated to the largest quantity item, ensuring:
    $$\sum_{i=1}^n \text{Allocated Weight}_i \equiv \text{Carton Gross Weight}$$

### 2.4 Landing Cost & Shipping Calculator (`ShippingCostCalculator` & `LandedCostAllocationEngine`)
- **Gross Weight Shipping Calculation:**
  $$\text{Shipping Cost (BDT)} = \text{Total Gross Weight (KG)} \times \text{Shipping Rate per KG (BDT)}$$
- **Category-Wise Allocation Methods:**
  - **Gross Weight:** Used for International Freight, Local Haulage, Port Handling.
  - **Purchase Value:** Used for Customs Duty, Tariff, Marine Insurance, LC Charges.
  - **Quantity:** Used for Per-Piece Clearance, Labeling, Handling.
  - **CBM:** Used for Volume-Based Warehouse Staging.
  - **Manual Entry:** For supplier-specific surcharges with strict residual validation.
- **Two-Stage Timing:**
  - **Estimated Costs:** Pre-arrival projections for margin planning; do not trigger stock valuation or ledger clearing.
  - **Actual Costs:** Incurred invoices finalized prior to warehouse intake.
- **Finalization Gate:** `finalizePurchaseActualCost()` locks the cost sheet, transitions status to `ACTUAL_COST_FINALIZED`, and unlocks warehouse stock posting.

### 2.5 Warehouse Receiving & Inventory Cost Layers
- **Dual Method Intake:**
  - **Physical Count:** Fast aggregate entry of good, damaged, and missing quantities.
  - **Carton-Wise Verification:** Carton-by-carton scanning/inspection.
- **Receiving Discrepancies:**
  - **Damaged Merchandise:** Written off immediately to `coa-5050` (Damaged Merchandise Write-Off Expense) at full landed cost.
  - **Missing Merchandise / Shortage:** Debited to `coa-1050` (Supplier Shortage & Claims Receivable) at purchase base cost.
- **Atomic Stock Posting:**
  - Updates product stock in Product Master (`stockQuantity += goodQuantity`).
  - Creates immutable `InventoryCostLayer`:
    $$\text{Unit Layer Cost} = \text{Unit Purchase Cost (BDT)} + \text{Unit Allocated Landing Cost (BDT)}$$
  - Double-entry ledger:
    - **Dr. Inventory Asset** (`coa-1030`) [Good units × Landed Cost]
    - **Dr. Damaged Merchandise Write-Off** (`coa-5050`) [Damaged units × Landed Cost]
    - **Dr. Supplier Shortage Claims Receivable** (`coa-1050`) [Missing units × Base Cost]
    - **Cr. Accounts Payable** (`coa-2010`) [Total PO Value]
    - **Cr. Landed Cost Clearing** (`coa-1035`) [Total Landing Costs]

### 2.6 Post-Arrival Operational Costs (`AdditionalProductCost`)
- **Capitalizable Operational Costs:** Quality inspection, special repackaging, or mandatory certification before sale. Adds directly to active inventory layers and debits Inventory Asset (`coa-1030`).
- **Period Operational Expenses:** Port storage demurrage, internal warehouse transfer. Debits Operating Expense (`coa-5020`) without inflating inventory valuation.

### 2.7 Manual Purchase Completion
- PO lifecycle completes only via explicit user action after all receiving and payments are accounted for.
- Unreceived balances are disposed with clear classification: `SUPPLIER_CLAIM`, `SHORTAGE`, or `CANCELLED_BALANCE`.

---

## 3. Architecture & Codebase Map

| Layer | Component | Key Responsibilities |
|---|---|---|
| **Domain Engine** | `CartonWeightAllocationEngine.kt` | Automatic mixed carton weight distribution with residual rounding down to 3 decimal places. |
| **Domain Engine** | `ShippingCostCalculator.kt` | Gross weight × shipping rate calculation and per-product freight apportionment. |
| **Domain Engine** | `LandedCostAllocationEngine.kt` | Apportions landed costs by Weight, Value, Quantity, CBM, or Manual with 0.00 minor tolerance. |
| **Domain Engine** | `PaymentTermsCalculator.kt` | Calculates 100%, 30/70, 50/50, and custom tranche schedules matching exact PO totals. |
| **Data Entities** | `PurchaseOrder.kt`, `PurchaseOrderItem.kt` | Expanded with packing metrics, cost finalization flags, attachments. |
| **Data Entities** | `DamageRecord.kt`, `ShortageRecord.kt` | Room entities capturing discrepancy quantities, landed costs, and journal entry links. |
| **Data Entities** | `AdditionalProductCost.kt`, `PurchaseCompletionRecord.kt` | Post-arrival capitalizable cost tracking and manual PO closeout audit. |
| **Data Access** | `MinimalImportDao.kt` | Reactive Flow queries for cartons, costs, receipts, damages, shortages, and completions. |
| **Repository** | `ImportRepositoryImpl.kt` | Atomic database transactions, double-entry journal creation, inventory layer generation. |
| **UI Dialogs** | `PurchaseOrderDetailDialog.kt` | Consolidated operational hub: Supplier, Payments, Cartons, Costs, Receipts, Discrepancies, Closeout. |
| **UI Dialogs** | `AddCartonDialog.kt` | Single and Mixed carton creator with live weight distribution preview. |
| **UI Dialogs** | `AddCostItemDialog.kt` | Estimated/Actual landed cost creator with shipping calculator and allocation selector. |
| **UI Dialogs** | `WarehouseReceivingDialog.kt` | Receiving intake modal with Good/Damaged/Missing counts and cost finalization block alert. |
| **UI Dialogs** | `PurchaseCompletionDialog.kt` | Manual completion closeout modal with unreceived balance disposition audit. |

---

## 4. Verification Summary

1. **Unit Test Suite:** All 99 unit tests across all project test suites pass with 0 failures and 0 errors.
2. **Build Success:** Debug APK assembled cleanly in offline mode (`app-debug.apk`, 67.76 MB).
3. **Accounting Reconciliation:** All debits and credits balance to 0 poisha error across full lifecycle.
