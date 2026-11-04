# Notifications Architecture

Notification component architecture: fetching, realtime delivery, preferences, analytics, and OneSignal registration.

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

## IDOR protection

All methods validate that the requesting user matches the target `userId` (from `supabase.auth.currentUserOrNull()?.id`):
- `fetchNotifications`, `fetchPreferences`, `updatePreferences`: throw `IllegalStateException` on mismatch (logged as IDOR attempt).
- `markAsRead`, `updateOneSignalPlayerId`: return early on mismatch (logged as IDOR attempt).

## Realtime flow

```mermaid
sequenceDiagram
    participant UI as UI
    participant REPO as SupabaseNotificationRepository
    participant WS[Supabase Realtime WebSocket]
    participant SUP[(notifications table)]

    UI->>REPO: getRealtimeNotifications(userId)
    REPO->>WS: realtime.connect()
    REPO->>WS: channel("notifications-$userId-uuid")
    REPO->>WS: postgresChangeFlow(Insert, table=notifications, filter recipient_id=userId)
    WS->>SUP: subscribe CDC
    SUP-->>WS: new notification insert
    WS-->>REPO: PostgresAction.Insert
    REPO->>REPO: decodeRecord<NotificationDto>() → toDomain()
    REPO-->>UI: Flow<Notification> emits
    note over UI, REPO: On close: unsubscribe + removeChannel
```

## Related

- Authentication: [`../Authentication/README.md`](../Authentication/README.md)
- Architecture: [`../Architecture/README.md`](../Architecture/README.md)
