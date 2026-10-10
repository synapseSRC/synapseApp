package com.synapse.social.studioasinc.feature.shared.utils

import android.content.Context
import android.content.Intent
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.home.home.FeedViewModel
import com.synapse.social.studioasinc.feature.shared.components.post.PostActionsFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PostShareUtilsTest {

    @Test
    fun getPostShareUrl_returnsValidSynapseDeepLink() {
        val postId = "post_12345"
        val expectedUrl = "synapse://post/post_12345"
        assertEquals(expectedUrl, PostShareUtils.getPostShareUrl(postId))
    }

    @Test
    fun getPostShareText_formatsWithResString() {
        val context: Context = mock()
        val postId = "post_999"
        val expectedText = "Check out this post on Synapse: synapse://post/post_999"

        whenever(context.getString(eq(R.string.share_post_synapse_text), eq(postId)))
            .thenReturn(expectedText)

        val result = PostShareUtils.getPostShareText(context, postId)
        assertEquals(expectedText, result)
    }

    @Test
    fun sharePost_launchesChooserIntentWithPostDeepLink() {
        val context: Context = mock()
        val postId = "post_abc"
        val shareText = "Check out this post on Synapse: synapse://post/post_abc"
        val chooserTitle = "Share via"

        whenever(context.getString(eq(R.string.share_post_synapse_text), eq(postId)))
            .thenReturn(shareText)
        whenever(context.getString(R.string.title_share_post))
            .thenReturn(chooserTitle)

        PostShareUtils.sharePost(context, postId)

        val intentCaptor = argumentCaptor<Intent>()
        verify(context).startActivity(intentCaptor.capture())

        val chooserIntent = intentCaptor.firstValue
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        assertEquals(shareText, targetIntent?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun shareComment_launchesChooserIntentWithCommentContent() {
        val context: Context = mock()
        val commentContent = "This is a great comment!"
        val chooserTitle = "Share Comment"

        whenever(context.getString(R.string.title_share_comment))
            .thenReturn(chooserTitle)

        PostShareUtils.shareComment(context, commentContent)

        val intentCaptor = argumentCaptor<Intent>()
        verify(context).startActivity(intentCaptor.capture())

        val chooserIntent = intentCaptor.firstValue
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        assertEquals(commentContent, targetIntent?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun postActionsFactory_wiresOnShareCallbackCorrectly() {
        val feedViewModel: FeedViewModel = mock()
        val testPost = Post(id = "p1", authorUid = "u1", postText = "Hello")
        var shareCalledWithPost: Post? = null

        val actions = PostActionsFactory.create(
            viewModel = feedViewModel,
            onComment = {},
            onShare = { post -> shareCalledWithPost = post },
            onQuote = {},
            onUserClick = {},
            onOptionClick = {},
            onMediaClick = {}
        )

        actions.onShare(testPost)
        assertEquals(testPost, shareCalledWithPost)
    }
}
