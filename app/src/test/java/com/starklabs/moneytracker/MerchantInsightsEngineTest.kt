package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.domain.CategoryAmount
import com.starklabs.moneytracker.domain.MerchantInsightType
import com.starklabs.moneytracker.domain.MerchantInsightsEngine
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TrendDirection
import org.junit.Assert.*
import org.junit.Test

class MerchantInsightsEngineTest {

    // ── helpers ───────────────────────────────────────────────────────────────────

    private fun summary(
        name: String,
        totalSpent: Double = 500.0,
        txCount: Int = 5,
        avg: Double = if (txCount > 0) totalSpent / txCount else 0.0,
        largest: Double = totalSpent,
        monthsActive: Int = 1,
        freqPerMonth: Double = if (monthsActive > 0) txCount.toDouble() / monthsActive else 0.0,
        trend: TrendDirection = TrendDirection.STABLE,
        catAmounts: List<CategoryAmount> = emptyList()
    ) = MerchantSummary(
        canonicalName = name,
        totalSpent = totalSpent,
        transactionCount = txCount,
        averageTransaction = avg,
        largestTransaction = largest,
        firstSeen = 0L,
        lastSeen = 1_000L,
        monthsActive = monthsActive,
        frequencyPerMonth = freqPerMonth,
        trendDirection = trend,
        topCategories = catAmounts,
        recentTransactions = emptyList()
    )

    private fun cat(id: Int, name: String) =
        Category(id = id, name = name, iconName = "category", colorHex = "#808080", budgetLimit = 1000.0)

    private fun catAmount(catId: Int, name: String, amount: Double) =
        CategoryAmount(categoryId = catId, name = name, colorHex = "#808080", amount = amount)

    private fun generate(
        current: List<MerchantSummary>,
        previous: List<MerchantSummary> = emptyList(),
        categories: List<Category> = emptyList(),
        minSpend: Double = 100.0,
        changePct: Double = 15.0
    ) = MerchantInsightsEngine.generate(current, previous, categories, minSpend, changePct)

    // ── empty states ──────────────────────────────────────────────────────────────

    @Test fun `empty current summaries returns empty list`() {
        assertTrue(generate(emptyList()).isEmpty())
    }

    @Test fun `no previous summaries does not crash`() {
        val result = generate(listOf(summary("Amazon", 500.0)), emptyList())
        assertNotNull(result)
    }

    @Test fun `merchant with zero total spend produces no concentration insight`() {
        val s = summary("Amazon", totalSpent = 0.0, txCount = 3)
        val result = generate(listOf(s))
        assertTrue(result.none { it.type == MerchantInsightType.MERCHANT_CONCENTRATION })
    }

    // ── spending increase ─────────────────────────────────────────────────────────

    @Test fun `spending increase of exactly 15 percent is detected`() {
        val curr = listOf(summary("Amazon", 1150.0))
        val prev = listOf(summary("Amazon", 1000.0))
        val result = generate(curr, prev, changePct = 15.0)
        assertTrue(result.any { it.type == MerchantInsightType.SPENDING_INCREASE })
    }

    @Test fun `spending increase below 15 percent is suppressed`() {
        val curr = listOf(summary("Amazon", 1140.0))
        val prev = listOf(summary("Amazon", 1000.0))
        val result = generate(curr, prev, changePct = 15.0)
        assertFalse(result.any { it.type == MerchantInsightType.SPENDING_INCREASE })
    }

    @Test fun `spending increase description contains merchant name and percentage`() {
        val curr = listOf(summary("Swiggy", 600.0))
        val prev = listOf(summary("Swiggy", 400.0))
        val result = generate(curr, prev)
        val insight = result.first { it.type == MerchantInsightType.SPENDING_INCREASE }
        assertTrue(insight.description.contains("Swiggy"))
        assertTrue(insight.description.contains("50"))
    }

    @Test fun `at most 2 SPENDING_INCREASE insights are emitted`() {
        val curr = (1..5).map { summary("M$it", 1000.0) }
        val prev = (1..5).map { summary("M$it", 200.0) }
        val result = generate(curr, prev)
        assertTrue(result.count { it.type == MerchantInsightType.SPENDING_INCREASE } <= 2)
    }

    @Test fun `zero previous spend does not generate spending increase`() {
        val curr = listOf(summary("NewMerchant", 500.0))
        val prev = listOf(summary("NewMerchant", 0.0))
        val result = generate(curr, prev)
        assertFalse(result.any { it.type == MerchantInsightType.SPENDING_INCREASE })
    }

    @Test fun `merchant absent from previous period produces no spending change insight`() {
        val curr = listOf(summary("NewMerchant", 500.0))
        val result = generate(curr, emptyList())
        assertFalse(result.any {
            it.type == MerchantInsightType.SPENDING_INCREASE ||
            it.type == MerchantInsightType.SPENDING_DECREASE
        })
    }

    // ── spending decrease ─────────────────────────────────────────────────────────

    @Test fun `spending decrease of exactly 15 percent is detected`() {
        val curr = listOf(summary("Amazon", 850.0))
        val prev = listOf(summary("Amazon", 1000.0))
        val result = generate(curr, prev, changePct = 15.0)
        assertTrue(result.any { it.type == MerchantInsightType.SPENDING_DECREASE })
    }

    @Test fun `spending decrease description contains merchant name and percentage`() {
        val curr = listOf(summary("Netflix", 500.0))
        val prev = listOf(summary("Netflix", 1000.0))
        val result = generate(curr, prev)
        val insight = result.first { it.type == MerchantInsightType.SPENDING_DECREASE }
        assertTrue(insight.description.contains("Netflix"))
        assertTrue(insight.description.contains("50"))
    }

    @Test fun `at most 1 SPENDING_DECREASE insight is emitted`() {
        val curr = (1..5).map { summary("M$it", 100.0) }
        val prev = (1..5).map { summary("M$it", 1000.0) }
        val result = generate(curr, prev)
        assertTrue(result.count { it.type == MerchantInsightType.SPENDING_DECREASE } <= 1)
    }

    // ── min spend threshold ───────────────────────────────────────────────────────

    @Test fun `merchant below minSpendThreshold is excluded from spending change insights`() {
        val curr = listOf(summary("TinyMerchant", 50.0))
        val prev = listOf(summary("TinyMerchant", 10.0))
        val result = generate(curr, prev, minSpend = 100.0)
        assertFalse(result.any { it.type == MerchantInsightType.SPENDING_INCREASE })
    }

    // ── fastest growing ───────────────────────────────────────────────────────────

    @Test fun `fastest growing merchant is not duplicated from SPENDING_INCREASE set`() {
        val curr = listOf(
            summary("Amazon",  1000.0),
            summary("Blinkit", 600.0),
            summary("Zepto",   450.0)
        )
        val prev = listOf(
            summary("Amazon",  200.0),
            summary("Blinkit", 200.0),
            summary("Zepto",   200.0)
        )
        val result = generate(curr, prev, changePct = 15.0)
        val increaseDescs = result.filter { it.type == MerchantInsightType.SPENDING_INCREASE }.map { it.description }
        val fastestGrowing = result.firstOrNull { it.type == MerchantInsightType.FASTEST_GROWING }
        if (fastestGrowing != null && increaseDescs.isNotEmpty()) {
            assertFalse(
                increaseDescs.any { d -> d.split(" ").firstOrNull()?.let { fastestGrowing.description.contains(it) } == true }
            )
        }
    }

    @Test fun `FASTEST_GROWING not emitted when all growing merchants already in SPENDING_INCREASE`() {
        val curr = listOf(summary("Amazon", 600.0), summary("Swiggy", 500.0))
        val prev = listOf(summary("Amazon", 200.0), summary("Swiggy", 200.0))
        val result = generate(curr, prev)
        val increaseNames = result.filter { it.type == MerchantInsightType.SPENDING_INCREASE }
            .map { it.description.split(" ").first() }
        result.filter { it.type == MerchantInsightType.FASTEST_GROWING }.forEach { fg ->
            assertFalse(
                "FASTEST_GROWING must not duplicate SPENDING_INCREASE merchant",
                increaseNames.any { fg.description.contains(it) }
            )
        }
    }

    @Test fun `fastest growing not emitted when no previous period data`() {
        val curr = (1..5).map { summary("M$it", it * 200.0) }
        val result = generate(curr, emptyList())
        assertFalse(result.any { it.type == MerchantInsightType.FASTEST_GROWING })
    }

    // ── top merchant ──────────────────────────────────────────────────────────────

    @Test fun `most visited merchant insight is generated`() {
        val summaries = listOf(
            summary("Amazon", 500.0, txCount = 10),
            summary("Swiggy", 300.0, txCount = 3)
        )
        val result = generate(summaries)
        val topInsight = result.first { it.type == MerchantInsightType.TOP_MERCHANT && it.title == "Most visited merchant" }
        assertTrue(topInsight.description.contains("Amazon"))
    }

    @Test fun `most money spent insight generated when different from most visited`() {
        val summaries = listOf(
            summary("Amazon", 5000.0, txCount = 3),
            summary("Swiggy", 300.0,  txCount = 20)
        )
        val result = generate(summaries)
        val titles = result.filter { it.type == MerchantInsightType.TOP_MERCHANT }.map { it.title }
        assertTrue("Most visited merchant" in titles)
        assertTrue("Most money spent at" in titles)
    }

    @Test fun `most money spent not duplicated when same as most visited`() {
        val summaries = listOf(summary("Amazon", 5000.0, txCount = 20))
        val result = generate(summaries)
        assertFalse(result.any { it.type == MerchantInsightType.TOP_MERCHANT && it.title == "Most money spent at" })
    }

    @Test fun `top merchant insight contains transaction count`() {
        val summaries = listOf(summary("Swiggy", 300.0, txCount = 7))
        val result = generate(summaries)
        val insight = result.first { it.type == MerchantInsightType.TOP_MERCHANT }
        assertTrue(insight.description.contains("7"))
    }

    // ── merchant concentration ────────────────────────────────────────────────────

    @Test fun `merchant at exactly 20 percent total spend triggers concentration insight`() {
        val summaries = listOf(
            summary("Amazon", 200.0),
            summary("Others", 800.0)
        )
        val result = generate(summaries)
        assertTrue(result.any { it.type == MerchantInsightType.MERCHANT_CONCENTRATION })
    }

    @Test fun `merchant below 20 percent does not trigger concentration insight`() {
        val summaries = listOf(
            summary("Amazon", 190.0),
            summary("Others", 820.0)
        )
        val result = generate(summaries)
        assertFalse(result.any {
            it.type == MerchantInsightType.MERCHANT_CONCENTRATION && it.description.contains("Amazon")
        })
    }

    @Test fun `concentration insight description contains merchant name and percentage`() {
        val summaries = listOf(
            summary("Amazon", 500.0),
            summary("Others", 500.0)
        )
        val result = generate(summaries)
        val insight = result.first { it.type == MerchantInsightType.MERCHANT_CONCENTRATION }
        assertTrue(insight.description.contains("50%"))
        assertTrue(insight.description.contains("Amazon"))
    }

    @Test fun `at most 3 concentration insights are emitted`() {
        // 10 merchants at equal spend = 10% each → all below 20% → 0 concentration insights
        val summaries = (1..10).map { summary("M$it", 1000.0) }
        val result = generate(summaries)
        assertTrue(result.count { it.type == MerchantInsightType.MERCHANT_CONCENTRATION } <= 3)
    }

    // ── recurring merchant ────────────────────────────────────────────────────────

    @Test fun `merchant with 3 months active triggers recurring insight`() {
        val summaries = listOf(summary("Netflix", monthsActive = 3))
        val result = generate(summaries)
        assertTrue(result.any { it.type == MerchantInsightType.RECURRING_MERCHANT })
    }

    @Test fun `merchant with 2 months active does not trigger recurring insight`() {
        val summaries = listOf(summary("Netflix", monthsActive = 2))
        val result = generate(summaries)
        assertFalse(result.any { it.type == MerchantInsightType.RECURRING_MERCHANT })
    }

    @Test fun `recurring insight contains merchant name and months count`() {
        val summaries = listOf(summary("Netflix", monthsActive = 6))
        val result = generate(summaries)
        val insight = result.first { it.type == MerchantInsightType.RECURRING_MERCHANT }
        assertTrue(insight.description.contains("Netflix"))
        assertTrue(insight.description.contains("6"))
    }

    @Test fun `at most 2 recurring insights are emitted`() {
        val summaries = (1..10).map { summary("M$it", monthsActive = 5) }
        val result = generate(summaries)
        assertTrue(result.count { it.type == MerchantInsightType.RECURRING_MERCHANT } <= 2)
    }

    // ── high average spend ────────────────────────────────────────────────────────

    @Test fun `highest average transaction merchant insight is generated`() {
        val summaries = listOf(
            summary("Apple",  totalSpent = 2000.0, txCount = 2, avg = 1000.0),
            summary("Amazon", totalSpent = 5000.0, txCount = 20, avg = 250.0)
        )
        val result = generate(summaries)
        val insight = result.first { it.type == MerchantInsightType.HIGH_AVERAGE_SPEND }
        assertTrue(insight.description.contains("Apple"))
    }

    @Test fun `single-transaction merchant excluded from high average insight`() {
        val summaries = listOf(
            summary("LuxuryStore", totalSpent = 50000.0, txCount = 1, avg = 50000.0),
            summary("Amazon",      totalSpent =  5000.0, txCount = 10, avg = 500.0)
        )
        val result = generate(summaries)
        val insight = result.firstOrNull { it.type == MerchantInsightType.HIGH_AVERAGE_SPEND }
        if (insight != null) {
            assertTrue(insight.description.contains("Amazon"))
        }
    }

    @Test fun `no high average insight when all merchants have exactly one transaction`() {
        val summaries = listOf(
            summary("M1", totalSpent = 1000.0, txCount = 1),
            summary("M2", totalSpent = 2000.0, txCount = 1)
        )
        val result = generate(summaries)
        assertFalse(result.any { it.type == MerchantInsightType.HIGH_AVERAGE_SPEND })
    }

    // ── category dominance ────────────────────────────────────────────────────────

    @Test fun `category dominance insight generated when merchant has 30 percent of category spend`() {
        val foodCat = cat(1, "Food")
        val summaries = listOf(
            summary("Swiggy", catAmounts = listOf(catAmount(1, "Food", 700.0))),
            summary("Zomato", catAmounts = listOf(catAmount(1, "Food", 300.0)))
        )
        val result = generate(summaries, categories = listOf(foodCat))
        assertTrue(result.any { it.type == MerchantInsightType.CATEGORY_DOMINANCE })
    }

    @Test fun `category dominance description contains merchant name and category name`() {
        val foodCat = cat(1, "Food")
        val summaries = listOf(
            summary("Swiggy", catAmounts = listOf(catAmount(1, "Food", 800.0))),
            summary("Zomato", catAmounts = listOf(catAmount(1, "Food", 200.0)))
        )
        val result = generate(summaries, categories = listOf(foodCat))
        val insight = result.first { it.type == MerchantInsightType.CATEGORY_DOMINANCE }
        assertTrue(insight.description.contains("Swiggy"))
        assertTrue(insight.description.contains("Food"))
    }

    @Test fun `merchant below 30 percent of category does not trigger category dominance`() {
        val foodCat = cat(1, "Food")
        // Swiggy = 29%, Zomato = 71%
        val summaries = listOf(
            summary("Swiggy", catAmounts = listOf(catAmount(1, "Food", 290.0))),
            summary("Zomato", catAmounts = listOf(catAmount(1, "Food", 710.0)))
        )
        val result = generate(summaries, categories = listOf(foodCat))
        assertTrue(result.any { it.type == MerchantInsightType.CATEGORY_DOMINANCE && it.description.contains("Zomato") })
        assertFalse(result.any { it.type == MerchantInsightType.CATEGORY_DOMINANCE && it.description.contains("Swiggy") })
    }

    @Test fun `at most 2 category dominance insights are emitted`() {
        val cats = (1..10).map { cat(it, "Cat$it") }
        val amounts = (1..10).map { catAmount(it, "Cat$it", 1000.0) }
        val summaries = listOf(summary("Amazon", catAmounts = amounts))
        val result = generate(summaries, categories = cats)
        assertTrue(result.count { it.type == MerchantInsightType.CATEGORY_DOMINANCE } <= 2)
    }

    // ── priority ordering ─────────────────────────────────────────────────────────

    @Test fun `all emitted insights are sorted by priority ascending`() {
        val curr = listOf(
            summary("Amazon", 1000.0, txCount = 10, monthsActive = 5,
                catAmounts = listOf(catAmount(1, "Food", 800.0))),
            summary("Swiggy", 200.0, txCount = 2, monthsActive = 1)
        )
        val prev = listOf(
            summary("Amazon", 500.0),
            summary("Swiggy", 100.0)
        )
        val result = generate(curr, prev, categories = listOf(cat(1, "Food")))
        for (i in 1 until result.size) {
            assertTrue(
                "Insight at index $i (priority ${result[i].priority}) must not be lower than index ${i-1} (priority ${result[i-1].priority})",
                result[i].priority >= result[i - 1].priority
            )
        }
    }

    @Test fun `SPENDING_INCREASE priority is lower number than RECURRING_MERCHANT priority`() {
        val curr = listOf(summary("Netflix", 600.0, monthsActive = 5))
        val prev = listOf(summary("Netflix", 300.0))
        val result = generate(curr, prev)
        val increase  = result.firstOrNull { it.type == MerchantInsightType.SPENDING_INCREASE }
        val recurring = result.firstOrNull { it.type == MerchantInsightType.RECURRING_MERCHANT }
        if (increase != null && recurring != null) {
            assertTrue(increase.priority < recurring.priority)
        }
    }

    // ── determinism ───────────────────────────────────────────────────────────────

    @Test fun `same inputs always produce identical output`() {
        val curr = listOf(
            summary("Amazon", 1000.0, txCount = 8, monthsActive = 4),
            summary("Swiggy", 500.0,  txCount = 15, monthsActive = 2)
        )
        val prev = listOf(
            summary("Amazon", 600.0),
            summary("Swiggy", 700.0)
        )
        assertEquals(generate(curr, prev), generate(curr, prev))
    }

    // ── large dataset smoke test ──────────────────────────────────────────────────

    @Test fun `generates insights without error for 500 merchants`() {
        val curr = (1..500).map { summary("Merchant$it", it * 10.0, txCount = it % 10 + 1, monthsActive = it % 12 + 1) }
        val prev = (1..500).map { summary("Merchant$it", it * 8.0) }
        val result = generate(curr, prev)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
