package com.synapse.social.studioasinc.feature.permission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.feature.permission.components.PermissionCard
import com.synapse.social.studioasinc.feature.permission.components.PermissionRationaleDialog
import com.synapse.social.studioasinc.feature.permission.components.PermissionRequestEffect
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

/**
 * The one screen that introduces permissions after sign-in.
 *
 * It never opens a system dialog on its own: a request exists only after a card's action was tapped, and
 * every status shown here is re-read from the OS whenever the screen resumes.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionOnboardingCards(
    state: PermissionOnboardingUiState,
    onAction: (PermissionSnapshot) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permission_onboarding_title)) },
                actions = {
                    TextButton(onClick = onSkip, enabled = state.pending == null) {
                        Text(stringResource(R.string.permission_onboarding_skip))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = Spacing.space8,
                bottom = Spacing.space24
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.permission_onboarding_headline),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(Spacing.space8))
                    Text(
                        text = stringResource(R.string.permission_onboarding_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.permissions, key = { it.permission.name }) { snapshot ->
                PermissionCard(
                    snapshot = snapshot,
                    isRequestPending = state.pending != null,
                    onAction = { onAction(snapshot) }
                )
            }

            item {
                Spacer(Modifier.height(Spacing.space4))
                Button(
                    onClick = onContinue,
                    enabled = state.pending == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.ButtonHeightLarge)
                ) {
                    Text(
                        text = stringResource(R.string.permission_onboarding_continue),
                        style = AppTypography.buttonText
                    )
                }
            }
        }
    }
}
