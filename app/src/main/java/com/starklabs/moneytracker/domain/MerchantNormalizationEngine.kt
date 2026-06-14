package com.starklabs.moneytracker.domain

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Resolved canonical merchant name and a confidence score.
 *
 * confidence scale:
 *   100 = explicit user/system alias (set by repository before this engine is consulted)
 *    95 = brand table match (known merchant recognised by pattern)
 *    80 = suffix cleanup changed the name (legal / generic entity words stripped)
 *    60 = formatting cleanup only (casing / whitespace / SMS artifact removed)
 *    40 = raw pass-through (no transformation fired; may need a user alias)
 */
data class MerchantResolution(
    val canonicalName: String,
    val confidence: Int
)

/**
 * Rule-based merchant normalisation engine. Pure and deterministic — no I/O, no state beyond
 * an in-memory resolution cache. Alias overrides are applied by [MoneyRepository] *before*
 * this engine is consulted, so callers never need to layer alias logic here.
 *
 * Idempotency contract:
 *   normalize(normalize(x).canonicalName).canonicalName == normalize(x).canonicalName  ∀ x
 *
 * The cache is memory-only and is cleared whenever the alias table changes (via [clearCache]),
 * providing a safety net in case the two systems are later unified.
 */
object MerchantNormalizationEngine {

    // --- pre-compiled patterns (init once, reused for every call) ---

    private val ARTIFACT_AT = Pattern.compile("@\\S+")
    private val SPECIAL_CHARS = Pattern.compile("[^a-zA-Z0-9 ]")
    private val MULTI_SPACE = Pattern.compile(" {2,}")

    // Corporate / geographic suffixes stripped iteratively until stable.
    // "Technologies" is intentionally excluded — it is kept as a meaningful brand word
    // (e.g. "ABC Technologies") to avoid over-stripping unknown company names.
    private val SUFFIX = Pattern.compile(
        "(?i)\\b(pvt\\.?|ltd\\.?|limited|private|llp|inc\\.?|corp\\.?|india|service|services)\\b"
    )

    // Ordered brand table — first match wins.
    // Only add entries whose canonical name is unambiguous and widely recognised in Indian SMS.
    private val BRANDS: List<Pair<Regex, String>> = listOf(
        Regex("(?i)\\bamazon\\b")             to "Amazon",
        Regex("(?i)\\bswiggy\\b")             to "Swiggy",
        Regex("(?i)\\bzomato\\b")             to "Zomato",
        Regex("(?i)\\bdreamplug\\b")          to "Dreamplug",
        Regex("(?i)\\buber\\b")               to "Uber",
        Regex("(?i)\\bola\\b")                to "Ola",
        Regex("(?i)\\bflipkart\\b")           to "Flipkart",
        Regex("(?i)\\bnetflix\\b")            to "Netflix",
        Regex("(?i)\\bspotify\\b")            to "Spotify",
        Regex("(?i)\\bjio\\w*")               to "Jio",
        Regex("(?i)\\bairtel\\b")             to "Airtel",
        Regex("(?i)\\bcred\\b")               to "CRED",
        Regex("(?i)\\bmr\\.?\\s*diy\\b")      to "MR DIY",
        Regex("(?i)\\bpaytm\\b")              to "Paytm",
        Regex("(?i)\\bphonepe\\b")            to "PhonePe",
        Regex("(?i)\\bblinkit\\b|\\bgrofers\\b") to "Blinkit",
        Regex("(?i)\\bbigbasket\\b")          to "BigBasket",
        Regex("(?i)\\bmyntra\\b")             to "Myntra",
        Regex("(?i)\\bnykaa\\b")              to "Nykaa",
        Regex("(?i)\\bmeesho\\b")             to "Meesho",
        Regex("(?i)\\bzepto\\b")              to "Zepto",
        Regex("(?i)\\bdunzo\\b")              to "Dunzo",
        Regex("(?i)\\brapido\\b")             to "Rapido",
        Regex("(?i)\\btata\\s*neu\\b")        to "Tata Neu",
    )

    // Memory-only cache — transparent to callers, cleared on alias updates.
    private val cache = ConcurrentHashMap<String, MerchantResolution>()

    /**
     * Returns the canonical name and confidence for [raw].
     * Results are cached; call [clearCache] when the alias table changes.
     */
    fun normalize(raw: String): MerchantResolution =
        cache.computeIfAbsent(raw) { compute(it) }

    /** Clears the in-memory resolution cache. Call after every alias write. */
    fun clearCache() = cache.clear()

    // -----------------------------------------------------------------
    // Private pipeline
    // -----------------------------------------------------------------

    private fun compute(raw: String): MerchantResolution {
        // Truly blank input — return empty string to preserve backward compatibility
        val rawTrimmed = raw.trim()
        if (rawTrimmed.isEmpty()) return MerchantResolution("", 40)

        // Stage 1: SMS artifact cleanup
        var clean = ARTIFACT_AT.matcher(rawTrimmed).replaceAll(" ")
        clean = SPECIAL_CHARS.matcher(clean).replaceAll(" ")
        clean = MULTI_SPACE.matcher(clean).replaceAll(" ").trim()
        val afterArtifact = clean

        // Stage 2: Brand recognition on the artifact-cleaned string (short-circuits suffix pass)
        val brand = BRANDS.firstOrNull { (regex, _) -> regex.containsMatchIn(clean) }
        if (brand != null) return MerchantResolution(brand.second, 95)

        // Stage 3: Entity-suffix stripping, iterated until stable (guarantees idempotency)
        var prev = ""
        while (prev != clean) {
            prev = clean
            clean = SUFFIX.matcher(clean).replaceAll(" ")
            clean = MULTI_SPACE.matcher(clean).replaceAll(" ").trim()
        }
        val suffixStripped = clean != afterArtifact

        // Guard: if suffix stripping consumed everything, fall back to artifact-cleaned form
        if (clean.isBlank()) return MerchantResolution(titleCase(afterArtifact).ifBlank { "Unknown" }, 40)

        // Stage 4: Title-casing
        val titled = titleCase(clean)

        // Stage 5: Confidence — compare titled output to original trimmed string (not lowercased)
        // so any visible formatting change (casing, artifact removal, whitespace) scores 60.
        val confidence = when {
            suffixStripped -> 80
            titled != rawTrimmed -> 60
            else -> 40
        }

        return MerchantResolution(titled, confidence)
    }

    private fun titleCase(s: String): String =
        s.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercaseChar) }
}
