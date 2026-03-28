# StarkLedger

A premium, high-performance finance and expense tracking application for Android. StarkLedger provides deep financial insights through a futuristic, high-contrast design language that prioritizes clarity and actionable intelligence.

## Core Features

- **Automated SMS Parsing:** High-precision, deterministic parsing of transaction alerts from major Indian banks.
- **Dynamic Weekly Pulse:** A rolling 7-day spending analysis that compares current trends against previous periods, identifying category spikes or celebrating savings.
- **Bento-Grid Dashboard:** A responsive, technical HUD that summarizes liquidity, income/expense flows, and budget health at a glance.
- **StarkLedger Design System:** A dark, clean, professional aesthetic featuring GlassCard components, custom vector iconography (Arc Reactor), and premium typography.
- **AI Strategy Insights:** Real-time analysis of spending patterns with proactive optimization recommendations.

## Recent UI/UX Enhancements

- **Responsive Layouts:** Fixed overlaps in transaction rows and bento grids to support long merchant names and large currency amounts (over ₹1M).
- **Optimized Budget Analysis:** Refined 3-column category grids and AI insight cards for better readability on various mobile screen sizes.
- **Auto-Scaling Typography:** Dynamic font adjustments for critical financial figures to ensure zero truncation.
- **Dynamic Feedback:** Context-aware pulse messaging with visual cues (Green for savings, Amber/Red for spending increases).

## Technical Foundation

- **Jetpack Compose:** Fully declarative UI built with the latest Compose BOM and Kotlin 2.0.0.
- **Room Database:** Robust local data persistence with pre-fetching for high-performance UI updates.
- **MVVM Architecture:** Clean separation of concerns with dedicated ViewModels for Dashboard, Analytics, and History.
- **Deterministic Logic:** Multi-step verification for financial SMS to ensure zero false positives.
