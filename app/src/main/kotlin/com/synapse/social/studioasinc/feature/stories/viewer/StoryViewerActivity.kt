package com.synapse.social.studioasinc.feature.stories.viewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userIds = intent.getStringArrayListExtra("user_ids")
        val initialUserId = intent.getStringExtra("user_id")

        if (userIds == null || initialUserId == null) {
            finish()
            return
        }

        val initialIndex = userIds.indexOf(initialUserId).coerceAtLeast(0)

        setContent {
            SynapseTheme {
                StoryPagerScreen(
                    userIds = userIds,
                    initialIndex = initialIndex,
                    onClose = { finish() }
                )
            }
        }
    }
}
