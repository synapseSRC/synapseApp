# Synapse Social ROST-Level Review Guidelines
This document outlines the strict, max-level rigorous principles and processes for conducting ROST (Rigorous Objective Structural Testing) code reviews in the Synapse Social project.
When conducting reviews at **ROST Level (Max Level)**, there is zero tolerance for architectural erosion, silent failures, or sloppy state management.
## 🛡️ ROST Core Pillars
 1. **Zero Architectural Drift**: Keep presentation, domain, and data layers completely decoupled.
 2. **Defensive Stability**: Code must actively prevent crashes, race conditions, and unhandled edge cases.
 3. **KMP Memory & Thread Safety**: Ensure correct concurrency boundaries across JVM/Android and Native (iOS) runtimes.
 4. **State Immutability**: All UI and business state must flow downstream in a strictly unidirectional, immutable fashion.
## 🔍 ROST Deep-Dive Checkpoints
### 🏗️ 1. Clean Architecture & Domain Purity
 * **The UseCase Rule**: Every UseCase must expose exactly **one public invoke() operator function**. It must be stateless and focus on a single piece of business logic.
 * **Domain Isolation**: The shared:domain layer must contain zero platform-specific imports, zero UI references, and absolutely no database/network framework code (e.g., SQLDelight, Ktor schemas must stay in shared:data).
 * **Dependency Rule**: Dependencies must only point inwards (Presentation -> Domain <- Data).
```
[UI Layer (Compose/SwiftUI)] ──> [ViewModel / State Holders]
                                         │
                                         ▼
                                [Domain (UseCases)] <── [Data (Repos/APIs)]
```
### 🧩 2. Kotlin Multiplatform (KMP) Concurrency & Safety
 * **Expect/Actual Boundaries**: Keep expect/actual declarations to an absolute minimum. Favor interface abstractions injected via dependency injection (Koin) over platform-specific hacks.
 * **Coroutine Scopes**:
   * ViewModel scopes must use viewModelScope or platform-agnostic equivalents.
   * Avoid using GlobalScope or un-scoped coroutine dispatchers.
   * Background tasks must always specify correct Dispatchers (Dispatchers.Default for CPU-heavy tasks, Dispatchers.IO for disk/network).
 * **Flows & Channels**: Ensure cold Flow flows are used for data streams and hot SharedFlow/StateFlow are used appropriately for state sharing. Ensure proper collector cancellation to prevent memory leaks on the iOS side.
### 🎨 3. UI Layer (Compose & SwiftUI State Hardening)
 * **Unidirectional Data Flow (UDF)**:
   * Composables must receive a single, immutable UI State object and emit events upwards (e.g., onAction: (UiEvent) -> Unit).
   * Never pass mutable state objects (like MutableState or MutableStateFlow) directly down to UI components.
 * **Performance & Recomposition**:
   * Ensure classes passed to Compose are stable. Wrap unstable types in immutable wrappers if necessary.
   * Remember values appropriately in Composables using remember or rememberSaveable.
   * Keep Composables side-effect-free. Use LaunchedEffect, SideEffect, or DisposableEffect exclusively for side-effects, never run business logic directly in the body of a Composable.
 * **SwiftUI Interop**: Ensure state publishers exposed to Swift do not cause memory leaks or thread access issues when collected in iOS.
### 🔌 4. Data Layer & Network Hardening
 * **Error Handling**: Network and database operations must not throw unhandled exceptions. All calls must return structured results (e.g., a custom functional Result class or Resource<T>).
 * **Caching & Single Source of Truth**: The repository must act as the orchestrator. UI must never query database/network helpers directly.
### 🧪 5. ROST-Grade Testing Standards
 * **No Happy-Path-Only Tests**: Unit tests must explicitly cover error states, empty states, slow networks, null payloads, and edge cases.
 * **Mocking**: Ensure repository interfaces are properly faked/mocked without using heavy-reflection mocking libraries in common code.
## 🤝 ROST Level Review Matrix

| Severity Level | Action Required | Examples |
| :--- | :--- | :--- |
| **🚨 Block (Fatal)** | Must fix before merge. No exceptions. | - Business logic leaking to UI. <br> - Platform-specific import in commonMain. <br> - Thread safety issues or unhandled repository exceptions. <br> - UI State mutation inside a UI Composable. |
| **⚠️ Warn (Major)** | Fix highly recommended; require explicit justification if bypassed. | - Highly complex logic missing direct unit tests. <br> - KDocs missing on a newly added public API. <br> - Non-optimal recomposition triggers in Compose. |
| **💡 Suggestion (Minor)** | Non-blocking. Up to developer's discretion. | - Code style alignment. <br> - Potential naming improvements for local variables. |

## 🚀 Conventional Comments
Use strict prefixes for review comments to keep them readable and actionable:
 * rost-block: Absolute blocker. Violates architecture or runtime safety.
 * rost-warn: Strongly advised change due to performance or test gaps.
 * suggestion: Helpful refactoring advice.
 * nit: Minor style, formatting, or readability point.