package com.synapse.social.studioasinc.shared.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException

class AndroidSecureStorage(private val context: Context, private val sharedPreferences: SharedPreferences? = null) : SecureStorage {

    private val prefs: SharedPreferences by lazy {
        sharedPreferences ?: try {
            val masterKey = createMasterKey(context)

            EncryptedSharedPreferences.create(
                context,
                "secure_storage_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize encrypted storage", e)
        }
    }

    private fun createMasterKey(context: Context): MasterKey {
        // Use MasterKey.Builder for stronger key derivation as MasterKeys is deprecated.
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    override fun clear(key: String) {
        prefs.edit().remove(key).apply()
    }
}
