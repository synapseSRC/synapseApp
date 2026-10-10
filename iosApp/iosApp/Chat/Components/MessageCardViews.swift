import Foundation
import shared

enum SwiftMessageCardType: String {
    case poll
    case contact
    case location
    case liveLocation
    case viewOnce
    case event
    case product
    case music
    case sharedPost
    case sharedStory
    case payment
    case map
    case codeSnippet
    case call
    case forwarded
    case unknown
}

extension SwiftMessage {
    var cardType: SwiftMessageCardType {
        switch messageType {
        case .poll: return .poll
        case .contact: return .contact
        case .location: return .location
        case .liveLocation: return .liveLocation
        case .ephemeralMedia: return .viewOnce
        case .event: return .event
        case .product: return .product
        case .music: return .music
        case .sharedPost: return .sharedPost
        case .storyShare: return .sharedStory
        case .payment: return .payment
        case .map: return .map
        case .codeSnippet: return .codeSnippet
        case .call: return .call
        case .forwarded: return .forwarded
        default: return .unknown
        }
    }
}

struct PollCardView: View {
    let message: SwiftMessage
    let onVote: (optionId: String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "dot.radiation")
                    .foregroundColor(.primary)
                Text(message.metadataContainer?.poll?.question ?? "Poll")
                    .font(.headline)
            }
            let totalVotes = message.metadataContainer?.poll?.options.reduce(0) { $0 + $1.voteCount } ?? 0
            if let options = message.metadataContainer?.poll?.options {
                ForEach(options) { option in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color(.systemGray5))
                            .frame(height: 28)
                            .cornerRadius(6)
                        Rectangle()
                            .fill(.primary.opacity(0.6))
                            .frame(width: max(4, Double(option.voteCount) / max(1, totalVotes) * 100), height: 28)
                            .cornerRadius(6)
                    }
                    .frame(height: 28)
                    .overlay(alignment: .leading) {
                        HStack {
                            Text(option.text)
                            Spacer()
                            Text("\(option.voteCount)")
                                .font(.caption)
                        }
                        .padding(.leading, 8)
                        .padding(.trailing, 8)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onVote(optionId: option.id)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct ContactCardView: View {
    let message: SwiftMessage

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.primary)
                VStack(alignment: .leading) {
                    Text(message.metadataContainer?.contact?.name ?? "Contact")
                        .font(.headline)
                    if let phone = message.metadataContainer?.contact?.phoneNumber {
                        Text(phone)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct LocationCardView: View {
    let message: SwiftMessage
    let onOpenMap: (Double, Double) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "map")
                    .foregroundColor(.primary)
                VStack(alignment: .leading) {
                    Text(message.metadataContainer?.location?.title ?? "Location")
                        .font(.headline)
                    if let address = message.metadataContainer?.location?.address {
                        Text(address)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                if let lat = message.metadataContainer?.location?.latitude,
                   let lon = message.metadataContainer?.location?.longitude {
                    onOpenMap(lat, lon)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct ViewOnceCardView: View {
    let message: SwiftMessage
    let isConsumed: Bool
    let onOpen: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: isConsumed ? "eye.slash" : "eye")
                    .foregroundColor(isConsumed ? .secondary : .primary)
                VStack(alignment: .leading) {
                    Text(message.metadataContainer?.viewOnce?.mode == "VIEW_TWICE" ? "View Twice" : "View Once")
                        .font(.headline)
                    Text(isConsumed ? "Opened" : "Tap to view")
                        .font(.caption)
                        .foregroundColor(isConsumed ? .secondary : .primary)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .onTapGesture {
            onOpen()
        }
    }
}

struct EventCardView: View {
    let message: SwiftMessage
    let onNavigateToEvent: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "calendar")
                    .foregroundColor(.primary)
                VStack(alignment: .leading) {
                    Text(message.metadataContainer?.event?.title ?? "Event")
                        .font(.headline)
                    if let startTime = message.metadataContainer?.event?.startTimeIso {
                        Text("📅 \(startTime)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .onTapGesture {
            if let eventId = message.metadataContainer?.event?.eventId {
                onNavigateToEvent(eventId)
            }
        }
    }
}

struct ProductCardView: View {
    let message: SwiftMessage
    let onNavigateToProduct: (String) -> Void

    var body: some View {
        HStack(spacing: 12) {
            if let imageUrl = message.metadataContainer?.product?.imageUrl {
                AsyncImage(url: URL(string: imageUrl)) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    Rectangle().fill(Color(.systemGray4))
                }
                .frame(width: 60, height: 60)
                .cornerRadius(8)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(message.metadataContainer?.product?.title ?? "Product")
                    .font(.headline)
                Text(message.metadataContainer?.product?.price ?? "0.00")
                    .font(.subheadline)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .onTapGesture {
            if let productId = message.metadataContainer?.product?.productId {
                onNavigateToProduct(productId)
            }
        }
    }
}

struct ForwardedMessageCardView: View {
    let message: SwiftMessage

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "arrowshape.turn.up.left")
                .font(.caption)
                .foregroundColor(.secondary)
            Text("Forwarded")
                .font(.caption)
                .foregroundColor(.secondary)
                .italic()
        }
    }
}
