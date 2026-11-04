# Caching

Caching behavior for messages, conversations, and decrypted E2EE content in Synapse Social.

## Caches overview

| Cache | Backend | Scope | Persists across restart? |
|-------|---------|-------|--------------------------|
| Message cache | SQLDelight `CachedMessage` | Per-chat messages | Yes |
| Conversation cache | SQLDelight `CachedConversation` | Inbox summaries | Yes |
| Decrypted-message cache (memory) | `ChatEncryptionHelper.decryptedMessageCache` | Per-message plaintext | No (process lifetime) |
| Decrypted-message cache (DB) | SQLDelight `CachedMessage.content` | Decrypted plaintext | Yes |
| Group metadata cache | Platform `SecureStorage` (key `e2ee_group_meta_<chatId>`) | Per-chat decrypted name/description/avatar | Yes |
| Session state | `AndroidSignalStore` (`EncryptedSharedPreferences`) | Signal identity, pre-keys, sessions | Yes |
| Auth session | Supabase SDK + `SecureStorage` tokens | Access/refresh tokens | Yes |
| Sync state | `InMemorySyncStateStorage` | Per-entity sync state | No (in-memory) |

## Message cache (SQLDelight `CachedMessage`)

### Read path

1. `CachedMessageDao.getMessages(chatId, limit)` — latest messages ordered by `created_at ASC`.
2. Cursor-based pagination:
   - `selectActiveByChatIdBefore` — older messages before a `(createdAt, id)` cursor.
   - `selectActiveByChatIdAfter` — newer messages after a cursor.
3. Rows with `is_locally_deleted = 1` are excluded.
4. User-deleted message IDs (`getUserDeletedMessageIds`) are filtered out and removed from cache.

### Write path

- Incoming/fetched messages are decrypted via `ChatEncryptionHelper`, then decryptable messages are upserted via `CachedMessageDao.upsertAll`.
- `trimToLimit(chatId, limit)` (`deleteOldestBeyondLimit`) retains only the latest `limit` rows per chat.
- Edits: `updateContent(messageId, content)` sets `is_edited = 1`, `content_status = 'EDITED'`, `edited_at`.
- Deletes: `markDeleted(messageId)` sets `is_deleted = 1`, `content_status = 'DELETED'` and blanks content/media/metadata (soft delete).
- Local-only deletes: `markLocallyDeleted(id)` sets `is_locally_deleted = 1`.

### Cache-first strategy

`getMessagesCacheFirst` returns cache immediately and refreshes in the background when stale (>30s since last sync). `getMessages` uses cache when available and non-empty, falling back to the network otherwise.

## Conversation cache (SQLDelight `CachedConversation`)

- `getAll()` ordered by `last_message_time DESC`.
- Upserted on conversation fetch and on realtime inbox updates.
- Fields: `last_message`, `last_message_time`, `unread_count`, `is_online`, `is_group`.
- `unread_count` incremented on inbox updates for non-active chats; zeroed when the chat is opened or marked read.

## Decrypted-message cache (E2EE)

Maintained by `ChatEncryptionHelper`:

1. **In-memory** (`decryptedMessageCache`, guarded by `cacheLock` + per-sender `Mutex`):
   - Checked first on every decryption attempt.
   - Populated after any successful decryption (memory cache, sender plaintext copy, DB restore).
2. **SQLDelight** (`CachedMessage.content`):
   - `updateContent(messageId, content)` writes decrypted plaintext back after decryption or after reading the sender plaintext copy.
   - Survives process restart; used as the second cache tier.

**Why caching is critical:** one-time PreKey Signal messages (type 3) can only be decrypted once by the libsignal state machine (the pre-key is consumed). Without caching, re-reading such a message would fail permanently.

On decryption failure, `encryptionFailureReason = "decryption_failed"` is set and raw ciphertext is never written over a readable cached message (`Message.hasReadableContent()` guard).

## Group metadata cache

`GroupMetadataCipher` caches decrypted group name/description/avatar in platform `SecureStorage` under `e2ee_group_meta_<chatId>`, keyed by field name with a fingerprint (the ciphertext envelope body). Repeat reads return the stale cached value if re-decryption fails, avoiding repeated one-time PreKey decrypt attempts.

## Sync state (not persisted)

`InMemorySyncStateStorage` holds per-entity `SyncState` (syncing flag, last synced timestamp, pending count, last error). Because it is in-memory, sync state is lost on process death; the next launch re-syncs from `lastSyncedAt = 0` (effectively a full pull per entity).

## Cache invalidation on logout

`AccountSyncManager.clearAllLocalData` clears:
- `CachedMessageDao` (all messages)
- `CachedConversationDao` (all conversations)
- `PostDao`, `StoryDao` (feed content)
- `OfflineActionRepository` (all pending actions)
- `SyncManager` (sync state, realtime channels, current user)

Called by `SupabaseAuthRepository.signOut()` before remote session revocation.

## Related

- Message lifecycle: [`../Chat/messaging.md`](../Chat/messaging.md)
- Database schema: [`./architecture.md`](./architecture.md)
- E2EE decryption cache: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md)
