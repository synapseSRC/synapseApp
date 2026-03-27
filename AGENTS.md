# Synapse Social Engineering Standards

This document defines the architectural, stylistic, and quality standards for the **Synapse Social** project. All contributors must strictly adhere to these rules.

---

## 🏛️ Core Philosophy — KMP + Clean Architecture

Synapse Social is built on **Kotlin Multiplatform (KMP)**. We share 100% of business logic, data handling, and security protocols, while maintaining 100% native UI on each platform.

### Layer Boundaries (Domain -> Data -> Presentation)

```text
Presentation (Compose / SwiftUI)
        ↕ observes StateFlow or Published properties
ViewModel (Presentation state & UI logic)
        ↕ calls
Domain (UseCases / Repository Interfaces / Domain Models)
        ↕ implemented by
Data (Repository Impls / DataSources / DTOs / Mappers)
```

1.  **Domain knows nothing** about Supabase, Android, iOS, or any framework.
2.  **Data handles external worlds** (Network, Local Storage, SDKs).
3.  **Presentation is platform-specific** and "dumb" — it only renders state and forwards events.

---

## ⚙️ The Shared Engine (`:shared`)

The `:shared` module is the "brain" of the application. It must remain pure Kotlin.

### Domain Layer
- **UseCases:** One public `operator fun invoke()` per class. No logic in constructor.
- **Repositories:** Interfaces ONLY.
- **Models:** Pure Kotlin data classes. No framework-specific annotations (e.g., `@Serializable` is allowed but avoid Room/SQL annotations here).

### Data Layer
- **Repository Impls:** Implement domain interfaces. Orchestrate DataSources.
- **DataSources:** Isolated logic for `SupabaseUserDataSource`, `SqlDelightPostDao`, etc.
- **DTOs:** Match external API/DB schemas exactly.
- **Mappers:** Mandatory conversion between `DTO ↔ Domain Model`. Never leak DTOs to Domain or UI.

### Dependency Injection (Koin)
- Use Koin to provide dependencies within the shared module.
- Exposed to iOS via a `KMPHelper` or `DependencyContainer` in the `iosApp` side.

---

## 🤖 Android Development (`:app`)

### UI Framework
- **100% Jetpack Compose.** No XML layouts (except for system requirements).
- **Theme:** Use `MaterialTheme.colorScheme`, `Spacing.md`, and `stringResource()`.
- **❌ No hardcoded values.** Colors go to `Theme`, dims to `Spacing`, text to `strings.xml`.

### Presentation (MVVM)
- **ViewModels:** One per screen. Hold `StateFlow<UiState>`.
- **DI:** Use **Hilt** for Android-specific injection.
- **Lifecycle:** Handle configuration changes properly via ViewModels.

---

## 🍎 iOS Development (`:iosApp`)

### UI Framework
- **100% SwiftUI.**
- **Concurrency:** Use `async/await` and `Task`. Kotlin `suspend` functions are exposed as `async throws` in Swift.
- **State Management:** Use `ObservableObject`, `@Published`, and `@MainActor` in ViewModels.

### KMP Integration
- Import `shared` framework.
- Consume UseCases directly in Swift ViewModels.
- Use `IosSecureStorage` (from KMP) for Keychain operations.

---

## 🗄️ Data Flow & Standards

### Result Types
- Always return a `Result<T>` or a sealed `Either` type from UseCases to handle errors gracefully without crashing or leaking raw exceptions to the UI.

### Naming Conventions
| Type | Example |
| :--- | :--- |
| UseCase | `SendMessageUseCase` |
| Repository | `ChatRepository` (interface), `SupabaseChatRepository` (impl) |
| ViewModel | `ChatViewModel` |
| DTO | `UserDto` |
| UI State | `ChatUiState` |
| Mapper | `UserMapper.toDomain()` |

---

## ✅ Pre-Commit Checklist

Before opening a PR, ensure:
- [ ] `./gradlew build` passes.
- [ ] No `android.*` or `java.*` imports in `shared/commonMain`.
- [ ] No hardcoded strings/colors/dimensions in `:app`.
- [ ] Business logic is in a `UseCase`, not a ViewModel or View.
- [ ] Every DTO is mapped to a Domain Model.
- [ ] Self-reviewed the diff for redundant code or "TODOs".

---

## 🎁 Pull Request Standards

- **Title:** Use an emoji and clear description (e.g., `✨ feat: Add real-time chat support`).
- **Template:** Always use `.github/PULL_REQUEST_TEMPLATE/feature.md` or `bug_fix.md`.
- **Context:** Explain *why* the change was made, not just *what*.

---

## 📁 Key Paths Reference

| What | Path |
| :--- | :--- |
| Shared Domain | `shared/src/commonMain/kotlin/.../domain/` |
| Shared Data | `shared/src/commonMain/kotlin/.../data/` |
| Android UI | `app/src/main/java/.../` |
| iOS UI | `iosApp/iosApp/` |
| Shared Resources | `shared/src/commonMain/resources/` (if any) |
| PR Templates | `.github/PULL_REQUEST_TEMPLATE/` |
