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
import com.synapse.social.studioasinc.R
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
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
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

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
private fun StoryCameraPreview(
    isFrontCamera: Boolean,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(isFrontCamera) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.Large)
        ) {
            Button(onClick = onGalleryClick) {
                Text(stringResource(R.string.story_open_gallery))
            }
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
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            viewModel.setMediaFromGallery(uri, mimeType)
        }
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.Black) // Intentional: story creator requires black background
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
                StoryCameraPreview(
                    isFrontCamera = state.isFrontCamera,
                    onGalleryClick = { launchGallery() }
                )
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.camera_permission_required), color = Color.White)
                }
            }
        }
    }
    } // end Scaffold
} // end StoryCreatorScreen
