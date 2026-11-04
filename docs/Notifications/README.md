# Notifications

Notification fetching, realtime delivery, preferences, analytics, and OneSignal device registration in Synapse Social.

## Notifications document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | Notification component architecture, flows, IDOR protection, Mermaid diagram. |

## Overview

Notifications are backed by Supabase (`notifications`, `notification_preferences`, `notification_analytics` tables) with Supabase Realtime for live delivery and OneSignal for push device registration. The domain interface is `NotificationRepository`; the implementation is `SupabaseNotificationRepository`.

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Domain interface | `NotificationRepository` | Fetch, realtime, mark read, preferences, analytics, OneSignal player ID. |
| Implementation | `SupabaseNotificationRepository` | Supabase PostgREST + Realtime for notifications; OneSignal player ID stored on `users.one_signal_player_id`. |
| Realtime | Supabase Realtime (WebSocket) | `postgresChangeFlow` on `notifications` table filtered by `recipient_id`. |
| Push | OneSignal | `updateOneSignalPlayerId(userId, playerId)` stores the OneSignal player ID on the user profile. |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/NotificationRepository.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/SupabaseNotificationRepository.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/Notification.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/NotificationPreferences.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/NotificationAnalytics.kt`

## Related feature documentation

- [Authentication](../Authentication/README.md) — user session required (`currentUserId`).
- [Architecture](../Architecture/README.md) — DI and module boundaries.

## Notification architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI[Notifications screen · preferences]
    end

    subgraph Domain["Domain (framework-free)"]
        NREPO[NotificationRepository interface]
        NMODEL[Notification · NotificationPreferences · NotificationAnalytics]
    end

    subgraph Data["Data layer"]
        SNR[SupabaseNotificationRepository]
    end

    subgraph Server["Supabase"]
        NOTIF[(notifications table)]
        PREFS[(notification_preferences table)]
        ANALYTICS[(notification_analytics table)]
        RT[Realtime · WebSocket CDC]
        USERS[(users · one_signal_player_id)]
    end

    UI --> NREPO
    NREPO --> SNR
    SNR --> NOTIF
    SNR --> PREFS
    SNR --> ANALYTICS
    SNR --> RT
    SNR --> USERS

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
```

## Key behaviors

### IDOR protection

Every method verifies `supabase.auth.currentUserOrNull()?.id == userId` before acting. A mismatch is logged as an "IDOR attempt" and either throws `IllegalStateException` (fetch/preferences) or silently returns/no-ops (mark as read / update OneSignal ID).

### Realtime notifications

`getRealtimeNotifications(userId)`:
1. Connects Supabase Realtime WebSocket.
2. Opens a channel `notifications-$userId-<uuid>`.
3. Registers `postgresChangeFlow<PostgresAction.Insert>` on `notifications` where `recipient_id = userId`.
4. Decodes each `NotificationDto` → domain `Notification` and emits on a `Flow`.
5. On close, unsubscribes and removes the channel.

### Preferences

- `fetchPreferences(userId)` → `notification_preferences` table, decoded to `NotificationPreferences`.
- `updatePreferences(userId, preferences)` → upsert on `user_id`.

### Analytics

`logAnalytics(analytics)` inserts into `notification_analytics` (best-effort; failures logged, not thrown).

### OneSignal push registration

`updateOneSignalPlayerId(userId, playerId)` updates `users.one_signal_player_id` for the current user — enables push notifications via OneSignal.

## Security considerations

- IDOR checks on all user-scoped operations prevent cross-user notification access.
- Realtime channel is scoped to `recipient_id = currentUserId`.
- Analytics failures are non-fatal (logged only).

## Limitations

- `NotificationRepository.fetchNotifications` returns `List<Notification>` (not `Result<T>`), unlike most other repositories — exceptions propagate.
- `markAsRead` and `updateOneSignalPlayerId` silently swallow authorization mismatches rather than failing.
