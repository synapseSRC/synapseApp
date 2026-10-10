package com.synapse.social.studioasinc.feature.profile.personalinfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.RelationshipStatus
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PronounsDialog(
    currentPronouns: String,
    onDismissRequest: () -> Unit,
    onPronounsSelected: (String) -> Unit
) {
    val presets = listOf("they/them", "she/her", "he/him", "xe/xem")
    var selectedPreset by remember {
        mutableStateOf(if (presets.contains(currentPronouns.lowercase())) currentPronouns.lowercase() else if (currentPronouns.isNotBlank()) "custom" else "")
    }
    var customText by remember {
        mutableStateOf(if (!presets.contains(currentPronouns.lowercase()) && currentPronouns.isNotBlank()) currentPronouns else "")
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.pronouns)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                presets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPreset = preset
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = preset, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPreset = "custom" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPreset == "custom",
                        onClick = { selectedPreset = "custom" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.custom_pronouns), style = MaterialTheme.typography.bodyLarge)
                }

                if (selectedPreset == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        placeholder = { Text("e.g. ze/zir") },
                        singleLine = true,
                        shape = SettingsShapes.inputShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = if (selectedPreset == "custom") customText.trim() else selectedPreset
                    onPronounsSelected(result)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onPronounsSelected("")
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDatePickerDialog(
    currentBirthdayString: String,
    onDismissRequest: () -> Unit,
    onBirthdaySelected: (String) -> Unit
) {
    val initialMillis = remember(currentBirthdayString) {
        if (currentBirthdayString.isNotBlank()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(currentBirthdayString)
                date?.time
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis ?: Calendar.getInstance().timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        format.timeZone = TimeZone.getTimeZone("UTC")
                        val formattedDate = format.format(Date(selectedMillis))
                        onBirthdaySelected(formattedDate)
                    }
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onBirthdaySelected("")
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun GenderDialog(
    currentGender: Gender,
    onDismissRequest: () -> Unit,
    onGenderSelected: (Gender) -> Unit
) {
    val genderOptions = listOf(
        Pair(Gender.Male, stringResource(R.string.gender_male)),
        Pair(Gender.Female, stringResource(R.string.gender_female)),
        Pair(Gender.Hidden, stringResource(R.string.gender_gone_title))
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.gender)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                genderOptions.forEach { (gender, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGenderSelected(gender)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentGender == gender,
                            onClick = {
                                onGenderSelected(gender)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RelationshipStatusDialog(
    currentStatus: RelationshipStatus?,
    onDismissRequest: () -> Unit,
    onStatusSelected: (RelationshipStatus?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.relationship)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                RelationshipStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStatusSelected(status)
                                onDismissRequest()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStatus == status,
                            onClick = {
                                onStatusSelected(status)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = status.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onStatusSelected(null)
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
