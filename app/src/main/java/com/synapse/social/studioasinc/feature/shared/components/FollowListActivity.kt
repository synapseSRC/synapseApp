package com.synapse.social.studioasinc.feature.shared.components

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.feature.shared.components.compose.FollowListScreen
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FollowListActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_INITIAL_TAB = "initial_tab"
        const val TYPE_FOLLOWERS = "followers"
        const val TYPE_FOLLOWING = "following"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra(EXTRA_USER_ID)
        val initialTab = intent.getIntExtra(EXTRA_INITIAL_TAB, 0)

        if (userId == null) {
            finish()
            return
        }

        setContent {
            SynapseTheme {
                FollowListScreen(
                    userId = userId,
                    initialTab = initialTab,
                    onNavigateBack = { finish() },
                    onUserClick = { targetUserId ->
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("synapse://profile/$targetUserId"))
                        intent.setClass(this@FollowListActivity, com.synapse.social.studioasinc.feature.shared.main.MainActivity::class.java)
                        startActivity(intent)
                    },
                    onMessageClick = { targetUserId, _ ->
                        startDirectChat(targetUserId)
                    }
                )
            }
        }
    }

    private fun startDirectChat(targetUserId: String) {
        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()

                if (currentUserId == null) {
                    return@launch
                }

                if (targetUserId == currentUserId) {
                    return@launch
                }

                Toast.makeText(this@FollowListActivity, "Chat feature not implemented", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {

            }
        }
    }
}
