package com.starklabs.moneytracker.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ------------------- ENTITIES -------------------

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "BANK", "CARD", "WALLET", "CASH"
    val balance: Double,
    val colorHex: String = "#00B0FF",
    val last4Digits: String? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String, 
    val budgetLimit: Double = 0.0,
    val colorHex: String = "#FFD700",
    val keywords: String? = null // Comma separated keywords for auto-detection e.g. "zomato,swiggy"
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["categoryId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val merchant: String,
    val date: Long,
    val type: String, // "DEBIT" or "CREDIT"
    val smsBody: String? = null,
    val accountId: Int, // Now strictly required
    val categoryId: Int? = null, // Can be null if uncategorized
    val notes: String? = null
)

// ------------------- DAOS -------------------

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?
    
    // Find account by matching last 4 digits
    @Query("SELECT * FROM accounts WHERE last4Digits = :last4 LIMIT 1")
    suspend fun getAccountByLast4(last4: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :id")
    suspend fun deductBalance(id: Int, amount: Double)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun addBalance(id: Int, amount: Double)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOneShot(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsOneShot(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end")
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>>
    
    // Filtered by Account
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("UPDATE transactions SET accountId = :targetId WHERE accountId = :sourceId")
    suspend fun reassignTransactions(sourceId: Int, targetId: Int)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'")
    fun getTotalIncome(): Flow<Double?>
    
    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' GROUP BY categoryId")
    fun getCategorySpending(): Flow<List<CategorySpending>>
}

data class CategorySpending(
    val categoryId: Int?,
    val total: Double
)

@Entity(tableName = "merchant_mappings")
data class MerchantCategoryMapping(
    @PrimaryKey val merchantName: String, // Normalized merchant name
    val categoryId: Int
)

@Dao
interface MerchantCategoryMappingDao {
    @Query("SELECT * FROM merchant_mappings WHERE merchantName = :merchantName COLLATE NOCASE")
    suspend fun getMapping(merchantName: String): MerchantCategoryMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mapping: MerchantCategoryMapping)
}

// ------------------- DATABASE -------------------

@Database(
    entities = [Transaction::class, Account::class, Category::class, MerchantCategoryMapping::class], 
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantMappingDao(): MerchantCategoryMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneytracker_db"
                )
                // Only allow destructive migration in debug builds.
                // In release, a missing migration will throw IllegalStateException
                // (fail-loud) instead of silently wiping all user data.
                if (com.starklabs.moneytracker.BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
