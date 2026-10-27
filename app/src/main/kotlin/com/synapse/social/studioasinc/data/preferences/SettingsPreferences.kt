package com.synapse.social.studioasinc.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsPreferences @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "synapse_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _securityNotificationsEnabled = MutableStateFlow(prefs.getBoolean("security_notifications", true))
    val securityNotificationsEnabled = _securityNotificationsEnabled.asStateFlow()

    private val _readReceiptsEnabled = MutableStateFlow(prefs.getBoolean("read_receipts", true))
    val readReceiptsEnabled = _readReceiptsEnabled.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock", false))
    val appLockEnabled = _appLockEnabled.asStateFlow()

    private val _chatLockEnabled = MutableStateFlow(prefs.getBoolean("chat_lock", false))
    val chatLockEnabled = _chatLockEnabled.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(prefs.getBoolean("auto_backup", false))
    val autoBackupEnabled = _autoBackupEnabled.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean("reminders", true))
    val remindersEnabled = _remindersEnabled.asStateFlow()

    private val _highPriorityEnabled = MutableStateFlow(prefs.getBoolean("high_priority", false))
    val highPriorityEnabled = _highPriorityEnabled.asStateFlow()

    private val _reactionNotificationsEnabled = MutableStateFlow(prefs.getBoolean("reaction_notifications", true))
    val reactionNotificationsEnabled = _reactionNotificationsEnabled.asStateFlow()

    private val _dataSaverEnabled = MutableStateFlow(prefs.getBoolean("data_saver", false))
    val dataSaverEnabled = _dataSaverEnabled.asStateFlow()

    fun getLockedChatIds(): Set<String> {
        return prefs.getStringSet("locked_chat_ids", emptySet()) ?: emptySet()
    }

    fun setLockedChatIds(chatIds: Set<String>) {
        prefs.edit().putStringSet("locked_chat_ids", chatIds).apply()
    }
}
