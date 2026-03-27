package com.synapse.social.studioasinc.core

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdatePresenceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import io.github.aakira.napier.Napier
import io.github.aakira.napier.DebugAntilog
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class SynapseApplication : Application() {

    companion object {
        lateinit var instance: SynapseApplication
            private set
    }

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var startPresenceTrackingUseCase: StartPresenceTrackingUseCase
    @Inject lateinit var updatePresenceUseCase: UpdatePresenceUseCase
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Napier logging
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Apply saved language before anything else
        applySavedLanguage()

        initializeOneSignal()

        setupLifecycleObserver()

        setupLifecycleObserver()

        SupabaseAuthenticationService.initialize(this)


        initializeMaintenanceServices()


        applyThemeOnStartup()


        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }

    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App moved to foreground
                applicationScope.launch(Dispatchers.IO) {
                    try {
                        val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                        if (authService.getCurrentUserId() != null) {
                            updatePresenceUseCase(true)
                            Napier.d("App foreground: Presence set to online")
                        }
                    } catch (e: Exception) {
                        Napier.e("Failed to update presence on foreground", e)
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                // App moved to background
                applicationScope.launch(Dispatchers.IO) {
                    try {
                        updatePresenceUseCase(false)
                        Napier.d("App background: Presence set to offline")
                    } catch (e: Exception) {
                        Napier.e("Failed to update presence on background", e)
                    }
                }
            }
        })
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
                val subscriptionId = state.current.id
                if (subscriptionId != null) {
                    android.util.Log.d("SynapseApplication", "Push subscription status: $subscriptionId (optedIn: ${state.current.optedIn})")
                    
                    // Sync with backend
                    applicationScope.launch(Dispatchers.IO) {
                        try {
                            val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                            val userId = authService.getCurrentUserId()
                            if (userId != null) {
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

        applicationScope.launch {
            // Request permission first
            OneSignal.Notifications.requestPermission(true)
            
            // Login and sync with user ID if available
            try {
                val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                val userId = authService.getCurrentUserId()
                if (userId != null) {
                    OneSignal.login(userId)
                    OneSignal.User.pushSubscription.optIn()
                    android.util.Log.d("OneSignal", "✅ App startup registration for user: $userId")
                    
                    val subId = OneSignal.User.pushSubscription.id
                    if (subId != null) {
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
                    android.util.Log.w("OneSignal", "⚠️ No user logged in at app startup for OneSignal registration")
                }
            } catch (e: Exception) {
                android.util.Log.e("OneSignal", "❌ App startup OneSignal registration failed", e)
            }
            
            // Start presence tracking
            try {
                val authService = SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                if (authService.getCurrentUserId() != null) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        // First set online status
                        updatePresenceUseCase(true)
                        Napier.d("Initial presence set to online")
                        // Then start heartbeat tracking
                        startPresenceTrackingUseCase()
                        Napier.d("Presence tracking started")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Failed to start presence tracking", e)
                Napier.e("Failed to start presence tracking", e)
            }
        }

        // Observe Supabase Auth changes to sync with OneSignal
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Wait for Supabase to be initialized via service
                SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                
                SupabaseClient.client.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val userId = status.session.user?.id
                            if (userId != null) {
                                withContext(Dispatchers.Main) {
                                    android.util.Log.d("OneSignal", "Syncing identity for authenticated user: $userId")
                                    OneSignal.login(userId)
                                    OneSignal.User.pushSubscription.optIn()
                                    
                                    val subId = OneSignal.User.pushSubscription.id
                                    if (subId != null) {
                                        applicationScope.launch(Dispatchers.IO) {
                                            try {
                                                notificationRepository.updateOneSignalPlayerId(userId, subId)
                                                android.util.Log.d("SynapseApplication", "✅ Session sync: OneSignal ID updated")
                                            } catch (e: Exception) {
                                                android.util.Log.e("SynapseApplication", "❌ Session sync: Failed to update OneSignal ID", e)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            withContext(Dispatchers.Main) {
                                android.util.Log.d("OneSignal", "User logged out, logging out from OneSignal")
                                OneSignal.logout()
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Error in OneSignal session listener", e)
            }
        }
    }

    private fun applySavedLanguage() {
        val settingsRepository = SettingsRepositoryImpl.getInstance(this)
        applicationScope.launch {
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
        applicationScope.launch {
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
