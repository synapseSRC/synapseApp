# AI

AI integration in Synapse Social: smart reply generation via a Gemini model exposed through a Supabase Edge Function.

## AI document index

| Document | Path | Contents |
|----------|------|----------|
| Architecture | `./architecture.md` | AI component architecture, request/response flow, Mermaid diagrams. |

## Overview

Synapse Social exposes a single AI capability: **smart reply generation** for chat conversations. The client builds a prompt from recent messages and sends it to a Supabase Edge Function (`/functions/v1/ai-generate`) backed by Gemini (`gemini-1.5-flash`), then displays up to three suggested replies.

AI runs entirely server-side; the client only sends plaintext (already decrypted) message content and renders suggestions.

## Technology overview

| Component | Technology | Responsibility |
|-----------|------------|----------------|
| Domain interface | `AiRepository` | Provider-neutral contract: `generateSmartReplies(recentMessages, conversationId) → Result<List<String>>`. |
| Implementation | `GeminiAiRepository` | POSTs prompt to Supabase Edge Function; parses up to 3 replies. |
| HTTP client | Ktor `HttpClient` + KotlinX JSON | POST to Edge Function with `apikey` and `Authorization: Bearer SUPABASE_ANON_KEY`. |
| UseCase | `GenerateSmartRepliesUseCase` | Wraps `AiRepository`; returns `Result<List<String>>`. |
| Telemetry | `AiTelemetry` (platform) | Platform-specific AI telemetry logging. |
| Config | `SynapseConfig` | API keys/endpoints (Gemini implemented; OpenAI/Anthropic/OpenRouter configured but unused). |

## Important source-code references

- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/ai/AiRepository.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/ai/GeminiAiRepository.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/ai/AiTelemetry.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/ai/GenerateSmartRepliesUseCase.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/di/AiModule.kt`
- `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/core/config/SynapseConfig.kt`

## Related feature documentation

- [Chat](../Chat/README.md) — smart replies are suggested in chat conversations.
- [Architecture](../Architecture/README.md) — DI and module boundaries.

## Security considerations

- Plaintext (already client-side decrypted) message content is sent to the AI provider — E2EE does **not** extend to the AI provider.
- Edge Function calls use `SUPABASE_ANON_KEY`; authorization is the Edge Function's responsibility.
- No API key is embedded in client code beyond `SynapseConfig` values.

## Limitations

- One use case only (`generateSmartReplies`); no summarization, chat-title generation, or other AI features are implemented.
- The configured OpenAI/Anthropic/OpenRouter endpoints are not wired to any active implementation — only the Supabase Edge Function path is used.
