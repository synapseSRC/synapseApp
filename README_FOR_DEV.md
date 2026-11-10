# Synapse Social — Developer Guide

**Synapse Social** is a Kotlin Multiplatform (KMP) social media application built for Android, iOS, desktop, and web. It follows Clean Architecture + MVVM, shares business logic across platforms, and ships with modern UI, secure local storage, real-time messaging, and a structured developer workflow.

> **Audience:** Developers, contributors, and AI agents who want to build, extend, or maintain the Synapse Social codebase.

---

## 1. Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language | Kotlin 2.4.x |
| Multiplatform | Kotlin Multiplatform (KMP) |
| Android UI | Jetpack Compose + Material 3 |
| iOS UI | SwiftUI |
| Desktop UI | Compose for Desktop |
| Web UI | Next.js (TypeScript/Tailwind) |
| DI (Android) | Hilt |
| DI (Shared) | Koin |
| Build tool | Gradle |
| Backend | Supabase (Auth, Realtime, Storage, PostgREST, Edge Functions) |
| Networking | Ktor |
| Local persistence | SQLDelight |
| E2EE | Signal Protocol (libsignal-client) |
| Calling / Media | WebRTC + LiveKit |
| AI / LLM | Firebase + Google Gemini |
| Push | Firebase Cloud Messaging (FCM) + OneSignal |
| Error tracking | Sentry |
| Logging | Napier |

---

## 2. Architecture

Synapse Social uses **Clean Architecture + MVVM** across all platforms.

```
Presentation → ViewModel → Domain (UseCases / Interfaces) → Data (Repos / DTOs / Mappers)
```

### Core rules

- **Domain** has zero knowledge of any framework, SDK, or platform. No Supabase types, schemas, or semantics.
- **Data** owns all external concerns: network, storage, SDKs.
- **Presentation** is dumb — it renders state and forwards events only.
- **UseCases** always return `Result<T>` or a sealed `Either` — never raw exceptions.
- **DTOs** never reach Domain or UI; mappers are mandatory.
- **DI:** Koin in shared, Hilt on Android.

### Dependency direction

```
[UI Layer (Compose/SwiftUI)]
        │
        ▼
[ViewModel / State Holders]
        │
        ▼
[Domain (UseCases)] ◄── [Data (Repos/APIs)]
```

---

## 3. Repository Structure

```text
synapseApp/
├── .agents/                  # AI agent conventions and skills
│   └── skills/               # Agent skill definitions
├── .github/                  # GitHub workflows, issue templates, PR templates
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE/
│   ├── stale.yml
│   └── workflows/
├── app/                      # Android app module (Jetpack Compose + Hilt)
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
├── desktop/                  # Desktop app module (Compose for Desktop)
│   ├── build.gradle.kts
│   └── src/
├── iosApp/                   # iOS app project (SwiftUI)
│   └── iosApp/
├── shared/                   # Shared KMP domain/data logic
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── commonMain/
│       ├── commonTest/
│       ├── androidMain/
│       ├── androidUnitTest/
│       ├── iosMain/
│       ├── jvmMain/
│       ├── wasmJsMain/
│       └── nativeInterop/
├── supabase/                 # Supabase configuration and Edge Functions
│   ├── config.toml
│   └── functions/
├── web/                      # Next.js web landing/site app
│   ├── src/
│   ├── tailwind.config.ts
│   ├── next.config.mjs
│   └── package.json
├── assets/                   # Brand images and visual assets
├── AGENTS.md                 # Engineering and contributor standards (authoritative)
├── README.md                 # Public project landing page
├── README_FOR_DEV.md         # This file — developer guide
├── REVIEW.md                 # ROST-level code review guidelines
├── build.gradle              # Root Gradle plugin declarations
├── gradle.properties         # Project properties + env var defaults
├── gradlew / gradlew.bat     # Gradle wrapper
├── gradle/
│   ├── wrapper/
│   └── gradle-daemon-jvm.properties
├── settings.gradle           # Module inclusions (:app, :shared, :desktop, :web)
└── .gitignore
```

---

## 4. Prerequisites

### Required tools

| Tool | Version | Notes |
| :--- | :--- | :--- |
| JDK | 17 | Required for Gradle/Kotlin compilation |
| Gradle Wrapper | bundled | Use `./gradlew`, do not install Gradle globally |
| Android Studio | latest | For Android development |
| Android SDK | API 37 compile, minSdk 26 | `compileSdk 37`, `minSdk 26` |
| Xcode | latest | Required for iOS builds (macOS only) |
| Node.js | 18+ | Required for the web app |
| Git | 2.30+ | Version control |

### Environment variables

Create a `local.properties` file (not committed) if you need local SDK paths:

```properties
sdk.dir=/path/to/android-sdk
```

Sensitive values (Supabase keys, OAuth client IDs, etc.) are managed via `gradle.properties` and system environment variables. See `gradle.properties` for the list of configurable properties. **Never commit real secrets.**

---

## 5. Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd synapseApp
```

### 2. Configure environment

Copy `gradle.properties` if you need local overrides. Required properties (from `gradle.properties`):

- `SUPABASE_URL` — Supabase project URL
- `SUPABASE_ANON_KEY` — Supabase anon key
- `GOOGLE_WEB_CLIENT_ID` — Google OAuth web client ID
- `ONESIGNAL_APP_ID` — OneSignal app ID
- `TURN_FUNCTION_NAME` — Supabase Edge Function for TURN credentials
- Auth config: `auth.require.email.verification`, `auth.development.mode`, etc.

> **Note:** `gradle.properties` already contains development defaults. Do not commit changes that expose real secrets.

### 3. Sync the project

```bash
./gradlew --stop
./gradlew clean
```

### 4. Compile

Verify the project compiles before making changes:

```bash
./gradlew compileDebugKotlin
```

For Android:

```bash
./gradlew :app:compileDebugKotlin
```

For shared (all targets):

```bash
./gradlew :shared:compileDebugKotlin
./gradlew :shared:compileReleaseKotlin
```

---

## 6. Build & Run

### Android

| Task | Description |
| :--- | :--- |
| `./gradlew :app:assembleDebug` | Build a debug APK |
| `./gradlew :app:installDebug` | Build and install debug APK on connected device |
| `./gradlew :app:assembleRelease` | Build a release APK |
| `./gradlew :app:compileDebugKotlin` | Compile Android debug sources |

Run on a connected device/emulator:

```bash
./gradlew :app:installDebug
```

### iOS (macOS only)

iOS targets are defined in `shared/build.gradle.kts`. Build the shared framework:

```bash
./gradlew :shared:compileReleaseKotlinIosArm64
```

Open the `iosApp/iosApp` Xcode project to build and run on simulator/device.

### Desktop (Compose for Desktop)

```bash
./gradlew :desktop:run
```

Package distributions:

```bash
./gradlew :desktop:package
```

### Web

The web app is a separate Next.js project in `/web`:

```bash
cd web
npm install
npm run dev
# or
npm run build
npm start
```

---

## 7. Configuration

### Gradle properties (`gradle.properties`)

Key properties:

```properties
projectVersion=1.0.2
android.suppressUnsupportedCompileSdk=36
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
ksp.useKSP2=true
```

Supabase and third-party credentials are injected into `BuildConfig` at build time from environment variables or `gradle.properties`:

- `SUPABASE_URL`, `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`
- `ONESIGNAL_APP_ID`, `ONESIGNAL_REST_API_KEY`
- `TURN_FUNCTION_NAME`
- Auth tuning: `auth.require.email.verification`, `auth.development.mode`, `auth.resend.cooldown.seconds`, etc.

### Build types

| Build type | Application ID | Use case |
| :--- | :--- | :--- |
| `debug` | `com.synapse.social.studioasinc.debug` | Local development |
| `release` | `com.synapse.social.studioasinc` | Release builds |

---

## 8. Coding Standards & Conventions

All contributors and AI agents must follow `AGENTS.md` without exception. Key points:

### Naming conventions

| Type | Convention | Example |
| :--- | :--- | :--- |
| UseCase | `SendMessageUseCase` | `operator fun invoke()` |
| Repository (interface) | `ChatRepository` | interface only |
| Repository (impl) | `SupabaseChatRepository` | data-layer impl |
| ViewModel | `ChatViewModel` | `StateFlow<UiState>` |
| DTO | `UserDto` | mirrors external schema |
| UI State | `ChatUiState` | immutable data class |
| Mapper | `UserMapper` | `toDomain()` extension |

### Layer responsibilities

- **Domain:** pure Kotlin data classes, repository interfaces, one `invoke()` per UseCase. No `android.*`/`java.*` imports in `shared/commonMain`.
- **Data:** DTOs, repository impls, DataSources, mappers (`toDomain()`), SQLDelight, Supabase/Ktor clients.
- **Presentation:** Compose/SwiftUI UI, ViewModels holding `StateFlow<UiState>`, no backend SDK calls.

### Dependency Injection

- Shared: Koin, exposed to iOS via `DependencyContainer`.
- Android: Hilt, one ViewModel per screen.

### Code style

- No hardcoded strings, colors, or dimensions in `:app`. Use `MaterialTheme.colorScheme`, `Spacing`, `stringResource()`.
- Unidirectional data flow: Composables receive immutable `UiState` and emit events upward via `onAction: (UiEvent) -> Unit`.
- No `MutableState`/`MutableStateFlow` passed down to UI.
- Side effects only in `LaunchedEffect`, `SideEffect`, or `DisposableEffect`.

---

## 9. Testing Policy — STRICT

> **Do NOT write or run tests unless explicitly requested by the user.** Verify work via compilation and build tasks only.

- Agents must not write, generate, or add unit tests, UI tests, or test suites.
- Agents must not run `./gradlew test` or `testDebugUnitTest`.
- Verify via: `./gradlew compileDebugKotlin` / `./gradlew assembleDebug`.

---

## 10. Pull Requests

All PRs must follow the template structure in `.github/PULL_REQUEST_TEMPLATE/`:

- **Feature PRs** → `feature.md`, title: `✨ feat: [summary]`
- **Bug Fix PRs** → `bug_fix.md`, title: `🐞 fix: [summary]`

Required sections: Feature/Bug Description, Implementation/Fix Approach, Verification, Build Status, References.

Every checkbox must be checked or marked N/A. Explain *why*, not just *what*.

Before committing:
1. Inspect `git status`, `git diff`, and `git log --oneline -10`.
2. Stage only intended files; never commit secrets.
3. Verify compilation: `./gradlew compileDebugKotlin`.
4. Self-review diff for dead code and TODOs.

---

## 11. Key Gradle Tasks

```bash
# Compile everything
./gradlew compileDebugKotlin

# Android debug build
./gradlew :app:assembleDebug

# Android release build
./gradlew :app:assembleRelease

# Install debug APK
./gradlew :app:installDebug

# Desktop run
./gradlew :desktop:run

# Desktop package
./gradlew :desktop:package

# iOS framework compile (macOS)
./gradlew :shared:compileReleaseKotlinIosArm64

# Stop Gradle daemon
./gradlew --stop
```

---

## 12. Common Workflows

### Adding a new feature

1. Create a UseCase in `shared/.../domain/` with `operator fun invoke()` returning `Result<T>`.
2. Define a repository interface in `shared/.../domain/`.
3. Implement the repository in `shared/.../data/` (e.g., `SupabaseXxxRepository`).
4. Add DTOs and a mapper (`toDomain()`).
5. Register the impl in Koin (`DependencyContainer`).
6. Call the UseCase from a ViewModel (`StateFlow<UiState>`).
7. Build the Compose/SwiftUI screen consuming the ViewModel state.
8. Verify: `./gradlew compileDebugKotlin`.

### Adding a new platform target

1. Add the target to `shared/build.gradle.kts` (`kotlin { ... }` block).
2. Add platform-specific dependencies in the corresponding source set.
3. Add platform UI code under the new module (`iosApp/`, `desktop/`, `web/`).
4. Update `settings.gradle` if a new Gradle module is introduced.

---

## 13. Security Notes

- Never commit API keys, OAuth secrets, or Supabase service-role keys.
- `TURN_SHARED_SECRET` is a Supabase function secret — set it via Supabase dashboard/CLI, never in `gradle.properties` or the app.
- Supabase Edge Function secrets live in Supabase function secrets, not in the client.
- The `release` signing config in `app/build.gradle` uses placeholder passwords; replace with real values for production.
- `local.properties`, `keystore.properties`, and `release_key.jks` must not be committed.

---

## 14. Troubleshooting

| Problem | Fix |
| :--- | :--- |
| Gradle daemon memory issues | Increase `org.gradle.jvmargs` heap (`-Xmx4g`) |
| SDK not found | Set `sdk.dir` in `local.properties` |
| iOS targets fail on non-macOS | iOS targets are guarded; build shared JVM/Android only |
| Supabase build config empty | Set `SUPABASE_URL`/`SUPABASE_ANON_KEY` in env or `gradle.properties` |
| KSP annotation processing errors | Run `./gradlew clean` then rebuild |

---

## 15. References

- `AGENTS.md` — Authoritative engineering standards
- `REVIEW.md` — ROST-level code review guidelines
- `README.md` — Public project overview and brand assets
- `.github/PULL_REQUEST_TEMPLATE/` — PR templates
- `settings.gradle` — Module declarations

---

*This guide is maintained alongside `AGENTS.md`. If anything here conflicts with `AGENTS.md`, `AGENTS.md` takes precedence.*