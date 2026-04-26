import Foundation
import shared
import UIKit
import AVFoundation

@MainActor
class StoryCreatorViewModel: ObservableObject {
    @Published var mediaURL: URL? = nil
    @Published var textOverlay: String = ""
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var isStoryPosted: Bool = false

    // Caching media resources so the view doesn't recreate them every render cycle
    @Published var cachedImage: UIImage? = nil
    var cachedPlayer: AVPlayer? = nil

    private let createStoryUseCase = KMPHelper.sharedHelper.createStoryUseCase
    private let uploadMediaUseCase = KMPHelper.sharedHelper.uploadMediaUseCase

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
        guard let url = mediaURL else {
            self.error = "Media is required for a story."
            return
        }

        self.isLoading = true
        self.error = nil

        let isVideo = url.pathExtension.lowercased() == "mov" || url.pathExtension.lowercased() == "mp4"
        let mediaType = isVideo ? shared.MediaType.video : shared.MediaType.photo
        let mediaTypeString = isVideo ? "video" : "photo"

        Task {
            do {
                // 1. Upload Media
                let result = try await uploadMediaUseCase.invoke(
                    filePath: url.path,
                    mediaType: mediaType,
                    bucketName: "stories",
                    onProgress: { _ in }
                )

                // Kotlin's Result automatically unboxes in Swift
                let uploadedUrl = result as! String

                // 2. Save Story Record
                try await createStoryUseCase.invoke(
                    mediaUrl: uploadedUrl,
                    mediaType: mediaTypeString,
                    textOverlay: self.textOverlay.isEmpty ? nil : self.textOverlay
                )

                self.isLoading = false
                self.isStoryPosted = true
                self.clearMedia()

            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    deinit {
        clearMedia()
    }
}
