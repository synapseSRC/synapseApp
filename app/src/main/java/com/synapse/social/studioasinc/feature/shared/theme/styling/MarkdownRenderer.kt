package com.synapse.social.studioasinc.styling

import android.content.Context
import com.synapse.social.studioasinc.core.di.DatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.feature.shared.main.MainActivity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.html.HtmlPlugin
import com.synapse.social.studioasinc.core.util.CoilImagesPlugin
import com.synapse.social.studioasinc.core.util.IntentUtils
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.LinkResolver
import coil.imageLoader

class MarkdownRenderer private constructor(private val markwon: Markwon) {

    fun render(textView: TextView, markdown: String) {
        textView.movementMethod = LinkMovementMethod.getInstance()

        textView.setHorizontallyScrolling(false)
        markwon.setMarkdown(textView, markdown)
    }

    companion object {
        @Volatile private var instance: MarkdownRenderer? = null

        @JvmStatic
        fun get(context: Context): MarkdownRenderer {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): MarkdownRenderer {
            val linkResolver = LinkResolver { view, link ->
                IntentUtils.openUrl(view.context, link)
            }

            val markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(CoilImagesPlugin.create(context, context.imageLoader))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(JLatexMathPlugin.create(16.0f))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .linkColor(ContextCompat.getColor(context, R.color.primary))
                            .isLinkUnderlined(true)
                    }
                })
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver(linkResolver)
                    }
                    override fun afterSetText(textView: TextView) {
                        super.afterSetText(textView)
                        applyMentionHashtagSpans(textView)
                    }
                })
                .build()

            return MarkdownRenderer(markwon)
        }

        private fun applyMentionHashtagSpans(textView: TextView) {
            val text = textView.text
            if (text !is android.text.Spannable) return
            if ('@' !in text && '#' !in text) return
            val pattern = java.util.regex.Pattern.compile("(?<![^\\s])([@#])([A-Za-z0-9_.-]+)")
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val symbol = matcher.group(1) ?: continue
                val full = matcher.group(0) ?: continue
                val span = if (symbol == "@") object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val ctx = widget.context
                        val username = full.substring(1)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val entryPoint = EntryPointAccessors.fromApplication(ctx.applicationContext, DatabaseEntryPoint::class.java)
                                val userDao = entryPoint.getUserDao()
                                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(userDao)

                                val userResult = userRepository.getUserByUsername(username)

                                userResult.fold(
                                    onSuccess = { user ->
                                        if (user != null) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                IntentUtils.openUrl(ctx, "synapse://profile/${user.uid}")
                                            }
                                        }
                                    },
                                    onFailure = { error ->
                                        Log.e("MarkdownRenderer", "Error finding user: ${error.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("MarkdownRenderer", "Error finding user: ${e.message}")
                            }
                        }
                    }
                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = Color.parseColor("#445E91")
                        ds.isUnderlineText = false
                        ds.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                } else object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Log.d("MarkdownRenderer", "Hashtag clicked: $full")
                    }
                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = Color.parseColor("#445E91")
                        ds.isUnderlineText = false
                        ds.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                }
                text.setSpan(span, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
