# StarkLedger — Merchant Intelligence Status

> Last updated: 2026-06-15
> Reference design: [MERCHANT_INTELLIGENCE_DESIGN.md](MERCHANT_INTELLIGENCE_DESIGN.md)

---

## Current Phase: Sprint 3B Complete

---

## Implemented

### Merchant Normalization Foundation (Sprint 3A)
- **`MerchantNormalizationEngine`** — pure, deterministic, idempotent 5-stage pipeline
  - Stage 1: SMS artifact cleanup (`@domain`, trailing `_`, special chars)
  - Stage 2: Brand recognition (ordered `List<Pair<Regex, String>>`)
  - Stage 3: Entity-suffix stripping (iterated until stable)
  - Stage 4: Title-casing
  - Stage 5: Confidence scoring
- **`MerchantResolution`** — value type: `(canonicalName: String, confidence: Int)`
- **Confidence scale**: 100 = alias override · 95 = brand match · 80 = suffix stripped · 60 = formatting only · 40 = pass-through
- **Normalization cache** — `ConcurrentHashMap<String, MerchantResolution>`, cleared on alias writes

### Merchant Alias Persistence (Sprint 3A)
- **`MerchantAlias`** entity — surrogate `Long` PK; `alias` (display text); `aliasKey` (`alias.trim().lowercase()`, UNIQUE index); `canonicalMerchant`; `createdAt`; `source` ("USER" | "SYSTEM")
- **`MerchantAliasDao`** — `getAllAliases` Flow, `getAliasByKey`, insert/update/delete/deleteByAliasKey, `reassignCanonical`
- **DB version**: 6 → 7 (additive migration, no changes to existing tables)

### Merchant Learning Infrastructure (Sprint 3A)
- **`MoneyRepository.setMerchantAlias`** — upsert with collapse (target-is-alias → resolve to final canonical) and self-rename reset
- **`MoneyRepository.renameMerchant`** — cascade + collapse + loop guard
- **`MoneyRepository.deleteMerchantAlias`** — instant revert to engine default
- **`MoneyRepository.resolveMerchant`** — returns `MerchantResolution` (alias at confidence 100, engine fallback)
- **Loop prevention invariant**: no `canonicalMerchant` value ever appears as an `aliasKey` — guaranteed at write time
- **Retroactivity**: any alias write instantly applies to all historical and future transactions (read-time resolution, no backfill)
- `allMerchantAliases: Flow<List<MerchantAlias>>` — live stream for ViewModels
- `CategoryNormalizer.normalizeMerchant` → delegates to `MerchantNormalizationEngine` (single source of truth)

### Merchant Analytics Engine (Sprint 3B)
- **`MerchantAnalyticsEngine`** — pure, deterministic object; no Room, no Compose
  - `computeAll(transactions, categories, resolve)` — O(unique merchants) resolution pass, O(n) grouping
  - `computeFor(canonicalName, ...)` — single-merchant detail for the detail screen
  - `sort(summaries, order)` — non-mutating; all 4 modes
  - `search(summaries, query)` — case-insensitive, trims query, blank = passthrough
- **`TrendDirection`** enum: `INCREASING · DECREASING · STABLE · INSUFFICIENT_DATA`
- **`MerchantSortOrder`** enum: `HIGHEST_SPEND · MOST_TRANSACTIONS · RECENTLY_ACTIVE · ALPHABETICAL`
- **`MerchantSummary`** data class — 12 fields:
  - `totalSpent`, `transactionCount`, `averageTransaction`, `largestTransaction`
  - `firstSeen`, `lastSeen`, `monthsActive`, `frequencyPerMonth`
  - `trendDirection`, `topCategories: List<CategoryAmount>`, `recentTransactions`

### Merchant Explorer UI (Sprint 3B)
- **`MerchantExplorerViewModel`** — `searchQuery`, `sortOrder` MutableStateFlows; `filteredMerchants` StateFlow derived via `combine`
- **`MerchantExplorerScreen`** — LazyColumn with search bar (instant, case-insensitive), sort chips, empty states, merchant count label, navigation to detail
- **`MerchantDetailScreen`** — 4-card detail layout: Spending Overview · Activity · Trend · Top Categories; recent transactions list using `TransactionRow`
- **Navigation**: `Screen.MerchantExplorer`, `Screen.MerchantDetail` with URL-encoded name parameter
- **Analytics integration**: `AnalyticsViewModel.topMerchants` StateFlow (top 5 by spend, alias-resolved); Top Merchants section in `AnalyticsScreen` with "View All →" link

---

## Test Coverage

| Test file | Cases | Focus |
|---|---|---|
| `MerchantNormalizationEngineTest` | 26 | Brand families, artifacts, suffixes, confidence, idempotency, determinism, boundary inputs |
| `MerchantCacheTest` | 6 | Cache hit/miss, clearCache correctness, independence of cached entries |
| `MerchantAliasTest` | 10 | Resolution priority, case-insensitivity, whitespace trim, miss fallback, source provenance |
| `MerchantInvariantTest` | 12 | Loop prevention, self-reference rejection, cascade rename, chain collapse, invariant property |
| `MerchantLearningTest` | 11 | Retroactivity, prospectivity, reversibility, re-correction, multi-merchant consistency |
| **Sprint 3A subtotal** | **65** | |
| `MerchantAnalyticsEngineTest` | 24 | Alias-aware grouping, credit filtering, aggregation, monthsActive, frequencyPerMonth, TrendDirection, topCategories, computeFor, recentTransactions order |
| `MerchantSortingTest` | 8 | All 4 sort modes, immutability, empty/single-element edge cases |
| `MerchantSearchTest` | 12 | Passthrough for blank, case-insensitive, partial match, no-match, multi-match, whitespace trim |
| **Sprint 3B subtotal** | **44** | |
| **Full suite** | **273** | **0 failures** |

---

## Pending (Sprint 3C)

- [ ] `InsightsEngine.generateMerchantInsights` — period-over-period, top merchant, purchase counts
- [ ] Merchant Corrections UI — view/edit/delete alias rows
- [ ] Merchant Merge workflow — merge two canonicals; cascade + collapse; reversible
- [ ] `MerchantInsightsTest`

---

## Architecture Quick Reference

```
Transaction.merchant  (raw, never modified)
         │
         ▼  read-time resolution
   merchant_aliases lookup  →  confidence 100
         │ miss
         ▼
   MerchantNormalizationEngine.normalize(raw)
   → MerchantResolution(canonicalName, 40–95)
         │
         ▼
   MerchantAnalyticsEngine.computeAll(txns, cats, resolve)
   → List<MerchantSummary>
         │
         ├── MerchantExplorerViewModel.filteredMerchants (search + sort)
         │         └── MerchantExplorerScreen
         │                   └── MerchantDetailScreen (computeFor)
         │
         └── AnalyticsViewModel.topMerchants (top 5)
                   └── AnalyticsScreen "Top Merchants" section
```

Resolution is always O(1): alias table lookup + cached engine result.
No transaction data is ever modified by the merchant intelligence system.
The `resolve: (String) -> String` lambda is built once per ViewModel emission (O(unique merchants)) then applied in O(n) groupBy — no repeated alias DB hits per transaction.
