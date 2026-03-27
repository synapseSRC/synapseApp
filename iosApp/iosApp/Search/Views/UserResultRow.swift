import SwiftUI
import shared

struct UserResultRow: View {
    let user: SearchAccount

    var body: some View {
        HStack(spacing: 12) {
            // Avatar Placeholder
            Circle()
                .fill(Color.gray.opacity(0.3))
                .frame(width: 48, height: 48)
                .overlay(
                    Text(user.displayName?.prefix(1).uppercased() ?? user.handle?.prefix(1).uppercased() ?? "?")
                        .foregroundColor(.gray)
                        .font(.headline)
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .center, spacing: 4) {
                    Text(user.displayName ?? user.handle ?? "Unknown User")
                        .font(.headline)
                        .lineLimit(1)

                    if user.isVerified {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(.blue)
                            .font(.system(size: 14))
                    }
                }

                Text("@\(user.handle ?? "")")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)

                if let bio = user.bio, !bio.isEmpty {
                    Text(bio)
                        .font(.caption)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .padding(.top, 2)
                }
            }

            Spacer()

            // Following button mock
            Button(action: {
                // To be implemented
            }) {
                Text(user.isFollowing ? "Following" : "Follow")
                    .font(.system(size: 14, weight: .semibold))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                    .background(user.isFollowing ? Color.clear : Color.blue)
                    .foregroundColor(user.isFollowing ? .primary : .white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(user.isFollowing ? Color.gray.opacity(0.5) : Color.clear, lineWidth: 1)
                    )
                    .cornerRadius(16)
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal)
    }
}
