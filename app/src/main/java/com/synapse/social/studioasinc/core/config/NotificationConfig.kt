package com.synapse.social.studioasinc.core.config

import com.synapse.social.studioasinc.BuildConfig



object NotificationConfig {




    const val USE_CLIENT_SIDE_NOTIFICATIONS = true





    const val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID





    const val NOTIFICATION_TITLE = "New Message"
    const val NOTIFICATION_SUBTITLE = "Synapse Social"
    const val NOTIFICATION_CHANNEL_ID = "messages"
    const val NOTIFICATION_PRIORITY = 10


    const val NOTIFICATION_TYPE_NEW_POST = "NEW_POST"
    const val NOTIFICATION_TYPE_NEW_COMMENT = "NEW_COMMENT"
    const val NOTIFICATION_TYPE_NEW_REPLY = "NEW_REPLY"
    const val NOTIFICATION_TYPE_NEW_LIKE_POST = "NEW_LIKE_POST"
    const val NOTIFICATION_TYPE_NEW_LIKE_COMMENT = "NEW_LIKE_COMMENT"
    const val NOTIFICATION_TYPE_MENTION = "MENTION"
    const val NOTIFICATION_TYPE_NEW_FOLLOWER = "NEW_FOLLOWER"


    const val NOTIFICATION_TITLE_NEW_POST = "New Post"
    const val NOTIFICATION_TITLE_NEW_COMMENT = "New Comment"
    const val NOTIFICATION_TITLE_NEW_REPLY = "New Reply"
    const val NOTIFICATION_TITLE_NEW_LIKE_POST = "New Like"
    const val NOTIFICATION_TITLE_NEW_LIKE_COMMENT = "New Like"
    const val NOTIFICATION_TITLE_MENTION = "New Mention"


    const val WORKER_URL = "https://my-app-notification-sender.mashikahamed0.workers.dev"
    const val EDGE_FUNCTION_SEND_PUSH = "send-push-notification"


    const val TAG_LIKES = "likes"
    const val TAG_COMMENTS = "comments"
    const val TAG_REPLIES = "replies"
    const val TAG_FOLLOWS = "follows"
    const val TAG_MENTIONS = "mentions"
    const val TAG_NEW_POSTS = "new_posts"
    const val TAG_SHARES = "shares"
    const val TAG_GLOBAL_ENABLED = "global_enabled"




    const val RECENT_ACTIVITY_THRESHOLD = 5 * 60 * 1000L



    const val ENABLE_SMART_SUPPRESSION = true


    const val ENABLE_FALLBACK_MECHANISMS = true


    const val ENABLE_DEEP_LINKING = true



    const val ENABLE_DEBUG_LOGGING = true



    fun isConfigurationValid(): Boolean {
        return if (USE_CLIENT_SIDE_NOTIFICATIONS) {
            ONESIGNAL_APP_ID.isNotBlank() && ONESIGNAL_APP_ID != "YOUR_ONESIGNAL_APP_ID_HERE"

        } else {
            true
        }
    }



    fun getTitleForNotificationType(type: String): String {
        return when (type) {
            NOTIFICATION_TYPE_NEW_POST -> NOTIFICATION_TITLE_NEW_POST
            NOTIFICATION_TYPE_NEW_COMMENT -> NOTIFICATION_TITLE_NEW_COMMENT
            NOTIFICATION_TYPE_NEW_REPLY -> NOTIFICATION_TITLE_NEW_REPLY
            NOTIFICATION_TYPE_NEW_LIKE_POST -> NOTIFICATION_TITLE_NEW_LIKE_POST
            NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> NOTIFICATION_TITLE_NEW_LIKE_COMMENT
            NOTIFICATION_TYPE_MENTION -> NOTIFICATION_TITLE_MENTION
            else -> NOTIFICATION_TITLE
        }
    }



    fun getNotificationSystemDescription(): String {
        return if (USE_CLIENT_SIDE_NOTIFICATIONS) {
            "Client-side OneSignal REST API"
        } else {
            "Server-side Cloudflare Worker"
        }
    }



    fun getConfigurationStatus(): Map<String, Any> {
        return mapOf(
            "useClientSideNotifications" to USE_CLIENT_SIDE_NOTIFICATIONS,
            "systemDescription" to getNotificationSystemDescription(),
            "isConfigurationValid" to isConfigurationValid(),
            "enableSmartSuppression" to ENABLE_SMART_SUPPRESSION,
            "enableFallbackMechanisms" to ENABLE_FALLBACK_MECHANISMS,
            "enableDeepLinking" to ENABLE_DEEP_LINKING,
            "enableDebugLogging" to ENABLE_DEBUG_LOGGING,
            "recentActivityThreshold" to RECENT_ACTIVITY_THRESHOLD
        )
    }
}
