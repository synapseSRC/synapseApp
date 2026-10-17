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

    /// CGFloat adapters over Synapse's custom shared scale; these are not M3 spacing tokens.
    struct Spacing {
        static let space0 = CGFloat(shared.SynapseSpacing.shared.space0)
        static let space2 = CGFloat(shared.SynapseSpacing.shared.space2)
        static let space4 = CGFloat(shared.SynapseSpacing.shared.space4)
        static let space6 = CGFloat(shared.SynapseSpacing.shared.space6)
        static let space8 = CGFloat(shared.SynapseSpacing.shared.space8)
        static let space10 = CGFloat(shared.SynapseSpacing.shared.space10)
        static let space12 = CGFloat(shared.SynapseSpacing.shared.space12)
        static let space16 = CGFloat(shared.SynapseSpacing.shared.space16)
        static let space20 = CGFloat(shared.SynapseSpacing.shared.space20)
        static let space24 = CGFloat(shared.SynapseSpacing.shared.space24)
        static let space28 = CGFloat(shared.SynapseSpacing.shared.space28)
        static let space32 = CGFloat(shared.SynapseSpacing.shared.space32)
        static let space40 = CGFloat(shared.SynapseSpacing.shared.space40)
        static let space48 = CGFloat(shared.SynapseSpacing.shared.space48)
        static let space64 = CGFloat(shared.SynapseSpacing.shared.space64)
    }

    struct Fonts {
        static let titleFont = Font.custom("Baskerville-Bold", size: 26)
        static let bodyFont = Font.system(size: 16)
    }
}
