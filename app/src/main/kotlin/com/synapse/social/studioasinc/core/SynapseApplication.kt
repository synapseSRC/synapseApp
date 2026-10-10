package com.synapse.social.studioasinc.core

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.AuthDevelopmentUtils
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.core.util.MediaCacheCleanupManager
import com.synapse.social.studioasinc.core.util.SupabaseStorageImageInterceptor
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.feature.shared.theme.ThemeManager
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StartPresenceTrackingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StopPresenceTrackingUseCase
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
import com.synapse.social.studioasinc.core.monitoring.SentryInitializer
import com.synapse.social.studioasinc.core.crash.GlobalCrashHandler
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class SynapseApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: SynapseApplication
            private set
    }

    @Inject lateinit var notificationRepository: NotificationRepository

    override fun newImageLoader(): ImageLoader {
        val placeholder = BitmapDrawable(
            resources,
            BitmapFactory.decodeResource(resources, R.raw.placeholder)
        )
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(
                SupabaseStorageImageInterceptor(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    anonKey = BuildConfig.SUPABASE_ANON_KEY,
                    accessTokenProvider = {
                        try {
                            SupabaseClient.client.auth.currentSessionOrNull()?.accessToken
                        } catch (_: Exception) {
                            null
                        }
                    }
                )
            )
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .placeholder(placeholder)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    @Inject lateinit var startPresenceTrackingUseCase: StartPresenceTrackingUseCase
    @Inject lateinit var stopPresenceTrackingUseCase: StopPresenceTrackingUseCase
    @Inject lateinit var updatePresenceUseCase: UpdatePresenceUseCase
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var isAppInForeground = false

    @Volatile
    private var isAuthenticated = false

    @Volatile
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        SentryInitializer.initialize(this)
        instance = this

        GlobalCrashHandler.install(this)

        // Initialize Napier logging
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Set default night mode to follow system
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        initializeOneSignal()

        setupLifecycleObserver()

        SupabaseAuthenticationService.initialize(this)

        setupAuthSessionObserver()


        initializeMaintenanceServices()


        applyThemeOnStartup()


        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }

    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
                managePresenceTracking()
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
                managePresenceTracking()
            }
        })
    }

    private fun managePresenceTracking() {
        val shouldTrack = isAppInForeground && isAuthenticated
        Napier.d(
            "Presence tracking decision: shouldTrack=$shouldTrack, " +
                "isAppInForeground=$isAppInForeground, " +
                "isAuthenticated=$isAuthenticated, " +
                "isTracking=$isTracking",
            tag = "PresenceTracker"
        )
        if (shouldTrack && !isTracking) {
            isTracking = true
            applicationScope.launch(Dispatchers.IO) {
                try {
                    startPresenceTrackingUseCase()
                    Napier.d("✅ Presence tracking started (foreground + authenticated)", tag = "PresenceTracker")
                } catch (e: Exception) {
                    Napier.e("❌ Failed to start presence tracking", e, tag = "PresenceTracker")
                    isTracking = false
                }
            }
        } else if (!shouldTrack && isTracking) {
            isTracking = false
            applicationScope.launch(Dispatchers.IO) {
                try {
                    stopPresenceTrackingUseCase()
                    Napier.d("🛑 Presence tracking stopped (background or unauthenticated)", tag = "PresenceTracker")
                } catch (e: Exception) {
                    Napier.e("❌ Failed to stop presence tracking", e, tag = "PresenceTracker")
                    isTracking = true
                }
            }
        } else {
            Napier.d("No presence tracking transition needed: shouldTrack=$shouldTrack, isTracking=$isTracking", tag = "PresenceTracker")
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
        }
    }

    private fun setupAuthSessionObserver() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    Napier.w("Supabase not configured, skipping auth session observer", tag = "SynapseApplication")
                    return@launch
                }
                SupabaseAuthenticationService.getInstance(this@SynapseApplication)
                
                SupabaseClient.client.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val userId = status.session.user?.id
                            if (userId != null) {
                                isAuthenticated = true
                                Napier.d(
                                    "Auth state: Authenticated (user=$userId, " +
                                        "isAppInForeground=$isAppInForeground, isTracking=$isTracking)",
                                    tag = "PresenceTracker"
                                )
                                managePresenceTracking()

                                try {
                                    val rt = io.github.jan.supabase.realtime.Realtime
                                    SupabaseClient.client.pluginManager.getPlugin(rt).connect()
                                    Napier.d("✅ Supabase Realtime connected globally for user: $userId")
                                } catch (e: Exception) {
                                    Napier.e("❌ Failed to connect to Supabase Realtime", e)
                                }

                                if (isOneSignalConfigured()) {
                                    withContext(Dispatchers.Main) {
                                        try {
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
                                        } catch (e: Exception) {
                                            android.util.Log.e("OneSignal", "❌ OneSignal identity sync failed", e)
                                        }
                                    }
                                }
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            isAuthenticated = false
                            Napier.d(
                                "Auth state: NotAuthenticated (isAppInForeground=$isAppInForeground, isTracking=$isTracking)",
                                tag = "PresenceTracker"
                            )
                            managePresenceTracking()

                            try {
                                val rt = io.github.jan.supabase.realtime.Realtime
                                SupabaseClient.client.pluginManager.getPlugin(rt).disconnect()
                                Napier.d("Supabase Realtime disconnected on logout", tag = "SynapseApplication")
                            } catch (e: Exception) {}

                            if (isOneSignalConfigured()) {
                                withContext(Dispatchers.Main) {
                                    try {
                                        android.util.Log.d("OneSignal", "User logged out, logging out from OneSignal")
                                        OneSignal.logout()
                                    } catch (e: Exception) {
                                        android.util.Log.e("OneSignal", "❌ OneSignal logout failed", e)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SynapseApplication", "Error in auth session observer", e)
            }
        }
    }

    private fun isOneSignalConfigured(): Boolean {
        val appId = NotificationConfig.ONESIGNAL_APP_ID
        return appId.isNotBlank() && appId != "YOUR_ONESIGNAL_APP_ID_HERE"
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
