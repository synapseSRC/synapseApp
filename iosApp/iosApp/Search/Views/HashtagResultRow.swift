import SwiftUI
import shared

struct HashtagResultRow: View {
    let hashtag: SearchHashtag
    let isTrending: Bool

    var body: some View {
        HStack(spacing: 16) {
            // Icon
            ZStack {
                Circle()
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 44, height: 44)

                Image(systemName: "number")
                    .foregroundColor(.blue)
                    .font(.system(size: 20, weight: .bold))
            }

            // Text Details
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("#\(hashtag.tag)")
                        .font(.headline)
                        .foregroundColor(.primary)

                    if isTrending {
                        Image(systemName: "arrow.up.right.circle.fill")
                            .foregroundColor(.orange)
                            .font(.system(size: 14))
                    }
                }

                Text("\(hashtag.count) Posts")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Sparkline pseudo representation
            if !hashtag.sparklinePoints.isEmpty {
                // Future expansion: render a tiny sparkline chart
                Image(systemName: "chart.xyaxis.line")
                    .foregroundColor(.blue.opacity(0.5))
            } else {
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray.opacity(0.5))
                    .font(.system(size: 14))
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal)
        .background(Color(.systemBackground))
    }
}
