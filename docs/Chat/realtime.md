# Realtime

Realtime message delivery, typing indicators, read receipts, reactions, and roster changes via Supabase Realtime (WebSocket).

## Realtime architecture

```mermaid
sequenceDiagram
    participant Client as Client app (Compose / SwiftUI)
    participant VM as ViewModel
    participant REPO as SupabaseChatRepository
    participant DS as SupabaseChatDataSource
    participant RTDS as ChatRealtimeDataSource
    participant WS[Supabase Realtime WebSocket]
    participant SUP[(Supabase server · Postgres CDC)]

    Client->>VM: open chat screen
    VM->>REPO: subscribeToMessages(chatId)
    REPO->>DS: subscribeToMessages(chatId, onConnected)
    DS->>RTDS: subscribeToMessages(chatId)
    RTDS->>WS: supabaseClient.realtime.channel(...).subscribe()
    WS->>SUP: subscribe to table CDC
    SUP-->>WS: new message / update / delete (Postgres CDC)
    WS-->>RTDS: MessageDto (ciphertext envelope)
    RTDS-->>DS-->>REPO: dto
    REPO->>REPO: ChatEncryptionHelper.decryptIfNecessary(currentUserId)
    REPO->>REPO: reconcileRealtimeMessage(chatId, message) — merge into cache
    REPO-->>VM: Flow<Message> emits
    VM-->>Client: render message
    note over Client, SUP: Same channel pattern used for:<br/>• subscribeToInboxUpdates<br/>• subscribeToTypingStatus<br/>• subscribeToReadReceipts<br/>• subscribeToMessageReactions<br/>• subscribeToRosterChanges
```

## Realtime subscriptions

| Subscription | Flow type | Purpose |
|--------------|-----------|---------|
| `subscribeToMessages(chatId)` | `Flow<Message>` | Live messages in an open chat; decrypts and reconciles into cache; updates conversation unread/last-message. |
| `subscribeToInboxUpdates(chatIds)` | `Flow<Message>` | Inbox-wide updates; updates `CachedConversation` (last message, unread count); skips badge increment for active chat. |
| `subscribeToTypingStatus(chatId)` | `Flow<Map<String, Any?>>` | Typing indicators broadcast via `broadcastTypingStatus`. |
| `subscribeToReadReceipts(chatId)` | `Flow<MessageDto>` | Read receipt events. |
| `subscribeToMessageReactions(chatId)` | `Flow<MessageReaction>` | Reaction events. |
| `subscribeToRosterChanges()` | `Flow<String>` | Chat participation changes (added/removed), defaults to empty flow. |

## Typing

- `broadcastTypingStatus(chatId, isTyping)` → `ChatRealtimeDataSource.broadcastTypingStatus` (Supabase Realtime broadcast).
- `releaseTypingChannel(chatId)` releases the per-chat typing broadcast channel when a screen closes.

## Message reconciliation

`reconcileRealtimeMessage(chatId, message)`:
1. Looks up existing cached message by ID.
2. Merges via `ChatMapper.mergeMonotonic` (preserves optimistic pending messages).
3. If the merged message has readable content, upserts to `CachedMessage` and records cache sync time.
4. Returns the message to display.

Per-chat mutexes serialize concurrent cache writes (Realtime delivery vs. inbox refresh).

## Connection handling

- `SyncManager.subscribeRealtime(entityType, tableName)` manages Realtime channels with auto-reconnect (5s delay) on channel loss.
- Network monitor (`NetworkMonitor`) triggers `onNetworkReconnected` → retries all registered entity syncers; `onNetworkDisconnected` sets offline state.
- Supabase Realtime is used both for chat messaging (`ChatRealtimeDataSource`) and the sync layer (`SyncManager`).

## Security / trust

- Realtime transports ciphertext only; decryption happens on the client after receipt.
- Undecryptable messages render as placeholder; raw ciphertext never replaces a readable cached message (`hasReadableContent` guard).

## Related

- Messaging: [`./messaging.md`](./messaging.md)
- E2EE decryption: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md)
- Sync & offline: [`../Database/README.md`](../Database/README.md)
