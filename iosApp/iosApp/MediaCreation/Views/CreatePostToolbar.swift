import SwiftUI

struct CreatePostToolbar: View {
    let onPhotoTapped: () -> Void
    let onCameraTapped: () -> Void
    let onGiftTapped: () -> Void // Placeholder for GIF
    let onPollToggled: () -> Void
    let onLocationToggled: () -> Void
    @Binding var audienceType: AudienceType

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onPhotoTapped) {
                Image(systemName: "photo.on.rectangle")
                    .font(.title3)
            }
            .accessibilityLabel("Add Photo")

            Button(action: onCameraTapped) {
                Image(systemName: "camera")
                    .font(.title3)
            }
            .accessibilityLabel("Open Camera")

            Button(action: onGiftTapped) {
                Image(systemName: "gift")
                    .font(.title3)
            }
            .accessibilityLabel("Add GIF")

            Button(action: onPollToggled) {
                Image(systemName: "chart.bar.xaxis")
                    .font(.title3)
            }
            .accessibilityLabel("Add Poll")

            Button(action: onLocationToggled) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.title3)
            }
            .accessibilityLabel("Add Location")

            Spacer()

            Menu {
                ForEach(AudienceType.allCases) { type in
                    Button(action: {
                        audienceType = type
                    }) {
                        Label(type.rawValue, systemImage: iconFor(type))
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: iconFor(audienceType))
                        .font(.subheadline)
                    Text(audienceType.rawValue)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Image(systemName: "chevron.down")
                        .font(.caption)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.blue.opacity(0.1))
                .foregroundColor(.blue)
                .cornerRadius(16)
            }
            .accessibilityLabel("Select Audience")
        }
        .padding()
        .background(Color(.systemBackground))
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color.gray.opacity(0.2)),
            alignment: .top
        )
    }

    private func iconFor(_ type: AudienceType) -> String {
        switch type {
        case .everyone: return "globe"
        case .followers: return "person.2.fill"
        case .closeFriends: return "star.circle.fill"
        }
    }
}

struct CreatePostToolbar_Previews: PreviewProvider {
    static var previews: some View {
        CreatePostToolbar(
            onPhotoTapped: {},
            onCameraTapped: {},
            onGiftTapped: {},
            onPollToggled: {},
            onLocationToggled: {},
            audienceType: .constant(.everyone)
        )
        .previewLayout(.sizeThatFits)
    }
}
