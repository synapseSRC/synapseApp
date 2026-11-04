# E2EE Architecture

Complete component architecture for Signal Protocol end-to-end encryption in Synapse Social, including Mermaid diagrams, component responsibilities, and client/server trust boundaries.

## Component architecture

```mermaid
flowchart TD
    subgraph UI["Presentation layer"]
        UI1[Compose UI / SwiftUI]
        VM[ViewModel · StateFlow]
    end

    subgraph Domain["Domain layer (framework-free)"]
        SEND[SendMessageUseCase]
        INIT[InitializeE2EUseCase]
        REPO[ChatRepository interface]
        EPD[EncryptedPayloadDetector]
    end

    subgraph Data["Data layer (shared/commonMain)"]
        CH[ChatEncryptionHelper · internal]
        GMC[GroupMetadataCipher · internal]
        SR[SignalRepository]
        DS[SupabaseChatDataSource / ChatKeyDataSource]
    end

    subgraph Crypto["Cryptography (platformMain)"]
        SPM[SignalProtocolManager interface]
        ASPM[AndroidSignalProtocolManager · libsignal-android]
        IOSPM[iOS placeholder · not implemented]
        SS[AndroidSignalStore · EncryptedSharedPreferences]
    end

    subgraph Server["Supabase server (untrusted for plaintext)"]
        UPK[(user_public_keys)]
        SIK[(signal_identity_keys)]
        COPK[claim_one_time_pre_key RPC]
        MSGT[(messages table · ciphertext)]
        RT[Realtime · WebSocket]
    end

    UI1 --> VM
    VM --> SEND
    VM --> INIT
    SEND --> REPO
    INIT --> REPO
    REPO --> CH
    REPO --> GMC
    CH --> SPM
    CH --> DS
    CH --> SR
    CH --> EPD
    GMC --> SPM
    GMC --> DS
    SPM --> ASPM
    ASPM --> SS
    DS --> UPK
    DS --> SIK
    DS --> COPK
    DS --> MSGT
    DS --> RT
    RT -.-> UI1
```

### Responsibilities

| Component | Layer | Responsibility |
|-----------|-------|----------------|
| `SendMessageUseCase` | Domain | Orchestrates outgoing E2EE: serializes plaintext JSON, ensures sessions, fan-out encrypts per recipient, encrypts for sender (multi-device), delegates persistence to `ChatRepository`. |
| `InitializeE2EUseCase` | Domain | Thin wrapper delegating to `ChatRepository.initializeE2EE()`. |
| `ChatRepository` | Domain (interface) | Contract for `initializeE2EE()`, `ensureSession(userId)`, and message send/receive. |
| `EncryptedPayloadDetector` | Domain (util) | Classifies message content as placeholder, single-cipher envelope, or multi-recipient encrypted envelope. |
| `ChatEncryptionHelper` | Data (internal) | Central E2EE orchestration: session lifecycle, decryption with two-tier cache (memory + SQLDelight), key initialization, identity mismatch handling, session-reset retry. |
| `GroupMetadataCipher` | Data (internal) | Fan-out encrypts group name/description/avatar per participant via 1:1 Signal sessions; graceful fallback to plaintext columns. |
| `SignalRepository` | Data | Fetches recipient pre-key bundles from Supabase (`signal_identity_keys`) and claims one-time pre-keys via `claim_one_time_pre_key` RPC. |
| `SupabaseChatDataSource` / `ChatKeyDataSource` | Data | Remote access to `user_public_keys` table (upload/get public key bundle). |
| `SignalProtocolManager` | Data (interface) | Platform-neutral contract: key generation, session establishment, encrypt/decrypt, identity management. |
| `AndroidSignalProtocolManager` | Data (androidMain) | libsignal-android implementation: X3DH, Double Ratchet, pre-key store delegation to `AndroidSignalStore`. |
| `AndroidSignalStore` | Data (androidMain) | `SignalProtocolStore` backed by `EncryptedSharedPreferences`; persists identity, pre-keys, sessions at rest. |

## Client/server trust boundaries

```mermaid
flowchart LR
    subgraph ClientTrusted["Client — TRUSTED (plaintext + private keys)"]
        UI[UI / ViewModel]
        UM[SendMessageUseCase]
        CH[ChatEncryptionHelper]
        ASPM[AndroidSignalProtocolManager]
        SS[AndroidSignalStore · EncryptedSharedPreferences]
        CACHE[CachedMessage SQLDelight · decrypted cache]
    end

    subgraph Network["Network — untrusted"]
        TLS[TLS to Supabase]
    end

    subgraph ServerUntrusted["Supabase server — UNTRUSTED (ciphertext only)"]
        UPK[(user_public_keys · public keys only)]
        SIK[(signal_identity_keys · public keys only)]
        MSGT[(messages · ciphertext blobs)]
        RT[Realtime · ciphertext delivery]
    end

    ClientTrusted -->|ciphertext| TLS
    TLS --> ServerUntrusted
    ServerUntrusted -->|ciphertext| TLS
    TLS --> ClientTrusted

    ASPM --> SS
    CH --> CACHE
    CH -.->|never sends| ServerUntrusted
    ASPM -.->|never sends| ServerUntrusted

    style ClientTrusted fill:#e8f5e9,stroke:#2e7d32
    style ServerUntrusted fill:#ffebee,stroke:#c62828
    style Network fill:#fff3e0,stroke:#ef6c00
```

### Trust boundary rules (enforced in code)

1. **Private keys never leave the device.** Identity key pairs and pre-keys are stored only in `AndroidSignalStore` (`EncryptedSharedPreferences`, Keystore-backed master key). `AndroidSignalProtocolManager` never transmits private key material.
2. **Supabase sees only public keys and ciphertext.** The `user_public_keys` and `signal_identity_keys` tables hold public key material. The `messages` table holds encrypted payloads (a map of `userId → EncryptedMessage` plus a sender plaintext copy).
3. **The server cannot decrypt.** Decryption requires the local Double Ratchet session state held in `AndroidSignalStore`. The server never receives session keys.
4. **Domain layer is crypto-agnostic.** UseCases and repository interfaces import no Signal/Supabase types; all crypto is encapsulated in the Data layer.

## Mermaid diagrams

### A. Complete E2EE architecture

The diagram above (component architecture) and the trust-boundary diagram together describe the complete E2EE architecture: client-side cryptographic components, key generation, identity/signed pre-key/one-time pre-key management, public-key publication to Supabase, session establishment, per-recipient encryption, message storage, realtime delivery, decryption, and local caching.

### B. Key initialization sequence

```mermaid
sequenceDiagram
    actor U as User / App
    participant UM as InitializeE2EUseCase
    participant CH as ChatEncryptionHelper
    participant SPM as SignalProtocolManager
    participant DS as ChatKeyDataSource (Supabase)
    participant UPK[(user_public_keys table)]

    U->>UM: invoke() — initialize E2EE
    UM->>CH: initializeE2EE(currentUserId)
    CH->>SPM: hasIdentity()
    alt Local keys exist
        SPM-->>CH: true
        CH->>DS: getUserPublicKey(currentUserId)
        DS->>UPK: SELECT * FROM user_public_keys WHERE user_id = ?
        UPK-->>DS: UserPublicKeyDto or null
        DS-->>CH: remoteKeyDto
        alt Remote exists AND identity matches local
            CH->>CH: Fully initialized - check rotation (30 days)
        else Local keys exist, remote missing OR identity mismatch
            alt Identity mismatch
                CH->>SPM: deleteAllSessions()
                SPM-->>CH: sessions cleared
            end
            CH->>SPM: buildBundleFromLocalKeys() → generates fresh pre-keys
            CH->>DS: uploadUserPublicKey(bundleStr)
            DS->>UPK: UPSERT user_public_keys
            UPK-->>DS: ok
            DS-->>CH: ok
            CH-->>UM: Result.success
        end
    else No local keys
        SPM-->>CH: false
        CH->>SPM: generateIdentityAndKeys()
        SPM-->>CH: SignalIdentityKeys
        CH->>SPM: generateOneTimePreKeys(startId=1, count=100)
        SPM-->>CH: 100 one-time pre-keys
        CH->>CH: build PreKeyBundle (registrationId, deviceId=1, signedPreKey, identityKey)
        CH->>DS: uploadUserPublicKey(bundleStr)
        DS->>UPK: UPSERT user_public_keys
        UPK-->>DS: ok
        DS-->>CH: ok
        CH-->>UM: Result.success
    end
    UM-->>U: Result<Unit>

    note over CH, UPK: Identity mismatch = remote identityKey != local identityKey<br/>→ clears all local sessions, re-uploads new bundle.<br/>Remote keys exist but local missing → full regeneration.
```

Key initialization scenarios handled (from `initializeE2EE`):

| Scenario | Action |
|----------|--------|
| Local + remote keys, identities match | Already initialized; log key-rotation recommendation if keys older than 30 days. |
| Local keys, remote missing | Re-upload bundle built from local keys (fresh one-time pre-keys). |
| Local keys, remote present, identity mismatch | `deleteAllSessions()`, then re-upload bundle (overwrites remote keys). |
| No local keys (fresh install) | Generate identity + 100 one-time pre-keys, upload bundle. |
| Remote keys exist but local missing | Warn and regenerate everything locally. |

### C. Outgoing message encryption

```mermaid
sequenceDiagram
    actor U as User (compose)
    participant VM as ViewModel
    participant SEND as SendMessageUseCase.invoke()
    participant REPO as ChatRepository
    participant CH as ChatEncryptionHelper
    participant SPM as SignalProtocolManager
    participant SS[AndroidSignalStore]
    participant DS[SupabaseChatDataSource]
    participant MSGT[(messages table)]
    participant RT[Realtime WebSocket]

    U->>VM: sendMessage(chatId, content, ...)
    VM->>SEND: invoke(...)
    SEND->>REPO: getParticipantIds(chatId)
    REPO-->>SEND: [participantIds]
    SEND->>SEND: build plaintext JSON payload {content, mediaUrl, metadata, attachments}
    SEND->>SEND: contentBytes = JSON.encodeToByteArray()
    loop each recipient (concurrent async)
        SEND->>REPO: ensureSession(userId)
        REPO->>CH: ensureSession(userId)
        CH->>SPM: hasSession(userId)?
        alt no session
            CH->>DS: getUserPublicKey(userId)
            DS->>UPK: SELECT user_public_keys
            UPK-->>DS: bundle
            DS-->>CH: UserPublicKeyDto
            CH->>SPM: processPreKeyBundle(userId, bundle)
            SPM-->>CH: session established
        end
        CH-->>REPO: session ready
        REPO-->>SEND: ok
        SEND->>SPM: encryptMessage(userId, contentBytes)
        SPM->>SS: SessionCipher.encrypt → CiphertextMessage
        SPM-->>SEND: EncryptedMessage {type, body, registrationId}
    end
    SEND->>SEND: encrypt for sender (currentUserId) if not present — multi-device
    SEND->>SEND: encryptedPayload = JsonObject {userId → EncryptedMessage}
    SEND->>REPO: sendMessage(content = encryptedPayload, senderPlaintext = jsonPayload)
    REPO->>DS: sendMessage(...) → INSERT messages row
    DS-->>REPO: MessageDto
    REPO-->>SEND: Result<Message>
    SEND-->>VM: Result<Message>
    VM-->>U: UI updated
    REPO->>RT: broadcast new message (ciphertext) to participants
    RT-->>Participants: realtime delivery

    note over SEND, SPM: Per-recipient ciphertext: each recipient gets an independently<br/>decryptable EncryptedMessage envelope. Sender also encrypted so their other<br/>devices can decrypt (multi-device parity). If ANY recipient encryption fails,<br/>the whole message is aborted (Result.failure) — no partial send.
```

### D. Incoming message decryption

```mermaid
sequenceDiagram
    actor UI as UI / ViewModel (realtime or cache read)
    participant REPO as ChatRepository / SupabaseChatRepository
    participant CH as ChatEncryptionHelper (MessageDto.decryptIfNecessary)
    participant MC[In-memory decryptedMessageCache]
    participant DB[(CachedMessage SQLDelight)]
    participant EPD[EncryptedPayloadDetector]
    participant SPM as SignalProtocolManager
    participant SS[AndroidSignalStore]

    UI->>REPO: getMessageById / subscribeToMessages / getMessages
    REPO->>CH: decryptIfNecessary(currentUserId)
    CH->>MC: cache hit? (messageId)
    alt memory cache hit
        MC-->>CH: plaintext
        CH-->>REPO: MessageDto with decrypted content
    else memory miss
        CH->>DB: getMessageById(messageId)
        alt DB cached plaintext & not encrypted/placeholder
            DB-->>CH: content
            CH->>MC: cacheDecrypted(messageId, content)
            CH-->>REPO: MessageDto
        else DB miss or undecryptable
            CH->>EPD: isEncryptedPayload(content)?
            alt not encrypted payload
                CH-->>REPO: MessageDto (plaintext, encryptionFailureReason = null)
            else encrypted multi-recipient envelope
                CH->>EPD: extract my entry [currentUserId]
                alt own entry is sender plaintext copy
                    CH-->>CH: read plaintext directly, cache, update DB
                    CH-->>REPO: MessageDto
                else own entry is EncryptedMessage cipher envelope
                    CH->>SPM: hasSession(senderId)?
                    alt no session
                        CH->>CH: ensureSession(senderId) — fetch bundle from DS, process
                    end
                    CH->>SPM: decryptMessage(senderId, myPayload)
                    SPM->>SS: SessionCipher.decrypt → plaintext bytes
                    SPM-->>CH: decrypted bytes
                    alt decryption fails
                        CH->>SPM: deleteSession(senderId)
                        CH->>SPM: ensureSession(senderId) — rebuild X3DH session
                        CH->>SPM: decryptMessage(senderId, myPayload) [retry]
                        alt retry succeeds
                            CH->>MC: cacheDecrypted
                            CH->>DB: updateContent(messageId, content)
                            CH-->>REPO: MessageDto (decrypted)
                        else retry fails
                            CH-->>REPO: MessageDto (encryptionFailureReason = "decryption_failed")
                        end
                    end
                    CH->>MC: cacheDecrypted(messageId, decryptedContent)
                    CH->>DB: updateContent(messageId, content)
                    CH-->>REPO: MessageDto (decrypted)
                end
            end
        end
    end
    REPO-->>UI: Message (decrypted or placeholder + failureReason)

    note over CH, SPM: Decryption cache tiers: 1. In-memory decryptedMessageCache (per-message Mutex). 2. SQLDelight CachedMessage (survives restart). One-time PreKey envelopes decrypt once, caching is critical. On decrypt failure: session reset + single retry, permanent failure results in decryption_failed.
```

### E. Key management architecture

```mermaid
classDiagram
    class SignalProtocolManager {
        <<interface>>
        +generateIdentityAndKeys() SignalIdentityKeys
        +generateOneTimePreKeys(startId, count) List<SignalOneTimePreKey>
        +processPreKeyBundle(userId, bundle)
        +hasSession(userId) Boolean
        +encryptMessage(recipientId, message) EncryptedMessage
        +decryptMessage(senderId, message) ByteArray
        +getLocalIdentityKey() String
        +hasIdentity() Boolean
        +deleteSession(userId)
        +deleteAllSessions()
    }
    class AndroidSignalProtocolManager {
        +store: AndroidSignalStore
        -DEVICE_ID = 1
    }
    class AndroidSignalStore {
        <<SignalProtocolStore>>
        +prefs: EncryptedSharedPreferences
        +hasIdentity() Boolean
        +storeLocalIdentity(pair, regId)
        +checkKeyRotationNeeded(days) Boolean
    }
    class EncryptedSharedPreferences {
        <<androidx.security.crypto>>
        +MasterKey (Keystore AES256_GCM)
        +AES256_SIV key names / AES256_GCM values
    }
    class SignalIdentityKeys {
        +registrationId Int
        +identityKey String
        +signedPreKeyId Int
        +signedPreKey String
        +signedPreKeySignature String
    }
    class SignalOneTimePreKey {
        +keyId Int
        +publicKey String
    }
    class PreKeyBundle {
        +registrationId Int
        +deviceId Int
        +preKeyId Int?
        +preKeyPublic String?
        +signedPreKeyId Int
        +signedPreKeyPublic String
        +signedPreKeySignature String
        +identityKey String
    }
    class EncryptedMessage {
        +type Int
        +body String
        +registrationId Int
    }
    class ChatEncryptionHelper {
        +initializeE2EE(currentUserId) Result<Unit>
        +ensureSession(userId)
        +MessageDto.decryptIfNecessary(currentUserId)
    }
    class SignalRepository {
        +fetchPreKeyBundle(userId) PreKeyBundle?
    }
    class SupabaseChatDataSource {
        +getUserPublicKey(userId) UserPublicKeyDto?
        +uploadUserPublicKey(publicKey)
    }
    class user_public_keys {
        <<Supabase table>>
        user_id, public_key
    }
    class signal_identity_keys {
        <<Supabase table>>
        user_id, device_id, registration_id, identity_key, ...
    }

    AndroidSignalProtocolManager --> AndroidSignalStore
    AndroidSignalStore --> EncryptedSharedPreferences : stores at rest
    AndroidSignalProtocolManager ..|> SignalProtocolManager : implements
    SignalProtocolManager <|.. ChatEncryptionHelper : uses
    SignalProtocolManager <|.. GroupMetadataCipher : uses
    ChatEncryptionHelper --> SignalRepository : ensureSession
    ChatEncryptionHelper --> SupabaseChatDataSource : upload/get keys
    SignalRepository --> user_public_keys : read
    SignalRepository --> signal_identity_keys : read identity
    SupabaseChatDataSource --> user_public_keys : upsert / select
    SignalIdentityKeys ..> PreKeyBundle : bundled into
    SignalOneTimePreKey ..> PreKeyBundle : bundled into
    EncryptedMessage ..> SignalProtocolManager : encrypt/decrypt output

    note "Client side (trusted)"
    note "Supabase server (public keys / ciphertext only)"
    AndroidSignalStore ..> N1
    user_public_keys ..> N2
    signal_identity_keys ..> N2
```

## Client/server trust boundaries summary

- **Client-trusted:** UI, ViewModel, UseCases, `ChatEncryptionHelper`, `SignalProtocolManager`, `AndroidSignalStore`, `EncryptedSharedPreferences`, SQLDelight `CachedMessage` decryption cache. Plaintext and private keys exist only here.
- **Server-untrusted:** Supabase `user_public_keys`, `signal_identity_keys`, `messages` (ciphertext), Realtime transport. The server never sees private keys or plaintext.
