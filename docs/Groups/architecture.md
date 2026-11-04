# Groups Architecture

Group component architecture: creation, membership, roles, invitations, and encrypted metadata.

## Component architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI[Group screens · create group · member list · settings]
    end

    subgraph Domain["Domain (framework-free)"]
        CREPO[ChatRepository interface]
    end

    subgraph Data["Data layer"]
        CGR[ChatGroupRepository]
        GMC[GroupMetadataCipher]
        DS[SupabaseChatDataSource · ChatGroupDataSource]
        RTDS[ChatRealtimeDataSource]
    end

    subgraph Server["Supabase"]
        CHAT[(chats · chat_participants)]
        RT[Realtime · WebSocket]
    end

    UI --> CREPO
    CREPO --> CGR
    CGR --> DS
    CGR --> GMC
    GMC --> DS
    DS --> CHAT
    RTDS --> RT
    RT -.-> UI
    CGR -.->|system messages| CHAT

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
```

## Group operations

| Operation | Behavior |
|-----------|----------|
| `createGroupChat(name, participantIds, avatarUrl)` | Encrypts name + avatar per participant via `GroupMetadataCipher.encryptField`; stores in `chats.enc_name` / `enc_avatar_url` jsonb; creates chat. |
| `addGroupMembers(chatId, userIds)` | Adds members; re-fan-outs metadata (`resyncMetadata`); emits "X was added" system message. |
| `removeGroupMember(chatId, userId)` | Removes member; emits "X was removed" system message. |
| `promoteToAdmin` / `demoteAdmin` | Role change; emits admin system message. |
| `leaveGroup(chatId)` | Current user leaves; emits "X left" system message. |
| `toggleOnlyAdminsCanMessage` | Restricts message sending to admins. |
| `setDisappearingMode` / `getDisappearingMode` | Per-chat disappearing-message mode. |
| `updateGroupPrivacy(allowMemberInvites, requireJoinApproval, allowMemberSettingsEdits)` | Privacy flags. |
| `deleteGroup(chatId)` | Deletes group. |
| `resyncGroupMetadata(chatId)` | Re-fan-outs encrypted metadata to the full current roster (for late-joining members). |
| Invites | `resolveGroupInvite`, `requestGroupJoinByInvite`, pending join requests (approve/decline), invite code rotation. |

## Group metadata E2EE

- `GroupMetadataCipher.encryptField(plaintext, participantIds, currentUserId)` forces a fresh PreKey envelope for every recipient (delete session → ensure session → encrypt → delete session) so late-joining members can decrypt independently.
- Stored as `EncryptedGroupField {editorId, envelopes: Map<userId, EncryptedMessage>}` in `chats.enc_name`, `enc_description`, `enc_avatar_url`.
- Decryption uses `decryptField(field, currentUserId, chatId, fieldKey)` with `SecureStorage` caching keyed by envelope body fingerprint.
- Graceful degradation: if E2EE is unavailable or fails, falls back to legacy plaintext columns (`chats.name`, `description`, `avatarUrl`).

## System messages

Group mutations emit centered system-message pills (`insertSystemMessage`) for join/leave/admin changes. System messages are **never encrypted**. Failures to write a system message are best-effort and do not roll back the completed mutation.

## Trust boundary

- Group metadata ciphertext is stored server-side in jsonb columns; the server cannot decrypt (requires client Signal sessions).
- Group membership mutations are server-authoritative; clients receive roster changes via Realtime.

## Related

- E2EE key management: [`../E2EE/key-management.md`](../E2EE/key-management.md)
- Group metadata cipher: [`GroupMetadataCipher.kt`](../../shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/GroupMetadataCipher.kt)
