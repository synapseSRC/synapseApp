package com.synapse.social.studioasinc.core.util

import android.content.Context
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.core.util.NotificationHelper
import com.synapse.social.studioasinc.core.di.DatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationUtils {

    fun sendPostLikeNotification(context: Context, postKey: String, postAuthorUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == postAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)
                val message = context.getString(R.string.notification_like_post, senderName)

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                }

                NotificationHelper.sendNotification(
                    postAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send post like notification: ${e.message}")
            }
        }
    }

    fun sendPostCommentNotification(context: Context, postKey: String, postAuthorUid: String, commentText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == postAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)
                val message = context.getString(R.string.notification_comment_post, senderName)

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    put("commentText", commentText.take(100))
                }

                NotificationHelper.sendNotification(
                    postAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send comment notification: ${e.message}")
            }
        }
    }

    fun sendCommentLikeNotification(context: Context, postKey: String, commentKey: String, commentAuthorUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == commentAuthorUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)
                val message = context.getString(R.string.notification_like_comment, senderName)

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    put("commentId", commentKey)
                }

                NotificationHelper.sendNotification(
                    commentAuthorUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send comment like notification: ${e.message}")
            }
        }
    }

    fun sendFollowNotification(context: Context, followedUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == followedUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)
                val message = context.getString(R.string.notification_follow, senderName)

                val data = hashMapOf<String, String>().apply {
                    put("followerId", currentUser.id)
                }

                NotificationHelper.sendNotification(
                    followedUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_FOLLOWER,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send follow notification: ${e.message}")
            }
        }
    }

    fun sendChatMessageNotification(context: Context, recipientUid: String, messageText: String, chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == recipientUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)

                val truncatedMessage = if (messageText.length > 50) {
                    messageText.take(47) + "..."
                } else {
                    messageText
                }

                val data = hashMapOf<String, String>().apply {
                    put("type", "chat_message")
                    put("sender_uid", currentUser.id)
                    put("chat_id", chatId)
                    put("message_preview", truncatedMessage)
                }

                NotificationHelper.sendNotification(
                    recipientUid,
                    currentUser.id,
                    "$senderName: $truncatedMessage",
                    "chat_message",
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send chat message notification: ${e.message}")
            }
        }
    }

    fun sendMentionNotification(context: Context, mentionedUid: String, postKey: String, commentKey: String?, contentType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = SupabaseAuthenticationService()
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                val userDao = entryPoint.getUserDao()
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                val currentUser = authService.getCurrentUser()
                if (currentUser == null || currentUser.id == mentionedUid) {
                    return@launch
                }

                val senderUser = userRepository.getUserById(currentUser.id).getOrNull()
                val senderName = senderUser?.username ?: context.getString(R.string.someone)
                val message = context.getString(R.string.notification_mention, senderName, contentType)

                val data = hashMapOf<String, String>().apply {
                    put("postId", postKey)
                    commentKey?.let { put("commentId", it) }
                }

                NotificationHelper.sendNotification(
                    mentionedUid,
                    currentUser.id,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_MENTION,
                    data
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationUtils", "Failed to send mention notification: ${e.message}")
            }
        }
    }
}
