package com.synapse.social.studioasinc.shared.data.crypto

import android.content.Context
import android.util.Base64
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore

class AndroidSignalProtocolStore(context: Context) : SignalProtocolStore {

    private val prefs = context.getSharedPreferences("signal_store", Context.MODE_PRIVATE)

    // --- IdentityKeyStore ---

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
        prefs.edit().putString("identity_${address.name}_${address.deviceId}", Base64.encodeToString(identityKey.serialize(), Base64.DEFAULT)).apply()
        return true
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        // Simple TOFU: Trust On First Use
        val existing = prefs.getString("identity_${address.name}_${address.deviceId}", null)
        if (existing == null) {
            return true
        }
        val existingKey = IdentityKey(Base64.decode(existing, Base64.DEFAULT), 0)
        return existingKey == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val existing = prefs.getString("identity_${address.name}_${address.deviceId}", null) ?: return null
        return IdentityKey(Base64.decode(existing, Base64.DEFAULT), 0)
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
        if (lastRotation == 0L) return false // Never rotated, use initial keys
        
        val daysSinceRotation = (System.currentTimeMillis() - lastRotation) / (1000 * 60 * 60 * 24)
        return daysSinceRotation >= thresholdDays
    }

    // --- PreKeyStore ---
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val serialized = prefs.getString("prekey_$preKeyId", null) ?: throw IllegalStateException("No prekey found")
        return PreKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        prefs.edit().putString("prekey_$preKeyId", Base64.encodeToString(record.serialize(), Base64.DEFAULT)).apply()
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return prefs.contains("prekey_$preKeyId")
    }

    override fun removePreKey(preKeyId: Int) {
        prefs.edit().remove("prekey_$preKeyId").apply()
    }

    // --- SessionStore ---
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val serialized = prefs.getString("session_${address.name}_${address.deviceId}", null)
        return if (serialized != null) {
            SessionRecord(Base64.decode(serialized, Base64.DEFAULT))
        } else {
            SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        return emptyList()
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        prefs.edit().putString("session_${address.name}_${address.deviceId}", Base64.encodeToString(record.serialize(), Base64.DEFAULT)).apply()
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return prefs.contains("session_${address.name}_${address.deviceId}")
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        prefs.edit().remove("session_${address.name}_${address.deviceId}").apply()
    }

    override fun deleteAllSessions(name: String) {
        // Not perfectly implemented for full deletion, but adequate for simple needs
    }

    // --- SignedPreKeyStore ---
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val serialized = prefs.getString("signedprekey_$signedPreKeyId", null) ?: throw IllegalStateException("No signed prekey")
        return SignedPreKeyRecord(Base64.decode(serialized, Base64.DEFAULT))
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return emptyList() // Naive impl
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        prefs.edit().putString("signedprekey_$signedPreKeyId", Base64.encodeToString(record.serialize(), Base64.DEFAULT)).apply()
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return prefs.contains("signedprekey_$signedPreKeyId")
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        prefs.edit().remove("signedprekey_$signedPreKeyId").apply()
    }
}
