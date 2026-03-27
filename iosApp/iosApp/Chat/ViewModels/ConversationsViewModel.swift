import Foundation
import shared

@MainActor
class ConversationsViewModel: ObservableObject {
    @Published var conversations: [SwiftConversation] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil

    // Using UseCase for Clean Architecture as defined in shared domain
    private let getConversationsUseCase: shared.GetConversationsUseCase?

    init(getConversationsUseCase: shared.GetConversationsUseCase? = KMPHelper.sharedHelper.getConversationsUseCase) {
        self.getConversationsUseCase = getConversationsUseCase
    }

    func fetchConversations() {
        guard let useCase = getConversationsUseCase else {
            self.errorMessage = "Use Case not initialized"
            return
        }

        self.isLoading = true
        self.errorMessage = nil

        Task {
            do {
                let result = try await useCase.invoke()

                // Kotlin's Result usually maps to an object in Swift where we have to check success/failure
                // If it's `kotlin.Result`, standard swift interop exposes methods to get value
                if let data = result.getOrNull() as? [shared.Conversation] {
                    self.conversations = data.map { SwiftConversation(from: $0) }
                } else if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message ?? "Failed to fetch conversations"
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }
            self.isLoading = false
        }
    }


    func archiveConversation(chatId: String) {
        // Optimistic update
        DispatchQueue.main.async {
            self.conversations.removeAll { $0.id == chatId }
        }

        Task {
            do {
                let repo = KMPHelper.sharedHelper.chatRepository
                let result = try await repo.updateConversationArchiveStatus(chatId: chatId, isArchived: true)
                if result.isFailure {
                    // Revert or show error
                    self.fetchConversations()
                }
            } catch {
                self.fetchConversations()
            }
        }
    }

    func deleteConversation(chatId: String) {
        DispatchQueue.main.async {
            self.conversations.removeAll { $0.id == chatId }
        }

        Task {
            do {
                let repo = KMPHelper.sharedHelper.chatRepository
                let result = try await repo.deleteConversation(chatId: chatId)
                if result.isFailure {
                    self.fetchConversations()
                }
            } catch {
                self.fetchConversations()
            }
        }
    }

}
