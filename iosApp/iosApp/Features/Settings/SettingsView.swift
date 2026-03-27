import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        List {
            Section(header: Text("Account")) {
                Text("Account Preferences")
                Text("Security")
                Text("Privacy")
            }

            Section(header: Text("App Preferences")) {
                Text("Notifications")
                Text("Appearance")
            }

            Section {
                Button(action: {
                    // Simulate Logout
                    navigator.isUserLoggedIn = false
                    navigator.reset()
                }) {
                    Text("Log Out")
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }
        }
        .navigationTitle("Settings")
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
