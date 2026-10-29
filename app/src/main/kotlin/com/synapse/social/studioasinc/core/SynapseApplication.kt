package com.synapse.social.studioasinc.core

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.AuthDevelopmentUtils
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.debug.LogLevel
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.core.navigation.NotificationNavigationCoordinator
import com.synapse.social.studioasinc.core.util.MediaCacheCleanupManager
import com.synapse.social.studioasinc.core.util.SupabaseStorageImageInterceptor
import com.synapse.social.studioasinc.feature.createpost.scheduling.ScheduledPostPublisherWorker
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.feature.shared.theme.ThemeManager
import com.synapse.social.studioasinc.feature.settings.backup.SettingsCloudSyncScheduler
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
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
class SynapseApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var instance: SynapseApplication
            private set
    }

    @Inject lateinit var notificationRepository: NotificationRepository

    override fun newImageLoader(context: Context): ImageLoader {
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

        return ImageLoader.Builder(context)
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    @Inject lateinit var startPresenceTrackingUseCase: StartPresenceTrackingUseCase
    @Inject lateinit var stopPresenceTrackingUseCase: StopPresenceTrackingUseCase
    @Inject lateinit var updatePresenceUseCase: UpdatePresenceUseCase
    @Inject lateinit var settingsCloudSyncScheduler: SettingsCloudSyncScheduler

    /**
     * The watch for calls addressed to this user, started and stopped with the session below.
     *
     * The application owns it rather than a screen because a ring has to be noticed while no screen is
     * looking: the app is backgrounded, the screen is off, or the process has only just come back for a
     * push. It is the same lifetime the presence tracker has, for the same reason.
     */
    @Inject lateinit var incomingCallWatcher: IncomingCallWatcher
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

        settingsCloudSyncScheduler.start()

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
                settingsCloudSyncScheduler.requestSyncIfEligible()
                // Reconnect Realtime on foreground resume (e.g. after screen-off/Doze)
                if (isAuthenticated) {
                    applicationScope.launch(Dispatchers.IO) {
                        try {
                            val rt = io.github.jan.supabase.realtime.Realtime
                            SupabaseClient.client.pluginManager.getPlugin(rt).connect()
                            Napier.d("✅ Supabase Realtime reconnected on foreground resume", tag = "SynapseApplication")
                        } catch (e: Exception) {
                            Napier.e("❌ Failed to reconnect Supabase Realtime on resume", e, tag = "SynapseApplication")
                        }
                    }
                }
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

        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = event.notification.additionalData
                // A call push is the one notification whose row may not be in this process's hands:
                // on a cold start nothing has subscribed yet, so the watcher is asked to reconcile
                // against the backend before navigation opens a screen that has to resolve it.
                if (data?.optString("type") == "incoming_call") {
                    incomingCallWatcher.start()
                }
                NotificationNavigationCoordinator.onNotificationClick(data)
            }
        })

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
            // Notification permission is requested from the permission onboarding and the permission
            // manager, never during startup; OneSignal observes whatever the OS has already granted.
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
                                // Watches for a ring, and re-reads whatever is already open server-side —
                                // which is the cold-start path for a call placed while this process was
                                // not running.
                                incomingCallWatcher.start()

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
                            // A ring belongs to the user who was signed in. Stopping the watch clears it,
                            // so the next account on this device cannot be handed the previous one's call.
                            incomingCallWatcher.stop()

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
        try {
            mediaCacheCleanupManager = MediaCacheCleanupManager(this)
            mediaCacheCleanupManager.initialize()
        } catch (e: Exception) {
            Napier.w("Skipping maintenance services initialization: ${e.message}", e, tag = "SynapseApplication")
        }
        try {
            ScheduledPostPublisherWorker.ensurePeriodicPublish(this)
        } catch (e: Exception) {
            Napier.w("Skipping scheduled post publisher initialization: ${e.message}", e, tag = "SynapseApplication")
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        if (::mediaCacheCleanupManager.isInitialized) {
            mediaCacheCleanupManager.shutdown()
        }
    }
}
