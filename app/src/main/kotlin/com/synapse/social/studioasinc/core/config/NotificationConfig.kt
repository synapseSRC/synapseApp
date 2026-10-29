package com.synapse.social.studioasinc.core.config

import com.synapse.social.studioasinc.BuildConfig

object NotificationConfig {
    const val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID

    const val TAG_LIKES = "likes"
    const val TAG_COMMENTS = "comments"
    const val TAG_REPLIES = "replies"
    const val TAG_FOLLOWS = "follows"
    const val TAG_MENTIONS = "mentions"
    const val TAG_NEW_POSTS = "new_posts"
    const val TAG_SHARES = "shares"
    const val TAG_MESSAGES = "messages"
    const val TAG_GLOBAL_ENABLED = "global_enabled"

    val ENABLE_DEBUG_LOGGING = BuildConfig.DEBUG
}
