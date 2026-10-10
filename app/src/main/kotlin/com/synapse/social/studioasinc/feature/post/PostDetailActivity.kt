package com.synapse.social.studioasinc.feature.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.synapse.social.studioasinc.CreatePostActivity
import com.synapse.social.studioasinc.core.ui.base.BaseComposeActivity
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostDetailActivity : BaseComposeActivity() {

    companion object {
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_AUTHOR_UID = "author_uid"
        const val EXTRA_COMMENT_ID = "comment_id"

        fun start(context: Context, postId: String, authorUid: String? = null) {
            context.startActivity(Intent(context, PostDetailActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_AUTHOR_UID, authorUid)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        val commentId = intent.getStringExtra(EXTRA_COMMENT_ID)

        if (postId == null) {
            finish()
            return
        }

        setSynapseContent {
            PostDetailScreen(
                postId = postId,
                rootCommentId = commentId,
                onNavigateBack = { finish() },
                onNavigateToProfile = { userId -> navigateToProfile(userId) },
                onNavigateToEditPost = { editPostId -> navigateToEditPost(editPostId) },
                onNavigateToReplyToPost = { replyPostId -> navigateToReplyToPost(replyPostId) },
                onNavigateToCommentDetail = { pId, cId ->
                    val intent = Intent(this, PostDetailActivity::class.java).apply {
                        putExtra(EXTRA_POST_ID, pId)
                        putExtra(EXTRA_COMMENT_ID, cId)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    private fun navigateToProfile(userId: String) {
        IntentUtils.openUrl(this, "synapse://profile/$userId")
    }

    private fun navigateToEditPost(postId: String) {
        startActivity(Intent(this, CreatePostActivity::class.java).apply {
            putExtra("edit_post_id", postId)
        })
    }

    private fun navigateToReplyToPost(postId: String) {
        startActivity(Intent(this, CreatePostActivity::class.java).apply {
            putExtra("reply_to_post_id", postId)
        })
    }
}
