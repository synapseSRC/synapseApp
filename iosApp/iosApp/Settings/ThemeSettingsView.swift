import SwiftUI

struct ThemeSettingsView: View {
    @AppStorage("appTheme") private var appTheme: String = "system"
    @AppStorage("highContrast") private var highContrast: Bool = false
    @AppStorage("reducedMotion") private var reducedMotion: Bool = false

    var body: some View {
        Form {
            Section(header: Text("Appearance")) {
                Picker("Theme", selection: $appTheme) {
                    Text("System").tag("system")
                    Text("Light").tag("light")
                    Text("Dark").tag("dark")
                }
                .pickerStyle(SegmentedPickerStyle())
                .accessibilityLabel("Select App Theme")
            }

            Section(header: Text("Accessibility")) {
                Toggle("High Contrast Mode", isOn: $highContrast)
                    .accessibilityLabel("Enable High Contrast Mode")
                Toggle("Reduced Motion", isOn: $reducedMotion)
                    .accessibilityLabel("Enable Reduced Motion")
            }
        }
        .navigationTitle("Theme & Appearance")
    }
}
