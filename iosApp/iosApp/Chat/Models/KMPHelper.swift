import Foundation
import shared

@MainActor
class KMPHelper {
    static let sharedHelper = KMPHelper()

    let chatRepository: shared.ChatRepository

    let getConversationsUseCase: shared.GetConversationsUseCase
    let getMessagesUseCase: shared.GetMessagesUseCase
    let subscribeToMessagesUseCase: shared.SubscribeToMessagesUseCase
    let sendMessageUseCase: shared.SendMessageUseCase
    let broadcastTypingStatusUseCase: shared.BroadcastTypingStatusUseCase
    let subscribeToTypingStatusUseCase: shared.SubscribeToTypingStatusUseCase
    let toggleMessageReactionUseCase: shared.ToggleMessageReactionUseCase
    let getMessageReactionsUseCase: shared.GetMessageReactionsUseCase
    let populateMessageReactionsUseCase: shared.PopulateMessageReactionsUseCase
    let subscribeToMessageReactionsUseCase: shared.SubscribeToMessageReactionsUseCase
    let setDisappearingModeUseCase: shared.SetDisappearingModeUseCase
    let getDisappearingModeUseCase: shared.GetDisappearingModeUseCase

    let uploadMediaUseCase: shared.UploadMediaUseCase

    let searchPostsUseCase: shared.SearchPostsUseCase

    let createStoryUseCase: shared.CreateStoryUseCase
    let getStoriesUseCase: shared.GetStoriesUseCase
    let markStoryAsSeenUseCase: shared.MarkStoryAsSeenUseCase
    let deleteStoryUseCase: shared.DeleteStoryUseCase

    let getUserProfileUseCase: shared.GetUserProfileUseCase

    let sharedImageLoader: shared.SharedImageLoader

    let getNotificationsUseCase: shared.GetNotificationsUseCase
    let markNotificationAsReadUseCase: shared.MarkNotificationAsReadUseCase
    let subscribeToNotificationsUseCase: shared.SubscribeToNotificationsUseCase
    let generateSmartRepliesUseCase: shared.GenerateSmartRepliesUseCase

    init() {
        let fileUploader = shared.FileUploader()
        let imgBBService = shared.ImgBBUploadService(httpClient: shared.SupabaseClient.shared.httpClient)
        let cloudinaryService = shared.CloudinaryUploadService()
        let supabaseService = shared.SupabaseUploadService(supabaseClient: shared.SupabaseClient.shared.client)
        let r2Service = shared.R2UploadService()
        let mediaUploadRepository = shared.MediaUploadRepositoryImpl(
            fileUploader: fileUploader,
            imgBBUploadService: imgBBService,
            cloudinaryUploadService: cloudinaryService,
            supabaseUploadService: supabaseService,
            r2UploadService: r2Service
        )

        self.chatRepository = shared.SupabaseChatRepository(
            dataSource: shared.SupabaseChatDataSource(),
            client: shared.SupabaseClient.shared.client,
            signalProtocolManager: nil,
            mediaUploadRepository: mediaUploadRepository,
            presenceRepository: nil,
            offlineActionRepository: nil,
            cachedMessageDao: nil,
            cachedConversationDao: nil,
            externalScope: nil
        )

        self.getConversationsUseCase = shared.GetConversationsUseCase(repository: chatRepository)
        self.getMessagesUseCase = shared.GetMessagesUseCase(repository: chatRepository)
        self.subscribeToMessagesUseCase = shared.SubscribeToMessagesUseCase(repository: chatRepository)
        self.sendMessageUseCase = shared.SendMessageUseCase(repository: chatRepository)
        self.broadcastTypingStatusUseCase = shared.BroadcastTypingStatusUseCase(repository: chatRepository)
        self.subscribeToTypingStatusUseCase = shared.SubscribeToTypingStatusUseCase(repository: chatRepository)
        self.toggleMessageReactionUseCase = shared.ToggleMessageReactionUseCase(repository: chatRepository)
        self.getMessageReactionsUseCase = shared.GetMessageReactionsUseCase(repository: chatRepository)
        self.populateMessageReactionsUseCase = shared.PopulateMessageReactionsUseCase(repository: chatRepository)
        self.subscribeToMessageReactionsUseCase = shared.SubscribeToMessageReactionsUseCase(repository: chatRepository)
        self.setDisappearingModeUseCase = shared.SetDisappearingModeUseCase(repository: chatRepository)
        self.getDisappearingModeUseCase = shared.GetDisappearingModeUseCase(repository: chatRepository)

        let storageRepository = shared.IOSDependencies.shared.getStorageRepository()
        self.uploadMediaUseCase = shared.UploadMediaUseCase(repository: chatRepository, storageRepository: storageRepository, mediaUploadRepository: mediaUploadRepository, fileUploader: fileUploader)

        self.searchPostsUseCase = shared.SearchPostsUseCase(repository: shared.SearchRepositoryImpl(client: shared.SupabaseClient.shared.client))

        let storyRepository = shared.SupabaseStoryRepository()
        self.createStoryUseCase = shared.CreateStoryUseCase(repository: storyRepository)
        self.getStoriesUseCase = shared.GetStoriesUseCase(repository: storyRepository)
        self.markStoryAsSeenUseCase = shared.MarkStoryAsSeenUseCase(repository: storyRepository)
        self.deleteStoryUseCase = shared.DeleteStoryUseCase(repository: storyRepository)

        let userRepository = shared.IOSDependencies.shared.getUserRepository()
        self.getUserProfileUseCase = shared.GetUserProfileUseCase(userRepository: userRepository)

        self.sharedImageLoader = shared.SharedImageLoader(httpClient: shared.Ktor_client_coreHttpClient())

        let notificationRepository = shared.SupabaseNotificationRepository(client: shared.SupabaseClient.shared.client)

        self.getNotificationsUseCase = shared.GetNotificationsUseCase(notificationRepository: notificationRepository, authRepository: shared.IOSDependencies.shared.getAuthRepository())
        self.markNotificationAsReadUseCase = shared.MarkNotificationAsReadUseCase(notificationRepository: notificationRepository, authRepository: shared.IOSDependencies.shared.getAuthRepository())
        self.subscribeToNotificationsUseCase = shared.SubscribeToNotificationsUseCase(notificationRepository: notificationRepository, authRepository: shared.IOSDependencies.shared.getAuthRepository())
        self.generateSmartRepliesUseCase = shared.IOSDependencies.shared.getGenerateSmartRepliesUseCase()
    }
}
