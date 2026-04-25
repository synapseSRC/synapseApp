import Foundation
import Combine
import shared

class FeedViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var error: String?

    // Pagination placeholder
    private var currentPage = 0
    private var canLoadMore = true

    private let searchPostsUseCase = KMPHelper.sharedHelper.searchPostsUseCase

    init() {
        fetchPosts()
    }

    @MainActor
    private func fetchFromUseCase(isRefresh: Bool = false) async {
        if isRefresh {
            self.isRefreshing = true
        } else {
            self.isLoading = true
        }
        self.error = nil

        do {
            let searchPosts = try await searchPostsUseCase.invoke(query: "")
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
                self.posts = mappedPosts
            } else {
                self.posts.append(contentsOf: mappedPosts)
            }
            self.currentPage += 1
            self.canLoadMore = mappedPosts.count >= 20
        } catch {
            self.error = error.localizedDescription
        }

        if isRefresh {
            self.isRefreshing = false
        } else {
            self.isLoading = false
        }
    }

    func fetchPosts() {
        guard !isLoading else { return }
        Task {
            await fetchFromUseCase(isRefresh: false)
        }
    }

    func refresh() {
        guard !isRefreshing else { return }
        currentPage = 0
        Task {
            await fetchFromUseCase(isRefresh: true)
        }
    }

    func loadMore() {
        guard !isLoading && canLoadMore else { return }
        // Pagination logic not fully implemented in SearchRepositoryImpl for infinite scrolling yet.
        // We'll leave it as a no-op or trigger the same call for now.
    }

    func toggleLike(for post: Post) {
        if let index = posts.firstIndex(where: { $0.id == post.id }) {
            var updatedPost = posts[index]
            if updatedPost.userReaction == .like {
                updatedPost.userReaction = nil
            } else {
                updatedPost.userReaction = .like
            }
            posts[index] = updatedPost
        }
    }

    func toggleBookmark(for post: Post) {
        if let index = posts.firstIndex(where: { $0.id == post.id }) {
            var updatedPost = posts[index]
            updatedPost.isBookmarked.toggle()
            posts[index] = updatedPost
        }
    }

    private func generateMockPosts(isRefresh: Bool = false) -> [Post] {
        let prefix = isRefresh ? "Refreshed" : "Post"
        return [
            Post(id: UUID().uuidString, authorUid: "1", postText: "\(prefix) 1: Just setting up my synsape. Loving the new KMP + SwiftUI integration!", likesCount: 15, commentsCount: 3, viewsCount: 150, resharesCount: 2, hasPoll: false, pollQuestion: nil, pollOptions: nil, createdAt: "2h ago", updatedAt: nil, username: "johndoe", displayName: "John Doe", isVerified: true),

            Post(id: UUID().uuidString, authorUid: "2", postText: "\(prefix) 2: Check out this beautiful view!", postType: "IMAGE", likesCount: 42, commentsCount: 12, viewsCount: 500, resharesCount: 5, mediaItems: [MediaItem(id: "m1", url: "https://images.unsplash.com/photo-1506744626753-dba37c1a84f6?w=600&h=400&fit=crop", type: .image, thumbnailUrl: nil, width: 600, height: 400, sizeBytes: nil)], hasPoll: false, pollQuestion: nil, pollOptions: nil, createdAt: "5h ago", updatedAt: nil, username: "janedoe", displayName: "Jane Doe", isVerified: false),

            Post(id: UUID().uuidString, authorUid: "3", postText: "\(prefix) 3: What is your favorite programming language?", postType: "POLL", likesCount: 8, commentsCount: 20, viewsCount: 300, resharesCount: 1, hasPoll: true, pollQuestion: "What is your favorite programming language?", pollOptions: [PollOption(id: 1, text: "Kotlin", votes: 45), PollOption(id: 2, text: "Swift", votes: 35), PollOption(id: 3, text: "TypeScript", votes: 20)], createdAt: "1d ago", updatedAt: nil, username: "devguru", displayName: "Dev Guru", isVerified: true)
        ]
    }
}
