# Overview
- [🏗️ Architectural Integrity](#️-architectural-integrity)
- [🎨 UI & Coding Standards](#-ui--coding-standards)
- [📩 Pre-Submission Checklist](#-pre-submission-checklist)
- [🎁 Submission & PR Template](#-submission--pr-template)
- [🚀 CI/CD & GitHub Actions Secrets](#-cicd--github-actions-secrets)
- [🍏 iOS Development](#-ios-development)

---


# 🏗️ Architectural Integrity

We follow a strict **Clean Architecture** pattern in this KMP monorepo. Keep the boundaries sharp! 🔪

### 🧱 Non-Negotiable Rules
- **NO Direct Backend SDKs in Domain** 🚫 → Use `Repository` interfaces.
- **NO Backend Types in Domain** 🚫 → Use **DTOs** (Data) & **Domain Models** (Business) with Mappers.
- **NO Hardcoded Backend Assumptions** 🚫 → Abstract via `DataSource` (e.g., `SupabaseDataSource`).
- **NO Android-only Room** 🚫 → Use **SQLDelight** or **Room KMP**.
- **NO Platform Leaks** 🚫 → No `android.*` or `java.*` in `commonMain`.
- **NO Business Logic in UI/ViewModels** 🚫 → Delegate to **UseCases**.
- **NO Mutable State in Composables** 🚫 → Use `StateFlow`.
- **NO God ViewModels** 🚫 → One ViewModel per feature/screen.

### 📐 Layer Boundaries
```mermaid
graph TD
    subgraph UI ["UI Layer (app/)"]
        UI_Nodes[ViewModels + Composables]
        UI_Note[NO business logic]
    end

    subgraph Domain ["Domain Layer (shared/domain/)"]
        Domain_Nodes[UseCases + Models + Interfaces]
        Domain_Note[Pure Kotlin, NO backend SDKs]
    end

    subgraph Data ["Data Layer (shared/data/)"]
        Data_Nodes[Repo Impls + DataSources + DTOs]
        Backend[Backend SDKs]
    end

    UI_Nodes -->|StateFlow| Domain_Nodes
    Data_Nodes -->|Implements| Domain_Nodes
    Data_Nodes --> Backend
```

---

# 🎨 UI & Coding Standards

Don't be a "hardcoder"! Keep our UI flexible and themeable! 🌈

- **NO Hardcoded Colors** 🖌️
  - Use `MaterialTheme.colorScheme`.
  - Reference: `com.synapse.social.studioasinc.feature.shared.theme`.
- **NO Hardcoded Dimensions/Spacing** 📏
  - Use the project's `Spacing` tokens.
  - Reference: `com.synapse.social.studioasinc.feature.shared.theme.Spacing`.
- **NO Hardcoded Text** ✍️
  - Always use `strings.xml` resources.
  - Path: `app/src/main/res/values/strings.xml`.

---

# 📩 Pre-commit steps
1. ✅ **Build MUST Pass**: No submission without a successful build.
2. 🔍 **Code Review**: Self-review or peer-review required.
3. 🧹 **No Cache Files**: Verify with `git status`.
4. 🚫 **Meaningful Commits**: No empty or "fixed stuff" commits.

---

# 🎁 Submission & PR Template

ALWAYS include a **PRESENT** for the user! 🎁

### 🧾 Pull Request Template

```md
**Title:** `[emoji] [type]: [concise summary]`

### 📝 Description
- **💡 What:** [Changes made]
- **🎯 Why:** [Motivation/Problem solved]

### ✅ Build Status
The build passed/failed/(N/A)
```

---

# 🚀 CI/CD & GitHub Actions Secrets

To ensure the Android CI/CD (`android.yml`) works properly, the following secrets and variables must be configured in GitHub Actions:

### 🔑 Required Secrets
| Secret Name | Purpose |
| :--- | :--- |
| `SUPABASE_SYNAPSE_URL` | Supabase API endpoint URL |
| `SUPABASE_SYNAPSE_ANON_KEY` | Supabase anonymous API key |
| `SUPABASE_SYNAPSE_S3_ENDPOINT_URL` | S3 endpoint for file storage |
| `SUPABASE_SYNAPSE_S3_ENDPOINT_REGION` | AWS/S3 region for storage |
| `SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID` | Storage access key ID |
| `SUPABASE_SYNAPSE_S3_ACCESS_KEY` | Storage secret access key |
| `GEMINI_API_KEY` | API key for AI feature integration |
| `IMGBB_API_KEY` | API key for image uploads (ImgBB) |
| `GOOGLE_WEB_CLIENT_ID` | OAuth Web Client ID for Google Login |
| `TELEGRAM_API_ID` | Telegram API ID for deployment bot |
| `TELEGRAM_API_HASH` | Telegram API Hash for deployment bot |
| `TELEGRAM_BOT_TOKEN` | Token for the Telegram bot used for delivery |
| `TELEGRAM_CHAT_ID` | Group ID where APKs and logs are sent |
| `TELEGRAM_TOPIC_ID` | (Optional) Thread ID within the Telegram group |

### ⚙️ Variables
- `IS_USING_HOSTED_RUNNERS`: Set to `true` to use `self-hosted` runners; otherwise, it defaults to `ubuntu-latest`.

---

# 🍏 iOS Development
- **Crypto:** Use `CoreCrypto` (C API) via Kotlin/Native cinterop. Avoid `CryptoKit` bridges.
- **Safety:** Handle empty byte arrays safely to prevent crashes.
