import Foundation
import shared

// Mock data model for iOS viewing
struct StoryItem: Identifiable {
    let id: String
    let mediaURL: URL
    let textOverlay: String?
    let type: StoryItemType
    let duration: Double
    let isOwnStory: Bool
    let viewsCount: Int
}

// Temporary mapping until shared module is fully integrated
enum StoryItemType {
    case image
    case video
}

@MainActor
class StoryViewerViewModel: ObservableObject {
    @Published var stories: [StoryItem] = []
    @Published var currentIndex: Int = 0 {
        didSet {
            resetProgress()
        }
    }
    @Published var isLoading: Bool = false
    @Published var error: String? = nil

    @Published var progress: Double = 0.0
    @Published var replyText: String = ""
    @Published var isMuted: Bool = false

    private var timer: Timer?
    private var isPaused: Bool = false
    private var currentStoryDuration: TimeInterval {
        guard !stories.isEmpty, currentIndex < stories.count else { return 5.0 }
        return stories[currentIndex].duration
    }

    init() {}

    deinit {
        timer?.invalidate()
    }

    func configure(with group: StoryGroup, isOwnStory: Bool) {
        self.stories = group.stories.compactMap { story in
            guard let urlString = story.getEffectiveMediaUrl(), let url = URL(string: urlString) else { return nil }
            let type: StoryItemType = story.mediaType == .video ? .video : .image
            let duration = Double(story.getDisplayDuration())
            return StoryItem(
                id: story.id ?? UUID().uuidString,
                mediaURL: url,
                textOverlay: story.content,
                type: type,
                duration: duration > 0 ? duration : 5.0,
                isOwnStory: isOwnStory,
                viewsCount: Int(story.viewCount ?? 0)
            )
        }
        self.currentIndex = 0
        if !self.stories.isEmpty {
            self.startProgress()
        } else {
            self.error = "No valid stories found in this group."
        }
    }

    var onFinished: (() -> Void)?

    func nextStory() {
        if currentIndex < stories.count - 1 {
            currentIndex += 1
        } else {
            onFinished?()
        }
    }

    func previousStory() {
        if currentIndex > 0 {
            currentIndex -= 1
        }
    }

    // MARK: - Playback controls

    private func startProgress() {
        timer?.invalidate()
        progress = 0.0

        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self, !self.isPaused else { return }

            DispatchQueue.main.async {
                self.progress += 0.1 / self.currentStoryDuration

                if self.progress >= 1.0 {
                    self.nextStory()
                }
            }
        }
    }

    private func resetProgress() {
        progress = 0.0
        startProgress()
    }

    func pause() {
        isPaused = true
    }

    func resume() {
        isPaused = false
    }

    func toggleMute() {
        isMuted.toggle()
    }
}
