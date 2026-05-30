# Synapse Social — Engineering Standards

All contributors and AI agents must follow these standards without exception.

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

- Title format: `✨ feat: Short description` or `🐛 fix: Short description`
- Use `.github/PULL_REQUEST_TEMPLATE/feature.md` or `bug_fix.md`
- Explain *why*, not just *what*

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

## AI Agent Operational Boundaries

To ensure efficient and safe autonomous operations, all AI agents (including Jules) must adhere to the following boundaries.

### 🛠️ Accessible Tools
Agents have access to a standard set of engineering tools, including but not limited to:
- **File System**: `list_files`, `read_file`, `write_file`, `delete_file`, `rename_file`.
- **Git**: `replace_with_git_merge_diff`, `submit`, `request_code_review`.
- **Bash**: `run_in_bash_session` for running builds, tests, and installing dependencies.
- **Documentation**: `set_plan`, `plan_step_complete`, `request_plan_review`.

### 📂 Modification Rights
- **Allowed**: Any source file in `:shared`, `:app`, `:iosApp`, `:desktop`, `:web`, and root configuration files (`build.gradle`, `gradle.properties`, etc.).
- **Restricted**: Do not modify build artifacts (e.g., `build/` directories, generated SQLDelight code, or compiled `.aar`/`.apk` files). Always edit the source and regenerate the artifact.

### ✅ Definition of Done (DoD)
A task is considered "Done" only when:
1. The code compiles successfully on the targeted platforms (Android, iOS, etc.).
   - *Note: If the changes are exclusively documentation (e.g., `.md` files) or code comments, this step and the next can be skipped to save time and resources.*
2. All relevant unit tests pass.
3. Documentation (KDocs or Markdown) is updated to reflect the changes.
4. The change has been verified via manual or automated checks (e.g., `read_file` or screenshot verification).
5. Pre-commit instructions have been followed.

### 🛑 Error Handling & Loop Prevention
- If a build fails more than 3 times with the same error, the agent must stop and use `request_user_input` with a summary of attempted fixes.
- Never attempt to install system-level packages without first checking if they are already available or if there is a project-local alternative.
- If a plan step is blocked by an external dependency or unclear requirement, seek clarification immediately rather than making assumptions.
