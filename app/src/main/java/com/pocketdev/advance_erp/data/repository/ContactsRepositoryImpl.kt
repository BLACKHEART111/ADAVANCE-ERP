package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.repository.ContactsRepository
import com.pocketdev.advance_erp.domain.repository.CustomerDeleteResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao
) : ContactsRepository {

    override fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()

    override fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()

    override suspend fun addCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
    }

    override suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    override suspend fun addSupplier(supplier: Supplier) {
        supplierDao.insertSupplier(supplier)
    }

    override suspend fun deleteOrArchiveCustomer(customerId: String, operator: String): CustomerDeleteResult {
        val customer = customerDao.getCustomerById(customerId)
            ?: return CustomerDeleteResult.Blocked("Customer not found: $customerId", 0L)

        // 1. Block if outstanding balance is non-zero
        if (customer.outstandingBalanceMinor != 0L) {
            val balanceFormatted = "৳" + String.format("%.2f", customer.outstandingBalanceMinor / 100.0)
            return CustomerDeleteResult.Blocked(
                reason = "Cannot delete customer '${customer.name}'. Outstanding due balance is $balanceFormatted. Settle all dues first.",
                balanceMinor = customer.outstandingBalanceMinor
            )
        }

        // 2. Check financial history: invoices, payments, returns
        val invoiceCount = salesDao.getInvoiceCountForCustomer(customerId)
        val paymentCount = customerPaymentDao.getPaymentCountForCustomer(customerId)
        val returnCount = salesReturnDao.getReturnCountForCustomer(customerId)
        val totalHistoryCount = invoiceCount + paymentCount + returnCount

        return db.withTransaction {
            if (totalHistoryCount > 0) {
                // Archive / Soft Delete
                customerDao.archiveCustomer(customerId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER",
                        recordId = customerId,
                        action = "ARCHIVE",
                        details = "Customer '${customer.name}' (${customer.customerCode}) archived. Found $totalHistoryCount financial records ($invoiceCount invoices, $paymentCount payments, $returnCount returns). Historical references preserved.",
                        operator = operator,
                        documentNumber = customer.customerCode,
                        operationType = "ARCHIVE",
                        previousStatus = "ACTIVE",
                        newStatus = "ARCHIVED"
                    )
                )
                CustomerDeleteResult.Archived(
                    message = "Customer '${customer.name}' has $totalHistoryCount historical transactions. Customer archived safely to preserve ledger audit trail.",
                    transactionCount = totalHistoryCount
                )
            } else {
                // Hard delete - absolutely no financial history
                customerDao.hardDeleteCustomer(customerId)
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER",
                        recordId = customerId,
                        action = "HARD_DELETE",
                        details = "Customer '${customer.name}' (${customer.customerCode}) permanently deleted (0 transactions found).",
                        operator = operator,
                        documentNumber = customer.customerCode,
                        operationType = "HARD_DELETE",
                        previousStatus = "ACTIVE",
                        newStatus = "DELETED"
                    )
                )
                CustomerDeleteResult.HardDeleted
            }
        }
    }
}
