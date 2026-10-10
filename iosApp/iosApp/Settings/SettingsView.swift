import SwiftUI

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()
    @State private var searchText = ""
    @State private var isAccountExpanded = false
    @State private var isPreferencesExpanded = false

    var body: some View {
        List {
            // Hero Cards Section
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        HeroCardView(
                            title: "Secure Account",
                            description: "Backup your encryption keys now.",
                            systemImage: "shield.checkered",
                            color: .purple
                        ) {
                            // Action
                        }

                        HeroCardView(
                            title: "Storage Low",
                            description: "Clear 1.2GB of cached data.",
                            systemImage: "externaldrive.fill",
                            color: .orange
                        ) {
                            // Action
                        }
                    }
                    .padding(.vertical, 8)
                }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            // Expandable Sections
            Section {
                DisclosureGroup(isExpanded: $isAccountExpanded) {
                    NavigationLink(destination: AccountSettingsView()) {
                        Label("Account Preferences", systemImage: "person.text.rectangle")
                    }
                    NavigationLink(destination: PrivacySettingsView()) {
                        Label("Security & Privacy", systemImage: "lock.shield")
                    }
                } label: {
                    Label("Account", systemImage: "person.crop.circle")
                        .font(.headline)
                }
            }

            Section {
                DisclosureGroup(isExpanded: $isPreferencesExpanded) {
                    NavigationLink(destination: NotificationSettingsView(viewModel: viewModel)) {
                        Label("Notifications", systemImage: "bell.badge")
                    }
                    NavigationLink(destination: ThemeSettingsView()) {
                        Label("Appearance", systemImage: "paintbrush")
                    }
                    NavigationLink(destination: DataStorageSettingsView()) {
                        Label("Data & Storage", systemImage: "tray.full")
                    }
                } label: {
                    Label("Preferences", systemImage: "slider.horizontal.3")
                        .font(.headline)
                }
            }

            Section(header: Text("Information")) {
                NavigationLink(destination: AboutHelpView()) {
                    Label("About & Help", systemImage: "questionmark.circle")
                }
            }
        }
        .listStyle(InsetGroupedListStyle())
        .navigationTitle("Settings")
        .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search Settings")
        .accessibilityLabel("Settings Menu")
        .animation(.default, value: isAccountExpanded)
        .animation(.default, value: isPreferencesExpanded)
    }
}
