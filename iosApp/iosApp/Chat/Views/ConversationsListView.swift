import SwiftUI

struct ConversationsListView: View {
    @StateObject private var viewModel = ConversationsViewModel()
    @State private var searchText = ""

    var filteredConversations: [SwiftConversation] {
        if searchText.isEmpty {
            return viewModel.conversations
        } else {
            return viewModel.conversations.filter { $0.participantName.localizedCaseInsensitiveContains(searchText) }
        }
    }

    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading && viewModel.conversations.isEmpty {
                    ProgressView(String(localized: "chat_list_loading"))
                } else if let error = viewModel.errorMessage, viewModel.conversations.isEmpty {
                    VStack {
                        Text("\(String(localized: "chat_list_error_prefix"))\(error)")
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding()
                        Button(String(localized: "chat_list_retry")) {
                            viewModel.fetchConversations()
                        }
                    }
                } else if viewModel.conversations.isEmpty {
                    Text(String(localized: "chat_list_no_chats"))
                        .foregroundColor(.gray)
                        .accessibilityIdentifier("EmptyConversationsText")
                } else {
                    List(filteredConversations) { conversation in
                        NavigationLink(destination: ChatDetailView(chatId: conversation.id, participantName: conversation.participantName)) {
                            ConversationRow(conversation: conversation)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                viewModel.deleteConversation(chatId: conversation.id)
                            } label: {
                                Label(String(localized: "chat_list_delete"), systemImage: "trash")
                            }
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                viewModel.archiveConversation(chatId: conversation.id)
                            } label: {
                                Label(String(localized: "chat_list_archive"), systemImage: "archivebox")
                            }
                            .tint(.green)
                        }
                    }
                    .listStyle(PlainListStyle())
                    .searchable(text: $searchText, prompt: Text(String(localized: "chat_list_search_prompt")))
                }
            }
            .navigationTitle(String(localized: "chat_list_nav_title"))
            .onAppear {
                viewModel.fetchConversations()
            }
            // Pull to refresh
            .refreshable {
                viewModel.fetchConversations()
            }
        }
    }
}

struct ConversationRow: View {
    let conversation: SwiftConversation

    var body: some View {
        HStack(spacing: 12) {
            // Avatar Placeholder
            Circle()
                .fill(Color.gray.opacity(0.3))
                .frame(width: 50, height: 50)
                .overlay(
                    Text(String(conversation.participantName.prefix(1)).uppercased())
                        .foregroundColor(.gray)
                        .font(.headline)
                )
                .overlay(
                    Circle()
                        .fill(conversation.isOnline ? Color.green : Color.clear)
                        .frame(width: 12, height: 12)
                        .offset(x: 17, y: 17)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(conversation.participantName)
                        .font(.headline)
                        .lineLimit(1)
                    Spacer()
                    if let time = conversation.lastMessageTime {
                        Text(formatTime(time))
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }

                HStack {
                    Text(conversation.lastMessage)
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .lineLimit(2)
                    Spacer()
                    if conversation.unreadCount > 0 {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 20, height: 20)
                            .overlay(
                                Text("\(conversation.unreadCount)")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white)
                            )
                    }
                }
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(conversation.participantName), \(conversation.unreadCount) unread messages. Last message: \(conversation.lastMessage)")
        .accessibilityHint("Double tap to open chat")
    }

    private func formatTime(_ timeStr: String) -> String {
        return DateFormatterUtil.formatTime(dateString: timeStr)
    }
}

struct ConversationsListView_Previews: PreviewProvider {
    static var previews: some View {
        ConversationsListView()
    }
}
