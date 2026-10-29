package com.synapse.social.studioasinc.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AccessibilitySizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

private data class NotificationSettingsPickerConfig(
    val title: String,
    val selectedOption: String,
    val options: List<String>,
    val onSelect: (String) -> Unit
)

/** Renders notification categories with UI-local placeholder values and basic choice dialogs. */
@Composable
internal fun GroupedNotificationSettingsSections(
    notificationPreferences: NotificationPreferences,
    onToggleGlobal: (Boolean) -> Unit
) {
    val defaultValue = stringResource(R.string.settings_notification_value_default)
    val chimeValue = stringResource(R.string.settings_notification_value_chime)
    val softToneValue = stringResource(R.string.settings_notification_value_soft_tone)
    val silentValue = stringResource(R.string.settings_notification_value_silent)
    val shortVibrationValue = stringResource(R.string.settings_notification_value_short)
    val longVibrationValue = stringResource(R.string.settings_notification_value_long)
    val offValue = stringResource(R.string.settings_off)
    val whiteLightValue = stringResource(R.string.settings_notification_value_white)
    val redLightValue = stringResource(R.string.settings_notification_value_red)
    val greenLightValue = stringResource(R.string.settings_notification_value_green)
    val blueLightValue = stringResource(R.string.settings_notification_value_blue)
    val noLightValue = stringResource(R.string.settings_notification_value_none)
    val classicRingtoneValue = stringResource(R.string.settings_notification_value_classic)
    val firstToneValue = stringResource(R.string.settings_notification_value_tone_one)
    val popupAlwaysValue = stringResource(R.string.settings_notification_popup_always)
    val popupNeverValue = stringResource(R.string.settings_notification_popup_never)
    val enabledValue = stringResource(R.string.settings_enabled)
    val disabledValue = stringResource(R.string.settings_disabled)

    val soundOptions = listOf(defaultValue, chimeValue, softToneValue, silentValue)
    val vibrationOptions = listOf(defaultValue, shortVibrationValue, longVibrationValue, offValue)
    val lightOptions = listOf(whiteLightValue, redLightValue, greenLightValue, blueLightValue, noLightValue)
    val ringtoneOptions = listOf(defaultValue, classicRingtoneValue, firstToneValue, silentValue)
    val popupOptions = listOf(defaultValue, popupAlwaysValue, popupNeverValue)
    val badgeOptions = listOf(enabledValue, disabledValue)

    var conversationTonesEnabled by rememberSaveable { mutableStateOf(true) }
    var remindersEnabled by rememberSaveable(notificationPreferences.remindersEnabled) {
        mutableStateOf(notificationPreferences.remindersEnabled)
    }
    var messagesTone by rememberSaveable { mutableStateOf(defaultValue) }
    var messagesVibration by rememberSaveable { mutableStateOf(defaultValue) }
    var messagesPopup by rememberSaveable { mutableStateOf(defaultValue) }
    var messagesLight by rememberSaveable { mutableStateOf(whiteLightValue) }
    var messagesHighPriority by rememberSaveable(notificationPreferences.highPriorityEnabled) {
        mutableStateOf(notificationPreferences.highPriorityEnabled)
    }
    var messagesReactions by rememberSaveable(notificationPreferences.reactionNotificationsEnabled) {
        mutableStateOf(notificationPreferences.reactionNotificationsEnabled)
    }
    var groupsTone by rememberSaveable { mutableStateOf(defaultValue) }
    var groupsVibration by rememberSaveable { mutableStateOf(defaultValue) }
    var groupsLight by rememberSaveable { mutableStateOf(whiteLightValue) }
    var groupsHighPriority by rememberSaveable(notificationPreferences.highPriorityEnabled) {
        mutableStateOf(notificationPreferences.highPriorityEnabled)
    }
    var groupsReactions by rememberSaveable(notificationPreferences.reactionNotificationsEnabled) {
        mutableStateOf(notificationPreferences.reactionNotificationsEnabled)
    }
    var callsRingtone by rememberSaveable { mutableStateOf(defaultValue) }
    var callsVibration by rememberSaveable { mutableStateOf(defaultValue) }
    var statusTone by rememberSaveable { mutableStateOf(defaultValue) }
    var statusVibration by rememberSaveable { mutableStateOf(defaultValue) }
    var statusHighPriority by rememberSaveable(notificationPreferences.highPriorityEnabled) {
        mutableStateOf(notificationPreferences.highPriorityEnabled)
    }
    var statusReactions by rememberSaveable(notificationPreferences.reactionNotificationsEnabled) {
        mutableStateOf(notificationPreferences.reactionNotificationsEnabled)
    }
    var appIconBadge by rememberSaveable { mutableStateOf(enabledValue) }
    var activePickerKey by remember { mutableStateOf<String?>(null) }

    val popupNotificationsSupported = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
    ) {
        GeneralNotificationSettingsSection(
            notificationPreferences = notificationPreferences,
            conversationTonesEnabled = conversationTonesEnabled,
            onConversationTonesChange = { conversationTonesEnabled = it },
            remindersEnabled = remindersEnabled,
            onRemindersChange = { remindersEnabled = it },
            onToggle = onToggleGlobal
        )
        MessagesNotificationSection(
            tone = messagesTone,
            vibration = messagesVibration,
            popup = messagesPopup,
            popupSupported = popupNotificationsSupported,
            light = messagesLight,
            highPriority = messagesHighPriority,
            reactions = messagesReactions,
            onToneClick = { activePickerKey = "messages-tone" },
            onVibrationClick = { activePickerKey = "messages-vibration" },
            onPopupClick = { activePickerKey = "messages-popup" },
            onLightClick = { activePickerKey = "messages-light" },
            onHighPriorityChange = { messagesHighPriority = it },
            onReactionsChange = { messagesReactions = it }
        )
        GroupsNotificationSection(
            tone = groupsTone,
            vibration = groupsVibration,
            light = groupsLight,
            highPriority = groupsHighPriority,
            reactions = groupsReactions,
            onToneClick = { activePickerKey = "groups-tone" },
            onVibrationClick = { activePickerKey = "groups-vibration" },
            onLightClick = { activePickerKey = "groups-light" },
            onHighPriorityChange = { groupsHighPriority = it },
            onReactionsChange = { groupsReactions = it }
        )
        CallsNotificationSection(
            ringtone = callsRingtone,
            vibration = callsVibration,
            onRingtoneClick = { activePickerKey = "calls-ringtone" },
            onVibrationClick = { activePickerKey = "calls-vibration" }
        )
        StatusNotificationSection(
            tone = statusTone,
            vibration = statusVibration,
            highPriority = statusHighPriority,
            reactions = statusReactions,
            appIconBadge = appIconBadge,
            onToneClick = { activePickerKey = "status-tone" },
            onVibrationClick = { activePickerKey = "status-vibration" },
            onHighPriorityChange = { statusHighPriority = it },
            onReactionsChange = { statusReactions = it },
            onAppIconBadgeClick = { activePickerKey = "app-icon-badge" }
        )
    }

    val pickerConfig = when (activePickerKey) {
        "messages-tone" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_notification_tone_title), messagesTone, soundOptions
        ) { messagesTone = it }
        "messages-vibration" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_vibrate_title), messagesVibration, vibrationOptions
        ) { messagesVibration = it }
        "messages-popup" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_popup_notification_title), messagesPopup, popupOptions
        ) { messagesPopup = it }
        "messages-light" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_light_title), messagesLight, lightOptions
        ) { messagesLight = it }
        "groups-tone" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_notification_tone_title), groupsTone, soundOptions
        ) { groupsTone = it }
        "groups-vibration" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_vibrate_title), groupsVibration, vibrationOptions
        ) { groupsVibration = it }
        "groups-light" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_light_title), groupsLight, lightOptions
        ) { groupsLight = it }
        "calls-ringtone" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_ringtone_title), callsRingtone, ringtoneOptions
        ) { callsRingtone = it }
        "calls-vibration" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_vibrate_title), callsVibration, vibrationOptions
        ) { callsVibration = it }
        "status-tone" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_notification_tone_title), statusTone, soundOptions
        ) { statusTone = it }
        "status-vibration" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_vibrate_title), statusVibration, vibrationOptions
        ) { statusVibration = it }
        "app-icon-badge" -> NotificationSettingsPickerConfig(
            stringResource(R.string.settings_app_icon_badge_title), appIconBadge, badgeOptions
        ) { appIconBadge = it }
        else -> null
    }
    pickerConfig?.let { picker ->
        NotificationChoiceDialog(
            title = picker.title,
            selectedOption = picker.selectedOption,
            options = picker.options,
            onOptionSelected = { option ->
                picker.onSelect(option)
                activePickerKey = null
            },
            onDismiss = { activePickerKey = null }
        )
    }
}

/** The switches [com.synapse.social.studioasinc.shared.domain.usecase.sound.PlaySoundEffectUseCase] reads; the tone pickers around them are UI-local placeholders. */
@Composable
internal fun SoundEffectsSection(
    soundPreferences: SoundPreferences,
    onToggleSoundEffects: (Boolean) -> Unit,
    onToggleMessageTones: (Boolean) -> Unit,
    onToggleCallTones: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_sound_effects_section_title)) {
        SettingsToggleItem(
            imageVector = Icons.Default.VolumeUp,
            title = stringResource(R.string.settings_sound_effects_title),
            subtitle = if (soundPreferences.masterEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
            checked = soundPreferences.masterEnabled,
            onCheckedChange = onToggleSoundEffects,
            position = SettingsItemPosition.Top
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_sound_message_tones_title),
            subtitle = stringResource(R.string.settings_sound_message_tones_subtitle),
            checked = soundPreferences.messageTonesEnabled,
            onCheckedChange = onToggleMessageTones,
            enabled = soundPreferences.masterEnabled,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_sound_call_tones_title),
            subtitle = stringResource(R.string.settings_sound_call_tones_subtitle),
            checked = soundPreferences.callTonesEnabled,
            onCheckedChange = onToggleCallTones,
            enabled = soundPreferences.masterEnabled,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun GeneralNotificationSettingsSection(
    notificationPreferences: NotificationPreferences,
    conversationTonesEnabled: Boolean,
    onConversationTonesChange: (Boolean) -> Unit,
    remindersEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_general_section_title)) {
        SettingsToggleItem(
            imageVector = Icons.Default.Notifications,
            title = stringResource(R.string.settings_enable_notifications_title),
            subtitle = if (notificationPreferences.globalEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
            checked = notificationPreferences.globalEnabled,
            onCheckedChange = onToggle,
            position = SettingsItemPosition.Top
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_conversation_tones_title),
            subtitle = if (conversationTonesEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
            checked = conversationTonesEnabled,
            onCheckedChange = onConversationTonesChange,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_reminders_title),
            subtitle = if (remindersEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
            checked = remindersEnabled,
            onCheckedChange = onRemindersChange,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun MessagesNotificationSection(
    tone: String,
    vibration: String,
    popup: String,
    popupSupported: Boolean,
    light: String,
    highPriority: Boolean,
    reactions: Boolean,
    onToneClick: () -> Unit,
    onVibrationClick: () -> Unit,
    onPopupClick: () -> Unit,
    onLightClick: () -> Unit,
    onHighPriorityChange: (Boolean) -> Unit,
    onReactionsChange: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_messages_section_title)) {
        SettingsClickableItem(
            title = stringResource(R.string.settings_notification_tone_title),
            subtitle = tone,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onToneClick,
            position = SettingsItemPosition.Top
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_vibrate_title),
            subtitle = vibration,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onVibrationClick,
            position = SettingsItemPosition.Middle
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_popup_notification_title),
            subtitle = if (popupSupported) popup else stringResource(R.string.settings_notification_not_available),
            trailingIcon = if (popupSupported) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
            onClick = onPopupClick,
            enabled = popupSupported,
            position = SettingsItemPosition.Middle
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_light_title),
            subtitle = light,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onLightClick,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_high_priority_notifications_title),
            subtitle = stringResource(R.string.settings_high_priority_messages_subtitle),
            checked = highPriority,
            onCheckedChange = onHighPriorityChange,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_reaction_notifications_title),
            subtitle = stringResource(R.string.settings_message_reactions_subtitle),
            checked = reactions,
            onCheckedChange = onReactionsChange,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun GroupsNotificationSection(
    tone: String,
    vibration: String,
    light: String,
    highPriority: Boolean,
    reactions: Boolean,
    onToneClick: () -> Unit,
    onVibrationClick: () -> Unit,
    onLightClick: () -> Unit,
    onHighPriorityChange: (Boolean) -> Unit,
    onReactionsChange: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_groups_section_title)) {
        SettingsClickableItem(
            title = stringResource(R.string.settings_notification_tone_title),
            subtitle = tone,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onToneClick,
            position = SettingsItemPosition.Top
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_vibrate_title),
            subtitle = vibration,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onVibrationClick,
            position = SettingsItemPosition.Middle
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_light_title),
            subtitle = light,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onLightClick,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_high_priority_notifications_title),
            checked = highPriority,
            onCheckedChange = onHighPriorityChange,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_reaction_notifications_title),
            checked = reactions,
            onCheckedChange = onReactionsChange,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun CallsNotificationSection(
    ringtone: String,
    vibration: String,
    onRingtoneClick: () -> Unit,
    onVibrationClick: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_calls_section_title)) {
        SettingsClickableItem(
            title = stringResource(R.string.settings_ringtone_title),
            subtitle = ringtone,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onRingtoneClick,
            position = SettingsItemPosition.Top
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_vibrate_title),
            subtitle = vibration,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onVibrationClick,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun StatusNotificationSection(
    tone: String,
    vibration: String,
    highPriority: Boolean,
    reactions: Boolean,
    appIconBadge: String,
    onToneClick: () -> Unit,
    onVibrationClick: () -> Unit,
    onHighPriorityChange: (Boolean) -> Unit,
    onReactionsChange: (Boolean) -> Unit,
    onAppIconBadgeClick: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_status_section_title)) {
        SettingsClickableItem(
            title = stringResource(R.string.settings_notification_tone_title),
            subtitle = tone,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onToneClick,
            position = SettingsItemPosition.Top
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_vibrate_title),
            subtitle = vibration,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onVibrationClick,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_high_priority_notifications_title),
            checked = highPriority,
            onCheckedChange = onHighPriorityChange,
            position = SettingsItemPosition.Middle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_status_reactions_title),
            subtitle = stringResource(R.string.settings_status_reactions_subtitle),
            checked = reactions,
            onCheckedChange = onReactionsChange,
            position = SettingsItemPosition.Middle
        )
        SettingsClickableItem(
            title = stringResource(R.string.settings_app_icon_badge_title),
            subtitle = appIconBadge,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onAppIconBadgeClick,
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
private fun NotificationChoiceDialog(
    title: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AccessibilitySizes.minimumTouchTarget)
                            .selectable(
                                selected = option == selectedOption,
                                onClick = { onOptionSelected(option) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = Spacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

