# StarkLedger — Merchant Intelligence Design
## Phase 3 Reference Document (Sprint 3A / 3B / 3C)

> Status: **Approved — Sprint 3A implementation in progress.**
> Approved: 2026-06-14.
> All future development must reference this document rather than chat history.

---

## 1. Strategic Context

StarkLedger's SMS Intelligence Engine extracts raw merchant strings directly from SMS bodies. These raw strings are noisy, inconsistent, and fragmented — the same real-world merchant can appear as dozens of different raw strings across different banks and SMS templates.

**Before Phase 3**, the only normalization is `CategoryNormalizer.normalizeMerchant()` — a 12-brand hardcoded whitelist applied only for display, never persisted, never applied to analytics grouping, and not user-extensible. The `merchant_mappings` table handles merchant→category overrides, but has no mechanism for merchant-name normalization or aliasing.

**Phase 3 goal**: Promote merchant identity from a noisy display string into a first-class, stable canonical concept — fully offline, privacy-first, deterministic, explainable, and extensible — transforming StarkLedger from a transaction tracker into a merchant-aware financial intelligence platform.

---

## 2. Core Design Principles

| Principle | How it is satisfied |
|---|---|
| Privacy-first / fully offline | No data leaves the device; no external lookup; all normalization is rule-based or user-defined |
| Deterministic & reproducible | Pure functions, no hidden state; same input always yields same output |
| Explainable | Every canonical name can be traced back to a rule match or an explicit user alias; `source` field records provenance |
| Read-time resolution (approved) | Raw `merchant` string is never overwritten in `transactions`; canonical name is always derived live |
| Override-first (approved) | Alias lookup runs before rule-based normalization, mirroring `identifyCategory`'s override pattern |
| Zero backward-incompatible changes | DB migration is additive; existing tables/columns unchanged; all existing call sites preserved |

---

## 3. Architecture Overview

```
SMS body
  │
  ▼
SmsParser.parseSms()
  │
  └─► Transaction.merchant  ← raw, stored as-is, never modified
              │
              │  (at read-time, per-request)
              ▼
   ┌──────────────────────────────────┐
   │  Canonical Merchant Resolution   │
   │                                  │
   │  1. merchant_aliases lookup      │  ← user overrides (COLLATE NOCASE)
   │     (alias → canonicalMerchant)  │
   │                                  │
   │  2. MerchantNormalizationEngine  │  ← rule-based fallback
   │     .normalize(raw)              │
   │     → MerchantResolution         │
   │       .canonicalName             │
   │       .confidence  (0–100)       │
   └──────────────────────────────────┘
              │
              ├──► MerchantAnalyticsEngine   (Sprint 3B)
              │      pure; takes aliasMap + filter
              │      → List<MerchantSummary>
              │
              ├──► AccountAnalyticsEngine    (alias-aware, Sprint 3A)
              │      optional aliasMap param; default = emptyMap()
              │
              ├──► InsightsEngine            (Sprint 3C)
              │      merchant insight strings
              │
              └──► MerchantExplorerScreen    (Sprint 3B)
                     search + health metrics
```

---

## 4. Resolution Pipeline (detail)

### Step 1 — Alias Lookup

```
raw = transaction.merchant.trim()
alias_row = SELECT * FROM merchant_aliases
            WHERE alias = raw COLLATE NOCASE
            LIMIT 1

if alias_row found:
    return MerchantResolution(alias_row.canonicalMerchant, confidence = 100)
```

Confidence is 100 for alias hits: the user (or a SYSTEM rule) explicitly declared this mapping.

### Step 2 — Rule-Based Normalization (fallback)

`MerchantNormalizationEngine.normalize(raw): MerchantResolution`

Pipeline (each stage operates on the output of the previous; patterns are pre-compiled at object init, reused across all calls):

| Stage | Operation | Example |
|---|---|---|
| 1. SMS artifact cleanup | Strip trailing `_`, `@domain`, non-alpha-numeric-space chars, squeeze spaces | `MR DIY_` → `MR DIY`, `AMAZON@UPI` → `AMAZON` |
| 2. Entity-suffix stripping | Remove `Pvt`, `Ltd`, `Limited`, `Private`, `LLP`, `India`, `Services?`, `Technologies?` (word-boundary, case-insensitive, iterated until stable → idempotent) | `Dreamplug Service Private Limited` → `Dreamplug` |
| 3. Whitespace & casing | Squeeze spaces, trim, title-case | `HDFC BANK` → `Hdfc Bank` |
| 4. Brand recognition | Match against extensible `List<Pair<Regex, String>>` table; first match wins; returns matched canonical + confidence 100 | `SWIGGY INSTAMART` → `Swiggy` (100); `ABC TECH` → `Abc Tech` (60) |
| 5. Confidence scoring | 100 if brand table matched; 80 if only suffix-stripping transformed the name; 60 if only whitespace/casing changed; 40 if no rules fired (raw pass-through, title-cased) | see below |

**Confidence heuristic**:

```
100  brand table matched                  (known merchant, high certainty)
 80  suffix stripping changed the name    (legal entity suffix removed, good certainty)
 60  only casing/whitespace changed       (cleaned up, moderate certainty)
 40  no transformation fired              (raw pass-through; may need user correction)
```

Confidence does **not** affect current behavior — it is stored on `MerchantResolution` for future use by Merchant Explorer, Merchant Corrections, and Data Quality Insights (Phase 4).

**Idempotency contract**: `normalize(normalize(x).canonicalName).canonicalName == normalize(x).canonicalName` for all inputs. Unit-tested.

### CategoryNormalizer backward compatibility

`CategoryNormalizer.normalizeMerchant(raw: String): String` is updated to a one-line delegate:

```kotlin
fun normalizeMerchant(raw: String): String =
    MerchantNormalizationEngine.normalize(raw).canonicalName
```

Zero churn to `InsightsEngine`, `AccountAnalyticsEngine`, or any other existing call site.

---

## 5. Data Model

### 5.1 MerchantAlias entity (NEW — DB v7)

```kotlin
@Entity(
    tableName = "merchant_aliases",
    indices = [
        Index(value = ["canonicalMerchant"]),          // fast "list aliases of X" queries
        Index(value = ["alias"], unique = true)        // COLLATE NOCASE enforced at app layer
    ]
)
data class MerchantAlias(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // surrogate PK (Change 1)
    val alias: String,                                 // raw/variant merchant string, trimmed
    val canonicalMerchant: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "USER"                        // "USER" | "SYSTEM"
)
```

**Why surrogate PK (Change 1)**: The `alias` column is logically unique (enforced by the unique index + COLLATE NOCASE at app layer) but using a `Long` auto-generated PK preserves schema flexibility — the alias column can be widened, re-indexed, or re-typed in a future migration without touching foreign-key relationships elsewhere.

**Why `source`**: Records whether the alias was created by the user ("USER") or populated by a system rule ("SYSTEM"). Powers the "explainability" requirement and the Corrections screen's filter/sort options.

### 5.2 MerchantResolution value object (NEW — no DB)

```kotlin
data class MerchantResolution(
    val canonicalName: String,
    val confidence: Int  // 0–100; see §4 for heuristic
)
```

Pure value type, domain layer only, no Room annotation.

### 5.3 Existing tables — unchanged

`transactions`, `accounts`, `categories`, `merchant_mappings` — no column additions, no type changes, no data migration.

---

## 6. Alias Invariant & Loop Prevention

**Invariant**: At all times, no value that appears as `canonicalMerchant` in `merchant_aliases` simultaneously appears as an `alias` key for a different `canonicalMerchant`.

This guarantees:
- Single-hop resolution — O(1) lookup, no chain-walking
- No cycles possible
- Corrections screen always shows a correct, non-recursive view

**Maintained by two operations in `MoneyRepository`**:

### `setMerchantAlias(rawAlias, canonical, source)` — add/override a single mapping

```
1. If rawAlias.trim() ==ᵢ canonical.trim() → delete any existing row for rawAlias (reset to engine default); return
2. Collapse: if canonical is itself an alias of Z, use Z as the final canonical
3. Upsert: (alias=rawAlias, canonicalMerchant=finalCanonical, source=source)
```

### `renameMerchant(from, to)` — rename a canonical merchant (Change 5 merge uses same cascade)

```
1. Cascade: UPDATE merchant_aliases SET canonicalMerchant = to WHERE canonicalMerchant = from COLLATE NOCASE
2. Collapse: resolve `to` through any existing alias for `to`
3. Upsert alias row: (alias=from, canonicalMerchant=resolvedTo)
   — so future raw strings that previously resolved to `from` now resolve to `to`
```

### `mergeMerchant(source, target)` — (Sprint 3C, described here for completeness)

```
1. Same as renameMerchant(from=source, to=target) — cascade + collapse
2. Result: every alias/transaction that was grouping under `source` now groups under `target`
3. Reversible: delete the alias row for `source`; it reverts to the engine's normalized form
```

---

## 7. ViewModel / Repository wiring

Domain engines (`MerchantAnalyticsEngine`, `AccountAnalyticsEngine`) receive a pre-built `aliasMap: Map<String, String>` — keyed by `alias.lowercase().trim()` → `canonicalMerchant`. This map is built once per observation cycle by the ViewModel (from `MoneyRepository.allMerchantAliases` Flow), not inside the engine. This keeps engines:

- Pure (no Room access, no suspend functions)
- Testable (pass a plain Map in unit tests)
- O(1) per lookup (lowercased-key map — no iteration)

```kotlin
// ViewModel pattern (Sprint 3B / 3C ViewModels)
val aliasMap: StateFlow<Map<String,String>> = repository.allMerchantAliases
    .map { list -> list.associate { it.alias.lowercase().trim() to it.canonicalMerchant } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```

`resolveCanonicalMerchant(raw)` in `MoneyRepository` is the suspend variant for single-shot lookups (e.g., ingestion time, or when a ViewModel needs to resolve one name without building the full map).

---

## 8. Database Migration

```
DB version: 6 → 7
Type: Additive only
Existing data: untouched
```

```sql
-- Migration 6 → 7
CREATE TABLE IF NOT EXISTS merchant_aliases (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    alias      TEXT    NOT NULL,
    canonicalMerchant TEXT NOT NULL,
    createdAt  INTEGER NOT NULL,
    source     TEXT    NOT NULL DEFAULT 'USER'
);
CREATE INDEX IF NOT EXISTS index_merchant_aliases_canonicalMerchant
    ON merchant_aliases(canonicalMerchant);
CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_aliases_alias
    ON merchant_aliases(alias);
```

The UNIQUE index on `alias` enforces one canonical per raw alias string. The app layer also enforces COLLATE NOCASE uniqueness by normalising to `lowercase().trim()` before insert.

---

## 9. Rollback Plan

| Layer | Rollback action | Data risk |
|---|---|---|
| DB migration | Drop `merchant_aliases` table | None — other tables untouched |
| `CategoryNormalizer` delegate | Restore inline logic (4 lines) | None |
| `AccountAnalyticsEngine` alias param | Remove optional param from call site | None — default is `emptyMap()` |
| New screens / routes | Remove 3 entries from `Navigation.kt` + `MainActivity.kt` | None |
| Repository methods | Remove 4 new suspend funs | None |
| `MerchantNormalizationEngine` | Delete file | None |

No existing table, column, or function signature is removed or altered by Phase 3. The entire feature can be disabled by removing the new table and its DAO/repository methods — existing behaviour is fully restored.

---

## 10. Sprint Breakdown

### Sprint 3A — Core Merchant Foundation
**Goal**: Normalization engine, alias schema, repository APIs, learning system, foundational tests. No UI.

Files created:
- `domain/MerchantNormalizationEngine.kt`
- `data/MerchantAliasDao.kt` (or inline in `AppDatabase.kt`)
- `test/MerchantNormalizationEngineTest.kt`
- `test/MerchantAliasTest.kt`
- `test/MerchantLearningTest.kt`

Files modified:
- `data/AppDatabase.kt` — `MerchantAlias` entity, `MerchantAliasDao`, `MIGRATION_6_7`, version bump, DB builder
- `data/MoneyRepository.kt` — `allMerchantAliases` Flow, `setMerchantAlias`, `renameMerchant`, `deleteMerchantAlias`, `resolveCanonicalMerchant`
- `domain/CategoryNormalizer.kt` — delegate to `MerchantNormalizationEngine`

### Sprint 3B — Merchant Analytics & Explorer
**Goal**: Analytics engine, Explorer screen with search and health metrics.

Files created:
- `domain/MerchantAnalyticsEngine.kt`
- `ui/merchants/MerchantExplorerScreen.kt` + ViewModel
- `test/MerchantAnalyticsEngineTest.kt`
- `test/MerchantExplorerTest.kt`

Files modified:
- `domain/AccountAnalyticsEngine.kt` — optional `merchantAliases` param
- `ui/Navigation.kt` — `Screen.MerchantExplorer`, `Screen.MerchantDetail`
- `MainActivity.kt` — composable registrations
- `ui/analytics/AnalyticsScreen.kt` — "View All Merchants" entry point

### Sprint 3C — Merchant Intelligence
**Goal**: Insights integration, Corrections UI, Merge workflow.

Files created:
- `ui/merchants/MerchantCorrectionsScreen.kt` + ViewModel
- `test/MerchantInsightsTest.kt`

Files modified:
- `domain/InsightsEngine.kt` — `generateMerchantInsights` function
- `ui/analytics/AnalyticsViewModel.kt` — merchant insights wiring
- `ui/settings/SettingsScreen.kt` — Merchant Corrections entry point
- `ui/Navigation.kt` — `Screen.MerchantCorrections`
- `MainActivity.kt` — composable registration

---

## 11. Test Plan

### Sprint 3A tests (mandatory before commit)

**`MerchantNormalizationEngineTest`**
- All spec brand-family examples: Amazon variants → Amazon, Swiggy variants → Swiggy, Dreamplug variants → Dreamplug
- SMS artifact cleanup: `MR DIY_` → `MR DIY`, `AMAZON@UPI` → `Amazon`
- Entity-suffix stripping: each suffix individually and in combination
- Confidence levels: brand-match = 100, suffix-only = 80, casing-only = 60, pass-through = 40
- Idempotency: `normalize(normalize(x).canonicalName) == normalize(x).canonicalName` across full fixture corpus
- Determinism: repeated calls with same input produce byte-identical output
- Boundary inputs: empty string, blank, whitespace-only, single char, numeric-only

**`MerchantAliasTest`**
- Alias hit returns `canonicalMerchant`, confidence = 100
- Case-insensitive: `"amazon pay india"` and `"AMAZON PAY INDIA"` resolve identically
- Miss falls through to `MerchantNormalizationEngine`
- Rename cascade: existing `X → A` rows update to `X → B` when canonical `A` is renamed to `B`
- Collapse on rename: `A → B` where `B` is already `alias of Z` results in `A → Z`, no chain
- Self-rename is a no-op
- Reset: renaming to the engine's own output deletes the override row
- Loop-prevention invariant: no `canonicalMerchant` value exists as an `alias` key after arbitrary rename sequences
- `source` field: USER vs SYSTEM provenance stored and readable

**`MerchantLearningTest`**
- Correction persists (survives re-fetch — simulates process restart)
- Retroactivity: transactions created before the alias was written resolve to the new canonical
- Prospectivity: new transactions with the same raw merchant string inherit the correction
- Reversibility: deleting the alias row instantly reverts to the engine's output for all transactions
- Re-correction: mapping `A → C` after `A → B` replaces, not duplicates
- Explainability: can enumerate all raw aliases feeding into a given canonical

### Sprint 3B tests
- `MerchantAnalyticsEngineTest`: totals, count, avg, largest, first/last date, alias-aware grouping, filter scoping, monthly trend, empty states, 10k-row perf smoke test
- `MerchantExplorerTest`: ranking, top-category resolution, search (case-insensitive, instant filter), health metrics (Last Seen, Months Active, Frequency), empty state

### Sprint 3C tests
- `MerchantInsightsTest`: most-visited, period-over-period % (increase/decrease/zero-previous guard), purchase count, highest-average, determinism, sparse-data graceful degradation

---

## 12. Open Design Decisions (deferred to Sprint 3B/3C)

| Decision | Deferred to | Notes |
|---|---|---|
| Merchant Detail screen navigation (URL-encode merchant name in route vs. pass Int ID via nav arg) | Sprint 3B | Merchant names can contain spaces/special chars; URL encoding (like `AccountDetail`) is preferred |
| Months Active definition (calendar months with ≥1 transaction vs. rolling 30-day windows) | Sprint 3B | Calendar months simpler and more intuitive for the "Last Seen: 3 days ago" pattern |
| Merge UI surface (from Explorer detail vs. from Corrections screen vs. both) | Sprint 3C | Both surfaces are desirable; implement from Corrections screen first as it's lower risk |
| SYSTEM-source alias population (pre-seeded brand table vs. populated at first-resolution time) | Sprint 3C | Pre-seeded is simpler; resolve in Sprint 3C when Corrections screen exists to manage them |
