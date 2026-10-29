package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.GitHubReleaseDto
import com.synapse.social.studioasinc.domain.model.release.AppRelease
import com.synapse.social.studioasinc.domain.model.release.ReleaseNotesParser
import com.synapse.social.studioasinc.domain.model.release.ReleaseVersion
import java.net.URI
import kotlinx.datetime.Instant

/**
 * Turns GitHub release entries into changelog data, dropping anything that cannot be presented
 * honestly: drafts, prereleases, entries without a usable tag, and entries whose page link does not
 * point back at this repository on github.com.
 */
object AppReleaseMapper {

    /** Published releases only, newest first, one entry per tag. */
    fun toDomainList(dtos: List<GitHubReleaseDto>): List<AppRelease> =
        dtos.asSequence()
            .filter { entry -> !entry.draft && !entry.prerelease }
            .mapNotNull { entry -> toDomain(entry) }
            .distinctBy { release -> release.tag.lowercase() }
            .sortedWith(
                compareByDescending<AppRelease> { release -> release.publishedAtMillis ?: Long.MIN_VALUE }
                    .thenByDescending { release -> ReleaseVersion.of(release.tag) ?: ReleaseVersion("") }
            )
            .toList()

    fun toDomain(dto: GitHubReleaseDto): AppRelease? {
        val tag = dto.tagName.trim()
        val page = dto.htmlUrl.trim()
        if (tag.isEmpty() || !isReleasePage(page)) return null
        return AppRelease(
            tag = tag,
            title = dto.name?.trim()?.takeIf { value -> value.isNotEmpty() } ?: tag,
            url = page,
            publishedAtMillis = parseInstant(dto.publishedAt) ?: parseInstant(dto.createdAt),
            groups = ReleaseNotesParser.parse(dto.body)
        )
    }

    /** Release pages live at `github.com/<owner>/<repo>/releases/tag/<tag>`; nothing else is linked. */
    private fun isReleasePage(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("github.com", ignoreCase = true) &&
            (uri.port == -1 || uri.port == 443) &&
            uri.rawUserInfo == null &&
            uri.path?.lowercase()?.startsWith(RELEASE_PATH_PREFIX) == true
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun parseInstant(value: String?): Long? {
        val raw = value?.trim()?.takeIf { text -> text.isNotEmpty() } ?: return null
        return try {
            Instant.parse(raw).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private const val RELEASE_PATH_PREFIX = "/synapsesrc/synapseapp/releases/"
}
