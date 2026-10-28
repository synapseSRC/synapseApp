package com.synapse.social.studioasinc.feature.shared.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.navigation.AppNavigation
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.domain.usecase.update.UpdateState
import androidx.appcompat.app.AppCompatActivity
import com.synapse.social.studioasinc.core.util.ChatLockManager
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var reelUploadManager: com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager

    @Inject
    lateinit var chatLockManager: ChatLockManager

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: androidx.navigation.NavHostController

    private var isAppUnlocked = false
    private var isCheckingLock = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isCheckingAuth.value || isCheckingLock || (!isAppUnlocked && isAppLockEnabledBlocking())
        }

        enableEdgeToEdge()

        lifecycleScope.launch {
            val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this@MainActivity)
            val privacySettings = settingsRepository.privacySettings.first()
            if (privacySettings.appLockEnabled) {
                chatLockManager.authenticate(
                    this@MainActivity,
                    onSuccess = {
                        isAppUnlocked = true
                        isCheckingLock = false
                        setupContent()
                    },
                    onError = {
                        // Exit the app if authentication fails or is cancelled
                        finish()
                    }
                )
            } else {
                isAppUnlocked = true
                isCheckingLock = false
                setupContent()
            }
        }

        createNotificationChannels()
        viewModel.checkForUpdates()
    }

    private fun isAppLockEnabledBlocking(): Boolean {
        // Fast path for the splash screen condition so we don't block
        // We'll rely on the coroutine to finish checking quickly.
        return true
    }

    private fun setupContent() {
        setContent {
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    UiEventManager.events.collect { event ->
                        val text = when (event) {
                            is UiEvent.Message -> event.text
                            is UiEvent.Error -> event.text
                            is UiEvent.Success -> event.text
                        }
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this@MainActivity)
            val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
                initial = com.synapse.social.studioasinc.ui.settings.AppearanceSettings()
            )

            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM ->
                    androidx.compose.foundation.isSystemInDarkTheme()
            }

            val dynamicColor = appearanceSettings.dynamicColorEnabled &&
                               android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                navController = rememberNavController()
                val updateState by viewModel.updateState.observeAsState()
                val startDestination by viewModel.startDestination.collectAsState()
                val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()
                val showProfilePicSuggestion by viewModel.showProfilePicSuggestion.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                if (!isCheckingAuth) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        reelUploadManager = reelUploadManager,
                        postAuthDestination = viewModel::postAuthDestination
                    )

                    val isAtHome = currentDestination?.hasRoute<AppDestination.Home>() == true
                    if (showProfilePicSuggestion && isAtHome) {
                        ProfilePicSuggestionDialog(
                            onUpload = {
                                viewModel.onDismissProfilePicSuggestion(false)
                                navController.navigate(AppDestination.EditProfile)
                            },
                            onDismiss = { hideAgain ->
                                viewModel.onDismissProfilePicSuggestion(hideAgain)
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        // Only an app that already belongs in Home may honour the inbox shortcut;
                        // onboarding must not be skipped by a launch intent.
                        if (startDestination == AppDestination.Home) {
                            val destination = this@MainActivity.intent.getStringExtra("destination")
                            if (destination == "inbox") {
                                navController.navigate(AppDestination.Inbox)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            navController.handleDeepLink(intent)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                enableLights(true)
                lightColor = 0xFFFF0000.toInt()
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }

            val generalChannel = NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}

@Composable
fun ProfilePicSuggestionDialog(
    onUpload: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    var hideAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(hideAgain) },
        title = { Text(text = stringResource(R.string.add_profile_picture_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.add_profile_picture_message))
                Spacer(modifier = Modifier.height(Spacing.space16))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { hideAgain = !hideAgain }
                ) {
                    Checkbox(
                        checked = hideAgain,
                        onCheckedChange = null
                    )
                    Text(
                        text = stringResource(R.string.dont_show_again),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpload) {
                Text(stringResource(R.string.upload))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(hideAgain) }) {
                Text(stringResource(R.string.later))
            }
        }
    )
}

