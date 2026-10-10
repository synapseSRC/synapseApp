# Android Sentry AI tracing

The Android app records GenAI operation, provider, model, status, and a one-way SHA-256 hash of a validated chat ID. The raw chat ID is never sent. Prompts, system instructions, model outputs, and tool payloads are intentionally omitted. Sentry can group spans by conversation, but transcripts and content-based titles are not collected.

AI traces are sampled using SENTRY_AGENT_TRACE_SAMPLE_RATE, which defaults to 0.1 and can be configured through the build environment or Gradle property (0.0–1.0). General Android traces remain sampled at 0.1.

AI request failures are sent as sanitized exceptions containing the exception class and original stack frames, not the original error message or provider response body. A beforeSendTransaction guard drops any transaction containing GenAI content attributes. Automatic user-info collection and supported HTTP request/response body collection are disabled for the Android Sentry SDK.
