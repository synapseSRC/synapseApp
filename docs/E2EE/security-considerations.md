# E2EE Security Considerations

Security properties of the Signal Protocol implementation in Synapse Social, clearly distinguishing **implemented** behavior from **recommended** improvements. Class or method names alone do not imply a security guarantee; this document states what is actually enforced in code.

## Private-key storage

### Implemented

- Identity key pairs, pre-keys, sessions, and sender keys are stored in `AndroidSignalStore` backed by `EncryptedSharedPreferences` (`signal_secure_store`).
- Master key: `MasterKey` with `AES256_GCM` scheme backed by the Android Keystore.
- Preference key names encrypted with `AES256_SIV`; values encrypted with `AES256_GCM`.
- Private key material never leaves the device and is never transmitted to Supabase.

### Limitations

- `androidx.security.crypto` (`EncryptedSharedPreferences`, `MasterKey`) is **deprecated upstream** with no in-library successor (noted in `AndroidSignalStore`). There is no migration path defined; re-keying every account's Signal identity would be required to move to a successor.
- On non-Android platforms there is no `SignalProtocolManager` implementation (iOS placeholder not present), so E2EE crypto is Android-only in practice.
- No hardware-bound key attestation; keys are software-protected by the Keystore-backed master key, not by a strongbox/TEE-bound key where available.

## Identity verification

### Implemented

- A 60-digit safety number is derived from both parties' identity keys (`SignalSafetyNumberGenerator`, Signal's numeric fingerprint: SHA-512, 5200 iterations, version 0) and shown in the chat's security sheet with a scannable QR code.
- `AndroidSignalStore.isTrustedIdentity()` compares the presented key with the stored one through `IdentityTrustPolicy`: a first key is accepted (trust-on-first-use), an identical key is trusted, and a **changed key returns `false`**, so libsignal refuses to build a session or decrypt with it. The stored key is never overwritten by the change.
- A detected change is recorded as a pending identity change in the encrypted store and surfaces as `ChatVerificationInfo.identityChanged`. Accepting it (after comparing the new code) advances the stored key, drops the previous verification and deletes the stale session.
- `AndroidSignalStore.saveIdentity()` no longer clobbers a changed identity either, so the change cannot disappear between calls.
- Local/remote identity comparison in `initializeE2EE` detects mismatch and triggers `deleteAllSessions()` + re-upload, but this is a **sync-repair action** for the local account, not an authentication ceremony; peers must re-verify that account afterwards.

### Trust-on-first-use behavior

- First contact with a recipient: their published identity key is accepted and a session is established without verification. This is classic trust-on-first-use (TOFU) — vulnerable to a man-in-the-middle who controls the server's `user_public_keys` table at first contact.
- Because Supabase is the source of public keys and there is no secondary verification channel, a compromised server can present attacker-controlled keys to both parties undetected.

### Identity changes

- Remote identity mismatch → local sessions cleared, new bundle uploaded, no user prompt.
- No safety-number change UI, no "security code changed" notification, no option to block messages from a changed identity.

## Cryptographic algorithm notes

| Property | Status |
|----------|--------|
| X3DH session establishment | Implemented via libsignal `SessionBuilder.process(PreKeyBundle)`. |
| Double Ratchet forward secrecy / future secrecy | Implemented via libsignal `SessionCipher`. |
| Per-recipient message encryption | Implemented (fan-out `EncryptedMessage` per recipient + sender). |
| One-time pre-key consumption | Implemented server-side via `claim_one_time_pre_key` RPC; client-side consumption via libsignal session processing. |
| Authenticated encryption | Provided by libsignal `SessionCipher` (AES-256-CBC + HMAC-SHA256 in Signal's format). |
| Post-quantum (Kyber) pre-keys | `AndroidSignalStore` implements `KyberPreKeyStore` and records exist, but 1:1 message encryption does not use Kyber; this is not part of the active signal path. |
| Group messaging E2EE (Sender Keys) | `SenderKeyStore` methods exist in `AndroidSignalStore`, but group **messages** are fan-out encrypted per recipient via 1:1 sessions (not a true Sender Key group protocol). Group **metadata** is fan-out encrypted via `GroupMetadataCipher`. |

## Potential security weaknesses

1. **Verification is opt-in.** TOFU is still the default on first contact, and nothing forces two people to compare safety numbers; a compromised public-key server can therefore still MITM a conversation whose codes were never compared.
2. **A change is refused, but quietly on the peer's side.** A changed key blocks session work on this device and is visible in the chat's security sheet; there is no notification, no chat-list badge and no message telling the other party. Until the UI surfaces it more widely, a user only notices when they open the security sheet or a message fails to send.
3. **Centralized key directory.** Supabase `user_public_keys` is a single point of trust for binding user IDs to keys. No key transparency, no pinned keys, no out-of-band fingerprint.
4. **One-time pre-key exhaustion.** Pre-keys are generated once in a batch of 100 at initialization with no background replenishment. If the pool is depleted, new sessions cannot be established via X3DH until keys are re-uploaded (which only happens on mismatch or remote-missing paths).
5. **Hardcoded `deviceId = 1`.** No cryptographic separation between multiple devices of the same account at the Signal address level.
6. **Deprecated crypto library.** `androidx.security.crypto` is deprecated; no migration planned.
7. **Plaintext sender copy stored server-side.** The sender's plaintext JSON payload is stored as one envelope entry addressed to `currentUserId`, so the sender's devices can read without ratchet consumption. This is by design (multi-device), but means the sender's own copy is addressable alongside recipients.
8. **Edit path encryption.** `encryptEditPayload` re-encrypts only for `otherParticipants` plus the sender; same session-establishment assumptions apply.
9. **Error handling in decryption.** On permanent decrypt failure, the message displays `decryption_failed` and a placeholder; no user-visible path to recover or re-request keys.

## Group metadata E2EE

- `GroupMetadataCipher` encrypts group name/description/avatar per participant using the editor's existing 1:1 Signal sessions.
- A fresh PreKey envelope is forced for every recipient (`deleteSession` + `ensureSession` + `encryptMessage` + `deleteSession` per recipient) so a member who never ran X3DH can still decrypt independently.
- Sessions are deliberately dropped after each recipient to avoid "couldn't decrypt message #1" bootstrapping issues.
- Graceful degradation: if `signalProtocolManager` is null or encryption fails, the caller falls back to legacy plaintext columns — group metadata may be stored plaintext when E2EE is unavailable.

## Recommendations (not implemented)

- ~~Add a safety-number / fingerprint verification UI.~~ Shipped (see [Identity verification](./verification.md)); still missing wider surfacing of a changed identity (notification, chat-list badge, in-thread warning).
- ~~Replace `isTrustedIdentity` always-true with a trust store that prompts on change.~~ Shipped: a changed key is refused and recorded; the prompt is the security sheet's re-verification path, which is not yet reachable from the conversation list.
- Extend verification to group chats (fan-out recipients), where each member needs an individual comparison.
- Add key transparency or certificate pinning for the public-key directory.
- Replenish one-time pre-keys in the background when the pool falls below a threshold.
- Add automatic or user-triggered key rotation with contact notification.
- Migrate off deprecated `androidx.security.crypto` to a maintained encrypted-storage solution (e.g., EncryptedSharedPreferences successor or platform Keychain/Keystore abstraction).
- Add hardware-backed (StrongBox/TEE) key storage where available.
- Define behavior for permanently undecryptable messages (re-request from sender, key-reset ceremony).

## What is NOT a security boundary

- Supabase Row Level Security / policies are not relied upon for E2EE confidentiality; ciphertext confidentiality rests on client-side keys only.
- `EncryptedPayloadDetector` is a classification heuristic, not a security control.
- `Result<Unit>` error returns and `Napier`/`Logger` calls do not alter the cryptographic trust model.
