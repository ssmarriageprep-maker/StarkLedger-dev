package com.starklabs.moneytracker.data

import com.starklabs.moneytracker.data.AccountDao
import com.starklabs.moneytracker.data.CategoryDao
import com.starklabs.moneytracker.data.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MoneyRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) {
    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalSpent: Flow<Double?> = transactionDao.getTotalSpent()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()

    suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction)
        // Update account balance
        if (transaction.type == "DEBIT") {
            accountDao.deductBalance(transaction.accountId, transaction.amount)
        } else {
            accountDao.addBalance(transaction.accountId, transaction.amount)
        }
    }
    
    // Accounts
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun addAccount(account: Account) = accountDao.insert(account)
    suspend fun getAccount(id: Int) = accountDao.getAccountById(id)
    
    // Smart Account Finding for SMS
    suspend fun findAccountForSms(last4: String?): Account? {
        if (last4 == null) return null
        return accountDao.getAccountByMaskedNumber(last4)
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
    
    // Smart Category Matching
    suspend fun identifyCategory(merchant: String, body: String): Int? {
        val categories = categoryDao.getAllCategoriesOneShot()
        val text = "$merchant $body".lowercase()
        
        // 1. Check if merchant matches category name directly
        categories.find { text.contains(it.name.lowercase()) }?.let { return it.id }
        
        // 2. Check keywords
        categories.forEach { cat ->
            cat.keywords?.split(",")?.forEach { keyword ->
                if (keyword.isNotBlank() && text.contains(keyword.trim().lowercase())) {
                    return cat.id
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
        // Similarly for accounts, check if any exist
        if (accountDao.getAllAccounts().firstOrNull().isNullOrEmpty()) {
            val accounts = listOf(
                 Account(name = "Cash", type = "CASH", balance = 0.0, colorHex = "#00B0FF", maskedNumber = null)
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
