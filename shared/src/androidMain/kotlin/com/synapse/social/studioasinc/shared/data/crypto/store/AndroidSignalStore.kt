package com.synapse.social.studioasinc.shared.data.crypto.store

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import org.whispersystems.libsignal.state.SignalProtocolStore
import java.io.IOException

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
            throw IOException(errorMessage)
        }
    }


    override fun getIdentityKeyPair(): IdentityKeyPair {
        val encoded = prefs.getString("identity_key_pair", null) ?: throw IOException("No identity key pair")
        return IdentityKeyPair(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun getLocalRegistrationId(): Int {
        return prefs.getInt("registration_id", 0)
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = "identity_${address.name}_${address.deviceId}"
        val existing = prefs.getString(key, null)
        if (existing != null) {
            val existingKey = IdentityKey(Base64.decode(existing, Base64.NO_WRAP), 0)
            if (existingKey != identityKey) {
                prefs.edit().putString(key, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to save identity key")
                return true
            }
            return false
        }
        prefs.edit().putString(key, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to save identity key")
        return true
    }

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction?): Boolean {
         val key = "identity_${address.name}_${address.deviceId}"
         val existing = prefs.getString(key, null)
         if (existing == null) return true
         val existingKey = IdentityKey(Base64.decode(existing, Base64.NO_WRAP), 0)
         return existingKey == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = "identity_${address.name}_${address.deviceId}"
        val existing = prefs.getString(key, null) ?: return null
        return IdentityKey(Base64.decode(existing, Base64.NO_WRAP), 0)
    }

    fun saveIdentityKeyPair(identityKeyPair: IdentityKeyPair) {
        prefs.edit().putString("identity_key_pair", Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to save identity key pair")
    }

    fun saveLocalRegistrationId(registrationId: Int) {
        prefs.edit().putInt("registration_id", registrationId).commitOrThrow("Failed to save registration id")
    }


    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val encoded = prefs.getString("prekey_$preKeyId", null) ?: throw IOException("No such prekey")
        return PreKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        prefs.edit().putString("prekey_$preKeyId", Base64.encodeToString(record.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to store prekey")
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return prefs.contains("prekey_$preKeyId")
    }

    override fun removePreKey(preKeyId: Int) {
        prefs.edit().remove("prekey_$preKeyId").commitOrThrow("Failed to remove prekey")
    }


    private val signedPreKeyIdsKey = "signed_prekey_ids"

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val encoded = prefs.getString("signed_prekey_$signedPreKeyId", null) ?: throw IOException("No such signed prekey")
        return SignedPreKeyRecord(Base64.decode(encoded, Base64.NO_WRAP))
    }

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> {
        val ids = prefs.getStringSet(signedPreKeyIdsKey, emptySet()) ?: emptySet()
        val list = mutableListOf<SignedPreKeyRecord>()
        for (idStr in ids) {
            try {
                val id = idStr.toInt()
                list.add(loadSignedPreKey(id))
            } catch (e: Exception) {

            }
        }
        return list
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        prefs.edit().putString("signed_prekey_$signedPreKeyId", Base64.encodeToString(record.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to store signed prekey")

        val ids = prefs.getStringSet(signedPreKeyIdsKey, mutableSetOf())!!.toMutableSet()
        ids.add(signedPreKeyId.toString())
        prefs.edit().putStringSet(signedPreKeyIdsKey, ids).commitOrThrow("Failed to update signed prekey ids")
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return prefs.contains("signed_prekey_$signedPreKeyId")
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
         prefs.edit().remove("signed_prekey_$signedPreKeyId").commitOrThrow("Failed to remove signed prekey")

         val ids = prefs.getStringSet(signedPreKeyIdsKey, mutableSetOf())!!.toMutableSet()
         ids.remove(signedPreKeyId.toString())
         prefs.edit().putStringSet(signedPreKeyIdsKey, ids).commitOrThrow("Failed to update signed prekey ids")
    }


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
        prefs.edit().putString(key, Base64.encodeToString(record.serialize(), Base64.NO_WRAP)).commitOrThrow("Failed to store session")
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
         val key = "session_${address.name}_${address.deviceId}"
         return prefs.contains(key)
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = "session_${address.name}_${address.deviceId}"
        prefs.edit().remove(key).commitOrThrow("Failed to delete session")
    }

    override fun deleteAllSessions(name: String?) {
        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        var deletedCount = 0

        for (key in allKeys) {
            if (key.startsWith("session_")) {
                if (name == null) {
                    editor.remove(key)
                    deletedCount++
                } else {
                    val lastUnderscoreIndex = key.lastIndexOf('_')
                    if (lastUnderscoreIndex > "session_".length) {
                        val storedName = key.substring("session_".length, lastUnderscoreIndex)
                        if (storedName == name) {
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


    fun getLastSignedPreKeyId(): Int {
        return prefs.getInt("last_signed_prekey_id", 0)
    }

    fun setLastSignedPreKeyId(id: Int) {
        prefs.edit().putInt("last_signed_prekey_id", id).commitOrThrow("Failed to save last signed prekey id")
    }
}
