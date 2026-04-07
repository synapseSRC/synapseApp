package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.synapse.social.studioasinc.feature.auth.ui.components.ProfileCompletionDialogFragment
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.feature.shared.theme.ThemeManager
import com.synapse.social.studioasinc.core.ui.animation.ActivityTransitions
import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerActivity
import com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    @Inject
    lateinit var reelUploadManager: ReelUploadManager
    
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase by lazy {
        GetCurrentUserIdUseCase(SupabaseAuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        applyThemeFromSettings()

        setContent {
            val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this@HomeActivity)
            val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
                initial = com.synapse.social.studioasinc.ui.settings.AppearanceSettings()
            )

            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM ->
                    isSystemInDarkTheme()
            }

            val dynamicColor = appearanceSettings.dynamicColorEnabled &&
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                HomeScreen(
                    reelUploadManager = reelUploadManager,
                    onNavigateToSearch = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, SearchActivity::class.java)
                        )
                    },
                    onNavigateToProfile = { userId ->
                        val targetUid = if (userId == "me") getCurrentUserIdUseCase() else userId
                        if (targetUid != null) {
                            IntentUtils.openUrl(this, "synapse://profile/$targetUid")
                        }
                    },
                    onNavigateToInbox = {
                        val intent = Intent(this, com.synapse.social.studioasinc.feature.shared.main.MainActivity::class.java).apply {
                            putExtra("destination", "inbox")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToCreatePost = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, CreatePostActivity::class.java)
                        )
                    },
                    onNavigateToQuotePost = { postId ->
                        val intent = Intent(this, com.synapse.social.studioasinc.feature.shared.main.MainActivity::class.java).apply {
                            putExtra("destination", "quote")
                        }
                        startActivity(intent)
                    },
                    onNavigateToStoryViewer = { userId ->
                        val intent = Intent(this@HomeActivity, StoryViewerActivity::class.java).apply {
                            putExtra("user_id", userId)
                        }
                        ActivityTransitions.startActivityWithTransition(this@HomeActivity, intent)
                    },
                    onNavigateToCreateReel = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, CreatePostActivity::class.java).apply {
                                putExtra("type", "reel")
                            }
                        )
                    }
                )
            }
        }

        checkProfileCompletionDialog()
    }

    private fun applyThemeFromSettings() {
        val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this)
        lifecycleScope.launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    ThemeManager.applyThemeMode(settings.themeMode)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Failed to apply theme from settings", e)
            }
        }
    }

    private fun checkProfileCompletionDialog() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val showDialog = sharedPreferences.getBoolean("show_profile_completion_dialog", false)

        if (showDialog) {
            ProfileCompletionDialogFragment().show(supportFragmentManager, ProfileCompletionDialogFragment.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUserId = getCurrentUserIdUseCase()
        if (currentUserId != null) {
        }
    }
}
