# StarkLedger ⚡

**StarkLedger** is a premium, performance-obsessed native Android application for automated expense tracking and financial management. Built with a futuristic 'StarkLedger' design language, it transforms raw bank SMS data into clear, actionable financial insights with zero manual input required.

---

## 🚀 Key Features

- **Automated High-Precision SMS Parsing**: Uses a deterministic, multi-step engine to accurately extract amounts, merchants, banks, and account details from complex Indian bank SMS formats (HDFC, SBI, ICICI, etc.).
- **StarkLedger Design System**: A dark, high-contrast, professional fintech aesthetic featuring Glassmorphism, Bento-grid layouts, and advanced UI components like `GlassCard`.
- **Intelligent Categorization**: Automatically maps transactions to categories like Food, Travel, Shopping, and Bills using user-defined merchant mappings and keyword matching.
- **Advanced Visualizations**:
    - `GlowingLineChart` for spending trends.
    - `AnimatedDonutChart` for category distribution.
- **Historical Scanning**: Instantly scans up to 1,000 recent SMS messages upon first launch to build your financial history.
- **Data Privacy & Security**: All data remains offline on your device, stored in an encrypted Room database.

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0.0 (Kotlin-Compose Compiler Plugin)
- **UI**: Jetpack Compose (BOM 2024.04.01)
- **Database**: Room (Single-activity architecture)
- **Architecture**: Clean Architecture / Domain-Driven Design (DDD)
- **Storage**: Jetpack DataStore for user preferences.

## ⚙️ Development & Build

### Prerequisites
- Android SDK (API Level 35)
- Java 17+

### Build Commands
To build the debug APK:
```bash
./gradlew assembleDebug
```

To run unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

To perform linting:
```bash
./gradlew :app:lintDebug
```

## 🔒 Security & Privacy

StarkLedger is designed with a "Privacy First" approach:
- **Zero Cloud**: No financial data ever leaves your device.
- **Biometric Lock**: Optional security layer to protect your financial logs.
- **Minimal Permissions**: Only requires `READ_SMS` for transaction automation.

---

*StarkLedger is a StarkLabs product. Precision in every byte, clarity in every pixel.*
