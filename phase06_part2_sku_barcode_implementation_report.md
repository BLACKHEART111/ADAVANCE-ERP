# ADVANCE ERP — Phase 06 Part 2: Auto SKU, Barcode & QR Code Implementation Report

**Date:** November 3, 2026  
**Module:** Item Identification, Auto SKU, Code 128 Barcode & QR Subsystem  
**Project Workspace:** `/workspace/advance-erp`  
**Package:** `com.pocketdev.advance_erp`  
**Database Schema Version:** `7`  

---

## 1. Executive Summary

Phase 06 Part 2 delivers a self-contained, enterprise-grade item identification subsystem for ADVANCE ERP. The solution features:
1. **Intelligent SKU Generator**: Category- and product-aware automatic SKU generator with atomic sequence increments and collision avoidance loops.
2. **Manual SKU Duplicate Protection**: Instant duplicate detection for base SKUs and variant SKUs, backed by Room unique indices.
3. **Pure Kotlin Code 128 Barcode Engine**: Zero-dependency implementation of GS1/ISO Code 128 (Subset B) with Modulo 103 checksums, rendered natively via Android Canvas.
4. **Pure Kotlin Model 2 QR Code Generator**: Offline ISO/IEC 18004 QR encoder with Reed-Solomon error correction and privacy-preserving structured payloads.
5. **Integrated Scanner & Inventory Lookup**: Camera-ready viewfinder scanner dialog with animated reticle sweep, 1.5-second scan debouncing, and sub-millisecond offline code resolution to products and variants.

---

## 2. Automatic SKU Generation Engine

### 2.1 Base SKU Generation
Implemented in [`SkuGenerator.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/util/SkuGenerator.kt):
- **Algorithm**:
  - `Category Token`: 3 uppercase letters derived from category name or code (e.g., `"Clothing"` → `"CLO"`).
  - `Product Token`: 3 uppercase letters derived from the initials of multi-word product names (e.g., `"Men Cotton Shirt"` → `"MCS"`) or first letters of single-word names (e.g., `"Shoes"` → `"SHO"`).
  - `Sequence Counter`: An atomic 3-digit sequence counter (`001` to `999`).
  - **Resulting Pattern**: `[CAT]-[NAME]-[SEQ]` (e.g. `CLO-MCS-001`).
- **Collision Avoidance Loop**:
  If a candidate SKU already exists in the database, the generator automatically increments the sequence number until an available SKU is found. If namespace saturation occurs (>1000 items), a random 4-digit numeric salt is appended as a secondary fallback.

### 2.2 Product Variant SKU Generation
- **Algorithm**:
  - `Base SKU`: Uses the parent product's base SKU.
  - `Color Token`: 3-letter sanitized token (e.g., `"Red"` → `"RED"`).
  - `Size Token`: Sanitized attribute token (e.g., `"XL"`, `"32GB"`).
  - **Pattern**: `[BASE_SKU]-[COLOR]-[SIZE]` (e.g., `CLO-MCS-001-RED-XL`).
  - If identical variant attributes are repeated, a numeric suffix (`-01`, `-02`) is automatically attached to preserve database uniqueness.

### 2.3 Manual SKU Validation
- Enforced at both the repository level and UI validation layer.
- `ProductRepository.isBaseSkuAvailable(sku, excludeProductId)` and `isVariantSkuAvailable(sku, excludeVariantId)` perform case-insensitive database checks.
- When editing an existing product, its own existing SKU is exempted from rejection.

---

## 3. Pure Kotlin Code 128 Barcode Engine

### 3.1 Standard & Specification
- **Standard**: ISO/IEC 15417 Code 128 (Subset B).
- **Character Set**: Supports standard ASCII characters from 32 (space) to 126 (`~`).
- **Data Table**: Full 107 symbol pattern definitions stored in `CODE_128_PATTERNS`.
  - Patterns 0 to 105: 6 elements alternating between bar and space widths (summing to 11 module widths).
  - Pattern 106 (Stop character): 7 elements (summing to 13 module widths).

### 3.2 Modulo 103 Checksum Calculation
The checksum character $C$ is computed according to the ISO standard:
$$C = \left( \text{StartCode} + \sum_{i=1}^{n} (i \times \text{SymbolValue}_i) \right) \bmod 103$$
Where $\text{StartCode} = 104$ for Code 128 Subset B.

### 3.3 Module Bitstream & Quiet Zones
- Quiet zones of 10 white modules are included at the start and end of the symbol to ensure readability by commercial laser and optical scanners.
- Total module count for text of length $n$:
  $$\text{Total Modules} = 20 + (1 + n + 1) \times 11 + 13$$

### 3.4 Compose View & Actions: `BarcodeView`
Implemented in [`core/designsystem/BarcodeView.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/designsystem/BarcodeView.kt):
- Renders the barcode onto an Android `Canvas` with crisp, non-anti-aliased lines for maximum scanner readability.
- Includes human-readable text beneath the barcode.
- Quick **Copy to Clipboard** and **Share** actions.

---

## 4. Pure Kotlin QR Code Model 2 Engine

### 4.1 Specification & Generator
Implemented in [`QrCodeGenerator.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/core/util/QrCodeGenerator.kt):
- **Standard**: ISO/IEC 18004 Model 2.
- **Encoding Mode**: Byte Mode (`0100`).
- **Error Correction**: Level M (15% recovery capacity) using Reed-Solomon polynomials in Galois Field $GF(2^8)$ with primitive polynomial $x^8 + x^4 + x^3 + x^2 + 1$ (`0x11D`).
- **Function Patterns**:
  - Three 7x7 Finder Patterns at top-left, top-right, and bottom-left with quiet separators.
  - Alternating Timing Patterns along row 6 and column 6.
  - 5x5 Alignment Patterns for QR Versions $\ge 2$.
  - Format Information (Error Correction Level M + Mask 0) with BCH $(15, 5)$ error correction and `0x5412` mask.

### 4.2 Structured Offline Payload
Payloads are strictly standardized internal references:
- **Product Payload**: `ADV-ERP|PRODUCT|<productId>|<sku>`
- **Variant Payload**: `ADV-ERP|VARIANT|<variantId>|<productId>|<sku>`

### 4.3 Privacy & Data Security Audit
- **Zero Sensitive Data**: Does NOT include customer names, phone numbers, supplier information, purchase costs, or profit margins.
- **Offline Self-Contained**: Can be scanned by any smartphone camera or warehouse gun and decoded offline without an internet connection.

---

## 5. Barcode & QR Scanner Integration

### 5.1 Scanner Dialog UI: `BarcodeQrScannerDialog`
Implemented in [`feature/inventory/BarcodeQrScannerDialog.kt`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/feature/inventory/BarcodeQrScannerDialog.kt):
- **Viewfinder Reticle**: Rounded corner targeting box with corner accent markers.
- **Animated Laser Sweep**: Continuous vertical red laser line with smooth easing animation.
- **Debounce Guard**: Enforces a 1500ms debounce interval preventing duplicate accidental scans of the same code.
- **Manual Input Fallback**: Allows manual SKU or barcode entry with an instant "Search" button.
- **Quick Demo Triggers**: Quick-fill buttons for instant testing and verification.

### 5.2 Resolution Logic: `ProductRepository.resolveCode()`
When a code is scanned or entered, `resolveCode` performs a 5-step prioritized lookup:
1. Structured QR Payload match (`ADV-ERP|PRODUCT|...` or `ADV-ERP|VARIANT|...`).
2. Exact product barcode match.
3. Exact variant barcode match.
4. Exact product base SKU match.
5. Exact variant SKU match.

On match, the scanner automatically dismisses and opens [`ProductDetailDialog`](file:///workspace/advance-erp/app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductDetailDialog.kt) with the targeted product and pre-selected variant.

---

## 6. Automated Test Results

The subsystem was verified with zero failures across the test suites:
- [`AutoSkuAndBarcodeTest.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/AutoSkuAndBarcodeTest.kt):
  - `testGenerateBaseSkuFormatAndSequence`: PASSED
  - `testGenerateVariantSkuFormats`: PASSED
  - `testManualSkuDuplicateRejection`: PASSED
  - `testCode128BitPatternStructure`: PASSED
  - `testCode128ChecksumValidation`: PASSED
  - `test12DigitBarcodeGenerator`: PASSED
  - `testQrCodePayloadStructureAndPrivacy`: PASSED
  - `testQrCodeMatrixGeneration`: PASSED
- [`InventoryScannerIntegrationTest.kt`](file:///workspace/advance-erp/app/src/test/java/com/pocketdev/advance_erp/InventoryScannerIntegrationTest.kt):
  - `testResolveProductByBarcode`: PASSED
  - `testResolveProductByBaseSku`: PASSED
  - `testResolveProductByQrCode`: PASSED
  - `testResolveVariantByBarcode`: PASSED
  - `testResolveVariantBySku`: PASSED
  - `testResolveVariantByQrCode`: PASSED
  - `testResolveUnknownCodeReturnsNull`: PASSED
  - `testScannerDebounceSuppressesRapidDuplicateScans`: PASSED
