import SwiftUI
import shared

struct MessageBubbleView: View {
    let message: SwiftMessage
    let isFromMe: Bool
    var onReactionSelected: ((shared.ReactionType) -> Void)? = nil

    var body: some View {
        HStack {
            if isFromMe {
                Spacer()
            }

            VStack(alignment: isFromMe ? .trailing : .leading, spacing: 4) {
                if message.messageType == .image || message.messageType == .video, let urlStr = message.mediaUrl, let url = URL(string: urlStr) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFit()
                    } placeholder: {
                        Rectangle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(height: 150)
                            .overlay(ProgressView())
                    }
                    .frame(maxWidth: 250)
                    .cornerRadius(12)
                }

                if !message.content.isEmpty {
                    Text(message.content)
                        .padding(12)
                        .foregroundColor(isFromMe ? .white : .primary)
                        .background(isFromMe ? Color.blue : Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                }

                if !message.reactions.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(Array(message.reactions.keys), id: \.self) { type in
                            if let count = message.reactions[type] {
                                Button(action: {
                                    onReactionSelected?(type)
                                }) {
                                    HStack(spacing: 2) {
                                        Text(type.emoji)
                                            .font(.system(size: 12))
                                        if count > 1 {
                                            Text("\(count)")
                                                .font(.system(size: 10))
                                        }
                                    }
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(message.userReaction == type ? Color.blue.opacity(0.2) : Color.gray.opacity(0.1))
                                    .clipShape(Capsule())
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                    }
                    .padding(.top, -8)
                }

                HStack(spacing: 4) {
                    Text(formatTime(message.createdAt))
                        .font(.caption2)
                        .foregroundColor(.gray)

                    if isFromMe {
                        Image(systemName: statusIcon(for: message.deliveryStatus))
                            .font(.caption2)
                            .foregroundColor(statusColor(for: message.deliveryStatus))
                    }
                }
            }

            if !isFromMe {
                Spacer()
            }
        }
        .padding(.horizontal, 4)
    }

    private func formatTime(_ timeStr: String) -> String {
        let split = timeStr.split(separator: "T")
        if split.count == 2 {
            let timePart = split[1].split(separator: ":")
            if timePart.count >= 2 {
                return "\(timePart[0]):\(timePart[1])"
            }
        }
        return timeStr
    }

    private func statusIcon(for status: shared.DeliveryStatus) -> String {
        switch status {
        case .sent:
            return "checkmark"
        case .delivered:
            return "checkmark.circle"
        case .read:
            return "checkmark.circle.fill"
        default:
            return "clock"
        }
    }

    private func statusColor(for status: shared.DeliveryStatus) -> Color {
        switch status {
        case .read:
            return .blue
        default:
            return .gray
        }
    }
}
