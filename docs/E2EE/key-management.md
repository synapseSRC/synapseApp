# E2EE Key Management

Identity keys, signed pre-keys, one-time pre-keys, key bundle synchronization, session establishment, key rotation, and identity mismatch behavior as implemented in Synapse Social.

## Key types

| Key | Algorithm | Purpose | Storage | Lifecycle |
|-----|-----------|---------|---------|-----------|
| **Identity key** | Curve25519 (`IdentityKeyPair`) | Long-term signing identity; signs the signed pre-key. | `AndroidSignalStore` (`identity_key_pair` in `EncryptedSharedPreferences`); public half published in `signal_identity_keys` / `user_public_keys`. | Generated once per install; reused across initializations (`generateIdentityAndKeys` reuses `store.identityKeyPair` if present). |
| **Signed pre-key** | Curve25519 (`SignedPreKeyRecord`) | Medium-term key used in X3DH; signed by the identity key so recipients can authenticate it. | `AndroidSignalStore` (`signed_prekey_<id>`); public half + signature published. | Regenerated on each `generateIdentityAndKeys()` call (ID derived from current time). |
| **One-time pre-keys** | Curve25519 (`PreKeyRecord`) | Ephemeral keys consumed once during X3DH session establishment to provide forward secrecy. | `AndroidSignalStore` (`prekey_<id>`); public halves published. | Generated in batches of 100 (IDs 1–100); claimed and deleted server-side via `claim_one_time_pre_key` RPC; removed locally when consumed by libsignal session processing. |
| **Registration ID** | Int | Random ID identifying the device/installation in the Signal protocol. | `registration_id` in `EncryptedSharedPreferences`. | Generated once via `KeyHelper.generateRegistrationId(false)`; persisted. |
| **Device ID** | Int (constant `1`) | Signal address device identifier. | Hardcoded `DEVICE_ID = 1` in `AndroidSignalProtocolManager`. | Fixed at 1 — no multi-device key separation at the Signal address level; multi-device is handled by encrypting for the sender userId separately. |

## Key generation

### Identity + signed pre-key (`generateIdentityAndKeys`)

Source: `AndroidSignalProtocolManager.generateIdentityAndKeys()`

1. If `AndroidSignalStore.hasIdentity()` → reuse the existing `identityKeyPair` and `registrationId`.
2. Otherwise → generate a fresh `IdentityKeyPair.generate()` and `KeyHelper.generateRegistrationId(false)`, then `storeLocalIdentity(pair, registrationId)`.
3. Generate a signed pre-key:
   - `signedPreKeyId = (System.currentTimeMillis() / 1000 % 0x00FFFFFF).toInt()`
   - `Curve.generateKeyPair()` → signed pre-key pair.
   - `identityKeyPair.privateKey.calculateSignature(signedPreKey.publicKey.serialize())` → signature.
   - Store as `SignedPreKeyRecord` in `AndroidSignalStore`.
4. Return `SignalIdentityKeys(registrationId, identityKey, signedPreKeyId, signedPreKey, signedPreKeySignature)` (all public material Base64-encoded).

### One-time pre-keys (`generateOneTimePreKeys`)

Source: `AndroidSignalProtocolManager.generateOneTimePreKeys(startId, count)`

- Generates `count` key pairs via `Curve.generateKeyPair()`, stores each as `PreKeyRecord(keyId, keyPair)` in `AndroidSignalStore`, returns `SignalOneTimePreKey` list with public keys Base64-encoded.
- Called with `startId = 1, count = 100` during E2EE initialization.

## Local key storage (`AndroidSignalStore`)

`AndroidSignalStore` implements libsignal's `SignalProtocolStore` (`IdentityKeyStore`, `PreKeyStore`, `SignedPreKeyStore`, `SessionStore`, `SenderKeyStore`, `KyberPreKeyStore`) backed by `EncryptedSharedPreferences`:

- **File:** `signal_secure_store`
- **Master key:** `MasterKey.Builder` with `AES256_GCM` scheme, backed by the Android Keystore.
- **Key names:** encrypted with `AES256_SIV`.
- **Values:** encrypted with `AES256_GCM`.

Key material persisted under these preference keys:

| Preference key | Contents |
|----------------|----------|
| `identity_key_pair` | Serialized `IdentityKeyPair` (private + public). |
| `registration_id` | Registration ID Int. |
| `last_key_rotation` | Timestamp (ms) of last identity generation. |
| `identity_<userId>_<deviceId>` | Recipient identity keys (trust store). |
| `prekey_<id>` | One-time pre-key records. |
| `signed_prekey_<id>` | Signed pre-key records. |
| `signed_prekey_ids` | Set of stored signed pre-key IDs. |
| `session_<userId>_<deviceId>` | Double Ratchet session records. |
| `senderkey_<...>` | Group sender keys (stored, not used for 1:1 messages). |
| `kyber_prekey_<id>` | PQC Kyber pre-keys (stored, not used for 1:1 messages). |

`hasIdentity()` returns true only when `identity_key_pair` is present **and** `registration_id != 0`.

## Remote public-key storage (Supabase)

### Tables

| Table | Purpose |
|-------|---------|
| `user_public_keys` | Stores each user's current public key bundle (JSON string in `public_key` column). Accessed via `ChatKeyDataSource`. |
| `signal_identity_keys` | Stores identity-key DTOs (`user_id`, `device_id`, `registration_id`, `identity_key`, `signed_pre_key_id`, `signed_pre_key`, `signed_pre_key_signature`). Accessed via `SignalRepository`. |

### Data flow

- **Publish:** `ChatEncryptionHelper.initializeE2EE()` → `ChatKeyDataSource.uploadUserPublicKey(bundleStr)` → `user_public_keys.upsert(UserPublicKeyDto(userId, publicKey))`.
- **Fetch:** `ChatEncryptionHelper.ensureSession(userId)` → `ChatKeyDataSource.getUserPublicKey(userId)` → `SELECT * FROM user_public_keys WHERE user_id = ? LIMIT 1`.
- **Claim one-time pre-key (session setup with recipient):** `SignalRepository.fetchPreKeyBundle(userId)` → reads `signal_identity_keys` for the identity, then calls RPC `claim_one_time_pre_key(target_user_id => userId)` to atomically claim and remove one one-time pre-key. Returns a `PreKeyBundle` combining the identity record and claimed pre-key.

> **Note:** `ChatEncryptionHelper.ensureSession` reads from `user_public_keys` (not `signal_identity_keys`) and decodes the `publicKey` column as a `PreKeyBundle`. `SignalRepository.fetchPreKeyBundle` is a separate code path that joins `signal_identity_keys` with the `claim_one_time_pre_key` RPC.

## Key bundle synchronization

`initializeE2EE` (in `ChatEncryptionHelper`) governs sync between local keys and remote keys:

| Local keys | Remote keys | Identity match | Action |
|------------|-------------|----------------|--------|
| Yes | Yes | Yes | Fully initialized. Logs key-rotation recommendation if keys older than 30 days. |
| Yes | No | N/A | Re-upload bundle built from local keys (fresh one-time pre-keys generated). |
| Yes | Yes | **No** | **Identity mismatch:** `deleteAllSessions()`, then re-upload bundle (overwrites remote). |
| No | Yes | N/A | Warn; regenerate everything locally (generate identity + pre-keys, upload). |
| No | No | N/A | Fresh setup: generate identity + 100 one-time pre-keys, upload bundle. |

### Identity mismatch handling

Detected by comparing `signalProtocolManager.getLocalIdentityKey()` with `remoteBundle.identityKey` decoded from the remote `user_public_keys` row.

On mismatch:
1. Log a warning (`E2EE_INIT: Identity mismatch detected!`).
2. Call `signalProtocolManager.deleteAllSessions()` — clears all Double Ratchet sessions because they are bound to the old identity.
3. Re-upload the locally built bundle via `uploadUserPublicKey`.

This is the app's **trust-on-first-use / identity-change resolution**: a changed remote identity is silently accepted and overwritten without user notification. See security-considerations.md.

## Session establishment

### Outgoing (X3DH)

When sending a message to a recipient with no existing session (`ChatEncryptionHelper.ensureSession` / `SendMessageUseCase`):

1. Fetch the recipient's public key bundle from `user_public_keys`.
2. Decode as `PreKeyBundle`.
3. `signalProtocolManager.processPreKeyBundle(userId, bundle)` → `SessionBuilder(store, address).process(nativeBundle)` establishes a libsignal session via X3DH.
4. `SessionCipher(store, address).encrypt(message)` produces a `CiphertextMessage` (type 2 Whisper if a session exists, or type 3 PreKeySignalMessage for the first message consuming a one-time pre-key).

### Per-recipient fan-out (`SendMessageUseCase.invoke`)

1. Serialize the plaintext message to JSON: `{content, mediaUrl, metadata, attachments}`.
2. For each recipient (concurrent via `coroutineScope { async { ... } }.awaitAll()`):
   - `repository.ensureSession(userId)` — establish X3DH session if absent.
   - `signalProtocolManager.encryptMessage(userId, contentBytes)` → `EncryptedMessage`.
3. Build `encryptedPayload = JsonObject { userId → EncryptedMessage }`.
4. Also encrypt for the sender (`currentUserId`) if not already a recipient → multi-device parity.
5. Persist via `repository.sendMessage(content = encryptedPayload, senderPlaintext = jsonPayload)`.

If **any** recipient encryption fails, the entire message is aborted (`Result.failure`) — no partial unencrypted send.

## Key rotation

- `checkKeyRotationNeeded(thresholdDays = 30)` reads `last_key_rotation` from `AndroidSignalStore` and returns `true` when `(now - lastRotation) >= thresholdDays`.
- Rotation is **recommended (logged as warning) but not enforced automatically**. There is no automatic identity rotation flow; `initializeE2EE` only warns when rotation is due.
- A manual rotation would require re-running `initializeE2EE` after clearing local identity, which is not exposed as a user-facing action.

> **Implemented:** rotation check + warning log only. **Not implemented:** automatic rotation, user-triggered rotation, rotation notification to contacts.

## Multi-device handling

- `DEVICE_ID = 1` is hardcoded; every installation uses the same Signal address `(userId, 1)`.
- Multi-device parity for the **same account** is achieved at the application layer: `SendMessageUseCase` encrypts an additional envelope addressed to `currentUserId` (the sender) so the sender's other logged-in devices can decrypt.
- There is no per-device key bundle; all devices of an account share the same identity/pre-keys stored locally.

## Limitations

- One-time pre-keys are generated in a fixed batch of 100 at initialization. There is no background replenishment when the pool runs low; `claim_one_time_pre_key` server-side removes claimed keys, but the client does not refill.
- Signed pre-key is regenerated on every `generateIdentityAndKeys()` call (even when reusing identity), which may create orphan signed pre-key records locally.
- `deviceId = 1` hardcoded means no cryptographic separation between multiple devices of the same account at the Signal protocol layer.
