package com.pocketdev.advance_erp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.ProductCategoryDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.StockTransferDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.AmountRangeFilter
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.DeliveryReturnFilter
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import com.pocketdev.advance_erp.domain.model.LineItemDetail
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.model.PaymentStatusFilter
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SavedFilterPreset
import com.pocketdev.advance_erp.domain.model.SortOption
import com.pocketdev.advance_erp.domain.model.TransactionDetailRecord
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessControlCenterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AdvanceErpDatabase,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val expenseDao: ExpenseDao,
    private val stockMovementDao: StockMovementDao,
    private val stockTransferDao: StockTransferDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val warehouseDao: WarehouseDao,
    private val productCategoryDao: ProductCategoryDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val businessProfileDao: BusinessProfileDao
) : BusinessControlCenterRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("control_center_presets", Context.MODE_PRIVATE)
    private val presetsFlow = MutableStateFlow<List<SavedFilterPreset>>(loadPresetsFromPrefs())

    override fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()

    override fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()

    override fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    override fun getAllCategories(): Flow<List<ProductCategory>> = productCategoryDao.getAllCategories()

    override fun getBusinessProfile(): Flow<BusinessProfile?> = businessProfileDao.getProfile()

    override fun getSavedPresets(): Flow<List<SavedFilterPreset>> = presetsFlow.asStateFlow()

    override suspend fun savePreset(name: String, filterState: ControlCenterFilterState): String {
        val newPreset = SavedFilterPreset(
            id = UUID.randomUUID().toString(),
            name = name,
            filterState = filterState
        )
        val current = presetsFlow.value.toMutableList()
        current.add(0, newPreset)
        savePresetsToPrefs(current)
        presetsFlow.value = current
        return newPreset.id
    }

    override suspend fun deletePreset(presetId: String) {
        val current = presetsFlow.value.filterNot { it.id == presetId }
        savePresetsToPrefs(current)
        presetsFlow.value = current
    }

    private fun loadPresetsFromPrefs(): List<SavedFilterPreset> {
        val jsonStr = prefs.getString("presets_list", null) ?: return emptyList()
        val list = mutableListOf<SavedFilterPreset>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val filterObj = obj.getJSONObject("filterState")
                val filterState = ControlCenterFilterState(
                    dateRangeOption = DateRangeOption.valueOf(filterObj.optString("dateRangeOption", DateRangeOption.ALL_TIME.name)),
                    customStartDate = if (filterObj.has("customStartDate")) filterObj.getLong("customStartDate") else null,
                    customEndDate = if (filterObj.has("customEndDate")) filterObj.getLong("customEndDate") else null,
                    category = ReportCategory.valueOf(filterObj.optString("category", ReportCategory.ALL_TRANSACTIONS.name)),
                    searchQuery = filterObj.optString("searchQuery", ""),
                    selectedCustomerId = if (filterObj.has("selectedCustomerId")) filterObj.getString("selectedCustomerId") else null,
                    selectedSupplierId = if (filterObj.has("selectedSupplierId")) filterObj.getString("selectedSupplierId") else null,
                    selectedWarehouseId = if (filterObj.has("selectedWarehouseId")) filterObj.getString("selectedWarehouseId") else null,
                    paymentStatus = PaymentStatusFilter.valueOf(filterObj.optString("paymentStatus", PaymentStatusFilter.ALL.name)),
                    deliveryReturn = DeliveryReturnFilter.valueOf(filterObj.optString("deliveryReturn", DeliveryReturnFilter.ALL.name)),
                    financialMetric = FinancialMetricFilter.valueOf(filterObj.optString("financialMetric", FinancialMetricFilter.ALL.name)),
                    amountFilter = AmountRangeFilter.valueOf(filterObj.optString("amountFilter", AmountRangeFilter.ALL.name)),
                    customMinAmountMinor = if (filterObj.has("customMinAmountMinor")) filterObj.getLong("customMinAmountMinor") else null,
                    customMaxAmountMinor = if (filterObj.has("customMaxAmountMinor")) filterObj.getLong("customMaxAmountMinor") else null,
                    sortBy = SortOption.valueOf(filterObj.optString("sortBy", SortOption.NEWEST_FIRST.name))
                )
                list.add(
                    SavedFilterPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        filterState = filterState,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun savePresetsToPrefs(presets: List<SavedFilterPreset>) {
        val arr = JSONArray()
        for (p in presets) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("createdAt", p.createdAt)
            val fObj = JSONObject()
            fObj.put("dateRangeOption", p.filterState.dateRangeOption.name)
            p.filterState.customStartDate?.let { fObj.put("customStartDate", it) }
            p.filterState.customEndDate?.let { fObj.put("customEndDate", it) }
            fObj.put("category", p.filterState.category.name)
            fObj.put("searchQuery", p.filterState.searchQuery)
            p.filterState.selectedCustomerId?.let { fObj.put("selectedCustomerId", it) }
            p.filterState.selectedSupplierId?.let { fObj.put("selectedSupplierId", it) }
            p.filterState.selectedWarehouseId?.let { fObj.put("selectedWarehouseId", it) }
            fObj.put("paymentStatus", p.filterState.paymentStatus.name)
            fObj.put("deliveryReturn", p.filterState.deliveryReturn.name)
            fObj.put("financialMetric", p.filterState.financialMetric.name)
            fObj.put("amountFilter", p.filterState.amountFilter.name)
            p.filterState.customMinAmountMinor?.let { fObj.put("customMinAmountMinor", it) }
            p.filterState.customMaxAmountMinor?.let { fObj.put("customMaxAmountMinor", it) }
            fObj.put("sortBy", p.filterState.sortBy.name)
            obj.put("filterState", fObj)
            arr.put(obj)
        }
        prefs.edit().putString("presets_list", arr.toString()).apply()
    }

    override fun getTransactions(filterState: ControlCenterFilterState): Flow<List<UnifiedTransactionItem>> = flow {
        val (fromMillis, toMillis) = filterState.dateRangeOption.getBoundsMillis(filterState.customStartDate, filterState.customEndDate)

        val suppliers = supplierDao.getAllSuppliersIncludingInactiveSync().associateBy { it.id }
        val allList = mutableListOf<UnifiedTransactionItem>()

        // 1. Sales Invoices
        val invoices = salesDao.getAllInvoicesSync()
        for (inv in invoices) {
            if (inv.invoiceDate in fromMillis..toMillis) {
                val isCancelled = inv.status == "CANCELLED"
                val statusColor = when (inv.status) {
                    "PAID" -> 0xFF10B981L
                    "PARTIALLY_PAID" -> 0xFFF59E0BL
                    "CANCELLED" -> 0xFFEF4444L
                    "DELIVERED" -> 0xFF059669L
                    "PARTIALLY_DELIVERED" -> 0xFF0284C7L
                    else -> 0xFF6366F1L
                }
                allList.add(
                    UnifiedTransactionItem(
                        id = inv.id,
                        referenceNumber = inv.invoiceNumber,
                        date = inv.invoiceDate,
                        category = if (inv.status in listOf("DELIVERED", "PARTIALLY_DELIVERED")) ReportCategory.DELIVERY else ReportCategory.SALES,
                        primaryEntityName = inv.customerName,
                        primaryEntityId = inv.customerId,
                        particulars = "Sales Invoice (${inv.status})",
                        amountMinor = inv.totalAmountMinor,
                        secondaryAmountMinor = inv.dueAmountMinor,
                        isInflow = true,
                        paymentMethod = when {
                            inv.paidAmountMinor >= inv.totalAmountMinor -> "PAID"
                            inv.paidAmountMinor > 0 -> "PARTIAL"
                            else -> "UNPAID"
                        },
                        status = inv.status,
                        statusColorHex = statusColor,
                        warehouseName = inv.warehouseName,
                        rawType = "INVOICE",
                        notes = inv.notes,
                        createdAt = inv.createdAt
                    )
                )
            }
        }

        // 2. Customer Payments (Receipts)
        val customerPayments = customerPaymentDao.getAllPaymentsSync()
        for (pmt in customerPayments) {
            if (pmt.paymentDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = pmt.id,
                        referenceNumber = pmt.paymentNumber,
                        date = pmt.paymentDate,
                        category = ReportCategory.RECEIPT,
                        primaryEntityName = pmt.customerName,
                        primaryEntityId = pmt.customerId,
                        particulars = pmt.referenceNote?.ifBlank { null } ?: "Customer Payment Received",
                        amountMinor = pmt.amountMinor,
                        secondaryAmountMinor = pmt.unallocatedCreditMinor,
                        isInflow = true,
                        paymentMethod = pmt.paymentMethod,
                        status = if (pmt.isVoided) "VOIDED" else "POSTED",
                        statusColorHex = if (pmt.isVoided) 0xFFEF4444L else 0xFF10B981L,
                        rawType = "PAYMENT",
                        notes = pmt.referenceNote,
                        createdAt = pmt.createdAt
                    )
                )
            }
        }

        // 3. Sales Returns
        val salesReturns = salesReturnDao.getAllReturnsSync()
        for (ret in salesReturns) {
            if (ret.returnDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = ret.id,
                        referenceNumber = ret.returnNumber,
                        date = ret.returnDate,
                        category = ReportCategory.SALES_RETURN,
                        primaryEntityName = ret.customerName,
                        primaryEntityId = ret.customerId,
                        particulars = ret.reason?.ifBlank { null } ?: "Return against ${ret.invoiceNumber}",
                        amountMinor = ret.totalReturnAmountMinor,
                        secondaryAmountMinor = ret.totalCogsReversalMinor,
                        isInflow = false,
                        paymentMethod = ret.refundOption,
                        status = "RETURNED",
                        statusColorHex = 0xFFDC2626L,
                        rawType = "RETURN",
                        notes = ret.reason,
                        createdAt = ret.createdAt
                    )
                )
            }
        }

        // 4. Purchase Orders
        val purchaseOrders = purchaseOrderDao.getAllOrdersSync()
        for (po in purchaseOrders) {
            if (po.orderDate in fromMillis..toMillis) {
                val supplierName = suppliers[po.supplierId]?.companyName ?: "Supplier"
                val isImport = po.currency != "BDT"
                val statusColor = when (po.status) {
                    "COMPLETED", "RECEIVED" -> 0xFF10B981L
                    "SHIPPED" -> 0xFF0284C7L
                    "CANCELLED" -> 0xFFEF4444L
                    else -> 0xFF8B5CF6L
                }
                allList.add(
                    UnifiedTransactionItem(
                        id = po.id,
                        referenceNumber = po.poNumber,
                        date = po.orderDate,
                        category = if (isImport) ReportCategory.IMPORT else ReportCategory.PURCHASE,
                        primaryEntityName = supplierName,
                        primaryEntityId = po.supplierId,
                        particulars = "Purchase Order [${po.currency} ${po.totalAmountMinor / 100.0}]",
                        amountMinor = po.totalBdtAmountMinor,
                        secondaryAmountMinor = po.totalBdtAmountMinor - po.paidBdtAmountMinor,
                        isInflow = false,
                        paymentMethod = po.currency,
                        status = po.status,
                        statusColorHex = statusColor,
                        rawType = "PURCHASE",
                        notes = po.notes,
                        createdAt = po.createdAt
                    )
                )
            }
        }

        // 5. Supplier Payments
        val supplierPayments = supplierPaymentDao.getAllPaymentsSync()
        for (sp in supplierPayments) {
            if (sp.paymentDate in fromMillis..toMillis) {
                val supplierName = suppliers[sp.supplierId]?.companyName ?: "Supplier"
                allList.add(
                    UnifiedTransactionItem(
                        id = sp.id,
                        referenceNumber = sp.paymentNumber,
                        date = sp.paymentDate,
                        category = ReportCategory.PAYMENT,
                        primaryEntityName = supplierName,
                        primaryEntityId = sp.supplierId,
                        particulars = "Supplier Remittance (${sp.paymentMethod})",
                        amountMinor = sp.bdtAmountMinor,
                        secondaryAmountMinor = sp.amountMinor,
                        isInflow = false,
                        paymentMethod = sp.paymentMethod,
                        status = "PAID",
                        statusColorHex = 0xFF0284C7L,
                        rawType = "SUPPLIER_PAYMENT",
                        notes = sp.notes,
                        createdAt = sp.createdAt
                    )
                )
            }
        }

        // 6. Expenses
        val expenses = expenseDao.getAllExpensesSync()
        for (exp in expenses) {
            if (exp.voucherDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = exp.id,
                        referenceNumber = exp.voucherNumber,
                        date = exp.voucherDate,
                        category = ReportCategory.EXPENSE,
                        primaryEntityName = exp.category,
                        particulars = exp.descriptionEn.ifBlank { exp.descriptionBn },
                        amountMinor = exp.bdtAmountMinor,
                        isInflow = false,
                        paymentMethod = exp.currency,
                        status = if (exp.isReversed) "REVERSED" else "POSTED",
                        statusColorHex = if (exp.isReversed) 0xFFEF4444L else 0xFFF97316L,
                        rawType = "EXPENSE",
                        notes = exp.notes,
                        createdAt = exp.createdAt
                    )
                )
            }
        }

        // 7. Stock Movements
        val movements = stockMovementDao.getAllMovementsWithDetailsSync()
        for (mv in movements) {
            if (mv.createdAt in fromMillis..toMillis) {
                val cat = when (mv.movementType) {
                    "PURCHASE_RECEIPT", "OPENING_STOCK" -> ReportCategory.INVENTORY
                    "SALE_DISPATCH" -> ReportCategory.DELIVERY
                    "SALE_RETURN" -> ReportCategory.SALES_RETURN
                    "PURCHASE_RETURN" -> ReportCategory.PURCHASE_RETURN
                    "STOCK_ADJUSTMENT_IN", "STOCK_ADJUSTMENT_OUT" -> ReportCategory.STOCK_ADJUSTMENT
                    "TRANSFER_IN", "TRANSFER_OUT" -> ReportCategory.STOCK_TRANSFER
                    else -> ReportCategory.INVENTORY
                }
                val costImpact = if (mv.totalCostImpactMinor != 0L) mv.totalCostImpactMinor else (mv.quantity.toLong() * mv.unitCostMinor)
                allList.add(
                    UnifiedTransactionItem(
                        id = mv.id,
                        referenceNumber = mv.movementNumber,
                        date = mv.createdAt,
                        category = cat,
                        primaryEntityName = mv.productNameEn,
                        particulars = "${mv.movementType} | Qty: ${mv.quantity} ${mv.baseSku}",
                        amountMinor = kotlin.math.abs(costImpact),
                        isInflow = mv.quantity >= 0,
                        paymentMethod = mv.warehouseName,
                        status = mv.movementType,
                        statusColorHex = if (mv.quantity >= 0) 0xFF10B981L else 0xFFF59E0BL,
                        warehouseName = mv.warehouseName,
                        rawType = "STOCK_MOVEMENT",
                        notes = mv.notes,
                        createdBy = mv.operatorName,
                        createdAt = mv.createdAt
                    )
                )
            }
        }

        // 8. Stock Transfers
        val transfers = stockTransferDao.getAllTransfersWithDetailsSync()
        for (trf in transfers) {
            if (trf.transferDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = trf.id,
                        referenceNumber = trf.transferNumber,
                        date = trf.transferDate,
                        category = ReportCategory.STOCK_TRANSFER,
                        primaryEntityName = "${trf.sourceWarehouseName} ➔ ${trf.destinationWarehouseName}",
                        particulars = "${trf.productNameEn} (Qty: ${trf.quantity})",
                        amountMinor = 0L,
                        isInflow = false,
                        paymentMethod = "INTERNAL_TRANSFER",
                        status = trf.status,
                        statusColorHex = 0xFF8B5CF6L,
                        warehouseName = trf.destinationWarehouseName,
                        rawType = "STOCK_TRANSFER",
                        notes = trf.notes,
                        createdAt = trf.createdAt
                    )
                )
            }
        }

        // Apply Filters
        var filtered = allList.filter { item ->
            // Category Filter
            val matchCategory = when (filterState.category) {
                ReportCategory.ALL_TRANSACTIONS -> true
                ReportCategory.SALES -> item.category == ReportCategory.SALES
                ReportCategory.PURCHASE -> item.category == ReportCategory.PURCHASE
                ReportCategory.IMPORT -> item.category == ReportCategory.IMPORT
                ReportCategory.CUSTOMER_LEDGER -> item.rawType in listOf("INVOICE", "PAYMENT", "RETURN")
                ReportCategory.SUPPLIER_LEDGER -> item.rawType in listOf("PURCHASE", "SUPPLIER_PAYMENT")
                ReportCategory.PAYMENT -> item.category == ReportCategory.PAYMENT || item.rawType in listOf("SUPPLIER_PAYMENT", "EXPENSE")
                ReportCategory.RECEIPT -> item.category == ReportCategory.RECEIPT || item.rawType == "PAYMENT"
                ReportCategory.INCOME -> item.isInflow
                ReportCategory.EXPENSE -> item.category == ReportCategory.EXPENSE || item.rawType == "EXPENSE"
                ReportCategory.INVENTORY -> item.category in listOf(ReportCategory.INVENTORY, ReportCategory.STOCK_ADJUSTMENT, ReportCategory.STOCK_TRANSFER)
                ReportCategory.DELIVERY -> item.category == ReportCategory.DELIVERY || item.status in listOf("DELIVERED", "PARTIALLY_DELIVERED", "SALE_DISPATCH")
                ReportCategory.SALES_RETURN -> item.category == ReportCategory.SALES_RETURN || item.rawType == "RETURN"
                ReportCategory.PURCHASE_RETURN -> item.category == ReportCategory.PURCHASE_RETURN
                ReportCategory.STOCK_TRANSFER -> item.category == ReportCategory.STOCK_TRANSFER || item.rawType == "STOCK_TRANSFER"
                ReportCategory.STOCK_ADJUSTMENT -> item.category == ReportCategory.STOCK_ADJUSTMENT
            }
            if (!matchCategory) return@filter false

            // Customer Filter
            if (filterState.selectedCustomerId != null) {
                if (item.primaryEntityId != filterState.selectedCustomerId) return@filter false
            }

            // Supplier Filter
            if (filterState.selectedSupplierId != null) {
                if (item.primaryEntityId != filterState.selectedSupplierId) return@filter false
            }

            // Warehouse Filter
            if (filterState.selectedWarehouseId != null) {
                val whName = warehouseDao.getWarehouseById(filterState.selectedWarehouseId)?.name
                if (whName != null && item.warehouseName != null && !item.warehouseName.contains(whName, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Payment Status Filter
            val matchPayment = when (filterState.paymentStatus) {
                PaymentStatusFilter.ALL -> true
                PaymentStatusFilter.PAID -> item.status.equals("PAID", ignoreCase = true) || item.paymentMethod.equals("PAID", ignoreCase = true)
                PaymentStatusFilter.UNPAID -> item.status.equals("DRAFT", ignoreCase = true) || item.paymentMethod.equals("UNPAID", ignoreCase = true)
                PaymentStatusFilter.PARTIAL_PAID -> item.status.contains("PARTIAL", ignoreCase = true) || item.paymentMethod.equals("PARTIAL", ignoreCase = true)
                PaymentStatusFilter.OVERDUE -> item.status.contains("OVERDUE", ignoreCase = true)
                PaymentStatusFilter.ADVANCE -> item.particulars.contains("ADVANCE", ignoreCase = true)
                PaymentStatusFilter.CASH -> item.paymentMethod?.contains("CASH", ignoreCase = true) == true
                PaymentStatusFilter.BANK -> item.paymentMethod?.contains("BANK", ignoreCase = true) == true
                PaymentStatusFilter.MOBILE_BANKING -> item.paymentMethod?.contains("BKASH", ignoreCase = true) == true || item.paymentMethod?.contains("NAGAD", ignoreCase = true) == true
                PaymentStatusFilter.LC -> item.paymentMethod?.contains("LC", ignoreCase = true) == true || item.paymentMethod?.contains("LETTER_OF_CREDIT", ignoreCase = true) == true
                PaymentStatusFilter.TT -> item.paymentMethod?.contains("TT", ignoreCase = true) == true || item.paymentMethod?.contains("TELEGRAPHIC", ignoreCase = true) == true
            }
            if (!matchPayment) return@filter false

            // Delivery & Return Filter
            val matchDeliveryReturn = when (filterState.deliveryReturn) {
                DeliveryReturnFilter.ALL -> true
                DeliveryReturnFilter.PENDING -> item.status.contains("PENDING", ignoreCase = true)
                DeliveryReturnFilter.PROCESSING -> item.status.contains("PROCESSING", ignoreCase = true)
                DeliveryReturnFilter.DISPATCHED -> item.status.contains("DISPATCH", ignoreCase = true)
                DeliveryReturnFilter.DELIVERED -> item.status.contains("DELIVERED", ignoreCase = true)
                DeliveryReturnFilter.PARTIAL_DELIVERED -> item.status.contains("PARTIAL", ignoreCase = true) && item.category == ReportCategory.DELIVERY
                DeliveryReturnFilter.CANCELLED -> item.status.contains("CANCEL", ignoreCase = true)
                DeliveryReturnFilter.ALL_RETURNS -> item.category in listOf(ReportCategory.SALES_RETURN, ReportCategory.PURCHASE_RETURN)
                DeliveryReturnFilter.APPROVED_RETURN -> item.category == ReportCategory.SALES_RETURN && !item.status.contains("REJECT", ignoreCase = true)
                DeliveryReturnFilter.PENDING_RETURN -> item.status.contains("PENDING", ignoreCase = true) && item.category == ReportCategory.SALES_RETURN
                DeliveryReturnFilter.REJECTED_RETURN -> item.status.contains("REJECT", ignoreCase = true)
                DeliveryReturnFilter.DAMAGED_PRODUCT -> item.particulars.contains("DAMAGE", ignoreCase = true) || item.status.contains("DAMAGE", ignoreCase = true)
                DeliveryReturnFilter.GOOD_CONDITION -> !item.particulars.contains("DAMAGE", ignoreCase = true)
            }
            if (!matchDeliveryReturn) return@filter false

            // Financial Metric Filter
            val matchFinancial = when (filterState.financialMetric) {
                FinancialMetricFilter.ALL -> true
                FinancialMetricFilter.INCOME -> item.isInflow
                FinancialMetricFilter.EXPENSE -> !item.isInflow && item.category == ReportCategory.EXPENSE
                FinancialMetricFilter.PROFIT -> item.category == ReportCategory.SALES
                FinancialMetricFilter.LOSS -> item.category == ReportCategory.EXPENSE || item.category == ReportCategory.SALES_RETURN
                FinancialMetricFilter.CUSTOMER_DUE -> item.secondaryAmountMinor != null && item.secondaryAmountMinor > 0 && item.rawType == "INVOICE"
                FinancialMetricFilter.SUPPLIER_DUE -> item.secondaryAmountMinor != null && item.secondaryAmountMinor > 0 && item.rawType == "PURCHASE"
                FinancialMetricFilter.OPENING_BALANCE -> true
                FinancialMetricFilter.CLOSING_BALANCE -> true
                FinancialMetricFilter.DISCOUNT -> item.particulars.contains("DISCOUNT", ignoreCase = true)
                FinancialMetricFilter.TAX -> item.particulars.contains("TAX", ignoreCase = true) || item.particulars.contains("VAT", ignoreCase = true)
                FinancialMetricFilter.REFUND -> item.category == ReportCategory.SALES_RETURN
            }
            if (!matchFinancial) return@filter false

            // Amount Range Filter
            val matchAmount = when (filterState.amountFilter) {
                AmountRangeFilter.ALL -> true
                AmountRangeFilter.RANGE_0_1K -> item.amountMinor in 0L..100000L
                AmountRangeFilter.RANGE_1K_10K -> item.amountMinor in 100001L..1000000L
                AmountRangeFilter.RANGE_10K_50K -> item.amountMinor in 1000001L..5000000L
                AmountRangeFilter.ABOVE_50K -> item.amountMinor > 5000000L
                AmountRangeFilter.CUSTOM -> {
                    val min = filterState.customMinAmountMinor ?: 0L
                    val max = filterState.customMaxAmountMinor ?: Long.MAX_VALUE
                    item.amountMinor in min..max
                }
            }
            if (!matchAmount) return@filter false

            // Search Query Filter
            if (filterState.searchQuery.isNotBlank()) {
                val q = filterState.searchQuery.trim().lowercase()
                val matchSearch = item.referenceNumber.lowercase().contains(q) ||
                        item.primaryEntityName.lowercase().contains(q) ||
                        item.particulars.lowercase().contains(q) ||
                        (item.warehouseName?.lowercase()?.contains(q) == true) ||
                        (item.notes?.lowercase()?.contains(q) == true)
                if (!matchSearch) return@filter false
            }

            true
        }

        // Sort items
        filtered = when (filterState.sortBy) {
            SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.date }
            SortOption.OLDEST_FIRST -> filtered.sortedBy { it.date }
            SortOption.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amountMinor }
            SortOption.LOWEST_AMOUNT -> filtered.sortedBy { it.amountMinor }
            SortOption.NAME_ASC -> filtered.sortedBy { it.primaryEntityName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.primaryEntityName.lowercase() }
        }

        // Compute running balance if party ledger or single entity is selected
        if (filterState.category in listOf(ReportCategory.CUSTOMER_LEDGER, ReportCategory.SUPPLIER_LEDGER) ||
            filterState.selectedCustomerId != null || filterState.selectedSupplierId != null) {
            var running = 0L
            // Calculate opening balance before fromMillis
            if (filterState.selectedCustomerId != null) {
                val pastInvoices = invoices.filter { it.customerId == filterState.selectedCustomerId && it.invoiceDate < fromMillis && it.status != "CANCELLED" }
                val pastPayments = customerPayments.filter { it.customerId == filterState.selectedCustomerId && it.paymentDate < fromMillis && !it.isVoided }
                val pastReturns = salesReturns.filter { it.customerId == filterState.selectedCustomerId && it.returnDate < fromMillis }
                running = pastInvoices.sumOf { it.totalAmountMinor } - pastPayments.sumOf { it.amountMinor } - pastReturns.sumOf { it.totalReturnAmountMinor }
            } else if (filterState.selectedSupplierId != null) {
                val pastPos = purchaseOrders.filter { it.supplierId == filterState.selectedSupplierId && it.orderDate < fromMillis && it.status != "CANCELLED" }
                val pastPmts = supplierPayments.filter { it.supplierId == filterState.selectedSupplierId && it.paymentDate < fromMillis }
                running = pastPos.sumOf { it.totalBdtAmountMinor } - pastPmts.sumOf { it.bdtAmountMinor }
            }

            val withRunning = mutableListOf<UnifiedTransactionItem>()
            // Sort chronologically to compute running balances correctly
            val chronological = filtered.sortedBy { it.date }
            val mapRunning = mutableMapOf<String, Long>()
            for (it in chronological) {
                if (filterState.category == ReportCategory.SUPPLIER_LEDGER || filterState.selectedSupplierId != null) {
                    // Supplier: PO increases payable (+), Payment decreases payable (-)
                    if (it.rawType == "PURCHASE") running += it.amountMinor
                    else if (it.rawType == "SUPPLIER_PAYMENT") running -= it.amountMinor
                } else {
                    // Customer: Invoice increases receivable (+), Payment decreases receivable (-), Return decreases (-)
                    if (it.rawType == "INVOICE") running += it.amountMinor
                    else if (it.rawType == "PAYMENT") running -= it.amountMinor
                    else if (it.rawType == "RETURN") running -= it.amountMinor
                }
                mapRunning[it.id] = running
            }

            // Restore desired sort order with calculated running balances
            filtered = filtered.map { item ->
                item.copy(runningBalanceMinor = mapRunning[item.id])
            }
        }

        emit(filtered)
    }.flowOn(Dispatchers.IO)

    override fun getSummaryDashboard(filterState: ControlCenterFilterState): Flow<BusinessSummaryDashboardData> = flow {
        val (fromMillis, toMillis) = filterState.dateRangeOption.getBoundsMillis(filterState.customStartDate, filterState.customEndDate)

        val invoices = salesDao.getAllInvoicesSync()
        val customerPayments = customerPaymentDao.getAllPaymentsSync()
        val salesReturns = salesReturnDao.getAllReturnsSync()
        val purchaseOrders = purchaseOrderDao.getAllOrdersSync()
        val supplierPayments = supplierPaymentDao.getAllPaymentsSync()
        val expenses = expenseDao.getAllExpensesSync()

        // 1. Total Sales
        val rangeInvoices = invoices.filter { it.invoiceDate in fromMillis..toMillis && it.status != "CANCELLED" }
        val totalSales = rangeInvoices.sumOf { it.totalAmountMinor }
        val totalCogs = rangeInvoices.sumOf { it.totalCogsMinor }
        val totalDiscounts = rangeInvoices.sumOf { it.discountMinor }

        // 2. Total Purchase
        val rangePurchases = purchaseOrders.filter { it.orderDate in fromMillis..toMillis && it.status != "CANCELLED" }
        val totalPurchases = rangePurchases.sumOf { it.totalBdtAmountMinor }

        // 3. Total Received (Collections)
        val rangePayments = customerPayments.filter { it.paymentDate in fromMillis..toMillis && !it.isVoided }
        val totalReceived = rangePayments.sumOf { it.amountMinor }

        // 4. Total Paid (Supplier remittances)
        val rangeSupplierPmts = supplierPayments.filter { it.paymentDate in fromMillis..toMillis }
        val totalPaid = rangeSupplierPmts.sumOf { it.bdtAmountMinor }

        // 5. Total Expenses
        val rangeExpenses = expenses.filter { it.voucherDate in fromMillis..toMillis && !it.isReversed }
        val totalExpenses = rangeExpenses.sumOf { it.bdtAmountMinor }

        // 6. Sales Returns
        val rangeReturns = salesReturns.filter { it.returnDate in fromMillis..toMillis }
        val totalSalesReturns = rangeReturns.sumOf { it.totalReturnAmountMinor }

        // 7. Purchase Returns (Damage/Shortage write-offs)
        val totalPurchaseReturns = 0L

        // 8. Total Deliveries
        val totalDeliveries = rangeInvoices.count { it.status in listOf("DELIVERED", "PARTIALLY_DELIVERED") }

        // 9. Total Outstanding Receivable (Customer Due)
        val totalReceivable = rangeInvoices.sumOf { it.dueAmountMinor }

        // 10. Total Outstanding Payable (Supplier Due)
        val totalPayable = rangePurchases.sumOf { (it.totalBdtAmountMinor - it.paidBdtAmountMinor).coerceAtLeast(0L) }

        // 11. Gross Profit = Net Sales Revenue - COGS
        // Net Sales = Total Sales - Sales Returns - Discounts
        val netSales = (totalSales - totalSalesReturns - totalDiscounts).coerceAtLeast(0L)
        val grossProfit = netSales - totalCogs

        // 12. Net Profit = Gross Profit - Operating Expenses
        val netProfit = grossProfit - totalExpenses

        // Opening & Closing Balance for the business Cash/Bank
        val pastReceipts = customerPayments.filter { it.paymentDate < fromMillis && !it.isVoided }.sumOf { it.amountMinor }
        val pastPayments = supplierPayments.filter { it.paymentDate < fromMillis }.sumOf { it.bdtAmountMinor } +
                expenses.filter { it.voucherDate < fromMillis && !it.isReversed }.sumOf { it.bdtAmountMinor }
        val openingBal = pastReceipts - pastPayments
        val closingBal = openingBal + totalReceived - (totalPaid + totalExpenses)

        emit(
            BusinessSummaryDashboardData(
                totalSalesMinor = totalSales,
                totalPurchaseMinor = totalPurchases,
                totalReceivedMinor = totalReceived,
                totalPaidMinor = totalPaid,
                totalExpensesMinor = totalExpenses,
                totalSalesReturnMinor = totalSalesReturns,
                totalPurchaseReturnMinor = totalPurchaseReturns,
                totalDeliveryCount = totalDeliveries,
                totalOutstandingReceivableMinor = totalReceivable,
                totalOutstandingPayableMinor = totalPayable,
                grossProfitMinor = grossProfit,
                netProfitMinor = netProfit,
                totalTransactionsCount = rangeInvoices.size + rangePayments.size + rangeReturns.size + rangePurchases.size + rangeSupplierPmts.size + rangeExpenses.size,
                openingBalanceMinor = openingBal,
                closingBalanceMinor = closingBal
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun getTransactionDetails(rawType: String, id: String): TransactionDetailRecord? {
        return when (rawType) {
            "INVOICE" -> {
                val inv = salesDao.getInvoiceById(id) ?: return null
                val items = salesDao.getItemsForInvoice(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = it.productNameEn,
                        sku = it.sku,
                        variantInfo = null,
                        quantity = it.quantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.lineTotalMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = inv.id,
                    referenceNumber = inv.invoiceNumber,
                    date = inv.invoiceDate,
                    category = ReportCategory.SALES,
                    primaryEntityName = inv.customerName,
                    primaryEntityId = inv.customerId,
                    particulars = "Sales Invoice (${inv.status})",
                    amountMinor = inv.totalAmountMinor,
                    secondaryAmountMinor = inv.dueAmountMinor,
                    isInflow = true,
                    status = inv.status,
                    warehouseName = inv.warehouseName,
                    rawType = "INVOICE",
                    notes = inv.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = inv.subtotalMinor,
                    discountMinor = inv.discountMinor,
                    taxMinor = inv.taxMinor,
                    paidMinor = inv.paidAmountMinor,
                    dueMinor = inv.dueAmountMinor,
                    contactPhone = inv.customerPhone,
                    referenceDocument = inv.invoiceNumber,
                    auditTrailInfo = "Invoice created at ${java.util.Date(inv.createdAt)}. Updated at ${java.util.Date(inv.updatedAt)}."
                )
            }
            "PURCHASE" -> {
                val po = purchaseOrderDao.getOrderById(id) ?: return null
                val items = purchaseOrderDao.getItemsForOrderSync(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = "Product Item (${it.productId.take(8)})",
                        sku = it.variantId ?: "SKU-${it.productId.take(6)}",
                        quantity = it.quantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.totalAmountMinor
                    )
                }
                val supplier = supplierDao.getSupplierById(po.supplierId)
                val tx = UnifiedTransactionItem(
                    id = po.id,
                    referenceNumber = po.poNumber,
                    date = po.orderDate,
                    category = if (po.currency != "BDT") ReportCategory.IMPORT else ReportCategory.PURCHASE,
                    primaryEntityName = supplier?.companyName ?: "Supplier",
                    primaryEntityId = po.supplierId,
                    particulars = "Purchase Order [${po.currency}] - Status: ${po.status}",
                    amountMinor = po.totalBdtAmountMinor,
                    secondaryAmountMinor = po.totalBdtAmountMinor - po.paidBdtAmountMinor,
                    isInflow = false,
                    paymentMethod = po.currency,
                    status = po.status,
                    rawType = "PURCHASE",
                    notes = po.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = po.totalBdtAmountMinor,
                    paidMinor = po.paidBdtAmountMinor,
                    dueMinor = (po.totalBdtAmountMinor - po.paidBdtAmountMinor).coerceAtLeast(0L),
                    contactPhone = supplier?.phone,
                    contactAddress = supplier?.address,
                    referenceDocument = po.poNumber,
                    auditTrailInfo = "Purchase Order created: ${java.util.Date(po.createdAt)}. Currency: ${po.currency} at rate ${po.exchangeRateToBdt} BDT."
                )
            }
            "PAYMENT" -> {
                val pmt = customerPaymentDao.getPaymentById(id) ?: return null
                val allocations = customerPaymentDao.getAllocationsForPayment(id)
                val lineItems = allocations.map {
                    LineItemDetail(
                        productName = "Allocation for Invoice ${it.invoiceNumber}",
                        sku = it.invoiceId.take(8),
                        quantity = 1,
                        unitPriceMinor = it.amountMinor,
                        totalAmountMinor = it.amountMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = pmt.id,
                    referenceNumber = pmt.paymentNumber,
                    date = pmt.paymentDate,
                    category = ReportCategory.RECEIPT,
                    primaryEntityName = pmt.customerName,
                    primaryEntityId = pmt.customerId,
                    particulars = pmt.referenceNote ?: "Customer Receipt",
                    amountMinor = pmt.amountMinor,
                    secondaryAmountMinor = pmt.unallocatedCreditMinor,
                    isInflow = true,
                    paymentMethod = pmt.paymentMethod,
                    status = if (pmt.isVoided) "VOIDED" else "POSTED",
                    rawType = "PAYMENT",
                    notes = pmt.referenceNote
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = pmt.amountMinor,
                    paidMinor = pmt.amountMinor - pmt.unallocatedCreditMinor,
                    dueMinor = pmt.unallocatedCreditMinor,
                    referenceDocument = pmt.paymentNumber,
                    auditTrailInfo = "Payment recorded: ${java.util.Date(pmt.createdAt)}. Void status: ${pmt.isVoided}."
                )
            }
            "RETURN" -> {
                val ret = salesReturnDao.getReturnById(id) ?: return null
                val items = salesReturnDao.getItemsForReturn(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = it.productNameEn,
                        sku = it.variantId ?: it.productId.take(8),
                        variantInfo = null,
                        quantity = it.returnQuantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.returnTotalMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = ret.id,
                    referenceNumber = ret.returnNumber,
                    date = ret.returnDate,
                    category = ReportCategory.SALES_RETURN,
                    primaryEntityName = ret.customerName,
                    primaryEntityId = ret.customerId,
                    particulars = ret.reason ?: "Return against ${ret.invoiceNumber}",
                    amountMinor = ret.totalReturnAmountMinor,
                    secondaryAmountMinor = ret.totalCogsReversalMinor,
                    isInflow = false,
                    paymentMethod = ret.refundOption,
                    status = "RETURNED",
                    rawType = "RETURN",
                    notes = ret.reason
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = ret.totalReturnAmountMinor,
                    referenceDocument = ret.invoiceNumber,
                    auditTrailInfo = "Sales Return registered: ${java.util.Date(ret.createdAt)}. Reason: ${ret.reason ?: "N/A"}."
                )
            }
            "EXPENSE" -> {
                val exp = expenseDao.getExpenseById(id) ?: return null
                val tx = UnifiedTransactionItem(
                    id = exp.id,
                    referenceNumber = exp.voucherNumber,
                    date = exp.voucherDate,
                    category = ReportCategory.EXPENSE,
                    primaryEntityName = exp.category,
                    particulars = exp.descriptionEn.ifBlank { exp.descriptionBn },
                    amountMinor = exp.bdtAmountMinor,
                    isInflow = false,
                    paymentMethod = exp.currency,
                    status = if (exp.isReversed) "REVERSED" else "POSTED",
                    rawType = "EXPENSE",
                    notes = exp.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    subtotalMinor = exp.bdtAmountMinor,
                    referenceDocument = exp.voucherNumber,
                    auditTrailInfo = "Expense voucher created: ${java.util.Date(exp.createdAt)}. Payment account: ${exp.paymentAccountId}."
                )
            }
            else -> null
        }
    }

    override suspend fun getCustomerLedgerStatement(
        customerId: String,
        fromDate: Long,
        toDate: Long
    ): PartyLedgerStatement? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val invoices = salesDao.getInvoicesForCustomerSync(customerId).filter { it.status != "CANCELLED" }
        val payments = customerPaymentDao.getPaymentsForCustomerSync(customerId).filter { !it.isVoided }
        val returns = salesReturnDao.getReturnsForCustomerSync(customerId)

        // Opening Balance as of fromDate
        val pastInvoices = invoices.filter { it.invoiceDate < fromDate }.sumOf { it.totalAmountMinor }
        val pastPayments = payments.filter { it.paymentDate < fromDate }.sumOf { it.amountMinor }
        val pastReturns = returns.filter { it.returnDate < fromDate }.sumOf { it.totalReturnAmountMinor }
        val openingBalance = pastInvoices - (pastPayments + pastReturns)

        // Transactions in date range
        val periodTx = mutableListOf<UnifiedTransactionItem>()

        for (inv in invoices.filter { it.invoiceDate in fromDate..toDate }) {
            periodTx.add(
                UnifiedTransactionItem(
                    id = inv.id,
                    referenceNumber = inv.invoiceNumber,
                    date = inv.invoiceDate,
                    category = ReportCategory.SALES,
                    primaryEntityName = customer.name,
                    primaryEntityId = customer.id,
                    particulars = "Invoice #${inv.invoiceNumber} (${inv.status})",
                    amountMinor = inv.totalAmountMinor,
                    secondaryAmountMinor = inv.dueAmountMinor,
                    isInflow = true, // Debit to customer ledger (receivable increases)
                    status = inv.status,
                    warehouseName = inv.warehouseName,
                    rawType = "INVOICE"
                )
            )
        }

        for (pmt in payments.filter { it.paymentDate in fromDate..toDate }) {
            periodTx.add(
                UnifiedTransactionItem(
                    id = pmt.id,
                    referenceNumber = pmt.paymentNumber,
                    date = pmt.paymentDate,
                    category = ReportCategory.RECEIPT,
                    primaryEntityName = customer.name,
                    primaryEntityId = customer.id,
                    particulars = pmt.referenceNote ?: "Payment Received (${pmt.paymentMethod})",
                    amountMinor = pmt.amountMinor,
                    isInflow = false, // Credit to customer ledger (receivable decreases)
                    paymentMethod = pmt.paymentMethod,
                    status = "POSTED",
                    rawType = "PAYMENT"
                )
            )
        }

        for (ret in returns.filter { it.returnDate in fromDate..toDate }) {
            periodTx.add(
                UnifiedTransactionItem(
                    id = ret.id,
                    referenceNumber = ret.returnNumber,
                    date = ret.returnDate,
                    category = ReportCategory.SALES_RETURN,
                    primaryEntityName = customer.name,
                    primaryEntityId = customer.id,
                    particulars = ret.reason ?: "Return against #${ret.invoiceNumber}",
                    amountMinor = ret.totalReturnAmountMinor,
                    isInflow = false, // Credit to customer ledger (receivable decreases)
                    paymentMethod = ret.refundOption,
                    status = "RETURNED",
                    rawType = "RETURN"
                )
            )
        }

        // Sort chronologically and calculate running balance
        val chronological = periodTx.sortedBy { it.date }
        var running = openingBalance
        val calculated = mutableListOf<UnifiedTransactionItem>()
        var totalDebit = 0L
        var totalCredit = 0L

        for (tx in chronological) {
            if (tx.isInflow) {
                // Inflow/Debit (Invoice)
                running += tx.amountMinor
                totalDebit += tx.amountMinor
            } else {
                // Outflow/Credit (Payment or Return)
                running -= tx.amountMinor
                totalCredit += tx.amountMinor
            }
            calculated.add(tx.copy(runningBalanceMinor = running))
        }

        return PartyLedgerStatement(
            partyId = customer.id,
            partyName = customer.name,
            partyCode = customer.customerCode,
            partyType = "CUSTOMER",
            phone = customer.phone,
            address = customer.address,
            openingBalanceMinor = openingBalance,
            transactions = calculated.reversed(), // default newest first for display
            totalDebitMinor = totalDebit,
            totalCreditMinor = totalCredit,
            closingBalanceMinor = running
        )
    }

    override suspend fun getSupplierLedgerStatement(
        supplierId: String,
        fromDate: Long,
        toDate: Long
    ): PartyLedgerStatement? {
        val supplier = supplierDao.getSupplierById(supplierId) ?: return null
        val allPos = purchaseOrderDao.getAllOrdersSync().filter { it.supplierId == supplierId && it.status != "CANCELLED" }
        val allPmts = supplierPaymentDao.getAllPaymentsSync().filter { it.supplierId == supplierId }

        // Opening Balance as of fromDate
        val pastPos = allPos.filter { it.orderDate < fromDate }.sumOf { it.totalBdtAmountMinor }
        val pastPmts = allPmts.filter { it.paymentDate < fromDate }.sumOf { it.bdtAmountMinor }
        val openingBalance = pastPos - pastPmts // Net payable to supplier

        val periodTx = mutableListOf<UnifiedTransactionItem>()

        for (po in allPos.filter { it.orderDate in fromDate..toDate }) {
            periodTx.add(
                UnifiedTransactionItem(
                    id = po.id,
                    referenceNumber = po.poNumber,
                    date = po.orderDate,
                    category = if (po.currency != "BDT") ReportCategory.IMPORT else ReportCategory.PURCHASE,
                    primaryEntityName = supplier.companyName,
                    primaryEntityId = supplier.id,
                    particulars = "PO #${po.poNumber} [${po.currency}] - Status: ${po.status}",
                    amountMinor = po.totalBdtAmountMinor,
                    secondaryAmountMinor = po.totalBdtAmountMinor - po.paidBdtAmountMinor,
                    isInflow = true, // Payable increases (Credit in accounting terms)
                    paymentMethod = po.currency,
                    status = po.status,
                    rawType = "PURCHASE"
                )
            )
        }

        for (sp in allPmts.filter { it.paymentDate in fromDate..toDate }) {
            periodTx.add(
                UnifiedTransactionItem(
                    id = sp.id,
                    referenceNumber = sp.paymentNumber,
                    date = sp.paymentDate,
                    category = ReportCategory.PAYMENT,
                    primaryEntityName = supplier.companyName,
                    primaryEntityId = supplier.id,
                    particulars = "Supplier Remittance (${sp.paymentMethod})",
                    amountMinor = sp.bdtAmountMinor,
                    secondaryAmountMinor = sp.amountMinor,
                    isInflow = false, // Payable decreases (Debit in accounting terms)
                    paymentMethod = sp.paymentMethod,
                    status = "PAID",
                    rawType = "SUPPLIER_PAYMENT"
                )
            )
        }

        val chronological = periodTx.sortedBy { it.date }
        var running = openingBalance
        val calculated = mutableListOf<UnifiedTransactionItem>()
        var totalDebit = 0L // Payments made
        var totalCredit = 0L // POs billed

        for (tx in chronological) {
            if (tx.isInflow) {
                // PO increases payable
                running += tx.amountMinor
                totalCredit += tx.amountMinor
            } else {
                // Payment decreases payable
                running -= tx.amountMinor
                totalDebit += tx.amountMinor
            }
            calculated.add(tx.copy(runningBalanceMinor = running))
        }

        return PartyLedgerStatement(
            partyId = supplier.id,
            partyName = supplier.companyName,
            partyCode = supplier.supplierCode,
            partyType = "SUPPLIER",
            phone = supplier.phone,
            address = supplier.address,
            openingBalanceMinor = openingBalance,
            transactions = calculated.reversed(),
            totalDebitMinor = totalDebit,
            totalCreditMinor = totalCredit,
            closingBalanceMinor = running
        )
    }
}
