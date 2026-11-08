package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

/**
 * One social provider entry point.
 *
 * The provider identifiers and the callbacks they raise are untouched; the presentation now comes
 * from [AuthOutlinedButton], so Google, Apple, GitHub and the passkey entry point share one height,
 * corner radius, border, icon lane and press animation. The labels are resolved from resources
 * instead of being hardcoded here.
 */
@Composable
fun OAuthButton(
    provider: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRes = when (provider.lowercase()) {
        "google" -> R.drawable.ic_google_logo
        "apple" -> R.drawable.ic_apple_logo
        "facebook" -> R.drawable.ic_facebook_logo
        "github" -> R.drawable.ic_github_logo
        else -> R.drawable.ic_google_logo
    }

    val label = when (provider.lowercase()) {
        "google" -> stringResource(R.string.continue_with_google)
        "apple" -> stringResource(R.string.continue_with_apple)
        "facebook" -> stringResource(R.string.continue_with_facebook)
        "github" -> stringResource(R.string.continue_with_github)
        else -> stringResource(R.string.continue_with_provider, provider)
    }

    AuthOutlinedButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconLarge)
            )
        }
    )
}
