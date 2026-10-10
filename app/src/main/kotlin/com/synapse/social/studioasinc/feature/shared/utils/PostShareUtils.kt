package com.synapse.social.studioasinc.feature.shared.utils

import android.content.Context
import android.content.Intent
import com.synapse.social.studioasinc.R

object PostShareUtils {

    fun getPostShareUrl(postId: String): String {
        return "synapse://post/$postId"
    }

    fun getPostShareText(context: Context, postId: String): String {
        return context.getString(R.string.share_post_synapse_text, postId)
    }

    fun sharePost(context: Context, postId: String) {
        if (postId.isBlank()) return
        val shareText = getPostShareText(context, postId)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooserTitle = context.getString(R.string.title_share_post)
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun shareComment(context: Context, content: String) {
        if (content.isBlank()) return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooserTitle = context.getString(R.string.title_share_comment)
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
}
