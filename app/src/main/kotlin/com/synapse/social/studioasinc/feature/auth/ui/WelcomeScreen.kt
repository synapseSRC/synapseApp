package com.synapse.social.studioasinc.feature.auth.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.onTextLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthSecondaryButton
import com.synapse.social.studioasinc.feature.auth.ui.util.WindowWidthSizeClass
import com.synapse.social.studioasinc.feature.auth.ui.util.calculateWindowSizeClass
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private val ContentMaxWidth = 420.dp
private val LogoSizeCompact = 96.dp
private val LogoSizeRegular = 112.dp

/**
 * First screen of the authentication flow.
 *
 * Layout contract:
 *  - The brand block lives in a scrollable region that takes every pixel left over by the
 *    actions, so it is optically centred on tall screens and scrolls instead of clipping on
 *    short ones (small phones, landscape, large font scales).
 *  - The actions block is pinned above the bottom inset and is never scrolled away.
 */
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCompactWidth = calculateWindowSizeClass() == WindowWidthSizeClass.Compact

    val openUrl: (String) -> Unit = remember(context) {
        { url ->
            val uri = Uri.parse(url)
            // Custom Tabs keeps the in-app feel; fall back to any browser when none is installed.
            runCatching { CustomTabsIntent.Builder().build().launchUrl(context, uri) }
                .onFailure {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.space24, vertical = Spacing.space32),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WelcomeHeader(isCompactWidth = isCompactWidth)
            }

            WelcomeActions(
                onGetStartedClick = onGetStartedClick,
                onSignInClick = onSignInClick,
                onOpenUrl = openUrl
            )
        }
    }
}

@Composable
private fun WelcomeHeader(isCompactWidth: Boolean) {
    val bodyStyle = MaterialTheme.typography.bodyLarge

    Column(
        modifier = Modifier.widthIn(max = ContentMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            // Decorative: the heading right below already announces the brand to screen readers.
            contentDescription = null,
            modifier = Modifier.size(if (isCompactWidth) LogoSizeCompact else LogoSizeRegular)
        )

        Spacer(modifier = Modifier.height(Spacing.space20))

        Text(
            text = stringResource(id = R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
        )

        Spacer(modifier = Modifier.height(Spacing.space12))

        Text(
            text = stringResource(id = R.string.welcome_subtitle),
            style = bodyStyle.copy(lineHeight = bodyStyle.fontSize * 1.5f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WelcomeActions(
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    // Guards against a double tap pushing the next destination twice while it animates in.
    var navigationStarted by remember { mutableStateOf(false) }
    val termsUrl = stringResource(id = R.string.welcome_terms_url)
    val privacyUrl = stringResource(id = R.string.welcome_privacy_url)

    Column(
        modifier = Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = Spacing.space24, vertical = Spacing.space24),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthButton(
            text = stringResource(id = R.string.welcome_get_started),
            onClick = {
                if (!navigationStarted) {
                    navigationStarted = true
                    onGetStartedClick()
                }
            },
            enabled = !navigationStarted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.space16))

        AuthSecondaryButton(
            text = stringResource(id = R.string.welcome_already_have_account),
            onClick = {
                if (!navigationStarted) {
                    navigationStarted = true
                    onSignInClick()
                }
            },
            enabled = !navigationStarted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.space24))

        LegalNotice(
            onTermsClick = { onOpenUrl(termsUrl) },
            onPrivacyClick = { onOpenUrl(privacyUrl) }
        )
    }
}

/**
 * The legal sentence with "Terms of Service" and "Privacy Policy" tappable individually.
 *
 *  - [R.string.welcome_link_terms] / [R.string.welcome_link_privacy] are the labels, so the
 *    highlighted range is always found again after a translation.
 *  - The whole sentence is a single semantics node that exposes one custom accessibility action
 *    per document, which is how a screen reader reaches them.
 */
@Composable
private fun LegalNotice(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noticeText = stringResource(id = R.string.welcome_terms_notice)
    val termsLabel = stringResource(id = R.string.welcome_link_terms)
    val privacyLabel = stringResource(id = R.string.welcome_link_privacy)
    val bodyStyle = MaterialTheme.typography.bodySmall
    val linkColor = MaterialTheme.colorScheme.primary

    val termsStart = remember(noticeText, termsLabel) { noticeText.indexOf(termsLabel) }
    val privacyStart = remember(noticeText, privacyLabel) { noticeText.indexOf(privacyLabel) }
    val termsEnd = if (termsStart >= 0) termsStart + termsLabel.length else -1
    val privacyEnd = if (privacyStart >= 0) privacyStart + privacyLabel.length else -1

    val annotatedNotice = remember(noticeText, termsStart, termsEnd, privacyStart, privacyEnd, linkColor) {
        buildAnnotatedString {
            append(noticeText)
            val linkStyle = SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
            if (termsStart >= 0) addStyle(linkStyle, termsStart, termsEnd)
            if (privacyStart >= 0) addStyle(linkStyle, privacyStart, privacyEnd)
        }
    }

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedNotice,
        style = bodyStyle.copy(lineHeight = bodyStyle.fontSize * 1.5f),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .onTextLayout { textLayout = it }
            .pointerInput(termsStart, termsEnd, privacyStart, privacyEnd) {
                detectTapGestures { position ->
                    val layout = textLayout ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    when {
                        termsStart >= 0 && offset in termsStart until termsEnd -> onTermsClick()
                        privacyStart >= 0 && offset in privacyStart until privacyEnd -> onPrivacyClick()
                    }
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(label = termsLabel) {
                        onTermsClick()
                        true
                    },
                    CustomAccessibilityAction(label = privacyLabel) {
                        onPrivacyClick()
                        true
                    }
                )
            }
    )
}
