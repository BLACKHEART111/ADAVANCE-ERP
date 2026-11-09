package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SalesCostLayerUsage
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.domain.repository.SalesReportSummary
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchItemDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryChallanInfo
import com.pocketdev.advance_erp.domain.repository.DeliveryChallanItemInfo
import com.pocketdev.advance_erp.domain.repository.UndeliveredItemInfo
import com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao,
    private val customerDao: CustomerDao,
    private val warehouseDao: WarehouseDao,
    private val productDao: ProductDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val accountingRepository: AccountingRepository,
    private val documentNumberService: com.pocketdev.advance_erp.core.util.DocumentNumberService
) : SalesRepository {

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private suspend fun generateInvoiceNumber(): String {
        return documentNumberService.getNextInvoiceNumber()
    }

    private suspend fun generatePaymentNumber(): String {
        return documentNumberService.getNextCustomerPaymentNumber()
    }

    private suspend fun generateReturnNumber(): String {
        return documentNumberService.getNextSalesReturnNumber()
    }

    override suspend fun createInvoice(draft: SalesInvoiceDraft): String {
        val customer = if (draft.newCustomerDraft != null) {
            val normalizedPhone = draft.newCustomerDraft.phone.replace("[^0-9+]".toRegex(), "").trim()
            val existing = customerDao.getCustomerByPhone(normalizedPhone)
            if (existing != null) {
                existing
            } else {
                val customerCode = "CUST-" + String.format(Locale.US, "%06d", (System.currentTimeMillis() % 1000000))
                val newCust = Customer(
                    id = UUID.randomUUID().toString(),
                    customerCode = customerCode,
                    name = draft.newCustomerDraft.name.trim(),
                    phone = normalizedPhone,
                    email = draft.newCustomerDraft.email?.trim()?.takeIf { it.isNotBlank() },
                    address = draft.newCustomerDraft.address.trim(),
                    creditLimitMinor = 0L,
                    outstandingBalanceMinor = draft.newCustomerDraft.openingBalanceMinor,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                customerDao.insertCustomer(newCust)
                newCust
            }
        } else {
            customerDao.getCustomerById(draft.customerId)
                ?: throw IllegalArgumentException("Customer not found: ${draft.customerId}")
        }

        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found: ${draft.warehouseId}")

        val invoiceId = UUID.randomUUID().toString()
        val invoiceNumber = generateInvoiceNumber()

        var subtotalMinor = 0L
        val invoiceItems = draft.items.map { itemDraft ->
            val product = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found: ${itemDraft.productId}")

            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotalMinor += lineGross

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                sku = product.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotalMinor - draft.discountMinor + draft.additionalChargesMinor)
        val initialPaid = minOf(draft.paidAmountMinor, totalAmount)
        val initialDue = maxOf(0L, totalAmount - initialPaid)

        val invoice = SalesInvoice(
            id = invoiceId,
            invoiceNumber = invoiceNumber,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            status = InvoiceStatus.DRAFT.name,
            subtotalMinor = subtotalMinor,
            discountMinor = draft.discountMinor,
            taxMinor = draft.additionalChargesMinor,
            totalAmountMinor = totalAmount,
            paidAmountMinor = initialPaid,
            dueAmountMinor = initialDue,
            totalCogsMinor = 0L,
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            isPosted = false
        )

        salesDao.insertInvoice(invoice)
        salesDao.insertInvoiceItems(invoiceItems)
        return invoiceId
    }

    override suspend fun createAndPostInvoice(draft: SalesInvoiceDraft): Result<String> {
        return try {
            val invoiceId = db.withTransaction {
                val id = createInvoice(draft)
                val postResult = postInvoice(id)
                if (postResult.isFailure) {
                    throw postResult.exceptionOrNull() ?: IllegalStateException("Failed to post invoice")
                }
                id
            }
            Result.success(invoiceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateInvoice(invoiceId: String, draft: SalesInvoiceDraft) {
        val existing = salesDao.getInvoiceById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")
        if (existing.isPosted) {
            throw IllegalStateException("Cannot edit a posted invoice. Use sales return or adjustment instead.")
        }

        val customer = customerDao.getCustomerById(draft.customerId)
            ?: throw IllegalArgumentException("Customer not found")
        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found")

        var subtotalMinor = 0L
        val invoiceItems = draft.items.map { itemDraft ->
            val product = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found")
            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotalMinor += lineGross

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                sku = product.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotalMinor - draft.discountMinor)
        val initialPaid = minOf(draft.paidAmountMinor, totalAmount)
        val initialDue = maxOf(0L, totalAmount - initialPaid)

        val updated = existing.copy(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            subtotalMinor = subtotalMinor,
            discountMinor = draft.discountMinor,
            totalAmountMinor = totalAmount,
            paidAmountMinor = if (draft.paidAmountMinor > 0L) initialPaid else existing.paidAmountMinor,
            dueAmountMinor = if (draft.paidAmountMinor > 0L) initialDue else maxOf(0L, totalAmount - existing.paidAmountMinor),
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            updatedAt = System.currentTimeMillis()
        )

        salesDao.updateInvoice(updated)
        salesDao.insertInvoiceItems(invoiceItems)
    }

    override fun getAllInvoices(): Flow<List<SalesInvoice>> = salesDao.getAllInvoices()

    override suspend fun getInvoiceById(id: String): SalesInvoice? = salesDao.getInvoiceById(id)

    override suspend fun getInvoiceItems(invoiceId: String): List<SalesInvoiceItem> =
        salesDao.getItemsForInvoice(invoiceId)

    override fun getInvoiceItemsFlow(invoiceId: String): Flow<List<SalesInvoiceItem>> =
        salesDao.getItemsForInvoiceFlow(invoiceId)

    override suspend fun postInvoice(invoiceId: String): Result<Unit> {
        return try {
            val invoice = salesDao.getInvoiceById(invoiceId)
                ?: return Result.failure(IllegalArgumentException("Invoice not found"))

            if (invoice.isPosted) {
                return Result.failure(IllegalStateException("Invoice $invoiceId is already posted"))
            }

            val items = salesDao.getItemsForInvoice(invoiceId)
            if (items.isEmpty()) {
                return Result.failure(IllegalStateException("Invoice has no items"))
            }

            // 1. Stock availability validation (sellable stock = quantity - reservedQuantity)
            for (item in items) {
                val balance = if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(invoice.warehouseId, item.productId, item.variantId)
                        ?: stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                } else {
                    stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                }
                val sellableQty = maxOf(0, (balance?.quantity ?: 0) - (balance?.reservedQuantity ?: 0))
                if (sellableQty < item.quantity) {
                    return Result.failure(
                        IllegalStateException(
                            "Insufficient sellable stock for '${item.productNameEn}'. Available: $sellableQty, Requested: ${item.quantity}"
                        )
                    )
                }
            }

            // 2. Atomic Stock Reduction, COGS & Cost Layer Depletion
            var totalInvoiceCogs = 0L
            val costLayerUsages = mutableListOf<SalesCostLayerUsage>()
            val allFifoConsumptions = mutableListOf<FifoConsumption>()
            val updatedItems = mutableListOf<SalesInvoiceItem>()

            for (item in items) {
                // Deduct stock balance (variant-specific if variantId is present)
                val currentBalance = if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(invoice.warehouseId, item.productId, item.variantId)
                        ?: stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                } else {
                    stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                }
                val existingQty = currentBalance?.quantity ?: 0
                val newQty = existingQty - item.quantity
                if (currentBalance != null) {
                    stockBalanceDao.insertOrUpdate(
                        currentBalance.copy(
                            quantity = newQty,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    stockBalanceDao.insertOrUpdate(
                        StockBalance(
                            warehouseId = invoice.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = newQty,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                // Deplete active cost layers (FIFO by receipt date, strictly warehouse & variant isolated)
                var neededQty = item.quantity
                var itemTotalCogs = 0L
                val activeLayers = if (item.variantId != null) {
                    val variantLayers = inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
                        productId = item.productId,
                        variantId = item.variantId,
                        warehouseId = invoice.warehouseId
                    )
                    if (variantLayers.isNotEmpty()) variantLayers else {
                        inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                            productId = item.productId,
                            warehouseId = invoice.warehouseId
                        )
                    }
                } else {
                    inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                        productId = item.productId,
                        warehouseId = invoice.warehouseId
                    )
                }

                for (layer in activeLayers) {
                    if (neededQty <= 0) break
                    val qtyToTake = minOf(neededQty, layer.remainingQuantity)
                    val layerCost = qtyToTake.toLong() * layer.unitLandedCostBdtMinor
                    itemTotalCogs += layerCost
                    neededQty -= qtyToTake

                    val newRemaining = layer.remainingQuantity - qtyToTake
                    inventoryCostLayerDao.updateRemainingQuantity(
                        layerId = layer.id,
                        remainingQuantity = newRemaining,
                        isActive = newRemaining > 0
                    )

                    costLayerUsages.add(
                        SalesCostLayerUsage(
                            invoiceId = invoice.id,
                            invoiceItemId = item.id,
                            costLayerId = layer.id,
                            quantityDepleted = qtyToTake,
                            unitCostMinor = layer.unitLandedCostBdtMinor
                        )
                    )

                    allFifoConsumptions.add(
                        FifoConsumption(
                            invoiceId = invoice.id,
                            invoiceItemId = item.id,
                            costLayerId = layer.id,
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = invoice.warehouseId,
                            quantity = qtyToTake,
                            unitCostBdtMinor = layer.unitLandedCostBdtMinor,
                            totalCogsBdtMinor = layerCost,
                            referenceType = "SALE",
                            referenceId = invoice.id
                        )
                    )
                }

                // If layers are exhausted (e.g. legacy/adjusted stock), fallback to product purchase price
                if (neededQty > 0) {
                    val prod = productDao.getProductById(item.productId)
                    val fallbackUnitCost = prod?.purchasePriceMinor ?: 0L
                    itemTotalCogs += (neededQty.toLong() * fallbackUnitCost)
                }

                val unitCogs = if (item.quantity > 0) itemTotalCogs / item.quantity else 0L
                totalInvoiceCogs += itemTotalCogs

                // Stock Movement Audit with running balance, cost impact, and sales reference
                val movement = StockMovement(
                    id = MovementNumberGenerator.generateId("mov-sale"),
                    movementNumber = MovementNumberGenerator.generate("MOV-SALE"),
                    warehouseId = invoice.warehouseId,
                    productId = item.productId,
                    variantId = item.variantId,
                    movementType = StockMovementType.SALE_DISPATCH.code,
                    quantity = item.quantity,
                    unitCostMinor = unitCogs,
                    balanceAfterMovement = newQty,
                    totalCostImpactMinor = itemTotalCogs,
                    referenceType = "SALES_INVOICE",
                    referenceId = invoice.id,
                    salesInvoiceId = invoice.id,
                    notes = "Sales Invoice #${invoice.invoiceNumber}",
                    createdAt = System.currentTimeMillis()
                )
                stockMovementDao.insertMovement(movement)

                updatedItems.add(
                    item.copy(
                        unitCostMinor = unitCogs,
                        totalCogsMinor = itemTotalCogs
                    )
                )
            }

            salesDao.insertInvoiceItems(updatedItems)
            if (costLayerUsages.isNotEmpty()) {
                salesDao.insertCostLayerUsages(costLayerUsages)
            }
            if (allFifoConsumptions.isNotEmpty()) {
                db.fifoConsumptionDao().insertConsumptions(allFifoConsumptions)
            }

            // Update Invoice as Posted
            val now = System.currentTimeMillis()
            val finalStatus = when {
                invoice.dueAmountMinor == 0L && invoice.totalAmountMinor > 0L -> InvoiceStatus.PAID.name
                invoice.paidAmountMinor > 0L -> InvoiceStatus.PARTIALLY_PAID.name
                else -> InvoiceStatus.POSTED.name
            }
            val postedInvoice = invoice.copy(
                isPosted = true,
                status = finalStatus,
                totalCogsMinor = totalInvoiceCogs,
                postedAt = now,
                updatedAt = now
            )
            salesDao.updateInvoice(postedInvoice)

            // If an initial cash payment was attached at draft creation, record customer payment & allocation
            val cashPaid = invoice.paidAmountMinor
            val arDue = invoice.dueAmountMinor

            if (cashPaid > 0L) {
                val paymentId = UUID.randomUUID().toString()
                val paymentNumber = generatePaymentNumber()
                val payment = CustomerPayment(
                    id = paymentId,
                    paymentNumber = paymentNumber,
                    customerId = invoice.customerId,
                    customerName = invoice.customerName,
                    amountMinor = cashPaid,
                    allocatedAmountMinor = cashPaid,
                    unallocatedCreditMinor = 0L,
                    paymentDate = now,
                    paymentMethod = "CASH",
                    accountId = "coa-1010",
                    referenceNote = "Initial payment on Invoice #${invoice.invoiceNumber}",
                    isAdvance = false,
                    isPosted = true
                )
                customerPaymentDao.insertPayment(payment)
                val allocation = PaymentAllocation(
                    id = UUID.randomUUID().toString(),
                    paymentId = paymentId,
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    amountMinor = cashPaid,
                    allocatedAt = now
                )
                customerPaymentDao.insertAllocations(listOf(allocation))
            }

            // 3. Double-Entry Accounting Integration
            // Entry A: Sales Revenue, Accounts Receivable, and Cash Received
            val revenueLines = mutableListOf<JournalLineDraft>()

            if (cashPaid > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-1010",
                        isDebit = true,
                        amountMinor = cashPaid,
                        description = "Cash Received: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (arDue > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = true,
                        amountMinor = arDue,
                        description = "Accounts Receivable: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (cashPaid == 0L && arDue == 0L && invoice.totalAmountMinor > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = true,
                        amountMinor = invoice.totalAmountMinor,
                        description = "Accounts Receivable: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (invoice.discountMinor > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-4030",
                        isDebit = true,
                        amountMinor = invoice.discountMinor,
                        description = "Sales Discount: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            revenueLines.add(
                JournalLineDraft(
                    accountId = "coa-4010",
                    isDebit = false,
                    amountMinor = invoice.subtotalMinor,
                    description = "Sales Revenue: Inv #${invoice.invoiceNumber}"
                )
            )

            accountingRepository.postJournalEntry(
                entryDate = now,
                descEn = "Sales Invoice #${invoice.invoiceNumber} posted for ${invoice.customerName}",
                descBn = "গ্রাহক ${invoice.customerName} এর জন্য বিক্রয় চালান #${invoice.invoiceNumber} পোস্ট করা হয়েছে",
                referenceType = "SALES_INVOICE",
                referenceId = invoice.id,
                lines = revenueLines
            )

            // Entry B: COGS Recognition & Inventory Asset Deduction
            if (totalInvoiceCogs > 0) {
                val cogsLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5010",
                        isDebit = true,
                        amountMinor = totalInvoiceCogs,
                        description = "Cost of Goods Sold: Inv #${invoice.invoiceNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1210",
                        isDebit = false,
                        amountMinor = totalInvoiceCogs,
                        description = "Inventory Asset deduction: Inv #${invoice.invoiceNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = now,
                    descEn = "COGS for Sales Invoice #${invoice.invoiceNumber}",
                    descBn = "বিক্রয় চালান #${invoice.invoiceNumber} এর বিক্রীত পণ্যের মোট ব্যয়",
                    referenceType = "SALES_COGS",
                    referenceId = invoice.id,
                    lines = cogsLines
                )
            }

            // 4. Update Customer Outstanding Balance
            recalculateCustomerBalance(invoice.customerId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordCustomerPayment(draft: CustomerPaymentDraft): String {
        val customer = customerDao.getCustomerById(draft.customerId)
            ?: throw IllegalArgumentException("Customer not found: ${draft.customerId}")

        val paymentId = UUID.randomUUID().toString()
        val paymentNumber = generatePaymentNumber()
        val allocations = mutableListOf<PaymentAllocation>()

        var totalAllocated = 0L

        if (draft.allocations.isNotEmpty()) {
            for (allocDraft in draft.allocations) {
                val inv = salesDao.getInvoiceById(allocDraft.invoiceId) ?: continue
                val maxAlloc = minOf(allocDraft.amountMinor, inv.dueAmountMinor)
                if (maxAlloc > 0) {
                    allocations.add(
                        PaymentAllocation(
                            id = UUID.randomUUID().toString(),
                            paymentId = paymentId,
                            invoiceId = inv.id,
                            invoiceNumber = inv.invoiceNumber,
                            amountMinor = maxAlloc,
                            allocatedAt = draft.paymentDate
                        )
                    )
                    totalAllocated += maxAlloc
                    val newPaid = inv.paidAmountMinor + maxAlloc
                    val newDue = maxOf(0L, inv.totalAmountMinor - newPaid)
                    val newStatus = if (newDue == 0L) InvoiceStatus.PAID.name else InvoiceStatus.PARTIALLY_PAID.name
                    salesDao.updateInvoicePaymentStatus(inv.id, newStatus, newPaid, newDue)
                }
            }
        } else {
            // Confirmed Business Rule 4.2: Customer payments must NOT automatically be distributed
            // across old invoice dues via FIFO. Standalone payments are recorded as customer ledger credits.
            totalAllocated = 0L
        }

        val unallocatedCredit = maxOf(0L, draft.amountMinor - totalAllocated)
        val isAdvance = unallocatedCredit > 0 && totalAllocated == 0L

        val payment = CustomerPayment(
            id = paymentId,
            paymentNumber = paymentNumber,
            customerId = customer.id,
            customerName = customer.name,
            amountMinor = draft.amountMinor,
            allocatedAmountMinor = totalAllocated,
            unallocatedCreditMinor = unallocatedCredit,
            paymentDate = draft.paymentDate,
            paymentMethod = draft.paymentMethod,
            accountId = draft.accountId,
            referenceNote = draft.referenceNote,
            isAdvance = isAdvance,
            isPosted = true
        )

        customerPaymentDao.insertPayment(payment)
        if (allocations.isNotEmpty()) {
            customerPaymentDao.insertAllocations(allocations)
        }

        // Double-Entry Accounting
        val journalLines = mutableListOf<JournalLineDraft>()
        // Debit: Cash/Bank
        journalLines.add(
            JournalLineDraft(
                accountId = draft.accountId,
                isDebit = true,
                amountMinor = draft.amountMinor,
                description = "Customer payment received: Receipt #${paymentNumber}"
            )
        )
        // Credit: Accounts Receivable for allocated portion
        if (totalAllocated > 0) {
            journalLines.add(
                JournalLineDraft(
                    accountId = "coa-1040",
                    isDebit = false,
                    amountMinor = totalAllocated,
                    description = "Payment allocated to invoice(s): Receipt #${paymentNumber}"
                )
            )
        }
        // Credit: Customer Advance for unallocated portion
        if (unallocatedCredit > 0) {
            journalLines.add(
                JournalLineDraft(
                    accountId = "coa-2030",
                    isDebit = false,
                    amountMinor = unallocatedCredit,
                    description = "Customer advance deposit: Receipt #${paymentNumber}"
                )
            )
        }

        accountingRepository.postJournalEntry(
            entryDate = draft.paymentDate,
            descEn = "Customer payment of ৳${draft.amountMinor / 100} from ${customer.name}",
            descBn = "গ্রাহক ${customer.name} এর কাছ থেকে ৳${draft.amountMinor / 100} পেমেন্ট গ্রহণ",
            referenceType = "CUSTOMER_PAYMENT",
            referenceId = paymentId,
            lines = journalLines
        )

        recalculateCustomerBalance(draft.customerId)
        return paymentId
    }

    override fun getAllPayments(): Flow<List<CustomerPayment>> = customerPaymentDao.getAllPayments()

    override fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsForCustomer(customerId)

    override fun getAllocationsForInvoice(invoiceId: String): Flow<List<PaymentAllocation>> =
        customerPaymentDao.getAllocationsForInvoice(invoiceId)

    override suspend fun getAllocationsForInvoiceSync(invoiceId: String): List<PaymentAllocation> =
        customerPaymentDao.getAllocationsForInvoiceSync(invoiceId)

    override suspend fun processSalesReturn(draft: SalesReturnDraft): String {
        return db.withTransaction {
            val invoice = salesDao.getInvoiceById(draft.invoiceId)
                ?: throw IllegalArgumentException("Invoice not found: ${draft.invoiceId}")
            if (!invoice.isPosted) {
                throw IllegalStateException("Cannot return items on an unposted invoice")
            }

            val returnId = UUID.randomUUID().toString()
            val returnNumber = generateReturnNumber()

            var totalReturnAmount = 0L
            var totalSoundCogsReversal = 0L
            var totalDamageLoss = 0L
            val returnItems = mutableListOf<SalesReturnItem>()
            val existingItems = salesDao.getItemsForInvoice(draft.invoiceId).associateBy { it.id }

            for (itemDraft in draft.items) {
                val invItem = existingItems[itemDraft.invoiceItemId]
                    ?: throw IllegalArgumentException("Invoice item not found: ${itemDraft.invoiceItemId}")

                val maxReturnable = invItem.quantity - invItem.returnedQuantity
                if (itemDraft.returnQuantity <= 0 || itemDraft.returnQuantity > maxReturnable) {
                    throw IllegalStateException(
                        "Invalid return quantity for '${invItem.productNameEn}'. Requested: ${itemDraft.returnQuantity}, Max returnable: $maxReturnable"
                    )
                }

                val lineReturnAmount = itemDraft.returnQuantity.toLong() * invItem.unitPriceMinor
                val lineCogsReversal = itemDraft.returnQuantity.toLong() * invItem.unitCostMinor
                totalReturnAmount += lineReturnAmount

                val damagedQty = if (itemDraft.isDamaged) itemDraft.returnQuantity else minOf(itemDraft.damagedQuantity, itemDraft.returnQuantity)
                val soundQty = itemDraft.returnQuantity - damagedQty

                val soundCogs = soundQty.toLong() * invItem.unitCostMinor
                val damageCogs = damagedQty.toLong() * invItem.unitCostMinor
                totalSoundCogsReversal += soundCogs
                totalDamageLoss += damageCogs

                // Only sound returned goods restore sellable warehouse stock
                if (soundQty > 0) {
                    val balance = if (itemDraft.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, itemDraft.productId, itemDraft.variantId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)
                    }
                    if (balance != null) {
                        stockBalanceDao.insertOrUpdate(
                            balance.copy(quantity = balance.quantity + soundQty, updatedAt = System.currentTimeMillis())
                        )
                    } else {
                        stockBalanceDao.insertOrUpdate(
                            StockBalance(
                                warehouseId = draft.warehouseId,
                                productId = itemDraft.productId,
                                variantId = itemDraft.variantId,
                                quantity = soundQty
                            )
                        )
                    }

                    val currentBal = stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)?.quantity ?: 0
                    // Movement Audit for Sound Return
                    val movement = StockMovement(
                        id = MovementNumberGenerator.generateId("mov-ret"),
                        movementNumber = MovementNumberGenerator.generate("MOV-RET"),
                        warehouseId = draft.warehouseId,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        movementType = StockMovementType.SALE_RETURN.code,
                        quantity = soundQty,
                        unitCostMinor = invItem.unitCostMinor,
                        balanceAfterMovement = currentBal,
                        totalCostImpactMinor = soundQty.toLong() * invItem.unitCostMinor,
                        referenceType = "SALES_RETURN",
                        referenceId = returnId,
                        salesInvoiceId = invoice.id,
                        notes = "Sales Return #${returnNumber} (Sound goods) for Invoice #${invoice.invoiceNumber}",
                        createdAt = System.currentTimeMillis()
                    )
                    stockMovementDao.insertMovement(movement)

                    // Restore/Create FIFO Cost Layer for sound goods
                    inventoryCostLayerDao.insertCostLayer(
                        InventoryCostLayer(
                            id = "icl-${UUID.randomUUID()}",
                            productId = itemDraft.productId,
                            variantId = itemDraft.variantId,
                            warehouseId = draft.warehouseId,
                            shipmentId = "RET",
                            shipmentNumber = returnNumber,
                            warehouseReceiptId = returnId,
                            receiptNumber = returnNumber,
                            initialQuantity = soundQty,
                            remainingQuantity = soundQty,
                            unitLandedCostBdtMinor = invItem.unitCostMinor,
                            originalPurchasePriceMinor = invItem.unitCostMinor,
                            exchangeRateToBdt = "1.0",
                            allocatedExpensePerUnitBdtMinor = 0L,
                            receiptDate = System.currentTimeMillis(),
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // Damaged returned goods are excluded from sellable stock and recorded as write-off
                if (damagedQty > 0) {
                    val currentBal = stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)?.quantity ?: 0
                    val dmgMovement = StockMovement(
                        id = MovementNumberGenerator.generateId("mov-dmg"),
                        movementNumber = MovementNumberGenerator.generate("MOV-DMG"),
                        warehouseId = draft.warehouseId,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                        quantity = damagedQty,
                        unitCostMinor = invItem.unitCostMinor,
                        balanceAfterMovement = currentBal,
                        totalCostImpactMinor = damagedQty.toLong() * invItem.unitCostMinor,
                        referenceType = "SALES_RETURN",
                        referenceId = returnId,
                        salesInvoiceId = invoice.id,
                        notes = "Damaged returned goods written off from Invoice #${invoice.invoiceNumber}. Excluded from sellable stock.",
                        createdAt = System.currentTimeMillis()
                    )
                    stockMovementDao.insertMovement(dmgMovement)
                }

                // Update item returned quantity
                salesDao.recordItemReturn(invItem.id, itemDraft.returnQuantity)

                returnItems.add(
                    SalesReturnItem(
                        id = UUID.randomUUID().toString(),
                        returnId = returnId,
                        invoiceItemId = invItem.id,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        productNameEn = invItem.productNameEn,
                        productNameBn = invItem.productNameBn,
                        returnQuantity = itemDraft.returnQuantity,
                        unitPriceMinor = invItem.unitPriceMinor,
                        returnTotalMinor = lineReturnAmount,
                        originalUnitCostMinor = invItem.unitCostMinor,
                        cogsReversalMinor = lineCogsReversal
                    )
                )
            }

            val salesReturn = SalesReturn(
                id = returnId,
                returnNumber = returnNumber,
                invoiceId = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                customerId = invoice.customerId,
                customerName = invoice.customerName,
                warehouseId = draft.warehouseId,
                totalReturnAmountMinor = totalReturnAmount,
                totalCogsReversalMinor = totalSoundCogsReversal + totalDamageLoss,
                refundOption = draft.refundOption.name,
                reason = draft.reason,
                returnDate = draft.returnDate,
                isPosted = true
            )

            salesReturnDao.insertReturn(salesReturn)
            salesReturnDao.insertReturnItems(returnItems)

            // Adjust Invoice Due Amount if Customer Credit was chosen
            if (draft.refundOption == ReturnRefundOption.CUSTOMER_CREDIT) {
                val adjustedDue = maxOf(0L, invoice.dueAmountMinor - totalReturnAmount)
                salesDao.updateInvoicePaymentStatus(
                    invoiceId = invoice.id,
                    status = if (adjustedDue == 0L) InvoiceStatus.PAID.name else invoice.status,
                    paidMinor = invoice.paidAmountMinor,
                    dueMinor = adjustedDue
                )
            }

            // Double-Entry Accounting
            // 1. Sales Reversal
            val creditAccount = when (draft.refundOption) {
                ReturnRefundOption.CUSTOMER_CREDIT -> "coa-1040" // Accounts Receivable
                ReturnRefundOption.CASH_REFUND -> "coa-1010"     // Cash in Hand
                ReturnRefundOption.BANK_REFUND -> "coa-1020"     // Bank
            }
            val returnRevenueLines = listOf(
                JournalLineDraft(
                    accountId = "coa-4020",
                    isDebit = true,
                    amountMinor = totalReturnAmount,
                    description = "Sales Return #${returnNumber} on Invoice #${invoice.invoiceNumber}"
                ),
                JournalLineDraft(
                    accountId = creditAccount,
                    isDebit = false,
                    amountMinor = totalReturnAmount,
                    description = "Refund/Credit for Return #${returnNumber}"
                )
            )
            accountingRepository.postJournalEntry(
                entryDate = draft.returnDate,
                descEn = "Sales Return #${returnNumber} for ${invoice.customerName}",
                descBn = "গ্রাহক ${invoice.customerName} এর জন্য বিক্রয় ফেরত #${returnNumber}",
                referenceType = "SALES_RETURN",
                referenceId = returnId,
                lines = returnRevenueLines
            )

            // 2. COGS Reversal & Inventory Restoration / Damage Write-Off
            if (totalSoundCogsReversal > 0) {
                val cogsReversalLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1210",
                        isDebit = true,
                        amountMinor = totalSoundCogsReversal,
                        description = "Restored Inventory Asset: Return #${returnNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-5010",
                        isDebit = false,
                        amountMinor = totalSoundCogsReversal,
                        description = "COGS Reversal: Return #${returnNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = draft.returnDate,
                    descEn = "COGS Reversal for Return #${returnNumber}",
                    descBn = "ফেরত #${returnNumber} এর জন্য COGS সমন্বয়",
                    referenceType = "RETURN_COGS",
                    referenceId = returnId,
                    lines = cogsReversalLines
                )
            }

            if (totalDamageLoss > 0) {
                val damageLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5050", // Damaged Merchandise Write-Off
                        isDebit = true,
                        amountMinor = totalDamageLoss,
                        description = "Damaged Return Write-Off: Return #${returnNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-5010", // COGS
                        isDebit = false,
                        amountMinor = totalDamageLoss,
                        description = "COGS Reversal for Damaged Return: Return #${returnNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = draft.returnDate,
                    descEn = "Damaged Return Write-Off #${returnNumber}",
                    descBn = "ক্ষতিগ্রস্ত ফেরত পণ্য অবলোপন #${returnNumber}",
                    referenceType = "RETURN_DAMAGE",
                    referenceId = returnId,
                    lines = damageLines
                )
            }

            recalculateCustomerBalance(invoice.customerId)
            returnId
        }
    }

    override fun getAllReturns(): Flow<List<SalesReturn>> = salesReturnDao.getAllReturns()

    override fun getReturnsForInvoice(invoiceId: String): Flow<List<SalesReturn>> =
        salesReturnDao.getReturnsForInvoice(invoiceId)

    override suspend fun getItemsForReturn(returnId: String): List<SalesReturnItem> =
        salesReturnDao.getItemsForReturn(returnId)

    override suspend fun getCustomerLedger(customerId: String): List<CustomerLedgerEntry> {
        val ledgerItems = mutableListOf<CustomerLedgerEntry>()
        val customer = customerDao.getCustomerById(customerId)

        // 1. Invoices (Debit AR)
        val invoices = salesDao.getPostedInvoicesInRange(0L, Long.MAX_VALUE)
            .filter { it.customerId == customerId }

        for (inv in invoices) {
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = inv.id,
                    date = inv.invoiceDate,
                    transactionType = "INVOICE",
                    referenceNumber = inv.invoiceNumber,
                    description = "Sales Invoice #${inv.invoiceNumber} (${inv.warehouseName})",
                    debitMinor = inv.totalAmountMinor,
                    creditMinor = 0L,
                    runningBalanceMinor = 0L
                )
            )
            // If invoice was cancelled, record offset in ledger
            if (inv.status == InvoiceStatus.CANCELLED.name) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${inv.id}-cancelled",
                        date = inv.cancelledAt ?: (inv.updatedAt),
                        transactionType = "INVOICE_CANCELLED",
                        referenceNumber = "CANCEL-${inv.invoiceNumber}",
                        description = "Cancelled Invoice #${inv.invoiceNumber}${if (!inv.cancellationReason.isNullOrBlank()) ": " + inv.cancellationReason else ""}",
                        debitMinor = 0L,
                        creditMinor = inv.totalAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // 2. Payments (Credit AR / Advance)
        val allCustomerPayments = customerPaymentDao.getAllPayments().first().filter { it.customerId == customerId }

        for (pay in allCustomerPayments) {
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = pay.id,
                    date = pay.paymentDate,
                    transactionType = "PAYMENT",
                    referenceNumber = pay.paymentNumber,
                    description = "Payment Receipt #${pay.paymentNumber} (${pay.paymentMethod})",
                    debitMinor = 0L,
                    creditMinor = pay.amountMinor,
                    runningBalanceMinor = 0L
                )
            )
            // If payment was voided, record debit reversal offset in ledger
            if (pay.isVoided) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${pay.id}-voided",
                        date = pay.voidedAt ?: (pay.paymentDate + 1),
                        transactionType = "PAYMENT_VOID",
                        referenceNumber = "VOID-${pay.paymentNumber}",
                        description = "Void Payment #${pay.paymentNumber}${if (!pay.voidReason.isNullOrBlank()) ": " + pay.voidReason else ""}",
                        debitMinor = pay.amountMinor,
                        creditMinor = 0L,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // 3. Returns (Credit AR)
        val allReturns = salesReturnDao.getAllReturns().first().filter { it.customerId == customerId }

        // 0. Opening Due / Previous Balance (Confirmed Rule 4.2)
        val activeInvoices = invoices.filter { it.status != InvoiceStatus.CANCELLED.name }
        val activePayments = allCustomerPayments.filter { !it.isVoided }
        val netTransactions = activeInvoices.sumOf { it.totalAmountMinor } -
            activePayments.sumOf { it.amountMinor } -
            allReturns.filter { it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name }.sumOf { it.totalReturnAmountMinor }
        val openingBal = (customer?.outstandingBalanceMinor ?: 0L) - netTransactions
        if (openingBal > 0L) {
            val initialDate = (customer?.createdAt ?: System.currentTimeMillis()) - (365L * 24 * 3600 * 1000)
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = "opening-$customerId",
                    date = initialDate,
                    transactionType = "OPENING_BALANCE",
                    referenceNumber = "OPENING",
                    description = "Opening / Previous Due / পূর্বের বকেয়া",
                    debitMinor = openingBal,
                    creditMinor = 0L,
                    runningBalanceMinor = openingBal
                )
            )
        }

        for (ret in allReturns) {
            if (ret.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = ret.id,
                        date = ret.returnDate,
                        transactionType = "RETURN",
                        referenceNumber = ret.returnNumber,
                        description = "Sales Return #${ret.returnNumber} on #${ret.invoiceNumber}",
                        debitMinor = 0L,
                        creditMinor = ret.totalReturnAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
            } else {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${ret.id}-credit",
                        date = ret.returnDate,
                        transactionType = "RETURN",
                        referenceNumber = ret.returnNumber,
                        description = "Sales Return #${ret.returnNumber} on #${ret.invoiceNumber}",
                        debitMinor = 0L,
                        creditMinor = ret.totalReturnAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${ret.id}-refund",
                        date = ret.returnDate + 1,
                        transactionType = "REFUND",
                        referenceNumber = "REF-${ret.returnNumber}",
                        description = "Refund Paid: #${ret.returnNumber} (${ret.refundOption})",
                        debitMinor = ret.totalReturnAmountMinor,
                        creditMinor = 0L,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // Sort chronologically
        ledgerItems.sortBy { it.date }

        // Compute running balance: Previous Balance + Debit - Credit = Closing Balance
        var runningBal = 0L
        return ledgerItems.map { entry ->
            runningBal += (entry.debitMinor - entry.creditMinor)
            entry.copy(runningBalanceMinor = runningBal)
        }
    }

    override suspend fun recalculateCustomerBalance(customerId: String): Long {
        val ledger = getCustomerLedger(customerId)
        val outstandingMinor = ledger.lastOrNull()?.runningBalanceMinor ?: 0L
        customerDao.updateOutstandingBalance(customerId, maxOf(0L, outstandingMinor))
        return outstandingMinor
    }

    override fun getCustomerAdvanceBalance(customerId: String): Flow<Long> =
        customerPaymentDao.getCustomerAdvanceCreditMinor(customerId)

    override suspend fun getSalesReportSummary(fromMillis: Long, toMillis: Long): SalesReportSummary {
        val invoices = salesDao.getPostedInvoicesInRange(fromMillis, toMillis)

        var totalGross = 0L
        var totalDiscount = 0L
        var totalNet = 0L
        var totalCogs = 0L
        var totalDue = 0L

        for (inv in invoices) {
            totalGross += inv.subtotalMinor
            totalDiscount += inv.discountMinor
            totalNet += inv.totalAmountMinor
            totalCogs += inv.totalCogsMinor
            totalDue += inv.dueAmountMinor
        }

        val grossProfit = totalNet - totalCogs
        val margin = if (totalNet > 0) (grossProfit.toDouble() / totalNet.toDouble()) * 100.0 else 0.0

        val allPayments = customerPaymentDao.getAllPayments().first().filter { it.paymentDate in fromMillis..toMillis }
        val totalCollected = allPayments.sumOf { it.amountMinor }

        val allReturns = salesReturnDao.getAllReturns().first().filter { it.returnDate in fromMillis..toMillis }
        val totalReturns = allReturns.sumOf { it.totalReturnAmountMinor }

        return SalesReportSummary(
            totalGrossSalesMinor = totalGross,
            totalDiscountMinor = totalDiscount,
            totalNetSalesMinor = totalNet,
            totalCogsMinor = totalCogs,
            totalGrossProfitMinor = grossProfit,
            grossProfitMarginPercent = margin,
            totalCollectedMinor = totalCollected,
            totalReceivableMinor = totalDue,
            totalReturnsMinor = totalReturns
        )
    }

    override suspend fun getProductWiseSalesReport(fromMillis: Long, toMillis: Long): List<ProductWiseSalesItem> {
        val invoices = salesDao.getPostedInvoicesInRange(fromMillis, toMillis)
        val productMap = mutableMapOf<String, MutableList<SalesInvoiceItem>>()

        for (inv in invoices) {
            val items = salesDao.getItemsForInvoice(inv.id)
            for (item in items) {
                productMap.getOrPut(item.productId) { mutableListOf() }.add(item)
            }
        }

        return productMap.map { (prodId, items) ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity - it.returnedQuantity }
            val totalRev = items.sumOf { it.lineTotalMinor }
            val totalCogs = items.sumOf { it.totalCogsMinor }
            ProductWiseSalesItem(
                productId = prodId,
                productNameEn = first.productNameEn,
                productNameBn = first.productNameBn,
                sku = first.sku,
                unitsSold = totalQty,
                revenueMinor = totalRev,
                cogsMinor = totalCogs,
                grossProfitMinor = totalRev - totalCogs
            )
        }.sortedByDescending { it.revenueMinor }
    }

    override suspend fun getCustomerWiseSalesReport(fromMillis: Long, toMillis: Long): List<CustomerWiseSalesItem> {
        val invoices = salesDao.getPostedInvoicesInRange(fromMillis, toMillis)
        val customerMap = invoices.groupBy { it.customerId }

        return customerMap.map { (custId, invList) ->
            val first = invList.first()
            val totalPurchased = invList.sumOf { it.totalAmountMinor }
            val totalPaid = invList.sumOf { it.paidAmountMinor }
            val totalDue = invList.sumOf { it.dueAmountMinor }
            CustomerWiseSalesItem(
                customerId = custId,
                customerName = first.customerName,
                customerPhone = first.customerPhone ?: "",
                invoiceCount = invList.size,
                totalPurchasedMinor = totalPurchased,
                totalPaidMinor = totalPaid,
                outstandingBalanceMinor = totalDue
            )
        }.sortedByDescending { it.totalPurchasedMinor }
    }

    override suspend fun getCustomerSummary(customerId: String): CustomerSummary? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val invoices = salesDao.getInvoicesForCustomerSync(customerId)
        val postedInvoices = invoices.filter { it.isPosted }
        val totalSalesMinor = postedInvoices.sumOf { it.totalAmountMinor }
        val payments = customerPaymentDao.getPaymentsForCustomerSync(customerId)
        val lifetimePaidMinor = payments.sumOf { it.amountMinor }
        val returns = salesReturnDao.getReturnsForCustomerSync(customerId)
        val totalReturnsMinor = returns.sumOf { it.totalReturnAmountMinor }

        return CustomerSummary(
            customer = customer,
            totalSalesMinor = totalSalesMinor,
            lifetimePaidMinor = lifetimePaidMinor,
            outstandingBalanceMinor = customer.outstandingBalanceMinor,
            totalReturnsMinor = totalReturnsMinor
        )
    }

    override suspend fun searchCustomers(query: String): List<Customer> {
        return customerDao.searchCustomersSync(query)
    }

    // ==============================================================
    // Quotation & Sales Order Management (Confirmed Section 7)
    // ==============================================================

    override suspend fun createQuotation(draft: SalesInvoiceDraft): String {
        val quoId = UUID.randomUUID().toString()
        val quoNumber = documentNumberService.getNextQuotationNumber()

        val customer = if (draft.customerId.isNotBlank()) customerDao.getCustomerById(draft.customerId) else null
        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found: ${draft.warehouseId}")

        var subtotal = 0L
        val items = draft.items.map { itemDraft ->
            val p = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found: ${itemDraft.productId}")
            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotal += lineGross

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = quoId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = p.nameEn,
                productNameBn = p.nameBn,
                sku = p.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotal - draft.discountMinor + draft.additionalChargesMinor)

        val quotation = SalesInvoice(
            id = quoId,
            invoiceNumber = quoNumber,
            customerId = customer?.id ?: "",
            customerName = customer?.name ?: "Valued Customer",
            customerPhone = customer?.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            status = InvoiceStatus.QUOTATION.name,
            subtotalMinor = subtotal,
            discountMinor = draft.discountMinor,
            taxMinor = 0L,
            totalAmountMinor = totalAmount,
            paidAmountMinor = 0L,
            dueAmountMinor = totalAmount,
            totalCogsMinor = 0L,
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            isPosted = false,
            createdAt = System.currentTimeMillis()
        )

        db.withTransaction {
            salesDao.insertInvoice(quotation)
            salesDao.insertInvoiceItems(items)
        }
        return quoId
    }

    override suspend fun convertQuotationToSalesOrder(quotationId: String): Result<String> {
        val quo = salesDao.getInvoiceById(quotationId)
            ?: return Result.failure(IllegalArgumentException("Quotation not found: $quotationId"))

        if (quo.status != InvoiceStatus.QUOTATION.name) {
            return Result.failure(IllegalStateException("Document is already converted or in status: ${quo.status}"))
        }

        val quoItems = salesDao.getItemsForInvoice(quotationId)
        val soId = UUID.randomUUID().toString()
        val soNumber = documentNumberService.getNextSalesOrderNumber()

        val salesOrder = quo.copy(
            id = soId,
            invoiceNumber = soNumber,
            status = InvoiceStatus.SALES_ORDER.name,
            notes = "Converted from Quotation #${quo.invoiceNumber}. ${quo.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis()
        )

        val soItems = quoItems.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = soId) }

        db.withTransaction {
            salesDao.insertInvoice(salesOrder)
            salesDao.insertInvoiceItems(soItems)
            salesDao.updateInvoiceStatus(quotationId, InvoiceStatus.CANCELLED.name)
        }

        return Result.success(soId)
    }

    override suspend fun convertSalesOrderToInvoice(salesOrderId: String): Result<String> {
        val so = salesDao.getInvoiceById(salesOrderId)
            ?: return Result.failure(IllegalArgumentException("Sales Order not found: $salesOrderId"))

        if (so.status != InvoiceStatus.SALES_ORDER.name) {
            return Result.failure(IllegalStateException("Document is not a Sales Order: ${so.status}"))
        }

        val soItems = salesDao.getItemsForInvoice(salesOrderId)
        val invId = UUID.randomUUID().toString()
        val invNumber = documentNumberService.getNextInvoiceNumber()

        val invoice = so.copy(
            id = invId,
            invoiceNumber = invNumber,
            status = InvoiceStatus.CONFIRMED.name,
            isPosted = true,
            postedAt = System.currentTimeMillis(),
            notes = "Converted from Sales Order #${so.invoiceNumber}. ${so.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis()
        )

        val invItems = soItems.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = invId) }

        db.withTransaction {
            salesDao.insertInvoice(invoice)
            salesDao.insertInvoiceItems(invItems)
            salesDao.updateInvoiceStatus(salesOrderId, InvoiceStatus.DELIVERED.name)

            // Post Accounting receivable & revenue
            val now = System.currentTimeMillis()
            val lines = listOf(
                JournalLineDraft("coa-1040", true, invoice.totalAmountMinor, "Accounts Receivable: Inv #$invNumber"),
                JournalLineDraft("coa-4010", false, invoice.subtotalMinor, "Sales Revenue: Inv #$invNumber")
            )
            accountingRepository.postJournalEntry(
                entryDate = now,
                descEn = "Sales Invoice #$invNumber posted from SO #${so.invoiceNumber}",
                descBn = "সেলস অর্ডার #${so.invoiceNumber} হতে বিক্রয় চালান #$invNumber পোস্ট করা হয়েছে",
                referenceType = "SALES_INVOICE",
                referenceId = invId,
                lines = lines
            )
        }

        return Result.success(invId)
    }

    // ==============================================================
    // Delivery Management & Partial Dispatch (Confirmed Section 5.4)
    // ==============================================================

    override suspend fun dispatchDelivery(draft: DeliveryDispatchDraft): Result<String> {
        return try {
            val deliveryNumber = db.withTransaction {
                val invoice = salesDao.getInvoiceById(draft.invoiceId)
                    ?: throw IllegalArgumentException("Invoice not found: ${draft.invoiceId}")

                val invoiceItems = salesDao.getItemsForInvoice(draft.invoiceId).associateBy { it.id }
                val delNumber = documentNumberService.getNextDeliveryNumber()
                val now = draft.deliveryDate

                for (dispItem in draft.items) {
                    if (dispItem.quantityToDispatch <= 0) continue

                    val invItem = invoiceItems[dispItem.invoiceItemId]
                        ?: throw IllegalArgumentException("Invoice item not found: ${dispItem.invoiceItemId}")

                    // Check already dispatched
                    val alreadyDispatched = stockMovementDao.getDispatchedQuantityForInvoiceItem(
                        invoiceId = draft.invoiceId,
                        productId = dispItem.productId,
                        variantId = dispItem.variantId
                    )
                    val remainingToDispatch = invItem.quantity - alreadyDispatched
                    if (dispItem.quantityToDispatch > remainingToDispatch) {
                        throw IllegalStateException(
                            "Cannot dispatch ${dispItem.quantityToDispatch} for '${invItem.productNameEn}'. Only $remainingToDispatch remaining."
                        )
                    }

                    // Check sellable stock in warehouse
                    val balance = if (dispItem.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, dispItem.productId, dispItem.variantId)
                            ?: stockBalanceDao.getBalance(draft.warehouseId, dispItem.productId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, dispItem.productId)
                    }
                    val sellable = maxOf(0, (balance?.quantity ?: 0) - (balance?.reservedQuantity ?: 0))
                    if (sellable < dispItem.quantityToDispatch) {
                        throw IllegalStateException(
                            "Insufficient sellable stock for '${invItem.productNameEn}'. Sellable: $sellable, Requested: ${dispItem.quantityToDispatch}"
                        )
                    }

                    // Deduct stock balance
                    val currentQty = balance?.quantity ?: 0
                    val newQty = currentQty - dispItem.quantityToDispatch
                    if (balance != null) {
                        stockBalanceDao.insertOrUpdate(balance.copy(quantity = newQty, updatedAt = now))
                    } else {
                        stockBalanceDao.insertOrUpdate(
                            StockBalance(
                                warehouseId = draft.warehouseId,
                                productId = dispItem.productId,
                                variantId = dispItem.variantId,
                                quantity = newQty,
                                updatedAt = now
                            )
                        )
                    }

                    // Deplete FIFO cost layers
                    var neededQty = dispItem.quantityToDispatch
                    var dispatchTotalCogs = 0L
                    val activeLayers = if (dispItem.variantId != null) {
                        val vLayers = inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
                            productId = dispItem.productId,
                            variantId = dispItem.variantId,
                            warehouseId = draft.warehouseId
                        )
                        if (vLayers.isNotEmpty()) vLayers else {
                            inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                                productId = dispItem.productId,
                                warehouseId = draft.warehouseId
                            )
                        }
                    } else {
                        inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                            productId = dispItem.productId,
                            warehouseId = draft.warehouseId
                        )
                    }

                    for (layer in activeLayers) {
                        if (neededQty <= 0) break
                        val qtyToTake = minOf(neededQty, layer.remainingQuantity)
                        val layerCost = qtyToTake.toLong() * layer.unitLandedCostBdtMinor
                        dispatchTotalCogs += layerCost
                        neededQty -= qtyToTake

                        val newRemaining = layer.remainingQuantity - qtyToTake
                        inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, newRemaining > 0)
                    }

                    val effectiveUnitCost = if (dispItem.quantityToDispatch > 0) dispatchTotalCogs / dispItem.quantityToDispatch else 0L

                    // Insert StockMovement
                    stockMovementDao.insertMovement(
                        StockMovement(
                            movementNumber = "$delNumber-${dispItem.productId.take(4)}",
                            warehouseId = draft.warehouseId,
                            productId = dispItem.productId,
                            variantId = dispItem.variantId,
                            movementType = StockMovementType.SALE_DISPATCH.code,
                            quantity = dispItem.quantityToDispatch,
                            unitCostMinor = effectiveUnitCost,
                            balanceAfterMovement = newQty,
                            totalCostImpactMinor = dispatchTotalCogs,
                            referenceType = "DELIVERY_CHALLAN",
                            referenceId = delNumber,
                            salesInvoiceId = draft.invoiceId,
                            notes = "Challan #$delNumber | Driver: ${draft.deliveryPerson ?: "N/A"}, Transport: ${draft.transportInfo ?: "N/A"}",
                            createdAt = now
                        )
                    )

                    // Post COGS vs Inventory for this dispatch
                    if (dispatchTotalCogs > 0) {
                        val cogsLines = listOf(
                            JournalLineDraft("coa-5010", true, dispatchTotalCogs, "COGS: Challan #$delNumber"),
                            JournalLineDraft("coa-1210", false, dispatchTotalCogs, "Inventory Dispatch: Challan #$delNumber")
                        )
                        accountingRepository.postJournalEntry(
                            entryDate = now,
                            descEn = "COGS for Delivery Challan #$delNumber (Inv #${invoice.invoiceNumber})",
                            descBn = "ডেলিভারি চালান #$delNumber এর বিক্রীত পণ্যের ব্যয়",
                            referenceType = "DELIVERY_CHALLAN",
                            referenceId = delNumber,
                            lines = cogsLines
                        )
                    }
                }

                // Check overall invoice delivery progress
                val allItems = salesDao.getItemsForInvoice(draft.invoiceId)
                var totalOrdered = 0
                var totalDispatched = 0
                for (item in allItems) {
                    totalOrdered += item.quantity
                    totalDispatched += stockMovementDao.getDispatchedQuantityForInvoiceItem(
                        invoiceId = draft.invoiceId,
                        productId = item.productId,
                        variantId = item.variantId
                    )
                }

                val newInvoiceStatus = if (totalDispatched >= totalOrdered) {
                    InvoiceStatus.DELIVERED.name
                } else if (totalDispatched > 0) {
                    InvoiceStatus.PARTIALLY_DELIVERED.name
                } else {
                    InvoiceStatus.CONFIRMED.name
                }

                salesDao.updateInvoiceStatus(draft.invoiceId, newInvoiceStatus, now)
                delNumber
            }
            Result.success(deliveryNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemainingUndeliveredItems(invoiceId: String): List<UndeliveredItemInfo> {
        val items = salesDao.getItemsForInvoice(invoiceId)
        return items.map { item ->
            val dispatched = stockMovementDao.getDispatchedQuantityForInvoiceItem(
                invoiceId = invoiceId,
                productId = item.productId,
                variantId = item.variantId
            )
            UndeliveredItemInfo(
                invoiceItemId = item.id,
                productId = item.productId,
                variantId = item.variantId,
                productNameEn = item.productNameEn,
                productNameBn = item.productNameBn,
                sku = item.sku,
                orderedQuantity = item.quantity,
                dispatchedQuantity = dispatched,
                remainingQuantity = maxOf(0, item.quantity - dispatched)
            )
        }
    }

    override suspend fun getDeliveryChallansForInvoice(invoiceId: String): List<DeliveryChallanInfo> {
        val invoice = salesDao.getInvoiceById(invoiceId) ?: return emptyList()
        val movements = stockMovementDao.getMovementsForInvoiceSync(invoiceId)
            .filter { it.referenceType == "DELIVERY_CHALLAN" }

        val byDeliveryNumber = movements.groupBy { it.referenceId ?: it.movementNumber }
        return byDeliveryNumber.map { (delNum, movList) ->
            val firstMov = movList.first()
            DeliveryChallanInfo(
                deliveryNumber = delNum,
                invoiceId = invoiceId,
                invoiceNumber = invoice.invoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                deliveryDate = firstMov.createdAt,
                deliveryPerson = firstMov.notes?.substringAfter("Driver: ")?.substringBefore(",")?.trim(),
                transportInfo = firstMov.notes?.substringAfter("Transport: ")?.trim(),
                notes = firstMov.notes,
                items = movList.map { m ->
                    val p = productDao.getProductById(m.productId)
                    DeliveryChallanItemInfo(
                        productId = m.productId,
                        variantId = m.variantId,
                        productNameEn = p?.nameEn ?: m.productId,
                        productNameBn = p?.nameBn ?: m.productId,
                        sku = p?.baseSku ?: "",
                        dispatchedQuantity = m.quantity
                    )
                }
            )
        }
    }

    // ==============================================================
    // Direct Product Exchange (Method A, Confirmed Section 6)
    // ==============================================================

    override suspend fun processDirectExchange(draft: DirectExchangeDraft): Result<String> {
        return try {
            val exchangeNumber = db.withTransaction {
                val exNum = documentNumberService.getNextSalesReturnNumber()
                val now = System.currentTimeMillis()

                // 1. Process returned products
                for (ret in draft.returnedItems) {
                    if (ret.isDamaged || ret.damagedQuantity > 0) {
                        // Damaged returns: quarantined / written-off, NEVER sellable
                        stockMovementDao.insertMovement(
                            StockMovement(
                                movementNumber = "$exNum-DAM-${ret.productId.take(4)}",
                                warehouseId = draft.warehouseId,
                                productId = ret.productId,
                                variantId = ret.variantId,
                                movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                                quantity = ret.damagedQuantity.coerceAtLeast(ret.returnQuantity),
                                unitCostMinor = 0L,
                                referenceType = "DIRECT_EXCHANGE",
                                referenceId = exNum,
                                notes = "Damaged return quarantined during Direct Exchange #$exNum",
                                createdAt = now
                            )
                        )
                    } else {
                        // Good stock restored to warehouse balance
                        val bal = stockBalanceDao.getBalance(draft.warehouseId, ret.productId)
                        val newQty = (bal?.quantity ?: 0) + ret.returnQuantity
                        if (bal != null) {
                            stockBalanceDao.insertOrUpdate(bal.copy(quantity = newQty, updatedAt = now))
                        } else {
                            stockBalanceDao.insertOrUpdate(
                                StockBalance(
                                    warehouseId = draft.warehouseId,
                                    productId = ret.productId,
                                    variantId = ret.variantId,
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        }

                        stockMovementDao.insertMovement(
                            StockMovement(
                                movementNumber = "$exNum-RET-${ret.productId.take(4)}",
                                warehouseId = draft.warehouseId,
                                productId = ret.productId,
                                variantId = ret.variantId,
                                movementType = StockMovementType.SALE_RETURN.code,
                                quantity = ret.returnQuantity,
                                unitCostMinor = 0L,
                                balanceAfterMovement = newQty,
                                referenceType = "DIRECT_EXCHANGE",
                                referenceId = exNum,
                                notes = "Good stock returned in Direct Exchange #$exNum",
                                createdAt = now
                            )
                        )
                    }
                }

                // 2. Validate and deduct replacement products
                for (rep in draft.replacementItems) {
                    val bal = stockBalanceDao.getBalance(draft.warehouseId, rep.productId)
                    val sellable = maxOf(0, (bal?.quantity ?: 0) - (bal?.reservedQuantity ?: 0))
                    if (sellable < rep.quantity) {
                        throw IllegalStateException("Insufficient sellable stock for replacement product: ${rep.productId}")
                    }
                    val newQty = (bal?.quantity ?: 0) - rep.quantity
                    stockBalanceDao.insertOrUpdate(bal!!.copy(quantity = newQty, updatedAt = now))

                    stockMovementDao.insertMovement(
                        StockMovement(
                            movementNumber = "$exNum-DISP-${rep.productId.take(4)}",
                            warehouseId = draft.warehouseId,
                            productId = rep.productId,
                            variantId = rep.variantId,
                            movementType = StockMovementType.SALE_DISPATCH.code,
                            quantity = rep.quantity,
                            unitCostMinor = 0L,
                            balanceAfterMovement = newQty,
                            referenceType = "DIRECT_EXCHANGE",
                            referenceId = exNum,
                            notes = "Replacement goods dispatched in Direct Exchange #$exNum",
                            createdAt = now
                        )
                    )
                }

                // 3. Post accounting difference
                if (draft.netDifferenceMinor != 0L) {
                    val lines = if (draft.netDifferenceMinor > 0) {
                        // Customer owes more -> Debit Cash/AR, Credit Sales Revenue
                        listOf(
                            JournalLineDraft(draft.paymentAccountId, true, draft.netDifferenceMinor, "Direct Exchange Extra Collection: #$exNum"),
                            JournalLineDraft("coa-4010", false, draft.netDifferenceMinor, "Net Sales Revenue Adjustment: #$exNum")
                        )
                    } else {
                        // Customer owed refund -> Debit Sales Returns, Credit Cash/AP
                        val refundAmt = -draft.netDifferenceMinor
                        listOf(
                            JournalLineDraft("coa-4020", true, refundAmt, "Sales Return Allowance: #$exNum"),
                            JournalLineDraft(draft.paymentAccountId, false, refundAmt, "Direct Exchange Refund Paid: #$exNum")
                        )
                    }
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Direct Product Exchange #$exNum for customer ${draft.customerId}",
                        descBn = "সরাসরি পণ্য বিনিময় #$exNum",
                        referenceType = "DIRECT_EXCHANGE",
                        referenceId = exNum,
                        lines = lines
                    )
                }

                exNum
            }
            Result.success(exchangeNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun voidCustomerPayment(
        paymentId: String,
        reason: String,
        operator: String
    ): Result<Unit> {
        return try {
            val payment = customerPaymentDao.getPaymentById(paymentId)
                ?: return Result.failure(IllegalArgumentException("Payment receipt not found: $paymentId"))

            if (payment.isVoided) {
                return Result.failure(IllegalStateException("Payment receipt #${payment.paymentNumber} has already been voided."))
            }

            if (reason.isBlank()) {
                return Result.failure(IllegalArgumentException("A valid cancellation reason is required to void a payment."))
            }

            db.withTransaction {
                val now = System.currentTimeMillis()

                // 1. Reverse payment allocations and recalculate invoices
                val allocations = customerPaymentDao.getAllocationsForPayment(paymentId)
                for (alloc in allocations) {
                    val invoice = salesDao.getInvoiceById(alloc.invoiceId)
                    if (invoice != null) {
                        val newPaid = maxOf(0L, invoice.paidAmountMinor - alloc.amountMinor)
                        val newDue = maxOf(0L, invoice.totalAmountMinor - newPaid)
                        val newStatus = when {
                            newPaid == 0L -> if (invoice.isPosted) InvoiceStatus.POSTED.name else InvoiceStatus.DRAFT.name
                            newDue > 0L -> InvoiceStatus.PARTIALLY_PAID.name
                            else -> InvoiceStatus.PAID.name
                        }
                        salesDao.updateInvoicePaymentStatus(invoice.id, newStatus, newPaid, newDue, now)
                    }
                }
                customerPaymentDao.deleteAllocationsForPayment(paymentId)

                // 2. Reverse accounting entries:
                val activeJournals = db.accountingDao().getActiveEntriesForReference("CUSTOMER_PAYMENT", paymentId)
                var revJournalId: String? = null
                if (activeJournals.isNotEmpty()) {
                    for (entry in activeJournals) {
                        revJournalId = accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else {
                    val revLines = mutableListOf<JournalLineDraft>()
                    if (payment.allocatedAmountMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-1040",
                                isDebit = true,
                                amountMinor = payment.allocatedAmountMinor,
                                description = "Restore AR: Void payment #${payment.paymentNumber}"
                            )
                        )
                    }
                    if (payment.unallocatedCreditMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-2030",
                                isDebit = true,
                                amountMinor = payment.unallocatedCreditMinor,
                                description = "Reverse advance credit: Void payment #${payment.paymentNumber}"
                            )
                        )
                    }
                    revLines.add(
                        JournalLineDraft(
                            accountId = payment.accountId,
                            isDebit = false,
                            amountMinor = payment.amountMinor,
                            description = "Reverse Cash/Bank debit: Void payment #${payment.paymentNumber}"
                        )
                    )
                    revJournalId = accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Void Payment Receipt #${payment.paymentNumber} ($reason)",
                        descBn = "পেমেন্ট রশিদ #${payment.paymentNumber} বাতিল ($reason)",
                        referenceType = "VOID_PAYMENT",
                        referenceId = paymentId,
                        lines = revLines
                    )
                }

                // 3. Mark payment record as voided without deleting it
                val updatedPayment = payment.copy(
                    isVoided = true,
                    voidedAt = now,
                    voidReason = reason.trim(),
                    voidedBy = operator,
                    allocatedAmountMinor = 0L,
                    unallocatedCreditMinor = 0L
                )
                customerPaymentDao.updatePayment(updatedPayment)

                // 4. Restore customer balance in database
                recalculateCustomerBalance(payment.customerId)

                // 5. Audit trail
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER_PAYMENT",
                        recordId = payment.id,
                        action = "VOID",
                        details = "Payment receipt #${payment.paymentNumber} of ৳${payment.amountMinor / 100} voided. Allocations reversed. Reason: $reason",
                        operator = operator,
                        documentNumber = payment.paymentNumber,
                        operationType = "PAYMENT_VOID",
                        previousStatus = "POSTED",
                        newStatus = "VOIDED",
                        cancellationReason = reason.trim(),
                        reversalReference = revJournalId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrDeleteInvoice(
        invoiceId: String,
        reason: String,
        operator: String
    ): Result<Unit> {
        return try {
            val invoice = salesDao.getInvoiceById(invoiceId)
                ?: return Result.failure(IllegalArgumentException("Invoice not found: $invoiceId"))

            if (invoice.status == InvoiceStatus.CANCELLED.name) {
                return Result.failure(IllegalStateException("Invoice #${invoice.invoiceNumber} is already cancelled."))
            }

            // Case A: Unposted invoice (DRAFT, QUOTATION, etc.)
            if (!invoice.isPosted) {
                db.withTransaction {
                    salesDao.deleteInvoiceItems(invoiceId)
                    salesDao.deleteInvoice(invoiceId)
                    db.auditLogDao().insertAuditLog(
                        AuditLog(
                            recordType = "SALES_INVOICE",
                            recordId = invoiceId,
                            action = "HARD_DELETE",
                            details = "Unposted draft invoice #${invoice.invoiceNumber} deleted. Reason: ${reason.ifBlank { "Deleted by user" }}",
                            operator = operator,
                            documentNumber = invoice.invoiceNumber,
                            operationType = "HARD_DELETE",
                            previousStatus = invoice.status,
                            newStatus = "DELETED",
                            cancellationReason = reason.ifBlank { "User deletion" }
                        )
                    )
                }
                return Result.success(Unit)
            }

            // Case B: Posted invoice (POSTED, PARTIALLY_PAID, PAID, etc.)
            if (reason.isBlank()) {
                return Result.failure(IllegalArgumentException("A valid cancellation reason is required to cancel a posted invoice."))
            }

            db.withTransaction {
                val now = System.currentTimeMillis()

                // 1. Stock Restoration & FIFO Cost Layer Recovery
                val items = salesDao.getItemsForInvoice(invoiceId)
                val costLayerUsages = salesDao.getCostLayerUsagesForInvoice(invoiceId)

                for (item in items) {
                    val netDispatched = maxOf(0, item.quantity - item.returnedQuantity)
                    if (netDispatched > 0) {
                        val currentBalance = if (item.variantId != null) {
                            stockBalanceDao.getVariantBalance(invoice.warehouseId, item.productId, item.variantId)
                                ?: stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                        } else {
                            stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                        }
                        val existingQty = currentBalance?.quantity ?: 0
                        val newQty = existingQty + netDispatched
                        if (currentBalance != null) {
                            stockBalanceDao.insertOrUpdate(
                                currentBalance.copy(
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        } else {
                            stockBalanceDao.insertOrUpdate(
                                StockBalance(
                                    warehouseId = invoice.warehouseId,
                                    productId = item.productId,
                                    variantId = item.variantId,
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        }

                        // Stock Movement audit for restoration
                        val movement = StockMovement(
                            id = MovementNumberGenerator.generateId("mov-cancel"),
                            movementNumber = MovementNumberGenerator.generate("MOV-CAN"),
                            warehouseId = invoice.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SALE_RETURN.code,
                            quantity = netDispatched,
                            unitCostMinor = item.unitCostMinor,
                            balanceAfterMovement = newQty,
                            totalCostImpactMinor = netDispatched.toLong() * item.unitCostMinor,
                            referenceType = "SALES_INVOICE_CANCEL",
                            referenceId = invoice.id,
                            salesInvoiceId = invoice.id,
                            operatorName = operator,
                            notes = "Stock restored from cancelled invoice #${invoice.invoiceNumber}. Reason: $reason",
                            createdAt = now
                        )
                        stockMovementDao.insertMovement(movement)
                    }
                }

                // Restore FIFO cost layers
                for (usage in costLayerUsages) {
                    val layer = inventoryCostLayerDao.getCostLayerById(usage.costLayerId)
                    if (layer != null) {
                        val newRemaining = layer.remainingQuantity + usage.quantityDepleted
                        inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, isActive = true)
                    }
                }

                // 2. Handle previously received payments allocated to this invoice:
                // Convert payments into customer advance credit (unallocated)
                val allocations = customerPaymentDao.getAllocationsForInvoiceSync(invoiceId)
                for (alloc in allocations) {
                    val payment = customerPaymentDao.getPaymentById(alloc.paymentId)
                    if (payment != null && !payment.isVoided) {
                        val freedAmount = minOf(alloc.amountMinor, payment.allocatedAmountMinor)
                        val updatedPayment = payment.copy(
                            allocatedAmountMinor = maxOf(0L, payment.allocatedAmountMinor - freedAmount),
                            unallocatedCreditMinor = payment.unallocatedCreditMinor + freedAmount,
                            isAdvance = true
                        )
                        customerPaymentDao.updatePayment(updatedPayment)

                        // Journal entry: reclassify from AR to Customer Advance
                        if (freedAmount > 0) {
                            accountingRepository.postJournalEntry(
                                entryDate = now,
                                descEn = "Convert payment #${payment.paymentNumber} to customer advance on cancelled invoice #${invoice.invoiceNumber}",
                                descBn = "বাতিলকৃত ইনভয়েস #${invoice.invoiceNumber} বাবদ পেমেন্ট #${payment.paymentNumber} অগ্রিমে রূপান্তর",
                                referenceType = "PAYMENT_TO_ADVANCE",
                                referenceId = invoice.id,
                                lines = listOf(
                                    JournalLineDraft(
                                        accountId = "coa-1040",
                                        isDebit = true,
                                        amountMinor = freedAmount,
                                        description = "Reclassify AR to Advance: Inv #${invoice.invoiceNumber}"
                                    ),
                                    JournalLineDraft(
                                        accountId = "coa-2030",
                                        isDebit = false,
                                        amountMinor = freedAmount,
                                        description = "Customer advance credit: Receipt #${payment.paymentNumber}"
                                    )
                                )
                            )
                        }
                    }
                }
                customerPaymentDao.deleteAllocationsForInvoice(invoiceId)

                // 3. Reverse Accounting Entries (Revenue and COGS)
                val activeRevenueJournals = db.accountingDao().getActiveEntriesForReference("SALES_INVOICE", invoiceId)
                var revRevId: String? = null
                if (activeRevenueJournals.isNotEmpty()) {
                    for (entry in activeRevenueJournals) {
                        revRevId = accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else {
                    val revLines = mutableListOf<JournalLineDraft>()
                    revLines.add(
                        JournalLineDraft(
                            accountId = "coa-4010",
                            isDebit = true,
                            amountMinor = invoice.subtotalMinor,
                            description = "Reversal of Sales Revenue: Inv #${invoice.invoiceNumber}"
                        )
                    )
                    if (invoice.discountMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-4030",
                                isDebit = false,
                                amountMinor = invoice.discountMinor,
                                description = "Reversal of Sales Discount: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    }
                    val netArReversal = invoice.totalAmountMinor
                    if (netArReversal > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-1040",
                                isDebit = false,
                                amountMinor = netArReversal,
                                description = "Reversal of AR: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    }
                    revRevId = accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Cancellation reversal for Invoice #${invoice.invoiceNumber}",
                        descBn = "ইনভয়েস #${invoice.invoiceNumber} বাতিলের বিপরীত দাখিলা",
                        referenceType = "VOID_INVOICE_SALES",
                        referenceId = invoiceId,
                        lines = revLines
                    )
                }

                // Reverse COGS
                val activeCogsJournals = db.accountingDao().getActiveEntriesForReference("SALES_COGS", invoiceId)
                if (activeCogsJournals.isNotEmpty()) {
                    for (entry in activeCogsJournals) {
                        accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else if (invoice.totalCogsMinor > 0) {
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Reversal of COGS for cancelled Invoice #${invoice.invoiceNumber}",
                        descBn = "বাতিলকৃত ইনভয়েস #${invoice.invoiceNumber} এর বিক্রীত পণ্যের ব্যয়ের বিপরীত দাখিলা",
                        referenceType = "VOID_INVOICE_COGS",
                        referenceId = invoiceId,
                        lines = listOf(
                            JournalLineDraft(
                                accountId = "coa-1210",
                                isDebit = true,
                                amountMinor = invoice.totalCogsMinor,
                                description = "Restore Inventory Asset: Inv #${invoice.invoiceNumber}"
                            ),
                            JournalLineDraft(
                                accountId = "coa-5010",
                                isDebit = false,
                                amountMinor = invoice.totalCogsMinor,
                                description = "Reverse COGS: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    )
                }

                // 4. Update Invoice Record
                val cancelledInvoice = invoice.copy(
                    status = InvoiceStatus.CANCELLED.name,
                    dueAmountMinor = 0L,
                    paidAmountMinor = 0L,
                    cancellationReason = reason.trim(),
                    cancelledAt = now,
                    updatedAt = now
                )
                salesDao.updateInvoice(cancelledInvoice)

                // 5. Restore Customer Balance
                recalculateCustomerBalance(invoice.customerId)

                // 6. Audit Trail
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "SALES_INVOICE",
                        recordId = invoice.id,
                        action = "CANCEL",
                        details = "Posted invoice #${invoice.invoiceNumber} (Total: ৳${invoice.totalAmountMinor / 100}) cancelled. Stock, FIFO, and accounting reversed. Reason: $reason",
                        operator = operator,
                        documentNumber = invoice.invoiceNumber,
                        operationType = "INVOICE_CANCEL",
                        previousStatus = invoice.status,
                        newStatus = "CANCELLED",
                        cancellationReason = reason.trim(),
                        reversalReference = revRevId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
