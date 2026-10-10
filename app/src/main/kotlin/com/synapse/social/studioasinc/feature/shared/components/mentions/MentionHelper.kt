package com.synapse.social.studioasinc.ui.components.mentions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class MentionQuery(
    val query: String,
    val range: TextRange
)

object MentionHelper {


    fun getMentionQuery(value: TextFieldValue): MentionQuery? {
        val text = value.text
        val cursor = value.selection.start

        if (cursor < 0) return null


        var matchStart = -1
        for (i in (cursor - 1) downTo 0) {
            val c = text[i]
            if (c == '@') {
                matchStart = i
                break
            }


            if (c.isWhitespace()) {
                return null
            }
        }

        if (matchStart != -1) {

            if (matchStart == 0 || text[matchStart - 1].isWhitespace()) {
                val query = text.substring(matchStart + 1, cursor)

                return MentionQuery(query, TextRange(matchStart, cursor))
            }
        }

        return null
    }
}
