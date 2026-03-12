import Foundation
import shared

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [SwiftMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var isSending = false

    // Using UseCases to interface with KMP Shared layer
    private let getMessagesUseCase: shared.GetMessagesUseCase?
    private let subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase?
    private let sendMessageUseCase: shared.SendMessageUseCase?
    private let uploadMediaUseCase: shared.UploadMediaUseCase?

    private var chatId: String? = nil
    private var subscriptionTask: Task<Void, Never>? = nil

    init(
        getMessagesUseCase: shared.GetMessagesUseCase? = KMPHelper.sharedHelper.getMessagesUseCase,
        subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase? = KMPHelper.sharedHelper.subscribeToMessagesUseCase,
        sendMessageUseCase: shared.SendMessageUseCase? = KMPHelper.sharedHelper.sendMessageUseCase,
        uploadMediaUseCase: shared.UploadMediaUseCase? = KMPHelper.sharedHelper.uploadMediaUseCase
    ) {
        self.getMessagesUseCase = getMessagesUseCase
        self.subscribeToMessagesUseCase = subscribeToMessagesUseCase
        self.sendMessageUseCase = sendMessageUseCase
        self.uploadMediaUseCase = uploadMediaUseCase
    }

    func setup(chatId: String) {
        self.chatId = chatId
        fetchMessages()
        subscribeToMessages()
    }

    deinit {
        subscriptionTask?.cancel()
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

        subscriptionTask?.cancel()
        subscriptionTask = Task {
            let flow = useCase.invoke(chatId: chatId)
            do {
                for try await message in flow.asAsyncStream(type: shared.Message.self) {
                    let swiftMsg = SwiftMessage(from: message)
                    if let index = self.messages.firstIndex(where: { $0.id == swiftMsg.id }) {
                        self.messages[index] = swiftMsg
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
                for (index, byte) in data.enumerated() {
                    kotlinArray.set(index: Int32(index), value: Int8(bitPattern: byte))
                }
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
