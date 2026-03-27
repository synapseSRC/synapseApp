package com.synapse.social.studioasinc.shared.domain.model.chat

/**
 * Defines the type of message content.
 *
 * Each type may require different handling for encryption, storage, and display.
 */
enum class MessageType {
    /** Plain text message */
    TEXT,

    /** Image file (JPEG, PNG, GIF, WebP, etc.) */
    IMAGE,

    /** Video file (MP4, MOV, etc.) */
    VIDEO,

    /** Audio message or voice note */
    AUDIO,

    /** Generic file attachment (PDF, DOC, ZIP, etc.) */
    FILE,

    /** Voice/video call initiation or status */
    CALL,

    /** System-generated message (user joined, left, settings changed, etc.) */
    SYSTEM
}
