# Issue #13 — Cross-platform E2EE provider integration: blocker assessment

- **Status:** BLOCKED — no provider has been approved by the issue #12 architecture decision.
- **Scope:** Evidence review for the provider and platform requirements in issue #13. This document intentionally does not implement crypto, enable private-content sending, or claim platform/security validation.
- **Relationship:** Stacked on feature/issue-12-e2ee-architecture; PR #30 remains the prerequisite architecture PR and is not merged.

## Decision

Do not integrate a library or adapt a protocol in this issue yet. The #12 ADR selects MLS v1 (RFC 9420) as the group protocol target, but explicitly approves no production implementation. No candidate in the primary sources reviewed here demonstrates all of: maintained RFC 9420 implementation, independently reviewed security posture, verified Android and iOS runtime targets, interoperable KMP/native bindings, and an acceptable cryptographic-provider/dependency chain.

Selecting an unaudited Android-only wrapper, an untested mobile target, or a bespoke FFI/crypto layer would violate issue #13 and the repository's no-custom-cryptography/no-downgrade guardrails. The existing SendMessageUseCase null-provider path now fails instead of sending plaintext, but that narrow guard is not equivalent to an approved cross-platform implementation. This issue remains blocked until issue #12 approves a provider and the evidence gates below are met.

## Candidate evidence

| Candidate | Primary-source evidence observed | Result against issue #13 gates |
| --- | --- | --- |
| OpenMLS 0.9.1 | The published docs.rs crate is MIT-licensed and implements RFC 9420. Its platform matrix calls Android and iOS targets “Unsupported, but built on CI” (built, not tested). The maintainers' May 2026 audit announcement reports eight findings, including a High finding, and says one Low finding remained in progress after fixes were published for 0.8.1 and 0.7.3. | Most promising candidate, but current official mobile support is explicitly insufficient. Verify the exact pinned release/advisories and finish platform runtime, persistence, FFI, and independent interoperability review before approval. |
| AWS Labs mls-rs 0.56.0 | The upstream README claims RFC 9420 conformance but explicitly states there has not been a full third-party security audit. Its crypto-provider table marks CryptoKit unsupported and Rust Crypto experimental; the docs.rs build target is Linux, not an Android/iOS runtime validation result. | Not approved for security-sensitive rollout without independent audit and documented mobile provider/binding/runtime evidence. |
| TraderJoe95 mls-kotlin | The upstream README identifies Kotlin/JVM 1.9.22 and Java 21 as current prerequisites and lists Kotlin Multiplatform as a long-term item. The IETF MLS implementation inventory lists it as RFC status, which is not a security audit or mobile-platform support claim. | Does not currently evidence the iOS/KMP support required by this app. Do not port it or use it as a substitute without a separately approved review. |
| zeroxv6 kotlin-mls | The upstream project describes itself as an Android library wrapping OpenMLS via Rust/UniFFI and explicitly warns it has not had a formal security audit. Its documented target is Android. | Android-only community wrapper; no iOS path or independent audit evidence. Not suitable as the cross-platform provider. |
| Cisco MLS++ | The IETF implementation inventory lists MLS++ as RFC status. The upstream README describes a C++17 implementation depending on OpenSSL or BoringSSL; the reviewed source does not establish a supported/tested Android+iOS application binding or an independent security audit. | Possible research reference only; no evidence here for safe product integration through the app's KMP/iOS architecture. A custom mobile port would be a separate security project, not a shortcut. |

“RFC” in the MLS implementation inventory indicates protocol implementation status; it does not certify product security, auditing, mobile support, or interoperability with this app's identity/service design.

## Existing repository state that remains blocking

- Android still has the legacy org.whispersystems:signal-protocol-android:2.8.1 pairwise adapter. It is not the approved MLS group provider and does not establish supported cross-platform behavior.
- The iOS homemade Signal-like implementation was removed in the #12 branch. The iOS send construction still has no provider and must fail closed; do not reintroduce the removed implementation.
- Existing send code has separately server-visible attachment and metadata fields, and the receive helper has legacy plaintext behavior. Those paths need an explicit versioned envelope and no-downgrade handling before private-content rollout.
- Actual deployed Supabase schema, RLS, attachment policy, and authorization boundaries were not verified; this issue makes no database changes.

## Unblocking gates

1. Update/approve the #12 ADR with a pinned provider, version, ciphersuite and cryptographic backend, dependency/license inventory, maintenance/advisory review, and explicit platform support evidence.
2. Demonstrate a maintained provider path on both supported Android and iOS targets, including KMP/native FFI, secure key storage, durable/atomic group state, lifecycle/thread-safety, and compatible identity credentials. “Builds” without platform runtime/interoperability validation is insufficient.
3. Complete independent security review of the exact provider/version and the app's identity, authentication, storage, delivery, recovery, and membership integration. Resolve/mitigate all blocking findings before activation.
4. Prove interoperation with an independent RFC 9420 implementation and reject invalid signatures/credentials, tampering, missing keys, replay/reordering, stale/forked epochs, provider/storage failures, and unsupported versions.
5. Keep unsupported clients unable to send or receive private content. Encrypt attachments and semantic metadata inside the authenticated application payload; do not emit plaintext in database fields, logs, push, analytics, or cache. Preserve legacy history explicitly as pre-E2EE.
6. After the provider decision, add and run the focused adversarial tests required by issue #13. Tests were not written or run for this task, as explicitly instructed.

## Acceptance status

- Approved maintained Android+iOS provider integrated: **Not met — provider unapproved.**
- No plaintext fallback on missing provider: **Partially addressed by #12** for the null-manager send path; other platform/provider and receive paths remain blocked.
- Provider errors and authentication failures fail closed: **Not demonstrated end-to-end.**
- Focused adversarial tests: **Not run/written per user instruction; required before activation.**
- Unsupported-platform matrix and security gate: **The #12 ADR documents the intended gate; runtime enforcement is not complete.**

**Disposition:** Keep issue #13 blocked. Do not start issues #14–#29 dependent implementation until provider selection and issue #13 security gates are resolved. No PR merge, deployment, or release is performed by this assessment.

## Primary references

- IETF RFC 9420, MLS Protocol: https://www.rfc-editor.org/rfc/rfc9420.html
- IETF RFC 9750, MLS Architecture: https://www.rfc-editor.org/rfc/rfc9750.html
- OpenMLS 0.9.1 docs and platform matrix: https://docs.rs/openmls/latest/openmls/
- OpenMLS audit announcement: https://blog.openmls.tech/posts/2026-05-27-independent-audit/
- AWS Labs mls-rs upstream README: https://github.com/awslabs/mls-rs
- AWS Labs mls-rs 0.56.0 documentation: https://docs.rs/mls-rs/latest/mls_rs/
- IETF MLS implementation inventory: https://github.com/mlswg/mls-implementations/blob/main/implementation_list.md
- mls-kotlin upstream: https://github.com/Traderjoe95/mls-kotlin
- Android-only kotlin-mls wrapper and security warning: https://github.com/zeroxv6/kotlin-mls
- Cisco MLS++ upstream: https://github.com/cisco/mlspp
