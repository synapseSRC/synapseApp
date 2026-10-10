import SwiftUI
import AVKit

struct DrawingLine: Identifiable {
    let id = UUID()
    var points: [CGPoint]
    var color: Color
    var lineWidth: CGFloat
}

struct StoryCreatorScreen: View {
    @StateObject private var viewModel = StoryCreatorViewModel()
    @Environment(\.presentationMode) var presentationMode
    @State private var isShowingCamera = false
    @State private var isShowingPicker = false
    @State private var selectedMediaURLs: [URL] = []

    // Editing State
    @State private var isDrawingMode = false
    @State private var lines: [DrawingLine] = []
    @State private var currentLine = DrawingLine(points: [], color: .red, lineWidth: 5.0)

    // Stickers
    @State private var stickers: [Sticker] = []
    @State private var selectedSticker: String? = nil

    // Text Position
    @State private var textPosition: CGPoint? = nil

    let emojiOptions = ["😀", "❤️", "🔥", "✨", "🎉", "💯"]

    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)

            if viewModel.mediaURL != nil {
                // Editing Mode
                GeometryReader { geometry in
                    ZStack {
                        // Base Media - uses cached instances to prevent disk I/O / player re-init on gestures
                        if let player = viewModel.cachedPlayer {
                            VideoPlayer(player: player)
                                .edgesIgnoringSafeArea(.all)
                                .allowsHitTesting(!isDrawingMode) // Disable touches if drawing
                        } else if let image = viewModel.cachedImage {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFit()
                        }

                        // Drawing Canvas
                        if isDrawingMode || !lines.isEmpty {
                            Canvas { context, size in
                                for line in lines {
                                    var path = Path()
                                    path.addLines(line.points)
                                    context.stroke(path, with: .color(line.color), lineWidth: line.lineWidth)
                                }
                                var path = Path()
                                path.addLines(currentLine.points)
                                context.stroke(path, with: .color(currentLine.color), lineWidth: currentLine.lineWidth)
                            }
                            .gesture(
                                DragGesture(minimumDistance: 0.1)
                                    .onChanged { value in
                                        if isDrawingMode {
                                            let newPoint = value.location
                                            currentLine.points.append(newPoint)
                                        }
                                    }
                                    .onEnded { _ in
                                        if isDrawingMode {
                                            lines.append(currentLine)
                                            currentLine = DrawingLine(points: [], color: currentLine.color, lineWidth: currentLine.lineWidth)
                                        }
                                    }
                            )
                            .allowsHitTesting(isDrawingMode)
                        }

                        // Stickers Layer
                        ForEach(stickers) { sticker in
                            Text(sticker.text)
                                .font(.system(size: sticker.scale * 50))
                                .position(sticker.position)
                                .gesture(
                                    DragGesture()
                                        .onChanged { value in
                                            if !isDrawingMode {
                                                if let index = stickers.firstIndex(where: { $0.id == sticker.id }) {
                                                    stickers[index].position = value.location
                                                }
                                            }
                                        }
                                )
                                .allowsHitTesting(!isDrawingMode)
                        }

                        // Text Overlay
                        if !viewModel.textOverlay.isEmpty {
                            Text(viewModel.textOverlay)
                                .font(.largeTitle)
                                .bold()
                                .foregroundColor(.white)
                                .padding()
                                .background(Color.black.opacity(0.5))
                                .cornerRadius(10)
                                .position(textPosition ?? CGPoint(x: geometry.size.width / 2, y: geometry.size.height / 2))
                                // Add basic drag gesture for text
                                .gesture(
                                    DragGesture()
                                        .onChanged { value in
                                            if !isDrawingMode {
                                                textPosition = value.location
                                            }
                                        }
                                )
                                .allowsHitTesting(!isDrawingMode)
                        }

                        // Top Controls
                        VStack {
                            HStack(spacing: 20) {
                                Button(action: {
                                    viewModel.clearMedia()
                                    lines.removeAll()
                                    stickers.removeAll()
                                }) {
                                    Image(systemName: "xmark")
                                        .font(.title)
                                        .foregroundColor(.white)
                                        .padding(10)
                                        .background(Color.black.opacity(0.5))
                                        .clipShape(Circle())
                                }

                                Spacer()

                                // Color Picker for drawing
                                if isDrawingMode {
                                    ColorPicker("", selection: $currentLine.color)
                                        .labelsHidden()
                                }

                                Button(action: {
                                    isDrawingMode.toggle()
                                }) {
                                    Image(systemName: "scribble")
                                        .font(.title)
                                        .foregroundColor(isDrawingMode ? .blue : .white)
                                        .padding(10)
                                        .background(Color.black.opacity(0.5))
                                        .clipShape(Circle())
                                }

                                Menu {
                                    ForEach(emojiOptions, id: \.self) { emoji in
                                        Button(emoji) {
                                            addSticker(text: emoji, to: geometry.size)
                                        }
                                    }
                                } label: {
                                    Image(systemName: "face.smiling")
                                        .font(.title)
                                        .foregroundColor(.white)
                                        .padding(10)
                                        .background(Color.black.opacity(0.5))
                                        .clipShape(Circle())
                                }
                            }
                            .padding(.top, 50)
                            .padding(.horizontal)

                            Spacer()

                            // Bottom Controls
                            if !isDrawingMode {
                                VStack {
                                    TextField("Add text...", text: $viewModel.textOverlay)
                                        .padding()
                                        .background(Color.black.opacity(0.5))
                                        .foregroundColor(.white)
                                        .cornerRadius(10)
                                        .padding()

                                    HStack {
                                        Spacer()
                                        Button(action: {
                                            viewModel.submitStory()
                                        }) {
                                            if viewModel.isLoading {
                                                ProgressView()
                                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            } else {
                                                Text("Post Story")
                                                    .bold()
                                            }
                                        }
                                        .padding()
                                        .background(Color.blue)
                                        .foregroundColor(.white)
                                        .cornerRadius(20)
                                        .disabled(viewModel.isLoading)
                                        .accessibilityLabel("Post Story Button")
                                    }
                                    .padding()
                                }
                            }
                        }
                    }
                }
            } else {
                // Creation Mode (Camera/Picker)
                VStack {
                    HStack {
                        Button("Cancel") {
                            presentationMode.wrappedValue.dismiss()
                        }
                        .foregroundColor(.white)
                        .padding()
                        Spacer()
                    }

                    Spacer()

                    HStack {
                        Button(action: {
                            isShowingPicker = true
                        }) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.largeTitle)
                                .foregroundColor(.white)
                        }
                        .padding()
                        .accessibilityLabel("Open photo library for story")

                        Spacer()

                        Button(action: {
                            isShowingCamera = true
                        }) {
                            Image(systemName: "camera.circle.fill")
                                .font(.system(size: 80))
                                .foregroundColor(.white)
                        }
                        .accessibilityLabel("Open camera for story")

                        Spacer()

                        // Placeholder for alignment
                        Color.clear.frame(width: 50, height: 50)
                    }
                    .padding(.bottom, 40)
                }
            }
        }
        .fullScreenCover(isPresented: $isShowingCamera) {
            CameraCaptureScreen(onMediaCaptured: { url in
                viewModel.setMedia(url: url)
                isShowingCamera = false
            }, onCancel: {
                isShowingCamera = false
            })
        }
        .sheet(isPresented: $isShowingPicker) {
             PhotosPicker(selectedMedia: $selectedMediaURLs, maxSelectionCount: 1)
        }
        .onChange(of: selectedMediaURLs) { urls in
            if let first = urls.first {
                viewModel.setMedia(url: first)
                selectedMediaURLs = []
            }
        }
        .alert(isPresented: $viewModel.isStoryPosted) {
            Alert(
                title: Text("Success"),
                message: Text("Story posted successfully!"),
                dismissButton: .default(Text("OK")) {
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
    }

    private func addSticker(text: String, to size: CGSize) {
        let newSticker = Sticker(
            text: text,
            position: CGPoint(x: size.width / 2, y: size.height / 2),
            scale: 1.0
        )
        stickers.append(newSticker)
    }
}

struct Sticker: Identifiable {
    let id = UUID()
    let text: String
    var position: CGPoint
    var scale: CGFloat
}
