package com.synapse.social.studioasinc.feature.shared.components.mentions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object HashtagTextFormatter {
    private val HASHTAG_REGEX = Regex("#([\\w]+)")

    fun buildHashtagText(
        text: String,
        hashtagColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0
            val matches = HASHTAG_REGEX.findAll(text)

            for (match in matches) {
                val tag = match.groupValues[1]
                if (tag.all { it.isDigit() }) {
                    append(text.substring(lastIndex, match.range.last + 1))
                    lastIndex = match.range.last + 1
                    continue
                }

                append(text.substring(lastIndex, match.range.first))

                pushStringAnnotation(tag = "HASHTAG", annotation = tag)
                withStyle(
                    SpanStyle(
                        color = hashtagColor,
                        fontWeight = FontWeight.Bold
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
