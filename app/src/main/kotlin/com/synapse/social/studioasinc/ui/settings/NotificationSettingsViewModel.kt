package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.repository.SoundSettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.NotificationPreferences as DomainNotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import com.synapse.social.studioasinc.shared.core.util.getCurrentIsoTime
import javax.inject.Inject
import kotlinx.serialization.json.*

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val soundSettingsRepository: SoundSettingsRepository
) : ViewModel() {
    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    // Device-local, unlike the notification preferences above: these govern tones this hardware plays,
    // so they live in DataStore and never sync to the backend.
    val soundPreferences: StateFlow<SoundPreferences> = soundSettingsRepository.soundPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoundPreferences())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val preferencesUpdateMutex = Mutex()
    private var persistedPreferences = NotificationPreferences()
    private var preferencesLoaded = false
    private val latestPreferenceUpdateVersion = AtomicLong(0L)

    init {
        loadNotificationPreferences()
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: throw IllegalStateException("User is not authenticated")
                val domainPrefs = notificationRepository.fetchPreferences(userId)
                val prefs = domainPrefs?.let(::mapDomainToPreferences) ?: NotificationPreferences()
                _notificationPreferences.value = prefs
                persistedPreferences = prefs
                preferencesLoaded = true
                updateOneSignalTags(prefs)
                _error.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to load notification preferences", e)
                _error.value = "Failed to load notification preferences"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun toggleGlobalNotifications(enabled: Boolean) {
        updatePreferences { it.copy(globalEnabled = enabled) }
    }

    fun toggleNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        updatePreferences { it.withCategory(category, enabled) }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        updatePreferences { it.copy(doNotDisturb = enabled) }
    }

    fun toggleQuietHours(enabled: Boolean) {
        updatePreferences { it.copy(quietHoursEnabled = enabled) }
    }

    fun toggleSoundEffects(enabled: Boolean) {
        viewModelScope.launch { soundSettingsRepository.setMasterEnabled(enabled) }
    }

    fun toggleMessageTones(enabled: Boolean) {
        viewModelScope.launch { soundSettingsRepository.setMessageTonesEnabled(enabled) }
    }

    fun toggleCallTones(enabled: Boolean) {
        viewModelScope.launch { soundSettingsRepository.setCallTonesEnabled(enabled) }
    }

    fun setQuietHours(start: String, end: String) {
        updatePreferences { it.copy(quietHoursStart = start, quietHoursEnd = end) }
    }

    private fun updatePreferences(updateBlock: (NotificationPreferences) -> NotificationPreferences) {
        if (!preferencesLoaded) {
            _error.value = "Notification preferences are not ready yet"
            return
        }

        val newPreferences = updateBlock(_notificationPreferences.value)
        _notificationPreferences.value = newPreferences
        val requestVersion = latestPreferenceUpdateVersion.incrementAndGet()

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: throw IllegalStateException("User is not authenticated")
                preferencesUpdateMutex.withLock {
                    val preferencesToPersist = _notificationPreferences.value
                    val versionBeingPersisted = latestPreferenceUpdateVersion.get()
                    if (preferencesToPersist != persistedPreferences) {
                        notificationRepository.updatePreferences(userId, mapPreferencesToDomain(userId, preferencesToPersist))
                        persistedPreferences = preferencesToPersist
                        updateOneSignalTags(preferencesToPersist)
                    }
                    if (latestPreferenceUpdateVersion.get() == versionBeingPersisted) _error.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to update preferences", e)
                _error.value = "Failed to sync preferences"
                preferencesUpdateMutex.withLock {
                    if (latestPreferenceUpdateVersion.get() == requestVersion) {
                        _notificationPreferences.value = persistedPreferences
                    }
                }
            }
        }
    }

    private fun mapDomainToPreferences(domainPrefs: DomainNotificationPreferences): NotificationPreferences {
        val settings = domainPrefs.settings
        val social = settings["social"] as? JsonObject ?: buildJsonObject { }
        val content = settings["content"] as? JsonObject ?: buildJsonObject { }
        val system = settings["system"] as? JsonObject ?: buildJsonObject { }
        val quietHours = domainPrefs.quietHours

        return NotificationPreferences(
            globalEnabled = domainPrefs.enabled,
            likesEnabled = social["likes"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_LIKES_ENABLED,
            commentsEnabled = social["comments"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_COMMENTS_ENABLED,
            messagesEnabled = social["messages"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_MESSAGES_ENABLED,
            repliesEnabled = social["replies"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_REPLIES_ENABLED,
            followsEnabled = social["follows"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_FOLLOWS_ENABLED,
            mentionsEnabled = social["mentions"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_MENTIONS_ENABLED,
            newPostsEnabled = content["new_posts"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_NEW_POSTS_ENABLED,
            sharesEnabled = content["shares"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_SHARES_ENABLED,
            securityEnabled = system["security"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_SECURITY_ENABLED,
            updatesEnabled = system["updates"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_UPDATES_ENABLED,
            quietHoursEnabled = quietHours["enabled"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_ENABLED,
            quietHoursStart = quietHours["start"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_START,
            quietHoursEnd = quietHours["end"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_END,
            doNotDisturb = domainPrefs.doNotDisturb,
            dndUntil = domainPrefs.dndUntil
        )
    }

    private fun mapPreferencesToDomain(userId: String, prefs: NotificationPreferences): DomainNotificationPreferences {
        return DomainNotificationPreferences(
            userId = userId,
            enabled = prefs.globalEnabled,
            settings = buildJsonObject {
                put("social", buildJsonObject {
                    put("likes", prefs.likesEnabled)
                    put("comments", prefs.commentsEnabled)
                    put("messages", prefs.messagesEnabled)
                    put("replies", prefs.repliesEnabled)
                    put("follows", prefs.followsEnabled)
                    put("mentions", prefs.mentionsEnabled)
                })
                put("content", buildJsonObject {
                    put("new_posts", prefs.newPostsEnabled)
                    put("shares", prefs.sharesEnabled)
                })
                put("system", buildJsonObject {
                    put("security", prefs.securityEnabled)
                    put("updates", prefs.updatesEnabled)
                })
            },
            quietHours = buildJsonObject {
                put("enabled", prefs.quietHoursEnabled)
                put("start", prefs.quietHoursStart)
                put("end", prefs.quietHoursEnd)
            },
            doNotDisturb = prefs.doNotDisturb,
            dndUntil = prefs.dndUntil,
            updatedAt = getCurrentIsoTime()
        )
    }

    private fun updateOneSignalTags(prefs: NotificationPreferences) {
        val tags = mutableMapOf<String, String>()
        tags[NotificationConfig.TAG_LIKES] = prefs.likesEnabled.toString()
        tags[NotificationConfig.TAG_COMMENTS] = prefs.commentsEnabled.toString()
        tags[NotificationConfig.TAG_REPLIES] = prefs.repliesEnabled.toString()
        tags[NotificationConfig.TAG_FOLLOWS] = prefs.followsEnabled.toString()
        tags[NotificationConfig.TAG_MENTIONS] = prefs.mentionsEnabled.toString()
        tags[NotificationConfig.TAG_NEW_POSTS] = prefs.newPostsEnabled.toString()
        tags[NotificationConfig.TAG_SHARES] = prefs.sharesEnabled.toString()
        tags[NotificationConfig.TAG_MESSAGES] = prefs.messagesEnabled.toString()
        tags[NotificationConfig.TAG_GLOBAL_ENABLED] = prefs.globalEnabled.toString()

        try {
            OneSignal.User.addTags(tags)
        } catch (e: Exception) {
            android.util.Log.w("NotificationSettingsViewModel", "Failed to sync OneSignal preference tags", e)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
