package com.synapse.social.studioasinc.shared.data.crypto.store

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// ── libsignal-android (org.signal:libsignal-android) ─────────────────────────
// Replaces archived org.whispersystems:signal-protocol-android:2.8.x.
// Key API change: IdentityKey(ByteArray) — the 'offset' second parameter was removed.
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore
import org.signal.libsignal.protocol.state.SignalProtocolStore

import org.signal.libsignal.protocol.InvalidKeyIdException

class AndroidSignalStore(context: Context, private val sharedPreferences: SharedPreferences? = null) : SignalProtocolStore {
    private val prefs: SharedPreferences by lazy {
        sharedPreferences ?: try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "signal_secure_store",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize encrypted storage", e)
        }
    }

    private fun SharedPreferences.Editor.commitOrThrow(errorMessage: String) {
        if (!this.commit()) {
            throw IllegalStateException(errorMessage)
        }
    }

    // ─── IdentityKeyStore ────────────────────────────────────────────────────

    override fun getIdentityKeyPair(): IdentityKeyPair {
        val encoded = prefs.getString("identity_key_pair", null) ?: throw IllegalStateException("No identity key pair")
        return IdentityKeyPair(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun getLocalRegistrationId(): Int {
        return prefs.getInt("registration_id", 0)
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = "identity_${address.name}_${address.deviceId}"
        val existing = prefs.getString(key, null)
        if (existing != null) {
            // new IdentityKey(bytes) — no offset parameter in libsignal-android
            val existingKey = IdentityKey(Base64.decode(existing, Base64.NO_WRAP))
            if (existingKey != identityKey) {
                prefs.edit()
                    .putString(key, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
                    .commitOrThrow("Failed to save identity key")
                return true
            }
            return false
        }
        prefs.edit()
            .putString(key, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to save identity key")
        return true
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction?
    ): Boolean {
        val key = "identity_${address.name}_${address.deviceId}"
        val existing = prefs.getString(key, null)
        if (existing == null) return true
        val existingKey = IdentityKey(Base64.decode(existing, Base64.NO_WRAP))
        if (existingKey != identityKey) {
            prefs.edit()
                .putString(key, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
                .commitOrThrow("Failed to save identity key")
        }
        return true
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = "identity_${address.name}_${address.deviceId}"
        val existing = prefs.getString(key, null) ?: return null
        return IdentityKey(Base64.decode(existing, Base64.NO_WRAP))
    }

    fun saveIdentityKeyPair(identityKeyPair: IdentityKeyPair) {
        prefs.edit()
            .putString("identity_key_pair", Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to save identity key pair")
    }

    fun saveLocalRegistrationId(registrationId: Int) {
        prefs.edit()
            .putInt("registration_id", registrationId)
            .commitOrThrow("Failed to save registration id")
    }

    // ─── PreKeyStore ─────────────────────────────────────────────────────────

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val encoded = prefs.getString("prekey_$preKeyId", null) ?: throw InvalidKeyIdException("No such prekey: $preKeyId")
        return PreKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        prefs.edit()
            .putString("prekey_$preKeyId", Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to store prekey")
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return prefs.contains("prekey_$preKeyId")
    }

    override fun removePreKey(preKeyId: Int) {
        prefs.edit()
            .remove("prekey_$preKeyId")
            .commitOrThrow("Failed to remove prekey")
    }

    // ─── SignedPreKeyStore ────────────────────────────────────────────────────

    private val signedPreKeyIdsKey = "signed_prekey_ids"

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val encoded = prefs.getString("signed_prekey_$signedPreKeyId", null)
            ?: throw InvalidKeyIdException("No such signed prekey: $signedPreKeyId")
        return SignedPreKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> {
        val ids = prefs.getStringSet(signedPreKeyIdsKey, emptySet()) ?: emptySet()
        val list = mutableListOf<SignedPreKeyRecord>()
        for (idStr in ids) {
            try {
                list.add(loadSignedPreKey(idStr.toInt()))
            } catch (_: Exception) { /* skip corrupt entries */ }
        }
        return list
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        prefs.edit()
            .putString("signed_prekey_$signedPreKeyId", Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to store signed prekey")

        val ids = prefs.getStringSet(signedPreKeyIdsKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(signedPreKeyId.toString())
        prefs.edit().putStringSet(signedPreKeyIdsKey, ids).commitOrThrow("Failed to update signed prekey ids")
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return prefs.contains("signed_prekey_$signedPreKeyId")
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        prefs.edit()
            .remove("signed_prekey_$signedPreKeyId")
            .commitOrThrow("Failed to remove signed prekey")

        val ids = prefs.getStringSet(signedPreKeyIdsKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.remove(signedPreKeyId.toString())
        prefs.edit().putStringSet(signedPreKeyIdsKey, ids).commitOrThrow("Failed to update signed prekey ids")
    }

    // ─── SessionStore ─────────────────────────────────────────────────────────

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val key = "session_${address.name}_${address.deviceId}"
        val encoded = prefs.getString(key, null)
        return if (encoded != null) {
            SessionRecord(Base64.decode(encoded, Base64.NO_WRAP))
        } else {
            SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String?): MutableList<Int> {
        return mutableListOf()
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val key = "session_${address.name}_${address.deviceId}"
        prefs.edit()
            .putString(key, Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to store session")
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val key = "session_${address.name}_${address.deviceId}"
        return prefs.contains(key)
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = "session_${address.name}_${address.deviceId}"
        prefs.edit().remove(key).commitOrThrow("Failed to delete session")
    }

    fun deleteIdentity(address: SignalProtocolAddress) {
        val key = "identity_${address.name}_${address.deviceId}"
        prefs.edit().remove(key).commitOrThrow("Failed to delete identity key")
    }

    override fun deleteAllSessions(name: String?) {
        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        var deletedCount = 0

        val prefix = "session_"
        for (key in allKeys) {
            if (key.startsWith(prefix)) {
                if (name == null) {
                    editor.remove(key)
                    deletedCount++
                } else {
                    val lastUnderscoreIndex = key.lastIndexOf('_')
                    if (lastUnderscoreIndex > prefix.length) {
                        val storedNameLength = lastUnderscoreIndex - prefix.length
                        if (storedNameLength == name.length &&
                            key.regionMatches(prefix.length, name, 0, storedNameLength)
                        ) {
                            editor.remove(key)
                            deletedCount++
                        }
                    }
                }
            }
        }

        if (deletedCount > 0) {
            editor.commitOrThrow("Failed to delete sessions")
        }
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        val result = mutableListOf<SessionRecord>()
        for (addr in addresses) {
            val key = "session_${addr.name}_${addr.deviceId}"
            val encoded = prefs.getString(key, null)
            if (encoded != null) {
                result.add(SessionRecord(Base64.decode(encoded, Base64.NO_WRAP)))
            }
        }
        return result
    }

    // ─── SenderKeyStore ───────────────────────────────────────────────────────

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID,
        record: org.signal.libsignal.protocol.groups.state.SenderKeyRecord
    ) {
        val key = "senderkey_${sender.name}_${sender.deviceId}_${distributionId}"
        prefs.edit()
            .putString(key, Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to store sender key")
    }

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID
    ): org.signal.libsignal.protocol.groups.state.SenderKeyRecord? {
        val key = "senderkey_${sender.name}_${sender.deviceId}_${distributionId}"
        val encoded = prefs.getString(key, null) ?: return null
        return try {
            org.signal.libsignal.protocol.groups.state.SenderKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
        } catch (_: Exception) {
            null
        }
    }

    // ─── KyberPreKeyStore ─────────────────────────────────────────────────────

    override fun loadKyberPreKey(kyberPreKeyId: Int): org.signal.libsignal.protocol.state.KyberPreKeyRecord {
        val encoded = prefs.getString("kyber_prekey_$kyberPreKeyId", null)
            ?: throw org.signal.libsignal.protocol.InvalidKeyIdException("No Kyber prekey: $kyberPreKeyId")
        return org.signal.libsignal.protocol.state.KyberPreKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun loadKyberPreKeys(): List<org.signal.libsignal.protocol.state.KyberPreKeyRecord> {
        val result = mutableListOf<org.signal.libsignal.protocol.state.KyberPreKeyRecord>()
        for (key in prefs.all.keys) {
            if (key.startsWith("kyber_prekey_") && !key.startsWith("kyber_prekey_used_")) {
                val encoded = prefs.getString(key, null)
                if (encoded != null) {
                    try {
                        result.add(
                            org.signal.libsignal.protocol.state.KyberPreKeyRecord(
                                Base64.decode(encoded, Base64.NO_WRAP)
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
        }
        return result
    }

    override fun storeKyberPreKey(
        kyberPreKeyId: Int,
        record: org.signal.libsignal.protocol.state.KyberPreKeyRecord
    ) {
        prefs.edit()
            .putString("kyber_prekey_$kyberPreKeyId", Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
            .commitOrThrow("Failed to store Kyber prekey")
    }

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean =
        prefs.contains("kyber_prekey_$kyberPreKeyId")

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int) {
        prefs.edit()
            .putBoolean("kyber_prekey_used_$kyberPreKeyId", true)
            .commitOrThrow("Failed to mark Kyber prekey used")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @get:JvmName("identityKeyPairHelper")
    val identityKeyPair: IdentityKeyPair
        get() = getIdentityKeyPair()

    fun hasIdentity(): Boolean {
        return prefs.contains("identity_key_pair") && prefs.getInt("registration_id", 0) != 0
    }

    fun storeLocalIdentity(identityKeyPair: IdentityKeyPair, registrationId: Int) {
        saveIdentityKeyPair(identityKeyPair)
        saveLocalRegistrationId(registrationId)
        prefs.edit()
            .putLong("last_key_rotation", System.currentTimeMillis())
            .commitOrThrow("Failed to save key rotation timestamp")
    }

    fun checkKeyRotationNeeded(thresholdDays: Int = 30): Boolean {
        val lastRotation = prefs.getLong("last_key_rotation", 0L)
        if (lastRotation == 0L) return false
        val daysSinceRotation = (System.currentTimeMillis() - lastRotation) / (1000L * 60 * 60 * 24)
        return daysSinceRotation >= thresholdDays
    }
}
