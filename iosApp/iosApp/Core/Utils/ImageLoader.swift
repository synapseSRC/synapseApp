import Foundation
import UIKit

class ImageLoader: ObservableObject {
    @Published var image: UIImage?
    private var cache = ImageCache.shared
    private var currentUrlString: String?

    init() {}

    func load(urlString: String) {
        self.currentUrlString = urlString

        if let cachedImage = cache.get(forKey: urlString) {
            self.image = cachedImage
            return
        }

        self.image = nil

        Task {
            do {
                let sharedLoader = shared.SharedImageLoader()
                let byteArray = try await sharedLoader.loadImageBytes(url: urlString)

                // Convert KotlinByteArray to Data
                var swiftArray = [Int8](repeating: 0, count: Int(byteArray.size))
                for i in 0..<byteArray.size {
                    swiftArray[Int(i)] = byteArray.get(index: i)
                }
                let data = Data(bytes: swiftArray, count: Int(byteArray.size))

                if let newImage = UIImage(data: data) {
                    self.cache.set(newImage, forKey: urlString)

                    await MainActor.run {
                        if self.currentUrlString == urlString {
                            self.image = newImage
                        }
                    }
                }
            } catch {
                print("Failed to load image via shared logic: \(error)")
            }
        }
    }
}

class ImageCache {
    static let shared = ImageCache()
    private var cache = NSCache<NSString, UIImage>()

    func get(forKey key: String) -> UIImage? {
        return cache.object(forKey: NSString(string: key))
    }

    func set(_ image: UIImage, forKey key: String) {
        cache.setObject(image, forKey: NSString(string: key))
    }
}
