import SwiftUI

struct StoryAvatarView: View {
    let imageUrl: URL?
    let title: String
    let isAddButton: Bool
    let hasUnseen: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                ZStack(alignment: .bottomTrailing) {
                    // Avatar or Placeholder
                    ZStack {
                        Circle()
                            .fill(Color.gray.opacity(0.3))

                        if let imageUrl = imageUrl {
                            AsyncImage(url: imageUrl) { image in
                                image
                                    .resizable()
                                    .scaledToFill()
                            } placeholder: {
                                ProgressView()
                            }
                            .clipShape(Circle())
                        } else {
                            Text(String(title.prefix(1)).uppercased())
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(width: 60, height: 60)
                    .clipShape(Circle())
                    .padding(3)
                    .background(
                        Group {
                            if hasUnseen {
                                Circle()
                                    .strokeBorder(
                                        LinearGradient(
                                            gradient: Gradient(colors: [.yellow, .red, .purple]),
                                            startPoint: .bottomLeading,
                                            endPoint: .topTrailing
                                        ),
                                        lineWidth: 3
                                    )
                            } else if !isAddButton {
                                Circle()
                                    .strokeBorder(Color.gray.opacity(0.5), lineWidth: 2)
                            } else {
                                Color.clear
                            }
                        }
                    )

                    if isAddButton {
                        ZStack {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 20, height: 20)

                            Image(systemName: "plus")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .background(Circle().fill(Color.black).frame(width: 24, height: 24))
                        .offset(x: 2, y: 2)
                    }
                }

                Text(isAddButton ? "Your Story" : title)
                    .font(.caption)
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .frame(width: 70)
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}
