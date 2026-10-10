import SwiftUI
import UIKit
import CoreImage

class UiAtmosphereState: ObservableObject {
    @Published var dominantColor: Color = .clear
    @Published var vibrantColor: Color = .clear
    @Published var mutedColor: Color = .clear

    static let shared = UiAtmosphereState()

    private init() {}

    func updateColors(dominant: Color, vibrant: Color, muted: Color) {
        self.dominantColor = dominant
        self.vibrantColor = vibrant
        self.mutedColor = muted
    }

    func reset() {
        self.dominantColor = .clear
        self.vibrantColor = .clear
        self.mutedColor = .clear
    }
}

extension UIImage {
    func extractColors() -> (dominant: Color, vibrant: Color, muted: Color) {
        guard let inputImage = CIImage(image: self) else {
            return (.clear, .clear, .clear)
        }

        // 1. Dominant color (Average)
        let extentVector = CIVector(x: inputImage.extent.origin.x, y: inputImage.extent.origin.y, z: inputImage.extent.size.width, w: inputImage.extent.size.height)
        guard let filter = CIFilter(name: "CIAreaAverage", parameters: [kCIInputImageKey: inputImage, kCIInputExtentKey: extentVector]),
              let outputImage = filter.outputImage else {
            return (.clear, .clear, .clear)
        }

        var bitmap = [UInt8](repeating: 0, count: 4)
        let context = CIContext(options: [.workingColorSpace: kCFNull as Any])
        context.render(outputImage, toBitmap: &bitmap, rowBytes: 4, bounds: CGRect(x: 0, y: 0, width: 1, height: 1), format: .RGBA8, colorSpace: nil)

        let dominantColor = Color(red: Double(bitmap[0]) / 255.0, green: Double(bitmap[1]) / 255.0, blue: Double(bitmap[2]) / 255.0)

        // 2. Simple Heuristic for "Vibrant" (Increase saturation/brightness of dominant if possible)
        // For a more robust solution on iOS without external libs, we would sample multiple areas.
        // For now, we'll return dominant for both to ensure a clean effect.

        return (dominantColor, dominantColor, dominantColor.opacity(0.8))
    }
}
