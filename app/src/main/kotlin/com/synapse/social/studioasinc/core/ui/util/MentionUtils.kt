package com.synapse.social.studioasinc.core.ui.util

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.core.di.DatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import java.util.regex.Pattern

object MentionUtils {

    private val mentionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun handleMentions(context: Context, textView: TextView, text: String) {
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val username = matcher.group(1)
            if (username != null) {
                val start = matcher.start()
                val end = matcher.end()

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        mentionScope.launch(Dispatchers.IO) {
                            try {
                                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, DatabaseEntryPoint::class.java)
                                val userDao = entryPoint.getUserDao()
                                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepositoryImpl(userDao)

                                val userResult = userRepository.getUserByUsername(username)

                                userResult.fold(
                                    onSuccess = { user ->
                                        if (user != null) {
                                            mentionScope.launch {
                                                IntentUtils.openUrl(context, "synapse://profile/${user.uid}")
                                            }
                                        }
                                    },
                                    onFailure = { error ->
                                        android.util.Log.e("MentionUtils", "Error finding user: ${error.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MentionUtils", "Error finding user: ${e.message}")
                            }
                        }
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = ContextCompat.getColor(context, R.color.primary)
                    }
                }
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    fun extractMentions(text: String): List<String> {
        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(text)
        val mentions = mutableListOf<String>()

        while (matcher.find()) {
            val username = matcher.group(1)
            if (username != null && !mentions.contains(username)) {
                mentions.add(username)
            }
        }

        return mentions
    }
}
