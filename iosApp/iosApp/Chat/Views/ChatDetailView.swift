import SwiftUI
import shared

struct ChatDetailView: View {
    let chatId: String
    let participantName: String

    @StateObject private var viewModel = ChatViewModel()

    private let currentUserId = "my_user_id"

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 8) {
                        if viewModel.isLoading && viewModel.messages.isEmpty {
                            ProgressView()
                                .padding()
                        } else {
                            ForEach(viewModel.messages) { message in
                                MessageBubbleView(
                                    message: message,
                                    isFromMe: message.senderId == currentUserId,
                                    onReactionSelected: { type in
                                        viewModel.toggleReaction(messageId: message.id, emoji: type.emoji)
                                    }
                                )
                                .id(message.id)
                            }
                        }
                    }
                    .padding()
                }
                .onChange(of: viewModel.messages.count) { _ in
                    if let lastMessage = viewModel.messages.last {
                        withAnimation {
                            proxy.scrollTo(lastMessage.id, anchor: .bottom)
                        }
                    }
                }
                .onAppear {
                    if let lastMessage = viewModel.messages.last {
                        proxy.scrollTo(lastMessage.id, anchor: .bottom)
                    }
                }
            }

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(8)
                    .background(Color.red.opacity(0.8))
                    .cornerRadius(8)
                    .padding(.bottom, 4)
            }

            if !viewModel.smartReplies.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.smartReplies, id: \.self) { reply in
                            Button(action: {
                                viewModel.sendMessage(content: reply)
                            }) {
                                HStack {
                                    Image(systemName: "sparkles")
                                    Text(reply)
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.blue.opacity(0.8))
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 4)
                }
            }

            ChatInputView(
                isSending: viewModel.isSending,
                onSendText: { text in
                    viewModel.sendMessage(content: text)
                },
                onAttachMedia: { data, name, mime in
                    if let d = data, let n = name, let m = mime {
                        viewModel.sendMediaMessage(data: d, fileName: n, mimeType: m)
                    }
                },
                onTyping: {
                    viewModel.onTyping()
                }
            )
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack {
                    Text(participantName)
                        .font(.headline)
                    if viewModel.isParticipantTyping {
                        Text("Typing...")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                }
            }
        }
        .onAppear {
            viewModel.setup(chatId: chatId)
        }
    }
}
