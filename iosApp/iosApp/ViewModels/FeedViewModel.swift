import Foundation
import Combine

class FeedViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var error: String?

    // Pagination placeholder
    private var currentPage = 0
    private var canLoadMore = true

    init() {
        fetchPosts()
    }

    func fetchPosts() {
        guard !isLoading else { return }
        isLoading = true

        // Mocking network delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.posts = self.generateMockPosts()
            self.isLoading = false
            self.currentPage += 1
        }
    }

    func refresh() {
        guard !isRefreshing else { return }
        isRefreshing = true

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            self.posts = self.generateMockPosts(isRefresh: true)
            self.isRefreshing = false
            self.currentPage = 1
            self.canLoadMore = true
        }
    }

    func loadMore() {
        guard !isLoading && canLoadMore else { return }
        isLoading = true

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            let morePosts = self.generateMockPosts()
            self.posts.append(contentsOf: morePosts)
            self.isLoading = false
            self.currentPage += 1
            if self.currentPage > 3 {
                self.canLoadMore = false // Mock limit
            }
        }
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
