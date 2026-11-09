# Authentication Architecture

Detailed component architecture for authentication and session management, including Mermaid diagrams for the sign-up, sign-in, OAuth, and passkey flows.

## Component architecture

```mermaid
flowchart TD
    subgraph UI["Presentation layer"]
        L[Login screen]
        S[Sign-up screen]
        O[OAuth browser / deep-link handler]
        PK[Passkey UI prompt]
    end

    subgraph VM["ViewModel layer"]
        AVM[AuthViewModel · StateFlow<AuthUiState>]
    end

    subgraph Domain["Domain layer (framework-free)"]
        AREPO[AuthRepository interface]
        PREPO[PasskeyRepository interface]
        PPM[PasskeyPlatformManager interface]
        ASESS[AuthSessionStatus]
        AUTHERR[AuthError]
    end

    subgraph Data["Data layer"]
        SAR[SupabaseAuthRepository]
        SAS[SupabaseAuthenticationService · IAuthenticationService]
        PRI[PasskeyRepositoryImpl]
        PPMA[AndroidPasskeyPlatformManager · Credential Manager]
        PPMI[IosPasskeyPlatformManager · ASAuthorizationController]
        SCS[SecureStorage · AndroidSecureStorage / IosSecureStorage]
        AEM[AuthErrorMapper]
    end

    subgraph Server["Supabase (GoTrue + passkey routes)"]
        GAUTH[(auth.users / sessions / identities)]
        OAUTH[OAuth authorize · /auth/v1/authorize]
        PKREG[POST /auth/v1/passkeys/registration/options]
        PKVER[POST /auth/v1/passkeys/registration/verify]
        PKAUTH[POST /auth/v1/passkeys/authentication/*]
    end

    UI --> AVM
    AVM --> AREPO
    AVM --> PREPO
    AREPO --> SAR
    AREPO --> SAS
    PREPO --> PRI
    SAR --> GAUTH
    SAR --> OAUTH
    SAS --> GAUTH
    SAR --> SCS
    SAS --> AEM
    PRI --> PKREG
    PRI --> PKVER
    PRI --> PKAUTH
    PRI --> PPMA
    PRI --> PPMI
    PPMA --> PK
    PPMI --> PK
    SCS <--> SAS

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
    style Domain fill:#e3f2fd,stroke:#1565c0
```

## UseCase inventory (auth)

| UseCase | Purpose |
|---------|---------|
| `SignInWithPasskeyUseCase` | Passkey discoverable-credential sign-in: options → platform assert → verify → session import. |
| `AddPasskeyUseCase` | Register a new passkey for the signed-in user. |
| `LoadPasskeysUseCase` | List the user's registered passkeys. |
| `RemovePasskeyUseCase` | Revoke a passkey by id. |

All auth UseCases return `Result<T>`.

## Koin DI wiring (auth)

- `passkeyModule` (`di/PasskeyModule.kt`): declares `expect val passkeyPlatformModule: Module` (platform-specific, registers `PasskeyPlatformManager`), binds `PasskeyRepository` → `PasskeyRepositoryImpl(SupabaseClient.client)`, and the passkey UseCases.
- `secureStorageModule` (`di/SecureStorageModule.kt`): `expect val secureStorageModule: Module` — platform implementations register `SecureStorage` (Android `AndroidSecureStorage`, iOS `IosSecureStorage`).
- `SupabaseClient.client` is a lazy singleton in `core/network/SupabaseClient.kt`, injected into repository implementations.

## Flows

### Sign-up (email/password)

```mermaid
sequenceDiagram
    actor U as User
    participant UI as Sign-up UI
    participant REPO as SupabaseAuthRepository
    participant SUP as Supabase Auth
    participant DB[(users table)]

    U->>UI: email, password, [username]
    UI->>REPO: signUpWithProfile(email, password, username)
    REPO->>SUP: auth.signUpWith(Email)
    SUP->>DB: create auth user + trigger: user_settings, user_presence
    SUP-->>REPO: user
    REPO->>DB: ensureProfileExists → INSERT users(uid, username) if missing
    REPO-->>UI: Result.success(userId)
```

### Sign-in (email/password)

```mermaid
sequenceDiagram
    actor U as User
    participant UI as Login UI
    participant REPO as SupabaseAuthRepository
    participant SUP as Supabase Auth
    participant SCS as SecureStorage

    U->>UI: email, password
    UI->>REPO: signIn(email, password)
    REPO->>SUP: auth.signInWith(Email)
    SUP-->>REPO: session
    REPO-->>UI: Result.success(userId)
    note right of SCS: SupabaseAuthenticationService also persists<br/>access/refresh tokens to SecureStorage
```

### OAuth (Google, Apple, GitHub, …)

```mermaid
sequenceDiagram
    actor U as User
    participant UI as UI
    participant REPO as SupabaseAuthRepository
    participant SUP as Supabase Auth
    participant IDP as Identity Provider

    UI->>REPO: getOAuthUrl(provider, redirectUrl)
    REPO->>SUP: build /auth/v1/authorize?provider=&redirect_to=
    SUP-->>REPO: URL
    REPO-->>UI: URL
    UI->>IDP: open URL (browser)
    IDP-->>UI: redirect_to with code
    UI->>REPO: handleOAuthCallback(code)
    REPO->>SUP: auth.exchangeCodeForSession(code)
    SUP-->>REPO: session
    REPO-->>UI: Result.success(Unit)
```

### Session restore (app launch)

```mermaid
sequenceDiagram
    participant UI as UI (launch)
    participant REPO as SupabaseAuthRepository
    participant SUP as Supabase Auth SDK
    participant SAS as SupabaseAuthenticationService
    participant SCS as SecureStorage

    UI->>REPO: restoreSession()
    REPO->>SUP: currentSessionOrNull()
    alt session present
        SUP-->>REPO: session
    else no session
        REPO->>SAS: restoreSession()
        SAS->>SCS: read access_token / refresh_token
        SAS->>SUP: auth.importAuthToken(access, refresh)
        SUP-->>SAS: session
        SAS-->>REPO: session or null
    end
    REPO-->>UI: Boolean (true if session)
    note over UI, SUP: sessionStatus Flow continues to emit<br/>Authenticated / NotAuthenticated / Initializing
```

## Sign-out

1. `AuthRepository.signOut()` → `SupabaseAuthRepository.signOut()`.
2. `AccountSyncManager.clearAllLocalData(...)` clears messages, conversations, posts, stories, offline actions, sync state.
3. Best-effort `client.auth.signOut()` (10s timeout); network failure does not block sign-out.
4. `client.auth.clearSession()` clears the locally persisted SDK session.

## Security considerations

- Tokens persisted to `SecureStorage`; no-op with warning when storage unavailable.
- OAuth redirect URLs allowlisted (`synapse://login`, `socialsynapse.vercel.app`, `synapsesocial.vercel.app`, `www.synapse.com/auth`).
- `isConfigured()` guard prevents OAuth with placeholder credentials.
- Sign-out clears local data before remote revocation to avoid transient data exposure.
- Two auth implementations coexist (`SupabaseAuthRepository` for the domain `AuthRepository` contract, `SupabaseAuthenticationService` for lower-level token storage); domain code uses `AuthRepository` only.
