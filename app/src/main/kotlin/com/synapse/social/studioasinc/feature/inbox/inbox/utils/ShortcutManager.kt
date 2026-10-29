package com.synapse.social.studioasinc.feature.inbox.inbox.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat

class ShortcutManager(private val context: Context) {

    fun createChatShortcut(
        chatId: String,
        chatName: String,
        avatarUrl: String?
    ): Result<Boolean> {
        return try {
            val intent = Intent(context, com.synapse.social.studioasinc.core.ui.base.BaseComposeActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("synapse://chat/$chatId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcutInfo = ShortcutInfoCompat.Builder(context, "chat_$chatId")
                .setShortLabel(chatName)
                .setLongLabel(chatName)
                .setIntent(intent)
                .build()

            Result.success(
                ShortcutManagerCompat.isRequestPinShortcutSupported(context) &&
                    ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
