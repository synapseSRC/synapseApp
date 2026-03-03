package com.synapse.social.studioasinc.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.AuthDevelopmentUtils
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.core.util.MediaCacheCleanupManager
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.feature.shared.theme.ThemeManager
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class SynapseApplication : Application() {

    @Inject lateinit var notificationRepository: NotificationRepository
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager

    override fun onCreate() {
        super.onCreate()


        initializeOneSignal()


        SupabaseAuthenticationService.initialize(this)


        initializeMaintenanceServices()


        applyThemeOnStartup()


        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }

    private fun initializeOneSignal() {

        if (com.synapse.social.studioasinc.BuildConfig.DEBUG && NotificationConfig.ENABLE_DEBUG_LOGGING) {
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
        }


        OneSignal.initWithContext(this, NotificationConfig.ONESIGNAL_APP_ID)


        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)


            try {
                val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                authService.getCurrentUserId()?.let { userId ->
                    OneSignal.login(userId)
                    android.util.Log.d("SynapseApplication", "Restored OneSignal session for user: $userId")


                    val subscriptionId = OneSignal.User.pushSubscription.id
                    if (!subscriptionId.isNullOrEmpty()) {
                        withContext(Dispatchers.IO) {
                            try {
                                notificationRepository.updateOneSignalPlayerId(userId, subscriptionId)
                                android.util.Log.d("SynapseApplication", "Synced OneSignal ID: $subscriptionId")
                            } catch (e: Exception) {
                                android.util.Log.e("SynapseApplication", "Failed to sync OneSignal ID", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to restore OneSignal session", e)
            }
        }
    }

    private fun applyThemeOnStartup() {
        val settingsRepository = SettingsRepositoryImpl.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    ThemeManager.applyThemeMode(settings.themeMode)
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to apply theme on startup", e)
            }
        }
    }

    private fun initializeMaintenanceServices() {

        mediaCacheCleanupManager = MediaCacheCleanupManager(this)
        mediaCacheCleanupManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()


        if (::mediaCacheCleanupManager.isInitialized) {
            mediaCacheCleanupManager.shutdown()
        }
    }
}
