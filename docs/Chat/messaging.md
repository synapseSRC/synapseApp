# Messaging

Message send/receive, reactions, read receipts, disappearing messages, and local caching as implemented in the chat subsystem.

## Sending a message

1. ViewModel calls `SendMessageUseCase.invoke(chatId, content, mediaUrl, messageType, expiresAt, replyToId, attachments, metadataContainer, requireE2EE)`.
2. If E2EE is unavailable and `requireE2EE == false`, the message is sent as plain text (graceful degradation). If `requireE2EE == true`, sending fails.
3. Plaintext is serialized to JSON `{content, mediaUrl, metadata, attachments}`.
4. Per-recipient E2EE fan-out: each participant (plus sender for multi-device) gets a separate `EncryptedMessage` envelope.
5. `SupabaseChatRepository.sendMessage` persists to the `messages` table. If the network call fails and an `OfflineActionRepository` is present, the message is queued as a `PendingAction.SEND_MESSAGE` for later retry (offline support).
6. On success, the sender's copy is cached locally (`cacheDecrypted` + `CachedMessageDao.upsert`) so the sender sees plaintext immediately.
7. The ciphertext is broadcast via Supabase Realtime to participants.

> E2EE send details: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md)

## Receiving / reading a message

Incoming messages (from network fetch or Realtime) pass through `ChatEncryptionHelper.MessageDto.decryptIfNecessary(currentUserId)`:
1. In-memory decryption cache → local SQLDelight cache → Signal Protocol decryption.
2. Sender plaintext copies are read directly without consuming ratchet state.
3. On failure, session reset + one retry; permanent failure yields `encryptionFailureReason = "decryption_failed"` and a placeholder ("Message is encrypted").
4. Decrypted plaintext is cached in memory and SQLDelight (`CachedMessage`).

> Decryption details: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md)

## Message reactions

- `toggleMessageReaction(messageId, emoji, chatId)` — add/remove a reaction; persisted to `message_reactions` table and broadcast via Realtime.
- `getReactionsForMessage(messageId)` / `getReactionsForMessages(messages)` — resolve reactions, populated into messages via `PopulateMessageReactionsUseCase`.
- Realtime: `subscribeToMessageReactions(chatId)` → `Flow<MessageReaction>`.
- Local: `MessageReactionDao` (SQLDelight `MessageReaction` table, composite key `message_id + user_id + reaction_emoji`).

## Read receipts

- `markMessagesAsRead(chatId, shareReceipt)` — marks messages read, updates `read_by`/`read_at`/delivery status locally and remotely; broadcast as read-receipt Realtime events.
- `markMessagesAsDelivered(chatId)` — marks delivered.
- `subscribeToReadReceipts(chatId)` → `Flow<Message>`.
- Unread count is maintained on `CachedConversation`; badge logic in `subscribeToInboxUpdates` avoids double-counting in the active chat.

## Disappearing messages

- `setDisappearingMode(chatId, mode)` / `getDisappearingMode(chatId)` — mode stored per chat (`DisappearingMode` enum with optional seconds).
- On send, `expiresAt` is calculated from the chat's disappearing mode when not explicitly provided.
- `deleteExpiredMessages(chatId)` removes rows where `expires_at < now`.
- `DeleteExpiredMessagesUseCase` triggers cleanup.

## Message editing

- `editMessage(messageId, newContent)` — re-encrypts the edit payload for participants (`encryptEditPayload`) and persists; local cache updated via `updateContent`.
- Edited messages carry `contentStatus = EDITED`, `edited_at` timestamp.

## Message deletion

- `deleteMessage(messageId)` / `deleteMessages(messageIds)` — hard delete for everyone.
- `deleteMessageForMe(messageId)` / `deleteMessagesForMe(messageIds)` — soft delete for the sender only (`is_locally_deleted`, `user_deleted_message_ids`).
- Deleted rows are excluded from cache reads and conversation previews.

## Local caching

- SQLDelight `CachedMessage` table with cursor-based keyset pagination (`selectActiveByChatIdBefore` / `After`).
- `trimToLimit(chatId, limit)` bounds per-chat cache size.
- `CachedConversation` holds inbox summaries (last message, unread count, online status).
- Cache-first reads (`getMessagesCacheFirst`) with background refresh (`syncMessagesInBackground`).
- `reconcileRealtimeMessage` merges Realtime messages into cache, preserving optimistic pending messages.

## Related

- E2EE encryption/decryption: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md)
- Realtime delivery: [`./realtime.md`](./realtime.md)
- Media & attachments: [`./media.md`](./media.md)
- Database cache schema: [`../Database/README.md`](../Database/README.md)
