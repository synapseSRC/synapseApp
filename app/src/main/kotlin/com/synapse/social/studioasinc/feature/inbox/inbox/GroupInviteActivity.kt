package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.synapse.social.studioasinc.feature.shared.main.MainActivity
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.core.navigation.NotificationNavigationCoordinator
import org.json.JSONObject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupInviteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inviteCode = intent?.data
            ?.takeIf { uri ->
                uri.scheme == "https" && uri.host == "synapse.chat" &&
                    uri.pathSegments.size == 2 && uri.pathSegments.firstOrNull() == "g"
            }
            ?.lastPathSegment
            ?.takeIf { code ->
                code.length == 32 && code.all { character ->
                    character in '0'..'9' || character.lowercaseChar() in 'a'..'f'
                }
            }
            .orEmpty()
        setContent {
            SynapseTheme {
                GroupInviteScreen(
                inviteCode = inviteCode,
                onNavigateBack = { finish() },
                onGroupOpened = { chatId ->
                    val navigation = JSONObject().put(
                        "navigation",
                        JSONObject().put("target", "CHAT").put("chatId", chatId)
                    )
                    NotificationNavigationCoordinator.onNotificationClick(navigation)
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                }
                )
            }
        }
    }
}
