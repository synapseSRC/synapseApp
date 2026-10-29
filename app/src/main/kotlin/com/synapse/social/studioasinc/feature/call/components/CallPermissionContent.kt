package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan

/**
 * A call stopped by a permission, with the one action that can actually unstop it.
 *
 * The two shapes this surface has to cover are different problems wearing the same clothes: a
 * permission that was declined once is granted by asking again, and one that was declined with "don't
 * ask again" is granted only in Settings. Offering the first for the second produces a dialog the user
 * has already closed, which is why the plan decides which button appears rather than the screen showing
 * both and hoping.
 */
@Composable
fun CallPermissionContent(
    plan: CallPermissionPlan,
    onTryAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val needsSettings = plan.needsSettings.isNotEmpty()
    val isVideo = plan.mediaType == CallMediaType.VIDEO

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.space24),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(CallDimens.EndedIconCircle)
                .clip(CircleShape)
                .background(scheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) CallIcons.videoOff else CallIcons.micOff,
                contentDescription = null,
                modifier = Modifier.size(CallDimens.EndedIcon),
                tint = scheme.onErrorContainer
            )
        }

        Spacer(Modifier.height(Spacing.space24))

        Text(
            text = stringResource(R.string.call_permission_required_title),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Spacing.space8))

        Text(
            text = stringResource(
                if (isVideo) R.string.call_permission_required_camera
                else R.string.call_permission_required_microphone
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (needsSettings) {
            Spacer(Modifier.height(Spacing.space4))
            Text(
                text = stringResource(R.string.call_permission_settings_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CallDimens.EndedActionMaxWidth)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = if (needsSettings) onOpenSettings else onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.ButtonHeightM3Medium),
                shape = CircleShape
            ) {
                Text(
                    stringResource(
                        if (needsSettings) R.string.call_permission_open_settings
                        else R.string.call_permission_try_again
                    )
                )
            }

            Spacer(Modifier.height(Spacing.space12))

            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.ButtonHeightM3Medium),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface)
            ) {
                Text(stringResource(R.string.call_action_done))
            }
        }

        Spacer(Modifier.height(Spacing.space24))
    }
}
