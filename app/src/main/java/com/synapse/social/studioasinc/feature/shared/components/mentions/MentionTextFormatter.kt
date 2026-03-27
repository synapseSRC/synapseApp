package com.synapse.social.studioasinc.ui.components.mentions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object MentionTextFormatter {
    private val MENTION_REGEX = Regex("(?<=^|\\s)@[a-zA-Z0-9_.]+")

    fun buildMentionText(
        text: String,
        mentionColor: Color,
        pillColor: Color? = null
    ): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0
            val matches = MENTION_REGEX.findAll(text)

            for (match in matches) {

                append(text.substring(lastIndex, match.range.first))


                pushStringAnnotation(tag = "MENTION", annotation = match.value.substring(1))
                withStyle(
                    SpanStyle(
                        color = mentionColor,
                        fontWeight = FontWeight.Bold,
                        background = pillColor ?: Color.Unspecified
                    )
                ) {
                    append(match.value)
                }
                pop()

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
