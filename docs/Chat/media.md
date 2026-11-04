# Media

Media uploads, storage providers, attachments, and voice/media handling in Synapse Social.

## Storage providers

`StorageProvider` enum (`domain/model/StorageProvider.kt`):

| Provider | Configured when |
|----------|-----------------|
| `DEFAULT` | Always (Supabase Storage default). |
| `IMGBB` | `imgBBKey` non-blank. |
| `CLOUDINARY` | `cloudinaryCloudName` + upload preset OR API key/secret non-blank. |
| `PRIMARY` (Supabase) | `primaryUrl` + `primaryKey` non-blank. |
| `CLOUDFLARE_R2` | `r2AccountId`, `r2AccessKeyId`, `r2SecretAccessKey`, `r2BucketName` non-blank. |

`StorageConfig` (`domain/model/StorageConfig.kt`) holds per-type provider selection (`photoProvider`, `videoProvider`, `otherProvider`) plus provider-specific credentials and `compressImages`.

## Upload flow

1. `UploadMediaUseCase` → `MediaUploadRepository.upload(filePath, provider, config, bucketName, onProgress)`.
2. `MediaUploadRepositoryImpl` selects an `UploadService` by provider:
   - `SupabaseUploadService` (DEFAULT / PRIMARY) — Supabase Storage (`supabase.storage.from(bucket).upload(path, bytes)`).
   - `ImgBBUploadService` — ImgBB API.
   - `CloudinaryUploadService` — Cloudinary API.
   - `R2UploadService` — Cloudflare R2 (S3-compatible).
3. Upload returns `UploadResult` with a URL; the URL becomes `mediaUrl` on the message.
4. `SupabaseChatRepository.uploadMedia` uploads to bucket `"chat-attachments"` and maps the result URL.

### Supabase upload specifics (`SupabaseUploadService`)

- Object name: `{timestamp}_{fileName}`.
- User-scoped buckets (`covers`, `story-media`, `story-thumbnails`, `reels`, avatars) prefix the path with `userId/`.
- Covers can be signed via `createSignedUrl` (15 min expiration).
- Protected buckets require a linked content row created after upload.

## Attachments on messages

- `MessageAttachment` domain model; mapped to/from `MessageAttachmentDto`.
- Attachments are serialized into the E2EE plaintext payload (`attachments` field) and encrypted per-recipient along with the message content.
- Media URL is stored separately on the message (`mediaUrl`), not inside the encrypted payload (for non-E2EE paths) or inside `mediaUrl` of the plaintext JSON (for E2EE).

## Voice messages

Voice handling lives in the domain service layer:
- `AudioRecorder` (platform: `AudioRecorder.android.kt` / `AudioRecorder.ios.kt`) — recording.
- `EchoAudioEngine` — playback engine.
- `MediaCompressor` (`AndroidMediaCompressor` / `IosMediaCompressor`) — compression.
- `SoundPlayer` / `PlaySoundEffectUseCase` / `ResolveCallSoundUseCase` — sound effects.

Voice messages are sent as messages with `messageType = "voice"` and a `mediaUrl` pointing to the uploaded audio file; the audio bytes are encrypted inside the E2EE plaintext payload when E2EE is active.

## Image processing

- `InitialsAvatar` utility generates initials-based avatar URLs/fallbacks.
- Images are compressed per `StorageConfig.compressImages` before upload where supported by the provider.

## Related

- Chat messaging: [`./messaging.md`](./messaging.md)
- E2EE attachments: [`../E2EE/message-encryption.md`](../E2EE/message-encryption.md) (attachments serialized into the encrypted payload)
- Storage providers & config: [`domain/model/StorageProvider.kt`](../../shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/StorageProvider.kt), [`domain/model/StorageConfig.kt`](../../shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/StorageConfig.kt)
