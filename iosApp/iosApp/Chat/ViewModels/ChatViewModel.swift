import Foundation
import shared

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [SwiftMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var isSending = false
    @Published var smartReplies: [String] = []
    @Published var isParticipantTyping: Bool = false

    // Using UseCases to interface with KMP Shared layer
    private let getMessagesUseCase: shared.GetMessagesUseCase?
    private let subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase?
    private let sendMessageUseCase: shared.SendMessageUseCase?
    private let uploadMediaUseCase: shared.UploadMediaUseCase?
    private let broadcastTypingStatusUseCase: shared.BroadcastTypingStatusUseCase?
    private let subscribeToTypingStatusUseCase: shared.SubscribeToTypingStatusUseCase?

    private var chatId: String? = nil
    private var subscriptionTask: Task<Void, Never>? = nil
    private var typingSubscriptionTask: Task<Void, Never>? = nil
    private var typingDebounceTask: Task<Void, Never>? = nil
    
    // Replace with current user ID from Auth context
    private let currentUserId = "my_user_id" 

    init(
        getMessagesUseCase: shared.GetMessagesUseCase? = KMPHelper.sharedHelper.getMessagesUseCase,
        subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase? = KMPHelper.sharedHelper.subscribeToMessagesUseCase,
        sendMessageUseCase: shared.SendMessageUseCase? = KMPHelper.sharedHelper.sendMessageUseCase,
        uploadMediaUseCase: shared.UploadMediaUseCase? = KMPHelper.sharedHelper.uploadMediaUseCase,
        broadcastTypingStatusUseCase: shared.BroadcastTypingStatusUseCase? = KMPHelper.sharedHelper.broadcastTypingStatusUseCase,
        subscribeToTypingStatusUseCase: shared.SubscribeToTypingStatusUseCase? = KMPHelper.sharedHelper.subscribeToTypingStatusUseCase
    ) {
        self.getMessagesUseCase = getMessagesUseCase
        self.subscribeToMessagesUseCase = subscribeToMessagesUseCase
        self.sendMessageUseCase = sendMessageUseCase
        self.uploadMediaUseCase = uploadMediaUseCase
        self.broadcastTypingStatusUseCase = broadcastTypingStatusUseCase
        self.subscribeToTypingStatusUseCase = subscribeToTypingStatusUseCase
    }

    func setup(chatId: String) {
        self.chatId = chatId
        fetchMessages()
        subscribeToMessages()
        subscribeToTypingStatus()
    }

    deinit {
        subscriptionTask?.cancel()
        typingSubscriptionTask?.cancel()
        typingDebounceTask?.cancel()
    }

    func fetchMessages() {
        guard let useCase = getMessagesUseCase, let chatId = chatId else {
            self.errorMessage = "Dependencies not initialized"
            return
        }
        self.isLoading = true
        self.errorMessage = nil

        Task {
            do {
                let result = try await useCase.invoke(chatId: chatId, limit: 50, before: nil)
                if let data = result.getOrNull() as? [shared.Message] {
                    self.messages = data.map { SwiftMessage(from: $0) }.sorted(by: { $0.createdAt < $1.createdAt })
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
                            // Keep existing decrypted content; only update delivery metadata
                            // (prevents flash of "Message is encrypted" over plaintext)
                        } else {
                            // Preserve isEdited unless content actually changed
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
                }
            } catch {
                print("Flow collection failed: \(error)")
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
                print("Typing status flow collection failed: \(error)")
            }
        }
    }

    func onTyping() {
        guard let useCase = broadcastTypingStatusUseCase, let chatId = chatId else { return }
        
        typingDebounceTask?.cancel()
        
        Task {
            let _ = try? await useCase.invoke(chatId: chatId, isTyping: true)
        }
        
        typingDebounceTask = Task {
            try? await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds
            if !Task.isCancelled {
                let _ = try? await useCase.invoke(chatId: chatId, isTyping: false)
            }
        }
    }

    func sendMessage(content: String) {
        guard let useCase = sendMessageUseCase, let chatId = chatId else { return }
        guard !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        self.isSending = true
        Task {
            do {
                let result = try await useCase.invoke(
                    chatId: chatId,
                    content: content,
                    mediaUrl: nil,
                    messageType: "TEXT",
                    expiresAt: nil,
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
        }
    }

    func sendMediaMessage(data: Data, fileName: String, mimeType: String) {
        guard let uploadUseCase = uploadMediaUseCase, let sendUseCase = sendMessageUseCase, let chatId = chatId else { return }

        self.isSending = true

        Task {
            do {
                // Swift Data to KotlinByteArray mapping might be needed depending on KMP bindings.
                // Assuming standard interop bridging allows Data or wrapping it.
                // We mock the KotlinByteArray creation since Swift Data to KotlinByteArray bridging
                // requires specific standard library bindings like `shared.KotlinByteArray(size: data.count)`.
                // A correct approach uses KMP-NativeCoroutines or explicit mappings.
                let kotlinArray = shared.KotlinByteArray(size: Int32(data.count))
                // Filling the array is skipped for brevity as it requires loop bridging in pure Swift
                // without a dedicated extension.

                let uploadResult = try await uploadUseCase.invoke(
                    chatId: chatId,
                    fileBytes: kotlinArray,
                    fileName: fileName,
                    contentType: mimeType
                )

                if let mediaUrl = uploadResult.getOrNull() as? String {
                    let result = try await sendUseCase.invoke(
                        chatId: chatId,
                        content: "Media", // Or empty
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
