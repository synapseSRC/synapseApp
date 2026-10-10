import SwiftUI
import shared

struct StoryViewerScreen: View {
    @Environment(\.presentationMode) var presentationMode

    let storyGroups: [StoryGroup]
    @State private var selectedGroupIndex: Int
    let isOwnStory: Bool

    init(storyGroups: [StoryGroup], initialIndex: Int, isOwnStory: Bool = false) {
        self.storyGroups = storyGroups
        self._selectedGroupIndex = State(initialValue: initialIndex)
        self.isOwnStory = isOwnStory
    }

    var body: some View {
        GeometryReader { outerGeo in
            TabView(selection: $selectedGroupIndex) {
                ForEach(0..<storyGroups.count, id: \.self) { index in
                    GroupView(
                        group: storyGroups[index],
                        isOwnStory: isOwnStory && index == 0,
                        onClose: {
                            presentationMode.wrappedValue.dismiss()
                        },
                        onFinished: {
                            if selectedGroupIndex < storyGroups.count - 1 {
                                withAnimation {
                                    selectedGroupIndex += 1
                                }
                            } else {
                                presentationMode.wrappedValue.dismiss()
                            }
                        }
                    )
                    .tag(index)
                    .visualEffect { content, proxy in
                        content
                            .rotation3DEffect(
                                .degrees(getRotation(proxy: proxy, screenWidth: outerGeo.size.width)),
                                axis: (x: 0, y: 1, z: 0),
                                anchor: getAnchor(proxy: proxy),
                                perspective: 1.0
                            )
                            .overlay(
                                Color.black.opacity(getOpacity(proxy: proxy, screenWidth: outerGeo.size.width))
                            )
                    }
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            .background(Color.black)
            .edgesIgnoringSafeArea(.all)
        }
    }

    private func getRotation(proxy: GeometryProxy, screenWidth: CGFloat) -> Double {
        let scrollOffset = proxy.frame(in: .global).minX
        let progress = scrollOffset / screenWidth
        return Double(-progress * 90)
    }

    private func getAnchor(proxy: GeometryProxy) -> UnitPoint {
        let scrollOffset = proxy.frame(in: .global).minX
        if scrollOffset > 0 {
            return .leading
        } else if scrollOffset < 0 {
            return .trailing
        } else {
            return .center
        }
    }

    private func getOpacity(proxy: GeometryProxy, screenWidth: CGFloat) -> Double {
        let scrollOffset = proxy.frame(in: .global).minX
        let progress = abs(scrollOffset / screenWidth)
        return Double(min(progress * 0.7, 0.7))
    }
}

private struct GroupView: View {
    @StateObject private var viewModel = StoryViewerViewModel()
    let group: StoryGroup
    let isOwnStory: Bool
    let onClose: () -> Void
    let onFinished: () -> Void

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
                    GeometryReader { geometry in
                        HStack(spacing: 0) {
                            Color.clear
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    viewModel.previousStory()
                                }
                                .frame(width: geometry.size.width * 0.3)

                            Color.clear
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    viewModel.nextStory()
                                }
                                .onLongPressGesture(minimumDuration: .infinity, pressing: { isPressing in
                                    if isPressing {
                                        viewModel.pause()
                                    } else {
                                        viewModel.resume()
                                    }
                                }, perform: {})
                                .frame(width: geometry.size.width * 0.7)
                        }
                    }
                }
                .edgesIgnoringSafeArea(.all)

                // Overlay Header
                VStack {
                    HStack(spacing: 4) {
                        ForEach(0..<viewModel.stories.count, id: \.self) { index in
                            GeometryReader { geo in
                                Rectangle()
                                    .fill(Color.white.opacity(0.3))
                                    .frame(height: 3)
                                    .cornerRadius(1.5)
                                    .overlay(
                                        Rectangle()
                                            .fill(Color.white)
                                            .frame(width: index < viewModel.currentIndex ? geo.size.width : (index == viewModel.currentIndex ? geo.size.width * CGFloat(viewModel.progress) : 0), height: 3)
                                            .cornerRadius(1.5),
                                        alignment: .leading
                                    )
                            }
                            .frame(height: 3)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 50)

                    HStack {
                        Spacer()
                        Button(action: {
                            onClose()
                        }) {
                            Image(systemName: "xmark")
                                .font(.title2)
                                .foregroundColor(.white)
                                .padding()
                        }
                    }
                    Spacer()

                    // Bottom Controls Overlay
                    HStack {
                        if currentStory.isOwnStory {
                            HStack {
                                Image(systemName: "eye.fill")
                                Text("\(currentStory.viewsCount)")
                            }
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.black.opacity(0.4))
                            .cornerRadius(20)
                            .padding(.horizontal)
                            .padding(.bottom, 20)

                            Spacer()
                        } else {
                            TextField("Reply...", text: $viewModel.replyText)
                                .padding()
                                .background(Color.black.opacity(0.4))
                                .cornerRadius(20)
                                .foregroundColor(.white)
                                .padding(.horizontal)
                                .padding(.bottom, 20)
                        }

                        if currentStory.type == .video {
                            Button(action: {
                                viewModel.toggleMute()
                            }) {
                                Image(systemName: viewModel.isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                                    .foregroundColor(.white)
                                    .padding()
                            }
                        }
                    }
                }
            }
        }
        .onAppear {
            viewModel.onFinished = onFinished
            viewModel.configure(with: group, isOwnStory: isOwnStory)
        }
    }
}
