import SwiftUI
struct NotificationSettingsView: View {
    @ObservedObject var viewModel: SettingsViewModel
    var body: some View {
        Form {
            Section {
                Toggle("Security Notifications", isOn: $viewModel.securityNotificationsEnabled)
                    .onChange(of: viewModel.securityNotificationsEnabled) { _ in
                        Task { await viewModel.savePreferences() }
                    }
            }
        }
        .navigationTitle("Notifications")
    }
}
