import Foundation
import shared

// Mock data model for iOS viewing
struct StoryItem: Identifiable {
    let id: String
    let mediaURL: URL
    let textOverlay: String?
    let type: StoryItemType
}

// Temporary mapping until shared module is fully integrated
enum StoryItemType {
    case image
    case video
}

@MainActor
class StoryViewerViewModel: ObservableObject {
    @Published var stories: [StoryItem] = []
    @Published var currentIndex: Int = 0
    @Published var isLoading: Bool = true
    @Published var error: String? = nil

    private let getStoriesUseCase = KMPHelper.sharedHelper.getStoriesUseCase

    init() {
        fetchStories()
    }

    func fetchStories() {
        self.isLoading = true
        self.error = nil

        Task {
            do {
                let domainStories = try await getStoriesUseCase.invoke()

                self.stories = domainStories.compactMap { story in
                    guard let urlString = story.getEffectiveMediaUrl(), let url = URL(string: urlString) else { return nil }
                    let type: StoryItemType = story.mediaType == .video ? .video : .image
                    return StoryItem(
                        id: story.id ?? UUID().uuidString,
                        mediaURL: url,
                        textOverlay: story.content,
                        type: type
                    )
                }
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func nextStory() {
        if currentIndex < stories.count - 1 {
            currentIndex += 1
        } else {
            // Reached the end, close viewer
        }
    }

    func previousStory() {
        if currentIndex > 0 {
            currentIndex -= 1
        }
    }
}
