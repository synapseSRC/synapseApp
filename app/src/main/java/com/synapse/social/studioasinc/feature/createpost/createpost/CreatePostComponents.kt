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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

@Composable
fun UserHeader(
    user: User?,
    privacy: String,
    onPrivacyClick: () -> Unit,
    taggedPeople: List<User> = emptyList(),
    feeling: FeelingActivity? = null,
    location: LocationData? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {

        if (user?.avatar != null) {
            AsyncImage(
                model = user.avatar,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person),
                error = painterResource(R.drawable.ic_person)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))


        Column {

            val annotatedText = buildAnnotatedString {

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(user?.displayName ?: user?.username ?: "You")
                }


                if (feeling != null) {
                    append(" is ")
                    append(feeling.emoji)
                    append(" feeling ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(feeling.text)
                    }
                }


                if (taggedPeople.isNotEmpty()) {
                    if (feeling == null) {
                        append(" \u2014 with ")
                    } else {
                        append(" with ")
                    }

                    if (taggedPeople.size == 1) {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                         }
                    } else if (taggedPeople.size == 2) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[1].displayName ?: taggedPeople[1].username)
                        }
                    } else {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append("${taggedPeople.size - 1} others")
                        }
                    }
                }


                if (location != null) {
                    if (feeling == null && taggedPeople.isEmpty()) {
                        append(" is at ")
                    } else {
                        append(" at ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(location.name)
                    }
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))


            Surface(
                onClick = onPrivacyClick,
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.height(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = when(privacy) {
                            "followers" -> Icons.Default.Group
                            "private" -> Icons.Default.Lock
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = privacy.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Who can see your post?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Your post will appear in Feed, on your profile and in search results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPrivacySelected(label.lowercase()) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreationSheet(
    onDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var duration by remember { mutableIntStateOf(24) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create Poll", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Ask a question...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            options.forEachIndexed { index, option ->
                OutlinedTextField(
                    value = option,
                    onValueChange = { options[index] = it },
                    label = { Text("Option ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = if (options.size > 2) {
                        {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove option")
                            }
                        }
                    } else null
                )
            }

            if (options.size < 4) {
                TextButton(
                    onClick = { options.add("") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Option")
                }
            }

            Button(
                onClick = {
                    val validOptions = options.filter { it.isNotBlank() }
                    if (question.isNotBlank() && validOptions.size >= 2) {
                        onCreatePoll(PollData(question, validOptions, duration))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Poll to Post", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
fun MediaPreviewGrid(
    mediaItems: List<MediaItem>,
    onRemove: (Int) -> Unit,
    onEdit: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        mediaItems.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                MediaItemView(item, onDelete = { onRemove(index) }, onEdit = { onEdit(index) })
            }
        }
    }
}

@Composable
fun MediaItemView(
    item: MediaItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, end = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = "Attached Media",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }


            if (item.type == MediaType.IMAGE) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }
            }
        }


        Surface(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Add content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp, start = 8.dp)
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
                modifier = Modifier.heightIn(max = 500.dp)
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
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
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
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
         Column(modifier = Modifier.padding(16.dp)) {
             Text("Tag People", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(16.dp))
             LazyColumn {
                 items(mockUsers) { user ->
                     Row(
                         modifier = Modifier
                             .fillMaxWidth()
                             .clickable { onPersonSelected(user) }
                             .padding(vertical = 12.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape))
                         Spacer(modifier = Modifier.width(16.dp))
                         Text(user.displayName ?: user.username ?: "Unknown")
                     }
                 }
             }
             Spacer(modifier = Modifier.height(32.dp))
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
         Column(modifier = Modifier.padding(16.dp)) {
             Text("How are you feeling?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(16.dp))
             LazyVerticalGrid(
                 columns = GridCells.Fixed(2),
                 verticalArrangement = Arrangement.spacedBy(8.dp),
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 items(feelings) { feeling ->
                     Surface(
                         shape = RoundedCornerShape(8.dp),
                         color = MaterialTheme.colorScheme.surfaceContainer,
                         onClick = { onFeelingSelected(feeling) }
                     ) {
                         Row(
                             modifier = Modifier.padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(feeling.emoji, style = MaterialTheme.typography.headlineSmall)
                             Spacer(modifier = Modifier.width(12.dp))
                             Text(feeling.text)
                         }
                     }
                 }
             }
             Spacer(modifier = Modifier.height(32.dp))
         }
    }
}

@Composable
fun PollPreviewCard(poll: PollData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            poll.options.forEach { option ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun YoutubePreviewCard(url: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.label_youtube_video), style = MaterialTheme.typography.labelMedium)
                Text(text = url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove))
            }
        }
    }
}

@Composable
fun LocationPreviewCard(location: LocationData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                location.address?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove))
            }
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
                 shape = RoundedCornerShape(12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostTopBar(
    isEditMode: Boolean,
    isLoading: Boolean,
    postText: String,
    mediaItemsCount: Int,
    hasPoll: Boolean,
    onNavigateUp: () -> Unit,
    onSubmitPost: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isEditMode) stringResource(R.string.title_edit_post) else stringResource(R.string.title_create_post),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }
        },
        actions = {
            val isEnabled = !isLoading && (
                postText.isNotBlank() ||
                mediaItemsCount > 0 ||
                hasPoll
            )
            val buttonText = if (isLoading) stringResource(R.string.button_posting) else stringResource(R.string.post)
            ExpressiveButton(
                onClick = onSubmitPost,
                enabled = isEnabled,
                text = buttonText,
                variant = ButtonVariant.Filled,
                modifier = Modifier.height(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
