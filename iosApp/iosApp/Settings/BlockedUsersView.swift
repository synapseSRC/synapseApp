import SwiftUI
import shared

struct BlockedUsersView: View {
    @StateObject private var viewModel = DependencyContainer.shared.makeBlockedUsersViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.blockedUsers.isEmpty {
                List {
                    ForEach(0..<3, id: \.self) { _ in
                        HStack {
                            Circle()
                                .fill(Color.gray.opacity(0.2))
                                .frame(width: 48, height: 48)
                            VStack(alignment: .leading, spacing: 4) {
                                Rectangle()
                                    .fill(Color.gray.opacity(0.2))
                                    .frame(height: 16)
                                    .cornerRadius(4)
                                Rectangle()
                                    .fill(Color.gray.opacity(0.2))
                                    .frame(width: 120, height: 12)
                                    .cornerRadius(4)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 4)
                    }
                }
                .navigationTitle("Blocked Users")
            } else if let errorMessage = viewModel.errorMessage {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundColor(.orange)
                        .font(.system(size: 48))
                    Text(errorMessage)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                }
                .navigationTitle("Blocked Users")
            } else if viewModel.blockedUsers.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "person.slash")
                        .foregroundColor(.secondary)
                        .font(.system(size: 48))
                    Text("You haven't blocked anyone yet.")
                        .foregroundColor(.secondary)
                }
                .navigationTitle("Blocked Users")
            } else {
                List {
                    ForEach(viewModel.blockedUsers, id: \.id) { blockedUser in
                        HStack(spacing: 12) {
                            Group {
                                if let avatarUrl = blockedUser.blockedUserAvatar, !avatarUrl.isEmpty,
                                   let url = URL(string: avatarUrl) {
                                    AsyncImage(url: url) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Circle().fill(Color.gray.opacity(0.2))
                                    }
                                } else {
                                    Circle()
                                        .fill(Color.gray.opacity(0.2))
                                        .frame(width: 48, height: 48)
                                    Text(String((blockedUser.blockedUsername ?? "User")?.prefix(1) ?? "?").uppercased())
                                        .foregroundColor(.gray)
                                        .font(.headline)
                                }
                            }
                            .frame(width: 48, height: 48)
                            .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 4) {
                                Text(blockedUser.blockedUsername ?? "Unknown User")
                                    .font(.headline)
                                Text("Blocked user")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()
                        }
                        .padding(.vertical, 4)
                    }
                }
                .navigationTitle("Blocked Users")
            }
        }
        .task {
            await viewModel.load()
        }
    }
}
