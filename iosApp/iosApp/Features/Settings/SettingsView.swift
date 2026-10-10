import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        List {
            Section(header: Text(String(localized: "settings_section_account"))) {
                Text(String(localized: "settings_account_preferences"))
                Text(String(localized: "settings_security"))
                Text(String(localized: "settings_privacy"))
            }

            Section(header: Text(String(localized: "settings_section_app_preferences"))) {
                Text(String(localized: "nav_notifications"))
                Text(String(localized: "settings_appearance"))
            }

            Section {
                Button(action: {
                    // Simulate Logout
                    navigator.isUserLoggedIn = false
                    navigator.reset()
                }) {
                    Text(String(localized: "action_log_out"))
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }
        }
        .navigationTitle(String(localized: "nav_settings"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            SettingsView()
                .environmentObject(AppNavigator())
        }
    }
}
