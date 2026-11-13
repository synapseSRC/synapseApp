import Foundation
import Combine

class PostDetailViewModel: ObservableObject {
    @Published var post: Post
    @Published var comments: [Post] = []
    @Published var isLoading = false
    @Published var isSubmittingComment = false
    @Published var commentText: String = ""
    @Published var showSummarySheet = false
    @Published var isSummarizing = false
    @Published var summary: String? = nil
    @Published var summaryError: String? = nil

    func summarizeThread() {
        self.showSummarySheet = true
        self.isSummarizing = true
        self.summaryError = nil

        // In a real app we'd call the shared KMP logic UseCase via Koin/manual injection
        // For now, mock summary simulation:
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            self.isSummarizing = false
            self.summary = "• User shared an update\n• Friends discussed the topic\n• Consensus was reached"
        }
    }

    init(postId: String) {
        self.post = Post(
            id: postId,
            authorUid: "",
            postText: nil,
            postImage: nil,
            postType: nil,
            likesCount: 0,
            commentsCount: 0,
            viewsCount: 0,
            resharesCount: 0,
            mediaItems: nil,
            hasPoll: false,
            pollQuestion: nil,
            pollOptions: nil,
            createdAt: nil,
            updatedAt: nil,
            username: nil,
            displayName: nil,
            avatarUrl: nil,
            isVerified: false,
            isBookmarked: false,
            isReshared: false,
            userReaction: nil,
            reactions: nil,
            inReplyToPostId: nil,
            rootPostId: nil
        )
        fetchComments()
    }

    func fetchComments() {
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.comments = [
                Post.mock(authorUid: "1", postText: "This is a great post!", username: "user1", displayName: "User One", createdAt: "1h ago", inReplyToPostId: postId, rootPostId: postId),
                Post.mock(authorUid: "2", postText: "I completely agree with you.", username: "user2", displayName: "User Two", createdAt: "30m ago", inReplyToPostId: postId, rootPostId: postId)
            ]
            self.isLoading = false
        }
    }

    func submitComment() {
        guard !commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        isSubmittingComment = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            let newComment = Post.mock(authorUid: "me", postText: self.commentText, username: "me_user", displayName: "Me", inReplyToPostId: postId, rootPostId: postId)

            self.comments.insert(newComment, at: 0)
            self.commentText = ""
            self.isSubmittingComment = false

            // Increment post comments count optimistically
            self.post = Post(
                id: postId,
                authorUid: self.post.authorUid,
                postText: self.post.postText,
                postImage: self.post.postImage,
                postType: self.post.postType,
                likesCount: self.post.likesCount,
                commentsCount: self.post.commentsCount + 1,
                viewsCount: self.post.viewsCount,
                resharesCount: self.post.resharesCount,
                mediaItems: self.post.mediaItems,
                hasPoll: self.post.hasPoll,
                pollQuestion: self.post.pollQuestion,
                pollOptions: self.post.pollOptions,
                createdAt: self.post.createdAt,
                updatedAt: self.post.updatedAt,
                username: self.post.username,
                displayName: self.post.displayName,
                avatarUrl: self.post.avatarUrl,
                isVerified: self.post.isVerified,
                isBookmarked: self.post.isBookmarked,
                isReshared: self.post.isReshared,
                userReaction: self.post.userReaction,
                reactions: self.post.reactions,
                inReplyToPostId: self.post.inReplyToPostId,
                rootPostId: self.post.rootPostId
            )
        }
    }

    func toggleLike() {
        self.post = Post(
            id: postId,
            authorUid: self.post.authorUid,
            postText: self.post.postText,
            postImage: self.post.postImage,
            postType: self.post.postType,
            likesCount: self.post.userReaction == .like ? self.post.likesCount - 1 : self.post.likesCount + 1,
            commentsCount: self.post.commentsCount,
            viewsCount: self.post.viewsCount,
            resharesCount: self.post.resharesCount,
            mediaItems: self.post.mediaItems,
            hasPoll: self.post.hasPoll,
            pollQuestion: self.post.pollQuestion,
            pollOptions: self.post.pollOptions,
            createdAt: self.post.createdAt,
            updatedAt: self.post.updatedAt,
            username: self.post.username,
            displayName: self.post.displayName,
            avatarUrl: self.post.avatarUrl,
            isVerified: self.post.isVerified,
            isBookmarked: self.post.isBookmarked,
            isReshared: self.post.isReshared,
            userReaction: self.post.userReaction == .like ? nil : .like,
            reactions: self.post.reactions,
            inReplyToPostId: self.post.inReplyToPostId,
            rootPostId: self.post.rootPostId
        )
    }

    func toggleBookmark() {
        self.post = Post(
            id: postId,
            authorUid: self.post.authorUid,
            postText: self.post.postText,
            postImage: self.post.postImage,
            postType: self.post.postType,
            likesCount: self.post.likesCount,
            commentsCount: self.post.commentsCount,
            viewsCount: self.post.viewsCount,
            resharesCount: self.post.resharesCount,
            mediaItems: self.post.mediaItems,
            hasPoll: self.post.hasPoll,
            pollQuestion: self.post.pollQuestion,
            pollOptions: self.post.pollOptions,
            createdAt: self.post.createdAt,
            updatedAt: self.post.updatedAt,
            username: self.post.username,
            displayName: self.post.displayName,
            avatarUrl: self.post.avatarUrl,
            isVerified: self.post.isVerified,
            isBookmarked: !self.post.isBookmarked,
            isReshared: self.post.isReshared,
            userReaction: self.post.userReaction,
            reactions: self.post.reactions,
            inReplyToPostId: self.post.inReplyToPostId,
            rootPostId: self.post.rootPostId
        )
    }

    func toggleShare() {
        // Share action is handled by presenting a share sheet in the view.
        // This method exists to wire the share button to a no-op placeholder
        // that can be replaced with a real share implementation.
    }
}
