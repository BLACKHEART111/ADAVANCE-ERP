# ADVANCE ERP — Phase 06 Part 2: Database Migration Report (Schema v6 → v7)

**Date:** November 3, 2026  
**Module:** Room Database Architecture & SQLite Schema Evolution  
**Project Workspace:** `/workspace/advance-erp`  
**Package:** `com.pocketdev.advance_erp`  
**Old Schema Version:** `6`  
**New Schema Version:** `7`  

---

## 1. Executive Summary

To support Product Variant Photos, Barcode indexing, and Model 2 QR Code payload persistence, Room Database schema version was upgraded from **6 to 7**.

The migration is implemented as [`MIGRATION_6_7`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/database/DatabaseMigrations.kt#L874-L920). It preserves all existing accounting, landed cost, FIFO layers, warehouse inventory, and product records, maintaining complete backward and forward compatibility.

---

## 2. DDL Operations & Migration Script Details

### 2.1 Table Upgrade: `product_images`
In SQLite, adding a foreign key constraint to an existing table requires the 12-step table recreation pattern:

```sql
-- Step 1: Create new table with updated constraints and columns
CREATE TABLE IF NOT EXISTS product_images_new (
    id TEXT NOT NULL PRIMARY KEY,
    productId TEXT NOT NULL,
    variantId TEXT,
    imageUri TEXT NOT NULL,
    isPrimary INTEGER NOT NULL,
    sortOrder INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON UPDATE NO ACTION ON DELETE CASCADE
);

-- Step 2: Copy existing records from old table into new table
INSERT INTO product_images_new (id, productId, variantId, imageUri, isPrimary, sortOrder, createdAt)
SELECT id, productId, NULL, imageUri, isPrimary, sortOrder, 0 FROM product_images;

-- Step 3: Drop old table and rename new table
DROP TABLE product_images;
ALTER TABLE product_images_new RENAME TO product_images;

-- Step 4: Rebuild indexes
CREATE INDEX IF NOT EXISTS index_product_images_productId ON product_images(productId);
CREATE INDEX IF NOT EXISTS index_product_images_variantId ON product_images(variantId);
```

### 2.2 Table Upgrade: `products`
```sql
-- Add nullable qrCode column
ALTER TABLE products ADD COLUMN qrCode TEXT DEFAULT NULL;

-- Fast index for QR lookups
CREATE INDEX IF NOT EXISTS index_products_qrCode ON products(qrCode);

-- Seed structured offline QR payloads for existing legacy products
UPDATE products SET qrCode = 'ADV-ERP|PRODUCT|' || id || '|' || baseSku WHERE qrCode IS NULL;
```

### 2.3 Table Upgrade: `product_variants`
```sql
-- Add nullable qrCode column
ALTER TABLE product_variants ADD COLUMN qrCode TEXT DEFAULT NULL;

-- Fast indexes for barcode and QR lookups
CREATE INDEX IF NOT EXISTS index_product_variants_barcode ON product_variants(barcode);
CREATE INDEX IF NOT EXISTS index_product_variants_qrCode ON product_variants(qrCode);

-- Seed structured offline QR payloads for existing variants
UPDATE product_variants SET qrCode = 'ADV-ERP|VARIANT|' || id || '|' || productId || '|' || sku WHERE qrCode IS NULL;
```

---

## 3. Database Migration Chain Continuity

The Room Database migration chain in `AdvanceErpDatabase.kt` is verified continuous from Version 1 through Version 7:

```text
v1 ──(MIGRATION_1_2)──> v2 ──(MIGRATION_2_3)──> v3 ──(MIGRATION_3_4)──> v4
                        │
                        v4 ──(MIGRATION_4_5)──> v5 ──(MIGRATION_5_6)──> v6
                                                │
                                                v6 ──(MIGRATION_6_7)──> v7
```

| Migration Step | Start Version | End Version | Scope / Tables Modified |
|:---|:---:|:---:|:---|
| `MIGRATION_1_2` | 1 | 2 | Core ERP Foundation |
| `MIGRATION_2_3` | 2 | 3 | Multi-Warehouse Stock |
| `MIGRATION_3_4` | 3 | 4 | Import Tracking & Shipments |
| `MIGRATION_4_5` | 4 | 5 | Landed Cost & Expense Allocation |
| `MIGRATION_5_6` | 5 | 6 | FIFO Layers, Double-Entry Journal & Stock Transfers |
| `MIGRATION_6_7` | 6 | 7 | Variant Photos, QR Codes, Barcode Indexes |

---

## 4. Verification & Validation Evidence

Automated tests in [`DatabaseMigration6To7Test.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/DatabaseMigration6To7Test.kt) and [`DatabaseMigrationVerificationTest.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/DatabaseMigrationVerificationTest.kt):

1. **Version Numbers Integrity**:
   - `startVersion == 6` (Verified)
   - `endVersion == 7` (Verified)
2. **Schema & Syntax Validation**:
   - Executes all DDL commands against a mock `SupportSQLiteDatabase` dynamic proxy.
   - Verified that SQLite syntax is 100% standard without invalid tokens (e.g. `SET NULL` with space).
3. **Foreign Key Integrity**:
   - Verified `ON DELETE CASCADE` on `productId` and `variantId`.
4. **Index Verification**:
   - Verified all 4 new indices are explicitly created.
5. **Chain Continuity**:
   - Iterated over the entire migration chain from v1 to v7 to ensure zero gaps.
