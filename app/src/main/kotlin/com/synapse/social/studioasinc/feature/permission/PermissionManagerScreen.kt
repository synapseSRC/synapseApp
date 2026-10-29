package com.synapse.social.studioasinc.feature.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.permission.components.PermissionCard
import com.synapse.social.studioasinc.feature.permission.components.PermissionRationaleDialog
import com.synapse.social.studioasinc.feature.permission.components.PermissionRequestEffect
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

/**
 * The permanent view of every permission the app uses, reachable from Settings and from the onboarding
 * summary. States are read live on entry and on every resume, so a change made in system Settings shows
 * up as soon as the user comes back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagerScreen(
    viewModel: PermissionManagerViewModel,
    onBackClick: () -> Unit
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

    PermissionRequestEffect(
        pending = state.pending,
        onLaunched = viewModel::onRequestLaunched,
        onResult = { viewModel.onRequestResult(canAsk) }
    )

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_permissions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
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
                Text(
                    text = stringResource(R.string.permission_manager_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(state.permissions, key = { it.permission.name }) { snapshot ->
                PermissionCard(
                    snapshot = snapshot,
                    isRequestPending = state.pending != null,
                    onAction = {
                        handlePermissionAction(context, snapshot, viewModel::onActionClick)
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.permission_manager_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.supportingText
                )
            }
        }
    }

    state.rationaleFor?.let { permission ->
        PermissionRationaleDialog(
            permission = permission,
            onConfirm = viewModel::confirmRationale,
            onDismiss = viewModel::dismissRationale
        )
    }
}
