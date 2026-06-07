package com.starklabs.moneytracker.domain

/**
 * Pure, framework-free validation/normalization for the category management form.
 *
 * Kept out of the Composable so it can be unit-tested on the JVM and reused by any
 * future entry point (import, quick-add, etc.).
 */
object CategoryForm {

    const val DEFAULT_COLOR = "#FFD700"

    data class Validated(
        val name: String,
        val budget: Double,
        val keywords: String?, // normalized comma-joined, or null when empty
        val colorHex: String
    )

    sealed class Result {
        data class Ok(val value: Validated) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Normalize a comma-separated keyword string: trim, lowercase, drop blanks and
     * duplicates while preserving order. Returns null when nothing usable remains.
     */
    fun parseKeywords(input: String): String? {
        val items = input.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (items.isEmpty()) null else items.joinToString(",")
    }

    /** Validates raw form fields. Budget is optional (defaults to 0) but must be a non-negative number when present. */
    fun validate(name: String, budgetInput: String, keywordsInput: String, colorHex: String): Result {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return Result.Error("Name is required")

        val budget = if (budgetInput.isBlank()) {
            0.0
        } else {
            budgetInput.trim().toDoubleOrNull() ?: return Result.Error("Budget must be a number")
        }
        if (budget < 0) return Result.Error("Budget cannot be negative")

        val color = colorHex.trim().ifEmpty { DEFAULT_COLOR }

        return Result.Ok(Validated(trimmedName, budget, parseKeywords(keywordsInput), color))
    }
}
