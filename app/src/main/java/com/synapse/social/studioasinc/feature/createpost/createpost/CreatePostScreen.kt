package com.synapse.social.studioasinc.ui.createpost

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageOptions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
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


    var tagSearchQuery by remember { mutableStateOf("") }
    var locationSearchQuery by remember { mutableStateOf("") }
    var feelingSearchQuery by remember { mutableStateOf("") }


    var showYoutubeDialog by remember { mutableStateOf(false) }


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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                UserHeader(
                    user = uiState.currentUserProfile,
                    privacy = uiState.privacy,
                    onPrivacyClick = { showPrivacySheet = true },
                    taggedPeople = uiState.taggedPeople,
                    feeling = uiState.feeling,
                    location = uiState.location
                )
            }


            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (uiState.postText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.hint_whats_on_your_mind),
                                style = if (uiState.mediaItems.isEmpty()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }


                        val textSize = if (uiState.postText.length < 80 && uiState.mediaItems.isEmpty() && uiState.pollData == null) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.bodyLarge
                        }

                        BasicTextField(
                            value = uiState.postText,
                            onValueChange = { viewModel.updateText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = textSize.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = textSize.lineHeight * 1.2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }



            item {

                 if (uiState.mediaItems.isNotEmpty()) {
                     MediaPreviewGrid(
                         mediaItems = uiState.mediaItems,
                         onRemove = { viewModel.removeMedia(it) },
                         onEdit = { index ->
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
                         }
                     )
                 }


                 uiState.pollData?.let { poll ->
                     PollPreviewCard(poll = poll, onDelete = { viewModel.setPoll(null) })
                 }


                 uiState.youtubeUrl?.let { url ->
                      YoutubePreviewCard(url = url, onDelete = { viewModel.setYoutubeUrl(null) })
                 }


                 uiState.location?.let { loc ->
                     LocationPreviewCard(location = loc, onDelete = { viewModel.setLocation(null) })
                 }
            }



            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }


    if (showPrivacySheet) {
        PrivacySelectionSheet(
            currentPrivacy = uiState.privacy,
            onPrivacySelected = {
                viewModel.setPrivacy(it)
                showPrivacySheet = false
            },
            onDismiss = { showPrivacySheet = false }
        )
    }

    if (showPollSheet) {
        PollCreationSheet(
            onDismiss = { showPollSheet = false },
            onCreatePoll = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.warn_remove_media_for_poll), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setPoll(it)
                    showPollSheet = false
                }
            }
        )
    }

    if (showAddToPostSheet) {
        AddToPostSheet(
            onDismiss = { showAddToPostSheet = false },
            onMediaClick = { launchMediaPicker() },
            onPollClick = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.warn_remove_media_for_poll), Toast.LENGTH_SHORT).show()
                } else {
                    showPollSheet = true
                }
            },
            onYoutubeClick = {
                showYoutubeDialog = true
            },
            onLocationClick = { showLocationScreen = true },
            onTagClick = { showTagScreen = true },
            onFeelingClick = { showFeelingScreen = true }
        )
    }


    if (showTagScreen) {
        val onTagScreenClose = {
            showTagScreen = false
            viewModel.clearSearchResults()
            tagSearchQuery = ""
        }
        TagPeopleScreen(
            onDismiss = onTagScreenClose,
            onDone = onTagScreenClose,
            searchQuery = tagSearchQuery,
            onSearchQueryChange = {
                tagSearchQuery = it
                viewModel.searchUsers(it)
            },
            searchResults = uiState.userSearchResults,
            selectedUsers = uiState.taggedPeople,
            onToggleUser = { viewModel.toggleTaggedPerson(it) },
            isLoading = uiState.isSearchLoading
        )
    }

    if (showLocationScreen) {
        LocationSelectScreen(
            onDismiss = {
                showLocationScreen = false
                viewModel.clearSearchResults()
                locationSearchQuery = ""
            },
            searchQuery = locationSearchQuery,
            onSearchQueryChange = {
                locationSearchQuery = it
                viewModel.searchLocations(it)
            },
            searchResults = uiState.locationSearchResults,
            onLocationSelected = { viewModel.setLocation(it) },
            isLoading = uiState.isSearchLoading
        )
    }

    if (showFeelingScreen) {
        FeelingSelectScreen(
            onDismiss = {
                showFeelingScreen = false
                viewModel.clearSearchResults()
                feelingSearchQuery = ""
            },
            searchQuery = feelingSearchQuery,
            onSearchQueryChange = {
                feelingSearchQuery = it
                viewModel.searchFeelings(it)
            },
            feelings = uiState.feelingSearchResults,
            onFeelingSelected = { viewModel.setFeelingActivity(it) }
        )
    }


    if (showYoutubeDialog) {
        YoutubeAddDialog(
            onDismiss = { showYoutubeDialog = false },
            onAddUrl = { url ->
                if (url.contains("youtube") || url.contains("youtu.be")) {
                    viewModel.setYoutubeUrl(url)
                    showYoutubeDialog = false
                } else {
                    Toast.makeText(context, context.getString(R.string.error_invalid_youtube_url), Toast.LENGTH_SHORT).show()
                }
            }
        )
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
