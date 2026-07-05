package com.synapse.social.studioasinc.feature.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

class CrashActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashLog = intent.getStringExtra(EXTRA_CRASH_LOG) ?: "No crash log available."
        setContent {
            MaterialTheme {
                CrashDialog(
                    crashLog = crashLog,
                    onDismiss = { finishAffinity() }
                )
            }
        }
    }
}

@Composable
private fun CrashDialog(crashLog: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_title)) },
        text = {
            Text(
                text = crashLog,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .heightIn(max = Sizes.HeightPreview)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashLog))
                Toast.makeText(context, context.getString(R.string.toast_copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.action_copy)) }
        }
    )
}
