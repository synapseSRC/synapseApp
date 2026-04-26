import SwiftUI

struct CachedImageView: View {
    @StateObject private var loader = ImageLoader()
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
}
