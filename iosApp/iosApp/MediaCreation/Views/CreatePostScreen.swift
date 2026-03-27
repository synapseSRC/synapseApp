import SwiftUI
import AVFoundation
import AVKit

struct CreatePostScreen: View {
    @StateObject private var viewModel = CreatePostViewModel()
    @State private var isShowingMediaPicker = false
    @State private var isShowingCamera = false

    var body: some View {
        NavigationView {
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

                TextEditor(text: $viewModel.text)
                    .frame(minHeight: 100)
                    .padding()
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.gray.opacity(0.5), lineWidth: 1)
                    )
                    .padding()
                    .accessibilityLabel("Post content text editor")

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

                Spacer()

                HStack {
                    Button(action: {
                        isShowingMediaPicker = true
                    }) {
                        Image(systemName: "photo.on.rectangle")
                            .font(.title2)
                    }
                    .padding()
                    .accessibilityLabel("Open photo library")

                    Button(action: {
                        isShowingCamera = true
                    }) {
                        Image(systemName: "camera")
                            .font(.title2)
                    }
                    .padding()
                    .accessibilityLabel("Open camera")

                    Spacer()

                    Picker("Privacy", selection: $viewModel.privacy) {
                        Text("Public").tag("public")
                        Text("Friends").tag("friends")
                        Text("Private").tag("private")
                    }
                    .pickerStyle(MenuPickerStyle())
                }
                .padding()
            }
            .navigationTitle("New Post")
            .navigationBarItems(
                leading: Button("Cancel") {
                    // Handle cancel, e.g., dismiss view
                },
                trailing: Button("Post") {
                    viewModel.submitPost()
                }
                .disabled(viewModel.isLoading || (viewModel.text.isEmpty && viewModel.mediaURLs.isEmpty))
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
                Alert(title: Text("Success"), message: Text("Post created successfully!"), dismissButton: .default(Text("OK")))
            }
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
                    Color.gray
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
                            .overlay(Circle().stroke(Color.gray, lineWidth: 2))
                    }
                    .accessibilityLabel("Take photo")
                    .padding()

                    Button(action: {
                        isRecording.toggle()
                    }) {
                        Circle()
                            .fill(isRecording ? Color.red : Color.white)
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
