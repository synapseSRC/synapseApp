# Voice

Voice message recording, playback, compression, and media handling in Synapse Social.

## Voice document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | Voice/media component architecture, recording/playback flow, Mermaid diagram. |

## Overview

Voice messages are sent as chat messages with `messageType = "voice"` and a `mediaUrl` pointing to an uploaded audio file. Recording and playback are handled by platform audio services in the shared domain layer; upload goes through the multi-provider media upload pipeline (see [`Chat/media.md`](../Chat/media.md)).

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Recording | `AudioRecorder` (`AudioRecorder.android.kt` / `AudioRecorder.ios.kt`) | Platform audio capture. |
| Playback | `EchoAudioEngine` | Audio playback engine. |
| Compression | `MediaCompressor` (`AndroidMediaCompressor` / `IosMediaCompressor`) | Audio/image compression before upload. |
| Sound effects | `SoundPlayer` / `PlaySoundEffectUseCase` / `ResolveCallSoundUseCase` | UI sound effects and call sounds. |
| Upload | `MediaUploadRepository` + upload services | Multi-provider upload (Supabase DEFAULT, ImgBB, Cloudinary, R2). |
| E2EE | `SendMessageUseCase` | Voice audio bytes encrypted inside the message plaintext payload per-recipient. |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/AudioRecorder.kt`
- `shared/src/androidMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/AudioRecorder.android.kt`
- `shared/src/iosMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/AudioRecorder.ios.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/EchoAudioEngine.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/MediaCompressor.kt`
- `shared/src/androidMain/kotlin/com/synapse/social/studioasinc/shared/core/media/AndroidMediaCompressor.kt`
- `shared/src/iosMain/kotlin/com/synapse/social/studioasinc/shared/core/media/IosMediaCompressor.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/service/SoundPlayer.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/sound/PlaySoundEffectUseCase.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/sound/ResolveCallSoundUseCase.kt`

## Related feature documentation

- [Chat media](../Chat/media.md) — upload pipeline, storage providers, attachments.
- [Chat messaging](../Chat/messaging.md) — voice messages sent as `messageType = "voice"`.
- [E2EE](../E2EE/README.md) — voice content encrypted inside E2EE payloads.

## Voice architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI[Voice message UI · record / play]
    end

    subgraph Domain["Domain (framework-free)"]
        REC[AudioRecorder interface]
        ENGINE[EchoAudioEngine]
        COMP[MediaCompressor]
        SOUND[SoundPlayer]
        UPLOAD[UploadMediaUseCase]
    end

    subgraph Data["Data / platform"]
        AREC[AudioRecorder.android / AudioRecorder.ios]
        MCOMP[AndroidMediaCompressor / IosMediaCompressor]
        MUR[MediaUploadRepository]
        SVCS[Supabase / ImgBB / Cloudinary / R2 upload services]
    end

    subgraph Server["Supabase Storage"]
        BUCKET[(chat-attachments bucket)]
    end

    UI --> REC
    UI --> ENGINE
    REC --> AREC
    ENGINE --> AREC
    REC --> COMP
    COMP --> MCOMP
    UPLOAD --> MUR
    MUR --> SVCS
    SVCS --> BUCKET
    REC -.-> UPLOAD

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
```

## Recording → send flow

1. User records audio via `AudioRecorder` (platform implementation).
2. Recorded audio is optionally compressed via `MediaCompressor`.
3. Audio file is uploaded via `UploadMediaUseCase` → `MediaUploadRepository` → bucket `"chat-attachments"`.
4. The returned `mediaUrl` is used to send a chat message (`messageType = "voice"`) through `SendMessageUseCase`.
5. The message content/plaintext payload (including any metadata) is E2EE-encrypted per-recipient and persisted.

## Playback flow

1. User taps play on a voice message (`mediaUrl`).
2. `EchoAudioEngine` plays the audio from the URL / local file.
3. `SoundPlayer` handles UI sound effects independently.

## Related

- Chat media & upload: [`../Chat/media.md`](../Chat/media.md)
- Message sending: [`../Chat/messaging.md`](../Chat/messaging.md)
- E2EE: [`../E2EE/README.md`](../E2EE/README.md)
