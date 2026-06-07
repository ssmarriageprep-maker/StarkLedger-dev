package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.CategoryForm
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the category management form logic (Phase 1, P4).
 */
class CategoryFormTest {

    // ── Keyword normalization ──────────────────────────────────────────────

    @Test
    fun `keywords are trimmed, lowercased, de-duplicated, blanks dropped`() {
        assertEquals("swiggy,zomato", CategoryForm.parseKeywords("  Swiggy , ZOMATO ,, swiggy "))
    }

    @Test
    fun `empty keyword input becomes null`() {
        assertNull(CategoryForm.parseKeywords("   ,  , "))
        assertNull(CategoryForm.parseKeywords(""))
    }

    @Test
    fun `keyword order is preserved`() {
        assertEquals("uber,ola,train", CategoryForm.parseKeywords("uber, ola, train"))
    }

    // ── Validation ─────────────────────────────────────────────────────────

    @Test
    fun `blank name is rejected`() {
        val r = CategoryForm.validate("   ", "100", "food", "#fff")
        assertTrue(r is CategoryForm.Result.Error)
    }

    @Test
    fun `non-numeric budget is rejected`() {
        val r = CategoryForm.validate("Food", "abc", "", "#fff")
        assertTrue(r is CategoryForm.Result.Error)
    }

    @Test
    fun `negative budget is rejected`() {
        val r = CategoryForm.validate("Food", "-5", "", "#fff")
        assertTrue(r is CategoryForm.Result.Error)
    }

    @Test
    fun `blank budget defaults to zero`() {
        val r = CategoryForm.validate("Food", "  ", "", "")
        assertTrue(r is CategoryForm.Result.Ok)
        val v = (r as CategoryForm.Result.Ok).value
        assertEquals(0.0, v.budget, 0.001)
    }

    @Test
    fun `blank color falls back to default`() {
        val r = CategoryForm.validate("Food", "100", "", "") as CategoryForm.Result.Ok
        assertEquals(CategoryForm.DEFAULT_COLOR, r.value.colorHex)
    }

    @Test
    fun `valid input is normalized correctly`() {
        val r = CategoryForm.validate("  Food ", "5000", "Swiggy, zomato", "#00E6FF") as CategoryForm.Result.Ok
        assertEquals("Food", r.value.name)
        assertEquals(5000.0, r.value.budget, 0.001)
        assertEquals("swiggy,zomato", r.value.keywords)
        assertEquals("#00E6FF", r.value.colorHex)
    }
}
