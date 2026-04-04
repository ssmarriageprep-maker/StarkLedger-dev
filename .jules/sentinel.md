## 2025-01-24 - [PIN Storage & Logging Security]
**Vulnerability:** User PINs were stored in plaintext in Jetpack DataStore, and full SMS bodies containing sensitive financial data were being logged to the system console.
**Learning:** Initial security implementations often prioritize functionality over defense-in-depth, leading to plaintext storage of secrets. Additionally, verbose logging for debugging often leaks PII/financial data in production-ready paths.
**Prevention:** Always hash secrets (like PINs) with a salt before storage. Use a migration strategy when upgrading security logic to avoid locking out existing users. Enforce strict logging policies that exclude raw transaction data and PII.
