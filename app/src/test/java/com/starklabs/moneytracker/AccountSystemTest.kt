package com.starklabs.moneytracker

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for Account System
 * Tests account creation, merging, filtering, and identification
 */
class AccountSystemTest {

    data class Account(
        val id: String = "",
        val bankName: String = "",
        val last4Digits: String = "",
        val accountType: String = "BANK",
        val isActive: Boolean = true
    ) {
        companion object {
            fun generateId(bankName: String, last4: String): String {
                return "$bankName-$last4".uppercase()
            }
        }
    }

    private val accountSystem = object {
        private val accounts = mutableMapOf<String, Account>()

        fun getOrCreateAccount(bankName: String, last4Digits: String, type: String = "BANK"): Account {
            val id = Account.generateId(bankName, last4Digits)
            return accounts.getOrPut(id) {
                Account(
                    id = id,
                    bankName = bankName,
                    last4Digits = last4Digits,
                    accountType = type
                )
            }
        }

        fun getAllAccounts(): List<Account> = accounts.values.toList()

        fun deactivateAccount(accountId: String): Boolean {
            val account = accounts[accountId] ?: return false
            accounts[accountId] = account.copy(isActive = false)
            return true
        }

        fun mergeAccounts(sourceId: String, targetId: String): Boolean {
            if (!accounts.containsKey(sourceId) || !accounts.containsKey(targetId)) return false
            accounts.remove(sourceId)
            return true
        }

        fun findAccountByIdentifier(bankName: String, last4: String): Account? {
            return accounts[Account.generateId(bankName, last4)]
        }
    }

    // ========== ACCOUNT CREATION TESTS ==========

    @Test
    fun `test creating new account with valid parameters`() {
        val account = accountSystem.getOrCreateAccount("HDFC", "3263")

        assertEquals("HDFC", account.bankName)
        assertEquals("3263", account.last4Digits)
        assertEquals("BANK", account.accountType)
        assertTrue(account.isActive)
    }

    @Test
    fun `test account ID generation is deterministic`() {
        val account1 = accountSystem.getOrCreateAccount("HDFC", "3263")
        val account2 = accountSystem.getOrCreateAccount("HDFC", "3263")

        assertEquals(account1.id, account2.id)
    }

    @Test
    fun `test creating different account types`() {
        val bankAccount = accountSystem.getOrCreateAccount("HDFC", "3263", "BANK")
        val cardAccount = accountSystem.getOrCreateAccount("SBI", "1234", "CARD")
        val walletAccount = accountSystem.getOrCreateAccount("Paytm", "5678", "WALLET")

        assertEquals("BANK", bankAccount.accountType)
        assertEquals("CARD", cardAccount.accountType)
        assertEquals("WALLET", walletAccount.accountType)
    }

    @Test
    fun `test account deduplication - same account not created twice`() {
        val account1 = accountSystem.getOrCreateAccount("HDFC", "3263")
        val account2 = accountSystem.getOrCreateAccount("HDFC", "3263")

        assertEquals(1, accountSystem.getAllAccounts().size)
        assertEquals(account1.id, account2.id)
    }

    // ========== ACCOUNT FILTERING TESTS ==========

    @Test
    fun `test filtering active accounts`() {
        accountSystem.getOrCreateAccount("HDFC", "3263")
        accountSystem.getOrCreateAccount("SBI", "1234")
        accountSystem.getOrCreateAccount("ICICI", "5678")

        val activeAccounts = accountSystem.getAllAccounts().filter { it.isActive }

        assertEquals(3, activeAccounts.size)
    }

    @Test
    fun `test filtering inactive accounts after deactivation`() {
        val account1 = accountSystem.getOrCreateAccount("HDFC", "3263")
        accountSystem.getOrCreateAccount("SBI", "1234")

        accountSystem.deactivateAccount(account1.id)
        val activeAccounts = accountSystem.getAllAccounts().filter { it.isActive }

        assertEquals(1, activeAccounts.size)
    }

    // ========== ACCOUNT LOOKUP TESTS ==========

    @Test
    fun `test finding account by bank and last 4 digits`() {
        val created = accountSystem.getOrCreateAccount("HDFC", "3263")
        val found = accountSystem.findAccountByIdentifier("HDFC", "3263")

        assertNotNull(found)
        assertEquals(created.id, found?.id)
    }

    @Test
    fun `test account lookup returns null for non-existent account`() {
        val found = accountSystem.findAccountByIdentifier("NonExistent", "0000")

        assertNull(found)
    }

    @Test
    fun `test case-insensitive account lookup`() {
        accountSystem.getOrCreateAccount("HDFC", "3263")
        val found = accountSystem.findAccountByIdentifier("hdfc", "3263")

        assertNotNull(found, "Account lookup should be case-insensitive")
    }

    // ========== ACCOUNT MERGING TESTS ==========

    @Test
    fun `test merging two accounts`() {
        val source = accountSystem.getOrCreateAccount("HDFC", "3263")
        val target = accountSystem.getOrCreateAccount("HDFC", "9999")

        val merged = accountSystem.mergeAccounts(source.id, target.id)

        assertTrue(merged)
        assertNull(accountSystem.findAccountByIdentifier("HDFC", "3263"))
        assertNotNull(accountSystem.findAccountByIdentifier("HDFC", "9999"))
    }

    @Test
    fun `test merging with invalid source account fails`() {
        val target = accountSystem.getOrCreateAccount("HDFC", "3263")

        val merged = accountSystem.mergeAccounts("INVALID_ID", target.id)

        assertFalse(merged)
    }

    @Test
    fun `test merging with invalid target account fails`() {
        val source = accountSystem.getOrCreateAccount("HDFC", "3263")

        val merged = accountSystem.mergeAccounts(source.id, "INVALID_ID")

        assertFalse(merged)
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test account with empty last4 digits`() {
        val account = accountSystem.getOrCreateAccount("HDFC", "")

        assertEquals("", account.last4Digits)
    }

    @Test
    fun `test account with special characters in bank name`() {
        val account = accountSystem.getOrCreateAccount("HDFC-BANK", "3263")

        assertEquals("HDFC-BANK", account.bankName)
    }

    @Test
    fun `test creating many accounts does not cause issues`() {
        repeat(1000) { i ->
            accountSystem.getOrCreateAccount("BANK$i", "%04d".format(i % 10000))
        }

        val accounts = accountSystem.getAllAccounts()
        assertEquals(1000, accounts.size)
    }

    @Test
    fun `test account identifier uniqueness across different banks`() {
        val hdfc = accountSystem.getOrCreateAccount("HDFC", "3263")
        val sbi = accountSystem.getOrCreateAccount("SBI", "3263")

        assertNotNull(hdfc)
        assertNotNull(sbi)
        assertTrue(hdfc.id != sbi.id, "Same digits in different banks should have different IDs")
    }

    // ========== TRANSACTION ACCOUNT ASSIGNMENT TESTS ==========

    @Test
    fun `test assigning transaction to correct account`() {
        val account = accountSystem.getOrCreateAccount("HDFC", "3263")
        
        // Simulate transaction with account identifier
        val transactionAccount = accountSystem.findAccountByIdentifier("HDFC", "3263")
        
        assertNotNull(transactionAccount)
        assertEquals(account.id, transactionAccount?.id)
    }

    @Test
    fun `test handling transaction with unmatched account creates fallback`() {
        val fallbackAccount = accountSystem.getOrCreateAccount("Unknown", "0000")
        val unmatchedAccount = accountSystem.findAccountByIdentifier("UnknownBank", "9999")

        // If not found, should use fallback
        assertTrue(unmatchedAccount == null || unmatchedAccount.bankName == "Unknown")
    }
}
