import Foundation

enum ReactionType: String, Codable, CaseIterable {
    case like = "LIKE"
    case love = "LOVE"
    case haha = "HAHA"
    case wow = "WOW"
    case sad = "SAD"
    case angry = "ANGRY"
    case unreact = "UNREACT"

    var emoji: String {
        switch self {
        case .like: return "👍"
        case .love: return "❤️"
        case .haha: return "😂"
        case .wow: return "😮"
        case .sad: return "😢"
        case .angry: return "😡"
        case .unreact: return ""
        }
    }
}
