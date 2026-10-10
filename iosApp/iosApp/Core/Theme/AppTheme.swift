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
    static let primaryColor = Color(hex: shared.SynapseColors.shared.Light.primary)
    static let secondaryColor = Color(hex: shared.SynapseColors.shared.Light.secondary)
    static let onlineColor = Color(hex: shared.SynapseColors.shared.Light.statusOnline)
    static let errorColor = Color(hex: shared.SynapseColors.shared.Light.error)

    struct AmbientBackground: View {
        @StateObject private var atmosphere = UiAtmosphereState.shared
        @Environment(\.colorScheme) var colorScheme

        var body: some View {
            LinearGradient(
                gradient: Gradient(colors: [
                    atmosphere.dominantColor.opacity(colorScheme == .dark ? 0.15 : 0.1),
                    .clear
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        }
    }

    struct Spacing {
        static let small = CGFloat(shared.SynapseTheme.shared.SpacingSmall)
        static let medium = CGFloat(shared.SynapseTheme.shared.SpacingMedium)
        static let large = CGFloat(shared.SynapseTheme.shared.SpacingLarge)
    }

    struct Fonts {
        static let titleFont = Font.custom("Baskerville-Bold", size: 26)
        static let bodyFont = Font.system(size: 16)
    }
}
