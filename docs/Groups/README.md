# Groups

Group chat creation, membership management, roles and permissions, invitations, and encrypted group metadata in Synapse Social.

## Groups document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | Group component architecture, flows, Mermaid diagrams, E2EE group metadata. |

## Overview

Groups are conversations with `is_group = 1` in the `chats` table, with participants tracked in `chat_participants`. Group operations (create, add/remove members, promote/demote admin, leave, delete, privacy settings) are coordinated by `ChatGroupRepository` (data layer) behind the `ChatRepository` domain interface. Group metadata (name, description, avatar) may be end-to-end encrypted per participant via `GroupMetadataCipher` (see [`../E2EE/README.md`](../E2EE/README.md)).

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Domain interface | `ChatRepository` | Group contract: create, members, admins, invites, privacy, disappearing mode, metadata resync. |
| Data implementation | `ChatGroupRepository` | Coordinates `SupabaseChatDataSource` + `GroupMetadataCipher`; emits system messages. |
| Data source | `ChatGroupDataSource` | PostgREST: group invites, join requests, membership, privacy. |
| Group metadata E2EE | `GroupMetadataCipher` | Fan-out encrypts name/description/avatar per participant (`EncryptedGroupField` in `chats.enc_*` jsonb columns). |
| Realtime | `ChatRealtimeDataSource` | Roster changes and group events via Supabase Realtime. |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/ChatGroupRepository.kt` — group repository implementation.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/GroupMetadataCipher.kt` — encrypted group metadata fan-out.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/ChatRepository.kt` — group methods on the domain interface.
- `shared/src/commonMain/sqldelight/com/synapse/social/studioasinc/shared/data/database/CachedConversation.sq` — `is_group` column.
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/datasource/SupabaseChatDataSource.kt` — group data source delegation.

## Related feature documentation

- [E2EE](../E2EE/README.md) — group metadata encryption via `GroupMetadataCipher`.
- [Chat](../Chat/README.md) — group conversations are chats.
- [Database](../Database/README.md) — `chats` / `chat_participants` caching.
