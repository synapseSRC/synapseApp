package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.model.NotificationPreferences as DomainNotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.serialization.json.*

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadNotificationPreferences()
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _isLoading.value = true
            try {
                val domainPrefs = notificationRepository.fetchPreferences(userId)
                if (domainPrefs != null) {
                    val prefs = mapDomainToPreferences(domainPrefs)
                    _notificationPreferences.value = prefs

                    updateOneSignalTags(prefs)
                }
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

    fun toggleInAppNotifications(enabled: Boolean) {
        updatePreferences { it.copy(inAppNotificationsEnabled = enabled) }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        updatePreferences { it.copy(doNotDisturb = enabled) }
    }

    fun toggleQuietHours(enabled: Boolean) {
        updatePreferences { it.copy(quietHoursEnabled = enabled) }
    }

    fun setQuietHours(start: String, end: String) {
        updatePreferences { it.copy(quietHoursStart = start, quietHoursEnd = end) }
    }

    private fun updatePreferences(updateBlock: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentPrefs = _notificationPreferences.value
            val newPrefs = updateBlock(currentPrefs)

            _notificationPreferences.value = newPrefs

            try {
                notificationRepository.updatePreferences(userId, mapPreferencesToDomain(userId, newPrefs))

                updateOneSignalTags(newPrefs)
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsViewModel", "Failed to update preferences", e)
                _error.value = "Failed to sync preferences"
                _notificationPreferences.value = currentPrefs
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
            likesEnabled = social["likes"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_LIKES_ENABLED,
            commentsEnabled = social["comments"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_COMMENTS_ENABLED,
            repliesEnabled = social["replies"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_REPLIES_ENABLED,
            followsEnabled = social["follows"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_FOLLOWS_ENABLED,
            mentionsEnabled = social["mentions"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_MENTIONS_ENABLED,
            newPostsEnabled = content["new_posts"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_NEW_POSTS_ENABLED,
            sharesEnabled = content["shares"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_SHARES_ENABLED,
            securityEnabled = system["security"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_SECURITY_ENABLED,
            updatesEnabled = system["updates"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_UPDATES_ENABLED,
            quietHoursEnabled = quietHours["enabled"]?.jsonPrimitive?.booleanOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_ENABLED,
            quietHoursStart = quietHours["start"]?.jsonPrimitive?.contentOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_START,
            quietHoursEnd = quietHours["end"]?.jsonPrimitive?.contentOrNull ?: NotificationPreferences.DEFAULT_QUIET_HOURS_END,
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
            updatedAt = null // Or current time
        )
    }

    fun isCategoryEnabled(category: NotificationCategory): Boolean {
        return _notificationPreferences.value.isEnabled(category)
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
        tags[NotificationConfig.TAG_GLOBAL_ENABLED] = prefs.globalEnabled.toString()

        OneSignal.User.addTags(tags)
    }

    fun clearError() {
        _error.value = null
    }
}
