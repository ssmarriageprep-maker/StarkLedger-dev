package com.starklabs.moneytracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
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
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        // Unique hash of the source SMS so re-scanning the inbox never creates
        // duplicates. NULL for manually-entered transactions (SQLite treats
        // multiple NULLs as distinct, so manual entries never collide).
        Index(value = ["smsHash"], unique = true)
    ]
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
    val notes: String? = null,
    val smsHash: String? = null // SHA-256 of the parsed SMS, used for dedup
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

    @Delete
    suspend fun delete(category: Category)
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

// ------------------- MERCHANT ALIASES -------------------

/**
 * Persisted merchant alias: maps a raw/variant merchant string to a user-confirmed canonical name.
 *
 * [alias]    — original user-visible text (display / audit)
 * [aliasKey] — alias.trim().lowercase() — the normalized lookup key; has a UNIQUE index so
 *              "AMAZON PAY INDIA", "amazon pay india", and "Amazon Pay India" collapse to one row.
 * [source]   — "USER" (user-created) or "SYSTEM" (seed / auto-generated)
 */
@Entity(
    tableName = "merchant_aliases",
    indices = [
        Index(value = ["canonicalMerchant"]),         // fast "list aliases of X" queries
        Index(value = ["aliasKey"], unique = true)    // enforces one canonical per normalized key
    ]
)
data class MerchantAlias(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val aliasKey: String,          // alias.trim().lowercase()
    val canonicalMerchant: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "USER"    // "USER" | "SYSTEM"
)

@Dao
interface MerchantAliasDao {
    @Query("SELECT * FROM merchant_aliases ORDER BY canonicalMerchant, alias")
    fun getAllAliases(): Flow<List<MerchantAlias>>

    @Query("SELECT * FROM merchant_aliases ORDER BY canonicalMerchant, alias")
    suspend fun getAllAliasesOneShot(): List<MerchantAlias>

    @Query("SELECT * FROM merchant_aliases WHERE aliasKey = :aliasKey LIMIT 1")
    suspend fun getAliasByKey(aliasKey: String): MerchantAlias?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(alias: MerchantAlias): Long

    @Update
    suspend fun update(alias: MerchantAlias)

    @Delete
    suspend fun delete(alias: MerchantAlias)

    @Query("DELETE FROM merchant_aliases WHERE aliasKey = :aliasKey")
    suspend fun deleteByAliasKey(aliasKey: String)

    // Cascade rename: all aliases pointing to oldCanonical are re-pointed to newCanonical.
    @Query("UPDATE merchant_aliases SET canonicalMerchant = :newCanonical WHERE lower(canonicalMerchant) = :oldCanonicalKey")
    suspend fun reassignCanonical(oldCanonicalKey: String, newCanonical: String)
}

// ------------------- DATABASE -------------------

@Database(
    entities = [Transaction::class, Account::class, Category::class, MerchantCategoryMapping::class, MerchantAlias::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantMappingDao(): MerchantCategoryMappingDao
    abstract fun merchantAliasDao(): MerchantAliasDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v5 -> v6: add smsHash column + unique index for cross-scan deduplication.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsHash TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_smsHash ON transactions(smsHash)")
            }
        }

        // v6 -> v7: add merchant_aliases table for user-extensible canonical merchant names.
        // Additive only — no changes to existing tables or data.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS merchant_aliases (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alias            TEXT    NOT NULL,
                        aliasKey         TEXT    NOT NULL,
                        canonicalMerchant TEXT   NOT NULL,
                        createdAt        INTEGER NOT NULL,
                        source           TEXT    NOT NULL DEFAULT 'USER'
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_merchant_aliases_canonicalMerchant ON merchant_aliases(canonicalMerchant)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_aliases_aliasKey ON merchant_aliases(aliasKey)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneytracker_db"
                ).addMigrations(MIGRATION_5_6, MIGRATION_6_7)
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
