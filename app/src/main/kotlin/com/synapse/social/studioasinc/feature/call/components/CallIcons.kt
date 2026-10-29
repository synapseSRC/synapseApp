package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PhoneMissed
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.ui.graphics.vector.ImageVector

/** Single place the call surface picks its glyphs from, so a state always maps to one icon. */
internal object CallIcons {
    val endCall: ImageVector = Icons.Rounded.CallEnd
    val acceptCall: ImageVector = Icons.Rounded.Call
    val micOn: ImageVector = Icons.Rounded.Mic
    val micOff: ImageVector = Icons.Rounded.MicOff
    val speakerOn: ImageVector = Icons.AutoMirrored.Rounded.VolumeUp
    val speakerOff: ImageVector = Icons.AutoMirrored.Rounded.VolumeDown
    val earpiece: ImageVector = Icons.Rounded.PhoneInTalk
    val bluetooth: ImageVector = Icons.Rounded.BluetoothAudio
    val wiredHeadset: ImageVector = Icons.Rounded.Headset
    val videoOn: ImageVector = Icons.Rounded.Videocam
    val videoOff: ImageVector = Icons.Rounded.VideocamOff
    val flipCamera: ImageVector = Icons.Rounded.Cameraswitch
    val qualityGood: ImageVector = Icons.Rounded.NetworkCheck
    val qualityWeak: ImageVector = Icons.Rounded.SignalCellularAlt
    val qualityPoor: ImageVector = Icons.Rounded.SignalCellularConnectedNoInternet4Bar
    val reconnect: ImageVector = Icons.Rounded.SyncProblem
    val reconnectNow: ImageVector = Icons.Rounded.Autorenew
    val missed: ImageVector = Icons.AutoMirrored.Rounded.PhoneMissed
    val unavailable: ImageVector = Icons.Rounded.PhoneDisabled
}
