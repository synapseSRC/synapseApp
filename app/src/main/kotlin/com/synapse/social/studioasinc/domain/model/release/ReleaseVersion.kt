package com.synapse.social.studioasinc.domain.model.release

/**
 * A release tag or installed version name compared the way versions are, so `v1.10.0` sorts after
 * `v1.9.2` and build suffixes such as `1.0.0-a1b2c3d` are ignored.
 */
class ReleaseVersion(val raw: String) : Comparable<ReleaseVersion> {

    private val segments: List<Int> = raw
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.trim().toIntOrNull() ?: 0 }

    override fun compareTo(other: ReleaseVersion): Int {
        for (index in 0 until maxOf(segments.size, other.segments.size)) {
            val comparison = (segments.getOrNull(index) ?: 0).compareTo(other.segments.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    override fun toString(): String = raw.trim()

    override fun equals(other: Any?): Boolean = other is ReleaseVersion && compareTo(other) == 0

    override fun hashCode(): Int = segments.dropWhile { it == 0 }.hashCode()

    companion object {
        /** Parses [value], returning `null` for a missing or blank tag rather than a zero version. */
        fun of(value: String?): ReleaseVersion? =
            value?.trim()?.takeIf { it.isNotEmpty() }?.let(::ReleaseVersion)
    }
}
