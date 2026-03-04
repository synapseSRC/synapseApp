package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current


    var editingMediaIndex by remember { mutableStateOf<Int?>(null) }
    val cropImage = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
             editingMediaIndex?.let { index ->
                 result.uriContent?.let { uri ->
                     viewModel.updateMediaItem(index, uri)
                 }
             }
        } else {
             val exception = result.error
             Toast.makeText(context, context.getString(R.string.error_crop_failed, exception?.message ?: ""), Toast.LENGTH_SHORT).show()
        }
        editingMediaIndex = null
    }


    var showPrivacySheet by remember { mutableStateOf(false) }
    var showPollSheet by remember { mutableStateOf(false) }
    var showAddToPostSheet by remember { mutableStateOf(false) }
    var showTagScreen by remember { mutableStateOf(false) }
    var showLocationScreen by remember { mutableStateOf(false) }
    var showFeelingScreen by remember { mutableStateOf(false) }
    var showYoutubeDialog by remember { mutableStateOf(false) }


    var tagSearchQuery by remember { mutableStateOf("") }
    var locationSearchQuery by remember { mutableStateOf("") }
    var feelingSearchQuery by remember { mutableStateOf("") }


    LaunchedEffect(true) {
        viewModel.loadDraft()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveDraft()
        }
    }

    LaunchedEffect(uiState.isPostCreated) {
        if (uiState.isPostCreated) {
            val message = if (uiState.isEditMode) context.getString(R.string.post_updated_toast) else context.getString(R.string.post_created_toast)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onNavigateUp()
        }
    }



    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.addMedia(uris)
    }

    fun launchMediaPicker() {
        if (uiState.pollData != null) {
            Toast.makeText(context, context.getString(R.string.warn_remove_poll_for_media), Toast.LENGTH_SHORT).show()
        } else {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    Scaffold(
        topBar = {
            CreatePostTopBar(
                isEditMode = uiState.isEditMode,
                isLoading = uiState.isLoading,
                postText = uiState.postText,
                mediaItemsCount = uiState.mediaItems.size,
                hasPoll = uiState.pollData != null,
                onNavigateUp = onNavigateUp,
                onSubmitPost = { viewModel.submitPost() }
            )
        },
        bottomBar = {
            StickyBottomActionArea(
                onMediaClick = { launchMediaPicker() },
                onMoreClick = { showAddToPostSheet = true },
                modifier = Modifier.imePadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        CreatePostContent(
            uiState = uiState,
            focusRequester = focusRequester,
            focusManager = focusManager,
            padding = padding,
            onPrivacyClick = { showPrivacySheet = true },
            onUpdateText = { viewModel.updateText(it) },
            onRemoveMedia = { viewModel.removeMedia(it) },
            onEditMedia = { index ->
                val item = uiState.mediaItems[index]
                editingMediaIndex = index
                val uri = if (item.url.startsWith("content://") || item.url.startsWith("file://") || item.url.startsWith("http")) {
                    Uri.parse(item.url)
                } else {
                    Uri.fromFile(java.io.File(item.url))
                }
                cropImage.launch(
                    CropImageContractOptions(
                        uri = uri,
                        cropImageOptions = CropImageOptions().apply {
                            guidelines = CropImageView.Guidelines.ON
                            activityTitle = context.getString(R.string.title_edit_image)
                            cropMenuCropButtonTitle = context.getString(R.string.action_save)
                            showCropOverlay = true
                            showProgressBar = true
                        }
                    )
                )
            },
            onRemovePoll = { viewModel.setPoll(null) },
            onRemoveYoutube = { viewModel.setYoutubeUrl(null) },
            onRemoveLocation = { viewModel.setLocation(null) }
        )
    }

    CreatePostSheets(
        uiState = uiState,
        showPrivacySheet = showPrivacySheet,
        onPrivacySheetDismiss = { showPrivacySheet = false },
        onPrivacySelected = {
            viewModel.setPrivacy(it)
            showPrivacySheet = false
        },
        showPollSheet = showPollSheet,
        onPollSheetDismiss = { showPollSheet = false },
        onCreatePoll = {
            viewModel.setPoll(it)
            showPollSheet = false
        },
        showAddToPostSheet = showAddToPostSheet,
        onAddToPostSheetDismiss = { showAddToPostSheet = false },
        onMediaClick = { launchMediaPicker() },
        onPollClick = { showPollSheet = true },
        onYoutubeClick = { showYoutubeDialog = true },
        onLocationClick = { showLocationScreen = true },
        onTagClick = { showTagScreen = true },
        onFeelingClick = { showFeelingScreen = true },
        showTagScreen = showTagScreen,
        onTagScreenDismiss = {
            showTagScreen = false
            viewModel.clearSearchResults()
            tagSearchQuery = ""
        },
        tagSearchQuery = tagSearchQuery,
        onTagSearchQueryChange = {
            tagSearchQuery = it
            viewModel.searchUsers(it)
        },
        onToggleUser = { viewModel.toggleTaggedPerson(it) },
        showLocationScreen = showLocationScreen,
        onLocationScreenDismiss = {
            showLocationScreen = false
            viewModel.clearSearchResults()
            locationSearchQuery = ""
        },
        locationSearchQuery = locationSearchQuery,
        onLocationSearchQueryChange = {
            locationSearchQuery = it
            viewModel.searchLocations(it)
        },
        onLocationSelected = { viewModel.setLocation(it) },
        showFeelingScreen = showFeelingScreen,
        onFeelingScreenDismiss = {
            showFeelingScreen = false
            viewModel.clearSearchResults()
            feelingSearchQuery = ""
        },
        feelingSearchQuery = feelingSearchQuery,
        onFeelingSearchQueryChange = {
            feelingSearchQuery = it
            viewModel.searchFeelings(it)
        },
        onFeelingSelected = { viewModel.setFeelingActivity(it) },
        showYoutubeDialog = showYoutubeDialog,
        onYoutubeDialogDismiss = { showYoutubeDialog = false },
        onYoutubeUrlAdd = {
            viewModel.setYoutubeUrl(it)
            showYoutubeDialog = false
        }
    )
}
