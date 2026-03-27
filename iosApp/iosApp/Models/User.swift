import Foundation

struct User: Codable, Identifiable {
    let id: String
    let username: String
    let displayName: String?
    let avatarUrl: String?
    let isVerified: Bool

    enum CodingKeys: String, CodingKey {
        case id = "uid"
        case username
        case displayName = "display_name"
        case avatarUrl = "avatar"
        case isVerified = "verify"
    }
}
