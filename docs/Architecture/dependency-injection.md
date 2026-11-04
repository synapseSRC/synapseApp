# Dependency Injection

Dependency injection in Synapse Social: Koin in the shared module, Hilt on Android, and direct framework consumption on iOS.

## DI strategy

| Platform | DI container | Scope |
|----------|-------------|-------|
| Shared (KMP) | Koin | Repositories, UseCases, data sources, crypto, sync, media, AI |
| Android (`:app`) | Hilt | Android app graph (ViewModels, platform bindings) |
| iOS (`:iosApp`) | Direct / manual | Consumes shared framework UseCases directly |

**Strict DI rule:** All backend-specific data sources and adapters are injected via DI. Concrete backend implementations (Supabase SDK, libsignal, secure storage) are never instantiated outside the data layer. Platform apps consume feature UseCases or domain interfaces exclusively.

## Shared Koin modules

| Module file | Purpose |
|-------------|---------|
| `di/PasskeyModule.kt` | `expect val passkeyPlatformModule: Module`; binds `PasskeyRepository` → `PasskeyRepositoryImpl(SupabaseClient.client)`; passkey UseCases. |
| `di/SecureStorageModule.kt` | `expect val secureStorageModule: Module` — platform implementations register `SecureStorage`. |
| `di/StorageModule.kt` | `expect val storageDriverModule`, `expect val fileUploaderModule`; `StorageDatabase` (SQLDelight), DAOs (`CachedMessageDao`, `CachedConversationDao`, `MessageReactionDao`, `PendingActionDao`, `StoryDao`), upload services (Cloudinary, ImgBB, Supabase, R2), `MediaUploadRepository`, storage UseCases. |
| `di/AiModule.kt` | `HttpClient`, `AiRepository` → `GeminiAiRepository`, `GenerateSmartRepliesUseCase`. |
| `di/SyncModule.kt` | `SyncStateStorage` (InMemory), `SyncManager`, `MessageEntitySyncer` registration, `NetworkMonitor`. |
| `di/DeviceModule.kt` | Device-related bindings. |
| `di/PresenceModule.kt` | Presence bindings. |
| `di/CommentModule.kt` | Comment bindings. |

### Example: passkey module

```kotlin
// Platform implementations register PasskeyPlatformManager here.
expect val passkeyPlatformModule: Module

val passkeyModule = module {
    includes(passkeyPlatformModule)

    single<PasskeyRepository> { PasskeyRepositoryImpl(SupabaseClient.client) }
    single { LoadPasskeysUseCase(get()) }
    single { RemovePasskeyUseCase(get()) }
    single { AddPasskeyUseCase(get(), get()) }
    single { SignInWithPasskeyUseCase(get(), get(), get()) }
}
```

### expect/actual platform bindings

| expect declaration | Android actual | iOS actual |
|--------------------|---------------|------------|
| `secureStorageModule` | `SecureStorageModule.android.kt` → `AndroidSecureStorage` | `SecureStorageModule.ios.kt` → `IosSecureStorage` |
| `passkeyPlatformModule` | `PasskeyPlatformModule.android.kt` → `AndroidPasskeyPlatformManager` | `PasskeyPlatformModule.ios.kt` → `IosPasskeyPlatformManager` |
| `storageDriverModule` | `StorageDriverModule.android.kt` | `StorageDriverModule.ios.kt` |
| `fileUploaderModule` | `FileUploaderModule.android.kt` | `FileUploaderModule.ios.kt` |

`SupabaseClient.client` (`core/network/SupabaseClient.kt`) is a lazy singleton injected into repository implementations.

## Android Hilt

Android `:app` uses Hilt for ViewModel and platform binding injection. UI components render `StateFlow<UiState>` and forward events to ViewModels only.

## iOS

iOS `:iosApp` uses SwiftUI `ObservableObject` / `@Published` / `@MainActor` ViewModels that consume shared UseCases directly from the shared framework. Keychain operations go through `IosSecureStorage`.

## DI rules (enforced)

1. No direct instantiation of `SupabaseClient`, `AndroidSignalProtocolManager`, `AndroidSignalStore`, or platform SDKs outside the data layer.
2. Repository implementations are injected (Koin `single`/`factory`), not constructed in UseCases or UI.
3. Domain layer imports no DI container, no Supabase, no libsignal.
4. `expect`/`actual` keeps platform-specific implementations out of `commonMain`.

## Related

- Project structure: [`./project-structure.md`](./project-structure.md)
- Feature docs: [`../README.md`](../README.md)
