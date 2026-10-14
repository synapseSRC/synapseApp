# ADR-001: Group end-to-end encryption architecture

- **Status:** MLS protocol direction proposed; rollout is **NO-GO** until the gates below pass.
- **Scope:** New private group content and the E2EE foundation in synapseApp. This ADR does not claim existing messages are end-to-end encrypted.

## Decision

Use **Messaging Layer Security (MLS) v1, RFC 9420**, as the protocol target for group key establishment, membership changes, and encrypted application messages. Do not design a group protocol, KDF, signature scheme, cipher, or key ratchet in application code.

**No MLS implementation/provider is approved for production integration yet.** OpenMLS is the leading candidate for an implementation spike, not a production approval: its upstream documentation says Android and iOS targets are built but not tested and labels them unsupported. That does not meet this project’s requirement for supported, verified Android and iOS paths. Do not enable new group-E2EE features until a provider and the gates below are approved. The current Android path still uses a legacy pairwise Signal adapter; it is not an MLS group implementation and is not approved for the new group scope. The custom iOS implementation has been removed in this change. A missing provider must not downgrade a send to plaintext.

This is a conditional selection: RFC 9420 is the group-protocol target; library selection and production activation remain blocked pending platform, security, maintenance, and licensing evidence. If those checks cannot be met, stop the rollout rather than substitute homegrown cryptography.

## Repository findings on main

- The common SignalProtocolManager interface exposes identity/pre-key, session, encrypt, and decrypt operations, but has no MLS group/epoch API or versioned message envelope.
- Android wires org.whispersystems:signal-protocol-android:2.8.1. Its adapter uses SessionBuilder and SessionCipher with a fixed device id and pairwise recipient sessions. Maven Central dates 2.8.1 to 2019-11-02. This is not evidence of the required MLS group behavior or a cross-platform implementation.
- IosSignalProtocolManager was an unreferenced, hand-built substitute. It returned the literal MOCK_SIGNATURE, ignored the bundle signature/identity during session setup, derived a raw Curve25519 secret, reused slices of that secret as an AES-CBC key and fixed IV, and used CBC with padding but no authentication. It wrote private key strings through multiplatform Settings without using the project’s iOS Keychain API. It is not an acceptable E2EE implementation and has been removed. The shared CoreCrypto interop remains because other iOS security code uses it; only the custom Curve25519 declarations are removed.
- KMPHelper.swift passes a null manager. Before this change, SendMessageUseCase logged that condition and sent plaintext. It now returns a failure without calling the repository. The iOS path is intentionally disabled until a reviewed provider exists.
- Even with a non-null manager, the current send path puts text into per-recipient ciphertext but forwards attachments and metadataContainer separately to the repository, leaving those fields service-visible. ChatEncryptionHelper also treats non-JSON content as plaintext. Message envelopes, legacy/pre-E2EE marking, attachment encryption, and receiver-side downgrade handling remain explicit follow-up gates; this PR does not claim to complete them.
- The iOS local SecurityCipher is separate at-rest code, not an E2EE provider. Its current CBC implementation is unauthenticated and returns plaintext on some encryption failures; it must not be reused for message E2EE and needs a separate vetted fail-closed storage decision.
- The observed DTO/API fields include message content, type, media URL, attachments, metadata, reply target, expiry, sender/chat ids, delivery/read state and timestamps; chat name/description/avatar and group flags; and participant user/admin/mute/archive/read state. These are code-level observations, not a verified database schema. No supabase/migrations directory is present on main; supabase/README.md notes earlier migration/test files were removed. Actual deployed schema and authorization policies were not verified, and this PR makes no schema changes.

## Threat model and guarantees

Protect private content and authenticated group state from a malicious or compromised delivery service, network attacker, unauthorized outsider, identity substitution, replay/reordering, and a removed device after the removal commit has advanced the group epoch. MLS state must be durably and atomically persisted; invalid authentication, unknown versions, stale/forked epochs, missing keys, unsupported clients, or failed commits block send/decrypt as applicable.

A compromised unlocked endpoint, a malicious authorized member, recipient screenshots/copies, coercion, and material already copied by a lost device are not solved by E2EE or remote revocation. The service can still observe the minimum identifiers needed for authentication/delivery, ciphertext sizes, timing/frequency, and any routing/delivery metadata left outside MLS. RFC 9420 and RFC 9750 explicitly discuss group metadata and the application/infrastructure responsibilities around MLS.

## Content and metadata classification

| Data | Required treatment | Service visibility |
| --- | --- | --- |
| Message text, captions, semantic message type, reply/quote context, structured metadata, poll/event/vote/announcement/mention content, pin/search data, notification preview | Put inside authenticated MLS application data; search only on device after decryption | Ciphertext only; never duplicate plaintext in DTO fields, logs, push, analytics, or server cache |
| Attachment bytes, filename, caption, MIME/type, dimensions/duration, media-group semantics, and decryption key | Encrypt bytes on the sender before upload; place key and descriptive fields inside MLS application data | Opaque object handle and ciphertext size may remain visible; no plaintext media URL or descriptive attachment DTO |
| Group name, description, photo reference, client-facing roster, roles, membership events, and settings | Encrypt in MLS application data where possible; enforce authorization separately at the trusted service boundary | Backend access-control membership/routing records may remain service-visible only when required, tightly access-controlled, and documented as metadata leakage |
| Authentication/delivery routing, opaque group route, account/device identifiers, transport version, ordering/delivery/read receipts, rate-limit/abuse fields, ciphertext length and timestamps | Minimize, avoid content-bearing values, and authenticate/authorize at the backend | Explicitly server-visible operational metadata; minimize retention and access |
| Existing unencrypted history | Preserve readability and explicitly label pre-E2EE; never reinterpret as encrypted or share to a new member by default | Existing stored values remain server-readable until a separately reviewed migration/policy changes that fact |

This classification applies to the fields observed in current DTOs and call paths. Confirm the actual Supabase schema, RLS, storage policies, push payloads, and retention before any schema or production-policy change.

## Wire, identity, and key lifecycle requirements

- Use only RFC 9420 MLS messages and the selected library’s standard serialization. The transport will carry a versioned outer envelope (wire version plus protocol identifier plus opaque MLS payload); the service treats its payload as opaque. Clients reject unknown versions/protocols and malformed/authentication-failing messages. No parser, key, or version error may retry as plaintext. Exact field names/encoding are finalized with the vetted provider and interoperability review, not invented in this ADR.
- Every physical device is a distinct cryptographic client with an authenticated account/device binding. Key changes must be visible and verifiable; never silently replace a peer identity. The authentication/credential service, verification UX, and multi-device binding are application responsibilities and are not supplied by RFC 9420 alone.
- Create/update/add/remove membership only through authenticated MLS proposals/commits under server-enforced authorization. A removal must advance the epoch before later messages are sent. If state, membership, or commit processing is not ready, pause private sending; never use the old epoch or plaintext as fallback.
- New members receive only the current group state and history allowed by the separately disclosed policy. No old epoch keys or history are shared by default. Any history sharing must be authorized and re-encrypted client-side under the policy.
- Keep group state updates atomic with the send/receive queue and reject replay, stale epochs, invalid commits, or forks according to the selected implementation. Attachments use a reviewed library-supported key-derivation/encryption API; no bespoke KDF or unauthenticated mode.
- Backups, if later approved, are encrypted with a user-controlled recovery secret that never reaches the server. Lost-device revocation cannot erase content or keys already copied to that device.

## Candidate evaluation

| Candidate | Evidence | Decision |
| --- | --- | --- |
| MLS v1 / RFC 9420 with OpenMLS | IETF Standards Track group protocol with epochs, membership evolution, forward secrecy and post-compromise security. OpenMLS documents Rust implementation, MIT licensing, maintenance by Phoenix R&D and CE Labs, and Android/iOS targets as built but not tested/unsupported. Its May 2026 audit announcement reports eight findings, including one High, says fixes shipped for versions 0.8.1 and 0.7.3 except one Low still in progress. Docs.rs listed 0.9.1 at this review; that does not prove mobile production support or that a selected dependency/provider tree is audit-free. | **Selected protocol target; provider remains NO-GO.** Verify the pinned version’s advisories/audit fixes, cryptographic provider, state storage, FFI, licensing, and Android/iOS runtime/interop before approval. |
| Current Android legacy Signal adapter | Project uses 2.8.1 (Maven Central publication dated 2019-11-02); source implements pairwise sessions, not MLS group state. | It remains wired in the current Android path, but is not approved as the group protocol or for the new group scope; separately review migration and compatibility. |
| Signal libsignal | Official repository exposes Java/Swift wrappers over Rust, but explicitly says use outside Signal is unsupported; repository is AGPLv3 and APIs/bridges may change. | Do not adopt without product/legal review and demonstrated group/platform fit. |
| Current custom iOS substitute | Placeholder signature, missing bundle verification, static CBC key/IV, unauthenticated encryption and non-Keychain private-key storage. | Rejected and removed. No replacement crypto is improvised. |

## Migration and rollout

1. Keep legacy history readable and mark it pre-E2EE in any future verified schema/API/UI change; do not infer encryption from JSON shape or content type.
2. Before new group creation/cutover, select and pin a maintained provider, license every transitive dependency, build and exercise it on supported Android and iOS devices, and prove interoperability with an independent MLS implementation.
3. Add versioned client/server migrations only after locating the repository’s real migration workflow and verifying actual deployed schema and RLS. Server stores/delivers opaque ciphertext and enforces membership/admin authorization; it never receives group decryption secrets.
4. Integrate per-device identity/verification, durable MLS state, fail-closed receive/send, attachment encryption, privacy-safe notifications, recovery and lost-device behavior before enabling any private group feature.
5. Require an independent security review of the integrated design and implementation. Do not claim an audit unless that review actually occurs.

## Go/no-go checklist

- [ ] A maintained provider and production cipher suite are pinned; maintenance, security advisories, provider dependencies, licensing, and platform API support are reviewed.
- [ ] Android and iOS builds and runtime/interop checks pass on supported devices; unsupported web/desktop/WASM clients cannot send or receive private content.
- [ ] Account-to-device identity binding, verification, key changes, multi-device lifecycle, durable/atomic state, backup/recovery, and lost-device response are reviewed.
- [ ] Membership permissions are enforced by trusted backend boundaries; add/remove/update commits rotate group state before subsequent sends.
- [ ] Adversarial tests cover tampering, invalid signatures/credentials, identity substitution, missing keys, replay/reordering, stale/forked epochs, unsupported versions, provider/storage failures, attachments, and no downgrade on Android and iOS.
- [ ] No private plaintext appears in service-visible fields, attachments, logs, push, analytics, or cache; legacy data is explicitly pre-E2EE.
- [ ] Independent security review is complete and all blocking findings are resolved; production feature activation is explicitly approved.

**Current result:** NO-GO. None of the implementation/platform/adversarial-test gates above is demonstrated by this ADR. This change does not add or run tests, as explicitly requested for this task.

## Primary references

- IETF RFC 9420, The Messaging Layer Security (MLS) Protocol: https://www.rfc-editor.org/rfc/rfc9420.html
- IETF RFC 9750, The Messaging Layer Security (MLS) Architecture: https://www.rfc-editor.org/rfc/rfc9750.html
- OpenMLS upstream README (platform support, crypto-provider model, maintainers, license): https://github.com/openmls/openmls/blob/main/README.md
- OpenMLS current crate documentation: https://docs.rs/openmls/latest/openmls/
- OpenMLS audit announcement and report: https://blog.openmls.tech/posts/2026-05-27-independent-audit/ and https://blog.openmls.tech/SRL-OpenMLS_security_assurance_assessment.pdf
- OpenMLS security advisories: https://github.com/openmls/openmls/security/advisories
- Signal libsignal upstream README/license/support statement: https://github.com/signalapp/libsignal
- Existing Android artifact publication record: https://repo1.maven.org/maven2/org/whispersystems/signal-protocol-android/2.8.1/
