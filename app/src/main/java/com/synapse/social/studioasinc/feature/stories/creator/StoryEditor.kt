package com.synapse.social.studioasinc.feature.stories.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StoryColorOrange
import com.synapse.social.studioasinc.feature.shared.theme.StoryColorGreen
import com.synapse.social.studioasinc.feature.shared.theme.StoryColorPurple

@Composable
internal fun StoryEditor(
    state: StoryCreatorState,
    viewModel: StoryCreatorViewModel,
    onClose: () -> Unit,
    onStoryPosted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawingMode by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showPrivacyMenu by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf(emptyList<Offset>()) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    val userProfile by viewModel.userProfile.collectAsState()

    val drawingColors = remember {
        listOf(Color.White, Color.Black, Color.Red, StoryColorOrange, Color.Yellow, StoryColorGreen, Color.Cyan, StoryColorPurple)
    }
    val stickerEmojis = remember {
        listOf("😊", "😂", "❤️", "🔥", "✨", "👍", "🎉", "😍", "🙌", "💯", "😎", "🥳")
    }

    val cachedPaths = remember(state.drawings) {
        state.drawings.map { drawing ->
            val path = Path().apply {
                if (drawing.points.isNotEmpty()) {
                    moveTo(drawing.points.first().x, drawing.points.first().y)
                    for (i in 1 until drawing.points.size) {
                        lineTo(drawing.points[i].x, drawing.points[i].y)
                    }
                }
            }
            Pair(path, drawing)
        }
    }

    val activePath = remember(currentPath) {
        Path().apply {
            if (currentPath.isNotEmpty()) {
                moveTo(currentPath.first().x, currentPath.first().y)
                for (i in 1 until currentPath.size) {
                    lineTo(currentPath[i].x, currentPath[i].y)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        StoryBackground(state)

        StoryDrawingCanvas(
            isDrawingMode = isDrawingMode,
            currentPath = currentPath,
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            cachedPaths = cachedPaths,
            activePath = activePath,
            onPathChange = { currentPath = it },
            onDrawingComplete = { path -> viewModel.addDrawing(path) }
        )

        StoryTextOverlays(
            textOverlays = state.textOverlays,
            onPositionChange = { index, position -> viewModel.updateTextPosition(index, position) },
            onContentChange = { index, content -> viewModel.updateTextContent(index, content) }
        )

        StoryStickerOverlays(
            stickers = state.stickers,
            onPositionChange = { index, position -> viewModel.updateStickerPosition(index, position) }
        )

        StoryTopControls(
            isDrawingMode = isDrawingMode,
            userProfile = userProfile,
            onClose = {
                viewModel.clearCapturedMedia()
                onClose()
            },
            onToggleDrawingMode = { isDrawingMode = !isDrawingMode },
            onAddText = { viewModel.addTextOverlay() },
            onAddSticker = { showStickerPicker = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Drawing toolbar
        if (isDrawingMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                drawingColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(Sizes.IconLarge)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color == currentColor) Sizes.BorderSelected else Sizes.BorderThin,
                                color = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape
                            )
                            .clickable { currentColor = color }
                    )
                }
                Spacer(Modifier.height(Spacing.Small))
                // Undo button
                IconButton(
                    onClick = { viewModel.undoDrawing() },
                    modifier = Modifier
                        .size(Sizes.IconLarge)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Sticker picker
        if (showStickerPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showStickerPicker = false },
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Sizes.CornerLarge))
                        .padding(Spacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    items(stickerEmojis) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 36.sp,
                            modifier = Modifier.clickable {
                                viewModel.addSticker(emoji)
                                showStickerPicker = false
                            }
                        )
                    }
                }
            }
        }

        // Privacy menu
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.Medium)) {
            TextButton(onClick = { showPrivacyMenu = true }) {
                Text(
                    text = state.selectedPrivacy.name.replace('_', ' '),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            DropdownMenu(expanded = showPrivacyMenu, onDismissRequest = { showPrivacyMenu = false }) {
                StoryPrivacy.entries.forEach { privacy ->
                    DropdownMenuItem(
                        text = { Text(privacy.name.replace('_', ' ')) },
                        onClick = {
                            viewModel.setPrivacy(privacy)
                            showPrivacyMenu = false
                        }
                    )
                }
            }
        }

        StoryBottomControls(
            isPosting = state.isPosting,
            privacyName = state.selectedPrivacy.name,
            onPost = { viewModel.postStory() },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    LaunchedEffect(state.isPosted) {
        if (state.isPosted) {
            onStoryPosted()
        }
    }
}