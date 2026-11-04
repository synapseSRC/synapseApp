# Authentication

Authentication, session management, OAuth, and passkeys (WebAuthn) in Synapse Social. Authentication is backed by **Supabase Auth** (GoTrue) via the `supabase-kt` SDK, with passkey ceremonies issued through Supabase's native passkey Edge Function routes called over a Ktor HTTP client.

## Authentication document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | Auth component architecture, flows, Mermaid diagrams, trust boundaries. |
| Passkeys | `./passkeys.md` | WebAuthn passkey registration, authentication, platform managers, backend endpoints. |

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Auth provider | Supabase Auth (GoTrue) via `io.github.jan.supabase:supabase-kt` | Email/password sign-in/sign-up, OAuth, session tokens, OAuth callbacks, session refresh. |
| Domain interface | `AuthRepository` | Provider-neutral auth contract (`Result<T>` returns, `sessionStatus` flow). |
| Auth implementation | `SupabaseAuthRepository` | Maps Supabase `SessionStatus` → `AuthSessionStatus`; orchestrates sign-up with profile creation, sign-in, sign-out with local data clearing. |
| Auth service | `SupabaseAuthenticationService` (`IAuthenticationService`) | Lower-level service that also persists access/refresh tokens to `SecureStorage` and restores sessions from storage. |
| Session status flow | `client.auth.sessionStatus` | Emits `Authenticated` / `NotAuthenticated` / `Initializing`. |
| Secure storage | Platform `SecureStorage` (`AndroidSecureStorage` / `IosSecureStorage`) | Access/refresh token persistence for session restore. |
| Social providers | Google, Apple, GitHub, Discord, Twitter, Facebook, Spotify, Slack | OAuth via Supabase built-in providers; Google also supports direct ID-token sign-in. |
| Profile sync | `users` table (PostgREST) | User profile created on sign-up (auto `user_settings` / `user_presence` via DB trigger `handle_new_auth_user`). |
| Passkeys | Supabase passkey routes `/auth/v1/passkeys/*` over Ktor | WebAuthn registration/authentication ceremonies. |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/AuthRepository.kt` — domain auth interface.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/SupabaseAuthRepository.kt` — Supabase-backed auth repository implementation.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/auth/SupabaseAuthenticationService.kt` — auth service with secure-storage token persistence.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/auth/IAuthenticationService.kt` — auth service interface.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/core/network/SupabaseClient.kt` — configured `SupabaseClient` singleton (Auth, PostgREST, Realtime, Storage, Functions).
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/core/config/SynapseConfig.kt` — `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_REDIRECT_URL`, provider keys.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/model/AuthDtos.kt` — auth DTOs.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/mapper/AuthErrorMapper.kt` — Supabase exception → `AuthError` mapping.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/auth/AuthError.kt` — domain auth error model.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/auth/AuthSessionStatus.kt` — session status enum.

## Related feature documentation

- [Passkeys](./passkeys.md) — WebAuthn passkey registration and sign-in.
- [E2EE](../E2EE/README.md) — E2EE key initialization depends on an authenticated user (`currentUserId`).
- [Database](../Database/README.md) — local data cleared on sign-out via `AccountSyncManager`.

## Authentication architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI1[Login / Sign-up screens]
        VM[Auth ViewModel]
    end

    subgraph Domain["Domain (framework-free)"]
        AREPO[AuthRepository interface]
        ASESS[AuthSessionStatus flow]
    end

    subgraph Data["Data layer"]
        SAR[SupabaseAuthRepository]
        SAS[SupabaseAuthenticationService · IAuthenticationService]
        SCS[SecureStorage · AndroidSecureStorage / IosSecureStorage]
        AEM[AuthErrorMapper]
    end

    subgraph Server["Supabase Auth (GoTrue)"]
        AUTH[(auth.users · sessions · identities)]
        OAUTH[OAuth providers · Google / Apple / ...]
        PASSKEY[/auth/v1/passkeys/* Edge Function routes]
    end

    UI1 --> VM
    VM --> AREPO
    AREPO --> SAR
    AREPO --> ASESS
    SAR --> SAS
    SAR --> SCS
    SAR --> AEM
    SAS --> SCS
    SAR --> AUTH
    SAS --> AUTH
    AUTH --> OAUTH
    SAR -.-> PASSKEY

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
```

### Component responsibilities

| Component | Responsibility |
|-----------|----------------|
| `AuthRepository` | Domain contract: sign-up/sign-in/sign-out, OAuth, session status flow, profile ensure, account delete. Returns `Result<T>`. |
| `SupabaseAuthRepository` | Implementation: Supabase `signUpWith`/`signInWith`, `sessionStatus` mapping, sign-out that clears local data (`AccountSyncManager.clearAllLocalData`) then revokes server session, profile creation in `users` table. |
| `SupabaseAuthenticationService` | Lower-level service: stores/clears access/refresh tokens in `SecureStorage`, session restore from storage, redirect-URL allowlisting for OAuth. |
| `SecureStorage` | Platform-encrypted key-value storage (Android `EncryptedSharedPreferences`, iOS Keychain). |
| `AuthErrorMapper` | Maps Supabase/Ktor exceptions to domain `AuthError`. |
| `SupabaseClient` | Configured supabase-kt client singleton (lazy), with `SettingsSessionManager` / `MemorySessionManager` fallback. |

## Authentication flow

```mermaid
sequenceDiagram
    actor U as User
    participant UI as Login / Sign-up UI
    participant VM as Auth ViewModel
    participant REPO as AuthRepository (SupabaseAuthRepository)
    participant SUP as Supabase Auth (GoTrue)
    participant DB[(users table)]
    participant SCS[SecureStorage]

    note over U, DB: Email / password sign-up
    U->>UI: email + password
    UI->>VM: signUp(email, password)
    VM->>REPO: signUp(email, password)
    REPO->>SUP: auth.signUpWith(Email) {email, password, data}
    SUP->>DB: insert auth.user + trigger create profile/settings/presence
    SUP-->>REPO: user (session created)
    REPO->>DB: ensureProfileExists → INSERT users(uid, username) if missing
    REPO-->>VM: Result.success(userId)
    VM-->>UI: navigate to home

    note over U, DB: Email / password sign-in
    U->>UI: email + password
    UI->>VM: signIn(email, password)
    VM->>REPO: signIn(email, password)
    REPO->>SUP: auth.signInWith(Email)
    SUP-->>REPO: session
    REPO-->>VM: Result.success(userId)
    VM-->>UI: session established

    note over U, SUP: OAuth flow (Google, Apple, etc.)
    UI->>VM: getOAuthUrl(provider, redirectUrl)
    VM->>REPO: getOAuthUrl(...)
    REPO->>SUP: build /auth/v1/authorize?provider=...&redirect_to=...
    SUP-->>REPO: OAuth URL
    REPO-->>VM-->>UI: open URL in browser
    UI->>SUP: user completes provider auth → redirect back
    UI->>VM: handleOAuthCallback(code)
    VM->>REPO: handleOAuthCallback(code)
    REPO->>SUP: auth.exchangeCodeForSession(code)
    SUP-->>REPO: session
    REPO-->>VM: Result.success(Unit)

    note over U, SCS: Session restore (app launch)
    VM->>REPO: restoreSession()
    REPO->>SUP: currentSessionOrNull()
    alt no session in SDK
        REPO->>SAS: restore session from SecureStorage
        SAS->>SCS: read access_token / refresh_token
        SAS->>SUP: auth.importAuthToken(access, refresh)
        SUP-->>SAS: session
    end
    SUP-->>REPO: session or null
    REPO-->>VM: Boolean / sessionStatus flow
```

## Session management

- Session state is exposed via `AuthRepository.sessionStatus` (`Flow<AuthSessionStatus>`), mapped from Supabase `SessionStatus` (`Authenticated` / `NotAuthenticated` / `Initializing`).
- `restoreSession()` returns true when `currentSessionOrNull() != null`; `SupabaseAuthenticationService` additionally restores from `SecureStorage` by importing access/refresh tokens.
- Tokens (`access_token`, `refresh_token`, `user_id`, `user_email`, `expires_in`) are persisted to platform `SecureStorage` after sign-in, sign-up, refresh, and OAuth callback; cleared on sign-out.
- `refreshSession()` calls `client.auth.refreshCurrentSession()` and re-stores tokens.
- Sign-out: clears all local account-scoped data via `AccountSyncManager.clearAllLocalData()` (messages, conversations, posts, stories, offline actions, sync state), then best-effort server revoke (`signOut()` with 10s timeout), then `clearSession()` locally.

## OAuth providers

Supported social providers (mapped in `SupabaseAuthRepository.mapSocialProviderToOAuthProvider`): Google, Apple, GitHub, Discord, Twitter, Facebook, Spotify, Slack. Google additionally supports `signInWithGoogleIdToken` via Supabase `IDToken` provider.

Redirect URLs are validated against an allowlist in `SupabaseAuthenticationService`:
- `synapse://login`
- `https://socialsynapse.vercel.app`
- `https://synapsesocial.vercel.app`
- `https://www.synapse.com/auth`

`SupabaseClient.isConfigured()` guards OAuth URL generation; without `SUPABASE_URL` / `SUPABASE_ANON_KEY` a placeholder client is used and OAuth fails with a configuration error.

## Passkeys

See [`./passkeys.md`](./passkeys.md) for WebAuthn registration, authentication, platform managers (Android Credential Manager, iOS ASAuthorizationController), and the Supabase passkey route contract.

## Security considerations

- Session tokens are persisted to platform encrypted storage; `SecureStorage` is optional and token operations are no-ops when unavailable (logged as warnings).
- Sign-out clears local data **before** server revocation so no account data is briefly visible after sign-out.
- OAuth redirect URLs are allowlisted; arbitrary redirect targets are rejected.
- Auth error messages are logged with exception class/message only (`logSafeError`) to avoid leaking sensitive details.
- `SupabaseAuthRepository` and `SupabaseAuthenticationService` are two partially overlapping auth implementations; both call Supabase Auth directly. Domain code depends on `AuthRepository`.

## Source-code paths

- Domain: `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/AuthRepository.kt`
- Data: `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/SupabaseAuthRepository.kt`
- Service: `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/auth/SupabaseAuthenticationService.kt`
- Client config: `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/core/network/SupabaseClient.kt`
- App config: `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/core/config/SynapseConfig.kt`
