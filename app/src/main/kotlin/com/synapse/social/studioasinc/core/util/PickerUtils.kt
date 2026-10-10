package com.synapse.social.studioasinc.core.util

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

object PickerUtils {

    fun pickSingleFile(launcher: ActivityResultLauncher<String>, mimeType: String) {
        launcher.launch(mimeType)
    }

    fun pickMultipleFiles(launcher: ActivityResultLauncher<String>, mimeType: String) {
        launcher.launch(mimeType)
    }

    fun pickDirectory(launcher: ActivityResultLauncher<Uri?>) {
        launcher.launch(null)
    }

    fun createFile(launcher: ActivityResultLauncher<String>, fileName: String) {
        launcher.launch(fileName)
    }
}
