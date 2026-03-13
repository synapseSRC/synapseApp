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


    private val _enterIsSendEnabled = MutableStateFlow(prefs.getBoolean("enter_is_send", false))
    val enterIsSendEnabled = _enterIsSendEnabled.asStateFlow()

    private val _mediaVisibilityEnabled = MutableStateFlow(prefs.getBoolean("media_visibility", true))
    val mediaVisibilityEnabled = _mediaVisibilityEnabled.asStateFlow()

    private val _voiceTranscriptsEnabled = MutableStateFlow(prefs.getBoolean("voice_transcripts", true))
    val voiceTranscriptsEnabled = _voiceTranscriptsEnabled.asStateFlow()

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


    fun setSecurityNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("security_notifications", enabled).apply()
        _securityNotificationsEnabled.value = enabled
    }

    fun setReadReceipts(enabled: Boolean) {
        prefs.edit().putBoolean("read_receipts", enabled).apply()
        _readReceiptsEnabled.value = enabled
    }

    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock", enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setChatLock(enabled: Boolean) {
        prefs.edit().putBoolean("chat_lock", enabled).apply()
        _chatLockEnabled.value = enabled
    }

    fun setEnterIsSend(enabled: Boolean) {
        prefs.edit().putBoolean("enter_is_send", enabled).apply()
        _enterIsSendEnabled.value = enabled
    }

    fun setMediaVisibility(enabled: Boolean) {
        prefs.edit().putBoolean("media_visibility", enabled).apply()
        _mediaVisibilityEnabled.value = enabled
    }

    fun setVoiceTranscripts(enabled: Boolean) {
        prefs.edit().putBoolean("voice_transcripts", enabled).apply()
        _voiceTranscriptsEnabled.value = enabled
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup", enabled).apply()
        _autoBackupEnabled.value = enabled
    }

    fun setReminders(enabled: Boolean) {
        prefs.edit().putBoolean("reminders", enabled).apply()
        _remindersEnabled.value = enabled
    }

    fun setHighPriority(enabled: Boolean) {
        prefs.edit().putBoolean("high_priority", enabled).apply()
        _highPriorityEnabled.value = enabled
    }

    fun setReactionNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("reaction_notifications", enabled).apply()
        _reactionNotificationsEnabled.value = enabled
    }

    fun setDataSaver(enabled: Boolean) {
        prefs.edit().putBoolean("data_saver", enabled).apply()
        _dataSaverEnabled.value = enabled
    }


    private val _chatWallpaperType = MutableStateFlow(prefs.getString("chat_wallpaper_type", "DEFAULT") ?: "DEFAULT")
    val chatWallpaperType = _chatWallpaperType.asStateFlow()

    private val _chatWallpaperValue = MutableStateFlow(prefs.getString("chat_wallpaper_value", null))
    val chatWallpaperValue = _chatWallpaperValue.asStateFlow()

    private val _chatWallpaperBlur = MutableStateFlow(prefs.getFloat("chat_wallpaper_blur", 0f))
    val chatWallpaperBlur = _chatWallpaperBlur.asStateFlow()

    fun setChatWallpaperType(type: String) {
        prefs.edit().putString("chat_wallpaper_type", type).apply()
        _chatWallpaperType.value = type
    }

    fun setChatWallpaperValue(value: String?) {
        prefs.edit().putString("chat_wallpaper_value", value).apply()
        _chatWallpaperValue.value = value
    }

    fun setChatWallpaperBlur(blur: Float) {
        prefs.edit().putFloat("chat_wallpaper_blur", blur).apply()
        _chatWallpaperBlur.value = blur
    }

    fun getLockedChatIds(): Set<String> {
        return prefs.getStringSet("locked_chat_ids", emptySet()) ?: emptySet()
    }

    fun setLockedChatIds(chatIds: Set<String>) {
        prefs.edit().putStringSet("locked_chat_ids", chatIds).apply()
    }
}
