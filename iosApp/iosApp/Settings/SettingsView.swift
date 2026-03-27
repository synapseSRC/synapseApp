import SwiftUI

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        List {
            Section(header: Text("Account")) {
                NavigationLink(destination: AccountSettingsView()) {
                    Label("Account Settings", systemImage: "person.crop.circle")
                }
                NavigationLink(destination: PrivacySettingsView()) {
                    Label("Privacy & Security", systemImage: "hand.raised")
                }
            }

            Section(header: Text("Preferences")) {
                NavigationLink(destination: NotificationSettingsView(viewModel: viewModel)) {
                    Label("Notifications", systemImage: "bell")
                }
                NavigationLink(destination: ThemeSettingsView()) {
                    Label("Appearance", systemImage: "paintbrush")
                }
                NavigationLink(destination: DataStorageSettingsView()) {
                    Label("Data & Storage", systemImage: "externaldrive")
                }
            }

            Section(header: Text("Information")) {
                NavigationLink(destination: AboutHelpView()) {
                    Label("About & Help", systemImage: "questionmark.circle")
                }
            }
        }
        .navigationTitle("Settings")
        .accessibilityLabel("Settings Menu")
    }
}
