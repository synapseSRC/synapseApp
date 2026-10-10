# Synapse Social — Engineering Standards

All contributors and AI agents must follow these standards without exception.

> **⚠️ MANDATORY — Agents MUST do this before starting ANY task:**
>
> 1. **Read [`REVIEW.md`](./REVIEW.md) in full** — ROST-level review pillars, deep-dive checkpoints, severity matrix (`rost-block` / `rost-warn` / `suggestion` / `nit`), and comment conventions.
>
> **These are not suggestions.** Agents that skip this step will produce non-compliant output that will be rejected:
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

## Agent Skills

### Material 3 / Material 3 Expressive — STRICT REQUIREMENT

For **any** Material 3 or Material 3 Expressive UI/UX task, agents **MUST** treat
`.agents/skills/m3-expressive/SKILL.md` as the authoritative design contract and follow it
strictly before making design, specification, or implementation decisions.

**Mandatory workflow:**

1. **Read `.agents/skills/m3-expressive/SKILL.md` in full before making M3 decisions.**
2. **Route every exact value through the skill references.**
   - Exact numbers, token names, and component names must come from the skill's references.
   - Use `references/tokens.md` for global numeric/token values.
   - Use `references/component-tokens.md` for per-component geometry and measurements.
   - Use the relevant `references/components/*.md` file for component choice, anatomy, placement, spacing, sizing, states, color/type mapping, and do/don't guidance.
   - Read the relevant supporting reference files named by `SKILL.md` before specifying a component or interaction.
3. **Never invent or silently approximate an M3 value.**
   - If a value is not published by the skill, explicitly label it as **My design decision (not in M3)**.
   - Do not present custom spacing, timings, stagger values, motion choreography, or other implementation choices as official M3 tokens.
4. **Preserve the M3 source contract.**
   - Do not substitute plausible numbers, token names, or component behavior from memory.
   - When M3 guidance and project-specific requirements differ, keep the distinction explicit and document the project-specific choice rather than misrepresenting it as M3.
5. **Use expressive tactics purposefully.**
   - Expressive choices must improve hierarchy, attention, usability, or interaction clarity.
   - Do not add expressive shapes, color, typography, containment, or motion merely for decoration.
   - Use hero treatments sparingly and only where the screen's hierarchy benefits from them.
6. **Apply the skill's motion rules.**
   - Spatial springs are for movement, resize, rotation, and shape/corner changes.
   - Effects springs are for color/opacity and must not overshoot.
   - Do not hand-author durations when the skill provides spring tokens.
   - Any choreography or timing not published by M3 must be labeled as a project design decision.
7. **Apply the skill's shape, color, and typography rules.**
   - Preserve the documented nested-radius rule: `inner = outer − padding`.
   - Use semantic M3 color roles instead of fixed accent colors when theme adaptation or contrast matters.
   - Use baseline and emphasized typography together; emphasized type is an intentional exception, not the default scale.
8. **Verify against the references before finishing.**
   - Self-review every M3 UI change for component selection, anatomy, measurements, tokens, states, motion, accessibility, and visual hierarchy.
   - For component/anatomy or measurement work, consult the skill's visual references when applicable.
   - Do not claim Material compliance unless the implementation has been checked against the relevant skill guidance.
9. **Conflict rule:**
   - `SKILL.md` governs Material 3 / M3 Expressive decisions.
   - `REVIEW.md`, `CONTRIBUTING.md`, and this file continue to govern repository architecture, review, branching, and engineering workflow.
   - When project requirements add constraints not specified by M3, apply both and clearly identify the project-specific decision.

This is a mandatory engineering requirement, not an optional design preference.

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

