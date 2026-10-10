import Foundation
import SwiftUI
import shared

struct MessageBubbleView: View {
    let message: SwiftMessage
    let isCurrentUser: Bool
    let onVote: (optionId: String) -> Void
    let onOpenMap: (Double, Double) -> Void
    let onOpenUrl: (String) -> Void
    let onNavigateToEvent: (String) -> Void
    let onNavigateToProduct: (String) -> Void
    let onOpenEphemeralMedia: (String) -> Void

    var body: some View {
        HStack {
            if isCurrentUser {
                Spacer()
                MessageContent(message: message, isCurrentUser: true)
                    .padding(.horizontal, 12)
                    .padding(.top, 4)
                    .padding(.bottom, 2)
                    .frame(maxWidth: 260)
                    .cornerRadius(12, corners: [.topLeading], cornerRadii: [12])
                    .background(Color(UIColor.systemBlue).opacity(0.1))
            } else {
                MessageContent(message: message, isCurrentUser: false)
                    .padding(.horizontal, 12)
                    .padding(.top, 4)
                    .padding(.bottom, 2)
                    .frame(maxWidth: 260, alignment: .leading)
                    .cornerRadius(12)
                    .background(Color(UIColor.systemGray6))
            }
            if isCurrentUser {
                Spacer()
            }
        }
        .padding(.horizontal, 16)
    }
}

private extension MessageBubbleView {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner, cornerRadii: CGSize) -> some View {
        GeometryReader { geometry in
            Path { path in
                let width = geometry.size.width
                let height = geometry.size.height

                for corner in corners {
                    path.move(to: .zero)
                    path.addLine(to: CGPoint(x: width, y: 0))
                    path.addLine(to: CGPoint(x: width, y: height))
                    path.addLine(to: CGPoint(x: 0, y: height))
                }
            }
            .fill(self.background)
            .overlay(
                RoundedRectangle(cornerRadius: radius)
                    .stroke(self.background, lineWidth: 0)
            )
        }
    }
}

private extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner, cornerRadii: CGSize) -> some View {
        GeometryReader { geometry in
            Path { path in
                let width = geometry.size.width
                let height = geometry.size.height

                for corner in corners {
                    path.move(to: .zero)
                    path.addLine(to: CGPoint(x: width, y: 0))
                    path.addLine(to: CGPoint(x: width, y: height))
                    path.addLine(to: CGPoint(x: 0, y: height))
                }
            }
            .fill(self.background)
            .overlay(
                RoundedRectangle(cornerRadius: radius)
                    .stroke(self.background, lineWidth: 0)
            )
        }
    }
}

private extension View {
    func background(_ color: Color) -> some View {
        self.overlay(self.backgroundColor(color))
    }

    private func backgroundColor(_ color: Color) -> Color {
        color
    }
}

private struct MessageContent: View {
    let message: SwiftMessage
    let isCurrentUser: Bool

    var body: some View {
        VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 4) {
            HStack {
                if !message.isForwarded {
                    MessageText(message: message, isCurrentUser: isCurrentUser)
                } else {
                    VStack(alignment: .leading, spacing: 4) {
                        MessageText(message: message, isCurrentUser: isCurrentUser)
                        ForwardedMessageCardView(message: message)
                    }
                }
            }

            if message.cardType == .poll {
                PollCardView(
                    message: message,
                    onVote: { optionId in
                        onVote(optionId: optionId)
                    }
                )
            } else if message.cardType == .contact {
                ContactCardView(message: message)
            } else if message.cardType == .location || message.cardType == .liveLocation {
                LocationCardView(
                    message: message,
                    onOpenMap: { lat, lon in
                        onOpenMap(lat, lon)
                    }
                )
            } else if message.cardType == .viewOnce {
                ViewOnceCardView(
                    message: message,
                    isConsumed: message.metadataContainer?.viewOnce?.isConsumed ?? false,
                    onOpen: {
                        if let url = message.mediaUrl {
                            onOpenEphemeralMedia(url)
                        }
                    }
                )
            } else if message.cardType == .event {
                EventCardView(
                    message: message,
                    onNavigateToEvent: onNavigateToEvent
                )
            } else if message.cardType == .product {
                ProductCardView(
                    message: message,
                    onNavigateToProduct: onNavigateToProduct
                )
            }
        }
    }
}

private struct MessageText: View {
    let message: SwiftMessage
    let isCurrentUser: Bool

    var body: some View {
        VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 2) {
            if !message.isDeleted && !message.isFailed {
                if !message.isDeleted {
                    Text(message.text)
                        .font(.body)
                        .foregroundColor(.primary)
                }
                if let timestamp = message.timestamp {
                    Text(timestamp)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }

            if message.isDeleted {
                Text("Message deleted")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .italic()
            }

            if message.isFailed {
                Image(systemName: "exclamationmark.circle")
                    .font(.caption)
                    .foregroundColor(.red)
            }

            if let mediaUrl = message.mediaUrl, !mediaUrl.isEmpty {
                AsyncImage(url: URL(string: mediaUrl)) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    Rectangle().fill(Color(.systemGray4))
                        .aspectRatio(1, contentMode: .fit)
                }
                .cornerRadius(8)
            }

            if let replyTo = message.replyTo {
                ReplyMessageView(message: replyTo)
                    .padding()
                    .background(Color(.systemGray5))
                    .cornerRadius(8)
            }
        }
    }
}

private struct ReplyMessageView: View {
    let message: SwiftMessage

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Reply to \(message.text)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}
