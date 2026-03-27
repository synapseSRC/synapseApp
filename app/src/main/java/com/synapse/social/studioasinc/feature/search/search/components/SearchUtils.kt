package com.synapse.social.studioasinc.feature.search.search.components

fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> "${String.format("%.1f", count / 1000.0)}K"
        else -> "${String.format("%.1f", count / 1000000.0)}M"
    }
}

fun formatTime(timestamp: String): String {

    return "recent"
}
