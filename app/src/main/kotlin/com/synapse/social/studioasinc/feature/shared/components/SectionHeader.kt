package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/** Section label above a group of contained-list rows, used by settings, profile and notifications. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = AppTypography.sectionHeader,
        color = AppColors.sectionTitle,
        modifier = modifier.padding(
            start = Spacing.space8,
            end = AppSpacing.itemHorizontalPadding,
            top = Spacing.space8,
            bottom = Spacing.space4
        )
    )
}
