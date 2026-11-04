package com.synapse.social.studioasinc.shared.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException

class AndroidSecureStorage(private val context: Context, private val sharedPreferences: SharedPreferences? = null) : SecureStorage {

    // androidx.security:security-crypto is deprecated upstream and has no in-library successor, so
    // every MasterKey/EncryptedSharedPreferences reference warns. Suppressing is deliberate, not a
    // shortcut: the on-disk format this produces (AES-SIV key names, AES-GCM values, master key in
    // the Android keystore) is what keeps already-issued tokens readable, so replacing it is a
    // key-migration project with sign-out consequences — not a warning fix.
    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION") // Part of the deprecated security-crypto artifact, see `prefs`.
    private fun createMasterKey(context: Context): MasterKey {
        // MasterKeys (the old static helper) is gone; MasterKey.Builder is the current form.
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
