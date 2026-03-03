package com.synapse.social.studioasinc.shared.core.util



fun sanitizeSearchQuery(query: String): String {
    return query.trim()
        .take(100)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
