package com.synapse.social.studioasinc.ui.settings

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatBytesToGB(bytes: Long): String {
    return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
