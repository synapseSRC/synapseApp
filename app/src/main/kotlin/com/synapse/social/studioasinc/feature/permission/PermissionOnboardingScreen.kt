package com.synapse.social.studioasinc.feature.permission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.permission.components.PermissionBackdrop
import com.synapse.social.studioasinc.feature.permission.components.PermissionCard
import com.synapse.social.studioasinc.feature.permission.components.PermissionRationaleDialog
import com.synapse.social.studioasinc.feature.permission.components.PermissionRequestEffect
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * The one screen that introduces permissions after sign-in.
 *
 * It never opens a system dialog on its own: a request exists only after a card's action was tapped, and
 * every status shown here is re-read from the OS whenever the screen resumes.
 */
@Composable
fun PermissionOnboardingScreen(
    viewModel: PermissionOnboardingViewModel,
    onFinished: () -> Unit,
    onManagePermissions: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val canAsk: (String) -> Boolean = remember(activity) {
        { manifestName -> activity?.shouldShowRequestPermissionRationale(manifestName) == true }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh(canAsk)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionOnboardingEvent.EnterMainApp -> onFinished()
            }
        }
    }

    PermissionRequestEffect(
        pending = state.pending,
        onLaunched = viewModel::onRequestLaunched,
        onResult = { viewModel.onRequestResult(canAsk) }
    )

    BackHandler {
        when (state.step) {
            PermissionOnboardingStep.CARDS -> viewModel.skip(canAsk)
            PermissionOnboardingStep.SUMMARY -> viewModel.backToCards(canAsk)
        }
    }

    when (state.step) {
        PermissionOnboardingStep.CARDS -> PermissionOnboardingCards(
            state = state,
            onAction = { snapshot -> handlePermissionAction(context, snapshot, viewModel::onActionClick) },
            onContinue = { viewModel.continueToSummary(canAsk) },
            onSkip = { viewModel.skip(canAsk) }
        )

        PermissionOnboardingStep.SUMMARY -> PermissionSummaryScreen(
            state = state,
            onBack = { viewModel.backToCards(canAsk) },
            onManagePermissions = onManagePermissions,
            onContinue = viewModel::complete
        )
    }

    state.rationaleFor?.let { permission ->
        PermissionRationaleDialog(
            permission = permission,
            onConfirm = viewModel::confirmRationale,
            onDismiss = viewModel::dismissRationale
        )
    }
}

/** One step of the entrance: its own opacity and how far it still has to travel. */
private data class PermissionReveal(val alpha: Float, val offset: Dp)

/**
 * The permission step itself: a fixed header, a scrolling list of cards, and Continue anchored above the
 * navigation bar.
 *
 * The header and the primary action stay put while the cards scroll, so Continue is always one reach away
 * and a compact display or a large font scale can still scroll every card into view. The steps arrive
 * once, headline first, using the theme's own motion springs — opacity on the effects spring, movement on
 * the spatial one — and they settle immediately when the system asks for reduced motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionOnboardingCards(
    state: PermissionOnboardingUiState,
    onAction: (PermissionSnapshot) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = state.permissions.size + 1
    val stagger = PermissionMetrics.revealStagger
    val revealSpan = (1f - stagger * (steps - 1)).coerceAtLeast(0.2f)

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val revealAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "permissionRevealAlpha"
    )
    val revealPosition by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "permissionRevealPosition"
    )

    val revealFor: (Int) -> PermissionReveal = { step ->
        val alpha = ((revealAlpha - step * stagger) / revealSpan).coerceIn(0f, 1f)
        val travelled = ((revealPosition - step * stagger) / revealSpan).coerceIn(0f, 1f)
        PermissionReveal(alpha = alpha, offset = PermissionMetrics.revealOffset * (1f - travelled))
    }

    Box(Modifier.fillMaxSize()) {
        PermissionBackdrop(Modifier.matchParentSize())

        Column(Modifier.fillMaxSize()) {
            PermissionHeader(
                onSkip = onSkip,
                skipEnabled = state.pending == null,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = AppSpacing.screenPadding,
                    end = AppSpacing.screenPadding,
                    top = Spacing.space4,
                    bottom = Spacing.space8
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                item {
                    val reveal = revealFor(0)
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = reveal.alpha
                            translationY = reveal.offset.toPx()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.permission_onboarding_headline),
                            style = PermissionTypography.heroHeadline,
                            color = PermissionColors.heading
                        )
                        Spacer(Modifier.height(Spacing.space8))
                        Text(
                            text = stringResource(R.string.permission_onboarding_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PermissionColors.body
                        )
                        Spacer(Modifier.height(Spacing.space12))
                    }
                }

                itemsIndexed(
                    items = state.permissions,
                    key = { _, snapshot -> snapshot.permission.name }
                ) { index, snapshot ->
                    val reveal = revealFor(index + 1)
                    PermissionCard(
                        snapshot = snapshot,
                        isRequestPending = state.pending != null,
                        onAction = { onAction(snapshot) },
                        modifier = Modifier.graphicsLayer {
                            alpha = reveal.alpha
                            translationY = reveal.offset.toPx()
                        }
                    )
                }
            }

            PermissionContinueBar(
                onContinue = onContinue,
                enabled = state.pending == null,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
}

/**
 * Title and the way out.
 *
 * Skip keeps its position and its meaning from the original screen; it is only restyled, and it stays
 * disabled while a system dialog is on screen so an answer cannot be lost to a double tap.
 */
@Composable
private fun PermissionHeader(
    onSkip: () -> Unit,
    skipEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.permission_onboarding_title),
            style = PermissionTypography.topBarTitle,
            color = PermissionColors.heading
        )

        TextButton(
            onClick = onSkip,
            enabled = skipEnabled,
            shape = PermissionMetrics.skipShape,
            colors = ButtonDefaults.textButtonColors(
                containerColor = PermissionColors.skipContainer,
                contentColor = PermissionColors.skipContent
            ),
            contentPadding = PaddingValues(
                horizontal = Spacing.space20,
                vertical = Spacing.space8
            ),
            modifier = Modifier.height(PermissionMetrics.skipMinHeight)
        ) {
            Text(
                text = stringResource(R.string.permission_onboarding_skip),
                style = AppTypography.buttonText
            )
        }
    }
}

/**
 * The primary action, anchored above the navigation bar and clear of it: the button sits inside the
 * navigation-bar inset rather than under the gesture area, and it keeps one height and one shape
 * wherever it is drawn.
 */
@Composable
private fun PermissionContinueBar(
    onContinue: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = Spacing.space16)
    ) {
        Button(
            onClick = onContinue,
            enabled = enabled,
            shape = PermissionMetrics.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PermissionColors.action,
                contentColor = PermissionColors.onAction
            ),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(PermissionMetrics.buttonHeight)
                .pressScale(interactionSource)
        ) {
            Text(
                text = stringResource(R.string.permission_onboarding_continue),
                style = AppTypography.buttonText
            )
        }
    }
}
