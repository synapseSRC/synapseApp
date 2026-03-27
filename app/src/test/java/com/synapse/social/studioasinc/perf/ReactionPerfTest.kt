package com.synapse.social.studioasinc.perf

import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.random.Random
import kotlinx.serialization.json.*

class ReactionPerfTest {

    @Test
    fun benchmarkReactionGrouping() {
        val random = Random(42)
        val totalReactions = 10_000
        val totalPosts = 5_000

        val allPosts = (1..totalPosts).map { it.toString() }

        // Generate mock JSON objects
        val reactions = (1..totalReactions).map {
            buildJsonObject {
                put("post_id", allPosts[random.nextInt(allPosts.size)])
                put("reaction_type", "LIKE")
                put("user_id", "user_${random.nextInt(100)}")
            }
        }

        val chunkIds = allPosts.shuffled(random).take(20)
        val iterations = 50

        var timeOldTotal = 0L
        var timeNewTotal = 0L

        for (i in 0..iterations) {
            val timeOld = measureTimeMillis {
                val result = chunkIds.map { postId ->
                    val postReactions = reactions.filter { it["post_id"]?.jsonPrimitive?.contentOrNull == postId }
                    val summary = postReactions
                        .groupBy { it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE" }
                        .mapValues { it.value.size }
                    postId to summary
                }
            }

            val timeNew = measureTimeMillis {
                val reactionsByPost = reactions.groupBy { it["post_id"]?.jsonPrimitive?.contentOrNull }
                val result = chunkIds.map { postId ->
                    val postReactions = reactionsByPost[postId] ?: emptyList()
                    val summary = postReactions
                        .groupBy { it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE" }
                        .mapValues { it.value.size }
                    postId to summary
                }
            }

            if (i >= 10) {
                timeOldTotal += timeOld
                timeNewTotal += timeNew
            }
        }

        val validIterations = iterations - 9
        println("BENCHMARK RESULT:")
        println("Old approach average took: ${timeOldTotal.toDouble() / validIterations} ms")
        println("New approach average took: ${timeNewTotal.toDouble() / validIterations} ms")
        val improvement = if (timeOldTotal > 0) ((timeOldTotal - timeNewTotal).toDouble() / timeOldTotal * 100) else 0.0
        println(String.format("Improvement: %.2f%%", improvement))
    }
}
