package com.synapse.social.studioasinc.feature.shared.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
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
import androidx.compose.material3.*
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
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.navigation.AppNavigation
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.synapse.social.studioasinc.shared.core.util.UiEventManager
import com.synapse.social.studioasinc.shared.core.util.UiEvent
import com.synapse.social.studioasinc.domain.usecase.update.UpdateState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var reelUploadManager: com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: androidx.navigation.NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isCheckingAuth.value
        }


        enableEdgeToEdge()

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

                if (!isCheckingAuth) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        reelUploadManager = reelUploadManager
                    )

                    LaunchedEffect(Unit) {
                        if (startDestination != AppDestination.Auth) {
                            val destination = this@MainActivity.intent.getStringExtra("destination")
                            if (destination == "inbox") {
                                navController.navigate(AppDestination.Inbox)
                            }
                        }
                    }
                }
            }
        }

        createNotificationChannels()


        viewModel.checkForUpdates()
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
                lightColor = Color.RED
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
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onOpenUpdateLink: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onSignOut: () -> Unit,
    onFinishApp: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.observeAsState()
    val authState by viewModel.authState.observeAsState()


    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.NoUpdate -> viewModel.checkUserAuthentication()
            is UpdateState.Error -> {

            }
            else -> {  }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onNavigateToHome()
            is AuthState.Unauthenticated -> onNavigateToAuth()
            is AuthState.Banned -> {
                onShowToast("You are banned & Signed Out.")
                onSignOut()
                onFinish()
            }
            else -> {  }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))


            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(85.dp)
                    .clickable(
                        onClick = onFinish,
                        onClickLabel = "Long press to exit"
                    ),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.weight(3f))


            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Company Trademark",
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 20.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.weight(1f))
        }


        updateState?.let { state ->
            when (state) {
                is UpdateState.UpdateAvailable -> {
                    UpdateDialog(
                        title = state.title,
                        versionName = state.versionName,
                        changelog = state.changelog,
                        updateLink = state.updateLink,
                        isCancelable = state.isCancelable,
                        onUpdate = { onOpenUpdateLink(state.updateLink) },
                        onLater = {
                            if (state.isCancelable) {
                                viewModel.checkUserAuthentication()
                            }
                        }
                    )
                }
                is UpdateState.Error -> {
                    ErrorDialog(
                        message = state.message,
                        onDismiss = {
                            if (!SupabaseClient.isConfigured()) {
                                onFinishApp()
                            } else {
                                viewModel.checkUserAuthentication()
                            }
                        }
                    )
                }
                else -> {  }
            }
        }

        authState?.let { state ->
            if (state is AuthState.Error) {
                ErrorDialog(
                    message = state.message,
                    onDismiss = {
                        if (!SupabaseClient.isConfigured()) {
                            onFinishApp()
                        } else {
                            viewModel.checkUserAuthentication()
                        }
                    }
                )
            }
        }


        if (!SupabaseClient.isConfigured()) {
            ErrorDialog(
                message = "Supabase Configuration Missing\n\nThe app is not properly configured. Please contact the developer to set up the backend services.\n\nConfiguration needed:\n• Supabase URL\n• Supabase API Key\n\nThis is a development/deployment issue that needs to be resolved by the app developer.",
                onDismiss = onFinishApp
            )
        }
    }
}

@Composable
fun UpdateDialog(
    title: String,
    versionName: String,
    changelog: String,
    updateLink: String,
    isCancelable: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    Dialog(onDismissRequest = { if (isCancelable) onLater() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = changelog.replace("\\n", "\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCancelable) Arrangement.SpaceBetween else Arrangement.Center
                ) {
                    if (isCancelable) {
                        TextButton(onClick = onLater) {
                            Text("Later")
                        }
                    }

                    Button(onClick = onUpdate) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}
