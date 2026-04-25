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

    let uploadMediaUseCase: shared.UploadMediaUseCase

    let searchPostsUseCase: shared.SearchPostsUseCase

    let sharedImageLoader: shared.SharedImageLoader

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

        let storageRepository = shared.IOSDependencies.shared.getStorageRepository()
        self.uploadMediaUseCase = shared.UploadMediaUseCase(repository: chatRepository, storageRepository: storageRepository, mediaUploadRepository: mediaUploadRepository, fileUploader: fileUploader)

        self.searchPostsUseCase = shared.SearchPostsUseCase(repository: shared.SearchRepositoryImpl(client: shared.SupabaseClient.shared.client))

        self.sharedImageLoader = shared.SharedImageLoader(httpClient: shared.Ktor_client_coreHttpClient())
    }
}
