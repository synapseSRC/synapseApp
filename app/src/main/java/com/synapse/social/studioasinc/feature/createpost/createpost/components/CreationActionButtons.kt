package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.FeelingType
import com.synapse.social.studioasinc.domain.model.LocationData
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import com.synapse.social.studioasinc.feature.shared.theme.AccentBlue
import com.synapse.social.studioasinc.feature.shared.theme.AccentYellow
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySelectionSheet(
    currentPrivacy: String,
    onPrivacySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.Large)
                .padding(bottom = Spacing.ExtraLarge)
        ) {
            Text(
                stringResource(R.string.privacy_who_can_see),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Small)
            )
            Text(
                stringResource(R.string.privacy_post_visibility_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Large)
            )

            val options = listOf(
                Triple("Public", "Anyone on or off the app", Icons.Default.Public),
                Triple("Followers", "Your followers on the app", Icons.Default.Group),
                Triple("Private", "Only me", Icons.Default.Lock)
            )

            options.forEach { (label, desc, icon) ->
                val isSelected = currentPrivacy == label.lowercase()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Sizes.CornerDefault))
                        .clickable { onPrivacySelected(label.lowercase()) }
                        .padding(vertical = Spacing.SmallMedium, horizontal = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(Sizes.IconGiant)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                }
            }
        }
    }
}
@Composable
fun AddToPostSheet(
    onDismiss: () -> Unit,
    onMediaClick: () -> Unit,
    onPollClick: () -> Unit,
    onLocationClick: () -> Unit,
    onYoutubeClick: () -> Unit,
    onTagClick: () -> Unit,
    onFeelingClick: () -> Unit
) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.Medium)
                .padding(bottom = Spacing.ExtraLarge)
        ) {
            Text(
                stringResource(R.string.add_content),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.MediumLarge, start = Spacing.Small)
            )

            val actions = listOf(
                Triple("Photo/Video", Icons.Filled.Image, MaterialTheme.colorScheme.primary) to onMediaClick,
                Triple("Tag People", Icons.Filled.Person, MaterialTheme.colorScheme.secondary) to onTagClick,
                Triple("Feeling/Activity", Icons.Filled.Mood, MaterialTheme.colorScheme.tertiary) to onFeelingClick,
                Triple("Check In", Icons.Filled.Place, MaterialTheme.colorScheme.error) to onLocationClick,
                Triple("Poll", Icons.Default.Poll, MaterialTheme.colorScheme.tertiary) to onPollClick,
                Triple("YouTube", Icons.Default.VideoLibrary, MaterialTheme.colorScheme.secondary) to onYoutubeClick
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                userScrollEnabled = false,
                modifier = Modifier.heightIn(max = Sizes.HeightSheetContent)
            ) {
                 items(actions) { (item, action) ->
                     val (label, icon, color) = item
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                action()
                                onDismiss()
                            })
                            .padding(vertical = Spacing.SmallMedium, horizontal = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color.copy(alpha = 0.12f),
                            modifier = Modifier.size(Sizes.IconMassive)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(Sizes.IconDefault)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                 }
            }
        }
}





@Composable
fun StickyBottomActionArea(
    onMediaClick: () -> Unit,
    onTagClick: () -> Unit,
    onFeelingClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPollClick: () -> Unit,
    onYoutubeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.Small,
        tonalElevation = Sizes.BorderDefault,
        shape = RoundedCornerShape(topStart = Sizes.CornerLarge, topEnd = Sizes.CornerLarge)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onMediaClick) {
                Icon(Icons.Filled.Image, contentDescription = "Photo/Video", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onTagClick) {
                Icon(Icons.Filled.Person, contentDescription = "Tag People", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onFeelingClick) {
                Icon(Icons.Filled.Mood, contentDescription = "Feeling/Activity", tint = MaterialTheme.colorScheme.tertiary)
            }
            IconButton(onClick = onLocationClick) {
                Icon(Icons.Filled.Place, contentDescription = "Check In", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onPollClick) {
                Icon(Icons.Default.Poll, contentDescription = "Poll", tint = MaterialTheme.colorScheme.tertiary)
            }
            IconButton(onClick = onYoutubeClick) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "YouTube", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPeopleSheet(
    onDismiss: () -> Unit,
    onPersonSelected: (User) -> Unit
) {

    val mockUsers = listOf(
        User(id="1", uid="1", username="john_doe", displayName="John Doe"),
        User(id="2", uid="2", username="jane_smith", displayName="Jane Smith"),
        User(id="3", uid="3", username="alex_chen", displayName="Alex Chen"),
        User(id="4", uid="4", username="sarah_jones", displayName="Sarah Jones")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
         Column(modifier = Modifier.padding(Spacing.Medium)) {
             Text(stringResource(R.string.tag_people_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(Spacing.Medium))
             LazyColumn {
                 items(mockUsers) { user ->
                     Row(
                         modifier = Modifier
                             .fillMaxWidth()
                             .clickable { onPersonSelected(user) }
                             .padding(vertical = Spacing.SmallMedium),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Box(modifier = Modifier.size(Sizes.IconMassive).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                         Spacer(modifier = Modifier.width(Spacing.Medium))
                         Text(user.displayName ?: user.username ?: "Unknown")
                     }
                 }
             }
             Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
         }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeelingActivitySheet(
    onDismiss: () -> Unit,
    onFeelingSelected: (FeelingActivity) -> Unit
) {
    val feelings = listOf(
        FeelingActivity("😊", "Happy", FeelingType.MOOD),
        FeelingActivity("😎", "Cool", FeelingType.MOOD),
        FeelingActivity("😍", "Loved", FeelingType.MOOD),
        FeelingActivity("😢", "Sad", FeelingType.MOOD),
        FeelingActivity("🥳", "Celebrating", FeelingType.MOOD),
        FeelingActivity("😴", "Tired", FeelingType.MOOD)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
         Column(modifier = Modifier.padding(Spacing.Medium)) {
             Text(stringResource(R.string.feeling_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(Spacing.Medium))
             LazyVerticalGrid(
                 columns = GridCells.Fixed(2),
                 verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                 horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
             ) {
                 items(feelings) { feeling ->
                     Surface(
                         shape = RoundedCornerShape(Sizes.CornerMedium),
                         color = MaterialTheme.colorScheme.surfaceContainer,
                         onClick = { onFeelingSelected(feeling) }
                     ) {
                         Row(
                             modifier = Modifier.padding(Spacing.Medium),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(feeling.emoji, style = MaterialTheme.typography.headlineSmall)
                             Spacer(modifier = Modifier.width(Spacing.SmallMedium))
                             Text(feeling.text)
                         }
                     }
                 }
             }
             Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
         }
    }
}
@Composable
fun YoutubeAddDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String) -> Unit
) {
    var youtubeUrl by remember { mutableStateOf("") }
    AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text(stringResource(R.string.dialog_title_add_youtube)) },
         text = {
             OutlinedTextField(
                 value = youtubeUrl,
                 onValueChange = { youtubeUrl = it },
                 label = { Text(stringResource(R.string.label_youtube_url)) },
                 singleLine = true,
                 shape = RoundedCornerShape(Sizes.CornerDefault)
             )
         },
         confirmButton = {
             Button(onClick = { onAddUrl(youtubeUrl) }) {
                 Text(stringResource(R.string.action_add))
             }
         },
         dismissButton = {
             TextButton(onClick = onDismiss) {
                 Text(stringResource(R.string.cancel))
             }
         }
    )
}
