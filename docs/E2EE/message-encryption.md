# E2EE Message Encryption

Outgoing encryption, incoming decryption, multi-recipient fan-out, multi-device handling, realtime delivery, decryption caching, and retry behavior as implemented in Synapse Social.

## Outgoing message encryption

Source: `SendMessageUseCase.invoke()` (`domain/usecase/chat/SendMessageUseCase.kt`) and `ChatEncryptionHelper` (`data/repository/ChatEncryptionHelper.kt`).

### Flow

1. **Authentication check:** `repository.getCurrentUserId()`; fails with `Result.failure("Not logged in")` if no session.
2. **E2EE availability:** If `signalProtocolManager == null` and `requireE2EE == false`, the message is sent as plain text (graceful degradation). If `requireE2EE == true`, sending fails with `IllegalStateException("End-to-end encryption is required...")`.
3. **Participant discovery:** `repository.getParticipantIds(chatId)`. Recipients = all participants except the current user. If the result is empty but the chat has members (self-chat / "saved messages"), recipients = all members (encrypt for self).
4. **Plaintext serialization:** Builds a JSON object `{content, mediaUrl?, metadata?, attachments?}` and encodes to UTF-8 bytes (`contentBytes`).
5. **Per-recipient session establishment:** For each recipient (concurrent `async {}` block inside `coroutineScope`):
   - `repository.ensureSession(userId)` → `ChatEncryptionHelper.ensureSession(userId)`: if no Signal session exists, fetch the recipient's public key bundle from `user_public_keys` and `processPreKeyBundle(userId, bundle)` to establish an X3DH session.
   - `signalProtocolManager.encryptMessage(userId, contentBytes)` → `EncryptedMessage {type, body, registrationId}` via libsignal `SessionCipher.encrypt`.
6. **Failure handling:** If any recipient fails to encrypt (session establishment or encryption throws), that recipient contributes `null`; after `awaitAll()`, if `payloadMap.size < otherParticipants.size`, the message is **aborted** (`Result.failure("Encryption failed for one or more recipients. Message not sent.")`). No partial unencrypted message is sent.
7. **Multi-device sender encryption:** If `currentUserId` is not already in `payloadMap`, encrypt an additional envelope for the sender so the sender's other logged-in devices can decrypt. Failure here is logged as a warning but does not abort.
8. **Payload assembly:** `encryptedPayload = JsonObject(finalPayloadMap).toString()` — a map of `userId → EncryptedMessage` JSON element.
9. **Persistence:** `repository.sendMessage(chatId, content = encryptedPayload, mediaUrl = null, ..., senderPlaintext = jsonPayload)`. The `senderPlaintext` is stored so the sending device can render the message without consuming a ratchet key; `content` holds the ciphertext envelope.
10. **Cache:** `encryptedMessage.cacheDecrypted(messageDto.id, senderPlaintext)` and `cachedMessageDao.upsert(...)` so the sender sees plaintext immediately.

### Message content format

The plaintext payload serialized before encryption:

```json
{
  "content": "<message text>",
  "mediaUrl": "<optional media URL>",
  "metadata": "<optional JSON metadata container>",
  "attachments": "<optional attachments array>"
}
```

The on-wire envelope stored in `messages.content`:

```json
{
  "<recipientUserId1>": { "type": 2, "body": "<base64 ciphertext>", "registrationId": 123 },
  "<recipientUserId2>": { "type": 3, "body": "<base64 ciphertext>", "registrationId": 123 },
  "<currentUserId>": "{ \"content\": \"...\" }"   // sender plaintext copy (not a ratchet envelope)
}
```

## Incoming message decryption

Source: `ChatEncryptionHelper.MessageDto.decryptIfNecessary(currentUserId)`.

### Decryption strategy (multi-tier, in order)

1. **In-memory cache** (`decryptedMessageCache`, guarded by `cacheLock` + per-sender Mutex). Fastest path; survives only for the process lifetime.
2. **Local database cache** (`CachedMessageDao.getMessageById(messageId)`): if cached content is non-blank, not a placeholder, and not an encrypted payload → restore from DB, populate memory cache, return.
3. **Signal Protocol decryption:**
   - `EncryptedPayloadDetector.isEncryptedPayload(content)` classifies the content.
   - Parse the JSON envelope; extract the entry keyed by `currentUserId`.
   - **Sender plaintext copy:** if the entry is a `JsonPrimitive` string that parses to a JSON object containing `content`, read it directly as plaintext (no ratchet consumption), cache, update DB, return.
   - **Cipher envelope:** if the entry is an `EncryptedMessage`:
     - Ensure a session exists with `senderId` (`ensureSession` → fetch bundle from `user_public_keys` → `processPreKeyBundle`).
     - `signalProtocolManager.decryptMessage(senderId, myPayload)` → plaintext bytes → UTF-8 string.
     - Cache (memory + DB), extract content/mediaUrl/metadata, return.

### Retry on decryption failure

If Signal decryption throws (non-cancellation):
1. Log error: `"Signal decryption failed ... Attempting session reset retry."`
2. `signalProtocolManager.deleteSession(senderId)` — drop the (possibly desynchronized) Double Ratchet session.
3. `ensureSession(senderId)` — rebuild the X3DH session from the recipient's published bundle.
4. Retry `signalProtocolManager.decryptMessage(senderId, myPayload)`.
5. If retry succeeds: cache (memory + DB), update DB content, return decrypted message.
6. If retry fails: log error, fall through; message is returned with `encryptionFailureReason = "decryption_failed"`.

> **Implemented:** one session-reset retry. **Not implemented:** escalation to user identity-verification, fallback to out-of-band key exchange.

### Content extraction

`extractContent(jsonString)` parses the decrypted plaintext as JSON and reads `content`, `mediaUrl`, `metadata`; on parse failure, returns the raw string as content.

### Decrypted content caching

| Tier | Backend | Persists across restart? | Purpose |
|------|---------|--------------------------|---------|
| Memory | `decryptedMessageCache: MutableMap<String, String>` | No | Fast re-read; avoids re-decrypting one-time PreKey messages that decrypt once. |
| DB | `CachedMessageDao` (SQLDelight `CachedMessage` table) | Yes | Survives app restart; `updateContent(messageId, content)` writes plaintext back. |

One-time PreKey envelopes (type 3) consume the pre-key on first decrypt, so they cannot be re-decrypted by libsignal; the cache is essential for re-reading such messages.

## Multi-recipient encryption

Every participant (plus the sender for multi-device) receives an **independently decryptable** `EncryptedMessage` envelope encrypted with that recipient's session. There is no shared group key for 1:1 chats; each recipient's ciphertext is separate.

Group metadata (name/description/avatar) uses the same fan-out pattern via `GroupMetadataCipher`: one `EncryptedMessage` envelope per participant under `EncryptedGroupField.editorId + envelopes`, stored in `chats.enc_name` / `enc_description` / `enc_avatar_url` jsonb columns.

## Multi-device handling

- Same account, different device → same `DEVICE_ID = 1` Signal address; no per-device Signal session.
- Application-layer multi-device: `SendMessageUseCase` encrypts for `currentUserId` (the sender) so other devices of the same account can decrypt the message they sent.
- On decryption, each device uses its own locally stored session state (`AndroidSignalStore`), so a message decrypted on Device A does not consume ratchet state on Device B — each device independently decrypts its own envelope.

## Realtime message delivery

- Outgoing messages are persisted to Supabase (`messages` table) via `SupabaseChatDataSource.sendMessage`.
- Realtime delivery uses Supabase Realtime (WebSocket) via `ChatRealtimeDataSource.subscribeToMessages` / `subscribeToInboxUpdates`.
- Incoming realtime DTOs are decrypted by `ChatEncryptionHelper.decryptIfNecessary` in `SupabaseChatRepository.subscribeToMessages` and reconciled into the local cache via `reconcileRealtimeMessage` (merge strategy preserving optimistic pending messages).
- Unread-count badge logic in `subscribeToInboxUpdates` avoids double-counting messages in the active chat.

## Decryption failure and placeholder rendering

- `EncryptedPayloadDetector.isPlaceholder(content)` matches legacy placeholders: `"Message is encrypted"`, `"🔒 Encrypted message"`, `"🔒 You sent an encrypted message"`, `"🔒 You sent an encrypted message (Copy)"`.
- `conversationPreview()` renders `EncryptedPayloadDetector.PLACEHOLDER` ("Message is encrypted") when a message has no readable content.
- `Message.hasReadableContent()` is true only when `encryptionFailureReason == null` and content is neither placeholder nor encrypted payload — used to gate cache writes and conversation previews so raw ciphertext never replaces a readable cached message.

## Security properties implemented

- Per-recipient ciphertext: no recipient can read another recipient's envelope without their own Signal session keys.
- Sender plaintext copy lets the sender read without consuming ratchet state.
- Decryption cache prevents one-time PreKey messages from becoming permanently unreadable.
- Session reset + retry on desynchronization.
- Raw ciphertext never written over a readable cached message (`hasReadableContent` guard).

## Limitations / not implemented

- No user-visible identity verification or safety-number comparison.
- No automatic one-time pre-key pool replenishment.
- No key-rotation enforcement (warning only).
- No fallback / out-of-band recovery when decryption permanently fails — message shows `decryption_failed`.
- Message content is searchable only after decryption at read time; server-side search over ciphertext is not possible.
