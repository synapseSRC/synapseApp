package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.core.util.SynapseLegalUrls
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.WelcomeHero
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.SynapseBrand
import com.synapse.social.studioasinc.feature.shared.theme.WelcomeHeroMetrics

/**
 * Welcome screen shown as the entry point of the authentication flow.
 *
 * Three regions, one above the other: the hero artwork with the Synapse mark, the introduction
 * copy, and the single bottom action with the legal notice. The hero and the copy share the
 * flexible space above the action, which is anchored to the bottom insets and never squeezed; that
 * flexible region scrolls as a fallback when the copy no longer fits (compact displays, larger font
 * scales). Nothing is positioned in screen coordinates.
 */
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = SynapseBrand.palette
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val openUrl: (String) -> Unit = remember(context) {
        { url -> IntentUtils.openUrl(context, url) }
    }
    val legalNotice = rememberLegalNotice(
        linkColor = palette.accent,
        onOpenUrl = openUrl
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.pageTop, palette.pageMid, palette.pageBottom)
                )
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val availableWidth = maxWidth
            val contentWidth = availableWidth.coerceAtMost(WelcomeHeroMetrics.ContentMaxWidth)
            val heroHeight = (maxHeight * WelcomeHeroMetrics.HeroHeightFraction)
                .coerceAtMost(availableWidth)
                .coerceIn(WelcomeHeroMetrics.HeroMinHeight, WelcomeHeroMetrics.HeroMaxHeight)
            val headingStyle = if (availableWidth < WelcomeHeroMetrics.CompactWidth) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineLarge
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Regions 1 and 2 share the flexible space above the action area. They scroll as a
                // fallback when the copy needs more room than the display has (compact screens,
                // large font scales) while the action stays anchored to the bottom.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Region 1 - brand artwork and mark.
                    WelcomeHero(heroHeight = heroHeight)

                    // Region 2 - introduction copy.
                    Column(
                        modifier = Modifier
                            .width(contentWidth)
                            .padding(horizontal = Spacing.space24),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.welcome_title),
                            style = headingStyle.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Spacing.space12))

                        Text(
                            text = stringResource(id = R.string.welcome_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = Spacing.space8)
                        )
                    }
                }

                // Region 3 - single primary action plus the legal notice.
                Column(
                    modifier = Modifier
                        .width(contentWidth)
                        .padding(
                            start = Spacing.space24,
                            end = Spacing.space24,
                            top = Spacing.space32,
                            bottom = Spacing.space32
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val getStartedText = stringResource(id = R.string.welcome_get_started)
                    AuthButton(
                        text = getStartedText,
                        onClick = onGetStartedClick,
                        modifier = Modifier.semantics {
                            contentDescription = getStartedText
                        },
                        containerColor = palette.accent,
                        contentColor = palette.onAccent,
                        shape = MaterialTheme.shapes.medium,
                        height = Sizes.ButtonHeightM3Medium,
                        textStyle = MaterialTheme.typography.titleMedium,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = Sizes.ElevationLow,
                            pressedElevation = Sizes.ElevationNone,
                            hoveredElevation = Sizes.ElevationMedium,
                            disabledElevation = Sizes.ElevationNone
                        )
                    )

                    Spacer(modifier = Modifier.height(Spacing.space20))

                    Text(
                        text = legalNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Builds the legal notice from the existing localized sentence and turns the two policy labels
 * inside it into individual links.
 *
 * The notice keeps its exact wording and translation: the link ranges are located by matching the
 * already-localized "Terms of Service" / "Privacy Policy" labels inside it, so a locale that has no
 * matching label simply renders plain text instead of breaking the sentence apart.
 */
@Composable
private fun rememberLegalNotice(
    linkColor: Color,
    onOpenUrl: (String) -> Unit
): AnnotatedString {
    val notice = stringResource(id = R.string.welcome_terms_notice)
    val termsLabel = stringResource(id = R.string.about_terms)
    val privacyLabel = stringResource(id = R.string.about_privacy_policy)

    return remember(notice, termsLabel, privacyLabel, linkColor, onOpenUrl) {
        val linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        )

        // Located in the notice so the sentence keeps its exact wording and translation; a locale
        // without a matching label simply renders the plain sentence.
        val links = listOfNotNull(
            linkSpan(notice, termsLabel)?.let {
                LinkSpan(it, TermsLinkTag, SynapseLegalUrls.termsOfService)
            },
            linkSpan(notice, privacyLabel)?.let {
                LinkSpan(it, PrivacyLinkTag, SynapseLegalUrls.privacyPolicy)
            }
        ).sortedBy { it.range.first }

        buildAnnotatedString {
            var cursor = 0
            links.forEach { span ->
                append(notice.substring(cursor, span.range.first))
                pushLink(
                    LinkAnnotation.Clickable(
                        tag = span.tag,
                        styles = linkStyles,
                        linkInteractionListener = { onOpenUrl(span.url) }
                    )
                )
                append(notice.substring(span.range.first, span.range.last + 1))
                pop()
                cursor = span.range.last + 1
            }
            append(notice.substring(cursor))
        }
    }
}

private data class LinkSpan(val range: IntRange, val tag: String, val url: String)

private fun linkSpan(notice: String, label: String): IntRange? {
    if (label.isBlank()) return null
    val start = notice.indexOf(label)
    return if (start < 0) null else start..(start + label.length - 1)
}

private const val TermsLinkTag = "welcome-terms-of-service"
private const val PrivacyLinkTag = "welcome-privacy-policy"
