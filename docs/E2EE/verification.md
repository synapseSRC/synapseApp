# Identity Verification (Safety Numbers)

How a Synapse Android user checks that the identity key their chat actually uses belongs to the
person they think they are talking to, and what the app does when that key changes.

> **Trust model:** verification is a local decision stored on the device. It is never sent to
> Supabase, never synced between devices, and the peer is not told. Signal session work with a peer
> whose key changed stays refused until the user accepts the new key.

## Components

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `SignalSafetyNumberGenerator` | `app/.../data/crypto/SignalSafetyNumberGenerator.kt` | 60-digit Signal-style numeric fingerprint from both identity keys (SHA-512, 5200 iterations, version 0, six 5-digit groups per party, halves ordered). |
| `IdentityTrustPolicy` | `shared/src/commonMain/.../data/crypto/IdentityTrust.kt` | Pure decision between `FIRST_USE`, `UNCHANGED` and `CHANGED`; unit tested in `shared/src/commonTest`. |
| Trust state | `AndroidSignalStore` (`signal_secure_store`, EncryptedSharedPreferences) | Verified safety numbers, pending identity changes and known identity keys, encrypted with the same Keystore-backed master key as the Signal material. |
| Trust enforcement | `AndroidSignalStore.isTrustedIdentity()` / `saveIdentity()` | Refuses session work when a peer presents a key different from the stored one, and never overwrites the stored key. |
| Verification UI | `ChatEncryptionSheet` + `ChatVerificationRepositoryImpl` | Compares or scans the code, reports failure reasons, and stores the verified number. |

## Verify flow

1. `ChatVerificationRepositoryImpl.load(chatId)` reads the peer's published key (`user_public_keys`),
   the local identity key and any pending identity change, then derives the safety number.
2. `ChatVerificationInfo.isVerified` is true only when the stored verified number equals the number
   derived from the keys in use now and no change is pending, so a key change invalidates a
   verification automatically.
3. The user compares the number out of band or scans the peer's QR code
   (`synapse-safety-v1|<userId>|<identityKey>`). A scan counts only when the scanned user id matches
   the peer and the scanned key equals the peer's current key (`MessageDigest.isEqual`).
4. On a successful comparison the app accepts a pending identity change
   (`SignalProtocolManager.acceptIdentityChange(peerId)`) and stores the verified number in the
   encrypted store.

## Identity changes

- First sight of a peer key: `FIRST_USE` — accepted and stored (trust-on-first-use).
- Same key as stored: `UNCHANGED` — trusted.
- Different key: `CHANGED` — `isTrustedIdentity()` returns `false` (libsignal refuses to build the
  session or decrypt), the stored key is **kept**, and the new key is recorded as a pending change so
  the UI can ask for re-verification.
- Accepting the change advances the stored key, clears the previous verification, deletes the stale
  session and clears the pending marker, so the next message re-runs X3DH against the new key.

## What verification does not cover yet

- **Group chats.** Verification is one-to-one; a group conversation reports
  `NOT_ONE_TO_ONE_CHAT`. Group messages are fan-out encrypted per recipient, so each member would
  need an individual comparison.
- **Peer notification.** A changed key is not announced to the other party and is not raised as a
  notification; it is visible where verification is shown (the chat's security sheet).
- **Cross-device state.** The verified number is stored on the device that performed the comparison.
  A new device or a reinstall starts unverified, and verification is not part of the (Android-only)
  backup path.
- **Key transparency.** Identity keys still come from Supabase; verification detects a substituted
  key only when the two people actually compare codes.
- **Device separation.** `deviceId` is hardcoded to `1`, so verification covers one Signal address
  per account.
