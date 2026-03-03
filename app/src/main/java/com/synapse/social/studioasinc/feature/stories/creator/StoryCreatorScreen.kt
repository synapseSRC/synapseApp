package com.synapse.social.studioasinc.feature.stories.creator

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import java.util.concurrent.Executors

const val EXTRA_SHARED_POST_ID = "shared_post_id"

@AndroidEntryPoint
class StoryCreatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPostId = intent.getStringExtra(EXTRA_SHARED_POST_ID)
        setContent {
            SynapseTheme {
                StoryCreatorScreen(
                    onClose = { finish() },
                    onStoryPosted = { finish() },
                    sharedPostId = sharedPostId
                )
            }
        }
    }
}

@Composable
private fun StoryMediaContent(state: StoryCreatorState) {
    val mediaUri = state.capturedMediaUri
    if (mediaUri != null) {
        AsyncImage(
            model = mediaUri,
            contentDescription = "Story Media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else if (state.sharedPost != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Shared Post Editor", color = Color.White)
        }
    }
}

@Composable
private fun StoryDrawingCanvas(
    state: StoryCreatorState,
    viewModel: StoryCreatorViewModel,
    isDrawingMode: Boolean,
    currentPath: List<Offset>,
    currentColor: Color,
    currentStrokeWidth: Float,
    onPathUpdate: (List<Offset>) -> Unit
) {
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

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isDrawingMode) {
                if (isDrawingMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onPathUpdate(listOf(offset))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onPathUpdate(currentPath + change.position)
                        },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                viewModel.addDrawing(
                                    DrawingPath(
                                        points = currentPath,
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth
                                    )
                                )
                                onPathUpdate(emptyList())
                            }
                        }
                    )
                }
            }
    ) {
        cachedPaths.forEach { (path, drawing) ->
            if (drawing.points.size > 1) {
                drawPath(
                    path = path,
                    color = drawing.color,
                    style = Stroke(
                        width = drawing.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        if (currentPath.size > 1) {
            drawPath(
                path = activePath,
                color = currentColor,
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryTextOverlays(state: StoryCreatorState, viewModel: StoryCreatorViewModel) {
    state.textOverlays.forEachIndexed { index, overlay ->
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(overlay.position.x.roundToInt(), overlay.position.y.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.updateTextPosition(
                            index,
                            Offset(
                                overlay.position.x + dragAmount.x,
                                overlay.position.y + dragAmount.y
                            )
                        )
                    }
                }
        ) {
            TextField(
                value = overlay.text,
                onValueChange = { viewModel.updateTextContent(index, it) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = overlay.color,
                    unfocusedTextColor = overlay.color,
                    cursorColor = overlay.color,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = (24 * overlay.scale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun StoryStickerOverlays(state: StoryCreatorState, viewModel: StoryCreatorViewModel) {
    state.stickers.forEachIndexed { index, sticker ->
        Text(
            text = sticker.emoji,
            fontSize = (48 * sticker.scale).sp,
            modifier = Modifier
                .offset {
                    IntOffset(sticker.position.x.roundToInt(), sticker.position.y.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.updateStickerPosition(
                            index,
                            Offset(
                                sticker.position.x + dragAmount.x,
                                sticker.position.y + dragAmount.y
                            )
                        )
                    }
                }
        )
    }
}

@Composable
private fun BoxScope.StoryTopActionBar(
    viewModel: StoryCreatorViewModel,
    isDrawingMode: Boolean,
    onDrawingModeChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            viewModel.clearCapturedMedia()
            onClose()
        }) {
            Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.White)
        }
        Row {
            IconButton(onClick = { onDrawingModeChange(!isDrawingMode) }) {
                Icon(Icons.Default.Edit, contentDescription = "Draw", tint = if (isDrawingMode) Color.Blue else Color.White)
            }
            IconButton(onClick = { viewModel.addTextOverlay() }) {
                Text("Aa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            IconButton(onClick = { viewModel.addSticker("\uD83D\uDE0A") }) { // Smiley emoji
                Icon(Icons.Default.Face, contentDescription = "Sticker", tint = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.StoryBottomPostButton(state: StoryCreatorState, viewModel: StoryCreatorViewModel) {
    Button(
        onClick = { viewModel.postStory() },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        if (state.isPosting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Text("Post to ${state.selectedPrivacy.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorScreen(
    onClose: () -> Unit,
    onStoryPosted: () -> Unit,
    sharedPostId: String? = null,
    viewModel: StoryCreatorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sharedPostId) {
        if (sharedPostId != null) {
            viewModel.loadSharedPost(sharedPostId)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setMediaFromGallery(it) }
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.isPosted) {
        if (state.isPosted) {
            onStoryPosted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.capturedMediaUri != null || state.sharedPost != null -> {
                StoryEditor(
                    state = state,
                    viewModel = viewModel,
                    onClose = onClose,
                    onStoryPosted = onStoryPosted
                )
            }
            hasCameraPermission -> {


                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { launchGallery() }) {
                        Text("Open Gallery")
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required")
                }
            }
        }
    }
}

@Composable
fun StoryEditor(
    state: StoryCreatorState,
    viewModel: StoryCreatorViewModel,
    onClose: () -> Unit,
    onStoryPosted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawingMode by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf(emptyList<Offset>()) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }

    Box(modifier = modifier.fillMaxSize()) {
        StoryMediaContent(state)

        StoryDrawingCanvas(
            state = state,
            viewModel = viewModel,
            isDrawingMode = isDrawingMode,
            currentPath = currentPath,
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            onPathUpdate = { currentPath = it }
        )

        StoryTextOverlays(state, viewModel)
        StoryStickerOverlays(state, viewModel)

        StoryTopActionBar(
            viewModel = viewModel,
            isDrawingMode = isDrawingMode,
            onDrawingModeChange = { isDrawingMode = it },
            onClose = onClose
        )

        StoryBottomPostButton(state, viewModel)
    }

    LaunchedEffect(state.isPosted) {
        if (state.isPosted) {
            onStoryPosted()
        }
    }
}
