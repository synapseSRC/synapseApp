package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoStepVerificationScreen(
    viewModel: TwoStepVerificationViewModel,
    onBackClick: () -> Unit
) {
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_two_step_verification_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.itemShape,
                color = AppColors.cardBackground,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppSpacing.itemHorizontalPadding,
                            vertical = AppSpacing.itemVerticalPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIconBadge(imageVector = Icons.Filled.Security)
                    Spacer(modifier = androidx.compose.foundation.layout.Modifier.width(AppSpacing.iconTextSpacing))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_two_step_verification_title),
                            style = AppTypography.itemTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = androidx.compose.foundation.layout.Modifier.height(Spacing.space4))
                        Text(
                            text = stringResource(R.string.settings_two_step_verification_subtitle),
                            style = AppTypography.itemSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Sizes.IconSmall),
                            strokeWidth = Sizes.ProgressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.setupTwoFactor() },
                            enabled = !twoFactorEnabled,
                            shape = AppShapes.itemShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        ) {
                            Text(
                                text = if (twoFactorEnabled) {
                                    stringResource(R.string.action_enabled)
                                } else {
                                    stringResource(R.string.action_setup_two_factor)
                                },
                                style = AppTypography.buttonText,
                            )
                        }
                    }
                }
            }

            Text(
                text = if (twoFactorEnabled) {
                    stringResource(R.string.settings_two_factor_enabled_status)
                } else {
                    stringResource(R.string.settings_two_factor_setup_description)
                },
                    style = AppTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

            Spacer(modifier = Modifier.height(Sizes.HeightSmall))
        }
    }
}
