package com.synapse.social.studioasinc.shared.core.util

object EducationSanitizer {

    fun sanitizeEducationString(raw: String?): String? {
        if (raw == null) return null
        val list = sanitizeEducationItem(raw)
        return list.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    fun sanitizeEducationList(rawList: List<String>): List<String> {
        return rawList.flatMap { sanitizeEducationItem(it) }.filter { it.isNotBlank() }
    }

    fun sanitizeEducationItem(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        var s = raw.trim()

        var changed = true
        var depth = 0
        while (changed && depth < 20) {
            changed = false
            depth++

            val trimmed = s.trim()
            if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))
            ) {
                if (trimmed.length >= 2) {
                    s = trimmed.substring(1, trimmed.length - 1)
                    changed = true
                }
            }
        }

        s = s.replace("\\\"", "\"").replace("\\", "").trim()

        depth = 0
        changed = true
        while (changed && depth < 10) {
            changed = false
            depth++

            val trimmed = s.trim()
            if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))
            ) {
                if (trimmed.length >= 2) {
                    s = trimmed.substring(1, trimmed.length - 1)
                    changed = true
                }
            }
        }

        if (s.isBlank()) return emptyList()

        return s.split(",")
            .map { item ->
                item.trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                    .removeSurrounding("[")
                    .removeSurrounding("]")
                    .trim()
            }
            .filter { it.isNotBlank() && it != "[]" }
    }
}
