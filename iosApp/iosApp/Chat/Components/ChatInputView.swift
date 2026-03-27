import SwiftUI
import PhotosUI

struct ChatInputView: View {
    let isSending: Bool
    let onSendText: (String) -> Void
    let onAttachMedia: (Data?, String?, String?) -> Void
    let onTyping: () -> Void

    @State private var inputText: String = ""
    @State private var selectedItem: PhotosPickerItem? = nil

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            HStack(alignment: .bottom, spacing: 12) {
                // Attach Media Button
                PhotosPicker(selection: $selectedItem, matching: .images) {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(.blue)
                }
                .disabled(isSending)
                .accessibilityLabel("Attach media")
                .onChange(of: selectedItem) { newItem in
                    Task {
                        if let data = try? await newItem?.loadTransferable(type: Data.self) {
                            // Ideally resolve filename/mimetype from item provider
                            onAttachMedia(data, "attachment.jpg", "image/jpeg")
                            selectedItem = nil
                        }
                    }
                }

                // Text Input
                TextField("Message", text: $inputText, axis: .vertical)
                    .lineLimit(1...5)
                    .padding(8)
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(20)
                    .disabled(isSending)
                    .onChange(of: inputText) { newValue in
                        if !newValue.isEmpty {
                            onTyping()
                        }
                    }

                // Send Button
                if isSending {
                    ProgressView()
                        .padding(.trailing, 4)
                } else {
                    Button(action: {
                        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                        if !text.isEmpty {
                            onSendText(text)
                            inputText = ""
                        }
                    }) {
                        Image(systemName: "paperplane.fill")
                            .font(.title2)
                            .foregroundColor(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .gray : .blue)
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityLabel("Send message")
                }
            }
            .padding(12)
            .background(Color(UIColor.systemBackground))
        }
    }
}
