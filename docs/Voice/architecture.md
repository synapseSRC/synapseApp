# Voice Architecture

Voice/message media component architecture.

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI[Voice message UI · record / play / pause]
    end

    subgraph Domain["Domain (framework-free)"]
        REC[AudioRecorder]
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

## Platform notes

- `AudioRecorder` is an expect/actual-style domain service with platform implementations in `androidMain` and `iosMain`.
- `MediaCompressor` likewise has `AndroidMediaCompressor` and `IosMediaCompressor`.
- `EchoAudioEngine` and `SoundPlayer` are shared domain services.

## E2EE boundary

Voice message content flows through `SendMessageUseCase`, so the audio metadata/plaintext is encrypted per-recipient like any other chat message. The media file URL itself is stored on the message record; the audio bytes are not separately encrypted beyond the message-level E2EE payload.
