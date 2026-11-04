package com.synapse.social.studioasinc.shared.data.util

import io.ktor.http.URLBuilder

/**
 * Builds a first-letters avatar URL from the DiceBear initials API — the same technique used for the
 * profile picture during account creation. Prefer this over client-side initials rendering so every
 * avatar is a real image the UI can load uniformly.
 *
 * @param seedName Name used to derive the displayed letters and the palette (typically a username or group name).
 * @param fallbackSeed Seed used when [seedName] is blank.
 */
internal fun initialsAvatarUrl(seedName: String, fallbackSeed: String = "member"): String {
    val seed = seedName.trim().lowercase().ifBlank { fallbackSeed }
    return URLBuilder("https://api.dicebear.com/9.x/initials/svg").apply {
        parameters.append("seed", seed)
    }.buildString()
}
