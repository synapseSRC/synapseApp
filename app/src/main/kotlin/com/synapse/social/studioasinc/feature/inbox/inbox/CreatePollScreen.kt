@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CreatePollUiState
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Full-screen poll composer reached from the chat attachment sheet.
 *
 * The [chatId] is captured by [CreatePollViewModel] from the type-safe navigation arguments; the
 * parameter is kept on the signature so callers wire the route explicitly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    onPollSent: () -> Unit,
    viewModel: CreatePollViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val motion = MaterialTheme.motionScheme

    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess) onPollSent()
    }

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_poll_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                )
            )
        },
        bottomBar = {
            Surface(color = AppColors.screenBackground) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding, vertical = Spacing.space12)
                ) {
                    Button(
                        onClick = viewModel::sendPoll,
                        enabled = state.canSend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.ButtonHeightM3Medium)
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Sizes.IconSmallMedium),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = Sizes.BorderThin,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = AppColors.disabledContentAlpha)
                            )
                            Spacer(modifier = Modifier.width(Spacing.space8))
                        }
                        Text(
                            text = stringResource(
                                if (state.isSending) R.string.create_poll_sending else R.string.create_poll_send
                            )
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenPadding, vertical = Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            QuestionField(
                question = state.question,
                isError = state.errorRes != null && state.question.isBlank(),
                onQuestionChange = viewModel::updateQuestion
            )

            OptionsSection(
                options = state.options,
                onOptionChange = viewModel::updateOption,
                onRemoveOption = viewModel::removeOption
            )

            AnimatedVisibility(
                visible = state.options.size < CreatePollUiState.MAX_OPTIONS,
                enter = expandVertically(animationSpec = motion.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motion.defaultEffectsSpec()),
                exit = shrinkVertically(animationSpec = motion.defaultSpatialSpec()) +
                    fadeOut(animationSpec = motion.defaultEffectsSpec())
            ) {
                OutlinedButton(
                    onClick = viewModel::addOption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.ButtonHeightM3Medium),
                    shape = AppShapes.inputShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconSmallMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Text(text = stringResource(R.string.create_poll_add_option))
                }
            }

            AllowMultipleAnswersRow(
                checked = state.allowMultipleAnswers,
                onCheckedChange = { viewModel.toggleAllowMultiple() }
            )

            AnimatedVisibility(
                visible = state.errorRes != null,
                enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
                exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
            ) {
                state.errorRes?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = AppTypography.itemSubtitle,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionField(
    question: String,
    isError: Boolean,
    onQuestionChange: (String) -> Unit
) {
    OutlinedTextField(
        value = question,
        onValueChange = onQuestionChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.create_poll_question_label)) },
        supportingText = {
            Text(
                stringResource(
                    R.string.create_poll_counter,
                    question.length,
                    CreatePollUiState.MAX_QUESTION_LENGTH
                )
            )
        },
        isError = isError,
        singleLine = false,
        shape = AppShapes.inputShape,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text
        )
    )
}

@Composable
private fun OptionsSection(
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onRemoveOption: (Int) -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val trimmed = options.map { it.trim() }
    // Case-insensitive duplicate counts, mirroring the use case uniqueness rule.
    val duplicateCounts = trimmed
        .filter { it.isNotEmpty() }
        .groupingBy { it.lowercase() }
        .eachCount()
    val canRemove = options.size > CreatePollUiState.MIN_OPTIONS

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.space8),
        modifier = Modifier.animateContentSize(animationSpec = motion.defaultSpatialSpec())
    ) {
        options.forEachIndexed { index, option ->
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(animationSpec = motion.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motion.defaultEffectsSpec()),
                exit = shrinkVertically(animationSpec = motion.defaultSpatialSpec()) +
                    fadeOut(animationSpec = motion.defaultEffectsSpec())
            ) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { onOptionChange(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_poll_option_label, index + 1)) },
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.create_poll_counter,
                                option.length,
                                CreatePollUiState.MAX_OPTION_LENGTH
                            )
                        )
                    },
                    isError = trimmed[index].isNotEmpty() &&
                        (duplicateCounts[trimmed[index].lowercase()] ?: 0) > 1,
                    singleLine = true,
                    shape = AppShapes.inputShape,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    trailingIcon = {
                        if (canRemove) {
                            IconButton(onClick = { onRemoveOption(index) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(
                                        R.string.create_poll_remove_option,
                                        index + 1
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AllowMultipleAnswersRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = GroupedItemPosition.Single.shape,
        color = AppColors.cardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.rowHorizontalPadding, vertical = AppSpacing.rowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.create_poll_allow_multiple),
                    style = AppTypography.itemTitle,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                    color = AppColors.itemContent
                )
                Text(
                    text = stringResource(R.string.create_poll_allow_multiple_supporting),
                    style = AppTypography.itemSubtitle,
                    color = AppColors.supportingText
                )
            }
            Spacer(modifier = Modifier.width(Spacing.space12))
            Box(contentAlignment = Alignment.Center) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}
