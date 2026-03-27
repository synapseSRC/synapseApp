import Foundation
import shared

// Mock data model for iOS viewing
struct StoryItem: Identifiable {
    let id = UUID()
    let mediaURL: URL
    let textOverlay: String?
    let type: MediaType // Requires shared.MediaType or mapped
}

// Temporary mapping until shared module is fully integrated
enum MediaType {
    case image
    case video
}

class StoryViewerViewModel: ObservableObject {
    @Published var stories: [StoryItem] = []
    @Published var currentIndex: Int = 0
    @Published var isLoading: Bool = true

    // Simulating fetching stories from KMP use case
    // let getStoriesUseCase: GetStoriesUseCase

    init() {
        fetchStories()
    }

    func fetchStories() {
        self.isLoading = true

        // Simulating network delay and response
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            // Mock Data
            if let sampleImageURL = URL(string: "https://via.placeholder.com/800x1200.png?text=Sample+Story+Image") {
                 self.stories = [
                    StoryItem(mediaURL: sampleImageURL, textOverlay: "Hello World!", type: .image),
                    StoryItem(mediaURL: sampleImageURL, textOverlay: "Next Story", type: .image)
                 ]
            }
            self.isLoading = false
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
