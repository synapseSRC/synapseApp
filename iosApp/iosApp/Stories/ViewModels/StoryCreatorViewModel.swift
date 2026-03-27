import Foundation
import shared
import UIKit
import AVFoundation

class StoryCreatorViewModel: ObservableObject {
    @Published var mediaURL: URL? = nil
    @Published var textOverlay: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var isStoryPosted: Bool = false

    // Caching media resources so the view doesn't recreate them every render cycle
    @Published var cachedImage: UIImage? = nil
    var cachedPlayer: AVPlayer? = nil

    func setMedia(url: URL) {
        self.mediaURL = url

        let pathExtension = url.pathExtension.lowercased()
        if pathExtension == "mov" || pathExtension == "mp4" {
            cachedPlayer = AVPlayer(url: url)
            cachedImage = nil
        } else {
            // Downsample image or at least cache it directly to avoid disk I/O on redraws
            cachedImage = UIImage(contentsOfFile: url.path)
            cachedPlayer = nil
        }
    }

    func clearMedia() {
        if let url = mediaURL, url.path.contains(FileManager.default.temporaryDirectory.path) {
            do {
                try FileManager.default.removeItem(at: url)
            } catch {
                print("Failed to clean up story media file: \(error)")
            }
        }
        self.mediaURL = nil
        self.textOverlay = ""
        self.cachedImage = nil
        self.cachedPlayer = nil
    }

    func submitStory() {
        guard let _ = mediaURL else {
            self.error = "Media is required for a story."
            return
        }

        self.isLoading = true
        self.error = nil

        // Simulate interaction with KMP use case for story submission
        // e.g., submitStoryUseCase.execute(media: mediaURL, text: textOverlay)

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            self.isLoading = false
            self.isStoryPosted = true
            self.clearMedia()
        }
    }

    deinit {
        clearMedia()
    }
}
