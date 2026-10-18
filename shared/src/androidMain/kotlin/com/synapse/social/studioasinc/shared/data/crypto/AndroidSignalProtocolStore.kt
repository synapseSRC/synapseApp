package com.synapse.social.studioasinc.shared.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// ── libsignal-android (org.signal:libsignal-android) ─────────────────────────
// Replaces archived org.whispersystems:signal-protocol-android:2.8.x.
// IdentityKey(ByteArray) — the 'offset' second parameter was removed in new API.
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore

class AndroidSignalProtocolStore(context: Context) : SignalProtocolStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "signal_store_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── IdentityKeyStore ────────────────────────────────────────────────────

    override fun getIdentityKeyPair(): IdentityKeyPair {
        val serialized = prefs.getString("identity_key_pair", null)
            ?: throw IllegalStateException("IdentityKeyPair not generated")
        return IdentityKeyPair(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun getLocalRegistrationId(): Int {
        val id = prefs.getInt("local_registration_id", -1)
        if (id == -1) throw IllegalStateException("Local registration info not found")
        return id
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        prefs.edit()
            .putString(
                "identity_${address.name}_${address.deviceId}",
                Base64.encodeToString(identityKey.serialize(), Base64.DEFAULT)
            )
            .apply()
        return true
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        // Simple TOFU: Trust On First Use
        val existing = prefs.getString("identity_${address.name}_${address.deviceId}", null)
        if (existing == null) return true
        // IdentityKey(bytes) — no offset in libsignal-android
        val existingKey = IdentityKey(Base64.decode(existing, Base64.DEFAULT))
        return existingKey == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val existing = prefs.getString("identity_${address.name}_${address.deviceId}", null)
            ?: return null
        return IdentityKey(Base64.decode(existing, Base64.DEFAULT))
    }

    fun storeLocalIdentity(identityKeyPair: IdentityKeyPair, registrationId: Int) {
        prefs.edit()
            .putString("identity_key_pair", Base64.encodeToString(identityKeyPair.serialize(), Base64.DEFAULT))
            .putInt("local_registration_id", registrationId)
            .putLong("last_key_rotation", System.currentTimeMillis())
            .apply()
    }

    fun hasIdentity(): Boolean {
        return prefs.contains("identity_key_pair") && prefs.contains("local_registration_id")
    }

    fun getLastKeyRotation(): Long {
        return prefs.getLong("last_key_rotation", 0L)
    }

    fun checkKeyRotationNeeded(thresholdDays: Int = 30): Boolean {
        val lastRotation = getLastKeyRotation()
        if (lastRotation == 0L) return false
        val daysSinceRotation = (System.currentTimeMillis() - lastRotation) / (1000 * 60 * 60 * 24)
        return daysSinceRotation >= thresholdDays
    }

    // ─── PreKeyStore ─────────────────────────────────────────────────────────

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val serialized = prefs.getString("prekey_$preKeyId", null)
            ?: throw IllegalStateException("No prekey found")
        return PreKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        prefs.edit()
            .putString("prekey_$preKeyId", Base64.encodeToString(record.serialize(), Base64.DEFAULT))
            .apply()
    }

    override fun containsPreKey(preKeyId: Int): Boolean = prefs.contains("prekey_$preKeyId")

    override fun removePreKey(preKeyId: Int) {
        prefs.edit().remove("prekey_$preKeyId").apply()
    }

    // ─── SessionStore ─────────────────────────────────────────────────────────

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val serialized = prefs.getString("session_${address.name}_${address.deviceId}", null)
        return if (serialized != null) {
            SessionRecord(Base64.decode(serialized, Base64.DEFAULT))
        } else {
            SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> = emptyList()

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        prefs.edit()
            .putString(
                "session_${address.name}_${address.deviceId}",
                Base64.encodeToString(record.serialize(), Base64.DEFAULT)
            )
            .apply()
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return prefs.contains("session_${address.name}_${address.deviceId}")
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        prefs.edit().remove("session_${address.name}_${address.deviceId}").apply()
    }

    override fun deleteAllSessions(name: String) {
        // No-op in this secondary store; primary session management is in AndroidSignalStore.
    }

    // ─── SignedPreKeyStore ────────────────────────────────────────────────────

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val serialized = prefs.getString("signedprekey_$signedPreKeyId", null)
            ?: throw IllegalStateException("No signed prekey")
        return SignedPreKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> = emptyList()

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        prefs.edit()
            .putString("signedprekey_$signedPreKeyId", Base64.encodeToString(record.serialize(), Base64.DEFAULT))
            .apply()
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean =
        prefs.contains("signedprekey_$signedPreKeyId")

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        prefs.edit().remove("signedprekey_$signedPreKeyId").apply()
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        val result = mutableListOf<SessionRecord>()
        for (addr in addresses) {
            val key = "session_${addr.name}_${addr.deviceId}"
            val serialized = prefs.getString(key, null)
            if (serialized != null) {
                result.add(SessionRecord(Base64.decode(serialized, Base64.DEFAULT)))
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
        prefs.edit().putString(key, Base64.encodeToString(record.serialize(), Base64.DEFAULT)).apply()
    }

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: java.util.UUID
    ): org.signal.libsignal.protocol.groups.state.SenderKeyRecord? {
        val key = "senderkey_${sender.name}_${sender.deviceId}_${distributionId}"
        val serialized = prefs.getString(key, null) ?: return null
        return try {
            org.signal.libsignal.protocol.groups.state.SenderKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
        } catch (_: Exception) {
            null
        }
    }

    // ─── KyberPreKeyStore ─────────────────────────────────────────────────────

    override fun loadKyberPreKey(kyberPreKeyId: Int): org.signal.libsignal.protocol.state.KyberPreKeyRecord {
        val serialized = prefs.getString("kyberprekey_$kyberPreKeyId", null)
            ?: throw org.signal.libsignal.protocol.InvalidKeyIdException("No Kyber prekey: $kyberPreKeyId")
        return org.signal.libsignal.protocol.state.KyberPreKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun loadKyberPreKeys(): List<org.signal.libsignal.protocol.state.KyberPreKeyRecord> {
        val result = mutableListOf<org.signal.libsignal.protocol.state.KyberPreKeyRecord>()
        for (key in prefs.all.keys) {
            if (key.startsWith("kyberprekey_")) {
                val serialized = prefs.getString(key, null)
                if (serialized != null) {
                    try {
                        result.add(
                            org.signal.libsignal.protocol.state.KyberPreKeyRecord(
                                Base64.decode(serialized, Base64.DEFAULT)
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
            .putString("kyberprekey_$kyberPreKeyId", Base64.encodeToString(record.serialize(), Base64.DEFAULT))
            .apply()
    }

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean =
        prefs.contains("kyberprekey_$kyberPreKeyId")

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int) {
        prefs.edit().putBoolean("kyberprekey_used_$kyberPreKeyId", true).apply()
    }
}
