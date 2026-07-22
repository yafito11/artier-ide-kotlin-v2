---
name: android-debug
description: Debug Android/Kotlin build and runtime issues including Gradle, Compose, Hilt, and Logcat. Use when the user hits build errors, crashes, or Android-specific bugs.
license: MIT
metadata:
  author: artier
  version: "1.0"
---

# Android Debug Skill

## Approach

1. Reproduce the failure (build log, stack trace, Logcat)
2. Identify layer: Gradle / Kotlin compile / runtime / Compose UI / DI (Hilt)
3. Check common causes:
   - Missing or wrong dependency versions
   - `minSdk` / API level mismatches
   - Hilt missing `@HiltAndroidApp` / `@AndroidEntryPoint`
   - Compose recomposition or state issues
   - Proguard/R8 only in release

## Output

- Root cause hypothesis
- Exact file/symbol to change
- Minimal fix steps
- How to verify (`./gradlew assembleDebug`, run instrumented test, etc.)
