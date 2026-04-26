import Foundation
import Combine
import shared

struct HomeUiState {
    var posts: [Post] = []
    var isLoading: Bool = false
    var isRefreshing: Bool = false
    var errorMessage: String? = nil
}

@MainActor
class HomeViewModel: ObservableObject {
    private let analyticsService: AnalyticsService

    @Published var uiState = HomeUiState()

    private var hasInitialFetch = false

    private let searchPostsUseCase = KMPHelper.sharedHelper.searchPostsUseCase

    init(analyticsService: AnalyticsService) {
        self.analyticsService = analyticsService
    }

    func fetchHomeData() {
        guard !uiState.isLoading && !hasInitialFetch else { return }
        hasInitialFetch = true
        analyticsService.trackEvent("home_feed_loaded", parameters: nil)
        Task {
            await fetchFromUseCase(isRefresh: false)
        }
    }

    func refresh() {
        guard !uiState.isRefreshing else { return }
        Task {
            await fetchFromUseCase(isRefresh: true)
        }
    }

    private func fetchFromUseCase(isRefresh: Bool = false) async {
        if isRefresh {
            uiState.isRefreshing = true
        } else {
            uiState.isLoading = true
        }
        uiState.errorMessage = nil

        do {
            let result = try await searchPostsUseCase.invoke(query: "")
            guard let searchPosts = result.getOrNull() as? [shared.SearchPost] else {
                if let error = result.exceptionOrNull() {
                    uiState.errorMessage = error.message ?? "Unknown error"
                }
                if isRefresh { uiState.isRefreshing = false } else { uiState.isLoading = false }
                return
            }

            let mappedPosts = searchPosts.map { searchPost -> Post in
                var mediaItems: [MediaItem]? = nil
                if let urls = searchPost.mediaUrls, !urls.isEmpty {
                    mediaItems = urls.map { url in
                        MediaItem(id: UUID().uuidString, url: url, type: .image, thumbnailUrl: nil, width: nil, height: nil, sizeBytes: nil)
                    }
                }
                return Post(
                    id: searchPost.id,
                    authorUid: searchPost.authorId,
                    postText: searchPost.content,
                    postImage: nil,
                    postType: mediaItems != nil ? "IMAGE" : "TEXT",
                    likesCount: Int(searchPost.likesCount),
                    commentsCount: Int(searchPost.commentsCount),
                    viewsCount: 0,
                    resharesCount: Int(searchPost.boostCount),
                    mediaItems: mediaItems,
                    hasPoll: false,
                    pollQuestion: nil,
                    pollOptions: nil,
                    createdAt: searchPost.createdAt,
                    updatedAt: nil,
                    username: searchPost.authorHandle,
                    displayName: searchPost.authorName,
                    avatarUrl: searchPost.authorAvatar,
                    isVerified: false,
                    inReplyToPostId: nil,
                    rootPostId: nil,
                    isBookmarked: false,
                    isReshared: false,
                    userReaction: nil,
                    reactions: nil
                )
            }
            if isRefresh {
                uiState.posts = mappedPosts
            } else {
                // To avoid duplicate loads appending, we reset here or just assign since we don't have pagination params
                uiState.posts = mappedPosts
            }
        } catch {
            uiState.errorMessage = error.localizedDescription
        }

        if isRefresh {
            uiState.isRefreshing = false
        } else {
            uiState.isLoading = false
        }
    }

    func toggleLike(for post: Post) {
        if let index = uiState.posts.firstIndex(where: { $0.id == post.id }) {
            var updatedPost = uiState.posts[index]
            if updatedPost.userReaction == .like {
                updatedPost.userReaction = nil
            } else {
                updatedPost.userReaction = .like
            }
            uiState.posts[index] = updatedPost
        }
    }

    func toggleBookmark(for post: Post) {
        if let index = uiState.posts.firstIndex(where: { $0.id == post.id }) {
            var updatedPost = uiState.posts[index]
            updatedPost.isBookmarked.toggle()
            uiState.posts[index] = updatedPost
        }
    }
}
