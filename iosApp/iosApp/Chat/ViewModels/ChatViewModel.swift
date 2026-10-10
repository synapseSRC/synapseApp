import Combine
import Foundation
import shared
import OSLog

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [SwiftMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var isSending = false
    @Published var smartReplies: [String] = []
    @Published var isParticipantTyping: Bool = false
    @Published var disappearingMode: shared.DisappearingMode = .off

    private let getMessagesUseCase: shared.GetMessagesUseCase?
    private let subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase?
    private let sendMessageUseCase: shared.SendMessageUseCase?
    private let uploadMediaUseCase: shared.UploadMediaUseCase?
    private let broadcastTypingStatusUseCase: shared.BroadcastTypingStatusUseCase?
    private let subscribeToTypingStatusUseCase: shared.SubscribeToTypingStatusUseCase?
    private let toggleMessageReactionUseCase: shared.ToggleMessageReactionUseCase?
    private let subscribeToMessageReactionsUseCase: shared.SubscribeToMessageReactionsUseCase?
    private let populateMessageReactionsUseCase: shared.PopulateMessageReactionsUseCase?
    private let generateSmartRepliesUseCase: shared.GenerateSmartRepliesUseCase?
    private let setDisappearingModeUseCase: shared.SetDisappearingModeUseCase?
    private let getDisappearingModeUseCase: shared.GetDisappearingModeUseCase?
    private let markViewOnceConsumedUseCase: shared.MarkViewOnceConsumedUseCase?
    private let voteInPollUseCase: shared.VoteInPollUseCase?

    private var chatId: String? = nil
    private var subscriptionTask: Task<Void, Never>? = nil
    private var typingSubscriptionTask: Task<Void, Never>? = nil
    private var reactionsSubscriptionTask: Task<Void, Never>? = nil
    private var smartRepliesTask: Task<Void, Never>? = nil
    private let typingSubject = PassthroughSubject<Bool, Never>()
    private var typingCancellable: AnyCancellable? = nil
     
    private let currentUserId = "my_user_id" 
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "ChatViewModel")

    init(
        getMessagesUseCase: shared.GetMessagesUseCase? = KMPHelper.sharedHelper.getMessagesUseCase,
        subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase? = KMPHelper.sharedHelper.subscribeToMessagesUseCase,
        sendMessageUseCase: shared.SendMessageUseCase? = KMPHelper.sharedHelper.sendMessageUseCase,
        uploadMediaUseCase: shared.UploadMediaUseCase? = KMPHelper.sharedHelper.uploadMediaUseCase,
        broadcastTypingStatusUseCase: shared.BroadcastTypingStatusUseCase? = KMPHelper.sharedHelper.broadcastTypingStatusUseCase,
        subscribeToTypingStatusUseCase: shared.SubscribeToTypingStatusUseCase? = KMPHelper.sharedHelper.subscribeToTypingStatusUseCase,
        toggleMessageReactionUseCase: shared.ToggleMessageReactionUseCase? = KMPHelper.sharedHelper.toggleMessageReactionUseCase,
        subscribeToMessageReactionsUseCase: shared.SubscribeToMessageReactionsUseCase? = KMPHelper.sharedHelper.subscribeToMessageReactionsUseCase,
        populateMessageReactionsUseCase: shared.PopulateMessageReactionsUseCase? = KMPHelper.sharedHelper.populateMessageReactionsUseCase,
        generateSmartRepliesUseCase: shared.GenerateSmartRepliesUseCase? = KMPHelper.sharedHelper.generateSmartRepliesUseCase,
        setDisappearingModeUseCase: shared.SetDisappearingModeUseCase? = KMPHelper.sharedHelper.setDisappearingModeUseCase,
        getDisappearingModeUseCase: shared.GetDisappearingModeUseCase? = KMPHelper.sharedHelper.getDisappearingModeUseCase,
        markViewOnceConsumedUseCase: shared.MarkViewOnceConsumedUseCase? = KMPHelper.sharedHelper.markViewOnceConsumedUseCase,
        voteInPollUseCase: shared.VoteInPollUseCase? = KMPHelper.sharedHelper.voteInPollUseCase
    ) {
        self.getMessagesUseCase = getMessagesUseCase
        self.subscribeToMessagesUseCase = subscribeToMessagesUseCase
        self.sendMessageUseCase = sendMessageUseCase
        self.uploadMediaUseCase = uploadMediaUseCase
        self.broadcastTypingStatusUseCase = broadcastTypingStatusUseCase
        self.subscribeToTypingStatusUseCase = subscribeToTypingStatusUseCase
        self.toggleMessageReactionUseCase = toggleMessageReactionUseCase
        self.subscribeToMessageReactionsUseCase = subscribeToMessageReactionsUseCase
        self.populateMessageReactionsUseCase = populateMessageReactionsUseCase
        self.generateSmartRepliesUseCase = generateSmartRepliesUseCase
        self.setDisappearingModeUseCase = setDisappearingModeUseCase
        self.getDisappearingModeUseCase = getDisappearingModeUseCase
        self.markViewOnceConsumedUseCase = markViewOnceConsumedUseCase
        self.voteInPollUseCase = voteInPollUseCase
    }

    func setup(chatId: String) {
        self.chatId = chatId
        fetchDisappearingMode()
        fetchMessages()
        subscribeToMessages()
        subscribeToTypingStatus()
        subscribeToMessageReactions()
        setupTypingDebounce()
    }

    func fetchDisappearingMode() {
        guard let useCase = getDisappearingModeUseCase, let chatId = chatId else { return }
        Task {
            do {
                let result = try await useCase.invoke(chatId: chatId)
                if let mode = result.getOrNull() as? shared.DisappearingMode {
                    self.disappearingMode = mode
                }
            } catch {
                logger.error("Failed to get disappearing mode: \(error.localizedDescription)")
            }
        }
    }

    func setDisappearingMode(mode: shared.DisappearingMode) {
        guard let useCase = setDisappearingModeUseCase, let chatId = chatId else { return }
        self.disappearingMode = mode
        Task {
            do {
                let _ = try await useCase.invoke(chatId: chatId, mode: mode)
            } catch {
                logger.error("Failed to set disappearing mode: \(error.localizedDescription)")
            }
        }
    }

    private func setupTypingDebounce() {
        typingCancellable = typingSubject
            .removeDuplicates()
            .handleEvents(receiveOutput: { isTyping in
                if isTyping {
                    Task { try? await self.broadcastTypingStatusUseCase?.invoke(chatId: self.chatId ?? "", isTyping: true) }
                }
            })
            .debounce(for: .seconds(2), scheduler: RunLoop.main)
            .sink { isTyping in
                if isTyping {
                    self.typingSubject.send(false)
                } else {
                    Task { try? await self.broadcastTypingStatusUseCase?.invoke(chatId: self.chatId ?? "", isTyping: false) }
                }
            }
    }

    func fetchMessages() {
        guard let useCase = getMessagesUseCase, let populateUseCase = populateMessageReactionsUseCase, let chatId = chatId else {
            self.errorMessage = "Dependencies not initialized"
            return
        }
        self.isLoading = true
        self.errorMessage = nil

        Task {
            do {
                let result = try await useCase.invoke(chatId: chatId, limit: 50, before: nil)
                if let data = result.getOrNull() as? [shared.Message] {
                    let populatedData = try await populateUseCase.invoke(messages: data)
                    self.messages = populatedData.map { SwiftMessage(from: $0) }.sorted(by: { $0.createdAt < $1.createdAt })
                } else if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }
            self.isLoading = false
        }
    }

    func subscribeToMessages() {
        guard let useCase = subscribeToMessagesUseCase, let chatId = chatId else { return }

        let encryptedPlaceholders: Set<String> = [
            "Message is encrypted",
            "🔒 Encrypted message",
            "🔒 You sent an encrypted message",
            "🔒 You sent an encrypted message (Copy)"
        ]

        subscriptionTask?.cancel()
        subscriptionTask = Task {
            let flow = useCase.invoke(chatId: chatId)
            do {
                for try await message in flow.asAsyncStream(type: shared.Message.self) {
                    let swiftMsg = SwiftMessage(from: message)
                    if let index = self.messages.firstIndex(where: { $0.id == swiftMsg.id }) {
                        let existing = self.messages[index]
                        if !encryptedPlaceholders.contains(existing.content) &&
                            encryptedPlaceholders.contains(swiftMsg.content) {
                        } else {
                            var merged = swiftMsg
                            if existing.content == swiftMsg.content {
                                merged.isEdited = existing.isEdited
                            }
                            self.messages[index] = merged
                        }
                    } else {
                        self.messages.append(swiftMsg)
                        self.messages.sort(by: { $0.createdAt < $1.createdAt })
                    }
                    self.generateSmartReplies()
                }
            } catch {
                logger.error("Flow collection failed: \(error.localizedDescription)")
            }
        }
    }

    func subscribeToTypingStatus() {
        guard let useCase = subscribeToTypingStatusUseCase, let chatId = chatId else { return }

        typingSubscriptionTask?.cancel()
        typingSubscriptionTask = Task {
            let flow = useCase.invoke(chatId: chatId)
            do {
                for try await status in flow.asAsyncStream(type: shared.TypingStatus.self) {
                    if status.userId != self.currentUserId {
                        self.isParticipantTyping = status.isTyping
                    }
                }
            } catch {
                logger.error("Typing status flow collection failed: \(error.localizedDescription)")
            }
        }
    }

    func subscribeToMessageReactions() {
        guard let useCase = subscribeToMessageReactionsUseCase, let chatId = chatId else { return }

        reactionsSubscriptionTask?.cancel()
        reactionsSubscriptionTask = Task {
            let flow = useCase.invoke(chatId: chatId)
            do {
                for try await reaction in flow.asAsyncStream(type: shared.MessageReaction.self) {
                    if let index = self.messages.firstIndex(where: { $0.id == reaction.messageId }) {
                        var existing = self.messages[index]

                        var reactions = existing.reactions
                        let reactionKey = reaction.reactionEmoji

                        if reaction.isDelete {
                            if let count = reactions[reactionKey], count > 1 {
                                reactions[reactionKey] = count - 1
                            } else {
                                reactions.removeValue(forKey: reactionKey)
                            }
                            if existing.userReaction == reactionKey && reaction.userId == self.currentUserId {
                                existing.userReaction = nil
                            }
                        } else {
                            reactions[reactionKey] = (reactions[reactionKey] ?? 0) + 1
                            if reaction.userId == self.currentUserId {
                                existing.userReaction = reactionKey
                            }
                        }

                        existing.reactions = reactions
                        self.messages[index] = existing
                    }
                }
            } catch {
                logger.error("Reaction flow collection failed: \(error.localizedDescription)")
            }
        }
    }

    func onTyping() {
        typingSubject.send(true)
    }

    func toggleReaction(messageId: String, emoji: String) {
        guard let useCase = toggleMessageReactionUseCase, let chatId = chatId else { return }
        Task {
            let _ = try? await useCase.invoke(messageId: messageId, emoji: emoji, chatId: chatId)
        }
    }

    func sendMessage(content: String) {
        guard let useCase = sendMessageUseCase, let chatId = chatId else { return }
        guard !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        self.isSending = true

        let expiresAt: String?
        if let seconds = self.disappearingMode.seconds {
            let expirationDate = Date().addingTimeInterval(TimeInterval(truncating: seconds))
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            expiresAt = formatter.string(from: expirationDate)
        } else {
            expiresAt = nil
        }

        Task {
            do {
                let result = try await useCase.invoke(
                    chatId: chatId,
                    content: content,
                    mediaUrl: nil,
                    messageType: "TEXT",
                    expiresAt: expiresAt,
                    replyToId: nil
                )
                if let data = result.getOrNull() as? shared.Message {
                    let swiftMsg = SwiftMessage(from: data)
                    if !self.messages.contains(where: { $0.id == swiftMsg.id }) {
                        self.messages.append(swiftMsg)
                    }
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }
            self.isSending = false
            self.generateSmartReplies()
        }
    }

    func generateSmartReplies() {
        guard let useCase = generateSmartRepliesUseCase else { return }

        let recentMessages = self.messages.suffix(10).map { "\($0.senderId): \($0.content)" }
        guard !recentMessages.isEmpty else { return }

        smartRepliesTask?.cancel()
        smartRepliesTask = Task {
            let result = try? await useCase.invoke(recentMessages: recentMessages)
            if Task.isCancelled { return }
            if let replies = result?.getOrNull() as? [String] {
                self.smartReplies = replies
            }
        }
    }

    func markViewOnceConsumed(messageId: String) {
        guard let useCase = markViewOnceConsumedUseCase else { return }
        Task {
            let _ = try? await useCase.invoke(messageId: messageId)
            if let index = messages.firstIndex(where: { $0.id == messageId }) {
                messages[index].metadataContainer?.viewOnce?.isConsumed = true
                messages[index].metadataContainer?.viewOnce?.isConsumed = true
            }
        }
    }

    func voteInPoll(messageId: String, optionId: String) {
        guard let useCase = voteInPollUseCase else { return }
        Task {
            let _ = try? await useCase.invoke(messageId: messageId, optionId: optionId)
        }
    }

    func sendMediaMessage(data: Data, fileName: String, mimeType: String) {
        guard let uploadUseCase = uploadMediaUseCase, let sendUseCase = sendMessageUseCase, let chatId = chatId else { return }

        self.isSending = true

        Task {
            do {
                let kotlinArray = shared.KotlinByteArray(size: Int32(data.count))

                let uploadResult = try await uploadUseCase.invoke(
                    chatId: chatId,
                    fileBytes: kotlinArray,
                    fileName: fileName,
                    contentType: mimeType
                )

                if let mediaUrl = uploadResult.getOrNull() as? String {
                    let result = try await sendUseCase.invoke(
                        chatId: chatId,
                        content: "Media",
                        mediaUrl: mediaUrl,
                        messageType: "IMAGE",
                        expiresAt: nil,
                        replyToId: nil
                    )

                    if let msgData = result.getOrNull() as? shared.Message {
                        let swiftMsg = SwiftMessage(from: msgData)
                        if !self.messages.contains(where: { $0.id == swiftMsg.id }) {
                            self.messages.append(swiftMsg)
                        }
                    }
                } else if let err = uploadResult.exceptionOrNull() {
                    self.errorMessage = err.message
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }
            self.isSending = false
        }
    }
}
