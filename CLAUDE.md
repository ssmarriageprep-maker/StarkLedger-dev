# StarkLedger Development Guide

## Project Overview

StarkLedger is a privacy-first Android personal finance application developed by StarkLabs.

The application automatically tracks financial transactions using an on-device SMS Intelligence Engine without requiring bank login, cloud processing, or external financial aggregation APIs.

All transaction parsing and financial analysis occur entirely on the user's device.

Primary goals:

1. Privacy-first
2. Zero cloud dependency
3. Fast and intuitive UX
4. High-quality financial insights
5. Modern fintech-grade design
6. Offline-first architecture

---

# Product Positioning

StarkLedger is NOT:

* An AI expense tracker
* A cloud-based budgeting platform
* A bank aggregation application

StarkLedger IS:

* A privacy-first financial intelligence application
* An on-device transaction tracking system
* A smart SMS-based expense tracker
* A financial insights platform

Core user promise:

"Understand your finances without sharing your data."

Never introduce features that require sending financial data to StarkLabs servers unless explicitly requested.

---

# Technology Stack

Language:

* Kotlin

UI:

* Jetpack Compose
* Material 3

Architecture:

* MVVM
* Clean Architecture

Storage:

* Room Database
* DataStore Preferences

Security:

* Android Biometric API

Android Versions:

* Min SDK: 24
* Target SDK: 34+
* Compile SDK: 35

---

# Repository Structure

data/

* Room entities
* DAOs
* Repositories
* Local persistence

domain/

* Business logic
* SMS Intelligence Engine
* Parsing
* Insights

sms/

* SmsReceiver
* SmsScanner
* SMS ingestion

ui/

* Compose screens
* ViewModels
* Navigation

theme/

* Colors
* Typography
* Theme system

components/

* Reusable Compose components

features/

* home
* analytics
* history
* budgets
* accounts
* security
* settings

---

# SMS Intelligence Engine

The SMS engine is the most important part of the application.

Treat it as critical infrastructure.

Avoid unnecessary rewrites.

Always preserve backward compatibility.

Current architecture:

Phase 1:
Classification

Phase 2:
Extraction

---

## Phase 1 Classification

Responsibilities:

* Identify transactional SMS
* Reject non-transactional SMS
* Pattern detection
* Confidence scoring

Outputs:

* category
* confidence
* patternDetected
* patternUsed

---

## Phase 2 Extraction

Responsibilities:

* Amount extraction
* Merchant extraction
* Bank extraction
* Account extraction
* Transaction type extraction
* Date extraction
* Reference extraction

---

# Existing Pattern Coverage

Current supported patterns:

* UPI_SENT
* UPI_RECEIVED
* BANK_DEBIT
* BANK_CREDIT
* CARD_SPEND
* CARD_PAYMENT_RECEIVED
* AUTOPAY_DEBIT
* ACH_DEBIT
* WALLET_SPEND
* WALLET_CREDIT
* ATM_WITHDRAWAL
* UNKNOWN

Never remove support for existing patterns.

Always add regression tests.

---

# SMS Parsing Rules

Must never:

* Return amount = 0.0 for valid transactions
* Extract phone numbers as merchants
* Treat telecom offers as transactions
* Treat OTP messages as transactions
* Store full account numbers

Must always:

* Use masked account identifiers
* Preserve privacy
* Use confidence scoring
* Generate deterministic results

---

# Account System

Account-based filtering is a core feature.

Account Types:

* BANK
* CARD
* WALLET

Account identity:

bankName + last4Digits

Examples:

HDFC + 3263

SBI + 1234

Pluxee + 1322

Must generate unique account IDs.

If account is unknown:

Use:

Unknown Account

Never crash due to missing account information.

---

# UI Design Philosophy

StarkLedger should feel like:

* Cred-level polish
* Google Pay clarity
* Notion simplicity

Not:

* Cyberpunk
* Gaming UI
* Over-designed dashboards

---

# UI Principles

Priority:

Clarity > Style

Insights > Charts

Speed > Animation

Trust > Decoration

---

# Visual Theme

Theme:

Dark mode first

Colors:

Background:
#0D0D0D

Card:
#161616

Primary:
#00E6FF

Secondary:
#FFB400

Success:
#34C759

Error:
#FF3B30

Text:
#FFFFFF

Secondary Text:
#A0A0A0

---

# Layout Rules

Padding:

16dp minimum

Spacing system:

4dp
8dp
12dp
16dp
20dp
24dp
32dp

Card radius:

16dp

Button radius:

12dp

---

# Performance Requirements

Every feature must maintain:

App startup:
< 2 seconds

Screen transitions:
< 200ms

Smooth scrolling:
60 FPS

No expensive recompositions.

Use:

* remember
* derivedStateOf
* immutable state
* LazyColumn

Avoid:

* Heavy calculations inside Composables
* Unnecessary recompositions

---

# Dashboard Design

Dashboard must answer:

1. How much money do I have?
2. Where is my money going?
3. Am I overspending?

Required sections:

1. Total Balance
2. Income vs Expense
3. Budget Health
4. Top Categories
5. Recent Transactions

Avoid clutter.

---

# Analytics Design

Analytics should be insight-first.

Display:

* Insight summary
* Category breakdown
* Spending trends
* Budget analysis

Never show charts without context.

---

# Future Roadmap

High Priority:

1. Account Filtering
2. Merchant Normalization
3. Category Engine
4. Insights Engine
5. Budget Forecasting

Medium Priority:

6. User Correction Learning
7. Multi-language SMS Support
8. Export to PDF
9. Export to Excel

Long-Term:

10. On-device ML
11. TensorFlow Lite NER
12. Auto Pattern Discovery

---

# Development Rules

Before implementing any feature:

1. Understand current architecture
2. Preserve existing behavior
3. Add tests
4. Run tests
5. Verify UI performance
6. Check edge cases

Never introduce breaking changes without migration support.

---

# Testing Requirements

Every feature must include:

Unit Tests

Edge Cases

Regression Tests

Account Filtering Tests

SMS Parsing Tests

Performance Validation

New functionality is not complete until tests pass.

---

# Code Quality Standards

Prefer:

* Small composables
* Single responsibility classes
* Explicit naming
* Immutable models

Avoid:

* God classes
* Large ViewModels
* Duplicate logic
* Hidden side effects

---

# Decision Framework

When choosing between two implementations:

Choose the solution that:

1. Preserves privacy
2. Improves clarity
3. Improves performance
4. Reduces complexity
5. Improves maintainability

Privacy always wins.

---

# StarkLabs Philosophy

The product should make users feel:

"I understand my money instantly."

The product should never make users feel:

"This app knows too much about me."

Protect user trust at all costs.
