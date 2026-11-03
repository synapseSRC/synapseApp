package com.synapse.social.studioasinc.ui.navigation

/**
 * Single source of truth for the app's `synapse://` deep-link format.
 *
 * Every deep-linkable destination is expressed as `synapse://{host}/{id}` and is resolved by the
 * matching [androidx.navigation.navDeepLink] registered on that destination's composable in
 * [AppNavigation]. `MainActivity` owns the `synapse://` intent-filters, so a link like
 * [post] routes straight to the right screen regardless of where it was tapped (feed embed,
 * chat bubble, markdown @mention/#tag, shortcut, clipboard).
 *
 * To make a new destination deep-linkable: add a builder here and a `navDeepLink` entry on its
 * composable, then register the host on `MainActivity`'s intent-filters.
 */
object DeepLink {
    const val SCHEME = "synapse"

    /** `synapse://post/{postId}` -> [AppDestination.PostDetail] */
    fun post(postId: String): String = "$SCHEME://post/$postId"

    /** `synapse://profile/{userId}` -> [AppDestination.Profile] */
    fun profile(userId: String): String = "$SCHEME://profile/$userId"

    /** `synapse://hashtag/{tag}` -> [AppDestination.HashtagFeed] */
    fun hashtag(tag: String): String = "$SCHEME://hashtag/$tag"

    /** `synapse://chat/{chatId}` -> [AppDestination.Chat] */
    fun chat(chatId: String): String = "$SCHEME://chat/$chatId"

    /** `synapse://home` -> [AppDestination.Home] */
    fun home(): String = "$SCHEME://home"
}
