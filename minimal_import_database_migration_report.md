# ADVANCE ERP — Minimal Import Database Migration Report

**Project:** ADVANCE ERP (`com.pocketdev.advance_erp`)  
**Schema Version:** Upgraded from version 4 to version 5  
**Migration Spec:** `AdvanceErpDatabase.MIGRATION_4_5` in `DatabaseMigrations.kt`  
**Date:** November 2026  
**Status:** VERIFIED & MIGRATION TESTED  

---

## 1. Migration Overview

To support the minimal import workflow without breaking existing production databases, Room Database Schema version 5 was introduced. The migration preserves 100% of existing historical data (Chart of Accounts, journal entries, product catalogs, customer/supplier ledgers, sales, and purchase records) while expanding the schema with dedicated tables and columns for packing, landing cost finalization, discrepancies, and purchase completion.

---

## 2. Table Modifications & New Entities

### 2.1 Newly Created Tables

#### `damage_records`
Tracks damaged items identified during warehouse physical or carton-wise receiving.
```sql
CREATE TABLE IF NOT EXISTS `damage_records` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `receiptId` TEXT NOT NULL,
    `purchaseId` TEXT NOT NULL,
    `productId` TEXT NOT NULL,
    `variantId` TEXT,
    `quantity` INTEGER NOT NULL,
    `unitLandedCostMinor` INTEGER NOT NULL,
    `totalCostMinor` INTEGER NOT NULL,
    `reason` TEXT NOT NULL,
    `disposition` TEXT NOT NULL,
    `journalEntryId` TEXT,
    `recordedAt` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_damage_records_purchaseId` ON `damage_records` (`purchaseId`);
CREATE INDEX IF NOT EXISTS `index_damage_records_receiptId` ON `damage_records` (`receiptId`);
```

#### `shortage_records`
Tracks short-shipped or missing goods with claim tracking against suppliers.
```sql
CREATE TABLE IF NOT EXISTS `shortage_records` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `receiptId` TEXT NOT NULL,
    `purchaseId` TEXT NOT NULL,
    `productId` TEXT NOT NULL,
    `variantId` TEXT,
    `expectedQuantity` INTEGER NOT NULL,
    `receivedQuantity` INTEGER NOT NULL,
    `shortageQuantity` INTEGER NOT NULL,
    `unitCostMinor` INTEGER NOT NULL,
    `totalClaimMinor` INTEGER NOT NULL,
    `claimStatus` TEXT NOT NULL,
    `journalEntryId` TEXT,
    `recordedAt` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_shortage_records_purchaseId` ON `shortage_records` (`purchaseId`);
CREATE INDEX IF NOT EXISTS `index_shortage_records_receiptId` ON `shortage_records` (`receiptId`);
```

#### `additional_product_costs`
Records post-arrival operational costs with explicit accounting distinction (Capitalizable vs Period Expense).
```sql
CREATE TABLE IF NOT EXISTS `additional_product_costs` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `purchaseId` TEXT NOT NULL,
    `costName` TEXT NOT NULL,
    `amountMinor` INTEGER NOT NULL,
    `costType` TEXT NOT NULL,
    `targetType` TEXT NOT NULL,
    `targetProductId` TEXT,
    `isCapitalized` INTEGER NOT NULL,
    `journalEntryId` TEXT,
    `recordedAt` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_additional_product_costs_purchaseId` ON `additional_product_costs` (`purchaseId`);
```

#### `purchase_completion_records`
Maintains permanent audit log of manual purchase completions and unreceived balance disposition.
```sql
CREATE TABLE IF NOT EXISTS `purchase_completion_records` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `purchaseId` TEXT NOT NULL,
    `completedAt` INTEGER NOT NULL,
    `completedByUserId` TEXT NOT NULL,
    `unreceivedDisposition` TEXT NOT NULL,
    `unreceivedUnitsTotal` INTEGER NOT NULL,
    `unreceivedValueMinor` INTEGER NOT NULL,
    `closingNotes` TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_purchase_completion_records_purchaseId` ON `purchase_completion_records` (`purchaseId`);
```

---

### 2.2 Altered Existing Tables

#### `purchase_orders`
Added columns to support packing aggregates, shipping rates, and landing cost finalization state:
```sql
ALTER TABLE `purchase_orders` ADD COLUMN `isActualCostFinalized` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `purchase_orders` ADD COLUMN `actualCostFinalizedAt` INTEGER;
ALTER TABLE `purchase_orders` ADD COLUMN `shippingRatePerKgBdt` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_orders` ADD COLUMN `totalGrossWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_orders` ADD COLUMN `totalNetWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_orders` ADD COLUMN `totalCbm` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_orders` ADD COLUMN `totalCartons` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `purchase_orders` ADD COLUMN `attachments` TEXT NOT NULL DEFAULT '';
```

#### `purchase_order_items`
Added item-level packing fields:
```sql
ALTER TABLE `purchase_order_items` ADD COLUMN `cartonCount` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `purchase_order_items` ADD COLUMN `grossWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_order_items` ADD COLUMN `netWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_order_items` ADD COLUMN `cbm` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `purchase_order_items` ADD COLUMN `pcsPerCarton` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `purchase_order_items` ADD COLUMN `unitOfMeasure` TEXT NOT NULL DEFAULT 'PCS';
```

#### `import_shipments`
Enhanced batch shipment entity:
```sql
ALTER TABLE `import_shipments` ADD COLUMN `grossWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `import_shipments` ADD COLUMN `netWeightKg` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `import_shipments` ADD COLUMN `cbm` TEXT NOT NULL DEFAULT '0';
ALTER TABLE `import_shipments` ADD COLUMN `totalCartons` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `import_shipments` ADD COLUMN `shippingRatePerKgBdt` TEXT NOT NULL DEFAULT '0';
```

#### `import_cost_items`
Linked landing cost items to purchases with allocation methodology:
```sql
ALTER TABLE `import_cost_items` ADD COLUMN `purchaseId` TEXT NOT NULL DEFAULT '';
ALTER TABLE `import_cost_items` ADD COLUMN `allocationMethod` TEXT NOT NULL DEFAULT 'GROSS_WEIGHT';
```

#### `warehouse_receipts`
Upgraded receiving record to track physical counts, discrepancy quantities, and purchase linkage:
```sql
ALTER TABLE `warehouse_receipts` ADD COLUMN `purchaseId` TEXT NOT NULL DEFAULT '';
ALTER TABLE `warehouse_receipts` ADD COLUMN `receiptMethod` TEXT NOT NULL DEFAULT 'PHYSICAL_COUNT';
ALTER TABLE `warehouse_receipts` ADD COLUMN `goodQuantity` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `warehouse_receipts` ADD COLUMN `damagedQuantity` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `warehouse_receipts` ADD COLUMN `missingQuantity` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `warehouse_receipts` ADD COLUMN `notes` TEXT NOT NULL DEFAULT '';
```

#### `inventory_cost_layers`
Granular cost breakdown in FIFO inventory cost layers:
```sql
ALTER TABLE `inventory_cost_layers` ADD COLUMN `purchaseId` TEXT NOT NULL DEFAULT '';
ALTER TABLE `inventory_cost_layers` ADD COLUMN `unitPurchaseCostMinor` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `inventory_cost_layers` ADD COLUMN `unitLandedCostMinor` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `inventory_cost_layers` ADD COLUMN `additionalCostMinor` INTEGER NOT NULL DEFAULT 0;
```

---

## 3. Chart of Accounts Seeding

The migration seeds two essential accounting codes in the standard Chart of Accounts:

| Account Code | Account Name | Type | Description |
|---|---|---|---|
| `coa-5050` | Damaged Merchandise Write-Off | **EXPENSE** | Direct P&L charge for transit or warehouse damaged goods. |
| `coa-1050` | Supplier Shortage & Claims Receivable | **ASSET** | Current asset representing short-shipped quantities billed to supplier. |

```sql
INSERT OR IGNORE INTO `accounts` (`id`, `code`, `name`, `type`, `balanceMinor`, `isSystem`, `isActive`, `createdAt`)
VALUES ('coa-5050', '5050', 'Damaged Merchandise Write-Off', 'EXPENSE', 0, 1, 1, 1730000000000);

INSERT OR IGNORE INTO `accounts` (`id`, `code`, `name`, `type`, `balanceMinor`, `isSystem`, `isActive`, `createdAt`)
VALUES ('coa-1050', '1050', 'Supplier Shortage & Claims Receivable', 'ASSET', 0, 1, 1, 1730000000000);
```

---

## 4. Migration Verification & Safety Guarantee

1. **No Data Loss:** All existing columns were preserved. New columns include default zero/blank values to maintain backward compatibility.
2. **Dao Registration:** `MinimalImportDao` registered in `AdvanceErpDatabase` abstract class and `DatabaseModule` for Hilt injection.
3. **SQLite Foreign Key DDL Compliance:** SQLite standard syntax `ON DELETE SET NULL` (with space, strictly adhering to SQLite's formal grammar) is verified across all migration scripts.
4. **Automated Verification:** Verified via `DatabaseMigrationVerificationTest` (including `testAllMigrationsExecuteWithoutSyntaxErrorsOrInvalidTokens`), executing all DDL statements cleanly with 0 syntax errors.
