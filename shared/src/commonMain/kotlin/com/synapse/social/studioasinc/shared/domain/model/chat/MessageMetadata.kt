package com.synapse.social.studioasinc.shared.domain.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class ContactMetadata(
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val userId: String? = null
)

@Serializable
data class LocationMetadata(
    val latitude: Double,
    val longitude: Double,
    val title: String? = null,
    val address: String? = null
)

@Serializable
data class LiveLocationMetadata(
    val latitude: Double,
    val longitude: Double,
    val expiresAtIso: String? = null,
    val isLive: Boolean = true
)

@Serializable
data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Int = 0,
    val voterUserIds: List<String> = emptyList()
)

@Serializable
data class PollMetadata(
    val pollId: String,
    val question: String,
    val options: List<PollOption> = emptyList(),
    val allowMultipleAnswers: Boolean = false,
    val isClosed: Boolean = false
)

@Serializable
data class MusicMetadata(
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtUrl: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int = 0
)

@Serializable
data class SharedPostMetadata(
    val postId: String,
    val authorName: String,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null,
    val content: String? = null,
    val mediaUrl: String? = null,
    val isVideo: Boolean = false
)

@Serializable
data class SharedStoryMetadata(
    val storyId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val mediaUrl: String? = null,
    val isVideo: Boolean = false
)

@Serializable
data class EventMetadata(
    val eventId: String,
    val title: String,
    val startTimeIso: String,
    val location: String? = null,
    val organizerName: String? = null,
    val bannerUrl: String? = null
)

@Serializable
data class ProductMetadata(
    val productId: String,
    val title: String,
    val price: String,
    val currency: String = "USD",
    val imageUrl: String? = null,
    val vendorName: String? = null
)

@Serializable
data class PaymentMetadata(
    val transactionId: String,
    val amount: String,
    val currency: String = "USD",
    val status: String, // COMPLETED, PENDING, FAILED
    val note: String? = null
)

@Serializable
data class MapMetadata(
    val latitude: Double,
    val longitude: Double,
    val locationName: String? = null,
    val zoomLevel: Float = 15f
)

@Serializable
data class CodeSnippetMetadata(
    val code: String,
    val language: String = "kotlin",
    val filename: String? = null
)

@Serializable
data class CallMetadata(
    val callId: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 0,
    val callStatus: String // MISSED, COMPLETED, REJECTED, BUSY
)

@Serializable
data class ViewOnceMetadata(
    val mode: String = "VIEW_ONCE", // VIEW_ONCE, VIEW_TWICE
    val mediaType: String = "IMAGE", // IMAGE, VIDEO
    val isConsumed: Boolean = false,
    val remainingViews: Int = 1
)

@Serializable
data class ForwardedMetadata(
    val originalSenderId: String? = null,
    val originalSenderName: String? = null,
    val originalMessageId: String? = null
)

@Serializable
data class MessageMetadataContainer(
    val contact: ContactMetadata? = null,
    val location: LocationMetadata? = null,
    val liveLocation: LiveLocationMetadata? = null,
    val poll: PollMetadata? = null,
    val music: MusicMetadata? = null,
    val sharedPost: SharedPostMetadata? = null,
    val sharedStory: SharedStoryMetadata? = null,
    val event: EventMetadata? = null,
    val product: ProductMetadata? = null,
    val payment: PaymentMetadata? = null,
    val map: MapMetadata? = null,
    val codeSnippet: CodeSnippetMetadata? = null,
    val call: CallMetadata? = null,
    val viewOnce: ViewOnceMetadata? = null,
    val forwarded: ForwardedMetadata? = null
)
