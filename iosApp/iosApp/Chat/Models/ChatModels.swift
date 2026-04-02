import Foundation
import shared

// MARK: - Swift Models mirroring KMP Domain Models

struct SwiftConversation: Identifiable {
    let id: String // Use chatId as Identifiable id
    let participantId: String
    let participantName: String
    let participantAvatar: String?
    let lastMessage: String
    let lastMessageTime: String?
    let unreadCount: Int
    let isOnline: Bool
    let isGroup: Bool
    let groupMembers: [String]

    init(from kmpConversation: shared.Conversation) {
        self.id = kmpConversation.chatId
        self.participantId = kmpConversation.participantId
        self.participantName = kmpConversation.participantName
        self.participantAvatar = kmpConversation.participantAvatar
        self.lastMessage = kmpConversation.lastMessage
        self.lastMessageTime = kmpConversation.lastMessageTime
        self.unreadCount = Int(kmpConversation.unreadCount)
        self.isOnline = kmpConversation.isOnline
        self.isGroup = kmpConversation.isGroup
        self.groupMembers = kmpConversation.groupMembers
    }
}

struct SwiftMessage: Identifiable {
    let id: String
    let chatId: String
    let senderId: String
    let content: String
    let messageType: shared.MessageType
    let mediaUrl: String?
    let deliveryStatus: shared.DeliveryStatus
    let isDeleted: Bool
    var isEdited: Bool
    let replyToId: String?
    let createdAt: String
    let updatedAt: String?
    let readBy: [String]
    let expiresAt: String?
    let reactions: [shared.ReactionType: Int]
    let userReaction: shared.ReactionType?

    init(from kmpMessage: shared.Message) {
        self.id = kmpMessage.id
        self.chatId = kmpMessage.chatId
        self.senderId = kmpMessage.senderId
        self.content = kmpMessage.content
        self.messageType = kmpMessage.messageType
        self.mediaUrl = kmpMessage.mediaUrl
        self.deliveryStatus = kmpMessage.deliveryStatus
        self.isDeleted = kmpMessage.isDeleted
        self.isEdited = kmpMessage.isEdited
        self.replyToId = kmpMessage.replyToId
        self.createdAt = kmpMessage.createdAt
        self.updatedAt = kmpMessage.updatedAt
        self.readBy = kmpMessage.readBy
        self.expiresAt = kmpMessage.expiresAt
        self.reactions = kmpMessage.reactions as? [shared.ReactionType: Int] ?? [:]
        self.userReaction = kmpMessage.userReaction
    }
}
