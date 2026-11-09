package com.synapse.social.studioasinc.shared.data.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Resolves the profile picture a social identity provider forwards through Supabase, so a
 * "Continue with Google" sign-in can seed the Synapse profile picture without asking the user to
 * upload one.
 *
 * GoTrue copies the provider profile into `raw_user_meta_data`, which reaches the client as the
 * session user metadata ([JsonObject]). Google publishes the account photo as `picture` and mirrors
 * it to `avatar_url` — the same key the app writes when a user picks a picture during an
 * email/password sign-up — so both keys are read, in that order.
 */
private val PROVIDER_AVATAR_METADATA_KEYS = listOf("picture", "avatar_url")

private const val GOOGLE_AVATAR_HOST_SUFFIX = "googleusercontent.com"
private const val GENERATED_AVATAR_HOST = "api.dicebear.com"

/**
 * First usable provider picture found in [metadata], or null when the provider shared none.
 * Entries that are not absolute http(s) URLs are skipped so a malformed value never reaches storage.
 */
fun providerAvatarUrl(metadata: JsonObject?): String? =
    PROVIDER_AVATAR_METADATA_KEYS
        .asSequence()
        .mapNotNull { key -> (metadata?.get(key) as? JsonPrimitive)?.contentOrNull }
        .map { it.trim() }
        .firstOrNull { isRemoteImageUrl(it) }

/**
 * Provider picture restricted to a Google-hosted image. Used on flows that are not tied to a single
 * provider (the OAuth callback), where applying another provider's photo would be wrong.
 */
fun googleAccountAvatarUrl(metadata: JsonObject?): String? =
    providerAvatarUrl(metadata)?.takeIf { isGoogleAvatarUrl(it) }

/**
 * True when [url] is an absolute http(s) URL with a host, i.e. something an image loader can open.
 * Anything else (blank, a relative path, a non-URL scheme) is treated as no picture at all.
 */
fun isRemoteImageUrl(url: String?): Boolean {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) return false
    val scheme = value.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return false
    val host = hostOf(value)
    return host.isNotEmpty() && host.contains('.')
}

/** Google serves account photos from `googleusercontent.com`. */
fun isGoogleAvatarUrl(url: String?): Boolean {
    val host = hostOf(url?.trim().orEmpty())
    return host == GOOGLE_AVATAR_HOST_SUFFIX || host.endsWith("." + GOOGLE_AVATAR_HOST_SUFFIX)
}

/** DiceBear initials avatars are Synapse-generated placeholders, not a picture the user picked. */
fun isGeneratedAvatarUrl(url: String?): Boolean =
    hostOf(url?.trim().orEmpty()) == GENERATED_AVATAR_HOST

/**
 * Decides which picture the profile record should hold once [providedAvatarUrl] is known.
 *
 * Returns null when nothing must change: the provider shared no usable picture, the profile already
 * holds that exact picture, or the stored picture is the user's own upload — a custom picture always
 * wins over the provider default and a later Google sign-in must never overwrite it.
 *
 * Placeholders (a provider picture that has changed, or a generated initials avatar) are replaced,
 * which is what keeps an imported Google photo fresh across sign-ins.
 */
fun resolveStoredAvatar(currentAvatar: String?, providedAvatarUrl: String?): String? {
    val provided = providedAvatarUrl?.trim() ?: return null
    if (!isRemoteImageUrl(provided)) return null

    val current = currentAvatar?.trim().orEmpty()
    if (current.isEmpty()) return provided
    if (current == provided) return null

    return when {
        isGoogleAvatarUrl(current) || isGeneratedAvatarUrl(current) -> provided
        else -> null
    }
}

/** Host of [url] in lower case, without a user-info section or port. Empty when there is none. */
private fun hostOf(url: String): String {
    if (url.isEmpty()) return ""
    return url
        .substringAfter("://", missingDelimiterValue = url)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')
        .lowercase()
}
