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
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StartPresenceTrackingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import io.github.aakira.napier.Napier
import io.github.aakira.napier.DebugAntilog
import com.synapse.social.studioasinc.BuildConfig

@HiltAndroidApp
class SynapseApplication : Application() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var startPresenceTrackingUseCase: StartPresenceTrackingUseCase
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logging
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Apply saved language before anything else
        applySavedLanguage()

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

        val appId = NotificationConfig.ONESIGNAL_APP_ID
        if (appId.isBlank() || appId == "YOUR_ONESIGNAL_APP_ID_HERE") {
            android.util.Log.w("SynapseApplication", "⚠️ OneSignal App ID not configured. Push notifications will not work.")
            return
        }

        OneSignal.initWithContext(this, appId)

        // Listen for subscription changes
        OneSignal.User.pushSubscription.addObserver(object : com.onesignal.user.subscriptions.IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: com.onesignal.user.subscriptions.PushSubscriptionChangedState) {
                if (state.current.optedIn && state.current.id != null) {
                    val subscriptionId = state.current.id
                    android.util.Log.d("SynapseApplication", "Push subscribed: $subscriptionId")
                    
                    // Sync with backend
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                            val userId = authService.getCurrentUserId()
                            if (userId != null && subscriptionId != null) {
                                notificationRepository.updateOneSignalPlayerId(userId, subscriptionId)
                                android.util.Log.d("SynapseApplication", "✅ Synced OneSignal ID to backend: $subscriptionId")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SynapseApplication", "❌ Failed to sync OneSignal ID", e)
                        }
                    }
                }
            }
        })

        CoroutineScope(Dispatchers.Main).launch {
            // Request permission first
            OneSignal.Notifications.requestPermission(true)
            
            // Wait a bit for permission to be granted
            kotlinx.coroutines.delay(1000)
            
            // Then login with user ID
            try {
                val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                val userId = authService.getCurrentUserId()
                if (userId != null) {
                    OneSignal.login(userId)
                    OneSignal.User.pushSubscription.optIn()
                    android.util.Log.d("OneSignal", "✅ App startup login successful with user: $userId")
                    
                    val subId = OneSignal.User.pushSubscription.id
                    if (subId != null && OneSignal.User.pushSubscription.optedIn) {
                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                            try {
                                notificationRepository.updateOneSignalPlayerId(userId, subId)
                                android.util.Log.d("SynapseApplication", "✅ Synced OneSignal ID to backend on startup: $subId")
                            } catch (e: Exception) {
                                android.util.Log.e("SynapseApplication", "❌ Failed to sync OneSignal ID on startup", e)
                            }
                        }
                    }
                } else {
                    android.util.Log.w("OneSignal", "⚠️ No user logged in at app startup")
                }
            } catch (e: Exception) {
                android.util.Log.e("OneSignal", "❌ App startup login failed", e)
            }
            
            // Start presence tracking
            try {
                val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                if (authService.getCurrentUserId() != null) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        startPresenceTrackingUseCase()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to start presence tracking", e)
            }
        }
    }

    private fun applySavedLanguage() {
        val settingsRepository = SettingsRepositoryImpl.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val languageCode = settingsRepository.language.first()
                if (languageCode.isNotEmpty() && languageCode != "en") {
                    val locale = if (languageCode.contains("-")) {
                        val parts = languageCode.split("-")
                        java.util.Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build()
                    } else {
                        java.util.Locale.Builder().setLanguage(languageCode).build()
                    }
                    
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.create(locale)
                    )
                    
                    android.util.Log.d("SynapseApplication", "Applied saved language: $languageCode")
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to apply saved language", e)
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
