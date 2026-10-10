import SwiftUI

struct MentionSuggestionView: View {
    let filter: String
    let onUserSelected: (User) -> Void

    // Mock user data for demonstration
    private var users: [User] {
        let allUsers = [
            User(id: "1", username: "john_doe", displayName: "John Doe", avatarUrl: nil, isVerified: true),
            User(id: "2", username: "jane_smith", displayName: "Jane Smith", avatarUrl: nil, isVerified: false),
            User(id: "3", username: "alex_jones", displayName: "Alex Jones", avatarUrl: nil, isVerified: true),
            User(id: "4", username: "synapse_official", displayName: "Synapse", avatarUrl: nil, isVerified: true)
        ]

        if filter.isEmpty {
            return allUsers
        }

        return allUsers.filter { user in
            user.username.lowercased().contains(filter.lowercased()) ||
            (user.displayName?.lowercased().contains(filter.lowercased()) ?? false)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if users.isEmpty {
                Text(String(localized: "mention_no_matching_users"))
                    .foregroundColor(.gray)
                    .padding()
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(users) { user in
                            Button(action: {
                                onUserSelected(user)
                            }) {
                                HStack {
                                    Circle()
                                        .fill(Color.gray.opacity(0.3))
                                        .frame(width: 40, height: 40)
                                        .overlay(
                                            Text(String((user.displayName ?? user.username).first ?? "?").uppercased())
                                                .foregroundColor(.white)
                                        )

                                    VStack(alignment: .leading) {
                                        HStack {
                                            Text(user.displayName ?? user.username)
                                                .fontWeight(.bold)
                                                .foregroundColor(.primary)

                                            if user.isVerified {
                                                Image(systemName: "checkmark.seal.fill")
                                                    .foregroundColor(AppTheme.primaryColor)
                                                    .font(.caption)
                                            }
                                        }

                                        Text("@\(user.username)")
                                            .foregroundColor(.gray)
                                            .font(.subheadline)
                                    }
                                    Spacer()
                                }
                                .padding(.vertical, 8)
                                .padding(.horizontal, 16)
                            }
                            Divider()
                        }
                    }
                }
            }
        }
        .frame(maxHeight: 200)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 5)
    }
}

struct MentionSuggestionView_Previews: PreviewProvider {
    static var previews: some View {
        MentionSuggestionView(filter: "j") { _ in }
            .padding()
            .background(Color.gray.opacity(0.1))
    }
}
