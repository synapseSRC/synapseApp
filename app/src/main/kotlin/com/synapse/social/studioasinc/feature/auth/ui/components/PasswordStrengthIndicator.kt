package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.DarkSuccess
import com.synapse.social.studioasinc.feature.shared.theme.DarkWarning
import com.synapse.social.studioasinc.feature.shared.theme.LightSuccess
import com.synapse.social.studioasinc.feature.shared.theme.LightWarning
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val isDark = isSystemInDarkTheme()

    val targetColor = when (strength) {
        PasswordStrength.Weak -> MaterialTheme.colorScheme.error
        PasswordStrength.Fair -> if (isDark) DarkWarning else LightWarning
        PasswordStrength.Strong -> if (isDark) DarkSuccess else LightSuccess
    }

    val animatedProgress by animateFloatAsState(
        targetValue = strength.progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300),
        label = "Progress Animation"
    )

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300),
        label = "Color Animation"
    )

    val strengthIcon = when (strength) {
        PasswordStrength.Weak -> Icons.Default.Warning
        PasswordStrength.Fair -> Icons.Default.Info
        PasswordStrength.Strong -> Icons.Default.CheckCircle
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = strengthIcon,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier.size(Sizes.IconSmall)
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = stringResource(id = R.string.password_strength),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = stringResource(id = strength.labelResId),
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.ExtraSmall),
            color = animatedColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

val PasswordStrength.labelResId: Int
    get() = when(this) {
        PasswordStrength.Weak -> R.string.password_strength_weak
        PasswordStrength.Fair -> R.string.password_strength_fair
        PasswordStrength.Strong -> R.string.password_strength_strong
    }

val PasswordStrength.progress: Float
    get() = when(this) {
        PasswordStrength.Weak -> 0.33f
        PasswordStrength.Fair -> 0.66f
        PasswordStrength.Strong -> 1.0f
    }
