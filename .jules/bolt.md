## 2025-01-24 - [N+1 Query in SMS Scanning]
**Learning:** During historical SMS scanning (1000+ messages), performing a database query to identify categories for every message (O(N)) significantly slows down the process.
**Action:** Pre-fetch lookup data (like categories) once before starting bulk processing loops and pass them as optional parameters to domain logic.

## 2025-01-24 - [Regex Allocation Overhead]
**Learning:** Repeatedly creating `Regex` or `Pattern` objects inside loops or high-frequency methods (like `SmsParser.classifyMessage`) leads to excessive garbage collection and CPU overhead.
**Action:** Pre-compile frequently used regexes as `private val` constants (using `java.util.regex.Pattern` for Android/JVM) and use simple `String.contains()` guards before executing the full regex.
