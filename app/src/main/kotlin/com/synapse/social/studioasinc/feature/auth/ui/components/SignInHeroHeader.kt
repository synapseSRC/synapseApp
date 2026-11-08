package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.SignInHeroMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Branded header for the sign-in mode.
 *
 * It reuses the welcome screen's hero artwork - same ribbon illustration, same logo plate - in a
 * much shorter band, so the two screens read as one flow while the form stays the primary focus.
 * The heading and its supporting line sit directly under the artwork, which keeps them above the
 * fold together with the first input field on a typical phone.
 *
 * Sign-up mode keeps the plain header treatment: this composable is only ever composed when the
 * screen is in sign-in mode.
 */
@Composable
fun SignInHeroHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WelcomeHero(
            heroHeight = SignInHeroMetrics.Height,
            plateSize = SignInHeroMetrics.PlateSize,
            markSize = SignInHeroMetrics.MarkSize,
            logoCentreFraction = SignInHeroMetrics.LogoCentreFraction
        )

        Spacer(modifier = Modifier.height(Spacing.space16))

        Text(
            text = stringResource(R.string.sign_in_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.space4))

        Text(
            text = stringResource(R.string.sign_in_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

