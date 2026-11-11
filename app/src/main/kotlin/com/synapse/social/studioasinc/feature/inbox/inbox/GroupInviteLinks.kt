package com.synapse.social.studioasinc.feature.inbox.inbox

import android.net.Uri
import com.synapse.social.studioasinc.shared.core.config.SynapseConfig

/**
 * Single source of truth for group invitation links.
 *
 * The invite cards that display a code ([GroupHubStage], [GroupInfoScreen]) and the activity that
 * receives one ([GroupInviteActivity]) all read the link format from here, so what the app shares
 * and what it accepts cannot drift apart.
 *
 * An invitation is shared as `https://{website}/g/{code}` on [SynapseConfig.APP_WEBSITE_URL] - the
 * same website the rest of the app links to, and the host that serves the `assetlinks.json` naming
 * this package. The host matters rather than decorating the link: Android only hands an `https`
 * link to the app when the host serves that file (the repo serves it from
 * `web/public/.well-known/assetlinks.json`), so an invitation pointing at a host the product does
 * not serve can never be verified and opens in the browser instead of the join screen.
 *
 * `synapse://invite/{code}` is accepted as well. It needs no verification, so an invitation still
 * reaches the app on a build whose certificate is missing from `assetlinks.json`, or on a device
 * that could not verify the host while the app was being installed. Only the `https` form is
 * shared, because it is the one every recipient can open.
 */
object GroupInviteLinks {
    private const val SCHEME = "synapse"
    private const val SCHEME_HOST = "invite"
    private const val PATH_PREFIX = "g"

    /** `generate_group_invite_code()` returns `substr(md5(...), 1, 16)`: accept 16-64 hex characters. */
    private const val MIN_CODE_LENGTH = 16
    private const val MAX_CODE_LENGTH = 64

    private val websiteHost: String = Uri.parse(SynapseConfig.APP_WEBSITE_URL).host.orEmpty()

    /** The shareable link for [code]: shown, copied, shared and encoded into the invitation QR code. */
    fun httpsLink(code: String): String = "https://$websiteHost/$PATH_PREFIX/$code"

    /**
     * The invitation code carried by [uri], or `null` when [uri] is not a group invitation link.
     *
     * `null` for an unrecognised link is deliberate: [GroupInviteScreen] then shows its invalid or
     * expired state instead of resolving a code the link never carried.
     */
    fun inviteCode(uri: Uri?): String? {
        if (uri == null) return null
        val code = when {
            uri.scheme == "https" && uri.host == websiteHost -> uri.lastPathSegment
                ?.takeIf { uri.pathSegments.size == 2 && uri.pathSegments.firstOrNull() == PATH_PREFIX }
            uri.scheme == SCHEME && uri.host == SCHEME_HOST -> uri.lastPathSegment
            else -> null
        }
        return code?.takeIf(::isInviteCode)
    }

    private fun isInviteCode(code: String): Boolean =
        code.length in MIN_CODE_LENGTH..MAX_CODE_LENGTH && code.all { character ->
            character in '0'..'9' || character.lowercaseChar() in 'a'..'f'
        }
}
