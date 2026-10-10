package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class PollOptionResult(
    val index: Int,
    val text: String,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val percentage: Float = 0f
) {
    companion object {


        fun calculateResults(
            options: List<String>,
            voteCounts: Map<Int, Int>
        ): List<PollOptionResult> {
            val totalVotes = voteCounts.values.sum()
            return options.mapIndexed { index, text ->
                val count = voteCounts[index] ?: 0
                val percentage = if (totalVotes > 0) {
                    (count.toFloat() / totalVotes) * 100f
                } else {
                    0f
                }
                PollOptionResult(
                    index = index,
                    text = text,
                    voteCount = count,
                    percentage = percentage
                )
            }
        }

    }
}
