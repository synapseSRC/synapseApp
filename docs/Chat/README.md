# Chat

Conversations, messaging, reactions, read receipts, disappearing messages, realtime delivery, and media in Synapse Social. All chat operations flow through the domain `ChatRepository` interface and are implemented by `SupabaseChatRepository` backed by Supabase (PostgREST + Realtime + Storage) with a SQLDelight local cache.

## Chat document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | Chat component architecture, repository contract, Mermaid diagrams. |
| Messaging | `./messaging.md` | Message send/receive, E2EE integration, reactions, read receipts, disappearing messages, caching. |
| Realtime | `./realtime.md` | Supabase Realtime subscriptions, message delivery, typing, presence, reconciliation. |
| Media | `./media.md` | Media uploads, storage providers, voice messages, compression. |

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Domain interface | `ChatRepository` | Provider-neutral chat contract: conversations, messages, send/receive, reactions, receipts, groups, E2EE session management. Returns `Result<T>`. |
| Implementation | `SupabaseChatRepository` | Supabase-backed chat repository; coordinates `SupabaseChatDataSource`, `ChatEncryptionHelper`, `GroupMetadataCipher`, media upload, offline actions. |
| Data source | `SupabaseChatDataSource` | Aggregates conversation/message/group/key/realtime/device/settings sub-datasources. |
| Message data source | `ChatMessageDataSource` | PostgREST CRUD for `messages` table. |
| Conversation data source | `ChatConversationDataSource` | PostgREST for `chat_participants`, `chats`, inbox summaries. |
| Group data source | `ChatGroupDataSource` | Group invites, join requests, membership, privacy. |
| Key data source | `ChatKeyDataSource` | `user_public_keys` table for E2EE public-key lookup/upload. |
| Realtime data source | `ChatRealtimeDataSource` | Supabase Realtime (WebSocket) subscriptions for messages, typing, receipts, reactions, roster. |
| E2EE helper | `ChatEncryptionHelper` | Per-message decrypt with cache, session establishment, key init, identity mismatch. |
| Local cache | SQLDelight (`CachedMessage`, `CachedConversation`) | Local persistence of messages and conversations; cursor-based pagination. |
| Sync | `SyncManager` + `MessageEntitySyncer` | Push pending local changes, pull remote, Realtime CDC, offline action queue. |
| Media | `MediaUploadRepository` + upload services | Multi-provider media upload (Supabase DEFAULT, ImgBB, Cloudinary, Cloudflare R2). |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/ChatRepository.kt` — chat repository interface (127 methods).
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/SupabaseChatRepository.kt` — chat repository implementation.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/datasource/SupabaseChatDataSource.kt` — data source aggregator.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/chat/Message.kt` — domain message model.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/chat/Conversation.kt` — domain conversation model.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/dto/chat/ChatDtos.kt` — chat DTOs.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/mapper/ChatMapper.kt` — DTO ↔ domain mapping.
- `shared/src/commonMain/sqldelight/com/synapse/social/studioasinc/shared/data/database/CachedMessage.sq` — cached message schema.
- `shared/src/commonMain/sqldelight/com/synapse/social/studioasinc/shared/data/database/CachedConversation.sq` — cached conversation schema.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/ChatEncryptionHelper.kt` — E2EE message helper (see `docs/E2EE`).
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/sync/MessageEntitySyncer.kt` — message sync syncer.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/sync/SyncManager.kt` — sync coordinator.

## Related feature documentation

- [E2EE](../E2EE/README.md) — message encryption/decryption applied to chat payloads.
- [Database](../Database/README.md) — SQLDelight cache and sync architecture.
- [Authentication](../Authentication/README.md) — user session required for all chat operations.
- [Groups](./architecture.md) — group creation, membership, roles (covered in Groups docs).
- [Media](./media.md) / [Voice](../Voice/README.md) — attachments and voice messages.

## Chat architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI1[Chat screens (Compose / SwiftUI)]
        VM[ChatViewModel · StateFlow<ChatUiState>]
    end

    subgraph Domain["Domain (framework-free)"]
        CREPO[ChatRepository interface]
        SEND[SendMessageUseCase]
        INIT[InitializeE2EUseCase]
        MSGSYNC[MessageSyncUseCase]
        READ[MarkMessagesAsReadUseCase]
        REA[ToggleMessageReactionUseCase]
    end

    subgraph Data["Data layer"]
        SR[SupabaseChatRepository]
        DS[SupabaseChatDataSource]
        MSGDS[ChatMessageDataSource]
        CONVDS[ChatConversationDataSource]
        GRPDS[ChatGroupDataSource]
        RTDS[ChatRealtimeDataSource]
        KEYDS[ChatKeyDataSource]
        CH[ChatEncryptionHelper]
        GMC[GroupMetadataCipher]
        MUR[MediaUploadRepository]
    end

    subgraph Local["Local persistence"]
        SQLD[(SQLDelight · CachedMessage / CachedConversation)]
    end

    subgraph Server["Supabase"]
        MSG[(messages table · ciphertext)]
        CHAT[(chats / chat_participants)]
        RT[Realtime · WebSocket]
        STG[Storage · chat-attachments bucket]
    end

    UI --> VM
    VM --> CREPO
    VM --> SEND
    CREPO --> SR
    SR --> DS
    DS --> MSGDS
    DS --> CONVDS
    DS --> GRPDS
    DS --> RTDS
    DS --> KEYDS
    SR --> CH
    SR --> GMC
    SR --> MUR
    CH --> SQLD
    MSGDS --> MSG
    CONVDS --> CHAT
    RTDS --> RT
    MUR --> STG
    RT -.-> UI1

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
    style Local fill:#fff3e0,stroke:#ef6c00
```

### Repository contract highlights (`ChatRepository`)

- Conversations: `getConversations`, `getOrCreateChat`, archive/pin/mute/delete.
- Messages: `getMessages`, `getMessagesCacheFirst`, `getMessagesAfter`, `searchMessages`, `sendMessage`, `editMessage`, `deleteMessage`, `markMessagesAsRead/Delivered`.
- Realtime: `subscribeToMessages`, `subscribeToInboxUpdates`, `subscribeToTypingStatus`, `subscribeToReadReceipts`, `reconcileRealtimeMessage`.
- Groups: `createGroupChat`, `addGroupMembers`, `promoteToAdmin`, `setDisappearingMode`, `resyncGroupMetadata`, etc.
- E2EE: `initializeE2EE()`, `ensureSession(userId)`.

All methods return `Result<T>` except flows (`Flow<Message>`, `Flow<TypingStatus>`, etc.) and plain getters.

## Cache-first message reading

`getMessagesCacheFirst` and `getMessages` follow a cache-first pattern:
1. Read from SQLDelight `CachedMessage` (cursor-based keyset pagination).
2. Filter out user-deleted and locally-deleted rows.
3. If cache is empty or stale, fetch from the network and upsert into the cache.
4. Decrypt each message via `ChatEncryptionHelper.decryptIfNecessary(currentUserId)` before returning to the UI.
5. `trimToLimit(chatId, limit)` bounds the local cache per chat.

Realtime messages are reconciled via `reconcileRealtimeMessage` with a merge strategy that preserves optimistic pending messages.
