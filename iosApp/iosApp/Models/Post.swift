import Foundation

struct PollOption: Codable, Identifiable {
    let id: Int
    let text: String
    var votes: Int
}

struct FeelingActivity: Codable {
    let emoji: String
    let text: String
    let type: String
}

struct PostMetadata: Codable {
    let layoutType: String?
    let taggedPeople: [User]?
    let feeling: FeelingActivity?
    let backgroundColor: Int64?

    enum CodingKeys: String, CodingKey {
        case layoutType = "layout_type"
        case taggedPeople = "tagged_people"
        case feeling
        case backgroundColor = "background_color"
    }
}

struct Post: Codable, Identifiable {
    let id: String
    let authorUid: String
    let postText: String?
    var postImage: String?
    var postType: String?

    let likesCount: Int
    let commentsCount: Int
    let viewsCount: Int
    let resharesCount: Int

    var mediaItems: [MediaItem]?

    let hasPoll: Bool?
    let pollQuestion: String?
    let pollOptions: [PollOption]?

    let createdAt: String?
    let updatedAt: String?

    // UI specific properties mapped manually or parsed
    var username: String?
    var displayName: String?
    var avatarUrl: String?
    var isVerified: Bool = false

    var inReplyToPostId: String?
    var rootPostId: String?

    var isBookmarked: Bool = false
    var isReshared: Bool = false

    var userReaction: ReactionType?
    var reactions: [String: Int]?

    enum CodingKeys: String, CodingKey {
        case id
        case authorUid = "author_uid"
        case postText = "post_text"
        case postImage = "post_image"
        case postType = "post_type"

        case likesCount = "likes_count"
        case commentsCount = "comments_count"
        case viewsCount = "views_count"
        case resharesCount = "reshares_count"

        case mediaItems = "media_items"

        case hasPoll = "has_poll"
        case pollQuestion = "poll_question"
        case pollOptions = "poll_options"

        case createdAt = "created_at"
        case updatedAt = "updated_at"

        case username = "author_username"
        case avatarUrl = "author_avatar_url"
        case isVerified = "author_is_verified"
        case inReplyToPostId = "reply_to_post_id"
        case rootPostId = "root_post_id"
    }
}

extension Post {
    static func mock(
        id: String = UUID().uuidString,
        authorUid: String = "mock_user",
        postText: String? = nil,
        username: String? = "mock_username",
        displayName: String? = "Mock User",
        createdAt: String? = "Just now",
        inReplyToPostId: String? = nil,
        rootPostId: String? = nil
    ) -> Post {
        return Post(
            id: id,
            authorUid: authorUid,
            postText: postText,
            postImage: nil,
            postType: "TEXT",
            likesCount: 0,
            commentsCount: 0,
            viewsCount: 0,
            resharesCount: 0,
            mediaItems: nil,
            hasPoll: false,
            pollQuestion: nil,
            pollOptions: nil,
            createdAt: createdAt,
            updatedAt: nil,
            username: username,
            displayName: displayName,
            avatarUrl: nil,
            isVerified: false,
            isBookmarked: false,
            isReshared: false,
            userReaction: nil,
            reactions: nil,
            inReplyToPostId: inReplyToPostId,
            rootPostId: rootPostId
        )
    }
}
