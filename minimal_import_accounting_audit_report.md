# ADVANCE ERP — Minimal Import Accounting Audit Report

**Project:** ADVANCE ERP (`com.pocketdev.advance_erp`)  
**Scope:** Double-Entry Bookkeeping & Landed Cost Inventory Accounting  
**Audit Standard:** Strict Double-Entry Balance Guarantee ($\sum \text{Debits} \equiv \sum \text{Credits}$)  
**Date:** November 2026  
**Status:** FULLY AUDITED & RECONCILED  

---

## 1. Accounting Lifecycle Summary

The Minimal Import System adheres to strict double-entry accounting rules across all lifecycle stages:

```
[1. PO Confirmed]       --> No balance changes (Financial commitment only)
[2. Supplier Payment]   --> Dr. Accounts Payable (2010)       Cr. Bank/Cash (1010/1020)
[3. Landing Cost Incur] --> Dr. Landed Cost Clearing (1035)   Cr. Cash/Bank/Payable
[4. Stock Posting]      --> Dr. Inventory Asset (1030)        Cr. Accounts Payable (2010)
                            Dr. Damage Write-Off (5050)       Cr. Landed Cost Clearing (1035)
                            Dr. Shortage Claims (1050)
[5. Sales COGS]         --> Dr. Cost of Goods Sold (5010)     Cr. Inventory Asset (1030)
[6. Post-Arrival Cost]  --> Dr. Inventory Asset (1030)        Cr. Cash/Bank (1010)  [Capitalizable]
                            Dr. Operating Expense (5020)      Cr. Cash/Bank (1010)  [Period Expense]
```

---

## 2. Stage-by-Stage Journal Entries Audit

### Stage 1: Purchase Order Creation & Confirmation
- **Business Rule:** PO creation does NOT increase inventory stock, does NOT create cost layers, and does NOT record automated cash or bank outflows.
- **Journal Entry:** None ($0.00). It is a non-financial commitment event.

---

### Stage 2: Supplier Payments Inside Purchase Details
Supports Advance (30/70, 50/50), Partial, and Full payments with foreign exchange calculation.
- **Example:** Payment of 3,000 CNY at FX rate 17.00 BDT/CNY = 51,000.00 BDT (5,100,000 minor poisha).
- **Journal Entry:**
  - **Debit:** `coa-2010` (Accounts Payable / Supplier Advance) = 51,000.00 BDT
  - **Credit:** `coa-1020` (Bank Account / Cash) = 51,000.00 BDT
- **Audit Verification:** Net balance change = 0 BDT. Supplier ledger updated with reference to the PO number and transaction receipt.

---

### Stage 3: Actual Landing Cost Incurrence
Incurred prior to receiving (e.g., Shipping freight of 42,000 BDT, Customs duty of 15,000 BDT, Port handling of 3,000 BDT = Total 60,000 BDT).
- **Journal Entry:**
  - **Debit:** `coa-1035` (Landed Cost Clearing / Import Accrual) = 60,000.00 BDT
  - **Credit:** `coa-1010` / `coa-1020` (Cash / Bank / Clearing Payable) = 60,000.00 BDT
- **Audit Verification:** Clears cash/bank while accumulating import cost in intermediate clearing account `coa-1035`.

---

### Stage 4: Warehouse Receiving & Atomic Inventory Posting
Triggered when physical stock is accepted into the warehouse. Strictly blocked until actual costs are finalized.

#### Golden Scenario Example:
- **Ordered:**
  - Shoe A: 600 pairs @ 170 BDT base = 102,000 BDT. Allocated landed cost = 36,000 BDT (60 BDT/pair). Total landed rate = 230 BDT/pair.
  - Shoe B: 400 pairs @ 170 BDT base = 68,000 BDT. Allocated landed cost = 24,000 BDT (60 BDT/pair). Total landed rate = 230 BDT/pair.
  - Total PO Base = 170,000 BDT. Total Landed Cost = 60,000 BDT. Grand Total = 230,000 BDT.
- **Received:**
  - Shoe A: 590 Good, 5 Damaged, 5 Missing.
  - Shoe B: 400 Good (100% accepted).
- **Valuation Breakdown:**
  1. **Good Stock Inventory Asset:**
     - Shoe A: $590 \times 230.00 = 135,700.00\text{ BDT}$
     - Shoe B: $400 \times 230.00 = 92,000.00\text{ BDT}$
     - Total Inventory Asset = **227,700.00 BDT**
  2. **Damaged Merchandise Write-Off:**
     - Shoe A: $5 \times 230.00 = \mathbf{1,150.00\text{ BDT}}$ (Written off to P&L expense `coa-5050`)
  3. **Supplier Shortage Claim:**
     - Shoe A: $5 \times 230.00 = \mathbf{1,150.00\text{ BDT}}$ (Debited to Claims Asset `coa-1050`)
  4. **Clearing Liabilities:**
     - Accounts Payable (`coa-2010`): **170,000.00 BDT**
     - Landed Cost Clearing (`coa-1035`): **60,000.00 BDT**

#### Comprehensive Journal Entry:
| Account ID | Account Name | Debit (BDT) | Credit (BDT) |
|---|---|---|---|
| `coa-1030` | Inventory Asset | 227,700.00 | — |
| `coa-5050` | Damaged Merchandise Write-Off | 1,150.00 | — |
| `coa-1050` | Supplier Shortage & Claims Receivable | 1,150.00 | — |
| `coa-2010` | Accounts Payable (Supplier) | — | 170,000.00 |
| `coa-1035` | Landed Cost Clearing | — | 60,000.00 |
| **TOTAL** | | **230,000.00** | **230,000.00** |

**Audit Result:** $\sum \text{Debits} - \sum \text{Credits} = 0.00\text{ BDT}$. Clearing account `coa-1035` is zeroed out.

---

### Stage 5: FIFO Sales Consumption
When goods from the import batch are sold:
- **Example:** Sale of 100 pairs of Shoe A:
  - Consumes oldest layer (`BATCH-PO-xxx`) at landed cost of 230.00 BDT/pair.
  - Total COGS = $100 \times 230.00 = 23,000.00\text{ BDT}$.
- **Journal Entry:**
  - **Debit:** `coa-5010` (Cost of Goods Sold) = 23,000.00 BDT
  - **Credit:** `coa-1030` (Inventory Asset) = 23,000.00 BDT
- **Audit Verification:** Inventory accurately depleted at true landed cost. Gross margin accurately reflects shipping and customs costs.

---

### Stage 6: Additional Post-Arrival Operational Costs
- **Case A: Capitalizable (e.g., Mandatory Quality Inspection: 5,000 BDT)**
  - Increases cost basis of remaining inventory layer.
  - **Debit:** `coa-1030` (Inventory Asset) = 5,000.00 BDT
  - **Credit:** `coa-1010` (Cash / Bank) = 5,000.00 BDT
- **Case B: Period Operational Expense (e.g., Warehouse Demurrage: 2,500 BDT)**
  - Charged to P&L immediately without modifying layer valuation.
  - **Debit:** `coa-5020` (Operating Expense / Warehousing) = 2,500.00 BDT
  - **Credit:** `coa-1010` (Cash / Bank) = 2,500.00 BDT

---

### Stage 7: Manual Purchase Completion
When a purchase order is closed manually:
- If outstanding unreceived balances remain, they are reconciled through explicit disposition:
  - `SHORTAGE`: Written off to `coa-5050` or billed to supplier.
  - `CANCELLED_BALANCE`: Reverses unreceived commitment.
  - `SUPPLIER_CLAIM`: Stays in `coa-1050` until refunded or adjusted against future purchases.
- Full completion audit log is recorded in `purchase_completion_records`.

---

## 3. Double-Entry Invariants Verification Matrix

| Lifecycle Event | Debits Balanced? | Residual Poisha | Ledger Reconciled? |
|---|---|---|---|
| Supplier Payment (30% Advance) | YES | 0.00 | YES |
| Supplier Payment (70% Balance) | YES | 0.00 | YES |
| Actual Freight Incurrence | YES | 0.00 | YES |
| Actual Duty & Port Incurrence | YES | 0.00 | YES |
| Stock Posting & Layer Creation | YES | 0.00 | YES |
| Damage Write-Off | YES | 0.00 | YES |
| Shortage Claim | YES | 0.00 | YES |
| Sales COGS FIFO Consumption | YES | 0.00 | YES |
| Capitalized Post-Arrival Cost | YES | 0.00 | YES |
| Period Post-Arrival Expense | YES | 0.00 | YES |
