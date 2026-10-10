import SwiftUI

struct AnimatedCounter: View {
    let count: Int
    let font: Font
    let fontWeight: Font.Weight
    let color: Color

    init(
        count: Int,
        font: Font = .body,
        fontWeight: Font.Weight = .regular,
        color: Color = .primary
    ) {
        self.count = count
        self.font = font
        self.fontWeight = fontWeight
        self.color = color
    }

    var body: some View {
        Text(formatCount(count))
            .font(font)
            .fontWeight(fontWeight)
            .foregroundColor(color)
            .contentTransition(.numericText())
            .animation(.spring(response: 0.35, dampingFraction: 0.7), value: count)
    }

    private func formatCount(_ count: Int) -> String {
        if count >= 1_000_000_000 {
            let formatted = Double(count) / 1_000_000_000.0
            return formatted.truncatingRemainder(dividingBy: 1) == 0
                ? String(format: "%.0fB", formatted)
                : String(format: "%.1fB", formatted)
        } else if count >= 1_000_000 {
            let formatted = Double(count) / 1_000_000.0
            return formatted.truncatingRemainder(dividingBy: 1) == 0
                ? String(format: "%.0fM", formatted)
                : String(format: "%.1fM", formatted)
        } else if count >= 1_000 {
            let formatted = Double(count) / 1_000.0
            return String(format: "%.1fK", formatted)
        } else {
            return "\(count)"
        }
    }
}
