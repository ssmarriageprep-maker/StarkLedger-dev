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
    val type: String, // "CASH", "BANK", "CREDIT_CARD", "UPI"
    val balance: Double,
    val colorHex: String = "#00B0FF",
    val maskedNumber: String? = null // e.g., "1234" to match with SMS "ac 1234"
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
    @Query("SELECT * FROM accounts WHERE maskedNumber = :last4 LIMIT 1")
    suspend fun getAccountByMaskedNumber(last4: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

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
    suspend fun insert(category: Category)
    
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

// ------------------- DATABASE -------------------

@Database(entities = [Transaction::class, Account::class, Category::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // FALLBACK DESTRUCTIVE MIGRATION FOR DEVELOPMENT SPEED
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneytracker_db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
