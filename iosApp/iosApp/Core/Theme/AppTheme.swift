import SwiftUI
import shared

extension Color {
    init(hex: Int64, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

struct AppTheme {
    static let primaryColor = Color(hex: shared.SynapseTheme.RoyalBlue)
    static let secondaryColor = Color.gray

    struct Spacing {
        static let small = CGFloat(shared.SynapseTheme.SpacingSmall)
        static let medium = CGFloat(shared.SynapseTheme.SpacingMedium)
        static let large = CGFloat(shared.SynapseTheme.SpacingLarge)
    }

    struct Fonts {
        static let titleFont = Font.custom("Baskerville-Bold", size: 26)
        static let bodyFont = Font.system(size: 16)
    }
}
