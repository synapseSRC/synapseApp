package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.domain.model.PasswordStrength
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.AccentOrange
import com.synapse.social.studioasinc.feature.shared.theme.LightError
import com.synapse.social.studioasinc.feature.shared.theme.InteractionRepostActive

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val reducedMotion = AnimationUtil.rememberReducedMotion()

    val animatedProgress by animateFloatAsState(
        targetValue = strength.progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300),
        label = "Progress Animation"
    )

    val animatedColor by animateColorAsState(
        targetValue = strength.color,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300),
        label = "Color Animation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

// Extension properties for mapping Shared Domain Model to UI
val PasswordStrength.color: Color
    get() = when(this) {
        PasswordStrength.Weak -> LightError
        PasswordStrength.Fair -> AccentOrange
        PasswordStrength.Strong -> InteractionRepostActive
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
