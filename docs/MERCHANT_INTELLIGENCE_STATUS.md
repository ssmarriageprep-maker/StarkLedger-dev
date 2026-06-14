# StarkLedger — Merchant Intelligence Status

> Last updated: 2026-06-14
> Reference design: [MERCHANT_INTELLIGENCE_DESIGN.md](MERCHANT_INTELLIGENCE_DESIGN.md)

---

## Current Phase: Sprint 3A Complete

---

## Implemented

### Merchant Normalization Foundation
- **`MerchantNormalizationEngine`** — pure, deterministic, idempotent 5-stage pipeline
  - Stage 1: SMS artifact cleanup (`@domain`, trailing `_`, special chars)
  - Stage 2: Brand recognition (ordered `List<Pair<Regex, String>>`)
  - Stage 3: Entity-suffix stripping (iterated until stable)
  - Stage 4: Title-casing
  - Stage 5: Confidence scoring
- **`MerchantResolution`** — value type: `(canonicalName: String, confidence: Int)`
- **Confidence scale**: 100 = alias override · 95 = brand match · 80 = suffix stripped · 60 = formatting only · 40 = pass-through
- **Normalization cache** — `ConcurrentHashMap<String, MerchantResolution>`, cleared on alias writes

### Merchant Alias Persistence
- **`MerchantAlias`** entity — surrogate `Long` PK; `alias` (display text); `aliasKey` (`alias.trim().lowercase()`, UNIQUE index); `canonicalMerchant`; `createdAt`; `source` ("USER" | "SYSTEM")
- **`MerchantAliasDao`** — `getAllAliases` Flow, `getAliasByKey`, insert/update/delete/deleteByAliasKey, `reassignCanonical`
- **DB version**: 6 → 7 (additive migration, no changes to existing tables)

### Merchant Learning Infrastructure
- **`MoneyRepository.setMerchantAlias`** — upsert with collapse (target-is-alias → resolve to final canonical) and self-rename reset
- **`MoneyRepository.renameMerchant`** — cascade + collapse + loop guard
- **`MoneyRepository.deleteMerchantAlias`** — instant revert to engine default
- **`MoneyRepository.resolveMerchant`** — returns `MerchantResolution` (alias at confidence 100, engine fallback)
- **Loop prevention invariant**: no `canonicalMerchant` value ever appears as an `aliasKey` — guaranteed at write time
- **Retroactivity**: any alias write instantly applies to all historical and future transactions (read-time resolution, no backfill)

### Repository & Compatibility
- `allMerchantAliases: Flow<List<MerchantAlias>>` — live stream for ViewModels
- `CategoryNormalizer.normalizeMerchant` → delegates to `MerchantNormalizationEngine` (single source of truth)
- All existing call sites (`InsightsEngine`, `AccountAnalyticsEngine`) unchanged

---

## Test Coverage

| Test file | Cases | Focus |
|---|---|---|
| `MerchantNormalizationEngineTest` | 26 | Brand families, artifacts, suffixes, confidence, idempotency, determinism, boundary inputs |
| `MerchantCacheTest` | 6 | Cache hit/miss, clearCache correctness, independence of cached entries |
| `MerchantAliasTest` | 10 | Resolution priority, case-insensitivity, whitespace trim, miss fallback, source provenance |
| `MerchantInvariantTest` | 12 | Loop prevention, self-reference rejection, cascade rename, chain collapse, invariant property |
| `MerchantLearningTest` | 11 | Retroactivity, prospectivity, reversibility, re-correction, multi-merchant consistency |
| **Total Sprint 3A** | **65** | |
| **Full suite** | **230** | 0 failures |

---

## Pending (Sprint 3B)

- [ ] `MerchantAnalyticsEngine` — per-merchant aggregations: total spent, count, average, largest, first/last seen, months active
- [ ] `MerchantExplorerScreen` — ranked merchant list with search (case-insensitive, instant)
- [ ] Merchant health metrics — Last Seen, Months Active, Transaction Frequency
- [ ] `MerchantAnalyticsEngineTest`, `MerchantExplorerTest`
- [ ] Performance validation: 10,000+ transactions, 500+ merchants

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
         ▼  resolveMerchant(raw)
   merchant_aliases lookup  →  confidence 100
         │ miss
         ▼
   MerchantNormalizationEngine.normalize(raw)
   → MerchantResolution(canonicalName, 40–95)
         │
         ▼
   ViewModels / Analytics Engines
   (receive pre-built aliasMap: Map<String,String>)
```

Resolution is always O(1): alias table lookup + cached engine result.
No transaction data is ever modified by the merchant intelligence system.
