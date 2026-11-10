package com.synapse.social.studioasinc.shared.data.util

import io.ktor.http.URLBuilder

/**
 * Builds a first-letters avatar URL from the DiceBear initials API — the same technique used for the
 * profile picture during account creation. Prefer this over client-side initials rendering so every
 * avatar is a real image the UI can load uniformly.
 *
 * The PNG format is requested on purpose: Android decodes notification large icons through
 * BitmapFactory, which cannot rasterize SVG, so an SVG URL silently drops the picture from
 * push notifications. Coil still renders SVG elsewhere, but every stored avatar URL must stay a
 * bitmap-native format for the notification path to work.
 *
 * @param seedName Name used to derive the displayed letters and the palette (typically a username or group name).
 * @param fallbackSeed Seed used when [seedName] is blank.
 */
internal fun initialsAvatarUrl(seedName: String, fallbackSeed: String = "member"): String {
    val seed = seedName.trim().lowercase().ifBlank { fallbackSeed }
    return URLBuilder("https://api.dicebear.com/9.x/initials/png").apply {
        parameters.append("seed", seed)
    }.buildString()
}
