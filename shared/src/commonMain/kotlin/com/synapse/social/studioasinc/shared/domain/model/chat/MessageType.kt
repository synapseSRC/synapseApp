package com.synapse.social.studioasinc.shared.domain.model.chat

import kotlinx.serialization.Serializable

/**
 * Defines the type of message content.
 *
 * Each type may require different handling for encryption, storage, and display.
 */
@Serializable
enum class MessageType {
    /** Plain text message */
    TEXT,

    /** Voice note message */
    VOICE,

    /** General audio file or track */
    AUDIO,

    /** Image file (JPEG, PNG, WebP, etc.) */
    IMAGE,

    /** Video file (MP4, MOV, etc.) */
    VIDEO,

    /** Batch of multiple media files (photos/videos) */
    MEDIA_GROUP,

    /** Generic file / document attachment (PDF, DOC, ZIP, etc.) */
    FILE,

    /** Shared contact card */
    CONTACT,

    /** Location coordinates and address */
    LOCATION,

    /** Real-time live location stream */
    LIVE_LOCATION,

    /** Animated GIF message */
    GIF,

    /** Sticker message */
    STICKER,

    /** Link preview card */
    LINK_PREVIEW,

    /** Interactive poll message */
    POLL,

    /** Music / Audio track card */
    MUSIC,

    /** Shared feed post */
    SHARED_POST,

    /** Shared story or reel */
    STORY_SHARE,

    /** Event invite / details card */
    EVENT,

    /** Product item preview card */
    PRODUCT,

    /** Payment transaction card */
    PAYMENT,

    /** Map location preview card */
    MAP,

    /** Code snippet with syntax highlighting support */
    CODE_SNIPPET,

    /** Voice/video call log message */
    CALL,

    /** System-generated message */
    SYSTEM,

    /** Ephemeral view once / view twice media */
    EPHEMERAL_MEDIA
}
