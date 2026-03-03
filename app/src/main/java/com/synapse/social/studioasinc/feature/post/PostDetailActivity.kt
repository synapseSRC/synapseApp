package com.synapse.social.studioasinc.feature.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.synapse.social.studioasinc.CreatePostActivity
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_AUTHOR_UID = "author_uid"

        fun start(context: Context, postId: String, authorUid: String? = null) {
            context.startActivity(Intent(context, PostDetailActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_AUTHOR_UID, authorUid)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val postId = intent.getStringExtra(EXTRA_POST_ID)

        if (postId == null) {
            finish()
            return
        }

        setContent {
            SynapseTheme {
                PostDetailScreen(
                    postId = postId,
                    onNavigateBack = { finish() },
                    onNavigateToProfile = { userId -> navigateToProfile(userId) },
                    onNavigateToEditPost = { editPostId -> navigateToEditPost(editPostId) }
                )
            }
        }
    }

    private fun navigateToProfile(userId: String) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("synapse://profile/$userId"))
        intent.setClass(this, com.synapse.social.studioasinc.feature.shared.main.MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToEditPost(postId: String) {
        startActivity(Intent(this, CreatePostActivity::class.java).apply {
            putExtra("edit_post_id", postId)
        })
    }
}
