---
title: Modules & Structure
description: Overview of the Synapse Social project structure.
---

Synapse is a multi-module Gradle project designed for Kotlin Multiplatform (KMP).

## Project Tree

```text
Synapse/
├── app/                  # 🤖 Android Application (Native UI)
├── shared/               # 🧠 KMP Shared Engine (Logic, Data, Domain)
├── iosApp/               # 🍎 iOS Application (Native SwiftUI)
├── webApp/               # 🌐 Web Application (React/Wasm)
└── gradle/               # 🐘 Build configuration
```

---

## Module Details

### 1. `:app` (Android Application)
The entry point for the Android application.
- **Type:** `com.android.application`
- **Focus:** Native UI (Jetpack Compose), Device specific features (Camera, Permissions), Dependency Injection (Hilt).
- **Key Packages:**
    - `ui/`: Design system, themes, common components.
    - `feature/`: Feature-sliced screens (e.g., `auth`, `feed`, `profile`).
    - `di/`: Hilt modules bridging Android to the Shared Engine.

### 2. `:shared` (The Engine)
The heart of the application. Contains all business logic, networking, and storage.
- **Type:** `com.android.library` (Multiplatform)
- **Targets:**
    - `androidTarget()`
    - `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`
    - `wasmJs()` (Experimental)
- **Structure:**
    - `commonMain/`: Code shared across all platforms (90% of logic).
    - `androidMain/`: Android-specific implementations.
    - `iosMain/`: iOS-specific implementations.

### 3. `:iosApp` (iOS Application)
A native Xcode project consuming the Shared Engine.
- **Type:** Xcode Project (Swift)
- **Focus:** Native UI (SwiftUI).
- **Integration:** Consumes `:shared` compiled as an XCFramework.

---

## Key Libraries

| Category | Library | Module |
| :--- | :--- | :--- |
| **Networking** | Ktor + Supabase-kt | `:shared` |
| **UI** | Jetpack Compose | `:app` |
| **Injection** | Hilt | `:app` |
| **Injection** | Koin | `:shared` |
| **Async** | Coroutines | All |
| **Image Loading** | Coil | `:app` |
