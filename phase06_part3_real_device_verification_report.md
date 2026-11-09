# ADVANCE ERP — Phase 06 Part 3: Real Device Verification & Deployment Report

## 1. Executive Summary
This document provides deployment guidelines, APK integrity verification, and step-by-step verification instructions for deploying the **Phase 06 Part 3: Inventory Cost History, FIFO Engine & Stock Movement Audit** build to physical Android devices.

> [!NOTE]
> Testing was performed in the PocketDev Linux proot headless container environment via automated unit test harnesses, Room SQLite in-memory suites, and Gradle APK builds. Physical device deployment and manual interactive touch-testing require sideloading the generated debug APK via Android Debug Bridge (ADB).

---

## 2. APK Artifact Specification

| Parameter | Value |
| :--- | :--- |
| **Package ID** | `com.pocketdev.advance_erp` |
| **Artifact Path** | `/workspace/advance-erp/app/build/outputs/apk/debug/app-debug.apk` |
| **File Size** | **68,697,681 bytes (66 MB)** |
| **SHA-256 Checksum** | `326f0c27cb7f901d441dff59a29d194009520dedd08f571548f1b6f4e07d51a2` |
| **Target SDK** | Android 15 (API 35) |
| **Min SDK** | Android 9 Pie (API 28) |
| **Database Version** | **8** (via `MIGRATION_7_8`) |

---

## 3. Installation Instructions

### 3.1 Via ADB (Android Debug Bridge)
Connect device with USB Debugging enabled:
```bash
# Verify device connection
adb devices

# Install APK preserving existing application data (Room DB version 7 -> 8 upgrade test)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the Application
adb shell am start -n com.pocketdev.advance_erp/.MainActivity
```

### 3.2 Inspecting Logcat for Migration 7 -> 8
```bash
adb logcat -s AdvanceErpDatabase DatabaseMigrations
```
Expected output:
```text
I/AdvanceErpDatabase: Running migration from 7 to 8...
I/DatabaseMigrations: MIGRATION_7_8 completed successfully.
```

---

## 4. Manual On-Device Verification Runbook

### Step 1: Verify Product Cost History Dialog
1. Launch ADVANCE ERP.
2. Navigate to **Products** screen (`/products`).
3. Tap any existing product (e.g. `Shoe A`).
4. In the Product Details dialog, observe the new **"Cost History & FIFO Layers"** card.
5. Tap **"View Cost History & Layers"**.
6. Verify:
   - Current Stock, Weighted Avg Landed Cost, and Total Valuation metrics render correctly.
   - Active layers appear with initial vs remaining progress bars and landed cost breakdowns.
   - Filter switch toggles between Active Layers and All Layers.

### Step 2: Test Landed Cost Adjustment
1. Inside the Cost History Dialog, tap **"Adjust Cost"** on an active layer.
2. In the adjustment modal, enter a new landed unit cost (e.g. $+10$ BDT/unit) and enter reason (e.g. `Late freight bill`).
3. Observe live calculation of variance, remaining inventory impact, and sold COGS impact.
4. Tap **"Post Adjustment"**.
5. Verify:
   - Success toast appears.
   - Layer unit landed cost updates.
   - General Ledger shows balanced journal (Dr 1210, Dr 5010, Cr 2010).

### Step 3: Verify 5-Way Reconciliation Tool
1. Navigate to **Inventory** screen (`/inventory`).
2. Swipe the top tab bar to **"Reconciliation"** tab.
3. Tap **"Run Audit"**.
4. Verify:
   - Audit runs across all active products.
   - Overview cards display Physical Stock Balance, Movement Ledger Qty, FIFO Layers Qty, Total Valuation, and GL Account 1210 Balance.
   - Discrepancy badge displays green `0 Discrepancies` (or flags products with discrepancies).
5. Tap **"PDF"** to export printable audit report. Verify PDF opens in Android viewer.

### Step 4: Verify Inventory Valuation Breakdown
1. Switch to **"Valuation"** tab on Inventory screen.
2. Observe 3 category cards:
   - **Sellable Stock** (Green card, Qty & Value in BDT).
   - **Damaged Stock** (Red card, Qty & Accumulated Loss).
   - **Total FIFO Valuation** (Purple card).
3. Check warehouse-by-warehouse breakdown table.
4. Tap **"Export PDF"** and confirm generated PDF document in English / Bengali.

### Step 5: Verify Movement Ledger & COGS Traceability
1. Switch to **"Movements"** tab on Inventory screen.
2. Verify all receipts, sales, and adjustments show:
   - Date & time
   - Signed quantity ($+Q$ green, $-Q$ red)
   - Running balance after movement
   - Total cost impact
3. Switch to **"COGS Trace"** tab.
4. Tap a recent sales invoice and expand items to verify exact FIFO cost layers and batch receipt numbers consumed.
