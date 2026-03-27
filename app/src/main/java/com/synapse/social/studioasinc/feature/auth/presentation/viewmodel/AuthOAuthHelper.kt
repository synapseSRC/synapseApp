package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import android.net.Uri
import com.synapse.social.studioasinc.shared.domain.model.OAuthDeepLink
import io.github.aakira.napier.Napier

object AuthOAuthHelper {
    fun parseDeepLink(uri: Uri?): OAuthDeepLink? {
        if (uri == null) return null

        Napier.d("Handling deep link: $uri", tag = "AuthViewModel")

        val code = uri.getQueryParameter("code")
        val fragment = uri.fragment

        var accessToken: String? = null
        var refreshToken: String? = null
        var error: String? = null
        var errorDescription: String? = null

        if (fragment != null) {
            Napier.d("Deep link has fragment: $fragment", tag = "AuthViewModel")
            val params = fragment.split("&").associate {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else "" to ""
            }
            accessToken = params["access_token"]
            refreshToken = params["refresh_token"]
            error = params["error"]
            errorDescription = params["error_description"]
        }

        if (uri.getQueryParameter("error") != null) {
            error = uri.getQueryParameter("error")
            errorDescription = uri.getQueryParameter("error_description")
            Napier.e("OAuth error in deep link: $error - $errorDescription", tag = "AuthViewModel")
        }

        return OAuthDeepLink(
            provider = null,
            code = code,
            accessToken = accessToken,
            refreshToken = refreshToken,
            type = if (code != null) "pkce" else "implicit",
            error = error,
            errorCode = null,
            errorDescription = errorDescription
        )
    }
}
