package com.pocketdev.advance_erp.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. purchase_orders
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id TEXT NOT NULL PRIMARY KEY,
                    poNumber TEXT NOT NULL,
                    supplierId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    currency TEXT NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    totalAmountMinor INTEGER NOT NULL,
                    totalBdtAmountMinor INTEGER NOT NULL,
                    paidAmountMinor INTEGER NOT NULL,
                    paidBdtAmountMinor INTEGER NOT NULL,
                    orderDate INTEGER NOT NULL,
                    expectedArrivalDate INTEGER,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_orders_supplierId ON purchase_orders(supplierId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_orders_poNumber ON purchase_orders(poNumber)")

            // 2. purchase_order_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS purchase_order_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    purchaseOrderId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    quantity INTEGER NOT NULL,
                    unitPriceMinor INTEGER NOT NULL,
                    totalAmountMinor INTEGER NOT NULL,
                    totalBdtAmountMinor INTEGER NOT NULL,
                    receivedQuantity INTEGER NOT NULL,
                    FOREIGN KEY(purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_order_items_purchaseOrderId ON purchase_order_items(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_order_items_productId ON purchase_order_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_order_items_variantId ON purchase_order_items(variantId)")

            // 3. supplier_payments
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS supplier_payments (
                    id TEXT NOT NULL PRIMARY KEY,
                    paymentNumber TEXT NOT NULL,
                    supplierId TEXT NOT NULL,
                    purchaseOrderId TEXT,
                    amountMinor INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    bdtAmountMinor INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    paymentAccount TEXT NOT NULL,
                    referenceNumber TEXT,
                    notes TEXT,
                    paymentDate INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE RESTRICT,
                    FOREIGN KEY(purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_payments_supplierId ON supplier_payments(supplierId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_payments_purchaseOrderId ON supplier_payments(purchaseOrderId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_supplier_payments_paymentNumber ON supplier_payments(paymentNumber)")

            // 4. import_shipments
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS import_shipments (
                    id TEXT NOT NULL PRIMARY KEY,
                    shipmentNumber TEXT NOT NULL,
                    supplierId TEXT NOT NULL,
                    purchaseOrderId TEXT,
                    shippingMethod TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    trackingReference TEXT,
                    shippingLine TEXT,
                    shippingDate INTEGER NOT NULL,
                    estimatedArrivalDate INTEGER NOT NULL,
                    actualArrivalDate INTEGER,
                    status TEXT NOT NULL,
                    totalCartons INTEGER NOT NULL,
                    totalQuantity INTEGER NOT NULL,
                    totalGrossWeightKg TEXT NOT NULL,
                    totalNetWeightKg TEXT NOT NULL,
                    totalCbm TEXT NOT NULL,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE RESTRICT,
                    FOREIGN KEY(purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_shipments_supplierId ON import_shipments(supplierId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_shipments_purchaseOrderId ON import_shipments(purchaseOrderId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_import_shipments_shipmentNumber ON import_shipments(shipmentNumber)")

            // 5. shipment_cartons
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS shipment_cartons (
                    id TEXT NOT NULL PRIMARY KEY,
                    shipmentId TEXT NOT NULL,
                    cartonNumber TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    quantity INTEGER NOT NULL,
                    lengthCm TEXT NOT NULL,
                    widthCm TEXT NOT NULL,
                    heightCm TEXT NOT NULL,
                    grossWeightKg TEXT NOT NULL,
                    netWeightKg TEXT NOT NULL,
                    cbm TEXT NOT NULL,
                    FOREIGN KEY(shipmentId) REFERENCES import_shipments(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shipment_cartons_shipmentId ON shipment_cartons(shipmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shipment_cartons_productId ON shipment_cartons(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shipment_cartons_variantId ON shipment_cartons(variantId)")

            // 6. import_cost_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS import_cost_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    shipmentId TEXT NOT NULL,
                    costCategory TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    bdtAmountMinor INTEGER NOT NULL,
                    costDate INTEGER NOT NULL,
                    paymentAccount TEXT NOT NULL,
                    documentReference TEXT,
                    allocationMethod TEXT NOT NULL,
                    isPosted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(shipmentId) REFERENCES import_shipments(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_cost_items_shipmentId ON import_cost_items(shipmentId)")

            // 7. landed_cost_allocations
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS landed_cost_allocations (
                    id TEXT NOT NULL PRIMARY KEY,
                    shipmentId TEXT NOT NULL,
                    costItemId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    allocationBasis TEXT NOT NULL,
                    basisValue TEXT NOT NULL,
                    basisPercentage TEXT NOT NULL,
                    allocatedBdtMinor INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(shipmentId) REFERENCES import_shipments(id) ON DELETE CASCADE,
                    FOREIGN KEY(costItemId) REFERENCES import_cost_items(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landed_cost_allocations_shipmentId ON landed_cost_allocations(shipmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landed_cost_allocations_costItemId ON landed_cost_allocations(costItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landed_cost_allocations_productId ON landed_cost_allocations(productId)")

            // 8. landed_cost_snapshots
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS landed_cost_snapshots (
                    id TEXT NOT NULL PRIMARY KEY,
                    shipmentId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    productNameEn TEXT NOT NULL,
                    productNameBn TEXT NOT NULL,
                    sku TEXT NOT NULL,
                    totalReceivedQuantity INTEGER NOT NULL,
                    totalPurchaseCostBdtMinor INTEGER NOT NULL,
                    totalAllocatedCostsBdtMinor INTEGER NOT NULL,
                    totalLandedCostBdtMinor INTEGER NOT NULL,
                    unitLandedCostBdtMinor INTEGER NOT NULL,
                    snapshotDate INTEGER NOT NULL,
                    isFinal INTEGER NOT NULL,
                    FOREIGN KEY(shipmentId) REFERENCES import_shipments(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landed_cost_snapshots_shipmentId ON landed_cost_snapshots(shipmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_landed_cost_snapshots_productId ON landed_cost_snapshots(productId)")

            // 9. warehouse_receipts
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS warehouse_receipts (
                    id TEXT NOT NULL PRIMARY KEY,
                    receiptNumber TEXT NOT NULL,
                    shipmentId TEXT NOT NULL,
                    warehouseId TEXT NOT NULL,
                    receiptDate INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    receivedBy TEXT NOT NULL,
                    hasDiscrepancies INTEGER NOT NULL,
                    discrepancyApproved INTEGER NOT NULL,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    postedAt INTEGER,
                    FOREIGN KEY(shipmentId) REFERENCES import_shipments(id) ON DELETE RESTRICT,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipts_shipmentId ON warehouse_receipts(shipmentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipts_warehouseId ON warehouse_receipts(warehouseId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_warehouse_receipts_receiptNumber ON warehouse_receipts(receiptNumber)")

            // 10. warehouse_receipt_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS warehouse_receipt_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    receiptId TEXT NOT NULL,
                    cartonId TEXT,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    expectedQuantity INTEGER NOT NULL,
                    receivedQuantity INTEGER NOT NULL,
                    damagedQuantity INTEGER NOT NULL,
                    missingQuantity INTEGER NOT NULL,
                    excessQuantity INTEGER NOT NULL,
                    unitLandedCostMinor INTEGER NOT NULL,
                    conditionNotes TEXT,
                    FOREIGN KEY(receiptId) REFERENCES warehouse_receipts(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipt_items_receiptId ON warehouse_receipt_items(receiptId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipt_items_productId ON warehouse_receipt_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipt_items_variantId ON warehouse_receipt_items(variantId)")

            // 11. inventory_cost_layers
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory_cost_layers (
                    id TEXT NOT NULL PRIMARY KEY,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    warehouseId TEXT NOT NULL,
                    shipmentId TEXT NOT NULL,
                    shipmentNumber TEXT NOT NULL,
                    warehouseReceiptId TEXT NOT NULL,
                    receiptNumber TEXT NOT NULL,
                    initialQuantity INTEGER NOT NULL,
                    remainingQuantity INTEGER NOT NULL,
                    unitLandedCostBdtMinor INTEGER NOT NULL,
                    originalCurrency TEXT NOT NULL,
                    originalPurchasePriceMinor INTEGER NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    allocatedExpensePerUnitBdtMinor INTEGER NOT NULL,
                    receiptDate INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON DELETE SET NULL,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_productId ON inventory_cost_layers(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_variantId ON inventory_cost_layers(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_warehouseId ON inventory_cost_layers(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_shipmentNumber ON inventory_cost_layers(shipmentNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_receiptNumber ON inventory_cost_layers(receiptNumber)")

            // 12. chart_of_accounts
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chart_of_accounts (
                    id TEXT NOT NULL PRIMARY KEY,
                    accountCode TEXT NOT NULL,
                    nameEn TEXT NOT NULL,
                    nameBn TEXT NOT NULL,
                    accountType TEXT NOT NULL,
                    normalBalance TEXT NOT NULL,
                    description TEXT,
                    isActive INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_chart_of_accounts_accountCode ON chart_of_accounts(accountCode)")

            // 13. journal_entries
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id TEXT NOT NULL PRIMARY KEY,
                    entryNumber TEXT NOT NULL,
                    entryDate INTEGER NOT NULL,
                    descriptionEn TEXT NOT NULL,
                    descriptionBn TEXT NOT NULL,
                    referenceType TEXT NOT NULL,
                    referenceId TEXT,
                    totalDebitMinor INTEGER NOT NULL,
                    totalCreditMinor INTEGER NOT NULL,
                    isPosted INTEGER NOT NULL,
                    isReversed INTEGER NOT NULL,
                    reversalOfEntryId TEXT,
                    reversalEntryId TEXT,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_entryNumber ON journal_entries(entryNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_referenceType ON journal_entries(referenceType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_referenceId ON journal_entries(referenceId)")

            // 14. journal_lines
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS journal_lines (
                    id TEXT NOT NULL PRIMARY KEY,
                    journalEntryId TEXT NOT NULL,
                    accountId TEXT NOT NULL,
                    accountCode TEXT NOT NULL,
                    accountName TEXT NOT NULL,
                    isDebit INTEGER NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    description TEXT,
                    FOREIGN KEY(journalEntryId) REFERENCES journal_entries(id) ON DELETE CASCADE,
                    FOREIGN KEY(accountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_lines_journalEntryId ON journal_lines(journalEntryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_lines_accountId ON journal_lines(accountId)")

            // Seed Standard Chart of Accounts
            seedStandardAccounts(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. sales_invoices
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sales_invoices (
                    id TEXT NOT NULL PRIMARY KEY,
                    invoiceNumber TEXT NOT NULL,
                    customerId TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    customerPhone TEXT,
                    warehouseId TEXT NOT NULL,
                    warehouseName TEXT NOT NULL,
                    status TEXT NOT NULL,
                    subtotalMinor INTEGER NOT NULL,
                    discountMinor INTEGER NOT NULL,
                    taxMinor INTEGER NOT NULL,
                    totalAmountMinor INTEGER NOT NULL,
                    paidAmountMinor INTEGER NOT NULL,
                    dueAmountMinor INTEGER NOT NULL,
                    totalCogsMinor INTEGER NOT NULL,
                    invoiceDate INTEGER NOT NULL,
                    dueDate INTEGER,
                    notes TEXT,
                    isPosted INTEGER NOT NULL,
                    postedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE RESTRICT,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_invoices_invoiceNumber ON sales_invoices(invoiceNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoices_customerId ON sales_invoices(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoices_warehouseId ON sales_invoices(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoices_status ON sales_invoices(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoices_invoiceDate ON sales_invoices(invoiceDate)")

            // 2. sales_invoice_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sales_invoice_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    invoiceId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    productNameEn TEXT NOT NULL,
                    productNameBn TEXT NOT NULL,
                    sku TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    returnedQuantity INTEGER NOT NULL,
                    unitPriceMinor INTEGER NOT NULL,
                    discountMinor INTEGER NOT NULL,
                    lineTotalMinor INTEGER NOT NULL,
                    unitCostMinor INTEGER NOT NULL,
                    totalCogsMinor INTEGER NOT NULL,
                    FOREIGN KEY(invoiceId) REFERENCES sales_invoices(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoice_items_invoiceId ON sales_invoice_items(invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoice_items_productId ON sales_invoice_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_invoice_items_variantId ON sales_invoice_items(variantId)")

            // 3. sales_cost_layer_usages
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sales_cost_layer_usages (
                    id TEXT NOT NULL PRIMARY KEY,
                    invoiceId TEXT NOT NULL,
                    invoiceItemId TEXT NOT NULL,
                    costLayerId TEXT NOT NULL,
                    quantityDepleted INTEGER NOT NULL,
                    unitCostMinor INTEGER NOT NULL,
                    FOREIGN KEY(invoiceId) REFERENCES sales_invoices(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_cost_layer_usages_invoiceId ON sales_cost_layer_usages(invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_cost_layer_usages_invoiceItemId ON sales_cost_layer_usages(invoiceItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_cost_layer_usages_costLayerId ON sales_cost_layer_usages(costLayerId)")

            // 4. customer_payments
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_payments (
                    id TEXT NOT NULL PRIMARY KEY,
                    paymentNumber TEXT NOT NULL,
                    customerId TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    allocatedAmountMinor INTEGER NOT NULL,
                    unallocatedCreditMinor INTEGER NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    accountId TEXT NOT NULL,
                    referenceNote TEXT,
                    isAdvance INTEGER NOT NULL,
                    isPosted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_customer_payments_paymentNumber ON customer_payments(paymentNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_customerId ON customer_payments(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_paymentDate ON customer_payments(paymentDate)")

            // 5. payment_allocations
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_allocations (
                    id TEXT NOT NULL PRIMARY KEY,
                    paymentId TEXT NOT NULL,
                    invoiceId TEXT NOT NULL,
                    invoiceNumber TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    allocatedAt INTEGER NOT NULL,
                    FOREIGN KEY(paymentId) REFERENCES customer_payments(id) ON DELETE CASCADE,
                    FOREIGN KEY(invoiceId) REFERENCES sales_invoices(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_paymentId ON payment_allocations(paymentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_invoiceId ON payment_allocations(invoiceId)")

            // 6. sales_returns
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sales_returns (
                    id TEXT NOT NULL PRIMARY KEY,
                    returnNumber TEXT NOT NULL,
                    invoiceId TEXT NOT NULL,
                    invoiceNumber TEXT NOT NULL,
                    customerId TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    warehouseId TEXT NOT NULL,
                    totalReturnAmountMinor INTEGER NOT NULL,
                    totalCogsReversalMinor INTEGER NOT NULL,
                    refundOption TEXT NOT NULL,
                    reason TEXT,
                    returnDate INTEGER NOT NULL,
                    isPosted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(invoiceId) REFERENCES sales_invoices(id) ON DELETE RESTRICT,
                    FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE RESTRICT,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_returns_returnNumber ON sales_returns(returnNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_returns_invoiceId ON sales_returns(invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_returns_customerId ON sales_returns(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_returns_warehouseId ON sales_returns(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_returns_returnDate ON sales_returns(returnDate)")

            // 7. sales_return_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sales_return_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    returnId TEXT NOT NULL,
                    invoiceItemId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    productNameEn TEXT NOT NULL,
                    productNameBn TEXT NOT NULL,
                    returnQuantity INTEGER NOT NULL,
                    unitPriceMinor INTEGER NOT NULL,
                    returnTotalMinor INTEGER NOT NULL,
                    originalUnitCostMinor INTEGER NOT NULL,
                    cogsReversalMinor INTEGER NOT NULL,
                    FOREIGN KEY(returnId) REFERENCES sales_returns(id) ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_return_items_returnId ON sales_return_items(returnId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_return_items_productId ON sales_return_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_return_items_invoiceItemId ON sales_return_items(invoiceItemId)")

            // Seed Phase 03 standard accounts
            seedStandardAccounts(db)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. expense_vouchers
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS expense_vouchers (
                    id TEXT NOT NULL PRIMARY KEY,
                    voucherNumber TEXT NOT NULL,
                    category TEXT NOT NULL,
                    descriptionEn TEXT NOT NULL,
                    descriptionBn TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    bdtAmountMinor INTEGER NOT NULL,
                    paymentAccountId TEXT NOT NULL,
                    expenseAccountId TEXT NOT NULL,
                    voucherDate INTEGER NOT NULL,
                    referenceNumber TEXT,
                    attachmentPath TEXT,
                    isPosted INTEGER NOT NULL,
                    isReversed INTEGER NOT NULL,
                    journalEntryId TEXT,
                    reversalJournalEntryId TEXT,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(paymentAccountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
                    FOREIGN KEY(expenseAccountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_vouchers_voucherNumber ON expense_vouchers(voucherNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_vouchers_category ON expense_vouchers(category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_vouchers_paymentAccountId ON expense_vouchers(paymentAccountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_vouchers_expenseAccountId ON expense_vouchers(expenseAccountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_vouchers_voucherDate ON expense_vouchers(voucherDate)")

            // 2. account_transfers
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS account_transfers (
                    id TEXT NOT NULL PRIMARY KEY,
                    transferNumber TEXT NOT NULL,
                    sourceAccountId TEXT NOT NULL,
                    destinationAccountId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    transferDate INTEGER NOT NULL,
                    referenceNumber TEXT,
                    notes TEXT,
                    journalEntryId TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(sourceAccountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
                    FOREIGN KEY(destinationAccountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_account_transfers_transferNumber ON account_transfers(transferNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_sourceAccountId ON account_transfers(sourceAccountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_destinationAccountId ON account_transfers(destinationAccountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_transferDate ON account_transfers(transferDate)")

            // 3. bank_reconciliations
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bank_reconciliations (
                    id TEXT NOT NULL PRIMARY KEY,
                    reconciliationNumber TEXT NOT NULL,
                    bankAccountId TEXT NOT NULL,
                    statementStartDate INTEGER NOT NULL,
                    statementEndDate INTEGER NOT NULL,
                    statementOpeningBalanceMinor INTEGER NOT NULL,
                    statementClosingBalanceMinor INTEGER NOT NULL,
                    erpClosingBalanceMinor INTEGER NOT NULL,
                    reconciledBalanceMinor INTEGER NOT NULL,
                    discrepancyMinor INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    notes TEXT,
                    reconciledAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(bankAccountId) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bank_reconciliations_reconciliationNumber ON bank_reconciliations(reconciliationNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_reconciliations_bankAccountId ON bank_reconciliations(bankAccountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_reconciliations_statementEndDate ON bank_reconciliations(statementEndDate)")

            // 4. bank_reconciliation_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bank_reconciliation_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    reconciliationId TEXT NOT NULL,
                    journalLineId TEXT,
                    transactionDate INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    isDebit INTEGER NOT NULL,
                    isMatched INTEGER NOT NULL,
                    statementReference TEXT,
                    matchedAt INTEGER,
                    FOREIGN KEY(reconciliationId) REFERENCES bank_reconciliations(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_reconciliation_items_reconciliationId ON bank_reconciliation_items(reconciliationId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_reconciliation_items_journalLineId ON bank_reconciliation_items(journalLineId)")

            // 5. audit_logs
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    recordType TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL,
                    operator TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_recordType ON audit_logs(recordType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_recordId ON audit_logs(recordId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_action ON audit_logs(action)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_timestamp ON audit_logs(timestamp)")

            // 6. backup_records
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_records (
                    id TEXT NOT NULL PRIMARY KEY,
                    fileName TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    fileSizeBytes INTEGER NOT NULL,
                    checksumSha256 TEXT NOT NULL,
                    schemaVersion INTEGER NOT NULL,
                    isAutomatic INTEGER NOT NULL,
                    notes TEXT,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_backup_records_createdAt ON backup_records(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_backup_records_isAutomatic ON backup_records(isAutomatic)")

            // Seed Phase 04 standard operational accounts
            seedStandardAccounts(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Alter purchase_orders
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN isActualCostFinalized INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN actualCostFinalizedAt INTEGER")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN shippingRatePerKgBdt TEXT NOT NULL DEFAULT '0.00'")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN totalGrossWeightKg TEXT NOT NULL DEFAULT '0.000'")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN totalNetWeightKg TEXT NOT NULL DEFAULT '0.000'")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN totalCbm TEXT NOT NULL DEFAULT '0.000000'")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN totalCartons INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE purchase_orders ADD COLUMN attachments TEXT")

            // 2. Alter purchase_order_items
            db.execSQL("ALTER TABLE purchase_order_items ADD COLUMN cartonQuantity INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE purchase_order_items ADD COLUMN pcsPerCarton INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE purchase_order_items ADD COLUMN grossWeightKg TEXT NOT NULL DEFAULT '0.000'")
            db.execSQL("ALTER TABLE purchase_order_items ADD COLUMN netWeightKg TEXT NOT NULL DEFAULT '0.000'")
            db.execSQL("ALTER TABLE purchase_order_items ADD COLUMN cbm TEXT NOT NULL DEFAULT '0.000000'")

            // 3. Alter shipment_cartons
            db.execSQL("ALTER TABLE shipment_cartons ADD COLUMN purchaseOrderId TEXT")
            db.execSQL("ALTER TABLE shipment_cartons ADD COLUMN isMixedCarton INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shipment_cartons ADD COLUMN totalCartonQuantity INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shipment_cartons ADD COLUMN cartonGrossWeightKg TEXT NOT NULL DEFAULT '0.000'")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shipment_cartons_purchaseOrderId ON shipment_cartons(purchaseOrderId)")

            // 4. Alter import_cost_items
            db.execSQL("ALTER TABLE import_cost_items ADD COLUMN purchaseOrderId TEXT")
            db.execSQL("ALTER TABLE import_cost_items ADD COLUMN isEstimated INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE import_cost_items ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE import_cost_items ADD COLUMN notes TEXT")
            db.execSQL("ALTER TABLE import_cost_items ADD COLUMN attachmentPath TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_cost_items_purchaseOrderId ON import_cost_items(purchaseOrderId)")

            // 5. Alter warehouse_receipts
            db.execSQL("ALTER TABLE warehouse_receipts ADD COLUMN purchaseOrderId TEXT")
            db.execSQL("ALTER TABLE warehouse_receipts ADD COLUMN receivingMethod TEXT NOT NULL DEFAULT 'PHYSICAL_COUNT'")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_receipts_purchaseOrderId ON warehouse_receipts(purchaseOrderId)")

            // 6. Alter inventory_cost_layers
            db.execSQL("ALTER TABLE inventory_cost_layers ADD COLUMN purchaseOrderId TEXT")
            db.execSQL("ALTER TABLE inventory_cost_layers ADD COLUMN purchaseBatchId TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_layers_purchaseOrderId ON inventory_cost_layers(purchaseOrderId)")

            // 7. Create damage_records
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS damage_records (
                    id TEXT NOT NULL PRIMARY KEY,
                    recordNumber TEXT NOT NULL,
                    purchaseOrderId TEXT NOT NULL,
                    warehouseReceiptId TEXT NOT NULL,
                    warehouseId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    damagedQuantity INTEGER NOT NULL,
                    unitCostMinor INTEGER NOT NULL,
                    totalLossMinor INTEGER NOT NULL,
                    reason TEXT,
                    disposition TEXT NOT NULL,
                    journalEntryId TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    FOREIGN KEY (warehouseReceiptId) REFERENCES warehouse_receipts(id) ON DELETE CASCADE,
                    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY (warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_records_purchaseOrderId ON damage_records(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_records_warehouseReceiptId ON damage_records(warehouseReceiptId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_records_productId ON damage_records(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_damage_records_warehouseId ON damage_records(warehouseId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_damage_records_recordNumber ON damage_records(recordNumber)")

            // 8. Create shortage_records
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS shortage_records (
                    id TEXT NOT NULL PRIMARY KEY,
                    recordNumber TEXT NOT NULL,
                    purchaseOrderId TEXT NOT NULL,
                    warehouseReceiptId TEXT NOT NULL,
                    warehouseId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    missingQuantity INTEGER NOT NULL,
                    unitCostMinor INTEGER NOT NULL,
                    totalAmountMinor INTEGER NOT NULL,
                    disposition TEXT NOT NULL,
                    claimReference TEXT,
                    notes TEXT,
                    journalEntryId TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    FOREIGN KEY (warehouseReceiptId) REFERENCES warehouse_receipts(id) ON DELETE CASCADE,
                    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY (warehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shortage_records_purchaseOrderId ON shortage_records(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shortage_records_warehouseReceiptId ON shortage_records(warehouseReceiptId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shortage_records_productId ON shortage_records(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shortage_records_warehouseId ON shortage_records(warehouseId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shortage_records_recordNumber ON shortage_records(recordNumber)")

            // 9. Create additional_product_costs
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS additional_product_costs (
                    id TEXT NOT NULL PRIMARY KEY,
                    costNumber TEXT NOT NULL,
                    purchaseOrderId TEXT NOT NULL,
                    warehouseId TEXT,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    exchangeRateToBdt TEXT NOT NULL,
                    bdtAmountMinor INTEGER NOT NULL,
                    isCapitalizable INTEGER NOT NULL,
                    allocationMethod TEXT NOT NULL,
                    allocatedToInventoryMinor INTEGER NOT NULL,
                    allocatedToCogsMinor INTEGER NOT NULL,
                    expenseDate INTEGER NOT NULL,
                    journalEntryId TEXT,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    FOREIGN KEY (warehouseId) REFERENCES warehouses(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_additional_product_costs_purchaseOrderId ON additional_product_costs(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_additional_product_costs_warehouseId ON additional_product_costs(warehouseId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_additional_product_costs_costNumber ON additional_product_costs(costNumber)")

            // 10. Create purchase_completion_records
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS purchase_completion_records (
                    id TEXT NOT NULL PRIMARY KEY,
                    purchaseOrderId TEXT NOT NULL,
                    completedAt INTEGER NOT NULL,
                    orderedQuantity INTEGER NOT NULL,
                    receivedQuantity INTEGER NOT NULL,
                    goodQuantity INTEGER NOT NULL,
                    damagedQuantity INTEGER NOT NULL,
                    missingQuantity INTEGER NOT NULL,
                    remainingQuantity INTEGER NOT NULL,
                    paymentDueMinor INTEGER NOT NULL,
                    unreceivedDisposition TEXT,
                    reason TEXT,
                    notes TEXT,
                    FOREIGN KEY (purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_completion_records_purchaseOrderId ON purchase_completion_records(purchaseOrderId)")

            // 11. Seed Phase 05 standard operational accounts
            seedStandardAccounts(db)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create stock_transfers table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_transfers (
                    id TEXT NOT NULL PRIMARY KEY,
                    transferNumber TEXT NOT NULL,
                    sourceWarehouseId TEXT NOT NULL,
                    destinationWarehouseId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    quantity INTEGER NOT NULL,
                    transferDate INTEGER NOT NULL,
                    notes TEXT,
                    status TEXT NOT NULL,
                    dispatchedAt INTEGER,
                    receivedAt INTEGER,
                    cancelledAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (sourceWarehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT,
                    FOREIGN KEY (destinationWarehouseId) REFERENCES warehouses(id) ON DELETE RESTRICT,
                    FOREIGN KEY (productId) REFERENCES products(id) ON DELETE RESTRICT,
                    FOREIGN KEY (variantId) REFERENCES product_variants(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_stock_transfers_transferNumber ON stock_transfers(transferNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_sourceWarehouseId ON stock_transfers(sourceWarehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_destinationWarehouseId ON stock_transfers(destinationWarehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_productId ON stock_transfers(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_variantId ON stock_transfers(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_status ON stock_transfers(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_transferDate ON stock_transfers(transferDate)")

            // 2. Create stock_transfer_cost_layers table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_transfer_cost_layers (
                    id TEXT NOT NULL PRIMARY KEY,
                    transferId TEXT NOT NULL,
                    costLayerId TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unitCostMinor INTEGER NOT NULL,
                    FOREIGN KEY (transferId) REFERENCES stock_transfers(id) ON DELETE CASCADE,
                    FOREIGN KEY (costLayerId) REFERENCES inventory_cost_layers(id) ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfer_cost_layers_transferId ON stock_transfer_cost_layers(transferId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfer_cost_layers_costLayerId ON stock_transfer_cost_layers(costLayerId)")

            // 3. Standardize historical stock movement types for backward compatibility
            db.execSQL("UPDATE stock_movements SET movementType = 'SALE_DISPATCH' WHERE movementType = 'SALE'")
            db.execSQL("UPDATE stock_movements SET movementType = 'STOCK_ADJUSTMENT_IN' WHERE movementType = 'ADJUSTMENT_INCREASE'")
            db.execSQL("UPDATE stock_movements SET movementType = 'STOCK_ADJUSTMENT_OUT' WHERE movementType = 'ADJUSTMENT_DECREASE'")
            db.execSQL("UPDATE stock_movements SET movementType = 'PURCHASE_RECEIPT' WHERE movementType = 'IMPORT_RECEIPT'")
            db.execSQL("UPDATE stock_movements SET movementType = 'DAMAGE_WRITE_OFF' WHERE movementType IN ('DAMAGE_EXCLUDED', 'MISSING_IN_TRANSIT', 'SHORTAGE_DISCREPANCY')")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Upgrade product_images table to support variantId and createdAt
            db.execSQL("""
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
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO product_images_new (id, productId, variantId, imageUri, isPrimary, sortOrder, createdAt)
                SELECT id, productId, NULL, imageUri, isPrimary, sortOrder, 0 FROM product_images
            """.trimIndent())

            db.execSQL("DROP TABLE product_images")
            db.execSQL("ALTER TABLE product_images_new RENAME TO product_images")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_images_productId ON product_images(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_images_variantId ON product_images(variantId)")

            // 2. Add qrCode column to products
            db.execSQL("ALTER TABLE products ADD COLUMN qrCode TEXT DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_qrCode ON products(qrCode)")
            db.execSQL("UPDATE products SET qrCode = 'ADV-ERP:PRD:' || id WHERE qrCode IS NULL")

            // 3. Add qrCode column and indexes to product_variants
            db.execSQL("ALTER TABLE product_variants ADD COLUMN qrCode TEXT DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_variants_barcode ON product_variants(barcode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_variants_qrCode ON product_variants(qrCode)")
            db.execSQL("UPDATE product_variants SET qrCode = 'ADV-ERP:VAR:' || id WHERE qrCode IS NULL")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Upgrade stock_movements table with new audit columns
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN balanceAfterMovement INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN totalCostImpactMinor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN purchaseOrderId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN purchaseBatchId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN salesInvoiceId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN stockTransferId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE stock_movements ADD COLUMN operatorName TEXT DEFAULT NULL")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_movementType ON stock_movements(movementType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_purchaseOrderId ON stock_movements(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_salesInvoiceId ON stock_movements(salesInvoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_stockTransferId ON stock_movements(stockTransferId)")

            // Backfill totalCostImpactMinor for existing movements
            db.execSQL("UPDATE stock_movements SET totalCostImpactMinor = quantity * unitCostMinor WHERE totalCostImpactMinor = 0 AND unitCostMinor > 0")

            // 2. Create fifo_consumptions table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS fifo_consumptions (
                    id TEXT NOT NULL PRIMARY KEY,
                    invoiceId TEXT,
                    invoiceItemId TEXT,
                    costLayerId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    warehouseId TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unitCostBdtMinor INTEGER NOT NULL,
                    totalCogsBdtMinor INTEGER NOT NULL,
                    referenceType TEXT NOT NULL DEFAULT 'SALE',
                    referenceId TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(costLayerId) REFERENCES inventory_cost_layers(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_costLayerId ON fifo_consumptions(costLayerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_productId ON fifo_consumptions(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_variantId ON fifo_consumptions(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_warehouseId ON fifo_consumptions(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_invoiceId ON fifo_consumptions(invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_invoiceItemId ON fifo_consumptions(invoiceItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fifo_consumptions_createdAt ON fifo_consumptions(createdAt)")

            // Backfill fifo_consumptions from sales_cost_layer_usages if any exist
            db.execSQL("""
                INSERT OR IGNORE INTO fifo_consumptions (
                    id, invoiceId, invoiceItemId, costLayerId, productId, variantId, warehouseId,
                    quantity, unitCostBdtMinor, totalCogsBdtMinor, referenceType, referenceId, createdAt
                )
                SELECT u.id, u.invoiceId, u.invoiceItemId, u.costLayerId, l.productId, l.variantId, l.warehouseId,
                       u.quantityDepleted, u.unitCostMinor, (u.quantityDepleted * u.unitCostMinor), 'SALE', u.invoiceId, strftime('%s','now') * 1000
                FROM sales_cost_layer_usages u
                INNER JOIN inventory_cost_layers l ON u.costLayerId = l.id
            """.trimIndent())

            // 3. Create inventory_cost_adjustments table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory_cost_adjustments (
                    id TEXT NOT NULL PRIMARY KEY,
                    adjustmentNumber TEXT NOT NULL,
                    costLayerId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    warehouseId TEXT NOT NULL,
                    purchaseOrderId TEXT,
                    referenceType TEXT,
                    referenceId TEXT,
                    reason TEXT NOT NULL,
                    oldUnitCostBdtMinor INTEGER NOT NULL,
                    newUnitCostBdtMinor INTEGER NOT NULL,
                    unitVarianceBdtMinor INTEGER NOT NULL,
                    totalRemainingQty INTEGER NOT NULL,
                    totalSoldQty INTEGER NOT NULL,
                    inventoryAdjustmentAmountMinor INTEGER NOT NULL,
                    cogsAdjustmentAmountMinor INTEGER NOT NULL,
                    totalAdjustmentAmountMinor INTEGER NOT NULL,
                    journalEntryId TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(costLayerId) REFERENCES inventory_cost_layers(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_cost_adjustments_adjustmentNumber ON inventory_cost_adjustments(adjustmentNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_costLayerId ON inventory_cost_adjustments(costLayerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_productId ON inventory_cost_adjustments(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_variantId ON inventory_cost_adjustments(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_warehouseId ON inventory_cost_adjustments(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_purchaseOrderId ON inventory_cost_adjustments(purchaseOrderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_cost_adjustments_createdAt ON inventory_cost_adjustments(createdAt)")

            // 4. Create inventory_reconciliations table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory_reconciliations (
                    id TEXT NOT NULL PRIMARY KEY,
                    reconciliationNumber TEXT NOT NULL,
                    warehouseId TEXT,
                    status TEXT NOT NULL,
                    totalStockBalanceQty INTEGER NOT NULL,
                    totalMovementQty INTEGER NOT NULL,
                    totalFifoRemainingQty INTEGER NOT NULL,
                    totalFifoValuationMinor INTEGER NOT NULL,
                    account1210BalanceMinor INTEGER NOT NULL,
                    quantityDiscrepancy INTEGER NOT NULL,
                    valuationDiscrepancyMinor INTEGER NOT NULL,
                    notes TEXT,
                    reconciledBy TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_reconciliations_reconciliationNumber ON inventory_reconciliations(reconciliationNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliations_warehouseId ON inventory_reconciliations(warehouseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliations_createdAt ON inventory_reconciliations(createdAt)")

            // 5. Create inventory_reconciliation_items table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory_reconciliation_items (
                    id TEXT NOT NULL PRIMARY KEY,
                    reconciliationId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    variantId TEXT,
                    warehouseId TEXT NOT NULL,
                    stockBalanceQty INTEGER NOT NULL,
                    movementNetQty INTEGER NOT NULL,
                    fifoRemainingQty INTEGER NOT NULL,
                    fifoValuationMinor INTEGER NOT NULL,
                    qtyMismatch INTEGER NOT NULL,
                    details TEXT,
                    FOREIGN KEY(reconciliationId) REFERENCES inventory_reconciliations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(variantId) REFERENCES product_variants(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(warehouseId) REFERENCES warehouses(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliation_items_reconciliationId ON inventory_reconciliation_items(reconciliationId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliation_items_productId ON inventory_reconciliation_items(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliation_items_variantId ON inventory_reconciliation_items(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_reconciliation_items_warehouseId ON inventory_reconciliation_items(warehouseId)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. customer_payments void tracking columns
            db.execSQL("ALTER TABLE customer_payments ADD COLUMN isVoided INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE customer_payments ADD COLUMN voidedAt INTEGER")
            db.execSQL("ALTER TABLE customer_payments ADD COLUMN voidReason TEXT")
            db.execSQL("ALTER TABLE customer_payments ADD COLUMN voidedBy TEXT")

            // 2. audit_logs unified audit trail columns
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN documentNumber TEXT")
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN operationType TEXT")
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN previousStatus TEXT")
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN newStatus TEXT")
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN cancellationReason TEXT")
            db.execSQL("ALTER TABLE audit_logs ADD COLUMN reversalReference TEXT")

            // 3. sales_invoices cancellation columns
            db.execSQL("ALTER TABLE sales_invoices ADD COLUMN cancellationReason TEXT")
            db.execSQL("ALTER TABLE sales_invoices ADD COLUMN cancelledAt INTEGER")

            // 4. Default Uncategorized category
            db.execSQL("""
                INSERT OR IGNORE INTO product_categories (id, nameEn, nameBn, code, description)
                VALUES ('cat-uncategorized', 'Uncategorized', 'শ্রেণীবিহীন', 'UNCAT', 'Default fallback category for unassigned products')
            """.trimIndent())
        }
    }

    fun seedStandardAccounts(db: SupportSQLiteDatabase) {
        val accounts = listOf(
            arrayOf("coa-1010", "1010", "Cash in Hand", "নগদ তহবিল", "ASSET", "DEBIT", "Office Petty Cash and Tejgaon Cash Vault"),
            arrayOf("coa-1020", "1020", "City Bank Corporate (Import LC)", "সিটি ব্যাংক কর্পোরেট হিসাব", "ASSET", "DEBIT", "Main import clearing bank account"),
            arrayOf("coa-1030", "1030", "Islami Bank Bangladesh Ltd.", "ইসলামী ব্যাংক বাংলাদেশ লিমিটেড", "ASSET", "DEBIT", "Corporate reserve account"),
            arrayOf("coa-1040", "1040", "Accounts Receivable (Trade Debtors)", "প্রাপ্য হিসাব (ব্যবসায়িক দেনাদার)", "ASSET", "DEBIT", "Customer trade receivables from merchandise sales"),
            arrayOf("coa-1050", "1050", "Supplier Shortage & Claims Receivable", "ঘাটতি ও সরবরাহকারী দাবি", "ASSET", "DEBIT", "Claims against supplier for missing goods or short shipments"),
            arrayOf("coa-1210", "1210", "Inventory Asset (Warehouse)", "মজুদ পণ্য সম্পদ (ওয়্যারহাউস)", "ASSET", "DEBIT", "Capitalized warehouse stock valuation"),
            arrayOf("coa-1220", "1220", "In-Transit Inventory (Imports)", "ট্রানজিটে থাকা পণ্য (আমদানি)", "ASSET", "DEBIT", "Goods dispatched from China before warehouse receiving"),
            arrayOf("coa-2010", "2010", "Accounts Payable (China Suppliers)", "প্রদেয় হিসাব (চীন সরবরাহকারী)", "LIABILITY", "CREDIT", "Trade payables owed to overseas factories"),
            arrayOf("coa-2020", "2020", "Accrued Customs & Freight", "বকেয়া শুল্ক ও ফ্রেইট ব্যয়", "LIABILITY", "CREDIT", "Accruals for pending clearance duties"),
            arrayOf("coa-2030", "2030", "Customer Advances & Deposits", "গ্রাহক অগ্রিম ও আমানত", "LIABILITY", "CREDIT", "Advances received from customers before invoice issuance"),
            arrayOf("coa-3010", "3010", "Owner's Capital & Retained Equity", "মালিকের মূলধন ও সংরক্ষিত তহবিল", "EQUITY", "CREDIT", "Net business equity"),
            arrayOf("coa-3020", "3020", "Retained Earnings (Accumulated Profit)", "সংরক্ষিত আয় ও মুনাফা", "EQUITY", "CREDIT", "Accumulated net profit carried forward from operations"),
            arrayOf("coa-4010", "4010", "Import Merchandise Sales Revenue", "পণ্য বিক্রয় আয়", "REVENUE", "CREDIT", "Gross sales revenue"),
            arrayOf("coa-4020", "4020", "Sales Returns & Allowances", "বিক্রয় ফেরত ও ছাড়", "REVENUE", "DEBIT", "Customer sales returns and price concessions"),
            arrayOf("coa-4030", "4030", "Sales Discounts Allowed", "বিক্রয় বাট্টা", "REVENUE", "DEBIT", "Invoice discounts granted to customers"),
            arrayOf("coa-5010", "5010", "Cost of Goods Sold (COGS)", "বিক্রীত পণ্যের মোট ব্যয়", "EXPENSE", "DEBIT", "Historical landed cost of dispatched merchandise"),
            arrayOf("coa-5020", "5020", "Customs, Tariff & Port Charges", "কাস্টমস, শুল্ক ও পোর্ট চার্জ", "EXPENSE", "DEBIT", "Import duties and terminal clearing"),
            arrayOf("coa-5030", "5030", "International Shipping & Air Freight", "আন্তর্জাতিক শিপিং ও এয়ার ফ্রেইট", "EXPENSE", "DEBIT", "Sea container and air cargo charges"),
            arrayOf("coa-5040", "5040", "Warehouse Handling & Logistics", "ওয়্যারহাউস হ্যান্ডলিং ও লোকাল পরিবহন", "EXPENSE", "DEBIT", "Domestic transport from port to warehouse"),
            arrayOf("coa-5050", "5050", "Damaged Merchandise Write-Off", "নষ্ট পণ্য অবলোপন", "EXPENSE", "DEBIT", "Write-off of damaged goods during transit or receiving"),
            arrayOf("coa-5090", "5090", "Foreign Exchange Gain / Loss", "বৈদেশিক মুদ্রা বিনিময় লাভ/ক্ষতি", "EXPENSE", "DEBIT", "CNY/USD to BDT exchange rate variance"),
            arrayOf("coa-5110", "5110", "Office Rent & Warehouse Utilities", "অফিস ভাড়া ও ওয়্যারহাউস ইউটিলিটি", "EXPENSE", "DEBIT", "Monthly lease, power, water, internet charges"),
            arrayOf("coa-5120", "5120", "Salaries, Wages & Benefits", "কর্মচারী বেতন ও সুবিধা", "EXPENSE", "DEBIT", "Staff payroll, overtime and bonuses"),
            arrayOf("coa-5130", "5130", "China Business Travel & Sourcing", "চীন ভ্রমণ ও সোর্সিং ব্যয়", "EXPENSE", "DEBIT", "Canton Fair, air tickets, hotel, factory visits"),
            arrayOf("coa-5140", "5140", "Marketing, Promotion & Samples", "মার্কেটিং ও স্যাম্পল ব্যয়", "EXPENSE", "DEBIT", "Digital advertising, product samples, catalogs"),
            arrayOf("coa-5150", "5150", "Bank Charges, Commission & LC Fees", "ব্যাংক চার্জ ও এলসি ফি", "EXPENSE", "DEBIT", "Bank service charges, LC opening commissions"),
            arrayOf("coa-5190", "5190", "General & Administrative Expenses", "সাধারণ ও প্রশাসনিক ব্যয়", "EXPENSE", "DEBIT", "Office supplies, legal, miscellaneous expenses")
        )

        db.beginTransaction()
        try {
            accounts.forEach { acc ->
                val escapedNameEn = acc[2].replace("'", "''")
                val escapedNameBn = acc[3].replace("'", "''")
                val escapedDesc = acc[6].replace("'", "''")
                db.execSQL("""
                    INSERT OR IGNORE INTO chart_of_accounts 
                    (id, accountCode, nameEn, nameBn, accountType, normalBalance, description, isActive)
                    VALUES ('${acc[0]}', '${acc[1]}', '$escapedNameEn', '$escapedNameBn', '${acc[4]}', '${acc[5]}', '$escapedDesc', 1)
                """.trimIndent())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        }
    }
