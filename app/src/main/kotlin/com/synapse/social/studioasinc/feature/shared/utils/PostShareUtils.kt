package com.synapse.social.studioasinc.feature.shared.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.navigation.DeepLink

object PostShareUtils {
    /**
     * Canonical, copyable permalink for a single post.
     *
     * This is the [DeepLink] `synapse://post/{postId}` destination that `MainActivity` registers,
     * so the copied link always identifies that exact post.
     */
    fun getPostLink(postId: String): String = DeepLink.post(postId)

    fun getPostShareText(context: Context, postId: String): String {
        return context.getString(R.string.share_post_synapse_text, postId)
    }

    /**
     * Copies the [postId] permalink to the system clipboard and reports the real outcome, so a
     * failed copy never looks like a success.
     *
     * @return `true` when the link was placed on the clipboard.
     */
    fun copyPostLink(context: Context, postId: String): Boolean {
        val link = if (postId.isBlank()) null else getPostLink(postId)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        if (link == null || clipboard == null) {
            Toast.makeText(context, R.string.link_copy_failed, Toast.LENGTH_SHORT).show()
            return false
        }
        val clip = ClipData.newPlainText(context.getString(R.string.clip_label_post_link), link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
        return true
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
