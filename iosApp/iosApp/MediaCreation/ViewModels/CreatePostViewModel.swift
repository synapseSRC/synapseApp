import Foundation
import shared

class CreatePostViewModel: ObservableObject {
    @Published var text: String = ""
    @Published var mediaURLs: [URL] = []
    @Published var privacy: String = "public"
    @Published var isLoading: Bool = false
    @Published var error: String? = nil
    @Published var isPostCreated: Bool = false
    @Published var uploadProgress: Float = 0.0

    // Represents KMP UseCase integrations
    // For a fully built app, inject these via an iOS DI container (like Swinject or a custom Factory)
    // private let uploadMediaUseCase: UploadMediaUseCase
    // private let postRepository: PostRepository

    // In a real environment:
    // init(uploadMediaUseCase: UploadMediaUseCase = Resolver.resolve(), postRepository: PostRepository = Resolver.resolve()) { ... }

    init() {
        // Dependencies would be injected here
    }

    func addMedia(_ urls: [URL]) {
        self.mediaURLs.append(contentsOf: urls)
    }

    func removeMedia(at index: Int) {
        guard index >= 0 && index < mediaURLs.count else { return }
        let url = mediaURLs.remove(at: index)
        cleanupFile(at: url)
    }

    func cleanupAllMedia() {
        for url in mediaURLs {
            cleanupFile(at: url)
        }
        mediaURLs.removeAll()
    }

    private func cleanupFile(at url: URL) {
        // Only clean up files residing in the temporary directory
        if url.path.contains(FileManager.default.temporaryDirectory.path) {
            do {
                try FileManager.default.removeItem(at: url)
            } catch {
                print("Failed to clean up temp file: \(error)")
            }
        }
    }

    func submitPost() {
        guard !text.isEmpty || !mediaURLs.isEmpty else {
            self.error = "Post cannot be empty."
            return
        }

        self.isLoading = true
        self.error = nil
        self.uploadProgress = 0.1

        // KMP Integration Point
        // 1. Map Swift URLs to String paths
        // 2. Call `uploadMediaUseCase.invoke(filePath: url.path, ...)`
        // 3. Collect mapped media URLs
        // 4. Construct `shared.Post` object
        // 5. Call `postRepository.createPost(post: post)`

        // Simulating KMP upload and response
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.uploadProgress = 0.5
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            self.uploadProgress = 1.0
            self.isLoading = false
            self.isPostCreated = true
            self.text = ""
            self.cleanupAllMedia()
        }
    }

    deinit {
        cleanupAllMedia()
    }
}
