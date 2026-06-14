package com.starklabs.moneytracker.data

import android.content.Context
import com.starklabs.moneytracker.data.AccountDao
import com.starklabs.moneytracker.data.CategoryDao
import com.starklabs.moneytracker.data.TransactionDao
import com.starklabs.moneytracker.data.MerchantCategoryMappingDao
import com.starklabs.moneytracker.data.MerchantAlias
import com.starklabs.moneytracker.data.MerchantAliasDao
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class MoneyRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val merchantMappingDao: MerchantCategoryMappingDao,
    private val merchantAliasDao: MerchantAliasDao
) {
    // Cache compiled patterns for category/keyword matching to avoid thousands
    // of redundant Regex compilations during bulk SMS scans.
    private val patternCache = ConcurrentHashMap<String, Pattern>()

    companion object {
        @Volatile
        private var INSTANCE: MoneyRepository? = null

        fun getInstance(context: Context): MoneyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    MoneyRepository(
                        db.transactionDao(),
                        db.accountDao(),
                        db.categoryDao(),
                        db.merchantMappingDao(),
                        db.merchantAliasDao()
                    ).also { INSTANCE = it }
                }
            }
        }
    }

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalSpent: Flow<Double?> = transactionDao.getTotalSpent()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()

    /**
     * Inserts a transaction and updates the account balance.
     * Returns the inserted row ID, or -1 if the row was ignored due to a conflict.
     * Account balance is only adjusted when an insert actually happened.
     */
    suspend fun addTransaction(transaction: Transaction, smsBalance: Double? = null): Long {
        val rowId = transactionDao.insert(transaction)
        if (rowId == -1L) return rowId

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
        return rowId
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
    // Transactions referencing this category have categoryId set to NULL (FK onDelete = SET_NULL).
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)
    
    
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
        categories.find { category ->
            val name = category.name.lowercase()
            if (text.contains(name)) {
                val pattern = patternCache.getOrPut("\\b$name\\b") {
                    Pattern.compile("\\b$name\\b")
                }
                pattern.matcher(text).find()
            } else false
        }?.let { return it.id }
        
        // 3. Check keywords with strict word boundaries
        categories.forEach { cat ->
            cat.keywords?.split(",")?.forEach { keyword ->
                val kw = keyword.trim().lowercase()
                if (kw.isNotBlank() && text.contains(kw)) {
                    val pattern = patternCache.getOrPut("\\b$kw\\b") {
                        Pattern.compile("\\b$kw\\b")
                    }
                    if (pattern.matcher(text).find()) {
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
    
    // ---- Merchant Aliases (Sprint 3A) ----

    /** Live stream of all aliases, ordered by canonical name then alias text. */
    val allMerchantAliases: Flow<List<MerchantAlias>> = merchantAliasDao.getAllAliases()

    /**
     * Resolves [raw] to a [MerchantResolution]: alias table checked first (confidence 100),
     * falling back to [MerchantNormalizationEngine] (confidence 40–95).
     */
    suspend fun resolveMerchant(raw: String): MerchantResolution {
        val key = raw.trim().lowercase()
        val alias = merchantAliasDao.getAliasByKey(key)
        return if (alias != null) MerchantResolution(alias.canonicalMerchant, 100)
        else MerchantNormalizationEngine.normalize(raw)
    }

    /**
     * Persists a single alias mapping [rawAlias] → [canonicalName].
     *
     * Self-mapping ([rawAlias] == [canonicalName] case-insensitively) deletes any existing
     * override, reverting resolution to the rule engine.
     *
     * Collapse: if [canonicalName] is itself an alias of another canonical Z, Z is used instead
     * to guarantee the no-chain invariant (single-hop resolution at all times).
     */
    suspend fun setMerchantAlias(rawAlias: String, canonicalName: String, source: String = "USER") {
        val aliasKey = rawAlias.trim().lowercase()
        val canonical = canonicalName.trim()

        if (aliasKey == canonical.lowercase()) {
            merchantAliasDao.deleteByAliasKey(aliasKey)
            MerchantNormalizationEngine.clearCache()
            return
        }

        // Collapse: if the target canonical is itself aliased, follow through to the final name.
        val existingForCanonical = merchantAliasDao.getAliasByKey(canonical.lowercase())
        val finalCanonical = existingForCanonical?.canonicalMerchant ?: canonical

        val existing = merchantAliasDao.getAliasByKey(aliasKey)
        if (existing != null) {
            merchantAliasDao.update(existing.copy(canonicalMerchant = finalCanonical, source = source))
        } else {
            merchantAliasDao.insert(
                MerchantAlias(
                    alias = rawAlias.trim(),
                    aliasKey = aliasKey,
                    canonicalMerchant = finalCanonical,
                    source = source
                )
            )
        }
        MerchantNormalizationEngine.clearCache()
    }

    /**
     * Renames a canonical merchant [from] → [to], cascading all existing aliases that pointed
     * to [from] so they now point to [to]. Maintains the no-loop invariant:
     *   - self-rename is a no-op
     *   - if renaming would create a cycle (resolved [to] == [from]), the call is a no-op
     *   - if [to] is itself an alias, the chain is collapsed to its canonical
     */
    suspend fun renameMerchant(from: String, to: String, source: String = "USER") {
        val fromTrimmed = from.trim()
        val toTrimmed = to.trim()
        if (fromTrimmed.equals(toTrimmed, ignoreCase = true)) return

        // Collapse: if `to` is an alias of Z, use Z as the final canonical.
        val existingForTo = merchantAliasDao.getAliasByKey(toTrimmed.lowercase())
        val finalCanonical = existingForTo?.canonicalMerchant ?: toTrimmed

        // Loop guard: renaming A → B where B eventually resolves back to A is rejected.
        if (finalCanonical.equals(fromTrimmed, ignoreCase = true)) return

        // Cascade all existing aliases that pointed to `from` → now point to `finalCanonical`.
        merchantAliasDao.reassignCanonical(
            oldCanonicalKey = fromTrimmed.lowercase(),
            newCanonical = finalCanonical
        )

        // Record the rename itself as an alias so raw strings matching `from` resolve correctly.
        setMerchantAlias(rawAlias = fromTrimmed, canonicalName = finalCanonical, source = source)
        // setMerchantAlias already calls clearCache(); no need to call it again here.
    }

    /** Deletes an alias row. Resolution reverts to [MerchantNormalizationEngine] for that key. */
    suspend fun deleteMerchantAlias(alias: MerchantAlias) {
        merchantAliasDao.delete(alias)
        MerchantNormalizationEngine.clearCache()
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
