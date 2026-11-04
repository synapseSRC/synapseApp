# Project Structure

Kotlin Multiplatform module layout, source sets, and platform applications.

## Repository layout

```
synapseSRC/SynapseApp/
├── shared/              # KMP shared module (Domain + Data + DI)
│   └── src/
│       ├── commonMain/  # Domain, Data, DI, models, theme (framework-free common code)
│       ├── androidMain/ # Android: libsignal, EncryptedSharedPreferences, Credential Manager
│       ├── iosMain/     # iOS: Keychain, ASAuthorizationController, iOS audio
│       ├── jvmMain/     # Desktop (JVM)
│       ├── wasmJsMain/  # Web (shared)
│       └── nativeInterop/
│           └── sqldelight/   # SQLDelight schema (.sq tables, .sqm migrations)
├── app/                 # Android app (100% Jetpack Compose, Hilt)
├── iosApp/              # iOS app (100% SwiftUI)
├── desktop/             # Compose Desktop
├── web/                 # Next.js web front-end
├── supabase/            # Supabase config / migrations / edge functions
├── docs/                # This documentation
├── build.gradle, settings.gradle
└── gradle.properties    # SUPABASE_URL, SUPABASE_ANON_KEY, API keys
```

## Source-set responsibilities

| Source set | Contents |
|------------|----------|
| `shared/src/commonMain` | Domain models, UseCases, repository interfaces, repository implementations, data sources, DTOs, mappers, DI modules (Koin), crypto interfaces, SQLDelight schema. Framework-free. |
| `shared/src/androidMain` | `AndroidSignalProtocolManager`, `AndroidSignalStore` (EncryptedSharedPreferences), `AndroidSecureStorage`, `AndroidPasskeyPlatformManager` (Credential Manager), `AndroidMediaCompressor`, platform dispatchers. |
| `shared/src/iosMain` | `IosSecureStorage` (Keychain), `IosPasskeyPlatformManager` (ASAuthorizationController), `IosMediaCompressor`, platform dispatchers, `KoinHelper`. |
| `shared/src/jvmMain` | Desktop JVM support. |
| `shared/src/wasmJsMain` | Web target shared code. |
| `app/` | Android Compose UI, ViewModels (Hilt), `google-services.json`. |
| `iosApp/` | SwiftUI UI, ViewModels (`ObservableObject`, `@MainActor`). |
| `web/` | Next.js front-end. |

## Key packages (`shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/`)

| Package | Purpose |
|---------|---------|
| `domain/model/` | Pure domain data classes (Message, Conversation, User, Passkey, StorageConfig, ...). |
| `domain/usecase/` | UseCases grouped by feature (auth, chat, ai, settings, sound, story, ...). |
| `domain/repository/` | Repository interfaces (ChatRepository, AuthRepository, PasskeyRepository, ...). |
| `domain/service/` | Domain services (AudioRecorder, EchoAudioEngine, MediaCompressor, SoundPlayer). |
| `data/repository/` | Repository implementations (SupabaseChatRepository, SupabaseAuthRepository, SignalRepository, ChatEncryptionHelper, ...). |
| `data/datasource/` | Data sources (SupabaseChatDataSource, ChatKeyDataSource, ChatRealtimeDataSource, ...). |
| `data/crypto/` | Signal Protocol: `SignalProtocolManager`, models (`SignalBundle.kt`). |
| `data/crypto/store/` | Platform crypto stores (Android: `AndroidSignalStore`). |
| `data/dto/` | DTOs mirroring external schemas. |
| `data/mapper/` | DTO ↔ domain mappers (ChatMapper, AuthErrorMapper, PasskeyMapper). |
| `data/local/database/` | SQLDelight DAOs (CachedMessageDao, CachedConversationDao, ...). |
| `data/sync/` | SyncManager, MessageEntitySyncer, SyncModels, NetworkMonitor, AccountSyncManager. |
| `data/source/remote/` | Upload services (SupabaseUploadService, ImgBBUploadService, ...). |
| `data/auth/` | SupabaseAuthenticationService, IAuthenticationService. |
| `di/` | Koin modules (PasskeyModule, SecureStorageModule, StorageModule, AiModule, SyncModule, ...). |
| `core/network/` | SupabaseClient singleton. |
| `core/config/` | SynapseConfig (expect object with API keys/URLs). |
| `core/util/` | Logger, TimeProvider, UUIDUtils, AppDispatchers, TimestampFormatter. |
| `domain/util/` | EncryptedPayloadDetector. |

## Platform apps

| App | Framework | DI |
|-----|-----------|-----|
| `:app` (Android) | 100% Jetpack Compose (no XML) | Hilt |
| `:iosApp` (iOS) | 100% SwiftUI | Uses shared framework directly |
| `desktop` | Compose Desktop | — |
| `web` | Next.js | — |

## Related

- Dependency injection: [`./dependency-injection.md`](./dependency-injection.md)
- Feature docs: [`../README.md`](../README.md)
