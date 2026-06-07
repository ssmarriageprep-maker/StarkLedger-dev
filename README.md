# StarkLedger

A premium, high-performance personal finance and expense tracking application for Android. StarkLedger automatically parses Indian bank SMS alerts to deliver real-time spending intelligence through a futuristic, dark OLED design language.

> Built by **StarkLabs** • Kotlin 2.0 • Jetpack Compose • Room • MVVM

---

## ✨ Core Features

### 🔍 Automated SMS Intelligence Engine
- **Two-phase parsing architecture** — classify first (confidence scoring 0–100), then extract.
- Deterministic, regex-based parsing of transaction SMS from **11+ Indian banks** (HDFC, SBI, ICICI, AXIS, KOTAK, PNB, IDFC, YES, CANARA, FEDERAL, UNION).
- **Smart rejection**: promotional keywords trigger penalty scoring, not hard rejection — real transactions containing "bill" or "statement" alongside strong bank signals still pass.
- **Multi-pass amount extraction**: strict → relaxed → context-based regex, with balance-range exclusion to avoid confusing "Avl Bal" with the transaction amount.
- Pattern detection classifies transactions into formal types: `UPI_SENT`, `BANK_DEBIT`, `CARD_SPEND`, `ATM_WITHDRAWAL`, `AUTOPAY_DEBIT`, `ACH_DEBIT`, `EMI_DEBIT`, `WALLET_PAYMENT`, etc.
- SHA-256 message hashing for duplicate detection.
- Specialized patterns for HDFC card spend, Union Bank, Pluxee wallet, and HDFC credit alerts.

### 📊 Bento-Grid Dashboard
- Responsive HUD displaying **current liquidity**, monthly income, monthly expenses, and budget health at a glance.
- Circular progress indicator for budget utilization with dynamic "safe to spend per day" AI insight.
- Animated entry transitions with staggered fade-in and slide effects.
- Auto-scaling typography for amounts exceeding ₹10L to prevent truncation.

### 📈 Analytics & Insights
- Category-level spending breakdown with custom charts.
- **InsightsEngine** generates smart observations: highest expense category, weekend overspending detection, and large single-transaction alerts.
- Weekly Pulse with period-over-period comparison and color-coded feedback (green = savings, amber/red = spending spikes).

### 💳 Multi-Account Wallet Management
- Support for **CASH**, **BANK**, **CREDIT_CARD**, and **UPI** account types.
- Auto-creation of bank accounts during SMS scanning by matching last-4 masked digits.
- Per-account transaction filtering and balance tracking.
- SMS balance updates used as source-of-truth when available.

### 🏷️ Smart Categorization
- **Three-tier matching**: user-defined merchant overrides → category name matching → keyword-based matching with word boundaries.
- Default categories seeded on first launch: Food, Rent, Travel, Shopping, Bills — each with budget limits and keyword sets.
- Manual re-categorization via tap-on-transaction with learning — merchant-to-category mappings are persisted for future auto-classification.
- `CategoryNormalizer` for merchant name cleanup and heuristic category inference in the insights engine.

### 🔒 Security
- **PIN-based lock screen** as the app entry point.
- **Biometric authentication** support via AndroidX Biometric library.
- DataStore-backed security preferences with encrypted PIN storage.

### 📤 Data Export
- CSV export of all transactions with date, merchant, amount, type, account, category, and notes.
- Share via Android intent using FileProvider.

### ➕ Manual Transaction Entry
- Full-featured form for adding transactions manually with account and category selection.

---

## 🏗️ Architecture

```
com.starklabs.moneytracker/
├── MainActivity.kt              # Entry point, navigation host, SMS permission flow
├── data/
│   ├── AppDatabase.kt           # Room DB (v4): Transaction, Account, Category, MerchantMapping entities
│   ├── MoneyRepository.kt       # Single repository for all data operations
│   ├── AppSettingsRepository.kt  # DataStore: first-launch flag, dashboard log count
│   └── SecurityRepository.kt    # DataStore: PIN storage
├── domain/
│   ├── SmsParser.kt             # 645-line SMS intelligence engine (classify + extract)
│   ├── CategoryNormalizer.kt    # Merchant normalization and heuristic categorization
│   └── InsightsEngine.kt        # AI-style spending pattern analysis
├── sms/
│   ├── SmsReceiver.kt           # BroadcastReceiver for real-time incoming SMS
│   └── SmsScanner.kt            # Bulk historical SMS scan (1000 messages, first-launch)
├── examples/
│   └── SmsParserExamples.kt     # Runnable demo of the parsing engine
└── ui/
    ├── Navigation.kt            # Sealed Screen routes
    ├── theme/                   # Color, Typography, Dimensions (dark OLED theme)
    ├── components/              # StarkCard, TransactionRow, BottomNav, Charts, Logo
    ├── home/                    # Dashboard screen + ViewModel
    ├── analytics/               # Analytics screen + ViewModel
    ├── history/                 # Transaction history screen + ViewModel
    ├── wallets/                 # Multi-account management + AddAccount
    ├── add/                     # Manual transaction entry
    ├── settings/                # Settings screen + AppSettingsViewModel
    └── security/                # PIN/Biometric lock screen + ViewModel
```

**Pattern**: MVVM with manual DI (no Hilt/Dagger). ViewModels use `ViewModelProvider.Factory` pattern. Repository layer wraps Room DAOs with business logic.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.0 |
| UI | Jetpack Compose (latest BOM) + Material 3 |
| Database | Room (v4, destructive migration for dev) |
| Preferences | DataStore Preferences |
| Navigation | Compose Navigation |
| Security | AndroidX Biometric |
| Build | Gradle KTS, KSP for Room annotation processing |
| CI/CD | GitHub Actions (lint, test, assembleDebug, assembleRelease, GitHub Releases) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 17
- Android SDK 35 (compile) / SDK 34 (target)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/StarkLabs-in/StarkLedger.git
cd StarkLedger

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lint
```

### Permissions Required
| Permission | Purpose |
|---|---|
| `READ_SMS` | Historical SMS scanning for transaction extraction |
| `RECEIVE_SMS` | Real-time transaction detection from incoming SMS |
| `USE_BIOMETRIC` | Optional biometric lock screen |

---

## 🎨 Design System

StarkLedger uses a custom **Midnight OLED** design system:

- **Background**: Pure black (`#0E0E0E` / `#131313`) for OLED power savings
- **Primary accent**: Cyan (`#00E6FF` / `#00DAF2`) — used for interactive elements
- **Secondary accent**: Gold (`#FEB300` / `#FFD798`) — used for AI insights and warnings
- **Tertiary accent**: Green (`#5FEC79`) — income and positive indicators
- **Error**: Warm red (`#FFB4AB`) — expenses and alerts
- **Typography**: System-scaled with custom `StarkTypography` based on Material 3 type scale
- **Components**: `StarkCard` (glass-morphism surfaces), `StarkClickableCard` (press-scale animation), `StarkHeader`, `StarkBottomNavigationBar`

---

## 📋 Known Issues

See the [Implementation Plan](docs/issue_fix_plan.md) for a detailed breakdown and fix strategy.

---

## 📄 License

Copyright © 2025-2026 StarkLabs. All rights reserved.
