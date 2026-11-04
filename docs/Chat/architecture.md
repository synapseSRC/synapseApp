# Chat Architecture

Detailed component architecture for the chat and messaging subsystem.

## Message domain model

`Message` (`domain/model/chat/Message.kt`) fields:

| Field | Type | Meaning |
|-------|------|---------|
| `id` | String | Message ID. |
| `chatId` | String | Conversation ID. |
| `senderId` | String | Sender user ID. |
| `content` | String | Plaintext (after decryption) or ciphertext envelope/placeholder (before decryption). |
| `messageType` | MessageType | TEXT, IMAGE, VOICE, SYSTEM, etc. |
| `mediaUrl` | String? | URL to attached media. |
| `deliveryStatus` | DeliveryStatus | SENDING, SENT, DELIVERED, READ, FAILED. |
| `contentStatus` | ContentStatus | ACTIVE, EDITED, DELETED. |
| `sentAt` / `createdAt` | String | Timestamps. |
| `expiresAt` | String? | Disappearing-message expiration. |
| `replyToId` | String? | Replied-to message ID. |
| `metadataContainer` | MessageMetadataContainer? | Rich metadata (polls, view-once, reactions summary). |
| `attachments` | List<MessageAttachment>? | Attachments. |
| `isDeleted`, `isEdited` | Boolean | Soft-delete / edit flags. |
| `encryptionFailureReason` | String? | `"decryption_failed"` when E2EE decryption could not recover plaintext. |

## Message lifecycle (send)

```mermaid
sequenceDiagram
    actor U as User
    participant VM as ChatViewModel
    participant SEND as SendMessageUseCase
    participant REPO as SupabaseChatRepository
    participant CH as ChatEncryptionHelper
    participant SPM as SignalProtocolManager
    participant DS as SupabaseChatDataSource
    participant MSG[(messages table)]
    participant RT[Realtime WebSocket]

    U->>VM: compose + send
    VM->>SEND: invoke(chatId, content, ...)
    SEND->>REPO: getParticipantIds(chatId)
    REPO-->>SEND: [participants]
    SEND->>SEND: serialize plaintext JSON → contentBytes
    loop each recipient (parallel)
        SEND->>REPO: ensureSession(userId)
        REPO->>CH: ensureSession → fetch bundle → processPreKeyBundle
        SEND->>SPM: encryptMessage(userId, contentBytes)
        SPM-->>SEND: EncryptedMessage
    end
    SEND->>SEND: encrypt for sender (multi-device)
    SEND->>SEND: build encryptedPayload {userId → EncryptedMessage}
    SEND->>REPO: sendMessage(content=encryptedPayload, senderPlaintext=json)
    REPO->>DS: INSERT messages row (ciphertext)
    DS-->>REPO: MessageDto
    REPO->>CH: cacheDecrypted(messageId, senderPlaintext)
    REPO->>SQLD: upsert cached message
    REPO-->>SEND: Result<Message>
    SEND-->>VM-->>UI: message displayed (plaintext)
    REPO->>RT: broadcast ciphertext to participants
    RT-->>Participants: realtime delivery
```

## Message lifecycle (receive / read)

```mermaid
sequenceDiagram
    actor UI as UI / ViewModel
    participant REPO as SupabaseChatRepository
    participant CH as ChatEncryptionHelper
    participant MC[In-memory cache]
    participant SQLD[(CachedMessage SQLDelight)]
    participant SPM as SignalProtocolManager

    UI->>REPO: getMessages / subscribeToMessages / getMessageById
    REPO->>CH: MessageDto.decryptIfNecessary(currentUserId)
    CH->>MC: memory cache hit?
    alt hit
        MC-->>CH: plaintext
    else miss
        CH->>SQLD: DB cached plaintext?
        alt DB hit (readable)
            SQLD-->>CH: content
            CH->>MC: populate memory cache
        else
            CH->>SPM: decryptMessage(senderId, envelope)
            alt decrypt OK
                SPM-->>CH: plaintext
                CH->>MC + SQLD: cache (memory + DB)
            else decrypt fails
                CH->>SPM: deleteSession + ensureSession (retry)
                alt retry OK
                    CH->>MC + SQLD: cache
                else retry fails
                    CH-->>REPO: encryptionFailureReason = "decryption_failed"
                end
            end
        end
    end
    CH-->>REPO: Message (decrypted or placeholder)
    REPO-->>UI: render message
```

## Group chat

Groups are represented by `chats` rows with `is_group = 1` and participants in `chat_participants`. Group metadata (name, description, avatar) may be E2EE-encrypted per participant via `GroupMetadataCipher` (`EncryptedGroupField` stored in `chats.enc_name` / `enc_description` / `enc_avatar_url` jsonb columns), falling back to plaintext columns.

Group operations:
- `createGroupChat(name, participantIds, avatarUrl)` — encrypts name + avatar per participant, creates chat.
- `addGroupMembers` / `removeGroupMember` — mutates membership, re-fan-outs metadata (`resyncMetadata`), posts system messages.
- `promoteToAdmin` / `demoteAdmin` — role changes, system messages.
- `toggleOnlyAdminsCanMessage`, `setDisappearingMode`, `updateGroupPrivacy`, `leaveGroup`, `deleteGroup`.
- Invites: `resolveGroupInvite`, `requestGroupJoinByInvite`, pending join requests (approve/decline).

## UseCases

| UseCase | Purpose |
|---------|---------|
| `SendMessageUseCase` | Outgoing message with per-recipient E2EE fan-out + sender multi-device encryption. |
| `InitializeE2EUseCase` | E2EE key initialization (delegates to repository). |
| `MessageSyncUseCase` | Cache-first message synchronization. |
| `MarkMessagesAsReadUseCase` / `MarkMessagesAsDeliveredUseCase` | Receipts. |
| `ToggleMessageReactionUseCase` | Message reactions. |
| `SearchMessagesUseCase` | Local cache search. |
| `DeleteExpiredMessagesUseCase` | Disappearing-message cleanup. |
| `GetMessageReactionsUseCase` / `PopulateMessageReactionsUseCase` | Reaction resolution. |
| `SubscribeToMessagesUseCase` / `SubscribeToMessageReactionsUseCase` | Realtime subscriptions. |

## Trust boundaries

- Plaintext exists only on the client (UI, ViewModel, `ChatEncryptionHelper` decrypted output, SQLDelight `CachedMessage` plaintext cache).
- Supabase `messages.content` holds ciphertext envelopes (`{userId → EncryptedMessage}`) plus a sender plaintext copy; the server never sees private keys.
- `EncryptedPayloadDetector` gates what is rendered as readable text vs. placeholder.

## Related

- E2EE details: [`../E2EE/README.md`](../E2EE/README.md)
- Local cache & sync: [`../Database/README.md`](../Database/README.md)
- Realtime delivery: [`./realtime.md`](./realtime.md)
- Media/attachments: [`./media.md`](./media.md)
