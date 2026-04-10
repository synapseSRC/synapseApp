package com.synapse.social.studioasinc.feature.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.AuthActivity
import com.synapse.social.studioasinc.feature.profile.ProfileEditActivity
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsNavHost
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch



@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var cachedConversationDao: CachedConversationDao

    @Inject
    lateinit var cachedMessageDao: CachedMessageDao

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("SettingsActivity", "onCreate called")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            android.util.Log.d("SettingsActivity", "Setting up content")
            setContent {

                val appearanceViewModel: AppearanceViewModel = viewModel()
                val appearanceSettings by appearanceViewModel.appearanceSettings.collectAsState()


                val darkTheme = when (appearanceSettings.themeMode) {
                    com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                    com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                    com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM ->
                        androidx.compose.foundation.isSystemInDarkTheme()
                }


                val dynamicColor = appearanceSettings.dynamicColorEnabled &&
                                   Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

                SynapseTheme(
                    darkTheme = darkTheme,
                    dynamicColor = dynamicColor
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SettingsNavHost(
                            onBackClick = {

                                finish()
                            },
                            onNavigateToProfileEdit = {

                                startActivity(Intent(this@SettingsActivity, ProfileEditActivity::class.java))
                            },
                            onNavigateToChatPrivacy = {
                                Toast.makeText(this@SettingsActivity, "Chat feature not implemented", Toast.LENGTH_SHORT).show()
                            },
                            onLogout = {
                                performLogout()
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error in onCreate", e)
            throw e
        }
    }



    private fun performLogout() {
        lifecycleScope.launch {
            cachedConversationDao.deleteAll()
            cachedMessageDao.deleteAll()
            authRepository.signOut()
            startActivity(Intent(this@SettingsActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
