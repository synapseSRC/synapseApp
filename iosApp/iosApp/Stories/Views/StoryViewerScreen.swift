import SwiftUI

struct StoryViewerScreen: View {
    @StateObject private var viewModel = StoryViewerViewModel()
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)

            if viewModel.isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
            } else if viewModel.stories.isEmpty {
                 Text("No stories available")
                    .foregroundColor(.white)
            } else {
                let currentStory = viewModel.stories[viewModel.currentIndex]

                // Story Content Display
                ZStack {
                    if currentStory.type == .image {
                        AsyncImage(url: currentStory.mediaURL) { image in
                            image
                                .resizable()
                                .scaledToFit()
                        } placeholder: {
                            Color.gray
                        }
                    } else {
                        // Implement Video Player logic similar to Creator if needed
                         Color.gray.overlay(Text("Video Unsupported Mock"))
                    }

                    if let text = currentStory.textOverlay {
                        VStack {
                            Spacer()
                            Text(text)
                                .font(.title)
                                .foregroundColor(.white)
                                .padding()
                                .background(Color.black.opacity(0.5))
                                .cornerRadius(10)
                                .padding(.bottom, 50)
                        }
                    }

                    // Tap Areas for Navigation
                    HStack(spacing: 0) {
                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture {
                                viewModel.previousStory()
                            }

                        Color.clear
                            .contentShape(Rectangle())
                            .onTapGesture {
                                viewModel.nextStory()
                            }
                    }
                }
                .edgesIgnoringSafeArea(.all)

                // Overlay Header
                VStack {
                    HStack {
                        // Progress bars would go here (requires a custom shape/animation)
                        ForEach(0..<viewModel.stories.count, id: \.self) { index in
                            Rectangle()
                                .fill(index <= viewModel.currentIndex ? Color.white : Color.white.opacity(0.3))
                                .frame(height: 3)
                                .cornerRadius(1.5)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 50) // Adjust for safe area

                    HStack {
                        Spacer()
                        Button(action: {
                            presentationMode.wrappedValue.dismiss()
                        }) {
                            Image(systemName: "xmark")
                                .font(.title2)
                                .foregroundColor(.white)
                                .padding()
                        }
                    }
                    Spacer()
                }
            }
        }
    }
}
