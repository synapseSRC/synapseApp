package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object IntentUtils {
    /**
     * Opens a URL safely.
     * - synapse:// schemes are restricted to the app's own package.
     * - http/https schemes are opened in Custom Tabs.
     * - Other schemes use Intent.createChooser to prevent automatic redirection to potentially malicious apps.
     */
    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return
        }

        val scheme = uri.scheme?.lowercase()

        when {
            scheme == "synapse" -> {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(context.packageName)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
            scheme == "http" || scheme == "https" -> {
                try {
                    CustomTabsIntent.Builder()
                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                        .build()
                        .launchUrl(context, uri)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
            }
            else -> {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
            }
        }
    }
}
