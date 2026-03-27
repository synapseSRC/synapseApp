package com.synapse.social.studioasinc.feature.stories.tray

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.components.shimmer
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.shared.theme.ProfileColors
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTray(
    currentUser: User?,
    myStory: StoryWithUser?,
    friendStories: List<StoryWithUser>,
    onMyStoryClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryWithUser) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val hasActiveStory = myStory != null && myStory.stories.isNotEmpty()

    val totalItems = if (isLoading) {
        6 // 1 Create Story + 5 shimmers
    } else {
        1 + (if (hasActiveStory) 1 else 0) + friendStories.size
    }

    Column(modifier = modifier.fillMaxWidth()) {
        key(totalItems) {
            val state = rememberCarouselState { totalItems }
            HorizontalUncontainedCarousel(
                state = state,
                itemWidth = Sizes.WidthMassive,
                itemSpacing = Spacing.Small,
                contentPadding = PaddingValues(horizontal = Spacing.Medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.Small)
                    .height(Sizes.HeightStoryTray)
            ) { index ->
                if (index == 0) {
                    StoryCard(
                        imageUrl = currentUser?.avatar,
                        avatarUrl = currentUser?.avatar,
                        title = "My Story",
                        isAddButton = true,
                        hasUnseen = false,
                        onClick = onAddStoryClick,
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                    )
                } else if (isLoading) {
                    StoryCardShimmer()
                } else if (hasActiveStory && index == 1) {
                    val latestStory = myStory?.stories?.lastOrNull()
                    StoryCard(
                        imageUrl = latestStory?.mediaUrl,
                        avatarUrl = currentUser?.avatar,
                        title = "My Story",
                        isAddButton = false,
                        hasUnseen = false,
                        onClick = onMyStoryClick,
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                    )
                } else {
                    val friendIndex = index - (if (hasActiveStory) 2 else 1)
                    if (friendIndex in friendStories.indices) {
                        val friendStory = friendStories[friendIndex]
                        val latestStory = friendStory.stories.lastOrNull()

                        StoryCard(
                            imageUrl = latestStory?.mediaUrl ?: friendStory.user.avatar,
                            avatarUrl = friendStory.user.avatar,
                            title = friendStory.user.displayName ?: friendStory.user.username ?: "User",
                            isAddButton = false,
                            hasUnseen = friendStory.hasUnseenStories,
                            onClick = { onStoryClick(friendStory) },
                            modifier = Modifier.clip(MaterialTheme.shapes.large)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = Spacing.Small),
            thickness = Sizes.BorderHairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun StoryCard(
    imageUrl: String?,
    avatarUrl: String?,
    title: String,
    isAddButton: Boolean,
    hasUnseen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = ShapeDefaults.Large,
        modifier = modifier
            .width(Sizes.WidthMassive)
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.4f to androidx.compose.ui.graphics.Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
                        )
                    )
            )


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.Small)
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(Sizes.IconHuge)
                ) {
                    val storyGradientColors = listOf(
                        ProfileColors.storyRingStart,
                        ProfileColors.storyRingMiddle,
                        ProfileColors.storyRingEnd
                    )
                    val seenStoryColor = MaterialTheme.colorScheme.outline
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (hasUnseen) {
                                    Modifier.border(
                                        width = Spacing.Tiny,
                                        brush = Brush.sweepGradient(storyGradientColors),
                                        shape = CircleShape
                                    )
                                } else if (!isAddButton) {

                                     Modifier.border(
                                        width = Sizes.BorderThin,
                                        color = seenStoryColor,
                                        shape = CircleShape
                                     )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(Spacing.Tiny)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                         if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {

                             Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                             ) {
                                Text(text = title.take(1), style = MaterialTheme.typography.labelSmall)
                             }
                        }
                    }
                }


                if (isAddButton) {
                     Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(Sizes.IconHuge)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(Sizes.BorderDefault, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Sizes.IconDefault)
                        )
                    }
                }


                Text(
                    text = if (isAddButton) stringResource(R.string.story_create) else title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun StoryCardShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(Sizes.WidthMassive)
            .fillMaxHeight()
            .clip(ShapeDefaults.Large)
            .shimmer()
    )
}
