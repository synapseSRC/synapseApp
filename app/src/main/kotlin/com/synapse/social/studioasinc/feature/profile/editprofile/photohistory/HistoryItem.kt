package com.synapse.social.studioasinc.presentation.editprofile.photohistory

data class HistoryItem(
    val id: String,
    val userId: String,
    /**
     * The reference exactly as the history table stores it. Picking an entry writes this value back
     * to the profile, so it stays unresolved: a signed URL would expire on the profile row.
     */
    val imageUrl: String,
    val createdAt: String? = null,
    /**
     * URL the history grid can load. Entries in a private bucket (covers) are signed for display, and
     * it falls back to [imageUrl] when signing is not possible.
     */
    val displayUrl: String = imageUrl
)

enum class PhotoType {
    PROFILE, COVER
}

/**
 * Identity of the stored object behind a media URL.
 *
 * A cover entry holds a `private-media://covers/...` reference while the profile holds a freshly
 * signed URL for that same object, so comparing the two strings never matches. Comparing the object
 * path instead keeps "is this the current cover?" stable, for both the selected marker in the grid
 * and the entry removal that clears the current photo.
 */
internal fun storageObjectKey(reference: String?): String? {
    val value = reference
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    if (value.startsWith("private-media://")) {
        return value.removePrefix("private-media://").trimStart('/')
    }

    val absoluteMarker = value.indexOf("/storage/v1/object/")
        .takeIf { it >= 0 }
        ?.plus("/storage/v1/object/".length)
    val relativeMarker = value.indexOf("/object/")
        .takeIf { it >= 0 }
        ?.plus("/object/".length)
    val marker = absoluteMarker ?: relativeMarker

    if (marker != null) {
        val segments = value.substring(marker).split('/', limit = 3)
        if (segments.size == 3) {
            return "${segments[1]}/${segments[2]}"
        }
    }

    return value
}
