package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.ChatVerificationFailureReason
import com.synapse.social.studioasinc.domain.model.ChatVerificationFeedback
import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatEncryptionUiState
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatEncryptionViewModel
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.ChatEncryptionMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class EncryptionSheetPage { OVERVIEW, VERIFY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatEncryptionSheet(
    chatId: String,
    participantName: String,
    onDismiss: () -> Unit,
    viewModel: ChatEncryptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    var page by remember { mutableStateOf(EncryptionSheetPage.OVERVIEW) }
    var showPrivacyExplanation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(chatId) { viewModel.load(chatId) }
    BackHandler(enabled = page == EncryptionSheetPage.VERIFY) { page = EncryptionSheetPage.OVERVIEW }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = ChatEncryptionMetrics.sheetCornerRadius, topEnd = ChatEncryptionMetrics.sheetCornerRadius),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == EncryptionSheetPage.VERIFY) {
                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = motion.defaultSpatialSpec()) + fadeIn(animationSpec = motion.fastEffectsSpec())) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }, animationSpec = motion.defaultSpatialSpec()) + fadeOut(animationSpec = motion.fastEffectsSpec()))
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }, animationSpec = motion.defaultSpatialSpec()) + fadeIn(animationSpec = motion.fastEffectsSpec())) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }, animationSpec = motion.defaultSpatialSpec()) + fadeOut(animationSpec = motion.fastEffectsSpec()))
                }
            },
            label = "chatEncryptionPage"
        ) { visiblePage ->
            when (visiblePage) {
                EncryptionSheetPage.OVERVIEW -> EncryptionOverview(
                    state = uiState,
                    participantName = participantName,
                    onShowVerification = { if (uiState.info != null) page = EncryptionSheetPage.VERIFY },
                    onLearnMore = { showPrivacyExplanation = true }
                )
                EncryptionSheetPage.VERIFY -> VerificationContent(
                    state = uiState,
                    participantName = participantName,
                    onBack = { page = EncryptionSheetPage.OVERVIEW },
                    onShare = { uiState.info?.let { shareSafetyNumber(context, participantName, it) } },
                    onScan = {
                        GmsBarcodeScanning.getClient(context).startScan()
                            .addOnSuccessListener { barcode ->
                                val value = barcode.rawValue
                                if (value.isNullOrBlank()) viewModel.reportScannerUnavailable() else viewModel.verifyScannedCode(value)
                            }
                            .addOnFailureListener { viewModel.reportScannerUnavailable() }
                    },
                    onToggleVerified = { viewModel.setVerified(!uiState.info?.isVerified.orFalse()) }
                )
            }
        }
    }
    if (showPrivacyExplanation) {
        AlertDialog(
            onDismissRequest = { showPrivacyExplanation = false },
            icon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null) },
            title = { Text(stringResource(R.string.chat_security_learn_title)) },
            text = { Text(stringResource(R.string.chat_security_learn_body, participantName)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyExplanation = false }) { Text(stringResource(R.string.chat_security_got_it)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun EncryptionOverview(
    state: ChatEncryptionUiState,
    participantName: String,
    onShowVerification: () -> Unit,
    onLearnMore: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = ChatEncryptionMetrics.sheetMaximumHeight)
            .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        Text(stringResource(R.string.chat_security_encryption_title), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
            Icon(
                imageVector = if (state.info != null) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (state.info != null) colors.onPrimaryContainer else colors.onErrorContainer,
                modifier = Modifier.size(ChatEncryptionMetrics.iconButtonSize)
                    .background(if (state.info != null) colors.primaryContainer else colors.errorContainer, CircleShape)
                    .padding(Spacing.space8)
            )
            Text(
                stringResource(when {
                    state.info == null -> R.string.chat_security_unavailable_status
                    state.info.isVerified -> R.string.chat_security_verified_status
                    state.info.secureSessionActive -> R.string.chat_security_active_status
                    else -> R.string.chat_security_ready_status
                }),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
        }
        val active = state.info?.secureSessionActive == true
        Column(verticalArrangement = Arrangement.spacedBy(ChatEncryptionMetrics.listGap)) {
            EncryptionFeatureItem(Icons.Rounded.Lock, stringResource(R.string.chat_security_private_title), stringResource(if (active) R.string.chat_security_private_active else R.string.chat_security_private_pending, participantName), 0)
            EncryptionFeatureItem(Icons.Rounded.Chat, stringResource(R.string.chat_security_messages_title), stringResource(if (active) R.string.chat_security_messages_active else R.string.chat_security_messages_pending), 1)
            EncryptionFeatureItem(Icons.Rounded.AttachFile, stringResource(R.string.chat_security_media_title), stringResource(if (active) R.string.chat_security_media_active else R.string.chat_security_media_pending), 2)
        }
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.failure?.let { VerificationUnavailable(it, participantName) }
        val verifySource = remember { MutableInteractionSource() }
        Button(
            onClick = onShowVerification,
            enabled = state.info != null && !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(ChatEncryptionMetrics.buttonHeight).pressScale(verifySource),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            interactionSource = verifySource
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(Spacing.space8))
            Text(stringResource(R.string.chat_security_verify_button))
        }
        val learnSource = remember { MutableInteractionSource() }
        TextButton(onClick = onLearnMore, modifier = Modifier.fillMaxWidth().heightIn(min = ChatEncryptionMetrics.buttonHeight).pressScale(learnSource), interactionSource = learnSource) {
            Text(stringResource(R.string.chat_security_learn_more))
            Spacer(Modifier.width(Spacing.space8))
            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
        }
        Spacer(Modifier.height(Spacing.space8))
    }
}

@Composable
private fun EncryptionFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    supporting: String,
    position: Int
) {
    val shape = when (position) {
        0 -> RoundedCornerShape(topStart = ChatEncryptionMetrics.listOuterCornerRadius, topEnd = ChatEncryptionMetrics.listOuterCornerRadius, bottomStart = ChatEncryptionMetrics.listInnerCornerRadius, bottomEnd = ChatEncryptionMetrics.listInnerCornerRadius)
        2 -> RoundedCornerShape(topStart = ChatEncryptionMetrics.listInnerCornerRadius, topEnd = ChatEncryptionMetrics.listInnerCornerRadius, bottomStart = ChatEncryptionMetrics.listOuterCornerRadius, bottomEnd = ChatEncryptionMetrics.listOuterCornerRadius)
        else -> RoundedCornerShape(ChatEncryptionMetrics.listInnerCornerRadius)
    }
    val colors = MaterialTheme.colorScheme
    Surface(shape = shape, color = colors.secondaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = ChatEncryptionMetrics.listItemMinimumHeight)
                .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(ChatEncryptionMetrics.iconCircleSize), shape = CircleShape, color = colors.surface) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(Spacing.space24))
                }
            }
            Spacer(Modifier.width(Spacing.space12))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.onSecondaryContainer)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VerificationContent(
    state: ChatEncryptionUiState,
    participantName: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onScan: () -> Unit,
    onToggleVerified: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = ChatEncryptionMetrics.sheetMaximumHeight)
            .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(ChatEncryptionMetrics.topBarHeight), verticalAlignment = Alignment.CenterVertically) {
            val backSource = remember { MutableInteractionSource() }
            IconButton(onClick = onBack, interactionSource = backSource, modifier = Modifier.pressScale(backSource)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.chat_security_back))
            }
            Text(stringResource(R.string.chat_security_verify_title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            val shareSource = remember { MutableInteractionSource() }
            IconButton(onClick = onShare, enabled = state.info != null, interactionSource = shareSource, modifier = Modifier.pressScale(shareSource)) {
                Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.chat_security_share))
            }
        }
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.info?.let { info ->
            SafetyNumberCard(participantName)
            Column(verticalArrangement = Arrangement.spacedBy(ChatEncryptionMetrics.listGap)) {
                FingerprintRow(info.safetyNumber.take(30), stringResource(R.string.chat_security_first_fingerprint), Icons.Rounded.Fingerprint, 0)
                FingerprintRow(info.safetyNumber.drop(30), stringResource(R.string.chat_security_second_fingerprint), Icons.Rounded.VpnKey, 1)
            }
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.surfaceContainerHighest, shape = RoundedCornerShape(ChatEncryptionMetrics.cardCornerRadius)) {
                Row(modifier = Modifier.padding(Spacing.space16), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space16)) {
                    SafetyQrCode(info.localQrPayload)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                        Text(stringResource(R.string.chat_security_qr_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.chat_security_qr_body, participantName), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            VerificationFeedback(state.feedback, participantName)
            val scanSource = remember { MutableInteractionSource() }
            Button(
                onClick = onScan,
                enabled = !state.isActionInProgress,
                modifier = Modifier.fillMaxWidth().height(ChatEncryptionMetrics.buttonHeight).pressScale(scanSource),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                interactionSource = scanSource
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(Spacing.space8))
                Text(stringResource(R.string.chat_security_scan_button))
            }
            val confirmSource = remember { MutableInteractionSource() }
            TextButton(
                onClick = onToggleVerified,
                enabled = !state.isActionInProgress,
                modifier = Modifier.fillMaxWidth().heightIn(min = ChatEncryptionMetrics.buttonHeight).pressScale(confirmSource),
                interactionSource = confirmSource
            ) {
                Text(stringResource(if (info.isVerified) R.string.chat_security_clear_verified else R.string.chat_security_manual_verify))
            }
        } ?: state.failure?.let { VerificationUnavailable(it, participantName) }
        if (state.failure != null && state.info != null) VerificationUnavailable(state.failure, participantName)
        if (state.isActionInProgress) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space8))
    }
}

@Composable
private fun SafetyNumberCard(participantName: String) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth().height(ChatEncryptionMetrics.verificationCardHeight), shape = RoundedCornerShape(ChatEncryptionMetrics.cardCornerRadius), color = colors.surfaceContainerHighest) {
        Column(modifier = Modifier.padding(Spacing.space20), verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = colors.primary)
                Text(stringResource(R.string.chat_security_safety_number_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.chat_security_safety_number_body, participantName), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun FingerprintRow(code: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, position: Int) {
    val shape = if (position == 0) {
        RoundedCornerShape(topStart = ChatEncryptionMetrics.listOuterCornerRadius, topEnd = ChatEncryptionMetrics.listOuterCornerRadius, bottomStart = ChatEncryptionMetrics.listInnerCornerRadius, bottomEnd = ChatEncryptionMetrics.listInnerCornerRadius)
    } else {
        RoundedCornerShape(topStart = ChatEncryptionMetrics.listInnerCornerRadius, topEnd = ChatEncryptionMetrics.listInnerCornerRadius, bottomStart = ChatEncryptionMetrics.listOuterCornerRadius, bottomEnd = ChatEncryptionMetrics.listOuterCornerRadius)
    }
    val colors = MaterialTheme.colorScheme
    Surface(shape = shape, color = colors.surfaceContainerLow) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = ChatEncryptionMetrics.listItemMinimumHeight).padding(Spacing.space16), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(ChatEncryptionMetrics.iconCircleSize), shape = CircleShape, color = colors.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(Spacing.space24)) }
            }
            Spacer(Modifier.width(Spacing.space12))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Text(code.chunked(5).joinToString("  "), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SafetyQrCode(payload: String) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    val foreground = if (dark) colors.inverseOnSurface else colors.inverseSurface
    val background = if (dark) colors.inverseSurface else colors.inverseOnSurface
    val px = with(LocalDensity.current) { ChatEncryptionMetrics.qrSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, payload, px, foreground, background) {
        value = withContext(Dispatchers.Default) {
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, px, px, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
            val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val foregroundArgb = foreground.toArgb()
            val backgroundArgb = background.toArgb()
            for (x in 0 until px) for (y in 0 until px) bitmap.setPixel(x, y, if (matrix[x, y]) foregroundArgb else backgroundArgb)
            bitmap.asImageBitmap()
        }
    }
    if (image != null) Image(image!!, contentDescription = stringResource(R.string.chat_security_qr_description), modifier = Modifier.size(ChatEncryptionMetrics.qrSize))
    else Box(Modifier.size(ChatEncryptionMetrics.qrSize), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun VerificationFeedback(feedback: ChatVerificationFeedback, participantName: String) {
    if (feedback == ChatVerificationFeedback.NONE) return
    val colors = MaterialTheme.colorScheme
    val (resource, foreground, background) = when (feedback) {
        ChatVerificationFeedback.CODE_MATCHED -> Triple(R.string.chat_security_scan_match, colors.onPrimaryContainer, colors.primaryContainer)
        ChatVerificationFeedback.CODE_MISMATCH -> Triple(R.string.chat_security_scan_mismatch, colors.onErrorContainer, colors.errorContainer)
        ChatVerificationFeedback.MANUALLY_VERIFIED -> Triple(R.string.chat_security_manual_success, colors.onPrimaryContainer, colors.primaryContainer)
        ChatVerificationFeedback.VERIFICATION_CLEARED -> Triple(R.string.chat_security_verification_cleared, colors.onSurface, colors.surfaceContainer)
        ChatVerificationFeedback.SCANNER_UNAVAILABLE -> Triple(R.string.chat_security_scanner_unavailable, colors.onErrorContainer, colors.errorContainer)
        ChatVerificationFeedback.NONE -> return
    }
    val usesName = feedback == ChatVerificationFeedback.CODE_MATCHED || feedback == ChatVerificationFeedback.CODE_MISMATCH
    Surface(color = background, shape = RoundedCornerShape(ChatEncryptionMetrics.cardCornerRadius)) {
        Text(
            text = if (usesName) stringResource(resource, participantName) else stringResource(resource),
            modifier = Modifier.fillMaxWidth().padding(Spacing.space16),
            color = foreground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun VerificationUnavailable(reason: ChatVerificationFailureReason, participantName: String) {
    val resource = when (reason) {
        ChatVerificationFailureReason.NOT_AUTHENTICATED -> R.string.chat_security_error_auth
        ChatVerificationFailureReason.LOCAL_IDENTITY_MISSING -> R.string.chat_security_error_local_keys
        ChatVerificationFailureReason.PARTICIPANT_IDENTITY_MISSING -> R.string.chat_security_error_remote_keys
        ChatVerificationFailureReason.NOT_ONE_TO_ONE_CHAT -> R.string.chat_security_error_group
        ChatVerificationFailureReason.INVALID_IDENTITY_KEY -> R.string.chat_security_error_invalid_key
        ChatVerificationFailureReason.IDENTITY_CHANGED -> R.string.chat_security_error_changed
        ChatVerificationFailureReason.STORAGE_FAILURE -> R.string.chat_security_error_storage
        ChatVerificationFailureReason.GENERAL -> R.string.chat_security_error_general
    }
    val message = if (reason == ChatVerificationFailureReason.PARTICIPANT_IDENTITY_MISSING) stringResource(resource, participantName) else stringResource(resource)
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(ChatEncryptionMetrics.cardCornerRadius)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(Spacing.space16), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun shareSafetyNumber(context: android.content.Context, participantName: String, info: ChatVerificationInfo) {
    val code = info.safetyNumber.chunked(5).joinToString(" ")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.chat_security_share_text, participantName, code))
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chat_security_share_title)))
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, R.string.chat_security_share_error, android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun Boolean?.orFalse(): Boolean = this == true
