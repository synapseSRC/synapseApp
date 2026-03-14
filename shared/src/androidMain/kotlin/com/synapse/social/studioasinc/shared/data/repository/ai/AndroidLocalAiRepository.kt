package com.synapse.social.studioasinc.shared.data.repository.ai

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.aakira.napier.Napier
import java.io.File

class AndroidLocalAiRepository(private val context: Context) : AiRepository {

    private var llmInference: LlmInference? = null
    private var textClassifier: TextClassifier? = null
    private var imageClassifier: ImageClassifier? = null

    private fun getLlmInference(): LlmInference {
        if (llmInference == null) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/gemini-nano.bin")
                .setMaxTokens(512)
                .setTemperature(0.7f)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
        return llmInference!!
    }

    private fun getTextClassifier(): TextClassifier {
        if (textClassifier == null) {
            val options = TextClassifier.TextClassifierOptions.builder()
                .setBaseOptions(com.google.mediapipe.tasks.core.BaseOptions.builder()
                    .setModelAssetPath("toxicity_model.tflite")
                    .build())
                .build()
            textClassifier = TextClassifier.createFromOptions(context, options)
        }
        return textClassifier!!
    }

    private fun getImageClassifier(): ImageClassifier {
        if (imageClassifier == null) {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(com.google.mediapipe.tasks.core.BaseOptions.builder()
                    .setModelAssetPath("nsfw_model.tflite")
                    .build())
                .setMaxResults(3)
                .setScoreThreshold(0.5f)
                .build()
            imageClassifier = ImageClassifier.createFromOptions(context, options)
        }
        return imageClassifier!!
    }

    override suspend fun generateSmartReplies(recentMessages: List<String>): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Based on these messages, suggest 3 short replies:\n" + recentMessages.joinToString("\n")
            val response = getLlmInference().generateResponse(prompt)
            val replies = response.lines().filter { it.isNotBlank() }.take(3)
            Result.success(replies)
        } catch (e: Exception) {
            Napier.e("Error generating smart replies locally", e)
            Result.failure(e)
        }
    }

    override suspend fun summarizeChat(messages: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Summarize this chat:\n" + messages.joinToString("\n")
            val response = getLlmInference().generateResponse(prompt)
            Result.success(response.trim())
        } catch (e: Exception) {
            Napier.e("Error summarizing chat locally", e)
            Result.failure(e)
        }
    }

    override suspend fun isContentToxic(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val result = getTextClassifier().classify(text)
            val isToxic = result.classificationResult().classifications().firstOrNull()?.categories()?.any {
                it.categoryName() == "toxic" && it.score() > 0.8f
            } ?: false
            Result.success(isToxic)
        } catch (e: Exception) {
            Napier.e("Error checking toxicity locally", e)
            Result.failure(e)
        }
    }

    override suspend fun detectSensitiveContent(mediaPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(mediaPath)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            val bitmap = BitmapFactory.decodeFile(mediaPath) ?: return@withContext Result.failure(Exception("Failed to decode image"))
            val mpImage = BitmapImageBuilder(bitmap).build()

            val result = getImageClassifier().classify(mpImage)
            val isSensitive = result.classificationResult().classifications().firstOrNull()?.categories()?.any {
                (it.categoryName() == "nsfw" || it.categoryName() == "violence") && it.score() > 0.7f
            } ?: false

            Result.success(isSensitive)
        } catch (e: Exception) {
            Napier.e("Error detecting sensitive content locally", e)
            Result.failure(e)
        }
    }
}
