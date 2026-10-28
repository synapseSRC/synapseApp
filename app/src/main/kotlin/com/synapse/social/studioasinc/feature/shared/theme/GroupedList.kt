package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Contained-list treatment shared by the Settings, Messages and Calls lists: items stack with a
 * 28dp corner on the outer edge of the group and an 8dp corner where two items meet.
 */
enum class GroupedItemPosition {
    Single,
    Top,
    Middle,
    Bottom;

    val shape: Shape
        get() = when (this) {
            Single -> RoundedCornerShape(Sizes.CornerMassive)
            Top -> RoundedCornerShape(
                topStart = Sizes.CornerMassive,
                topEnd = Sizes.CornerMassive,
                bottomStart = Sizes.CornerMedium,
                bottomEnd = Sizes.CornerMedium
            )
            Middle -> RoundedCornerShape(Sizes.CornerMedium)
            Bottom -> RoundedCornerShape(
                topStart = Sizes.CornerMedium,
                topEnd = Sizes.CornerMedium,
                bottomStart = Sizes.CornerMassive,
                bottomEnd = Sizes.CornerMassive
            )
        }

    companion object {
        fun at(index: Int, count: Int): GroupedItemPosition = when {
            count <= 1 -> Single
            index <= 0 -> Top
            index >= count - 1 -> Bottom
            else -> Middle
        }
    }
}

/** Seam between two stacked contained-list items. */
val GroupedListSpacing = 3.dp
