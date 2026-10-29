package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition

enum class SettingsItemPosition {
    Single,

    Top,

    Middle,

    Bottom;

    @Composable
    fun getShape(): Shape = when (this) {
        Single -> GroupedItemPosition.Single.shape
        Top -> GroupedItemPosition.Top.shape
        Middle -> GroupedItemPosition.Middle.shape
        Bottom -> GroupedItemPosition.Bottom.shape
    }
}
