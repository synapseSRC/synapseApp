import SwiftUI

struct PostCardView: View {
    var post: Post
    var onLike: () -> Void
    var onComment: () -> Void
    var onShare: () -> Void
    var onBookmark: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack(alignment: .center, spacing: 10) {
                // Avatar Placeholder
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Text(String(post.displayName?.prefix(1) ?? post.username?.prefix(1) ?? "?").uppercased())
                            .foregroundColor(.gray)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 4) {
                        Text(post.displayName ?? post.username ?? "Unknown")
                            .font(.headline)
                            .foregroundColor(.primary)

                        if post.isVerified {
                            Image(systemName: "checkmark.seal.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        }

                        Text("@\(post.username ?? "")")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Text(post.createdAt ?? "Just now")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button(action: { /* More options */ }) {
                    Image(systemName: "ellipsis")
                        .foregroundColor(.secondary)
                }
            }
            .padding(.horizontal)

            // Text Content
            if let text = post.postText, !text.isEmpty {
                Text(text)
                    .font(.body)
                    .foregroundColor(.primary)
                    .padding(.horizontal)
                    .lineLimit(4)
            }

            // Media Content (Image/Video)
            if let mediaItems = post.mediaItems, let firstMedia = mediaItems.first {
                if firstMedia.type == .image {
                    AsyncImage(url: URL(string: firstMedia.url)) { phase in
                        switch phase {
                        case .empty:
                            Rectangle()
                                .fill(Color.gray.opacity(0.2))
                                .frame(height: 250)
                                .overlay(ProgressView())
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxHeight: 300)
                                .clipped()
                                .cornerRadius(12)
                        case .failure:
                            Rectangle()
                                .fill(Color.gray.opacity(0.2))
                                .frame(height: 200)
                                .overlay(
                                    Image(systemName: "photo")
                                        .foregroundColor(.gray)
                                )
                        @unknown default:
                            EmptyView()
                        }
                    }
                    .padding(.horizontal)
                } else if firstMedia.type == .video {
                    // Video Placeholder
                    Rectangle()
                        .fill(Color.black)
                        .frame(height: 250)
                        .cornerRadius(12)
                        .overlay(
                            Image(systemName: "play.circle.fill")
                                .font(.system(size: 50))
                                .foregroundColor(.white.opacity(0.8))
                        )
                        .padding(.horizontal)
                }
            }

            // Poll
            if post.hasPoll == true, let options = post.pollOptions {
                VStack(spacing: 8) {
                    ForEach(options) { option in
                        HStack {
                            Text(option.text)
                                .font(.subheadline)
                            Spacer()
                            Text("\(option.votes)%")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(8)
                    }
                    Text("Total votes: \(options.reduce(0) { $0 + $1.votes })")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)
            }

            // Footer Action Buttons
            HStack(spacing: 20) {
                actionButton(
                    icon: post.userReaction == .like ? "heart.fill" : "heart",
                    color: post.userReaction == .like ? .red : .secondary,
                    count: post.likesCount,
                    action: onLike
                )

                actionButton(
                    icon: "bubble.right",
                    color: .secondary,
                    count: post.commentsCount,
                    action: onComment
                )

                actionButton(
                    icon: "arrow.2.squarepath",
                    color: post.isReshared ? .green : .secondary,
                    count: post.resharesCount,
                    action: onShare
                )

                Spacer()

                actionButton(
                    icon: post.isBookmarked ? "bookmark.fill" : "bookmark",
                    color: post.isBookmarked ? .blue : .secondary,
                    count: nil,
                    action: onBookmark
                )
            }
            .padding(.horizontal)
            .padding(.top, 4)

            Divider()
                .padding(.top, 8)
        }
        .padding(.vertical, 8)
        .background(Color(UIColor.systemBackground))
    }

    @ViewBuilder
    private func actionButton(icon: String, color: Color, count: Int?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 16))
                if let count = count, count > 0 {
                    Text("\(count)")
                        .font(.footnote)
                }
            }
            .foregroundColor(color)
        }
    }
}
