import SwiftUI
import AVFoundation
import AVKit

struct CreatePostScreen: View {
    var onPostSuccess: (() -> Void)? = nil
    @StateObject private var viewModel = CreatePostViewModel()
    @Environment(\.presentationMode) var presentationMode
    @State private var isShowingMediaPicker = false
    @State private var isShowingCamera = false
    @State private var showLocationPicker = false

    private var showMentionSuggestions: Bool {
        if let lastWord = viewModel.text.split(separator: " ").last, lastWord.hasPrefix("@") {
            return true
        }
        return false
    }

    private var mentionFilter: String {
        if let lastWord = viewModel.text.split(separator: " ").last, lastWord.hasPrefix("@") {
            return String(lastWord.dropFirst())
        }
        return ""
    }

    var body: some View {
        NavigationView {
            ZStack(alignment: .bottom) {
                ScrollView {
                    VStack {
                        if viewModel.isLoading {
                            ProgressView("Uploading...")
                                .padding()
                        }

                        if let error = viewModel.error {
                            Text(error)
                                .foregroundColor(.red)
                                .padding()
                        }

                        // Thread view: display each post in the thread
                        ForEach(Array(viewModel.threadPosts.enumerated()), id: \.offset) { index, _ in
                            VStack(alignment: .leading) {
                                if index > 0 {
                                    HStack {
                                        Rectangle()
                                            .fill(Color(.systemGray4))
                                            .frame(width: 2, height: 20)
                                            .padding(.leading, 24)
                                        Spacer()

                                        Button(action: {
                                            if index < viewModel.threadPosts.count {
                                                viewModel.threadPosts.remove(at: index)
                                            }
                                        }) {
                                            Image(systemName: "xmark")
                                                .foregroundColor(.gray)
                                        }
                                        .padding(.trailing)
                                    }
                                }

                                ZStack(alignment: .bottomTrailing) {
                                    if index == 0 {
                                        // Main post
                                        RichTextEditor(text: $viewModel.text)
                                            .frame(minHeight: 150)
                                            .padding()
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(Color(.systemGray3), lineWidth: 1)
                                            )
                                            .padding(.horizontal)
                                            .accessibilityLabel("Post content text editor")
                                            .onChange(of: viewModel.text) { newText in
                                                if !viewModel.threadPosts.isEmpty {
                                                    viewModel.threadPosts[0] = newText
                                                }
                                            }

                                        Text("\(viewModel.characterCount)/280")
                                            .font(.caption)
                                            .foregroundColor(viewModel.characterCount > 280 ? .red : (viewModel.characterCount > 260 ? .orange : .gray))
                                            .padding(.trailing, 24)
                                            .padding(.bottom, 8)
                                    } else {
                                        // Thread post
                                        RichTextEditor(text: Binding(
                                            get: {
                                                index < viewModel.threadPosts.count ? viewModel.threadPosts[index] : ""
                                            },
                                            set: {
                                                if index < viewModel.threadPosts.count {
                                                    viewModel.threadPosts[index] = $0
                                                }
                                            }
                                        ))
                                            .frame(minHeight: 100)
                                            .padding()
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(Color(.systemGray3), lineWidth: 1)
                                            )
                                            .padding(.horizontal)
                                            .accessibilityLabel("Thread content text editor")
                                    }
                                }
                            }
                        }

                        // Add to thread button
                        Button(action: {
                            viewModel.threadPosts.append("")
                        }) {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                Text(String(localized: "create_add_another_post"))
                            }
                            .foregroundColor(AppTheme.primaryColor)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)

                        if viewModel.showPoll {
                            PollCreatorView(options: $viewModel.pollOptions, duration: $viewModel.pollDuration)
                                .padding(.horizontal)
                        }

                        if showLocationPicker {
                            HStack {
                                Image(systemName: "mappin.and.ellipse")
                                    .foregroundColor(AppTheme.primaryColor)
                                TextField(String(localized: "create_search_location_placeholder"), text: Binding(
                                    get: { viewModel.location ?? "" },
                                    set: { viewModel.location = $0.isEmpty ? nil : $0 }
                                ))
                                .textFieldStyle(RoundedBorderTextFieldStyle())

                                Button(action: {
                                    showLocationPicker = false
                                    viewModel.location = nil
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding()
                            .background(AppTheme.primaryColor.opacity(0.05))
                            .cornerRadius(8)
                            .padding(.horizontal)
                        }

                        if viewModel.uploadProgress > 0 && viewModel.uploadProgress < 1.0 {
                            ProgressView(value: viewModel.uploadProgress)
                                .padding(.horizontal)
                                .accessibilityLabel("Upload progress \(Int(viewModel.uploadProgress * 100)) percent")
                        }

                        if !viewModel.mediaURLs.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack {
                                    ForEach(viewModel.mediaURLs.indices, id: \.self) { index in
                                        ZStack(alignment: .topTrailing) {
                                             MediaPreviewView(url: viewModel.mediaURLs[index])
                                                .frame(width: 100, height: 100)
                                                .cornerRadius(8)
                                                .clipped()

                                            Button(action: {
                                                viewModel.removeMedia(at: index)
                                            }) {
                                                Image(systemName: "xmark.circle.fill")
                                                    .foregroundColor(.red)
                                                    .background(Color.white.clipShape(Circle()))
                                            }
                                            .padding(4)
                                            .accessibilityLabel("Remove media attachment")
                                        }
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }

                        Spacer().frame(height: 100) // Space for toolbar
                    }
                }

                if showMentionSuggestions {
                    VStack {
                        Spacer()
                        MentionSuggestionView(filter: mentionFilter) { user in
                            let words = viewModel.text.split(separator: " ", omittingEmptySubsequences: false)
                            if var last = words.last, last.hasPrefix("@") {
                                var newWords = Array(words.dropLast())
                                newWords.append(Substring("@\(user.username) "))
                                viewModel.text = newWords.joined(separator: " ")
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 70) // Above toolbar
                    }
                }

                CreatePostToolbar(
                    onPhotoTapped: { isShowingMediaPicker = true },
                    onCameraTapped: { isShowingCamera = true },
                    onGiftTapped: { /* Show GIF picker placeholder */ },
                    onPollToggled: { viewModel.showPoll.toggle() },
                    onLocationToggled: { showLocationPicker.toggle() },
                    audienceType: $viewModel.audienceType
                )
            }
            .navigationTitle(String(localized: "create_new_post_nav_title"))
            .navigationBarItems(
                leading: Button(String(localized: "chat_cancel")) {
                    presentationMode.wrappedValue.dismiss()
                },
                trailing: Button(String(localized: "action_create_post")) {
                    viewModel.submitPost()
                }
                .disabled(viewModel.isLoading || (viewModel.text.isEmpty && viewModel.mediaURLs.isEmpty) || viewModel.characterCount > 280)
            )
            .sheet(isPresented: $isShowingMediaPicker) {
                PhotosPicker(selectedMedia: $viewModel.mediaURLs)
            }
            .fullScreenCover(isPresented: $isShowingCamera) {
                CameraCaptureScreen(onMediaCaptured: { url in
                    viewModel.mediaURLs.append(url)
                    isShowingCamera = false
                }, onCancel: {
                    isShowingCamera = false
                })
            }
            .alert(isPresented: $viewModel.isPostCreated) {
                Alert(
                    title: Text(String(localized: "alert_success_title")),
                    message: Text(String(localized: "alert_post_created_message")),
                    dismissButton: .default(Text(String(localized: "action_ok"))) {
                        if let onSuccess = onPostSuccess {
                            onSuccess()
                        } else {
                            presentationMode.wrappedValue.dismiss()
                        }
                    }
                )
            }
            .onDisappear {
                if !viewModel.isPostCreated {
                    viewModel.saveDraft()
                }
            }
        }
    }
}

// A custom wrapper for UITextView to support hashtag highlighting
struct RichTextEditor: UIViewRepresentable {
    @Binding var text: String

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.font = UIFont.systemFont(ofSize: 16)
        textView.backgroundColor = .clear
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        // Only update text completely if it's vastly different (like clear draft) to preserve cursor.
        // But we MUST reapply attributes.
        let selectedRange = uiView.selectedRange

        let mutableAttributedString = NSMutableAttributedString(string: text, attributes: [
            .font: UIFont.systemFont(ofSize: 16),
            .foregroundColor: UIColor.label
        ])

        // Highlight hashtags
        let hashtagRegex = try? NSRegularExpression(pattern: "#\\w+", options: [])
        let matches = hashtagRegex?.matches(in: text, options: [], range: NSRange(location: 0, length: text.utf16.count)) ?? []

        for match in matches {
            mutableAttributedString.addAttribute(.foregroundColor, value: UIColor.systemBlue, range: match.range)
        }

        uiView.attributedText = mutableAttributedString

        // Restore cursor
        if let currentText = uiView.text, currentText == text {
             uiView.selectedRange = selectedRange
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UITextViewDelegate {
        var parent: RichTextEditor

        init(_ parent: RichTextEditor) {
            self.parent = parent
        }

        func textViewDidChange(_ textView: UITextView) {
            parent.text = textView.text
        }
    }
}

// Helper to display media preview (image or video thumbnail)
struct MediaPreviewView: View {
    let url: URL

    var body: some View {
        Group {
            if url.pathExtension.lowercased() == "mov" || url.pathExtension.lowercased() == "mp4" {
                 VideoPlayer(player: AVPlayer(url: url))
                    .disabled(true) // Just for preview
            } else {
                if let image = UIImage(contentsOfFile: url.path) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                } else {
                    Color(.systemGray)
                }
            }
        }
    }
}

struct CameraCaptureScreen: View {
    let onMediaCaptured: (URL) -> Void
    let onCancel: () -> Void
    @State private var isRecording = false
    @State private var capturedMediaURL: URL?
    @State private var isTakingPhoto = false
    @State private var isFrontCamera = false
    @State private var isFlashOn = false

    var body: some View {
        ZStack {
            CameraView(isRecording: $isRecording, capturedMediaURL: $capturedMediaURL, isTakingPhoto: $isTakingPhoto, isFrontCamera: $isFrontCamera, isFlashOn: $isFlashOn)
                .edgesIgnoringSafeArea(.all)
                .accessibilityLabel("Camera Viewfinder")

            VStack {
                HStack {
                    Button("Cancel") {
                        onCancel()
                    }
                    .padding()
                    .foregroundColor(.white)
                    .background(Color.black.opacity(0.5))
                    .cornerRadius(8)
                    .accessibilityLabel("Cancel camera")

                    Spacer()

                    Button(action: {
                        isFlashOn.toggle()
                    }) {
                        Image(systemName: isFlashOn ? "bolt.fill" : "bolt.slash.fill")
                            .font(.title)
                            .foregroundColor(.white)
                            .padding()
                    }
                    .accessibilityLabel(isFlashOn ? "Turn off flash" : "Turn on flash")

                    Button(action: {
                        isFrontCamera.toggle()
                    }) {
                        Image(systemName: "arrow.triangle.2.circlepath.camera")
                            .font(.title)
                            .foregroundColor(.white)
                            .padding()
                    }
                    .accessibilityLabel("Switch camera")
                }
                Spacer()

                HStack {
                    Spacer()
                    Button(action: {
                        isTakingPhoto = true
                    }) {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 70, height: 70)
                            .overlay(Circle().stroke(Color(.systemGray), lineWidth: 2))
                    }
                    .accessibilityLabel("Take photo")
                    .padding()

                    Button(action: {
                        isRecording.toggle()
                    }) {
                        Circle()
                            .fill(isRecording ? AppTheme.errorColor : Color.white)
                            .frame(width: 70, height: 70)
                            .overlay(
                                Circle().stroke(Color.white, lineWidth: 4)
                            )
                    }
                    .accessibilityLabel(isRecording ? "Stop recording video" : "Start recording video")
                    .padding()
                    Spacer()
                }
                .padding(.bottom, 30)
            }
        }
        .onChange(of: capturedMediaURL) { url in
            if let url = url {
                onMediaCaptured(url)
            }
        }
    }
}
