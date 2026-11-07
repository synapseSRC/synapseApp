package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.PollMetadata
import com.synapse.social.studioasinc.shared.domain.model.chat.PollOption
import com.synapse.social.studioasinc.shared.util.UUIDUtils

/**
 * UseCase responsible for creating and sending an interactive poll message.
 *
 * It validates the poll question and options, generates stable identifiers, builds the
 * [PollMetadata] payload and delegates persistence/transport to [SendMessageUseCase] using
 * the "poll" message type. All validation failures are surfaced as
 * [Result.failure] wrapping an [IllegalArgumentException] — never thrown.
 *
 * This is pure domain logic: it has no knowledge of any framework, SDK or platform.
 */
class CreatePollUseCase(private val sendMessageUseCase: SendMessageUseCase) {

    suspend operator fun invoke(
        chatId: String,
        question: String,
        options: List<String>,
        allowMultipleAnswers: Boolean
    ): Result<Message> {
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("Poll requires a target chat."))
        }
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank()) {
            return Result.failure(IllegalArgumentException("Poll question cannot be empty."))
        }
        if (trimmedQuestion.length > MAX_QUESTION_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Poll question must be at most $MAX_QUESTION_LENGTH characters.")
            )
        }

        val trimmedOptions = options.map { it.trim() }
        if (trimmedOptions.size < MIN_OPTIONS || trimmedOptions.size > MAX_OPTIONS) {
            return Result.failure(
                IllegalArgumentException("A poll must have between $MIN_OPTIONS and $MAX_OPTIONS options.")
            )
        }

        trimmedOptions.forEach { option ->
            if (option.isBlank()) {
                return Result.failure(IllegalArgumentException("Poll options cannot be empty."))
            }
            if (option.length > MAX_OPTION_LENGTH) {
                return Result.failure(
                    IllegalArgumentException("Each poll option must be at most $MAX_OPTION_LENGTH characters.")
                )
            }
        }

        val distinctOptions = trimmedOptions.map { it.lowercase() }.distinct()
        if (distinctOptions.size != trimmedOptions.size) {
            return Result.failure(IllegalArgumentException("Poll options must be unique."))
        }

        val pollOptions = trimmedOptions.map { text ->
            PollOption(
                id = UUIDUtils.randomUUID(),
                text = text,
                voteCount = 0,
                voterUserIds = emptyList()
            )
        }

        val pollMetadata = PollMetadata(
            pollId = UUIDUtils.randomUUID(),
            question = trimmedQuestion,
            options = pollOptions,
            allowMultipleAnswers = allowMultipleAnswers,
            isClosed = false
        )

        val metadataContainer = MessageMetadataContainer(poll = pollMetadata)

        return sendMessageUseCase(
            chatId = chatId,
            content = trimmedQuestion,
            messageType = POLL_MESSAGE_TYPE,
            metadataContainer = metadataContainer
        )
    }

    private companion object {
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 10
        const val MAX_QUESTION_LENGTH = 255
        const val MAX_OPTION_LENGTH = 80
        const val POLL_MESSAGE_TYPE = "poll"
    }
}
