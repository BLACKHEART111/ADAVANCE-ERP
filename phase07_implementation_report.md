# ADVANCE ERP — Phase 07: Safe Deletion & Financial Reversal Report

## Executive Summary
This document details the architectural implementation, database migration (Version 8 to 9), double-entry financial reversal mechanisms, inventory restoration, and audit compliance for **Phase 07: Safe Deletion & Financial Reversal** within `com.pocketdev.advance_erp`.

---

## 1. Database Schema & Migration (v8 → v9)

### 1.1 Schema Changes
1. **`customer_payments` Table**:
   - Added `isVoided INTEGER NOT NULL DEFAULT 0`
   - Added `voidedAt INTEGER`
   - Added `voidReason TEXT`
   - Added `voidedBy TEXT`
2. **`sales_invoices` Table**:
   - Added `cancellationReason TEXT`
   - Added `cancelledAt INTEGER`
3. **`audit_logs` Table**:
   - Added `documentNumber TEXT`
   - Added `operationType TEXT`
   - Added `previousStatus TEXT`
   - Added `newStatus TEXT`
   - Added `cancellationReason TEXT`
   - Added `reversalReference TEXT`
4. **`product_categories` Seeding**:
   - Added default system category `'cat-uncategorized'` (`code = 'UNCAT'`, `nameEn = 'Uncategorized'`, `nameBn = 'শ্রেণীবিহীন'`) to serve as a safe destination during category deletion/reassignment.

### 1.2 Migration Specification (`MIGRATION_8_9`)
- Location: [DatabaseMigrations.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/database/DatabaseMigrations.kt#L198-L232)
- Added to database builder migration list in [AdvanceErpDatabase.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/database/AdvanceErpDatabase.kt#L208).
- Database version incremented from 8 to 9.

---

## 2. Module Implementations

### 2.1 Category Safe Deletion & Reassignment (PART 1)
- **DAO & Repository**:
  - [ProductCategoryDao.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/dao/ProductCategoryDao.kt): Added `getProductCountForCategory`, `deleteCategory`, `reassignCategoryProducts`, `getCategoryByCode`.
  - [ProductRepositoryImpl.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ProductRepositoryImpl.kt): Atomic `@Transaction` method `reassignCategoryAndHardDelete(oldCategoryId, targetCategoryId, operator)` reassigns all referencing products to the selected target category (or fallback `'cat-uncategorized'`) and hard-deletes the old category while emitting an audit log entry.
  - Hard deletion without reassignment is blocked if `getProductCountForCategory(categoryId) > 0`.
- **UI & ViewModel**:
  - [ProductViewModel.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/inventory/ProductViewModel.kt): Category count check, delete, and reassign flows.
  - [ProductListScreen.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/inventory/ProductListScreen.kt): "Manage / ক্যাটাগরি পরিচালনা" action displays linked product counts, blocks deleting non-empty categories without reassignment, and provides dialogs for reassignment to target category or Uncategorized.

### 2.2 Customer Safe Deletion & Archival (PART 2)
- **DAO & Repository**:
  - [CustomerDao.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/dao/CustomerDao.kt): `archiveCustomer(customerId, updatedAt)` and `hardDeleteCustomer(customerId)`.
  - [ContactsRepositoryImpl.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/ContactsRepositoryImpl.kt):
    - Blocked if customer balance != 0 (`CustomerDeleteResult.BlockedByBalance`).
    - If outstanding balance is 0, queries financial history: `salesDao.getInvoiceCountForCustomer`, `paymentDao.getPaymentCountForCustomer`, and `salesReturnDao.getReturnCountForCustomer`.
    - If financial history > 0: executes `archiveCustomer(customerId)` and emits audit log (`CustomerDeleteResult.Archived`).
    - If financial history == 0: executes `hardDeleteCustomer(customerId)` and emits audit log (`CustomerDeleteResult.HardDeleted`).
- **UI & ViewModel**:
  - [ContactsViewModel.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/contacts/ContactsViewModel.kt): Dispatches delete or archive operation.
  - [ContactsScreen.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/contacts/ContactsScreen.kt): Delete icon on customer cards triggers balance validation, showing a blocking alert if balance != 0, and clear confirmation indicating whether the customer will be archived or deleted.

### 2.3 Payment Void & Financial Reversal (PART 3)
- **DAO & Repository**:
  - [CustomerPaymentDao.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/dao/CustomerPaymentDao.kt): Added `isVoided = 0` filtering across payment sums and balance calculations.
  - [SalesRepositoryImpl.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/SalesRepositoryImpl.kt): Atomic `@Transaction` method `voidCustomerPayment(paymentId, reason, operator)`:
    1. Validates payment is not already voided (idempotency).
    2. Updates payment: `isVoided = true`, `voidedAt`, `voidReason`, `voidedBy`.
    3. Fetches original accounting entries for payment reference and creates balancing reversal entries (flipping Debit and Credit amounts, marking source document status as `VOIDED`).
    4. Removes allocations from `sales_payment_allocations` for this payment.
    5. Re-queries and recalculates each affected invoice's `paidAmountMinor`, `dueAmountMinor`, and `paymentStatus` (`UNPAID` / `PARTIALLY_PAID`).
    6. Updates customer `totalDueMinor` and writes customer ledger reversal entry.
    7. Creates unified `AuditLog` entry.
- **UI & ViewModel**:
  - [SalesViewModel.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/sales/SalesViewModel.kt): Exposes `voidCustomerPayment`.
  - [SalesScreen.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/sales/SalesScreen.kt): Payments tab displays void badges and reason, and provides void button opening confirmation dialog with mandatory reason text field.

### 2.4 Sales Invoice Cancellation & Return-to-Stock (PART 4)
- **DAO & Repository**:
  - [SalesRepositoryImpl.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/data/repository/SalesRepositoryImpl.kt): Atomic `@Transaction` method `cancelOrDeleteInvoice(invoiceId, reason, operator)`:
    1. For unposted `DRAFT` invoices: cleanly removes line items and invoice record via `deleteInvoiceItems` and `deleteInvoice`.
    2. For posted invoices (`PAID`, `PARTIALLY_PAID`, `UNPAID`):
       - Sets `status = 'CANCELLED'`, preserving invoice number permanently.
       - Restores physical warehouse stock: for each invoice item, calculates net dispatched quantity (`dispatchedQuantity - returnQuantity`) and inserts restoration `stock_movements` (`movementType = 'INVOICE_CANCELLED_RESTORE'`) while updating `stock_balances`.
       - Restores FIFO cost layers: finds consumed cost layers associated with the invoice or product in warehouse, incrementing `remainingQuantity` up to original `initialQuantity`.
       - Reverses accounting journal: creates balancing reversal entries for Sales Revenue (`coa-4010`), Accounts Receivable (`coa-1030`), COGS (`coa-5010`), and Inventory (`coa-1040`).
       - Converts any allocated customer payments into unapplied customer advances (`coa-2030` Customer Advances) to prevent double counting and allow future application.
       - Reverses customer due and posts ledger cancellation record.
       - Emits unified `AuditLog` entry.
- **UI & ViewModel**:
  - [InvoiceDetailDialog.kt](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/presentation/sales/InvoiceDetailDialog.kt): Displays cancel/delete action with confirmation dialog, reason input, and cancelled banner when viewing cancelled invoices.

---

## 3. Testing Verification (PART 7)

### 3.1 Phase 07 Integration & Business Logic Tests
File: [Phase07SafeDeletionAndReversalTest.kt](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/Phase07SafeDeletionAndReversalTest.kt)

| Test Case | Scenario Verified | Status |
|:---|:---|:---:|
| `testPaymentFullyAllocatedVoidRestoresFullInvoiceDue` | Voiding payment restores original invoice due & status to `UNPAID` | **PASS** |
| `testPaymentPartiallyAllocatedVoidRestoresInvoiceDue` | Voiding partial payment restores unpaid remainder accurately | **PASS** |
| `testPaymentVoidDuplicatePrevention` | Calling void on an already voided payment returns false (idempotent) | **PASS** |
| `testCustomerWithOutstandingDueBlocksDeletion` | Customer with unpaid balance cannot be deleted or archived | **PASS** |
| `testCustomerWithZeroDueAndHistoricalInvoicesIsArchived` | Customer with historical invoices is archived (`isActive = false`) | **PASS** |
| `testPostedInvoiceCancellationReversesSalesAndAR` | Invoice cancellation creates balancing GL entries for AR and Revenue | **PASS** |
| `testInvoiceDispatchedAndUndeliveredStockRestoration` | Only net dispatched units are restored to warehouse inventory | **PASS** |
| `testFifoRestorationOnCancellation` | Consumed FIFO cost layer remaining quantities are restored | **PASS** |
| `testInvoiceWithFullPaymentConvertsToCustomerAdvance` | Fully paid cancelled invoice converts payment to advance (`coa-2030`) | **PASS** |
| `testInvoiceWithPartialPaymentConvertsToCustomerAdvance` | Partially paid cancelled invoice converts partial payment to advance | **PASS** |
| `testDuplicateInvoiceCancellationPrevention` | Re-attempting to cancel an already cancelled invoice is rejected | **PASS** |
| `testUnifiedAuditTrailIntegrity` | Audit log records entity, document number, reversal ref, and reason | **PASS** |

### 3.2 Database Migration Tests
File: [DatabaseMigration8To9Test.kt](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/DatabaseMigration8To9Test.kt)

| Test Case | Scenario Verified | Status |
|:---|:---|:---:|
| `testMigration8To9VersionNumbers` | `MIGRATION_8_9` start version is 8 and end version is 9 | **PASS** |
| `testMigration8To9ExecutesExpectedSqlStatements` | DDL columns and Uncategorized category seed executed | **PASS** |

### 3.3 Full Suite Test Results
- **Total Test Classes Executed**: 49
- **Total Test Cases**: 215
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0

---

## 4. Build Artifacts

- **APK File**: `app/build/outputs/apk/debug/app-debug.apk`
- **File Size**: 96 MB
- **SHA-256 Checksum**:
  `e7ba462883f39706c01ae9cc1911d7ec05dd2f430b188c67313a1b3dbdc060a2`
