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
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
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
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User



private val storyGradientColors = listOf(
    Color(0xFFE040FB),
    Color(0xFFFF4081),
    Color(0xFFFF6E40),
    Color(0xFFFFAB00)
)



private val seenStoryRingColor = Color(0xFF424242)



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
    if (isLoading) {
        StoryTrayShimmer(modifier)
        return
    }


    val hasActiveStory = myStory != null && myStory.stories.isNotEmpty()





    val totalItems = 1 + (if (hasActiveStory) 1 else 0) + friendStories.size
    val state = rememberCarouselState { totalItems }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalMultiBrowseCarousel(
            state = state,
            preferredItemWidth = 120.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(180.dp)
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


        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
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
            .fillMaxSize()
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
                            0.4f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
            )


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (hasUnseen) {
                                    Modifier.border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(storyGradientColors),
                                        shape = CircleShape
                                    )
                                } else if (!isAddButton) {

                                     Modifier.border(
                                        width = 1.dp,
                                        color = seenStoryRingColor,
                                        shape = CircleShape
                                     )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(2.dp)
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
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }


                Text(
                    text = if (isAddButton) "Create Story" else title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
private fun StoryTrayShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(ShapeDefaults.Large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}
