import SwiftUI
import PhotosUI

struct PhotosPicker: UIViewControllerRepresentable {
    @Binding var selectedMedia: [URL]
    var maxSelectionCount: Int = 10
    var filter: PHPickerFilter = .any(of: [.images, .videos])

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.selectionLimit = maxSelectionCount
        configuration.filter = filter

        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: PhotosPicker

        init(_ parent: PhotosPicker) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)

            guard !results.isEmpty else { return }

            var newMediaURLs: [URL] = []
            let group = DispatchGroup()

            for result in results {
                group.enter()
                let provider = result.itemProvider

                if provider.hasItemConformingToTypeIdentifier(UTType.movie.identifier) {
                    provider.loadFileRepresentation(forTypeIdentifier: UTType.movie.identifier) { url, error in
                        if let url = url {
                            // Copy to temp directory so it isn't deleted when the closure returns
                            let uniqueString = UUID().uuidString
                            let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("\(uniqueString)_\(url.lastPathComponent)")
                            do {
                                try FileManager.default.copyItem(at: url, to: tempURL)
                                DispatchQueue.main.async {
                                    newMediaURLs.append(tempURL)
                                }
                            } catch {
                                print("Error copying movie: \(error)")
                            }
                        }
                        group.leave()
                    }
                } else if provider.canLoadObject(ofClass: UIImage.self) {
                    provider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { url, error in
                        if let url = url {
                            let uniqueString = UUID().uuidString
                            let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("\(uniqueString)_\(url.lastPathComponent)")
                            do {
                                try FileManager.default.copyItem(at: url, to: tempURL)
                                DispatchQueue.main.async {
                                    newMediaURLs.append(tempURL)
                                }
                            } catch {
                                print("Error copying image: \(error)")
                            }
                        }
                        group.leave()
                    }
                } else {
                    group.leave()
                }
            }

            group.notify(queue: .main) {
                self.parent.selectedMedia.append(contentsOf: newMediaURLs)
            }
        }
    }
}
