import SwiftUI

struct PrivacySettingsView: View {
    @State private var isPrivate = false
    @State private var whoCanMessageMe = "Everyone"
    @State private var twoFactorEnabled = false

    var body: some View {
        Form {
            Section(header: Text("Privacy Preferences")) {
                Toggle("Private Account", isOn: $isPrivate)
                    .accessibilityLabel("Toggle Private Account Status")

                Picker("Who can message me", selection: $whoCanMessageMe) {
                    Text("Everyone").tag("Everyone")
                    Text("Followers Only").tag("Followers")
                    Text("No One").tag("No One")
                }
                .accessibilityLabel("Who can message me preference")
            }

            Section(header: Text("Security")) {
                Toggle("Two-Factor Authentication (2FA)", isOn: $twoFactorEnabled)
                    .accessibilityLabel("Toggle Two-Factor Authentication")

                Button("View Login Alerts") {
                    print("Navigating to login alerts")
                }
                .accessibilityLabel("View Login Alerts Button")
            }

            Section(header: Text("Blocked Users")) {
                NavigationLink("Manage Blocked Users", destination: BlockedUsersView())
                    .accessibilityLabel("Manage Blocked Users List Button")
            }
        }
        .navigationTitle("Privacy & Security")
    }
}
