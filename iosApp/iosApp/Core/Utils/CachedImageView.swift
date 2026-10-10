import SwiftUI

struct CachedImageView: View {
    @StateObject private var loader = ImageLoader()
    @StateObject private var atmosphere = UiAtmosphereState.shared
    let urlString: String

    init(urlString: String) {
        self.urlString = urlString
    }

    var body: some View {
        Group {
            if let uiImage = loader.image {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .onAppear {
                        updateAtmosphere(with: uiImage)
                    }
                    .onChange(of: uiImage) { newImage in
                        updateAtmosphere(with: newImage)
                    }
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .overlay(ProgressView())
            }
        }
        .onAppear {
            loader.load(urlString: urlString)
        }
        .onChange(of: urlString) { newUrl in
            loader.load(urlString: newUrl)
        }
    }

    private func updateAtmosphere(with uiImage: UIImage) {
        let colors = uiImage.extractColors()
        withAnimation(.easeInOut(duration: 2.0)) {
            atmosphere.updateColors(dominant: colors.dominant, vibrant: colors.vibrant, muted: colors.muted)
        }
    }
}
