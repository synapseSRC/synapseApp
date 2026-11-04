# AI Architecture

AI component architecture and the smart-reply request/response flow.

## Component architecture

```mermaid
flowchart TD
    subgraph UI["Presentation"]
        UI[Chat screen · reply suggestions chip]
    end

    subgraph Domain["Domain (framework-free)"]
        USECASE[GenerateSmartRepliesUseCase]
        REPO[AiRepository interface]
    end

    subgraph Data["Data layer"]
        GEMINI[GeminiAiRepository]
        HTTP[Ktor HttpClient]
        TELEM[AiTelemetry · platform]
    end

    subgraph Server["Supabase Edge Function"]
        EF[/functions/v1/ai-generate · gemini-1.5-flash]
    end

    UI --> USECASE
    USECASE --> REPO
    REPO --> GEMINI
    GEMINI --> HTTP
    HTTP --> EF
    GEMINI --> TELEM

    style Server fill:#ffebee,stroke:#c62828
    style Data fill:#e8f5e9,stroke:#2e7d32
```

## Smart replies flow

```mermaid
sequenceDiagram
    participant UI as Chat UI
    participant UC as GenerateSmartRepliesUseCase
    participant REPO as AiRepository (GeminiAiRepository)
    participant HTTP as Ktor HttpClient
    participant EF[Supabase Edge Function /functions/v1/ai-generate]
    participant GEM[Gemini API (server-side)]

    UI->>UC: generateSmartReplies(recentMessages, conversationId)
    UC->>REPO: generateSmartReplies(recentMessages, conversationId)
    REPO->>REPO: build prompt "Suggest three short context-aware replies..."
    REPO->>HTTP: POST /functions/v1/ai-generate\n {prompt, model: "gemini-1.5-flash"}\n headers: apikey, Authorization: Bearer SUPABASE_ANON_KEY
    HTTP->>EF: request
    EF->>GEM: call Gemini
    GEM-->>EF: text (one reply per line)
    EF-->>HTTP: {text, error}
    HTTP-->>REPO: AiGenerateResponse
    alt success
        REPO->>REPO: split lines → strip numbering → trim → filter blank → take 3
        REPO-->>UC: Result.success([reply1, reply2, reply3])
    else failure
        REPO-->>UC: Result.failure(exception)
    end
    UC-->>UI: Result<List<String>>
```

## Request/response format

**Request** (to Supabase Edge Function):
```json
{ "prompt": "...", "model": "gemini-1.5-flash" }
```

**Response**:
```json
{ "text": "Reply one\nReply two\nReply three", "error": null }
```

## Configuration notes

- `SynapseConfig` defines endpoints for Gemini, OpenAI, Anthropic, OpenRouter, ImgBB, Cloudinary, but the **implemented** AI path only calls the Supabase Edge Function (`ai-generate`) with the Gemini model.
- The other endpoints and keys are configured but not wired to an active implementation.
