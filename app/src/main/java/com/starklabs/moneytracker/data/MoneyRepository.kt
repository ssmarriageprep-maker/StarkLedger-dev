package com.starklabs.moneytracker.data

import com.starklabs.moneytracker.data.AccountDao
import com.starklabs.moneytracker.data.CategoryDao
import com.starklabs.moneytracker.data.TransactionDao
import com.starklabs.moneytracker.data.MerchantCategoryMappingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MoneyRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val merchantMappingDao: MerchantCategoryMappingDao
) {
    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalSpent: Flow<Double?> = transactionDao.getTotalSpent()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()

    suspend fun addTransaction(transaction: Transaction, smsBalance: Double? = null) {
        transactionDao.insert(transaction)
        // Update account balance
        if (smsBalance != null) {
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                // Trust the SMS balance as the source of truth
                accountDao.update(account.copy(balance = smsBalance))
            }
        } else {
            if (transaction.type == "DEBIT") {
                accountDao.deductBalance(transaction.accountId, transaction.amount)
            } else {
                accountDao.addBalance(transaction.accountId, transaction.amount)
            }
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
        // Reverse account balance
        if (transaction.type == "DEBIT") {
            accountDao.addBalance(transaction.accountId, transaction.amount)
        } else {
            accountDao.deductBalance(transaction.accountId, transaction.amount)
        }
    }

    suspend fun updateTransactionCategory(transactionId: Int, newCategoryId: Int, merchant: String? = null) {
        val allTx = transactionDao.getAllTransactionsOneShot()
        val tx = allTx.find { it.id == transactionId }
        if (tx != null) {
            transactionDao.update(tx.copy(categoryId = newCategoryId))
            if (merchant != null) {
                merchantMappingDao.insertOrUpdate(com.starklabs.moneytracker.data.MerchantCategoryMapping(merchant.trim(), newCategoryId))
            }
        }
    }
    
    // Accounts
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun addAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun getAccount(id: Int) = accountDao.getAccountById(id)
    
    suspend fun mergeAccounts(sourceId: Int, targetId: Int) {
        transactionDao.reassignTransactions(sourceId, targetId)
        val sourceAccount = accountDao.getAccountById(sourceId)
        if (sourceAccount != null) {
            accountDao.addBalance(targetId, sourceAccount.balance)
            accountDao.delete(sourceAccount)
        }
    }

    suspend fun deleteAccount(account: Account) {
        // First reassign transactions to default account
        val defaultAccountId = getDefaultAccount()?.id ?: 1
        transactionDao.reassignTransactions(account.id, defaultAccountId)
        accountDao.delete(account)
    }

    // Smart Account Finding for SMS
    suspend fun findAccountForSms(last4: String?): Account? {
        if (last4 == null) return null
        return accountDao.getAccountByLast4(last4)
    }

    // Default Account (Fallback)
    suspend fun getDefaultAccount(): Account? {
        // Return Cash account or just the first available one
        val account = accountDao.getAllAccounts().firstOrNull()?.firstOrNull { it.type == "CASH" }
        return account ?: accountDao.getAllAccounts().firstOrNull()?.firstOrNull()
    }

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun addCategory(category: Category) = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.insert(category) // insert with REPLACE acts as update
    
    
    // Smart Category Matching
    suspend fun identifyCategory(
        merchant: String,
        body: String,
        preFetchedCategories: List<Category>? = null
    ): Int? {
        // 1. Check user-defined merchant mapping override
        val override = merchantMappingDao.getMapping(merchant.trim())
        if (override != null) {
            return override.categoryId
        }

        val categories = preFetchedCategories ?: categoryDao.getAllCategoriesOneShot()
        if (categories.isEmpty()) return null

        val text = "$merchant $body".lowercase()
        
        // 2. Check if merchant matches category name exactly
        categories.find {
            // Using a simple contains check before doing regex if possible,
            // or just use a more efficient way to match.
            // For performance, we'll use a pre-compiled regex if we were to do this at scale,
            // but since we are in a suspend function, we'll at least avoid redundant DB calls.
            text.contains(it.name.lowercase()) &&
            text.contains(Regex("\\b${it.name.lowercase()}\\b"))
        }?.let { return it.id }
        
        // 3. Check keywords with strict word boundaries
        categories.forEach { cat ->
            cat.keywords?.split(",")?.forEach { keyword ->
                val kw = keyword.trim().lowercase()
                if (kw.isNotBlank() && text.contains(kw)) {
                    if (text.contains(Regex("\\b$kw\\b"))) {
                        return cat.id
                    }
                }
            }
        }
        return null // Uncategorized
    }
    
    suspend fun seedDefaults() {
        if (categoryDao.getAllCategoriesOneShot().isEmpty()) {
            val categories = listOf(
                Category(name = "Food", iconName = "restaurant", budgetLimit = 5000.0, colorHex = "#FFD700", keywords = "swiggy,zomato,mcdonalds,starbucks,cafe"),
                Category(name = "Rent", iconName = "home", budgetLimit = 15000.0, colorHex = "#00B0FF", keywords = "rent,landlord,housing"),
                Category(name = "Travel", iconName = "commute", budgetLimit = 3000.0, colorHex = "#FF6D00", keywords = "uber,ola,train,flight,fuel,petrol"),
                Category(name = "Shopping", iconName = "shopping_bag", budgetLimit = 5000.0, colorHex = "#B3001B", keywords = "amazon,flipkart,myntra,mall"),
                Category(name = "Bills", iconName = "receipt", budgetLimit = 2000.0, colorHex = "#00E6FF", keywords = "electricity,water,gas,broadband,mobile")
            )
            categoryDao.insertAll(categories)
        }
        
        // Similarly for accounts, check if any exist
        if (accountDao.getAllAccounts().firstOrNull().isNullOrEmpty()) {
            val accounts = listOf(
                 Account(name = "Cash", type = "CASH", balance = 0.0, colorHex = "#00B0FF", last4Digits = null)
            )
            accounts.forEach { accountDao.insert(it) }
        }
    }
    
    // Export Logic
    suspend fun getExportDataCsv(): String {
        val transactions = transactionDao.getAllTransactionsOneShot()
        val builder = StringBuilder()
        builder.append("Date,Merchant,Amount,Type,Account ID,Category ID,Notes\n")
        transactions.forEach { t ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            builder.append("$date,\"${t.merchant}\",${t.amount},${t.type},${t.accountId},${t.categoryId ?: ""},\"${t.notes ?: ""}\"\n")
        }
        return builder.toString()
    }
}
