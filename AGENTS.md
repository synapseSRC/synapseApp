# Synapse Social — Engineering Standards

All contributors and AI agents must follow these standards without exception.

> **⚠️ MANDATORY — Agents MUST do this before starting ANY task:**
>
> 1. **Read [`REVIEW.md`](./REVIEW.md) in full** — ROST-level review pillars, deep-dive checkpoints, severity matrix (`rost-block` / `rost-warn` / `suggestion` / `nit`), and comment conventions.
>
> **These are not suggestions.** Agents that skip this step will produce non-compliant output that will be rejected:
> - PRs targeting `main` directly → rejected (must target `develop`)
> - Commits not following Conventional Commits format → rejected
> - Code with `rost-block` violations (business logic in UI, platform imports in `commonMain`, unhandled exceptions) → rejected
>
> If CONTRIBUTING.md or REVIEW.md conflict with anything in this file, **CONTRIBUTING.md and REVIEW.md win**.

---

## Architecture

Synapse Social uses **Kotlin Multiplatform (KMP)** with **Clean Architecture + MVVM**.

```
Presentation  →  ViewModel  →  Domain (UseCases / Interfaces)  →  Data (Repos / DTOs / Mappers)
```

- Domain has zero knowledge of any framework, SDK, or platform.
- Data owns all external concerns: network, storage, SDKs.
- Presentation is dumb — it renders state and forwards events only.

---

## Shared Module (`:shared`)

**Domain**
- One `operator fun invoke()` per UseCase. No constructor logic.
- Repositories are interfaces only.
- Models are pure Kotlin data classes. No Room/SQL annotations.

**Data**
- Repository impls orchestrate DataSources.
- DTOs mirror the external schema exactly.
- Mappers are mandatory. DTOs never reach Domain or UI.

**DI:** Koin. Exposed to iOS via `DependencyContainer`.

---

## Android (`:app`)

- 100% Jetpack Compose. No XML.
- One ViewModel per screen, holding `StateFlow<UiState>`.
- DI via Hilt.
- Use `MaterialTheme.colorScheme`, `Spacing`, and `stringResource()`. No hardcoded values.

---

## iOS (`:iosApp`)

- 100% SwiftUI.
- ViewModels use `ObservableObject`, `@Published`, `@MainActor`.
- Consume UseCases directly from the shared framework.
- Use `IosSecureStorage` for Keychain operations.

---

## Code Standards

- UseCases always return `Result<T>` or a sealed `Either` — never raw exceptions.
- No `android.*` or `java.*` imports in `shared/commonMain`.
- Business logic lives in UseCases, not ViewModels or Views.

**Naming**

| Type | Convention |
| --- | --- |
| UseCase | `SendMessageUseCase` |
| Repository | `ChatRepository` (interface), `SupabaseChatRepository` (impl) |
| ViewModel | `ChatViewModel` |
| DTO | `UserDto` |
| UI State | `ChatUiState` |
| Mapper | `UserMapper.toDomain()` |

---

## Pre-Commit Checklist

- [ ] `./gradlew build` passes
- [ ] No framework imports in `shared/commonMain`
- [ ] No hardcoded strings, colors, or dimensions in `:app`
- [ ] Business logic is in a UseCase
- [ ] Every DTO has a mapper to a Domain Model
- [ ] Diff self-reviewed for dead code and TODOs

---

## Pull Requests

All PRs **must** follow the structure defined in the template files:

- **Feature PRs** → use `.github/PULL_REQUEST_TEMPLATE/feature.md`
  - Title: `✨ feat: [concise summary]`
  - Required sections: Feature Description, Implementation Details, UI/UX (if applicable), Verification, Build Status, References

- **Bug Fix PRs** → use `.github/PULL_REQUEST_TEMPLATE/bug_fix.md`
  - Title: `🐞 fix: [concise summary]`
  - Required sections: Bug Description, Fix Approach, Verification, Build Status, References

**Rules:**
- All contributors and agents must follow the workflow, branching strategy, and commit format defined in [CONTRIBUTING.md](./CONTRIBUTING.md).
- Every checkbox in the template must be explicitly checked or marked N/A — do not leave items blank.
- Explain *why*, not just *what*.
- PRs that do not follow the template structure or `CONTRIBUTING.md` guidelines will be rejected.

---

## Key Paths

| What | Path |
| --- | --- |
| Shared Domain | `shared/src/commonMain/kotlin/.../domain/` |
| Shared Data | `shared/src/commonMain/kotlin/.../data/` |
| Android UI | `app/src/main/kotlin/.../` |
| iOS UI | `iosApp/iosApp/` |
| PR Templates | `.github/PULL_REQUEST_TEMPLATE/` |

---

