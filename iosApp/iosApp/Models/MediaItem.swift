import Foundation

enum MediaType: String, Codable {
    case image = "IMAGE"
    case video = "VIDEO"
    case document = "DOCUMENT"
    case gif = "GIF"
}

struct MediaItem: Codable, Identifiable {
    let id: String
    let url: String
    let type: MediaType
    let thumbnailUrl: String?
    let width: Int?
    let height: Int?
    let sizeBytes: Int64?

    enum CodingKeys: String, CodingKey {
        case id
        case url
        case type
        case thumbnailUrl = "thumbnail_url"
        case width
        case height
        case sizeBytes = "size_bytes"
    }
}
