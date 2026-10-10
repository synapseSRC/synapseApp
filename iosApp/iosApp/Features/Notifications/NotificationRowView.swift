import SwiftUI
import shared

enum GroupPosition {
    case single
    case first
    case middle
    case last
}

struct NotificationRowView: View {
    let notification: NotificationItem
    let position: GroupPosition
    var onActorTap: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // Avatar
            Button(action: { onActorTap?() }) {
                if let avatarUrlString = notification.actorAvatar, let url = URL(string: avatarUrlString) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 40, height: 40)
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())
                        case .failure:
                            fallbackAvatar
                        @unknown default:
                            fallbackAvatar
                        }
                    }
                } else {
                    fallbackAvatar
                }
            }
            .buttonStyle(.plain)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Button(action: { onActorTap?() }) {
                        Text(notification.actorName)
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                    }
                    .buttonStyle(.plain)

                    Text(notification.message)
                        .font(.subheadline)
                        .foregroundColor(.primary)
                }
                .lineLimit(2)

                HStack(spacing: 6) {
                    typeIcon
                        .foregroundColor(.secondary)
                        .font(.caption)

                    Text(notification.timestamp)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            if !notification.isRead {
                Circle()
                    .fill(Color.accentColor)
                    .frame(width: 8, height: 8)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            notification.isRead
                ? Color(uiColor: .tertiarySystemGroupedBackground)
                : Color.accentColor.opacity(0.1)
        )
        .clipShape(GroupedCardShape(position: position))
    }

    private var fallbackAvatar: some View {
        Circle()
            .fill(Color.gray.opacity(0.3))
            .frame(width: 40, height: 40)
            .overlay(
                Text(String(notification.actorName.prefix(1)).uppercased())
                    .font(.subheadline)
                    .foregroundColor(.gray)
            )
    }

    @ViewBuilder
    private var typeIcon: some View {
        switch notification.type.lowercased() {
        case "like", "new_like_post", "new_like_comment":
            Image(systemName: "heart.fill")
                .foregroundColor(.red)
        case "comment", "new_comment", "new_reply", "reply":
            Image(systemName: "bubble.right.fill")
                .foregroundColor(.blue)
        case "follow", "new_follower":
            Image(systemName: "person.badge.plus.fill")
                .foregroundColor(.green)
        case "mention":
            Image(systemName: "at")
                .foregroundColor(.purple)
        default:
            Image(systemName: "bell.fill")
        }
    }
}

struct GroupedCardShape: Shape {
    let position: GroupPosition
    let radius: CGFloat = 16

    func path(in rect: CGRect) -> Path {
        var corners: UIRectCorner = []
        switch position {
        case .single:
            corners = .allCorners
        case .first:
            corners = [.topLeft, .topRight]
        case .middle:
            corners = []
        case .last:
            corners = [.bottomLeft, .bottomRight]
        }

        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
