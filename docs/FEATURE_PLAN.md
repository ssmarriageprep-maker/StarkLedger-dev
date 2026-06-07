# StarkLedger — Feature Comparison & Implementation Plan (DRAFT v2)

> Status: **Draft for review (revised after strategy feedback).**
> Reference app for comparison: **mMoney: Expense & SMS Tracker**
> (Play Store `dev.sanjaygangwar.splitwisely`, by Sanjay Gangwar).
> Documentation only — **not committed** so it doesn't trigger a CI build.

---

## 0. Strategic direction (decided)

> **Be the best privacy-first *automatic expense tracker* before becoming a finance super-app.**

Reinforce StarkLedger's core strengths — privacy-first, on-device SMS intelligence, automatic
tracking, cloud-free insights — before expanding into unrelated finance utilities. Therefore the
reference app's headline extras (**Bill Splitter**, **HRA Rent-Receipt Generator**) are
**deprioritized** to the last phases; the near-term roadmap focuses on data quality, intelligence,
and day-to-day usability of the tracking experience.

---

## 1. What the two apps are

**StarkLedger** (`com.starklabs.moneytracker`) — privacy-first, 100% on-device personal finance
tracker. Reads bank/UPI SMS, classifies + extracts transactions deterministically, shows
balances, budgets, and insights. No cloud, no login.

**mMoney** (reference) — a broader money super-app on the same SMS core, also bundling bill
splitting and HRA rent receipts, plus optional cloud sync.

---

## 2. StarkLedger — current features & use cases

| Area | Current capability | Use case |
|---|---|---|
| SMS engine | Two-phase parse (classify → extract), confidence scoring, 11+ banks, UPI/card/ATM/wallet/EMI/autopay | "Log spends automatically" |
| Ingestion | First-launch bulk scan (≤1000), real-time receiver, manual re-scan | Past + future coverage |
| Dedup | SHA-256 of normalized SMS + unique index | Re-scan never duplicates |
| Accounts | BANK/CREDIT_CARD/CASH/UPI; auto-created by last-4; per-account filter; SMS balance | "See each account" |
| Categories | Seeded defaults; 3-tier match (override → name → keywords); tap-to-recategorize | "Know where money goes" |
| Dashboard | Liquidity, monthly income/expense, budget ring | "How am I doing this month?" |
| Analytics | Category breakdown, InsightsEngine, weekly pulse | "Where am I overspending?" |
| History | All transactions, swipe-to-delete | Audit / correct |
| Manual entry | Add-transaction form | Cash spends |
| Security | PIN + biometric helper | Privacy |
| Export | CSV via share sheet | Data portability |

### Verified state of two "missing" items (important nuance)
- **Merchant normalization** — `CategoryNormalizer.normalizeMerchant()` exists, but only used in
  *one* insights string with a hardcoded 12-brand list. Not applied at ingestion, in
  `identifyCategory`, or in aggregation. → fragmentation is real.
- **Correction learning** — *Partially works.* `updateTransactionCategory` already persists a
  `merchant→category` override (from Dashboard + History) and `identifyCategory` checks it first,
  so future transactions inherit corrections. Gaps: key is the **raw** merchant (exact match), and
  no **retroactive** apply to existing rows.

### Known small fixes (bundle into early phases)
- Settings **Biometric toggle is cosmetic** (not wired); "Upgrade Protection" link is a no-op.
- `WalletsScreen` account-type **icon map** uses stale labels (`CARD`/`WALLET`) vs created
  `CREDIT_CARD`/`UPI` → default icon shown.

---

## 3. mMoney reference features → StarkLedger status

| mMoney feature | StarkLedger |
|---|---|
| SMS auto-tracking | ✅ already (arguably stronger) |
| Budget/expense, monthly **+ yearly** charts, filter by category/date/amount | ⚠️ monthly only, limited filters |
| Bill Splitter (groups, who-owes-whom, settle-up) | ❌ — **deferred** (Phase 6) |
| HRA Rent-Receipt PDF (PAN/address, batch months) | ❌ — **deferred** (Phase 5) |
| Offline-first + cloud sync | ✅ offline; ❌ cloud (out of scope, see §6) |
| On-device privacy | ✅ already |

---

## 4. Revised priority backlog

| ID | Item | Value | Effort | Phase |
|---|---|---|---|---|
| P4 | **Category management UI** (create/edit/delete, keywords, budgets) | High | Low | 1 |
| F0 | Polish: wire biometric toggle, fix account icon map | Low | Low | 1 |
| P3 | **Advanced filtering** (date range, amount, category, account, type) | High | Low-Med | 2 |
| G3 | **Yearly view** + account analytics | Med | Low-Med | 2 |
| P1 | **Merchant normalization engine** (alias system, applied everywhere) | Very High | Medium | 3 |
| P2 | **Correction learning** (normalize key + retroactive apply) | Very High | Medium | 3 |
| P5 | **Insights engine upgrade** (period-over-period, top merchant, etc.) | Very High | Medium | 4 |
| HS | **Financial Health Score** (0–100) | High | Medium | 4 |
| LR | **Launch readiness** (privacy policy, onboarding, empty states, crash reporting, a11y) | High | Med | 4–5 |
| G2 | HRA Rent-Receipt PDF | Med | Med | 5 |
| G1 | Bill Splitter | High (only if demanded) | Large | 6 |

---

## 5. Implementation plans

Principles (CLAUDE.md): preserve the SMS engine, keep it simple, add tests, dark-first UI
(#0D0D0D bg / #161616 card / #00E6FF primary, 16dp card / 12dp button radius). **One batched
commit + one build per feature** to respect CI limits.

### Phase 1 — P4 + F0 (quick wins, low risk)
**P4 Category management** — new `ui/categories/CategoriesScreen.kt`: list/edit name, budget,
keywords, color; add; delete. Repo already has `addCategory`/`updateCategory`; add
`deleteCategory`. Entry from Settings. *Tests:* keyword CSV round-trip, budget validation.
**F0 Polish** — persist a `biometricEnabled` flag in `SecurityRepository`, gate the lock screen,
bind the toggle. Fix `WalletsScreen` `when(type)` icon branches to `CREDIT_CARD`/`UPI`.
**No schema change.** Effort: Low.

### Phase 2 — P3 + G3 (usability)
**P3 Advanced filtering** — a reusable filter state (date range, min/max amount, category,
account, type) applied in History and Analytics. DAO already has `getTransactionsInRange`; add
parameterized queries or filter in-memory for simplicity first. *Tests:* filter predicates.
**G3 Yearly view** — add yearly aggregation (group by month-of-year) + Month/Year toggle in
`AnalyticsViewModel`; per-account analytics. **No schema change.** Effort: Low-Med.

### Phase 3 — P1 + P2 (merchant intelligence; do together)
**P1 Merchant normalization** — promote normalization from display-only to a first-class engine:
- Replace the 12-item hardcode with: entity-suffix stripping + casing/whitespace cleanup + a
  **user-extensible alias table** (`merchant_aliases(rawPattern → canonicalName)`, Room, +1 DB
  version + migration).
- Apply the canonical name **at ingestion** (store normalized merchant on the transaction) and in
  `identifyCategory`, so analytics/budgets/insights all group consistently.
- Keep raw SMS body for traceability (already stored).
**P2 Correction learning (complete + harden)** — store overrides keyed on the **normalized**
merchant; when a user recategorizes, also offer "apply to all past <merchant>" (retroactive
update). Optionally let users edit a merchant's canonical name → writes an alias (feeds P1).
*Tests:* alias resolution, override precedence, retroactive update count, idempotency.
Effort: Medium. **Highest data-quality leverage.**

### Phase 4 — P5 + HS + (LR start) (intelligence & differentiation)
**P5 Insights upgrade** — extend `InsightsEngine` with deterministic, on-device observations:
period-over-period ("22% more than last month"), category deltas ("Food +₹3,200"), weekend
share, top merchant (now reliable post-P1), unusual-spike detection. *Tests:* each insight on
fixed fixtures.
**HS Financial Health Score** — 0–100 from savings rate, budget adherence, expense stability,
spending concentration; show on dashboard with a short explainer. *Tests:* score bounds +
component weighting on fixtures.
**LR Launch readiness** — privacy policy screen, SMS-permission rationale, onboarding flow, empty
states, error handling, **local crash diagnostics by default; external crash reporting only
through explicit user opt-in**, accessibility pass.

### Phase 5 — G2 HRA Rent-Receipt PDF
`LandlordProfile` + `RentReceiptConfig` (DataStore). Generate via Android `PrintedPdfDocument`/
`Canvas` (no dep) → one page per month → share via existing FileProvider. UI under Settings →
Tools. *Tests:* month-range expansion (Apr→Mar = 12), amount formatting. Effort: Medium.

### Phase 6 — G1 Bill Splitter (only if users ask)
New Room entities (`Group`, `GroupMember`, `SharedExpense`, `ExpenseShare`) + migration;
`domain/SplitEngine.kt` (equal/exact/% split, greedy settle-up); `ui/split/` screens; optional
link to a parsed transaction. *Tests:* split math, settle-up minimization, rounding. Effort:
Large. Build isolated from the SMS engine.

---

## 5b. Future backlog

- **Merchant Explorer** — a per-merchant drill-down: total spent, transaction count, average
  spend, last-seen date, trend. High value and low extra infrastructure **once P1 merchant
  normalization lands** (it relies on a stable canonical merchant). Slot after Phase 3.

---

## 6. Out of scope (for now)

- **Cloud sync** — conflicts with the privacy promise ("understand your finances without sharing
  your data"). If ever needed: **user-controlled encrypted backup** to the user's own storage
  (e.g., their Drive), never a StarkLabs server. Separate explicit decision.

---

## 7. Final sequence (each phase = one batched commit + one build)

1. **Phase 1** — P4 category management + F0 polish
2. **Phase 2** — P3 filters + G3 yearly/account analytics
3. **Phase 3** — P1 merchant normalization + P2 correction learning (data quality)
4. **Phase 4** — P5 insights + HS health score + launch-readiness work
5. **Phase 5** — G2 HRA rent receipts
6. **Phase 6** — G1 Bill Splitter (validate demand first)

> Next action awaiting confirmation: start **Phase 1 (P4 + F0)** as a single batched commit.
