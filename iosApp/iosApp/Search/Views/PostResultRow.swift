import SwiftUI
import shared

struct PostResultRow: View {
    let post: SearchPost

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Author info
            HStack(alignment: .top, spacing: 10) {
                // Avatar Placeholder
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Text(post.authorName?.prefix(1).uppercased() ?? post.authorHandle?.prefix(1).uppercased() ?? "?")
                            .foregroundColor(.gray)
                            .font(.subheadline)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 4) {
                        Text(post.authorName ?? "Unknown")
                            .font(.headline)
                            .lineLimit(1)
                        Text("@\(post.authorHandle ?? "unknown")")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                        Text("•")
                            .foregroundColor(.secondary)
                        Text(formatDate(dateString: post.createdAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if let content = post.content, !content.isEmpty {
                        Text(content)
                            .font(.body)
                            .fixedSize(horizontal: false, vertical: true)
                            .lineLimit(4)
                    } else {
                        Text("No text content")
                            .font(.body)
                            .foregroundColor(.gray)
                            .italic()
                    }
                }
            }

            // Interaction stats
            HStack(spacing: 24) {
                InteractionButton(icon: "bubble.right", count: post.commentsCount)
                InteractionButton(icon: "arrow.2.squarepath", count: post.boostCount)
                InteractionButton(icon: "heart", count: post.likesCount)
                InteractionButton(icon: "square.and.arrow.up", count: 0) // Share, placeholder
                Spacer()
            }
            .padding(.leading, 50) // align with text, considering avatar width + spacing
            .foregroundColor(.secondary)
        }
        .padding(.vertical, 12)
        .padding(.horizontal)
        .background(Color(.systemBackground))
    }

    // Simple date formatter (in production, use proper ISO8601 parsing)
    private func formatDate(dateString: String) -> String {
        // Quick short formatting hack for ISO strings
        let index = dateString.firstIndex(of: "T") ?? dateString.endIndex
        let datePart = dateString[..<index]
        return String(datePart)
    }
}

struct InteractionButton: View {
    let icon: String
    let count: Int32

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 16))
            if count > 0 {
                Text("\(count)")
                    .font(.caption)
            }
        }
    }
}
